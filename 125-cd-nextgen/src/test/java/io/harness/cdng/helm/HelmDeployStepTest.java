/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.helm;

import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.ACHYUTH;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.MLUKIC;
import static io.harness.rule.OwnerRule.PRATYUSH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.cdng.helm.beans.NativeHelmExecutionPassThroughData;
import io.harness.cdng.instance.info.InstanceInfoService;
import io.harness.cdng.instance.outcome.DeploymentInfoOutcome;
import io.harness.cdng.k8s.beans.GitFetchResponsePassThroughData;
import io.harness.cdng.k8s.beans.HelmValuesFetchResponsePassThroughData;
import io.harness.cdng.k8s.beans.StepExceptionPassThroughData;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.common.NGTimeConversionHelper;
import io.harness.delegate.beans.helm.HelmDeployProgressData;
import io.harness.delegate.beans.helm.HelmDeployProgressDataVersion;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.exception.HelmNGException;
import io.harness.delegate.task.helm.HelmChartInfo;
import io.harness.delegate.task.helm.HelmCmdExecResponseNG;
import io.harness.delegate.task.helm.HelmCommandRequestNG;
import io.harness.delegate.task.helm.HelmInstallCmdResponseNG;
import io.harness.delegate.task.helm.HelmInstallCommandRequestNG;
import io.harness.exception.GeneralException;
import io.harness.k8s.model.K8sPod;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.tasks.ProgressData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class HelmDeployStepTest extends AbstractHelmStepExecutorTestBase {
  @Mock ExecutionSweepingOutputService executionSweepingOutputService;
  @Mock private InstanceInfoService instanceInfoService;
  @InjectMocks private HelmDeployStep helmDeployStep;
  @Override
  protected NativeHelmStepExecutor getHelmStepExecutor() {
    return helmDeployStep;
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteHelmTask() {
    HelmDeployStepParams stepParameters = HelmDeployStepParams.infoBuilder().build();
    final StepElementParameters stepElementParameters =
        StepElementParameters.builder().spec(stepParameters).timeout(ParameterField.createValueField("30m")).build();

    HelmInstallCommandRequestNG request = executeTask(stepElementParameters, HelmInstallCommandRequestNG.class);
    assertThat(request.getK8sInfraDelegateConfig()).isEqualTo(infraDelegateConfig);
    assertThat(request.getManifestDelegateConfig()).isEqualTo(manifestDelegateConfig);
    assertThat(request.getAccountId()).isEqualTo(accountId);
    assertThat(request.getTimeoutInMillis()).isEqualTo(NGTimeConversionHelper.convertTimeStringToMilliseconds("30m"));
    assertThat(request.isUseSteadyStateCheckForJobs()).isFalse();

    ArgumentCaptor<String> releaseNameCaptor = ArgumentCaptor.forClass(String.class);
    verify(nativeHelmStepHelper, times(1)).publishReleaseNameStepDetails(eq(ambiance), releaseNameCaptor.capture());
    assertThat(releaseNameCaptor.getValue()).isEqualTo(releaseName);
  }

  @Test
  @Owner(developers = PRATYUSH)
  @Category(UnitTests.class)
  public void testExecuteHelmTaskWithAccountSettings() {
    HelmDeployStepParams stepParameters = HelmDeployStepParams.infoBuilder().build();
    final StepElementParameters stepElementParameters =
        StepElementParameters.builder().spec(stepParameters).timeout(ParameterField.createValueField("30m")).build();

    doReturn(true).when(nativeHelmStepHelper).isSteadyStateForJobsEnabled(ambiance);
    HelmInstallCommandRequestNG request = executeTask(stepElementParameters, HelmInstallCommandRequestNG.class);
    assertThat(request.getK8sInfraDelegateConfig()).isEqualTo(infraDelegateConfig);
    assertThat(request.getManifestDelegateConfig()).isEqualTo(manifestDelegateConfig);
    assertThat(request.getAccountId()).isEqualTo(accountId);
    assertThat(request.getTimeoutInMillis()).isEqualTo(NGTimeConversionHelper.convertTimeStringToMilliseconds("30m"));
    assertThat(request.isUseSteadyStateCheckForJobs()).isTrue();

    ArgumentCaptor<String> releaseNameCaptor = ArgumentCaptor.forClass(String.class);
    verify(nativeHelmStepHelper, times(1)).publishReleaseNameStepDetails(eq(ambiance), releaseNameCaptor.capture());
    assertThat(releaseNameCaptor.getValue()).isEqualTo(releaseName);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteK8sTaskNullParameterFields() {
    HelmDeployStepParams stepParameters = HelmDeployStepParams.infoBuilder().build();
    final StepElementParameters stepElementParameters =
        StepElementParameters.builder().spec(stepParameters).timeout(ParameterField.ofNull()).build();

    HelmInstallCommandRequestNG request = executeTask(stepElementParameters, HelmInstallCommandRequestNG.class);
    assertThat(request.getTimeoutInMillis()).isEqualTo(600000);
  }

  @SneakyThrows
  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testOutcomesInResponse() {
    HelmDeployStepParams stepParameters = HelmDeployStepParams.infoBuilder().build();
    final StepElementParameters stepElementParameters = StepElementParameters.builder().spec(stepParameters).build();
    HelmChartInfo helmChartInfo = HelmChartInfo.builder().name("todolist").version("0.2.0").build();

    HelmCmdExecResponseNG helmCmdExecResponseNG =
        HelmCmdExecResponseNG.builder()
            .helmCommandResponse(HelmInstallCmdResponseNG.builder()
                                     .containerInfoList(Collections.emptyList())
                                     .helmChartInfo(helmChartInfo)
                                     .releaseName("releaseName")
                                     .k8sPodList(List.of(K8sPod.builder().podIP("ip1").build(),
                                         K8sPod.builder().podIP("ip2").build(), K8sPod.builder().podIP("ip3").build()))
                                     .build())
            .commandUnitsProgress(UnitProgressData.builder().build())
            .commandExecutionStatus(SUCCESS)
            .build();
    when(cdStepHelper.getReleaseName(any(), any())).thenReturn("releaseName");
    StepOutcome stepOutcome = StepOutcome.builder()
                                  .name(OutcomeExpressionConstants.DEPLOYMENT_INFO_OUTCOME)
                                  .outcome(DeploymentInfoOutcome.builder().build())
                                  .build();
    doReturn(stepOutcome).when(instanceInfoService).saveDeploymentInfoOutcomeIntoSweepingOutput(any(), any());
    ReleaseHelmChartOutcome releaseHelmChartOutcome =
        ReleaseHelmChartOutcome.builder().name(helmChartInfo.getName()).version(helmChartInfo.getVersion()).build();
    doReturn(releaseHelmChartOutcome).when(nativeHelmStepHelper).getHelmChartOutcome(eq(helmChartInfo));

    StepResponse response = helmDeployStep.finalizeExecutionWithSecurityContextAndNodeInfo(ambiance,
        stepElementParameters, NativeHelmExecutionPassThroughData.builder().build(), () -> helmCmdExecResponseNG);
    assertThat(response.getStatus()).isEqualTo(Status.SUCCEEDED);
    assertThat(response.getStepOutcomes()).hasSize(3);

    StepOutcome outcome = response.getStepOutcomes().stream().collect(Collectors.toList()).get(0);
    assertThat(outcome.getOutcome()).isInstanceOf(DeploymentInfoOutcome.class);
    assertThat(outcome.getName()).isEqualTo(OutcomeExpressionConstants.DEPLOYMENT_INFO_OUTCOME);
    assertThat(outcome.getGroup()).isNull();

    StepOutcome deploymentInfoOutcome = new ArrayList<>(response.getStepOutcomes()).get(1);
    assertThat(deploymentInfoOutcome.getOutcome()).isInstanceOf(NativeHelmDeployOutcome.class);
    assertThat(deploymentInfoOutcome.getName()).isEqualTo(OutcomeExpressionConstants.HELM_DEPLOY_OUTCOME);
    ((NativeHelmDeployOutcome) deploymentInfoOutcome.getOutcome())
        .getPodIps()
        .forEach(ip -> assertThat(List.of("ip1", "ip2", "ip3").contains(ip)).isTrue());

    StepOutcome helmChartOutcome = new ArrayList<>(response.getStepOutcomes()).get(2);
    assertThat(helmChartOutcome.getOutcome()).isInstanceOf(ReleaseHelmChartOutcome.class);
    assertThat(helmChartOutcome.getName()).isEqualTo(OutcomeExpressionConstants.RELEASE_HELM_CHART_OUTCOME);
    assertThat(((ReleaseHelmChartOutcome) helmChartOutcome.getOutcome()).getName()).isEqualTo(helmChartInfo.getName());
    assertThat(((ReleaseHelmChartOutcome) helmChartOutcome.getOutcome()).getVersion())
        .isEqualTo(helmChartInfo.getVersion());

    ArgumentCaptor<NativeHelmDeployOutcome> argumentCaptor = ArgumentCaptor.forClass(NativeHelmDeployOutcome.class);
    verify(executionSweepingOutputService, times(1))
        .consume(eq(ambiance), eq(OutcomeExpressionConstants.HELM_DEPLOY_OUTCOME), argumentCaptor.capture(),
            eq(StepOutcomeGroup.STEP.name()));
    assertThat(argumentCaptor.getValue().getReleaseName()).isEqualTo("releaseName");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testFinalizeExecutionException() throws Exception {
    final StepElementParameters stepElementParameters = StepElementParameters.builder().build();
    final Exception thrownException = new HelmNGException(0, new GeneralException("Something went wrong"), false);
    final NativeHelmExecutionPassThroughData executionPassThroughData =
        NativeHelmExecutionPassThroughData.builder().build();
    final StepResponse stepResponse = StepResponse.builder().status(Status.FAILED).build();

    doReturn(stepResponse)
        .when(nativeHelmStepHelper)
        .handleTaskException(ambiance, executionPassThroughData, thrownException);

    StepResponse response = helmDeployStep.finalizeExecutionWithSecurityContextAndNodeInfo(
        ambiance, stepElementParameters, executionPassThroughData, () -> { throw thrownException; });

    assertThat(response).isEqualTo(stepResponse);

    verify(nativeHelmStepHelper, times(1)).handleTaskException(ambiance, executionPassThroughData, thrownException);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testDontSaveReleaseOutputIfQueueTaskFails() {
    final HelmDeployStepParams stepParameters = HelmDeployStepParams.infoBuilder().build();
    final StepElementParameters stepElementParameters =
        StepElementParameters.builder().spec(stepParameters).timeout(ParameterField.createValueField("30m")).build();
    final RuntimeException thrownException = new RuntimeException("test");

    doThrow(thrownException)
        .when(nativeHelmStepHelper)
        .queueNativeHelmTask(eq(stepElementParameters), any(HelmCommandRequestNG.class), eq(ambiance),
            any(NativeHelmExecutionPassThroughData.class));

    assertThatThrownBy(() -> executeTask(stepElementParameters, HelmInstallCommandRequestNG.class))
        .isSameAs(thrownException);

    verify(executionSweepingOutputService, never())
        .consume(eq(ambiance), eq(OutcomeExpressionConstants.HELM_DEPLOY_OUTCOME), any(NativeHelmDeployOutcome.class),
            eq(StepOutcomeGroup.STAGE.name()));
  }

  @Test
  @Owner(developers = ACHYUTH)
  @Category(UnitTests.class)
  public void testFinalizeExecutionWithSecurityContext() throws Exception {
    final HelmDeployStepParams stepParameters = HelmDeployStepParams.infoBuilder().build();
    final StepElementParameters stepElementParameters = StepElementParameters.builder().spec(stepParameters).build();

    HelmCmdExecResponseNG helmCmdExecResponseNG =
        HelmCmdExecResponseNG.builder()
            .helmCommandResponse(HelmInstallCmdResponseNG.builder()
                                     .containerInfoList(Collections.emptyList())
                                     .releaseName("releaseName")
                                     .build())
            .commandUnitsProgress(UnitProgressData.builder().build())
            .commandExecutionStatus(SUCCESS)
            .build();
    when(cdStepHelper.getReleaseName(any(), any())).thenReturn("releaseName");
    StepResponse.StepOutcome stepOutcome = StepResponse.StepOutcome.builder()
                                               .name(OutcomeExpressionConstants.DEPLOYMENT_INFO_OUTCOME)
                                               .outcome(DeploymentInfoOutcome.builder().build())
                                               .build();
    doReturn(stepOutcome).when(instanceInfoService).saveServerInstancesIntoSweepingOutput(any(), any());
    when(cdStepHelper.handleGitTaskFailure(any())).thenReturn(StepResponse.builder().status(Status.SUCCEEDED).build());
    StepResponse response = helmDeployStep.finalizeExecutionWithSecurityContextAndNodeInfo(ambiance,
        stepElementParameters, GitFetchResponsePassThroughData.builder().build(), () -> helmCmdExecResponseNG);
    assertThat(response.getStatus()).isEqualTo(Status.SUCCEEDED);

    when(nativeHelmStepHelper.handleHelmValuesFetchFailure(any()))
        .thenReturn(StepResponse.builder().status(Status.SUCCEEDED).build());
    assertThat(helmDeployStep
                   .finalizeExecutionWithSecurityContextAndNodeInfo(ambiance, stepElementParameters,
                       HelmValuesFetchResponsePassThroughData.builder().build(), () -> helmCmdExecResponseNG)
                   .getStatus())
        .isEqualTo(Status.SUCCEEDED);

    when(cdStepHelper.handleStepExceptionFailure(any()))
        .thenReturn(StepResponse.builder().status(Status.SUCCEEDED).build());
    assertThat(helmDeployStep
                   .finalizeExecutionWithSecurityContextAndNodeInfo(ambiance, stepElementParameters,
                       StepExceptionPassThroughData.builder().build(), () -> helmCmdExecResponseNG)
                   .getStatus())
        .isEqualTo(Status.SUCCEEDED);

    HelmCmdExecResponseNG helmCmdExecResponseNGFail =
        HelmCmdExecResponseNG.builder()
            .helmCommandResponse(HelmInstallCmdResponseNG.builder()
                                     .containerInfoList(Collections.emptyList())
                                     .releaseName("releaseName")
                                     .build())
            .commandUnitsProgress(UnitProgressData.builder().build())
            .commandExecutionStatus(FAILURE)
            .build();
    assertThat(helmDeployStep
                   .finalizeExecutionWithSecurityContextAndNodeInfo(ambiance, stepElementParameters,
                       NativeHelmExecutionPassThroughData.builder().build(), () -> helmCmdExecResponseNGFail)
                   .getStatus())
        .isEqualTo(Status.FAILED);
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testHandleProgressWIthHelmDeployProgressDataWithFFEnabled() {
    final HelmDeployStepParams stepParameters = HelmDeployStepParams.infoBuilder().build();
    final StepElementParameters stepElementParameters = StepElementParameters.builder().spec(stepParameters).build();
    ProgressData progressData =
        HelmDeployProgressData.builder().progressDataVersion(HelmDeployProgressDataVersion.V1.getVersionName()).build();

    ProgressData result = helmDeployStep.handleProgressTaskChain(ambiance, stepElementParameters, progressData);

    verify(executionSweepingOutputService, times(1))
        .consume(eq(ambiance), eq(OutcomeExpressionConstants.HELM_DEPLOY_RELEASE_OUTCOME),
            any(NativeHelmDeployOutcome.class), eq(StepOutcomeGroup.STEP.name()));
    assertThat(result).isNull();
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testHandleProgressWithUnitProgressDataWhenFFEnabled() {
    final HelmDeployStepParams stepParameters = HelmDeployStepParams.infoBuilder().build();
    final StepElementParameters stepElementParameters = StepElementParameters.builder().spec(stepParameters).build();
    ProgressData progressData = UnitProgressData.builder().build();

    helmDeployStep.handleProgressTaskChain(ambiance, stepElementParameters, progressData);

    verify(executionSweepingOutputService, times(0)).consume(any(), any(), any(), any());
  }
}
