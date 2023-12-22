/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.taskresponse;

import static io.harness.annotations.dev.HarnessTeam.DEL;
import static io.harness.rule.OwnerRule.MARKO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.Status;
import io.harness.delegate.core.beans.ExecutionStatus;
import io.harness.delegate.core.beans.StatusCode;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.protobuf.ByteString;
import com.google.protobuf.Duration;
import java.nio.charset.Charset;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@OwnedBy(DEL)
@RunWith(MockitoJUnitRunner.class)
public class TaskResponseServiceTest {
  private static final String ACCOUNT_ID = "accountId";
  private static final String TASK_ID = "taskId";
  private static final String DELEGATE_ID = "delegateId";
  private static final int DURATION = 3;
  @Mock private HPersistence persistence;
  @Captor private ArgumentCaptor<TaskResponse> responseCaptor;
  private TaskResponseService underTest;

  @Before
  public void setUp() {
    underTest = new TaskResponseService(persistence);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testHandleSuccessResponse() {
    final var execution = buildExecutionStatus(null);
    underTest.handleResponse(ACCOUNT_ID, TASK_ID, execution, DELEGATE_ID);

    final var expected = expectedResponse(execution, null);
    verify(persistence).save(responseCaptor.capture(), eq(false));
    assertThat(responseCaptor.getValue()).usingRecursiveComparison().ignoringFields("validUntil").isEqualTo(expected);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testHandleFailureResponse() {
    final var error = "error";
    final var execution = buildExecutionStatus(error);
    underTest.handleResponse(ACCOUNT_ID, TASK_ID, execution, DELEGATE_ID);

    final var expected = expectedResponse(execution, error);
    verify(persistence).save(responseCaptor.capture(), eq(false));
    assertThat(responseCaptor.getValue()).usingRecursiveComparison().ignoringFields("validUntil").isEqualTo(expected);
  }

  private static TaskResponse expectedResponse(final ExecutionStatus execution, final String error) {
    final var status = error != null ? Status.FAILURE : Status.SUCCESS;
    final var builder = TaskResponse.builder()
                            .uuid(TASK_ID)
                            .accountId(ACCOUNT_ID)
                            .data(execution.getBinaryData().toByteArray())
                            .code(status)
                            .executionTime(java.time.Duration.ofSeconds(DURATION))
                            .createdByDelegateId(DELEGATE_ID);
    if (error != null) {
      builder.errorMessage(error);
    }
    return builder.build();
  }

  private ExecutionStatus buildExecutionStatus(final String error) {
    final var statusCode = error != null ? StatusCode.CODE_FAILED : StatusCode.CODE_SUCCESS;
    final var builder = ExecutionStatus.newBuilder()
                            .setCode(statusCode)
                            .setExecutionTime(Duration.newBuilder().setSeconds(DURATION).build())
                            .setBinaryData(ByteString.copyFrom("some data", Charset.defaultCharset()));
    if (error != null) {
      builder.setErrorMessage(error);
    }
    return builder.build();
  }
}
