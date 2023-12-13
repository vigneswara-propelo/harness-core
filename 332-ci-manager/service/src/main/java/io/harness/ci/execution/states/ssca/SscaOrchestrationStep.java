/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ci.execution.states.ssca;

import static io.harness.beans.steps.outcome.StepArtifacts.StepArtifactsBuilder;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.steps.outcome.StepArtifacts;
import io.harness.ci.execution.states.AbstractStepExecutable;
import io.harness.delegate.task.stepstatus.StepStatus;
import io.harness.delegate.task.stepstatus.artifact.ArtifactMetadata;
import io.harness.delegate.task.stepstatus.artifact.ArtifactMetadataType;
import io.harness.delegate.task.stepstatus.artifact.SscaArtifactMetadata;
import io.harness.delegate.task.stepstatus.artifact.ssca.DriftSummary;
import io.harness.delegate.task.stepstatus.artifact.ssca.Scorecard;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.spec.server.ssca.v1.model.OrchestrationDriftSummary;
import io.harness.spec.server.ssca.v1.model.OrchestrationSummaryResponse;
import io.harness.ssca.beans.SscaConstants;
import io.harness.ssca.client.SSCAServiceUtils;
import io.harness.ssca.client.beans.SBOMArtifactResponse;
import io.harness.ssca.execution.orchestration.outcome.PublishedSbomArtifact;

import com.google.inject.Inject;

@OwnedBy(HarnessTeam.SSCA)
public class SscaOrchestrationStep extends AbstractStepExecutable {
  public static final StepType STEP_TYPE = SscaConstants.SSCA_ORCHESTRATION_STEP_TYPE;
  @Inject private SSCAServiceUtils sscaServiceUtils;

  @Override
  protected void modifyStepStatus(Ambiance ambiance, StepStatus stepStatus, String stepIdentifier) {
    String stepExecutionId = AmbianceUtils.obtainCurrentRuntimeId(ambiance);

    if (sscaServiceUtils.isSSCAManagerEnabled()) {
      OrchestrationSummaryResponse stepExecutionResponse =
          sscaServiceUtils.getOrchestrationSummaryResponse(stepExecutionId, AmbianceUtils.getAccountId(ambiance),
              AmbianceUtils.getOrgIdentifier(ambiance), AmbianceUtils.getProjectIdentifier(ambiance));

      SscaArtifactMetadata sscaArtifactMetadata = SscaArtifactMetadata.builder()
                                                      .id(stepExecutionResponse.getArtifact().getId())
                                                      .imageName(stepExecutionResponse.getArtifact().getName())
                                                      .registryUrl(stepExecutionResponse.getArtifact().getRegistryUrl())
                                                      .registryType(stepExecutionResponse.getArtifact().getType())
                                                      .isSbomAttested(stepExecutionResponse.isIsAttested())
                                                      .sbomName(stepExecutionResponse.getSbom().getName())
                                                      .stepExecutionId(stepExecutionId)
                                                      .imageTag(stepExecutionResponse.getArtifact().getTag())
                                                      .build();

      if (stepExecutionResponse.getScorecardSummary() != null) {
        sscaArtifactMetadata.setScorecard(Scorecard.builder()
                                              .avgScore(stepExecutionResponse.getScorecardSummary().getAvgScore())
                                              .maxScore(stepExecutionResponse.getScorecardSummary().getMaxScore())
                                              .build());
      }

      if (stepExecutionResponse.getDriftSummary() != null) {
        sscaArtifactMetadata.setDrift(getDriftSummary(stepExecutionResponse.getDriftSummary()));
      }

      stepStatus.setArtifactMetadata(ArtifactMetadata.builder()
                                         .type(ArtifactMetadataType.SSCA_ARTIFACT_METADATA)
                                         .spec(sscaArtifactMetadata)
                                         .build());

    } else {
      SBOMArtifactResponse sbomArtifactResponse =
          sscaServiceUtils.getSbomArtifact(stepExecutionId, AmbianceUtils.getAccountId(ambiance),
              AmbianceUtils.getOrgIdentifier(ambiance), AmbianceUtils.getProjectIdentifier(ambiance));

      stepStatus.setArtifactMetadata(ArtifactMetadata.builder()
                                         .type(ArtifactMetadataType.SSCA_ARTIFACT_METADATA)
                                         .spec(SscaArtifactMetadata.builder()
                                                   .id(sbomArtifactResponse.getArtifact().getId())
                                                   .imageName(sbomArtifactResponse.getArtifact().getName())
                                                   .registryUrl(sbomArtifactResponse.getArtifact().getUrl())
                                                   .registryType(sbomArtifactResponse.getArtifact().getType())
                                                   .isSbomAttested(sbomArtifactResponse.getAttestation().isAttested())
                                                   .sbomName(sbomArtifactResponse.getSbom().getName())
                                                   .sbomUrl(sbomArtifactResponse.getSbom().getUrl())
                                                   .stepExecutionId(stepExecutionId)
                                                   .imageTag(sbomArtifactResponse.getArtifact().getTag())
                                                   .build())
                                         .build());
    }
  }

