/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.aws.sam;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.FeatureName;
import io.harness.beans.steps.nodes.GitCloneStepNode;
import io.harness.beans.steps.stepinfo.GitCloneStepInfo;
import io.harness.cdng.aws.sam.beans.AwsSamValuesYamlDataOutcome;
import io.harness.cdng.aws.sam.beans.AwsSamValuesYamlDataOutcome.AwsSamValuesYamlDataOutcomeBuilder;
import io.harness.cdng.containerStepGroup.DownloadAwsS3Step;
import io.harness.cdng.containerStepGroup.DownloadAwsS3StepInfo;
import io.harness.cdng.containerStepGroup.DownloadAwsS3StepNode;
import io.harness.cdng.featureFlag.CDFeatureFlagHelper;
import io.harness.cdng.manifest.steps.outcome.ManifestsOutcome;
import io.harness.cdng.manifest.yaml.AwsSamDirectoryManifestOutcome;
import io.harness.cdng.manifest.yaml.S3StoreConfig;
import io.harness.cdng.manifest.yaml.ValuesManifestOutcome;
import io.harness.cdng.pipeline.steps.CdAbstractStepNode;
import io.harness.cdng.plugininfoproviders.DownloadAwsS3PluginInfoProvider;
import io.harness.cdng.plugininfoproviders.GitClonePluginInfoProvider;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.task.stepstatus.StepExecutionStatus;
import io.harness.delegate.task.stepstatus.StepMapOutput;
import io.harness.delegate.task.stepstatus.StepOutput;
import io.harness.delegate.task.stepstatus.StepStatusTaskResponseData;
import io.harness.eraro.ErrorCode;
import io.harness.exception.AccessDeniedException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plugin.GitCloneStep;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.AsyncExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.plan.PluginCreationRequest;
import io.harness.pms.contracts.plan.PluginCreationResponseList;
import io.harness.pms.contracts.plan.PluginCreationResponseWrapper;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.expression.EngineExpressionService;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.plugin.ContainerPluginParseException;
import io.harness.pms.sdk.core.plugin.ContainerStepExecutionResponseHelper;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlUtils;
import io.harness.tasks.ResponseData;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_ECS})
@Slf4j
public class AwsSamDownloadManifestsStepHelper {
  @Inject private OutcomeService outcomeService;

  @Inject private ExecutionSweepingOutputService executionSweepingOutputService;

  @Inject private EngineExpressionService engineExpressionService;

  @Inject private GitClonePluginInfoProvider gitClonePluginInfoProvider;

  @Inject private DownloadManifestsCommonHelper downloadManifestsCommonHelper;

  @Inject private AwsSamStepHelper awsSamStepHelper;

  @Inject private DownloadAwsS3PluginInfoProvider downloadAwsS3PluginInfoProvider;

  @Inject private DownloadAwsS3Step downloadAwsS3Step;

  @Inject private CDFeatureFlagHelper cdFeatureFlagHelper;
  @Inject private ContainerStepExecutionResponseHelper containerStepExecutionResponseHelper;

  public AsyncExecutableResponse executeAsyncAfterRbac(
      Ambiance ambiance, StepInputPackage inputPackage, GitCloneStep gitCloneStep) {
    ManifestsOutcome manifestsOutcome = downloadManifestsCommonHelper.fetchManifestsOutcome(ambiance);
    // @todo(hinger) render manifests here
    AsyncExecutableResponse samDirectoryAsyncExecutableResponse =
        getAsyncExecutableResponseForAwsSamDirectoryManifest(ambiance, inputPackage, gitCloneStep, manifestsOutcome);

    List<String> callbackIds = new ArrayList<>(samDirectoryAsyncExecutableResponse.getCallbackIdsList());
    List<String> logKeys = new ArrayList<>(samDirectoryAsyncExecutableResponse.getLogKeysList());

    ValuesManifestOutcome valuesManifestOutcome =
        (ValuesManifestOutcome) awsSamStepHelper.getAwsSamValuesManifestOutcome(manifestsOutcome.values());

    if (valuesManifestOutcome != null) {
      AsyncExecutableResponse valuesAsyncExecutableResponse =
          getAsyncExecutableResponseForValuesManifest(ambiance, inputPackage, gitCloneStep, valuesManifestOutcome);
      callbackIds.addAll(valuesAsyncExecutableResponse.getCallbackIdsList());
      logKeys.addAll(valuesAsyncExecutableResponse.getLogKeysList());
    }

    return AsyncExecutableResponse.newBuilder()
        .addAllCallbackIds(callbackIds)
        .setStatus(samDirectoryAsyncExecutableResponse.getStatus())
        .addAllLogKeys(logKeys)
        .build();
  }

