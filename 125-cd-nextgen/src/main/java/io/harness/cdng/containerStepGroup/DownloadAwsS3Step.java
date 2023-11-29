/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.containerStepGroup;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.callback.DelegateCallbackToken;
import io.harness.cdng.plugininfoproviders.PluginExecutionConfig;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.plugin.AbstractContainerStepV2;
import io.harness.pms.sdk.core.plugin.ContainerUnitStepUtils;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.product.ci.engine.proto.UnitStep;
import io.harness.tasks.ResponseData;
import io.harness.yaml.core.timeout.Timeout;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDP)
@Slf4j
public class DownloadAwsS3Step extends AbstractContainerStepV2<StepElementParameters> {
  @Inject private Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;

  @Inject private DownloadAwsS3StepHelper downloadAwsS3StepHelper;

  @Inject private ContainerStepGroupHelper containerStepGroupHelper;

  @Inject private PluginExecutionConfig pluginExecutionConfig;

  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(ExecutionNodeType.DOWNLOAD_AWS_S3.getYamlType())
                                               .setStepCategory(StepCategory.STEP)
                                               .build();

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }

  @Override
  public long getTimeout(Ambiance ambiance, StepElementParameters stepElementParameters) {
    return Timeout.fromString((String) stepElementParameters.getTimeout().fetchFinalValue()).getTimeoutInMillis();
  }

  @Override
  public UnitStep getSerialisedStep(Ambiance ambiance, StepElementParameters stepElementParameters, String accountId,
      String logKey, long timeout, String parkedTaskId) {
    DownloadAwsS3StepParameters downloadAwsS3StepParameters =
        (DownloadAwsS3StepParameters) stepElementParameters.getSpec();

    Map<String, String> envVars = downloadAwsS3StepHelper.getEnvironmentVariables(
        ambiance, downloadAwsS3StepParameters, stepElementParameters.getIdentifier());
    containerStepGroupHelper.removeAllEnvVarsWithSecretRef(envVars);
    containerStepGroupHelper.validateEnvVariables(envVars);

    String stepIdentifier = AmbianceUtils.obtainStepIdentifier(ambiance);
    String completeStepIdentifier = containerStepGroupHelper.getCompleteStepIdentifier(ambiance, stepIdentifier);

    return ContainerUnitStepUtils.serializeStepWithStepParameters(getPort(ambiance, stepIdentifier), parkedTaskId,
        logKey, completeStepIdentifier, getTimeout(ambiance, stepElementParameters), accountId,
        stepElementParameters.getName(), delegateCallbackTokenSupplier, ambiance, envVars,
        pluginExecutionConfig.getDownloadAwsS3Config().getImage(), Collections.EMPTY_LIST);
  }

  @Override
  public StepResponse.StepOutcome getAnyOutComeForStep(
      Ambiance ambiance, StepElementParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    return null;
  }

  @Override
  public void validateResources(Ambiance ambiance, StepElementParameters stepParameters) {
    // we need to check if rbac check is req or not.
  }
}
