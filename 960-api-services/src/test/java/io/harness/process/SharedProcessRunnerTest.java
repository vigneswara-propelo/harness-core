/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.process;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.rule.OwnerRule.ABOSII;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessOutput;
import org.zeroturnaround.exec.ProcessResult;

public class SharedProcessRunnerTest extends CategoryTest {
  private static final String PROCESS_RESULT_NAME = "Process result: %d";

  private ThreadPoolExecutor processExecutorService;
  private ExecutorService callerExecutorService;
  private CompletionService<ProcessResult> completionService;
  private SharedProcessRunner sharedProcessRunner;

  private final AtomicInteger createdProcesses = new AtomicInteger(0);

  @Before
  public void setup() {
    processExecutorService = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    callerExecutorService = Executors.newFixedThreadPool(11);
    completionService = new ExecutorCompletionService<>(callerExecutorService);
    sharedProcessRunner = new SharedProcessRunner(processExecutorService);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  @SneakyThrows
  public void testScheduleConcurrentExecution() {
    final int runningCount = 10;
    ProcessExecutorFactory mockFactory = createMockFactory(300);

    for (int i = 0; i < runningCount; i++) {
      final int index = i % 2 == 0 ? 1 : 0;
      completionService.submit(() -> {
        try (ProcessRef ref = sharedProcessRunner.execute("process-" + index, mockFactory)) {
          return ref.get();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      });
    }

    int processed = 0;
    while (processed < runningCount) {
      Future<ProcessResult> future = completionService.take();
      ProcessResult result = future.get();
      assertThat(result.getExitValue()).isLessThanOrEqualTo(2);
      processed++;
    }

    assertThat(sharedProcessRunner.getRunningProcesses()).isEmpty();
    verify(mockFactory, times(2)).create();
    assertThat(processExecutorService.getActiveCount()).isZero();
    assertThat(processExecutorService.getCompletedTaskCount()).isEqualTo(processExecutorService.getTaskCount());
    assertThat(processExecutorService.getTaskCount()).isEqualTo(2);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  @SneakyThrows
  public void testInterruptRunning() {
    ProcessExecutorFactory mockFactory = createMockFactory(10000);

    try (ProcessRef ref = sharedProcessRunner.execute("process-1", mockFactory)) {
      // don't wait for process to complete
    } catch (Exception e) {
      Assert.fail("unexpected test failure");
    }

    // give time to interrupt current execution
    Thread.sleep(100);
    assertThat(sharedProcessRunner.getRunningProcesses()).isEmpty();
    assertThat(processExecutorService.getActiveCount()).isZero();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  @SneakyThrows
  public void testInterruptRunningConcurrent() {
    final int runningCount = 3;
    ProcessExecutorFactory mockFactory = createMockFactory(10000);
    List<Future<ProcessResult>> runningFeatures = new ArrayList<>();

    for (int i = 0; i < runningCount; i++) {
      runningFeatures.add(completionService.submit(() -> {
        try (ProcessRef ref = sharedProcessRunner.execute("process-1", mockFactory)) {
          return ref.get();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }));
    }

    Future<ProcessResult> first = runningFeatures.get(0);
    first.cancel(true);
    runningFeatures.remove(0);
    assertThat(first.isCancelled()).isTrue();
    assertThat(sharedProcessRunner.getRunningProcesses()).isNotEmpty();

    for (Future<ProcessResult> runningFeature : runningFeatures) {
      runningFeature.cancel(true);
    }

    // give time to close
    Thread.sleep(50);
    assertThat(sharedProcessRunner.getRunningProcesses()).isEmpty();
    verify(mockFactory, times(1)).create();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetLoggableProcessKey() {
    Set<String> binPathVariations = Set.of("cmd", "/cmd", "/usr/bin/cmd",
        "/opt/harness-delegate/client-tools/v0.12.1/cmd", "/custom-path/v0-12/cmd", "/custom/v--12/cmd");

    for (String binPath : binPathVariations) {
      assertLoggableProcessKey(binPath, "", "");
      assertLoggableProcessKey(binPath, "repo", "repo");
      assertLoggableProcessKey(binPath, "repo add", "repo add");
      assertLoggableProcessKey(binPath, "repo add --username abc", "repo add --username ***");
      assertLoggableProcessKey(binPath, "repo add -p abc", "repo add -p ***");
      assertLoggableProcessKey(binPath, "test --username abc@value -p zXc!@£$%^&&*()_+=-€# add -u Klm0@$",
          "test --username *** -p *** add -u ***");
    }
  }

  @After
  public void cleanup() {
    processExecutorService.shutdownNow();
    callerExecutorService.shutdownNow();
  }

  private void assertLoggableProcessKey(String binPath, String cmd, String expected) {
    String spacedCmd = isNotEmpty(cmd) ? " " + cmd : cmd;
    String spacedExpected = isNotEmpty(expected) ? " " + expected : expected;
    assertThat(sharedProcessRunner.getLoggableProcessKey(binPath + spacedCmd)).isEqualTo(binPath + spacedExpected);
  }

  private ProcessExecutorFactory createMockFactory(long sleepTimeMs) {
    ProcessExecutorFactory mockFactory = mock(ProcessExecutorFactory.class);
    doAnswer(ignore -> {
      ProcessExecutor mockExecutor = mock(ProcessExecutor.class);

      doAnswer(processIgnore -> {
        int processCount = createdProcesses.incrementAndGet();
        Thread.sleep(sleepTimeMs);
        return new ProcessResult(processCount, new ProcessOutput(format(PROCESS_RESULT_NAME, processCount).getBytes()));
      })
          .when(mockExecutor)
          .execute();

      return mockExecutor;
    })
        .when(mockFactory)
        .create();

    return mockFactory;
  }
}