  private AsyncExecutableResponse getAsyncExecutableResponseForValuesManifest(Ambiance ambiance,
      StepInputPackage inputPackage, GitCloneStep gitCloneStep, ValuesManifestOutcome valuesManifestOutcome) {
    if (valuesManifestOutcome.getStore() instanceof S3StoreConfig) {
      checkForS3DownloadFeatureFlag(ambiance);

      S3StoreConfig s3StoreConfig = (S3StoreConfig) valuesManifestOutcome.getStore();
      DownloadAwsS3StepInfo downloadAwsS3StepInfo =
          downloadManifestsCommonHelper.getAwsS3StepInfoWithOutputFilePathContents(valuesManifestOutcome, s3StoreConfig,
              awsSamStepHelper.getValuesPathFromValuesManifestOutcome(valuesManifestOutcome));

      StepElementParameters stepElementParameters = downloadManifestsCommonHelper.getDownloadS3StepElementParameters(
          valuesManifestOutcome, downloadAwsS3StepInfo);
      Ambiance ambianceForServerlessAwsLambdaManifest = downloadManifestsCommonHelper.buildAmbiance(
          ambiance, downloadManifestsCommonHelper.getDownloadS3StepIdentifier(valuesManifestOutcome));
      return downloadAwsS3Step.executeAsyncAfterRbac(
          ambianceForServerlessAwsLambdaManifest, stepElementParameters, inputPackage);
    }

    GitCloneStepInfo valuesGitCloneStepInfo =
        downloadManifestsCommonHelper.getGitCloneStepInfoFromManifestOutcome(valuesManifestOutcome);

    StepElementParameters valuesStepElementParameters =
        downloadManifestsCommonHelper.getGitStepElementParameters(valuesManifestOutcome, valuesGitCloneStepInfo);

    Ambiance ambianceForValuesManifest = downloadManifestsCommonHelper.buildAmbiance(
        ambiance, downloadManifestsCommonHelper.getGitCloneStepIdentifier(valuesManifestOutcome));

    return gitCloneStep.executeAsyncAfterRbac(ambianceForValuesManifest, valuesStepElementParameters, inputPackage);
  }

  private void checkForS3DownloadFeatureFlag(Ambiance ambiance) {
    if (!cdFeatureFlagHelper.isEnabled(
            AmbianceUtils.getAccountId(ambiance), FeatureName.CDS_CONTAINER_STEP_GROUP_AWS_S3_DOWNLOAD)) {
      throw new AccessDeniedException(
          "CDS_CONTAINER_STEP_GROUP_AWS_S3_DOWNLOAD FF is not enabled for this account. Please contact harness customer care.",
          ErrorCode.NG_ACCESS_DENIED, WingsException.USER);
    }
  }

