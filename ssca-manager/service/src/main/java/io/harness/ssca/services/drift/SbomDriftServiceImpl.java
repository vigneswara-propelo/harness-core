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
import io.harness.spec.server.ssca.v1.model.ArtifactSbomDriftRequestBody;
import io.harness.spec.server.ssca.v1.model.ArtifactSbomDriftResponse;
import io.harness.ssca.beans.drift.ComponentDrift;
import io.harness.ssca.beans.drift.ComponentDriftResults;
import io.harness.ssca.beans.drift.ComponentDriftStatus;
import io.harness.ssca.beans.drift.DriftBase;
import io.harness.ssca.beans.drift.LicenseDrift;
import io.harness.ssca.entities.ArtifactEntity;
import io.harness.ssca.entities.drift.DriftEntity;
import io.harness.ssca.entities.drift.DriftEntity.DriftEntityKeys;
import io.harness.ssca.helpers.SbomDriftCalculator;
import io.harness.ssca.services.ArtifactService;

import com.google.inject.Inject;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(HarnessTeam.SSCA)
public class SbomDriftServiceImpl implements SbomDriftService {
  @Inject SbomDriftRepository sbomDriftRepository;
  @Inject ArtifactService artifactService;
  @Inject SbomDriftCalculator sbomDriftCalculator;

  @Override
  public ArtifactSbomDriftResponse calculateSbomDrift(
      String accountId, String orgId, String projectId, String artifactId, ArtifactSbomDriftRequestBody requestBody) {
    String tag = requestBody.getTag();
    String baseTag = requestBody.getBaseTag();
    ArtifactEntity baseArtifact = artifactService.getLatestArtifact(accountId, orgId, projectId, artifactId, baseTag);
    if (baseArtifact == null) {
      throw new InvalidRequestException("Could not find artifact with tag: " + baseTag);
    }

    ArtifactEntity driftArtifact = artifactService.getLatestArtifact(accountId, orgId, projectId, artifactId, tag);
    if (driftArtifact == null) {
      throw new InvalidRequestException("Could not find artifact with tag: " + tag);
    }

    Optional<DriftEntity> optionalDriftEntity =
        sbomDriftRepository
            .findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndOrchestrationIdAndBaseOrchestrationId(
                accountId, orgId, projectId, driftArtifact.getOrchestrationId(), baseArtifact.getOrchestrationId());
    DriftEntity driftEntity;
    if (optionalDriftEntity.isPresent()) {
      driftEntity = optionalDriftEntity.get();
      // update validUntil if same request has been found.
      Criteria criteria = Criteria.where(DriftEntityKeys.uuid).is(driftEntity.getUuid());
      Update update = new Update();
      update.set(DriftEntityKeys.validUntil, Date.from(OffsetDateTime.now().plusHours(1).toInstant()));
      sbomDriftRepository.update(new Query(criteria), update);
    } else {
      List<ComponentDrift> componentDrifts = sbomDriftCalculator.findComponentDrifts(
          baseArtifact.getOrchestrationId(), driftArtifact.getOrchestrationId());
      List<LicenseDrift> licenseDrifts =
          sbomDriftCalculator.findLicenseDrift(baseArtifact.getOrchestrationId(), driftArtifact.getOrchestrationId());
      driftEntity = sbomDriftRepository.save(DriftEntity.builder()
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
                                                 .licenseDrifts(licenseDrifts)
                                                 .validUntil(Date.from(OffsetDateTime.now().plusHours(1).toInstant()))
                                                 .build());
    }
    return new ArtifactSbomDriftResponse()
        .driftId(driftEntity.getUuid())
        .tag(tag)
        .baseTag(baseTag)
        .artifactName(driftArtifact.getName());
  }

  @Override
  public ComponentDriftResults getComponentDrifts(String accountId, String orgId, String projectId, String driftId,
      ComponentDriftStatus status, Pageable pageable) {
    Criteria criteria = Criteria.where("_id").is(driftId);
    if (!sbomDriftRepository.exists(criteria)) {
      throw new InvalidRequestException("Couldn't find the drift with drift ID " + driftId);
    }

    int page = pageable.getPageNumber();
    int pageSize = pageable.getPageSize();
    Aggregation aggregation = Aggregation.newAggregation(Aggregation.match(criteria),
        Aggregation.project(DriftEntityKeys.componentDrifts, "_id"), Aggregation.unwind("$componentDrifts"),
        Aggregation.match(getStatusMatchCriteria(status)),
        Aggregation.group("$_id").count().as("totalComponentDrifts").push("componentDrifts").as("componentDrifts"),
        Aggregation.project("totalComponentDrifts")
            .and("componentDrifts")
            .slice(pageSize, page * pageSize)
            .as("componentDrifts")
            .andExclude("_id"));

    List<ComponentDriftResults> componentDriftResults =
        sbomDriftRepository.aggregate(aggregation, ComponentDriftResults.class);

    // this would mean that there is something wrong with the component drift query, do recheck
    if (componentDriftResults == null) {
      throw new InvalidRequestException("Unable to find component drifts");
    }

    // If result list is empty, it means query was correct but returned empty results.
    if (componentDriftResults.isEmpty()) {
      return null;
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
}
