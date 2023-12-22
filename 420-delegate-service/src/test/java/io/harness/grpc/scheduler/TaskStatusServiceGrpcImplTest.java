/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.grpc.scheduler;

import static io.harness.annotations.dev.HarnessTeam.DEL;
import static io.harness.delegate.Status.SUCCESS;
import static io.harness.rule.OwnerRule.MARKO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.IntegrationTests;
import io.harness.delegate.GetTaskStatusRequest;
import io.harness.delegate.TaskId;
import io.harness.delegate.TaskStatusServiceGrpc;
import io.harness.rule.Owner;
import io.harness.taskresponse.TaskResponse;
import io.harness.taskresponse.TaskResponseService;

import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@OwnedBy(DEL)
@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class TaskStatusServiceGrpcImplTest {
  private static final String ACCOUNT_ID = "accountId";
  private static final String TASK_ID = "taskId";
  private static final int DURATION = 3;
  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private TaskStatusServiceGrpc.TaskStatusServiceBlockingStub underTest;
  @Mock private TaskResponseService responseService;

  @Before
  public void setUp() throws Exception {
    final var underTest = new TaskStatusServiceGrpcImpl(responseService);

    final var serverName = InProcessServerBuilder.generateName();
    final Server server =
        InProcessServerBuilder.forName(serverName).directExecutor().addService(underTest).build().start();
    grpcCleanup.register(server);

    final var channel = grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build());
    this.underTest = TaskStatusServiceGrpc.newBlockingStub(channel);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(IntegrationTests.class)
  public void testGetTaskStatus() {
    final var request = request(ACCOUNT_ID, TASK_ID);
    final var response = getResponse();

    when(responseService.getTaskResponse(ACCOUNT_ID, TASK_ID)).thenReturn(response);

    final var actual = underTest.getTaskStatus(request);

    assertThat(actual.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(actual.getTaskId().getId()).isEqualTo(TASK_ID);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);
    assertThat(actual.getExecutionTime())
        .isEqualTo(com.google.protobuf.Duration.newBuilder().setSeconds(DURATION).build());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(IntegrationTests.class)
  public void testGetTaskStatusFailNoAccountId() {
    final var request = request("", TASK_ID);

    assertThatThrownBy(() -> underTest.getTaskStatus(request))
        .isInstanceOf(StatusRuntimeException.class)
        .hasMessageContaining("accountId and taskId are mandatory")
        .matches(e -> ((StatusRuntimeException) e).getStatus().getCode() == Status.INVALID_ARGUMENT.getCode());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(IntegrationTests.class)
  public void testGetTaskStatusFailNoTaskId() {
    final var request = request(ACCOUNT_ID, null);

    assertThatThrownBy(() -> underTest.getTaskStatus(request))
        .isInstanceOf(StatusRuntimeException.class)
        .hasMessageContaining("accountId and taskId are mandatory")
        .matches(e -> ((StatusRuntimeException) e).getStatus().getCode() == Status.INVALID_ARGUMENT.getCode());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(IntegrationTests.class)
  public void testInitTaskFailGenericException() {
    final var request = request(ACCOUNT_ID, TASK_ID);

    when(responseService.getTaskResponse(ACCOUNT_ID, TASK_ID)).thenThrow(new NullPointerException("unknown exception"));

    assertThatThrownBy(() -> underTest.getTaskStatus(request))
        .isInstanceOf(StatusRuntimeException.class)
        .matches(e -> ((StatusRuntimeException) e).getStatus().getCode() == Status.INTERNAL.getCode());
  }

  private static GetTaskStatusRequest request(final String accountId, final String taskId) {
    final var requestBuilder = GetTaskStatusRequest.newBuilder().setAccountId(accountId);
    if (taskId != null) {
      requestBuilder.setTaskId(TaskId.newBuilder().setId(taskId).build());
    }
    return requestBuilder.build();
  }

  private static TaskResponse getResponse() {
    return TaskResponse.builder()
        .uuid(TASK_ID)
        .accountId(ACCOUNT_ID)
        .executionTime(Duration.ofSeconds(DURATION))
        .code(SUCCESS)
        .build();
  }
}
