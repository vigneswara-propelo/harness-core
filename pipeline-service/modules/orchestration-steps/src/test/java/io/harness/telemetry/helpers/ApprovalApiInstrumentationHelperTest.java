/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.telemetry.helpers;

import static io.harness.rule.OwnerRule.RISHABH;

import static junit.framework.TestCase.assertTrue;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.telemetry.TelemetryReporter;

import java.util.concurrent.CompletableFuture;
import org.jooq.tools.reflect.Reflect;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ApprovalApiInstrumentationHelperTest extends CategoryTest {
  private static final String ACCOUNT = "account";
  private static final String ORG = "org";
  private static final String PROJECT = "project";
  private static final String EXECUTION_ID = "execution_id";
  @InjectMocks ApprovalApiInstrumentationHelper instrumentationHelper;
  @Mock TelemetryReporter telemetryReporter;
  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    Reflect.on(instrumentationHelper).set("telemetryReporter", telemetryReporter);
  }

  @Test
  @Owner(developers = RISHABH)
  @Category(UnitTests.class)
  public void testSuccessEvent() {
    CompletableFuture<Void> telemetryTask = instrumentationHelper.sendApprovalApiEvent(
        ACCOUNT, ORG, PROJECT, EXECUTION_ID, ApprovalApiInstrumentationHelper.SUCCESS, null);
    telemetryTask.join();
    assertTrue(telemetryTask.isDone());
  }

  @Test
  @Owner(developers = RISHABH)
  @Category(UnitTests.class)
  public void testFailureEvent() {
    CompletableFuture<Void> telemetryTask =
        instrumentationHelper.sendApprovalApiEvent(ACCOUNT, ORG, PROJECT, EXECUTION_ID,
            ApprovalApiInstrumentationHelper.FAILURE, ApprovalApiInstrumentationHelper.MULTIPLE_APPROVALS_FOUND);
    telemetryTask.join();
    assertTrue(telemetryTask.isDone());
  }
}
