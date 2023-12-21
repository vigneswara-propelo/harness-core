/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.executables;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.DelegateTaskRequest;
import io.harness.delegate.SubmitTaskRequest;
import io.harness.delegate.TaskDetails;
import io.harness.delegate.TaskSelector;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.AsyncExecutableResponse;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.sdk.core.waiter.AsyncWaitEngine;
import io.harness.serializer.KryoSerializer;
import io.harness.service.DelegateGrpcClientWrapper;

import software.wings.beans.SerializationFormat;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_K8S})
@OwnedBy(HarnessTeam.CDP)
@Singleton
public class AsyncExecutableTaskHelper {
  @Inject @Named("referenceFalseKryoSerializer") private KryoSerializer kryoSerializer;
  @Inject private DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  @Inject private AsyncWaitEngine asyncWaitEngine;

  public TaskData extractTaskRequest(TaskDetails taskDetails) {
    Object[] parameters = null;
    byte[] data;
    SerializationFormat serializationFormat;
    if (taskDetails.getParametersCase().equals(TaskDetails.ParametersCase.KRYO_PARAMETERS)) {
      data = taskDetails.getKryoParameters().toByteArray();
      parameters = new Object[] {kryoSerializer.asInflatedObject(data)};
      serializationFormat = SerializationFormat.KRYO;
    } else if (taskDetails.getParametersCase().equals(TaskDetails.ParametersCase.JSON_PARAMETERS)) {
      data = taskDetails.getJsonParameters().toStringUtf8().getBytes(StandardCharsets.UTF_8);
      serializationFormat = SerializationFormat.JSON;
    } else {
      throw new InvalidRequestException("Invalid task response type.");
    }
    return TaskData.builder()
        .parameters(parameters)
        .data(data)
        .taskType(taskDetails.getType().getType())
        .timeout(taskDetails.getExecutionTimeout().getSeconds() * 1000)
        .expressionFunctorToken((int) taskDetails.getExpressionFunctorToken())
        .parked(taskDetails.getParked())
        .serializationFormat(serializationFormat)
        .async(true)
        .build();
  }

  // Copied method from CDStepHelper until we refactor it to make it easy to import
  public DelegateTaskRequest mapTaskRequestToDelegateTaskRequest(TaskRequest taskRequest, TaskData taskData,
      Set<String> taskSelectors, String baseLogKey, boolean shouldSkipOpenStream) {
    final SubmitTaskRequest submitTaskRequest = taskRequest.getDelegateTaskRequest().getRequest();
    return DelegateTaskRequest.builder()
        .taskParameters((TaskParameters) taskData.getParameters()[0])
        .taskType(taskData.getTaskType())
        .parked(taskData.isParked())
        .accountId(submitTaskRequest.getAccountId().getId())
        .taskSetupAbstractions(submitTaskRequest.getSetupAbstractions().getValuesMap())
        .taskSelectors(taskSelectors)
        .executionTimeout(Duration.ofMillis(taskData.getTimeout()))
        .logStreamingAbstractions(new LinkedHashMap<>(submitTaskRequest.getLogAbstractions().getValuesMap()))
        .forceExecute(submitTaskRequest.getForceExecute())
        .expressionFunctorToken(taskData.getExpressionFunctorToken())
        .serializationFormat(taskData.getSerializationFormat())
        .eligibleToExecuteDelegateIds(submitTaskRequest.getEligibleToExecuteDelegateIdsList())
        .executeOnHarnessHostedDelegates(submitTaskRequest.getExecuteOnHarnessHostedDelegates())
        .emitEvent(submitTaskRequest.getEmitEvent())
        .stageId(submitTaskRequest.getStageId())
        .baseLogKey(baseLogKey)
        .shouldSkipOpenStream(shouldSkipOpenStream)
        .selectionLogsTrackingEnabled(true)
        .build();
  }

  public AsyncExecutableResponse getAsyncExecutableResponse(Ambiance ambiance, TaskRequest taskRequest) {
    SubmitTaskRequest request = taskRequest.getDelegateTaskRequest().getRequest();
    TaskData taskData = extractTaskRequest(request.getDetails());
    Set<String> selectorsList =
        request.getSelectorsList().stream().map(TaskSelector::getSelector).collect(Collectors.toSet());
    String taskName = taskRequest.getDelegateTaskRequest().getTaskName();
    DelegateTaskRequest delegateTaskRequest =
        mapTaskRequestToDelegateTaskRequest(taskRequest, taskData, selectorsList, "", false);

    String taskId = delegateGrpcClientWrapper.submitAsyncTaskV2(delegateTaskRequest, Duration.ZERO);
    return createAsyncExecutableResponse(ambiance, taskId, taskName, taskRequest, taskData.getTimeout());
  }

  private AsyncExecutableResponse createAsyncExecutableResponse(
      Ambiance ambiance, String callbackId, String taskName, TaskRequest taskRequest, long timeout) {
    List<String> logKeysList = taskRequest.getDelegateTaskRequest().getLogKeysList();
    List<String> units = taskRequest.getDelegateTaskRequest().getUnitsList();
    byte[] ambianceBytes = ambiance.toByteArray();
    AsyncDelegateResumeCallback asyncDelegateResumeCallback = AsyncDelegateResumeCallback.builder()
                                                                  .ambianceBytes(ambianceBytes)
                                                                  .taskId(callbackId)
                                                                  .taskName(taskName)
                                                                  .build();
    asyncWaitEngine.waitForAllOn(asyncDelegateResumeCallback, null, Collections.singletonList(callbackId), 0);
    return AsyncExecutableResponse.newBuilder()
        .addAllLogKeys(logKeysList)
        .addAllUnits(units)
        .addCallbackIds(callbackId)
        .setTimeout(Math.toIntExact(timeout))
        .build();
  }
}
