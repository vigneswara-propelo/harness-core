/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.ssca.services.remediation_tracker;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidArgumentsException;
import io.harness.repositories.remediation_tracker.RemediationTrackerRepository;
import io.harness.spec.server.ssca.v1.model.ComponentFilter;
import io.harness.spec.server.ssca.v1.model.Operator;
import io.harness.spec.server.ssca.v1.model.RemediationTrackerCreateRequestBody;
import io.harness.ssca.beans.remediation_tracker.PatchedPendingArtifactEntitiesResult;
import io.harness.ssca.enforcement.executors.mongo.filter.denylist.fields.VersionField;
import io.harness.ssca.entities.ArtifactEntity;
import io.harness.ssca.entities.CdInstanceSummary;
import io.harness.ssca.entities.remediation_tracker.ArtifactInfo;
import io.harness.ssca.entities.remediation_tracker.DeploymentsCount;
import io.harness.ssca.entities.remediation_tracker.EnvironmentInfo;
import io.harness.ssca.entities.remediation_tracker.Pipeline;
import io.harness.ssca.entities.remediation_tracker.RemediationCondition;
import io.harness.ssca.entities.remediation_tracker.RemediationStatus;
import io.harness.ssca.entities.remediation_tracker.RemediationTrackerEntity;
import io.harness.ssca.entities.remediation_tracker.RemediationTrackerEntity.RemediationTrackerEntityKeys;
import io.harness.ssca.mapper.RemediationTrackerMapper;
import io.harness.ssca.services.ArtifactService;
import io.harness.ssca.services.CdInstanceSummaryService;
import io.harness.ssca.services.NormalisedSbomComponentService;