  @Override
  protected StepArtifacts handleArtifactForVm(
      ArtifactMetadata artifactMetadata, StepElementParameters stepParameters, Ambiance ambiance) {
    String stepExecutionId = AmbianceUtils.obtainCurrentRuntimeId(ambiance);
    if (sscaServiceUtils.isSSCAManagerEnabled()) {
      OrchestrationSummaryResponse stepExecutionResponse =
          sscaServiceUtils.getOrchestrationSummaryResponse(stepExecutionId, AmbianceUtils.getAccountId(ambiance),
              AmbianceUtils.getOrgIdentifier(ambiance), AmbianceUtils.getProjectIdentifier(ambiance));

      PublishedSbomArtifact publishedSbomArtifact = PublishedSbomArtifact.builder()
                                                        .id(stepExecutionResponse.getArtifact().getId())
                                                        .url(stepExecutionResponse.getArtifact().getRegistryUrl())
                                                        .imageName(stepExecutionResponse.getArtifact().getName())
                                                        .isSbomAttested(stepExecutionResponse.isIsAttested())
                                                        .sbomName(stepExecutionResponse.getSbom().getName())
                                                        .stepExecutionId(stepExecutionId)
                                                        .tag(stepExecutionResponse.getArtifact().getTag())
                                                        .build();

      if (stepExecutionResponse.getScorecardSummary() != null) {
        publishedSbomArtifact.setScorecard(Scorecard.builder()
                                               .avgScore(stepExecutionResponse.getScorecardSummary().getAvgScore())
                                               .maxScore(stepExecutionResponse.getScorecardSummary().getMaxScore())
                                               .build());
      }

      if (stepExecutionResponse.getDriftSummary() != null) {
        publishedSbomArtifact.setDrift(getDriftSummary(stepExecutionResponse.getDriftSummary()));
      }

      return StepArtifacts.builder().publishedSbomArtifact(publishedSbomArtifact).build();
    } else {
      SBOMArtifactResponse sbomArtifactResponse =
          sscaServiceUtils.getSbomArtifact(stepExecutionId, AmbianceUtils.getAccountId(ambiance),
              AmbianceUtils.getOrgIdentifier(ambiance), AmbianceUtils.getProjectIdentifier(ambiance));

      return StepArtifacts.builder()
          .publishedSbomArtifact(PublishedSbomArtifact.builder()
                                     .id(sbomArtifactResponse.getArtifact().getId())
                                     .url(sbomArtifactResponse.getArtifact().getUrl())
                                     .imageName(sbomArtifactResponse.getArtifact().getName())
                                     .isSbomAttested(sbomArtifactResponse.getAttestation().isAttested())
                                     .sbomName(sbomArtifactResponse.getSbom().getName())
                                     .sbomUrl(sbomArtifactResponse.getSbom().getUrl())
                                     .stepExecutionId(stepExecutionId)
                                     .tag(sbomArtifactResponse.getArtifact().getTag())
                                     .build())
          .build();
    }
  }

