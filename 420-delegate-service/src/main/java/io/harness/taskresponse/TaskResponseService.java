/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.taskresponse;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.delegate.Status;
import io.harness.delegate.core.beans.ExecutionStatus;
import io.harness.delegate.core.beans.StatusCode;
import io.harness.delegate.task.tasklogging.ExecutionLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.mapstruct.protobuf.StandardProtobufMappers;
import io.harness.persistence.HPersistence;
import io.harness.taskresponse.TaskResponse.TaskResponseKeys;

import com.google.inject.Inject;
import com.google.protobuf.Duration;
import dev.morphia.query.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class TaskResponseService {
  private final HPersistence persistence;

  public void handleResponse(
      final String accountId, final String taskId, final ExecutionStatus status, final String delegateId) {
    try (AutoLogContext ignore = new ExecutionLogContext(taskId, OVERRIDE_ERROR)) {
      final var code = mapCode(status.getCode());
      storeResponse(accountId, taskId, getResponseData(status), code, status.getErrorMessage(),
          status.getExecutionTime(), delegateId);
      // ToDo: Send Response Event
    }
  }

  public TaskResponse getTaskResponse(final String accountId, final String taskId) {
    final var response = findResponse(accountId, taskId).first();
    if (response == null) {
      throw new IllegalArgumentException("TaskResponse not found for task " + taskId);
    }
    return response;
  }

  private String storeResponse(final String accountId, final String taskId, final byte[] response, final Status code,
      final String errorMessage, final Duration executionTime, final String delegateId) {
    try (AutoLogContext ignore = new ExecutionLogContext(taskId, OVERRIDE_ERROR)) {
      final var builder = TaskResponse.builder()
                              .uuid(taskId)
                              .accountId(accountId)
                              .data(response)
                              .code(code)
                              .executionTime(StandardProtobufMappers.INSTANCE.mapDuration(executionTime))
                              .createdByDelegateId(delegateId);

      if (isNotBlank(errorMessage)) {
        builder.errorMessage(errorMessage);
      }
      return persistence.save(builder.build(), false);
    }
  }

  private boolean deleteResponse(final String accountId, final String taskId) {
    return persistence.delete(findResponse(accountId, taskId));
  }

  private Query<TaskResponse> findResponse(final String accountId, final String taskId) {
    return persistence.createQuery(TaskResponse.class)
        .filter(TaskResponseKeys.accountId, accountId)
        .filter(TaskResponseKeys.uuid, taskId);
  }

  private static byte[] getResponseData(final io.harness.delegate.core.beans.ExecutionStatus status) {
    if (status.hasProtoData()) {
      throw new IllegalArgumentException("Proto response data not supported yet");
    }
    return status.hasBinaryData() ? status.getBinaryData().toByteArray() : null;
  }

  private static Status mapCode(final StatusCode code) {
    switch (code) {
      case CODE_SUCCESS:
        return Status.SUCCESS;
      case CODE_FAILED:
        return Status.FAILURE;
      case CODE_TIMEOUT:
        return Status.TIMEOUT;
      case CODE_UNKNOWN:
      default:
        throw new IllegalArgumentException("Unknown status code " + code);
    }
  }
}
