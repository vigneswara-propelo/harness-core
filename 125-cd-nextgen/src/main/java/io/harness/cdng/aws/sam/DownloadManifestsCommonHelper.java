/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.aws.sam;

import static io.harness.ci.commonconstants.CIExecutionConstants.GIT_CLONE_STEP_ID;

import static java.lang.String.format;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.steps.nodes.GitCloneStepNode;
import io.harness.beans.steps.stepinfo.GitCloneStepInfo;
import io.harness.cdng.containerStepGroup.DownloadAwsS3StepInfo;
import io.harness.cdng.containerStepGroup.DownloadAwsS3StepNode;
import io.harness.cdng.containerStepGroup.DownloadAwsS3StepParameters;
import io.harness.cdng.manifest.steps.outcome.ManifestsOutcome;
import io.harness.cdng.manifest.yaml.GitStoreConfig;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.manifest.yaml.S3StoreConfig;
import io.harness.cdng.pipeline.steps.CdAbstractStepNode;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.beans.storeconfig.FetchType;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidRequestException;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.data.OptionalOutcome;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.extended.ci.codebase.Build;
import io.harness.yaml.extended.ci.codebase.BuildType;
import io.harness.yaml.extended.ci.codebase.impl.BranchBuildSpec;
import io.harness.yaml.extended.ci.codebase.impl.CommitShaBuildSpec;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_ECS})
public class DownloadManifestsCommonHelper {
  @Inject private OutcomeService outcomeService;

  public ManifestsOutcome fetchManifestsOutcome(Ambiance ambiance) {
    OptionalOutcome manifestsOutcome = outcomeService.resolveOptional(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.MANIFESTS));

