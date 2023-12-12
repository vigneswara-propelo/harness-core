/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.service;

import static io.harness.beans.DelegateTask.Status.QUEUED;
import static io.harness.beans.DelegateTask.Status.STARTED;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability;
import static io.harness.persistence.HPersistence.upsertReturnNewOptions;
import static io.harness.rule.OwnerRule.ARPIT;
import static io.harness.rule.OwnerRule.ASHISHSANODIA;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.JENNY;

import static java.lang.System.currentTimeMillis;
import static java.time.Duration.ofMillis;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.harness.DelegateServiceTestBase;
import io.harness.beans.DelegateTask;
import io.harness.beans.DelegateTask.DelegateTaskBuilder;
import io.harness.category.element.UnitTests;
import io.harness.delegate.NoEligibleDelegatesInAccountException;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.Delegate.DelegateBuilder;
import io.harness.delegate.beans.DelegateSyncTaskResponse;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.TaskDataV2;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.exception.ExceptionUtils;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import io.harness.service.intfc.DelegateCache;
import io.harness.service.intfc.DelegateTaskService;
import io.harness.threading.Morpheus;

import software.wings.delegatetasks.validation.core.DelegateConnectionResult;
import software.wings.delegatetasks.validation.core.DelegateConnectionResult.DelegateConnectionResultKeys;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import dev.morphia.query.Query;
import dev.morphia.query.UpdateOperations;
import java.util.Arrays;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DelegateTaskServiceTest extends DelegateServiceTestBase {
  @Inject HPersistence persistence;
  @Inject @Named("referenceFalseKryoSerializer") private KryoSerializer referenceFalseKryoSerializer;
  @Inject DelegateTaskService delegateTaskService;

  @Inject DelegateCache delegateCache;

  private static final String TEST_ACCOUNT_ID = "testAccount";

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testTouchExecutingTasksWithEmpty() {
    assertThatCode(() -> delegateTaskService.touchExecutingTasks(null, null, null)).doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testTouchExecutingTasks() {
    String delegateId = generateUuid();
    DelegateTaskBuilder delegateTaskBuilder = DelegateTask.builder()
                                                  .accountId(TEST_ACCOUNT_ID)
                                                  .delegateId(delegateId)
                                                  .status(STARTED)
                                                  .data(TaskData.builder().timeout(1000L).build())
                                                  .expiry(currentTimeMillis() + 1000L);
    DelegateTask delegateTask1 = delegateTaskBuilder.uuid(generateUuid()).build();
    persistence.save(delegateTask1);
    DelegateTask delegateTask2 = delegateTaskBuilder.uuid(generateUuid()).status(QUEUED).build();
    persistence.save(delegateTask2);

    Morpheus.sleep(ofMillis(1));

    delegateTaskService.touchExecutingTasks(
        TEST_ACCOUNT_ID, delegateId, asList(delegateTask1.getUuid(), delegateTask2.getUuid()));

    DelegateTask updatedDelegateTask1 = persistence.get(DelegateTask.class, delegateTask1.getUuid());
    assertThat(updatedDelegateTask1.getExpiry()).isGreaterThan(delegateTask1.getExpiry());

    DelegateTask updatedDelegateTask2 = persistence.get(DelegateTask.class, delegateTask2.getUuid());
    assertThat(updatedDelegateTask2.getExpiry()).isEqualTo(delegateTask2.getExpiry());
  }

  @Test
  @Owner(developers = ARPIT)
  @Category(UnitTests.class)
  public void testSupportedTaskType() {
    DelegateBuilder delegateBuilder =
        Delegate.builder().accountId(TEST_ACCOUNT_ID).lastHeartBeat(System.currentTimeMillis());

    Delegate delegate1 = delegateBuilder.supportedTaskTypes(Arrays.asList("type1", "type2")).build();
    Delegate delegate2 = delegateBuilder.supportedTaskTypes(Arrays.asList("type1", "type3")).build();

    String delegateId1 = persistence.save(delegate1);
    String delegateId2 = persistence.save(delegate2);

    boolean isTaskType1Supported = delegateTaskService.isTaskTypeSupportedByAllDelegates(TEST_ACCOUNT_ID, "type1");
    boolean isTaskType2Supported = delegateTaskService.isTaskTypeSupportedByAllDelegates(TEST_ACCOUNT_ID, "type2");

    assertThat(isTaskType1Supported).isTrue();
    assertThat(isTaskType2Supported).isFalse();
  }

  @Test
  @Owner(developers = ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testHandleResponseV2() {
    String delegateId = generateUuid();
    DelegateTask delegateTask = DelegateTask.builder()
                                    .uuid(generateUuid())
                                    .accountId(TEST_ACCOUNT_ID)
                                    .delegateId(delegateId)
                                    .status(STARTED)
                                    .taskDataV2(TaskDataV2.builder().timeout(1000L).build())
                                    .expiry(currentTimeMillis() + 1000L)
                                    .build();

    NoEligibleDelegatesInAccountException exception =
        new NoEligibleDelegatesInAccountException("some url not provided exception");
    DelegateTaskResponse delegateTaskResponse = DelegateTaskResponse.builder()
                                                    .response(ErrorNotifyResponseData.builder()
                                                                  .errorMessage(ExceptionUtils.getMessage(exception))
                                                                  .exception(exception)
                                                                  .build())
                                                    .responseCode(DelegateTaskResponse.ResponseCode.FAILED)
                                                    .accountId(TEST_ACCOUNT_ID)
                                                    .build();

    delegateTaskService.handleResponseV2(delegateTask, delegateTaskResponse);
    DelegateSyncTaskResponse delegateSyncTaskResponse =
        persistence.get(DelegateSyncTaskResponse.class, delegateTask.getUuid());
    Object responseData = referenceFalseKryoSerializer.asInflatedObject(delegateSyncTaskResponse.getResponseData());
    assertThat(delegateSyncTaskResponse.isUsingKryoWithoutReference()).isTrue();
    assertThat(responseData).isInstanceOf(ErrorNotifyResponseData.class);
  }

  @Test
  @Owner(developers = JENNY)
  @Category(UnitTests.class)
  public void testClearConnectionResultOnTaskResponse() {
    String delegateId = generateUuid();
    HttpConnectionExecutionCapability matchingExecutionCapability =
        buildHttpConnectionExecutionCapability("criteria", null);
    HttpConnectionExecutionCapability matchingExecutionCapability2 =
        buildHttpConnectionExecutionCapability("criteria2", null);
    saveConnectionResult("criteria", delegateId);
    saveConnectionResult("criteria2", delegateId);
    saveConnectionResult("criteria", delegateId);
    DelegateTask delegateTask =
        DelegateTask.builder()
            .uuid(generateUuid())
            .accountId(TEST_ACCOUNT_ID)
            .delegateId(delegateId)
            .status(STARTED)
            .taskDataV2(TaskDataV2.builder().timeout(1000L).build())
            .executionCapabilities(asList(matchingExecutionCapability, matchingExecutionCapability2))
            .expiry(currentTimeMillis() + 1000L)
            .build();

    DelegateTaskResponse delegateTaskResponse =
        DelegateTaskResponse.builder()
            .response(ErrorNotifyResponseData.builder().errorMessage("failure").build())
            .responseCode(DelegateTaskResponse.ResponseCode.FAILED)
            .accountId(TEST_ACCOUNT_ID)
            .build();

    DelegateConnectionResult before = persistence.createQuery(DelegateConnectionResult.class).get();
    assertThat(before).isNotNull();
    delegateTaskService.handleResponseV2(delegateTask, delegateTaskResponse);
    DelegateConnectionResult after = persistence.createQuery(DelegateConnectionResult.class).get();
    assertThat(after).isNull();
  }

  private void saveConnectionResult(String criteria, String delegateId) {
    Query<DelegateConnectionResult> query = persistence.createQuery(DelegateConnectionResult.class)
                                                .filter(DelegateConnectionResultKeys.accountId, TEST_ACCOUNT_ID)
                                                .filter(DelegateConnectionResultKeys.delegateId, delegateId)
                                                .filter(DelegateConnectionResultKeys.criteria, criteria);
    UpdateOperations<DelegateConnectionResult> updateOperations =
        persistence.createUpdateOperations(DelegateConnectionResult.class)
            .setOnInsert(DelegateConnectionResultKeys.accountId, TEST_ACCOUNT_ID)
            .setOnInsert(DelegateConnectionResultKeys.delegateId, delegateId)
            .setOnInsert(DelegateConnectionResultKeys.criteria, criteria)
            .set(DelegateConnectionResultKeys.validated, true);

    persistence.upsert(query, updateOperations, upsertReturnNewOptions);
  }
}
