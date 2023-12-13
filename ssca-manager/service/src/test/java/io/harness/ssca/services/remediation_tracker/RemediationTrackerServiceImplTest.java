/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.services.remediation_tracker;

import static io.harness.rule.OwnerRule.VARSHA_LALWANI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.harness.BuilderFactory;
import io.harness.SSCAManagerTestBase;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidArgumentsException;
import io.harness.rule.Owner;
import io.harness.spec.server.ssca.v1.model.RemediationCondition;
import io.harness.spec.server.ssca.v1.model.RemediationTrackerCreateRequestBody;
import io.harness.ssca.beans.remediation_tracker.PatchedPendingArtifactEntitiesResult;
import io.harness.ssca.entities.remediation_tracker.RemediationTrackerEntity;
import io.harness.ssca.services.ArtifactService;
import io.harness.ssca.services.CdInstanceSummaryService;
import io.harness.ssca.services.NormalisedSbomComponentService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class RemediationTrackerServiceImplTest extends SSCAManagerTestBase {
  @Inject RemediationTrackerService remediationTrackerService;

  @Mock ArtifactService artifactService;

  @Mock CdInstanceSummaryService cdInstanceSummaryService;

  @Mock NormalisedSbomComponentService normalisedSbomComponentService;

  private BuilderFactory builderFactory;

  @Before
  public void setup() throws IllegalAccessException {
    MockitoAnnotations.initMocks(this);
    builderFactory = BuilderFactory.getDefault();
    when(normalisedSbomComponentService.getOrchestrationIds(any(), any(), any(), any(), any()))
        .thenReturn(new ArrayList<>());
    when(artifactService.getDistinctArtifactIds(any(), any(), any(), any()))
        .thenReturn(Set.of("artifactId1", "artifactId2"));
    when(artifactService.listDeployedArtifactsFromIdsWithCriteria(any(), any(), any(), any(), any()))
        .thenReturn(Collections.singletonList(builderFactory.getPatchedPendingArtifactEntitiesResult()));
    when(cdInstanceSummaryService.getCdInstanceSummaries(any(), any(), any(), any()))
        .thenReturn(List.of(
            builderFactory.getCdInstanceSummaryBuilder().artifactCorrelationId("patched").envIdentifier("env1").build(),
            builderFactory.getCdInstanceSummaryBuilder().artifactCorrelationId("pending").build()));
    FieldUtils.writeField(remediationTrackerService, "artifactService", artifactService, true);
    FieldUtils.writeField(remediationTrackerService, "cdInstanceSummaryService", cdInstanceSummaryService, true);
  }

  @Test
  @Owner(developers = VARSHA_LALWANI)
  @Category(UnitTests.class)
  public void testCreateRemediationTracker() {
    RemediationTrackerCreateRequestBody remediationTrackerCreateRequestBody =
        builderFactory.getRemediationTrackerCreateRequestBody();
    String remediationTrackerId = remediationTrackerService.createRemediationTracker(
        builderFactory.getContext().getAccountId(), builderFactory.getContext().getOrgIdentifier(),
        builderFactory.getContext().getProjectIdentifier(), remediationTrackerCreateRequestBody);
    assertThat(remediationTrackerId).isNotNull();
    RemediationTrackerEntity remediationTrackerEntity =
        remediationTrackerService.getRemediationTracker(remediationTrackerId);
    assertThat(remediationTrackerEntity).isNotNull();
    assertThat(remediationTrackerEntity.getAccountId()).isEqualTo(builderFactory.getContext().getAccountId());
    assertThat(remediationTrackerEntity.getOrgIdentifier()).isEqualTo(builderFactory.getContext().getOrgIdentifier());
    assertThat(remediationTrackerEntity.getProjectIdentifier())
        .isEqualTo(builderFactory.getContext().getProjectIdentifier());
    assertThat(remediationTrackerEntity.getCondition()).isNotNull();
    assertThat(remediationTrackerEntity.getCondition().getOperator())
        .isEqualTo(io.harness.ssca.entities.remediation_tracker.RemediationCondition.Operator.ALL);
    assertThat(remediationTrackerEntity.getVulnerabilityInfo().getType())
        .isEqualTo(io.harness.ssca.entities.remediation_tracker.VulnerabilityInfoType.DEFAULT);
    assertThat(remediationTrackerEntity.getVulnerabilityInfo().getComponent()).isEqualTo("log4j");
    assertThat(remediationTrackerEntity.getComments()).isEqualTo("test");
    assertThat(remediationTrackerEntity.getArtifactInfos().size()).isEqualTo(1);
    assertThat(remediationTrackerEntity.getArtifactInfos().get("artifactId").getArtifactId()).isEqualTo("artifactId");
    assertThat(remediationTrackerEntity.getArtifactInfos().get("artifactId").getArtifactName()).isEqualTo("test/image");
    assertThat(remediationTrackerEntity.getArtifactInfos().get("artifactId").getEnvironments().size()).isEqualTo(2);
    assertThat(
        remediationTrackerEntity.getArtifactInfos().get("artifactId").getEnvironments().get(0).getEnvIdentifier())
        .isEqualTo("env1");
    assertThat(remediationTrackerEntity.getArtifactInfos().get("artifactId").getEnvironments().get(0).isPatched())
        .isTrue();
    assertThat(
        remediationTrackerEntity.getArtifactInfos().get("artifactId").getEnvironments().get(1).getEnvIdentifier())
        .isEqualTo("envId");
    assertThat(remediationTrackerEntity.getArtifactInfos().get("artifactId").getEnvironments().get(1).isPatched())
        .isFalse();
  }

  @Test
  @Owner(developers = VARSHA_LALWANI)
  @Category(UnitTests.class)
  public void testCreateRemediationTracker_withFailedValidation_ForVersion() {
    RemediationTrackerCreateRequestBody remediationTrackerCreateRequestBody =
        builderFactory.getRemediationTrackerCreateRequestBody();
    remediationTrackerCreateRequestBody.setRemediationCondition(
        new RemediationCondition().operator(RemediationCondition.OperatorEnum.LESSTHAN).version("tag1"));
    assertThatExceptionOfType(InvalidArgumentsException.class)
        .isThrownBy(()
                        -> remediationTrackerService.createRemediationTracker(
                            builderFactory.getContext().getAccountId(), builderFactory.getContext().getOrgIdentifier(),
                            builderFactory.getContext().getProjectIdentifier(), remediationTrackerCreateRequestBody))
        .withMessage(
            "Unsupported Version Format. Semantic Versioning is required for LessThan and LessThanEquals operator.");
  }

  @Test
  @Owner(developers = VARSHA_LALWANI)
  @Category(UnitTests.class)
  public void testUpdateArtifactsAndEnvironmentsInRemediationTracker() throws IllegalAccessException {
    RemediationTrackerCreateRequestBody remediationTrackerCreateRequestBody =
        builderFactory.getRemediationTrackerCreateRequestBody();
    String remediationTrackerId = remediationTrackerService.createRemediationTracker(
        builderFactory.getContext().getAccountId(), builderFactory.getContext().getOrgIdentifier(),
        builderFactory.getContext().getProjectIdentifier(), remediationTrackerCreateRequestBody);
    assertThat(remediationTrackerId).isNotNull();
    RemediationTrackerEntity remediationTrackerEntity =
        remediationTrackerService.getRemediationTracker(remediationTrackerId);

    when(artifactService.listDeployedArtifactsFromIdsWithCriteria(any(), any(), any(), any(), any()))
        .thenReturn(Collections.singletonList(
            PatchedPendingArtifactEntitiesResult.builder()
                .patchedArtifacts(
                    List.of(builderFactory.getArtifactEntityBuilder().artifactCorrelationId("patched").build(),
                        builderFactory.getArtifactEntityBuilder().artifactCorrelationId("patched1").build()))
                .pendingArtifacts(
                    List.of(builderFactory.getArtifactEntityBuilder().artifactCorrelationId("pending").build(),
                        builderFactory.getArtifactEntityBuilder()
                            .artifactCorrelationId("pending1")
                            .artifactId("artifactId1")
                            .build()))
                .build()));
    when(cdInstanceSummaryService.getCdInstanceSummaries(any(), any(), any(), any()))
        .thenReturn(List.of(
            builderFactory.getCdInstanceSummaryBuilder().artifactCorrelationId("patched").envIdentifier("env2").build(),
            builderFactory.getCdInstanceSummaryBuilder().artifactCorrelationId("patched").envIdentifier("env1").build(),
            builderFactory.getCdInstanceSummaryBuilder()
                .artifactCorrelationId("patched1")
                .envIdentifier("env3")
                .build(),
            builderFactory.getCdInstanceSummaryBuilder()
                .artifactCorrelationId("patched1")
                .envIdentifier("env1")
                .build(),
            builderFactory.getCdInstanceSummaryBuilder()
                .artifactCorrelationId("pending1")
                .envIdentifier("env2")
                .build(),
            builderFactory.getCdInstanceSummaryBuilder().artifactCorrelationId("pending").envIdentifier("env1").build(),
            builderFactory.getCdInstanceSummaryBuilder().artifactCorrelationId("pending").build()));
    FieldUtils.writeField(remediationTrackerService, "artifactService", artifactService, true);
    FieldUtils.writeField(remediationTrackerService, "cdInstanceSummaryService", cdInstanceSummaryService, true);
    remediationTrackerService.updateArtifactsAndEnvironmentsInRemediationTracker(remediationTrackerEntity);
    remediationTrackerEntity = remediationTrackerService.getRemediationTracker(remediationTrackerEntity.getUuid());
    assertThat(remediationTrackerEntity.getArtifactInfos()).isNotNull();
    assertThat(remediationTrackerEntity.getArtifactInfos().size()).isEqualTo(2);
    assertThat(remediationTrackerEntity.getArtifactInfos().get("artifactId").getArtifactId()).isEqualTo("artifactId");
    assertThat(remediationTrackerEntity.getArtifactInfos().get("artifactId").getArtifactName()).isEqualTo("test/image");
    assertThat(remediationTrackerEntity.getArtifactInfos().get("artifactId").getEnvironments().size()).isEqualTo(6);

    assertThat(remediationTrackerEntity.getArtifactInfos().get("artifactId1").getArtifactId()).isEqualTo("artifactId1");
    assertThat(remediationTrackerEntity.getArtifactInfos().get("artifactId1").getArtifactName())
        .isEqualTo("test/image");
    assertThat(remediationTrackerEntity.getArtifactInfos().get("artifactId1").getEnvironments().size()).isEqualTo(1);
  }
}