    if (!manifestsOutcome.isFound()) {
      String stageName =
          AmbianceUtils.getStageLevelFromAmbiance(ambiance).map(Level::getIdentifier).orElse("Deployment stage");
      String stepType =
          Optional.ofNullable(AmbianceUtils.getCurrentStepType(ambiance)).map(StepType::getType).orElse("This");
      throw new GeneralException(format(
          "No manifests found in stage %s. %s step requires at least one manifest defined in stage service definition",
          stageName, stepType));
    }
    return (ManifestsOutcome) manifestsOutcome.getOutcome();
  }

  public Ambiance buildAmbiance(Ambiance ambiance, String identifier) {
    Level level = Level.newBuilder()
                      .setIdentifier(identifier)
                      .setSkipExpressionChain(true)
                      .setSetupId(UUIDGenerator.generateUuid())
                      .setRuntimeId(UUIDGenerator.generateUuid())
                      .build();
    return AmbianceUtils.cloneForChild(ambiance, level);
  }

  public GitCloneStepInfo getGitCloneStepInfoFromManifestOutcome(ManifestOutcome gitManifestOutcome) {
    GitStoreConfig gitStoreConfig = (GitStoreConfig) gitManifestOutcome.getStore();

    Build build;

    if (FetchType.COMMIT.equals(gitStoreConfig.getGitFetchType())) {
      build =
          new Build(BuildType.COMMIT_SHA, CommitShaBuildSpec.builder().commitSha(gitStoreConfig.getCommitId()).build());
    } else if (FetchType.BRANCH.equals(gitStoreConfig.getGitFetchType())) {
      build = new Build(BuildType.BRANCH, BranchBuildSpec.builder().branch(gitStoreConfig.getBranch()).build());
    } else {
      throw new InvalidRequestException(format("%s git fetch type is not supported", gitStoreConfig.getGitFetchType()));
    }

    return GitCloneStepInfo.builder()
        .cloneDirectory(ParameterField.<String>builder().value(gitManifestOutcome.getIdentifier()).build())
        .identifier(gitManifestOutcome.getIdentifier())
        .name(gitManifestOutcome.getIdentifier())
        .connectorRef(gitStoreConfig.getConnectorRef())
        .repoName(gitStoreConfig.getRepoName())
        .build(ParameterField.<Build>builder().value(build).build())
        .build();
  }

  public GitCloneStepInfo getGitCloneStepInfoFromManifestOutcomeWithOutputFilePathContents(
      ManifestOutcome gitManifestOutcome, List<String> outputFilePathContent) {
    GitStoreConfig gitStoreConfig = (GitStoreConfig) gitManifestOutcome.getStore();

    Build build = new Build(BuildType.BRANCH, BranchBuildSpec.builder().branch(gitStoreConfig.getBranch()).build());

    return GitCloneStepInfo.builder()
        .cloneDirectory(ParameterField.<String>builder().value(gitManifestOutcome.getIdentifier()).build())
        .identifier(gitManifestOutcome.getIdentifier())
        .name(gitManifestOutcome.getIdentifier())
        .connectorRef(gitStoreConfig.getConnectorRef())
        .repoName(gitStoreConfig.getRepoName())
        .outputFilePathsContent(ParameterField.<List<String>>builder().value(outputFilePathContent).build())
        .build(ParameterField.<Build>builder().value(build).build())
        .build();
  }

  public StepElementParameters getGitStepElementParameters(
      ManifestOutcome gitManifestOutcome, GitCloneStepInfo gitCloneStepInfo) {
    return StepElementParameters.builder()
        .name(gitManifestOutcome.getIdentifier())
        .spec(gitCloneStepInfo)
        .identifier(getGitCloneStepIdentifier(gitManifestOutcome))
        .build();
  }

  public GitCloneStepNode getGitCloneStepNode(
      ManifestOutcome gitManifestOutcome, GitCloneStepInfo gitCloneStepInfo, CdAbstractStepNode cdAbstractStepNode) {
    return GitCloneStepNode.builder()
        .gitCloneStepInfo(gitCloneStepInfo)
        .failureStrategies(cdAbstractStepNode.getFailureStrategies())
        .timeout(cdAbstractStepNode.getTimeout())
        .type(GitCloneStepNode.StepType.GitClone)
        .identifier(GIT_CLONE_STEP_ID + gitManifestOutcome.getIdentifier())
        .name(gitManifestOutcome.getIdentifier())
        .uuid(gitManifestOutcome.getIdentifier())
        .build();
  }

  @NotNull
  public DownloadAwsS3StepNode getAwsS3StepNode(CdAbstractStepNode cdAbstractStepNode, ManifestOutcome manifestOutcome,
      DownloadAwsS3StepInfo downloadAwsS3StepInfo) {
    DownloadAwsS3StepNode downloadAwsS3StepNode = new DownloadAwsS3StepNode();
    downloadAwsS3StepNode.setDownloadAwsS3StepInfo(downloadAwsS3StepInfo);

    downloadAwsS3StepNode.setFailureStrategies(cdAbstractStepNode.getFailureStrategies());
    downloadAwsS3StepNode.setTimeout(cdAbstractStepNode.getTimeout());
    downloadAwsS3StepNode.setIdentifier(getDownloadS3StepIdentifier(manifestOutcome));
    downloadAwsS3StepNode.setName(manifestOutcome.getIdentifier());
    downloadAwsS3StepNode.setUuid(manifestOutcome.getIdentifier());
    return downloadAwsS3StepNode;
  }

  public DownloadAwsS3StepInfo getAwsS3StepInfo(ManifestOutcome manifestOutcome, S3StoreConfig s3StoreConfig) {
    return DownloadAwsS3StepInfo.infoBuilder()
        .connectorRef(s3StoreConfig.getConnectorRef())
        .bucketName(s3StoreConfig.getBucketName())
        .region(s3StoreConfig.getRegion())
        .paths(s3StoreConfig.getPaths())
        .downloadPath(ParameterField.createValueField("/harness/" + manifestOutcome.getIdentifier()))
        .build();
  }

  public DownloadAwsS3StepInfo getAwsS3StepInfoWithOutputFilePathContents(
      ManifestOutcome manifestOutcome, S3StoreConfig s3StoreConfig, String valuesPath) {
    return DownloadAwsS3StepInfo.infoBuilder()
        .connectorRef(s3StoreConfig.getConnectorRef())
        .bucketName(s3StoreConfig.getBucketName())
        .region(s3StoreConfig.getRegion())
        .paths(s3StoreConfig.getPaths())
        .downloadPath(ParameterField.createValueField("/harness/" + manifestOutcome.getIdentifier()))
        .outputFilePathsContent(ParameterField.createValueField(Collections.singletonList(valuesPath)))
        .build();
  }

  public String getGitCloneStepIdentifier(ManifestOutcome gitManifestOutcome) {
    return GIT_CLONE_STEP_ID + gitManifestOutcome.getIdentifier();
  }

  public StepElementParameters getDownloadS3StepElementParameters(
      ManifestOutcome manifestOutcome, DownloadAwsS3StepInfo downloadAwsS3StepInfo) {
    DownloadAwsS3StepParameters downloadAwsS3StepParameters =
        (DownloadAwsS3StepParameters) downloadAwsS3StepInfo.getSpecParameters();
    return StepElementParameters.builder()
        .name(manifestOutcome.getIdentifier())
        .identifier(manifestOutcome.getIdentifier())
        .spec(downloadAwsS3StepParameters)
        .timeout(ParameterField.createValueField("10m"))
        .build();
  }

  public String getDownloadS3StepIdentifier(ManifestOutcome manifestOutcome) {
    return manifestOutcome.getIdentifier();
  }
}