  private AsyncExecutableResponse getAsyncExecutableResponseForAwsSamDirectoryManifest(
      Ambiance ambiance, StepInputPackage inputPackage, GitCloneStep gitCloneStep, ManifestsOutcome manifestsOutcome) {
    AwsSamDirectoryManifestOutcome awsSamDirectoryManifestOutcome =
        (AwsSamDirectoryManifestOutcome) awsSamStepHelper.getAwsSamDirectoryManifestOutcome(manifestsOutcome.values());

    if (awsSamDirectoryManifestOutcome.getStore() instanceof S3StoreConfig) {
      checkForS3DownloadFeatureFlag(ambiance);

      S3StoreConfig s3StoreConfig = (S3StoreConfig) awsSamDirectoryManifestOutcome.getStore();
      DownloadAwsS3StepInfo downloadAwsS3StepInfo =
          downloadManifestsCommonHelper.getAwsS3StepInfo(awsSamDirectoryManifestOutcome, s3StoreConfig);

      StepElementParameters stepElementParameters = downloadManifestsCommonHelper.getDownloadS3StepElementParameters(
          awsSamDirectoryManifestOutcome, downloadAwsS3StepInfo);
      Ambiance ambianceForServerlessAwsLambdaManifest = downloadManifestsCommonHelper.buildAmbiance(
          ambiance, downloadManifestsCommonHelper.getDownloadS3StepIdentifier(awsSamDirectoryManifestOutcome));
      return downloadAwsS3Step.executeAsyncAfterRbac(
          ambianceForServerlessAwsLambdaManifest, stepElementParameters, inputPackage);
    }

    GitCloneStepInfo gitCloneStepInfo =
        downloadManifestsCommonHelper.getGitCloneStepInfoFromManifestOutcome(awsSamDirectoryManifestOutcome);

    StepElementParameters stepElementParameters =
        downloadManifestsCommonHelper.getGitStepElementParameters(awsSamDirectoryManifestOutcome, gitCloneStepInfo);

    Ambiance ambianceForAwsSamDirectoryManifest = downloadManifestsCommonHelper.buildAmbiance(
        ambiance, downloadManifestsCommonHelper.getGitCloneStepIdentifier(awsSamDirectoryManifestOutcome));
    return gitCloneStep.executeAsyncAfterRbac(ambianceForAwsSamDirectoryManifest, stepElementParameters, inputPackage);
  }

