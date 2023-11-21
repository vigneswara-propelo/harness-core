/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.utils;

import static io.harness.logging.CommandExecutionStatus.RUNNING;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.rule.OwnerRule.ACASIAN;
import static io.harness.rule.OwnerRule.PUNEET;

import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.COMMAND_UNIT_NAME;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class ExecutionLogWriterTest extends CategoryTest {
  @Mock private LogCallback logCallback;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void smokeTest() {
    ExecutionLogWriter testWriter = ExecutionLogWriter.builder()
                                        .accountId(ACCOUNT_ID)
                                        .appId(APP_ID)
                                        .commandUnitName(COMMAND_UNIT_NAME)
                                        .executionId(WORKFLOW_EXECUTION_ID)
                                        .hostName("localhost")
                                        .logCallback(logCallback)
                                        .stringBuilder(new StringBuilder(1024))
                                        .logLevel(INFO)
                                        .build();

    String logLineFirstSegment = "Hello ";
    String logLineSecondSegment = "World";
    testWriter.write(logLineFirstSegment.toCharArray(), 0, logLineFirstSegment.length());
    testWriter.write(logLineSecondSegment.toCharArray(), 0, logLineSecondSegment.length());

    String newLine = "\n";
    testWriter.write(newLine.toCharArray(), 0, 1);

    Mockito.verify(logCallback).saveExecutionLog(logLineFirstSegment + logLineSecondSegment, INFO, RUNNING);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testWarningMessage() {
    ExecutionLogWriter testWriter = ExecutionLogWriter.builder()
                                        .accountId(ACCOUNT_ID)
                                        .appId(APP_ID)
                                        .commandUnitName(COMMAND_UNIT_NAME)
                                        .executionId(WORKFLOW_EXECUTION_ID)
                                        .hostName("localhost")
                                        .logCallback(logCallback)
                                        .stringBuilder(new StringBuilder(1024))
                                        .logLevel(INFO)
                                        .build();

    String logLineFirstSegment = "WARNING: This is a warn message";
    testWriter.write(logLineFirstSegment.toCharArray(), 0, logLineFirstSegment.length());
    String newLine = "\n";
    testWriter.write(newLine.toCharArray(), 0, 1);

    Mockito.verify(logCallback).saveExecutionLog(logLineFirstSegment, LogLevel.WARN, RUNNING);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testDebugMessage() {
    ExecutionLogWriter testWriter = ExecutionLogWriter.builder()
                                        .accountId(ACCOUNT_ID)
                                        .appId(APP_ID)
                                        .commandUnitName(COMMAND_UNIT_NAME)
                                        .executionId(WORKFLOW_EXECUTION_ID)
                                        .hostName("localhost")
                                        .logCallback(logCallback)
                                        .stringBuilder(new StringBuilder(1024))
                                        .logLevel(INFO)
                                        .build();

    String logLineFirstSegment = "DEBUG: This is a debug message";
    testWriter.write(logLineFirstSegment.toCharArray(), 0, logLineFirstSegment.length());
    String newLine = "\n";
    testWriter.write(newLine.toCharArray(), 0, 1);

    Mockito.verify(logCallback).saveExecutionLog(logLineFirstSegment, LogLevel.DEBUG, RUNNING);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testInfoMessage() {
    ExecutionLogWriter testWriter = ExecutionLogWriter.builder()
                                        .accountId(ACCOUNT_ID)
                                        .appId(APP_ID)
                                        .commandUnitName(COMMAND_UNIT_NAME)
                                        .executionId(WORKFLOW_EXECUTION_ID)
                                        .hostName("localhost")
                                        .logCallback(logCallback)
                                        .stringBuilder(new StringBuilder(1024))
                                        .logLevel(INFO)
                                        .build();

    String logLineFirstSegment = "This is a info message";
    testWriter.write(logLineFirstSegment.toCharArray(), 0, logLineFirstSegment.length());
    String newLine = "\n";
    testWriter.write(newLine.toCharArray(), 0, 1);

    Mockito.verify(logCallback).saveExecutionLog(logLineFirstSegment, INFO, RUNNING);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testErrorMessage() {
    ExecutionLogWriter testWriter = ExecutionLogWriter.builder()
                                        .accountId(ACCOUNT_ID)
                                        .appId(APP_ID)
                                        .commandUnitName(COMMAND_UNIT_NAME)
                                        .executionId(WORKFLOW_EXECUTION_ID)
                                        .hostName("localhost")
                                        .logCallback(logCallback)
                                        .stringBuilder(new StringBuilder(1024))
                                        .logLevel(ERROR)
                                        .build();

    String logLineFirstSegment = "ERROR: This is a error message";
    testWriter.write(logLineFirstSegment.toCharArray(), 0, logLineFirstSegment.length());
    String newLine = "\n";
    testWriter.write(newLine.toCharArray(), 0, 1);

    Mockito.verify(logCallback).saveExecutionLog(logLineFirstSegment, ERROR, RUNNING);
  }
}
