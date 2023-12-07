/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.services.drift;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.repositories.drift.SbomDriftRepository;
import io.harness.ssca.beans.drift.ComponentDrift;
import io.harness.ssca.beans.drift.ComponentDriftResults;
import io.harness.ssca.beans.drift.ComponentDriftStatus;
import io.harness.ssca.beans.drift.DriftBase;
import io.harness.ssca.entities.ArtifactEntity;
import io.harness.ssca.entities.drift.DriftEntity;
import io.harness.ssca.entities.drift.DriftEntity.DriftEntityKeys;
import io.harness.ssca.helpers.SbomDriftCalculator;
import io.harness.ssca.services.ArtifactService;

import com.google.inject.Inject;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(HarnessTeam.SSCA)
public class SbomDriftServiceImpl implements SbomDriftService {
  @Inject SbomDriftRepository sbomDriftRepository;
  @Inject ArtifactService artifactService;
  @Inject SbomDriftCalculator sbomDriftCalculator;

  @Override
  public void calculateAndStoreComponentDrift(
      String accountId, String orgId, String projectId, String artifactId, String baseTag, String tag) {
    ArtifactEntity baseArtifact = artifactService.getLatestArtifact(accountId, orgId, projectId, artifactId, baseTag);
    if (baseArtifact == null) {
      throw new InvalidRequestException("Could not find artifact with tag: " + baseTag);
    }

    ArtifactEntity driftArtifact = artifactService.getLatestArtifact(accountId, orgId, projectId, artifactId, tag);
    if (driftArtifact == null) {
      throw new InvalidRequestException("Could not find artifact with tag: " + tag);
    }

    // TODO: Check if component drift is already calculated.

    List<ComponentDrift> componentDrifts =
        sbomDriftCalculator.findComponentDrifts(baseArtifact.getOrchestrationId(), driftArtifact.getOrchestrationId());
    DriftEntity driftEntity = DriftEntity.builder()
                                  .accountIdentifier(accountId)
                                  .orgIdentifier(orgId)
                                  .projectIdentifier(projectId)
                                  .artifactId(artifactId)
                                  .tag(tag)
                                  .baseTag(baseTag)
                                  .base(DriftBase.MANUAL)
                                  .orchestrationId(driftArtifact.getOrchestrationId())
                                  .baseOrchestrationId(baseArtifact.getOrchestrationId())
                                  .componentDrifts(componentDrifts)
                                  .validUntil(Date.from(OffsetDateTime.now().plusHours(1).toInstant()))
                                  .build();

    sbomDriftRepository.save(driftEntity);
  }

  @Override
  public ComponentDriftResults getComponentDriftsByArtifactId(String accountId, String orgId, String projectId,
      String artifactId, String baseTag, String tag, ComponentDriftStatus status, Pageable pageable) {
    Criteria criteria = getTagAndBaseTagDriftCriteria(accountId, orgId, projectId, artifactId, baseTag, tag);

    int page = pageable.getPageNumber();
    int pageSize = pageable.getPageSize();
    Aggregation aggregation =
        Aggregation.newAggregation(Aggregation.match(criteria), Aggregation.project(DriftEntityKeys.componentDrifts),
            Aggregation.unwind("$componentDrifts"), Aggregation.match(getStatusMatchCriteria(status)),
            Aggregation.group("_id").count().as("totalComponentDrifts").push("componentDrifts").as("componentDrifts"),
            Aggregation.project("totalComponentDrifts")
                .and("componentDrifts")
                .slice(page * pageSize, pageSize)
                .as("componentDrifts"));

    List<ComponentDriftResults> componentDriftResults =
        sbomDriftRepository.aggregate(aggregation, ComponentDriftResults.class);
    if (componentDriftResults == null) {
      throw new InvalidRequestException("Unable to find component drifts");
    }

    // By nature of our query, componentDriftResults will only have one element
    return componentDriftResults.get(0);
  }

  private Criteria getStatusMatchCriteria(ComponentDriftStatus status) {
    if (status == null) {
      return new Criteria();
    }
    return Criteria.where(DriftEntityKeys.COMPONENT_DRIFT_STATUS).is(status.name());
  }

  private Criteria getTagAndBaseTagDriftCriteria(
      String accountId, String orgId, String projectId, String artifactId, String baseTag, String tag) {
    return Criteria.where(DriftEntityKeys.accountIdentifier)
        .is(accountId)
        .and(DriftEntityKeys.orgIdentifier)
        .is(orgId)
        .and(DriftEntityKeys.projectIdentifier)
        .is(projectId)
        .and(DriftEntityKeys.artifactId)
        .is(artifactId)
        .and(DriftEntityKeys.baseTag)
        .is(baseTag)
        .and(DriftEntityKeys.tag)
        .is(tag);
  }
}
