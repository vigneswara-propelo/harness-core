/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.k8s.asyncsteps;

import static io.harness.rule.OwnerRule.MLUKIC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.executables.AsyncExecutableTaskHelper;
import io.harness.cdng.featureFlag.CDFeatureFlagHelper;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.k8s.K8sStepHelper;
import io.harness.cdng.k8s.K8sTrafficRoutingStepParameters;
import io.harness.cdng.k8s.trafficrouting.AbstractK8sTrafficRouting;
import io.harness.cdng.k8s.trafficrouting.ConfigK8sTrafficRouting;
import io.harness.cdng.k8s.trafficrouting.K8sTrafficRoutingDestination;
import io.harness.cdng.k8s.trafficrouting.K8sTrafficRoutingHelper;
import io.harness.cdng.k8s.trafficrouting.K8sTrafficRoutingRoute;
import io.harness.cdng.k8s.trafficrouting.TrafficRoutingIstioProvider;
import io.harness.delegate.AccountId;
import io.harness.delegate.TaskId;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.exception.TaskNGDataException;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.trafficrouting.IstioProviderConfig;
import io.harness.delegate.task.k8s.trafficrouting.K8sTrafficRoutingConfig;
import io.harness.delegate.task.k8s.trafficrouting.K8sTrafficRoutingConfigType;
import io.harness.delegate.task.k8s.trafficrouting.RouteType;
import io.harness.delegate.task.k8s.trafficrouting.TrafficRoute;
import io.harness.delegate.task.k8s.trafficrouting.TrafficRoutingDestination;
import io.harness.exception.GeneralException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.UnitProgress;
import io.harness.logging.UnitStatus;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.AsyncExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.sdk.core.execution.invokers.StrategyHelper;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.service.DelegateGrpcClientWrapper;
import io.harness.supplier.ThrowingSupplier;

import software.wings.beans.TaskType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@OwnedBy(HarnessTeam.CDP)
public class K8sTrafficRoutingStepTest {
  @Mock ExecutionSweepingOutputService executionSweepingOutputService;
  @InjectMocks private K8sTrafficRoutingStep k8sTrafficRoutingStep;
  @Mock private CDFeatureFlagHelper cdFeatureFlagHelper;
  @Mock private CDStepHelper cdStepHelper;
  @Mock K8sTrafficRoutingHelper k8sTrafficRoutingHelper;
  @Mock protected InfrastructureOutcome infrastructureOutcome;
  @Mock private K8sStepHelper k8sStepHelper;
  @Mock private AsyncExecutableTaskHelper asyncExecutableTaskHelper;
  @Mock private DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  protected final String releaseName = "releaseName";

  private final String accountId = "accountId";
  private final Ambiance ambiance = Ambiance.newBuilder().putSetupAbstractions("accountId", accountId).build();
  private final StepElementParameters stepElementParameters = StepElementParameters.builder().build();