  public StepResponse handleAsyncResponse(Ambiance ambiance, Map<String, ResponseData> responseDataMap) {
    ManifestsOutcome manifestsOutcome =
        (ManifestsOutcome) outcomeService
            .resolveOptional(ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.MANIFESTS))
            .getOutcome();
    handleResponseForValuesManifest(ambiance, responseDataMap, manifestsOutcome);
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }

  private void handleResponseForValuesManifest(
      Ambiance ambiance, Map<String, ResponseData> responseDataMap, ManifestsOutcome manifestsOutcome) {
    ValuesManifestOutcome valuesManifestOutcome =
        (ValuesManifestOutcome) awsSamStepHelper.getAwsSamValuesManifestOutcome(manifestsOutcome.values());

    if (valuesManifestOutcome != null) {
      AwsSamValuesYamlDataOutcomeBuilder awsSamValuesYamlDataOutcomeBuilder = AwsSamValuesYamlDataOutcome.builder();

      containerStepExecutionResponseHelper.deserializeResponse(responseDataMap);

      for (Map.Entry<String, ResponseData> entry : responseDataMap.entrySet()) {
        ResponseData responseData = entry.getValue();
        if (responseData instanceof StepStatusTaskResponseData) {
          StepStatusTaskResponseData stepStatusTaskResponseData = (StepStatusTaskResponseData) responseData;
          if (StepExecutionStatus.SUCCESS == stepStatusTaskResponseData.getStepStatus().getStepExecutionStatus()) {
            StepOutput stepOutput = stepStatusTaskResponseData.getStepStatus().getOutput();

            renderValuesManifestAndSaveToSweepingOutput(
                ambiance, valuesManifestOutcome, awsSamValuesYamlDataOutcomeBuilder, stepOutput);
          }
        }
      }
    }
  }

  private void renderValuesManifestAndSaveToSweepingOutput(Ambiance ambiance,
      ValuesManifestOutcome valuesManifestOutcome,
      AwsSamValuesYamlDataOutcomeBuilder awsSamValuesYamlDataOutcomeBuilder, StepOutput stepOutput) {
    if (stepOutput instanceof StepMapOutput) {
      StepMapOutput stepMapOutput = (StepMapOutput) stepOutput;
      if (EmptyPredicate.isNotEmpty(stepMapOutput.getMap())) {
        String valuesYamlPath = awsSamStepHelper.getValuesPathFromValuesManifestOutcome(valuesManifestOutcome);
        String valuesYamlContent = getValuesManifestContent(stepMapOutput, valuesYamlPath);

        if (!isEmpty(valuesYamlPath) && !isEmpty(valuesYamlContent)) {
          valuesYamlContent = engineExpressionService.renderExpression(ambiance, valuesYamlContent);
          awsSamValuesYamlDataOutcomeBuilder.valuesYamlPath(valuesYamlPath);
          awsSamValuesYamlDataOutcomeBuilder.valuesYamlContent(valuesYamlContent);
          executionSweepingOutputService.consume(ambiance, OutcomeExpressionConstants.AWS_SAM_VALUES_YAML_DATA_OUTCOME,
              awsSamValuesYamlDataOutcomeBuilder.build(), StepOutcomeGroup.STEP.name());
        }
      }
    }
  }

  @VisibleForTesting
  String getValuesManifestContent(StepMapOutput stepMapOutput, String valuesYamlPath) {
    String valuesYamlContentBase64 = stepMapOutput.getMap().get(valuesYamlPath);
    if (isEmpty(valuesYamlContentBase64)) {
      return EMPTY;
    }

    // fixing yaml base64 content because github.com/joho/godotenv.Read() can't parse == while fetching env variables
    String fixedValuesYamlContentBase64 = valuesYamlContentBase64.replace("-", "=").replace(SPACE, EMPTY);
    try {
      return new String(Base64.getDecoder().decode(fixedValuesYamlContentBase64));
    } catch (Exception ex) {
      throw new InvalidRequestException(format("Unable to fetch values YAML, valuesYamlPath: %s", valuesYamlPath), ex);
    }
  }

  public PluginCreationResponseList getPluginInfoList(
      PluginCreationRequest request, Set<Integer> usedPorts, Ambiance ambiance) {
    String stepJsonNode = request.getStepJsonNode();
    CdAbstractStepNode cdAbstractStepNode;
    try {
      cdAbstractStepNode = YamlUtils.read(stepJsonNode, CdAbstractStepNode.class);
    } catch (IOException e) {
      throw new ContainerPluginParseException(
          format("Error in parsing CD step for step type [%s]", request.getType()), e);
    }

    List<PluginCreationResponseWrapper> pluginCreationResponseWrapperList = new ArrayList<>();

    ManifestsOutcome manifestsOutcome =
        (ManifestsOutcome) outcomeService
            .resolveOptional(ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.MANIFESTS))
            .getOutcome();

    PluginCreationResponseWrapper pluginCreationResponseWrapper =
        getPluginCreationResponseWrapperForAwsSamDirectoryManifest(
            request, usedPorts, ambiance, cdAbstractStepNode, manifestsOutcome);

    usedPorts.addAll(pluginCreationResponseWrapper.getResponse().getPluginDetails().getPortUsedList());

    pluginCreationResponseWrapperList.add(pluginCreationResponseWrapper);

    // Values Yaml

    ValuesManifestOutcome valuesManifestOutcome =
        (ValuesManifestOutcome) awsSamStepHelper.getAwsSamValuesManifestOutcome(manifestsOutcome.values());

    if (valuesManifestOutcome != null) {
      PluginCreationResponseWrapper valuesPluginCreationResponseWrapper =
          getPluginCreationResponseWrapperForValuesManifest(
              request, usedPorts, ambiance, cdAbstractStepNode, valuesManifestOutcome);
      pluginCreationResponseWrapperList.add(valuesPluginCreationResponseWrapper);
    }
    return PluginCreationResponseList.newBuilder().addAllResponse(pluginCreationResponseWrapperList).build();
  }

  private PluginCreationResponseWrapper getPluginCreationResponseWrapperForValuesManifest(PluginCreationRequest request,
      Set<Integer> usedPorts, Ambiance ambiance, CdAbstractStepNode cdAbstractStepNode,
      ValuesManifestOutcome valuesManifestOutcome) {
    if (valuesManifestOutcome.getStore() instanceof S3StoreConfig) {
      checkForS3DownloadFeatureFlag(ambiance);

      S3StoreConfig s3StoreConfig = (S3StoreConfig) valuesManifestOutcome.getStore();
      DownloadAwsS3StepInfo downloadAwsS3StepInfo =
          downloadManifestsCommonHelper.getAwsS3StepInfoWithOutputFilePathContents(valuesManifestOutcome, s3StoreConfig,
              awsSamStepHelper.getValuesPathFromValuesManifestOutcome(valuesManifestOutcome));

      DownloadAwsS3StepNode downloadAwsS3StepNode = downloadManifestsCommonHelper.getAwsS3StepNode(
          cdAbstractStepNode, valuesManifestOutcome, downloadAwsS3StepInfo);

      PluginCreationRequest pluginCreationRequest =
          request.toBuilder().setStepJsonNode(YamlUtils.writeYamlString(downloadAwsS3StepNode)).build();
      return downloadAwsS3PluginInfoProvider.getPluginInfo(pluginCreationRequest, usedPorts, ambiance);
    }

    GitCloneStepInfo valuesGitCloneStepInfo =
        downloadManifestsCommonHelper.getGitCloneStepInfoFromManifestOutcome(valuesManifestOutcome);

    String valuesYamlPath = awsSamStepHelper.getValuesPathFromValuesManifestOutcome(valuesManifestOutcome);

    valuesGitCloneStepInfo.setOutputFilePathsContent(
        ParameterField.<List<String>>builder().value(Collections.singletonList(valuesYamlPath)).build());

    GitCloneStepNode valuesGitCloneStepNode = downloadManifestsCommonHelper.getGitCloneStepNode(
        valuesManifestOutcome, valuesGitCloneStepInfo, cdAbstractStepNode);

    PluginCreationRequest valuesPluginCreationRequest =
        request.toBuilder().setStepJsonNode(YamlUtils.writeYamlString(valuesGitCloneStepNode)).build();

    return gitClonePluginInfoProvider.getPluginInfo(valuesPluginCreationRequest, usedPorts, ambiance);
  }

  private PluginCreationResponseWrapper getPluginCreationResponseWrapperForAwsSamDirectoryManifest(
      PluginCreationRequest request, Set<Integer> usedPorts, Ambiance ambiance, CdAbstractStepNode cdAbstractStepNode,
      ManifestsOutcome manifestsOutcome) {
    AwsSamDirectoryManifestOutcome awsSamDirectoryManifestOutcome =
        (AwsSamDirectoryManifestOutcome) awsSamStepHelper.getAwsSamDirectoryManifestOutcome(manifestsOutcome.values());

    if (awsSamDirectoryManifestOutcome.getStore() instanceof S3StoreConfig) {
      checkForS3DownloadFeatureFlag(ambiance);

      S3StoreConfig s3StoreConfig = (S3StoreConfig) awsSamDirectoryManifestOutcome.getStore();
      DownloadAwsS3StepInfo downloadAwsS3StepInfo =
          downloadManifestsCommonHelper.getAwsS3StepInfo(awsSamDirectoryManifestOutcome, s3StoreConfig);

      DownloadAwsS3StepNode downloadAwsS3StepNode = downloadManifestsCommonHelper.getAwsS3StepNode(
          cdAbstractStepNode, awsSamDirectoryManifestOutcome, downloadAwsS3StepInfo);

      PluginCreationRequest pluginCreationRequest =
          request.toBuilder().setStepJsonNode(YamlUtils.writeYamlString(downloadAwsS3StepNode)).build();
      return downloadAwsS3PluginInfoProvider.getPluginInfo(pluginCreationRequest, usedPorts, ambiance);
    }

    GitCloneStepInfo gitCloneStepInfo =
        downloadManifestsCommonHelper.getGitCloneStepInfoFromManifestOutcome(awsSamDirectoryManifestOutcome);

    GitCloneStepNode gitCloneStepNode = downloadManifestsCommonHelper.getGitCloneStepNode(
        awsSamDirectoryManifestOutcome, gitCloneStepInfo, cdAbstractStepNode);

    PluginCreationRequest pluginCreationRequest =
        request.toBuilder().setStepJsonNode(YamlUtils.writeYamlString(gitCloneStepNode)).build();

    return gitClonePluginInfoProvider.getPluginInfo(pluginCreationRequest, usedPorts, ambiance);
  }
}