  @Override
  protected StepArtifacts handleArtifact(ArtifactMetadata artifactMetadata, StepElementParameters stepParameters) {
    StepArtifactsBuilder stepArtifactsBuilder = StepArtifacts.builder();
    if (artifactMetadata == null) {
      return stepArtifactsBuilder.build();
    }
    if (artifactMetadata.getType() == ArtifactMetadataType.SSCA_ARTIFACT_METADATA) {
      SscaArtifactMetadata sscaArtifactMetadata = (SscaArtifactMetadata) artifactMetadata.getSpec();

      if (sscaArtifactMetadata != null) {
        PublishedSbomArtifact publishedSbomArtifact = PublishedSbomArtifact.builder()
                                                          .id(sscaArtifactMetadata.getId())
                                                          .url(sscaArtifactMetadata.getRegistryUrl())
                                                          .digest(sscaArtifactMetadata.getDigest())
                                                          .imageName(sscaArtifactMetadata.getImageName())
                                                          .isSbomAttested(sscaArtifactMetadata.isSbomAttested())
                                                          .sbomName(sscaArtifactMetadata.getSbomName())
                                                          .sbomUrl(sscaArtifactMetadata.getSbomUrl())
                                                          .stepExecutionId(sscaArtifactMetadata.getStepExecutionId())
                                                          .tag(sscaArtifactMetadata.getImageTag())
                                                          .build();

        if (sscaArtifactMetadata.getScorecard() != null) {
          publishedSbomArtifact.setScorecard(Scorecard.builder()
                                                 .avgScore(sscaArtifactMetadata.getScorecard().getAvgScore())
                                                 .maxScore(sscaArtifactMetadata.getScorecard().getMaxScore())
                                                 .build());
        }

        if (sscaArtifactMetadata.getDrift() != null) {
          publishedSbomArtifact.setDrift(
              DriftSummary.builder()
                  .base(sscaArtifactMetadata.getDrift().getBase())
                  .driftId(sscaArtifactMetadata.getDrift().getDriftId())
                  .baseTag(sscaArtifactMetadata.getDrift().getBaseTag())
                  .totalDrifts(sscaArtifactMetadata.getDrift().getTotalDrifts())
                  .componentDrifts(sscaArtifactMetadata.getDrift().getComponentDrifts())
                  .licenseDrifts(sscaArtifactMetadata.getDrift().getLicenseDrifts())
                  .componentsAdded(sscaArtifactMetadata.getDrift().getComponentsAdded())
                  .componentsModified(sscaArtifactMetadata.getDrift().getComponentsModified())
                  .componentsDeleted(sscaArtifactMetadata.getDrift().getComponentsDeleted())
                  .licenseAdded(sscaArtifactMetadata.getDrift().getLicenseAdded())
                  .licenseDeleted(sscaArtifactMetadata.getDrift().getLicenseDeleted())
                  .build());
        }

        stepArtifactsBuilder.publishedSbomArtifact(publishedSbomArtifact);
      }
    }
    return stepArtifactsBuilder.build();
  }

  private DriftSummary getDriftSummary(OrchestrationDriftSummary driftSummary) {
    return DriftSummary.builder()
        .base(driftSummary.getBase())
        .driftId(driftSummary.getDriftId())
        .baseTag(driftSummary.getBaseTag())
        .totalDrifts(driftSummary.getTotalDrifts())
        .componentDrifts(driftSummary.getComponentDrifts())
        .licenseDrifts(driftSummary.getLicenseDrifts())
        .componentsAdded(driftSummary.getComponentsAdded())
        .componentsModified(driftSummary.getComponentsModified())
        .componentsDeleted(driftSummary.getComponentsDeleted())
        .licenseAdded(driftSummary.getLicenseAdded())
        .licenseDeleted(driftSummary.getLicenseDeleted())
        .build();
  }
}