  @Before
  public void prepare() {
    openMocks(this);
    doReturn(releaseName).when(cdStepHelper).getReleaseName(ambiance, infrastructureOutcome);
    doReturn(infrastructureOutcome).when(cdStepHelper).getInfrastructureOutcome(ambiance);
    doNothing().when(k8sStepHelper).publishReleaseNameStepDetails(any(), any());
    doReturn(TaskType.K8S_COMMAND_TASK_NG_TRAFFIC_ROUTING).when(k8sStepHelper).getK8sTaskType(any(), any());
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testExecuteAsyncAfterRbacWithoutFF() {
    final StepElementParameters stepElementParameters = StepElementParameters.builder()
                                                            .spec(new K8sTrafficRoutingStepParameters())
                                                            .timeout(ParameterField.createValueField("30m"))
                                                            .build();
    assertThatThrownBy(() -> k8sTrafficRoutingStep.executeAsyncAfterRbac(ambiance, stepElementParameters, null))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testExecuteAsyncAfterRbac() {
    when(cdFeatureFlagHelper.isEnabled(any(), any())).thenReturn(true);

    Map<String, Integer> dests = new HashMap<>();
    dests.put("svc1", 80);
    dests.put("svc2", 20);

    List<K8sTrafficRoutingDestination> destinations = getDestinations(dests);

    ConfigK8sTrafficRouting k8sTrafficRouting =
        ConfigK8sTrafficRouting.builder()
            .provider(AbstractK8sTrafficRouting.ProviderType.ISTIO)
            .spec(TrafficRoutingIstioProvider.builder()
                      .routes(Collections.singletonList(
                          K8sTrafficRoutingRoute.builder()
                              .route(K8sTrafficRoutingRoute.RouteSpec.builder()
                                         .type(K8sTrafficRoutingRoute.RouteSpec.RouteType.HTTP)
                                         .build())
                              .build()))
                      .destinations(destinations)
                      .build())
            .build();

    K8sTrafficRoutingStepParameters stepParameters = new K8sTrafficRoutingStepParameters();
    stepParameters.setType(K8sTrafficRoutingConfigType.CONFIG);
    stepParameters.setTrafficRouting(k8sTrafficRouting);
    final StepElementParameters stepElementParameters =
        StepElementParameters.builder().spec(stepParameters).timeout(ParameterField.createValueField("30m")).build();

    when(k8sTrafficRoutingHelper.validateAndGetTrafficRoutingConfig(k8sTrafficRouting))
        .thenReturn(Optional.of(
            K8sTrafficRoutingConfig.builder()
                .providerConfig(IstioProviderConfig.builder().build())
                .routes(Arrays.asList(TrafficRoute.builder().routeType(RouteType.HTTP).build()))
                .destinations(Arrays.asList(TrafficRoutingDestination.builder().host("svc1").weight(20).build(),
                    TrafficRoutingDestination.builder().host("svc2").weight(80).build()))
                .build()));

    TaskRequest taskRequest = TaskRequest.newBuilder().build();
    doReturn(taskRequest).when(k8sStepHelper).createTaskRequest(any(), any(), any(), any());
    AsyncExecutableResponse taskAsyncExecutableResponse =
        AsyncExecutableResponse.newBuilder().addCallbackIds("taskId1").build();
    doReturn(taskAsyncExecutableResponse)
        .when(asyncExecutableTaskHelper)
        .getAsyncExecutableResponse(ambiance, taskRequest);

    AsyncExecutableResponse asyncExecutableResponse =
        k8sTrafficRoutingStep.executeAsyncAfterRbac(ambiance, stepElementParameters, null);

    ArgumentCaptor<String> releaseNameCaptor = ArgumentCaptor.forClass(String.class);
    verify(k8sStepHelper, times(1)).publishReleaseNameStepDetails(eq(ambiance), releaseNameCaptor.capture());
    assertThat(releaseNameCaptor.getValue()).isEqualTo(releaseName);
    assertThat(asyncExecutableResponse.getCallbackIds(0)).isEqualTo("taskId1");
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testHandleAsyncResponseInternalWithTaskNGException() {
    final StepElementParameters stepElementParameters = StepElementParameters.builder().build();

    try (MockedStatic<StrategyHelper> mockRestStatic = Mockito.mockStatic(StrategyHelper.class)) {
      Exception e = new TaskNGDataException(null, new Exception("TaskNGDataException message"));
      when(StrategyHelper.buildResponseDataSupplier(any())).thenThrow(e);
      StepResponse response = k8sTrafficRoutingStep.handleAsyncResponseInternal(ambiance, stepElementParameters, null);

      assertThat(response.getStatus()).isEqualTo(Status.FAILED);
      assertThat(response.getFailureInfo().getErrorMessage()).isEqualTo("TaskNGDataException message");
    }
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testHandleAsyncResponseInternalWithOtherException() {
    final StepElementParameters stepElementParameters = StepElementParameters.builder().build();

    try (MockedStatic<StrategyHelper> mockRestStatic = Mockito.mockStatic(StrategyHelper.class)) {
      Exception e = new GeneralException("General Exception message");
      when(StrategyHelper.buildResponseDataSupplier(any())).thenThrow(e);
      StepResponse response = k8sTrafficRoutingStep.handleAsyncResponseInternal(ambiance, stepElementParameters, null);

      assertThat(response.getStatus()).isEqualTo(Status.FAILED);
      assertThat(response.getFailureInfo().getErrorMessage()).isEqualTo("General Exception message");
    }
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testHandleAsyncResponseInternal() {
    final StepElementParameters stepElementParameters = StepElementParameters.builder().build();

    try (MockedStatic<StrategyHelper> mockRestStatic = Mockito.mockStatic(StrategyHelper.class)) {
      List<UnitProgress> unitProgressList = new ArrayList<>();
      unitProgressList.add(UnitProgress.newBuilder().setUnitName("unit1").setStatus(UnitStatus.SUCCESS).build());
      UnitProgressData unitProgressData = UnitProgressData.builder().unitProgresses(unitProgressList).build();
      ThrowingSupplier<K8sDeployResponse> k8sDeployResponse = ()
          -> K8sDeployResponse.builder()
                 .commandUnitsProgress(unitProgressData)
                 .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                 .build();

      when(StrategyHelper.buildResponseDataSupplier(any())).thenAnswer(a -> k8sDeployResponse);
      StepResponse response = k8sTrafficRoutingStep.handleAsyncResponseInternal(ambiance, stepElementParameters, null);

      assertThat(response.getStatus()).isEqualTo(Status.SUCCEEDED);
      assertThat(response.getUnitProgressList().get(0).getStatus()).isEqualTo(UnitStatus.SUCCESS);
      assertThat(response.getUnitProgressList().get(0).getUnitName()).isEqualTo("unit1");
    }
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testHandleAbort() {
    k8sTrafficRoutingStep.handleAbort(
        ambiance, stepElementParameters, AsyncExecutableResponse.newBuilder().addCallbackIds("taskId").build(), false);

    verify(delegateGrpcClientWrapper)
        .cancelV2Task(AccountId.newBuilder().setId("accountId").build(), TaskId.newBuilder().setId("taskId").build());
  }

  private List<K8sTrafficRoutingDestination> getDestinations(Map<String, Integer> dests) {
    List<K8sTrafficRoutingDestination> destinations = new ArrayList<>();
    dests.forEach((k, v) -> { destinations.add(getDestination(k, v)); });
    return destinations;
  }

  private K8sTrafficRoutingDestination getDestination(String host, Integer weight) {
    return K8sTrafficRoutingDestination.builder()
        .destination(K8sTrafficRoutingDestination.DestinationSpec.builder()
                         .host(ParameterField.<String>builder().value(host).build())
                         .weight(ParameterField.<Integer>builder().value(weight).build())
                         .build())
        .build();
  }
}