import com.google.inject.Inject;
import com.nimbusds.oauth2.sdk.util.CollectionUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public class RemediationTrackerServiceImpl implements RemediationTrackerService {
  @Inject RemediationTrackerRepository repository;

  @Inject ArtifactService artifactService;

  @Inject CdInstanceSummaryService cdInstanceSummaryService;

  @Inject NormalisedSbomComponentService normalisedSbomComponentService;
  @Override
  public String createRemediationTracker(
      String accountId, String orgId, String projectId, RemediationTrackerCreateRequestBody body) {
    validateRemediationCreateRequest(body);
    RemediationTrackerEntity remediationTracker =
        RemediationTrackerEntity.builder()
            .accountIdentifier(accountId)
            .orgIdentifier(orgId)
            .projectIdentifier(projectId)
            .comments(body.getComments())
            .contactInfo(RemediationTrackerMapper.getContactInfo(body.getContact()))
            .condition(RemediationTrackerMapper.getRemediationCondition(body.getRemediationCondition()))
            .vulnerabilityInfo(RemediationTrackerMapper.getVulnerabilityInfo(body.getVulnerabilityInfo()))
            .status(RemediationStatus.ON_GOING)
            .startTimeMilli(System.currentTimeMillis())
            .build();
    remediationTracker = repository.save(remediationTracker);
    // If this increases API latency, we can move this to a separate thread or a job.
    updateArtifactsAndEnvironments(remediationTracker);
    return remediationTracker.getUuid();
  }

  @Override
  public void updateArtifactsAndEnvironments(RemediationTrackerEntity remediationTracker) {
    List<ComponentFilter> componentFilter = getComponentFilters(remediationTracker);
    List<String> orchestrationIdsMatchingTrackerFilter = getOrchestrationIds(remediationTracker, componentFilter);

    // Auto closing if no orchestrations matching the filter.
    closeTrackerIfNoOrchestrations(remediationTracker, orchestrationIdsMatchingTrackerFilter);

    List<ArtifactEntity> patchedArtifactEntities = new ArrayList<>();
    List<ArtifactEntity> pendingArtifactEntities = new ArrayList<>();
    processPatchedPendingArtifactEntities(
        remediationTracker, orchestrationIdsMatchingTrackerFilter, patchedArtifactEntities, pendingArtifactEntities);

    // Auto closing if no pending entities.
    closeTrackerIfNoPendingEntities(pendingArtifactEntities, remediationTracker);

    List<ArtifactInfo> artifactInfos =
        getArtifactInfo(remediationTracker, patchedArtifactEntities, pendingArtifactEntities);

    updateRemediationTrackerWithDetails(remediationTracker, artifactInfos);
    Criteria criteria = Criteria.where(RemediationTrackerEntityKeys.uuid).is(remediationTracker.getUuid());
    Update update = new Update();
    update.set(RemediationTrackerEntityKeys.artifactInfos, remediationTracker.getArtifactInfos());
    update.set(RemediationTrackerEntityKeys.deploymentsCount, remediationTracker.getDeploymentsCount());
    update.set(RemediationTrackerEntityKeys.status, remediationTracker.getStatus());
    update.set(RemediationTrackerEntityKeys.endTimeMilli, remediationTracker.getEndTimeMilli());
    repository.update(new Query(criteria), update);
  }

  @Override
  public RemediationTrackerEntity getRemediationTracker(String remediationTrackerId) {
    return repository.findById(remediationTrackerId)
        .orElseThrow(() -> new InvalidArgumentsException("Remediation Tracker not found"));
  }

  private void validateRemediationCreateRequest(RemediationTrackerCreateRequestBody body) {
    if ((body.getRemediationCondition().getOperator()
            != io.harness.spec.server.ssca.v1.model.RemediationCondition.OperatorEnum.ALL)
        && (body.getRemediationCondition().getOperator()
            != io.harness.spec.server.ssca.v1.model.RemediationCondition.OperatorEnum.MATCHES)) {
      List<Integer> versions = VersionField.getVersion(body.getVulnerabilityInfo().getComponentVersion());
      if (versions.size() != 3 || versions.get(0) == -1) {
        throw new InvalidArgumentsException(
            "Unsupported Version Format. Semantic Versioning is required for LessThan and LessThanEquals operator.");
      }
    }
  }

  private void closeTrackerIfNoOrchestrations(
      RemediationTrackerEntity remediationTracker, List<String> orchestrationIdsMatchingTrackerFilter) {
    if (orchestrationIdsMatchingTrackerFilter.isEmpty()) {
      closeTracker(remediationTracker);
    }
  }

  private void closeTrackerIfNoPendingEntities(
      List<ArtifactEntity> pendingArtifactEntities, RemediationTrackerEntity remediationTracker) {
    if (pendingArtifactEntities.isEmpty()) {
      closeTracker(remediationTracker);
    }
  }

  private void closeTracker(RemediationTrackerEntity remediationTracker) {
    remediationTracker.setStatus(RemediationStatus.COMPLETED);
    remediationTracker.setEndTimeMilli(System.currentTimeMillis());
  }

  private void updateRemediationTrackerWithDetails(
      RemediationTrackerEntity remediationTracker, List<ArtifactInfo> artifactInfos) {
    DeploymentsCount deploymentsCount = DeploymentsCount.builder().build();
    Map<String, ArtifactInfo> artifactInfoMap =
        artifactInfos.stream().collect(Collectors.toMap(ArtifactInfo::getArtifactId, artifactInfo -> {
          if (remediationTracker.getArtifactInfos() != null
              && remediationTracker.getArtifactInfos().containsKey(artifactInfo.getArtifactId())) {
            artifactInfo.setTicketId(
                remediationTracker.getArtifactInfos().get(artifactInfo.getArtifactId()).getTicketId());
            artifactInfo.setExcluded(
                remediationTracker.getArtifactInfos().get(artifactInfo.getArtifactId()).isExcluded());
          }
          if (!artifactInfo.isExcluded()) {
            deploymentsCount.add(artifactInfo.getDeploymentsCount());
          }
          return artifactInfo;
        }));
    remediationTracker.setDeploymentsCount(deploymentsCount);
    remediationTracker.setArtifactInfos(artifactInfoMap);
  }

  private List<PatchedPendingArtifactEntitiesResult> getPatchedAndPendingArtifacts(
      RemediationTrackerEntity remediationTracker, List<String> orchestrationIdsMatchingTrackerFilter) {
    Set<String> artifactIdsFromTrackerEntity = remediationTracker.getArtifactInfos() != null
        ? remediationTracker.getArtifactInfos().keySet()
        : new HashSet<>();
    Set<String> artifactIdsMatchingTrackerFilter =
        artifactService.getDistinctArtifactIds(remediationTracker.getAccountId(), remediationTracker.getOrgIdentifier(),
            remediationTracker.getProjectIdentifier(), orchestrationIdsMatchingTrackerFilter);
    Set<String> artifactIds = new HashSet<>(artifactIdsFromTrackerEntity);
    artifactIds.addAll(artifactIdsMatchingTrackerFilter);
    return artifactService.listDeployedArtifactsFromIdsWithCriteria(remediationTracker.getAccountId(),
        remediationTracker.getOrgIdentifier(), remediationTracker.getProjectIdentifier(), artifactIds,
        orchestrationIdsMatchingTrackerFilter);
  }

  private void processPatchedPendingArtifactEntities(RemediationTrackerEntity remediationTracker,
      List<String> orchestrationIdsMatchingTrackerFilter, List<ArtifactEntity> patchedArtifactEntities,
      List<ArtifactEntity> pendingArtifactEntities) {
    List<PatchedPendingArtifactEntitiesResult> results =
        getPatchedAndPendingArtifacts(remediationTracker, orchestrationIdsMatchingTrackerFilter);
    if (EmptyPredicate.isNotEmpty(results)) {
      PatchedPendingArtifactEntitiesResult result = results.get(0);
      patchedArtifactEntities.addAll(result.getPatchedArtifacts());
      pendingArtifactEntities.addAll(result.getPendingArtifacts());
    }
  }

  private List<String> getOrchestrationIds(
      RemediationTrackerEntity remediationTracker, List<ComponentFilter> componentFilter) {
    return (CollectionUtils.isNotEmpty(componentFilter))
        ? normalisedSbomComponentService.getOrchestrationIds(remediationTracker.getAccountId(),
            remediationTracker.getOrgIdentifier(), remediationTracker.getProjectIdentifier(), null, componentFilter)
        : Collections.emptyList();
  }

  private List<ComponentFilter> getComponentFilters(RemediationTrackerEntity entity) {
    List<ComponentFilter> componentFilter = new ArrayList<>();
    componentFilter.add(new ComponentFilter()
                            .fieldName(ComponentFilter.FieldNameEnum.COMPONENTNAME)
                            .operator(Operator.EQUALS)
                            .value(entity.getVulnerabilityInfo().getComponent()));

    Operator mappedOperator = mapOperator(entity.getCondition().getOperator());
    if (mappedOperator != null) {
      componentFilter.add(new ComponentFilter()
                              .fieldName(ComponentFilter.FieldNameEnum.COMPONENTVERSION)
                              .operator(mappedOperator)
                              .value(entity.getCondition().getVersion()));
    }

    return componentFilter;
  }

  private List<ArtifactInfo> getArtifactInfo(RemediationTrackerEntity remediationTracker,
      List<ArtifactEntity> patchedArtifactEntities, List<ArtifactEntity> pendingArtifactEntities) {
    // Steps: 1. Get cd instance summaries for all the artifact correlation ids.
    // 2. Build artifact details from the patched and pending artifact entities.
    // 3. Build artifact info from the cd instance summaries. If the tag is patched, we update the
    // latest tag with fix details build in the prev step.
    // 4. If the artifact is not excluded, we update the deployments count.
    List<CdInstanceSummary> cdInstanceSummaries =
        getCdInstanceSummaries(remediationTracker, patchedArtifactEntities, pendingArtifactEntities);
    Map<String, ArtifactDetails> artifactCorelationIdToDetailMap =
        getArtifactCorelationIdToDetailMap(patchedArtifactEntities, pendingArtifactEntities);
    Map<String, ArtifactInfo> artifactIdtoInfoMap = new HashMap<>();
    DeploymentsCount deploymentsCount = DeploymentsCount.builder().build();
    for (CdInstanceSummary summary : cdInstanceSummaries) {
      ArtifactDetails details = artifactCorelationIdToDetailMap.get(summary.getArtifactCorrelationId());
      ArtifactInfo info = artifactIdtoInfoMap.computeIfAbsent(details.getArtifactId(),
          id
          -> ArtifactInfo.builder()
                 .artifactId(details.getArtifactId())
                 .artifactName(details.getArtifactName())
                 .environments(new ArrayList<>())
                 .deploymentsCount(DeploymentsCount.builder().build())
                 .build());
      // Build details for the tag are fetched from the artifact entity.
      // We generated those details from the artifact entity. So, we can use the same details.
      if (details.isPatched() && info.getLatestTagWithFixPipelineTriggeredAt() < details.getCreatedOn()) {
        info.setLatestTagWithFixPipelineTriggeredAt(details.getCreatedOn());
        info.setLatestTagWithFix(details.getArtifactTag());
        info.setLatestTagWithFixPipelineId(details.getBuildPipelineId());
        info.setLatestTagWithFixPipelineExecutionId(details.getBuildPipelineExecutionId());
      }

      info.getDeploymentsCount().update(summary.getEnvType(), details.isPatched());
      EnvironmentInfo environmentInfo = buildEnvironmentInfo(summary, details);
      info.getEnvironments().add(environmentInfo);
      artifactIdtoInfoMap.put(details.getArtifactId(), info);
      // if artifact is excluded, we don't count it in deployments count.
      if (!info.isExcluded()) {
        deploymentsCount.update(summary.getEnvType(), details.isPatched());
      }
    }

    return new ArrayList<>(artifactIdtoInfoMap.values());
  }

  private Map<String, ArtifactDetails> getArtifactCorelationIdToDetailMap(
      List<ArtifactEntity> patchedArtifactEntities, List<ArtifactEntity> pendingArtifactEntities) {
    Map<String, ArtifactDetails> artifactCorelationIdToDetailMap = new HashMap<>();
    for (ArtifactEntity entity : patchedArtifactEntities) {
      artifactCorelationIdToDetailMap.put(entity.getArtifactCorrelationId(),
          ArtifactDetails.builder()
              .artifactId(entity.getArtifactId())
              .artifactName(entity.getName())
              .artifactTag(entity.getTag())
              .patched(true)
              .createdOn(entity.getCreatedOn().toEpochMilli())
              .buildPipelineId(entity.getPipelineId())
              .buildPipelineExecutionId(entity.getPipelineExecutionId())
              .build());
    }
    for (ArtifactEntity entity : pendingArtifactEntities) {
      artifactCorelationIdToDetailMap.put(entity.getArtifactCorrelationId(),
          ArtifactDetails.builder()
              .artifactId(entity.getArtifactId())
              .artifactName(entity.getName())
              .artifactTag(entity.getTag())
              .patched(false)
              .createdOn(entity.getCreatedOn().toEpochMilli())
              .buildPipelineId(entity.getPipelineId())
              .buildPipelineExecutionId(entity.getPipelineExecutionId())
              .build());
    }
    return artifactCorelationIdToDetailMap;
  }

  private List<CdInstanceSummary> getCdInstanceSummaries(RemediationTrackerEntity remediationTracker,
      List<ArtifactEntity> patchedArtifactEntities, List<ArtifactEntity> pendingArtifactEntities) {
    List<String> artifactCorelationIds =
        Stream
            .concat(patchedArtifactEntities.stream().map(ArtifactEntity::getArtifactCorrelationId),
                pendingArtifactEntities.stream().map(ArtifactEntity::getArtifactCorrelationId))
            .collect(Collectors.toList());
    return cdInstanceSummaryService.getCdInstanceSummaries(remediationTracker.getAccountId(),
        remediationTracker.getOrgIdentifier(), remediationTracker.getProjectIdentifier(), artifactCorelationIds);
  }

  private Operator mapOperator(RemediationCondition.Operator operator) {
    switch (operator) {
      case LESS_THAN:
        return Operator.LESSTHAN;
      case LESS_THAN_EQUALS:
        return Operator.LESSTHANEQUALS;
      case EQUALS:
        return Operator.EQUALS;
      case ALL:
        return null;
      default:
        throw new InvalidArgumentsException("Unsupported Operator " + operator);
    }
  }

  private EnvironmentInfo buildEnvironmentInfo(CdInstanceSummary summary, ArtifactDetails details) {
    return EnvironmentInfo.builder()
        .envIdentifier(summary.getEnvIdentifier())
        .envName(summary.getEnvName())
        .tag(details.getArtifactTag())
        .envType(summary.getEnvType())
        .deploymentPipeline(buildDeploymentPipeline(summary))
        .isPatched(details.isPatched())
        .build();
  }

  private Pipeline buildDeploymentPipeline(CdInstanceSummary summary) {
    return Pipeline.builder()
        .pipelineId(summary.getLastPipelineExecutionName())
        .pipelineExecutionId(summary.getLastPipelineExecutionId())
        .triggeredById(summary.getLastDeployedById())
        .triggeredBy(summary.getLastDeployedByName())
        .triggeredAt(summary.getLastDeployedAt())
        .build();
  }

  @Data
  @Builder
  static class ArtifactDetails {
    String artifactId;
    String artifactName;
    String artifactTag;
    boolean patched;
    long createdOn;
    String buildPipelineId;
    String buildPipelineExecutionId;
  }
}
