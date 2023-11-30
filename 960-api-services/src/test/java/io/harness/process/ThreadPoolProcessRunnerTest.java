/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.process;

import static io.harness.rule.OwnerRule.ABOSII;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

public class ThreadPoolProcessRunnerTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock private ExecutorService executorService;
  private ThreadPoolProcessRunner threadPoolProcessRunner;

  @Before
  public void setup() {
    threadPoolProcessRunner = new ThreadPoolProcessRunner(executorService);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  @SneakyThrows
  public void testExecute() {
    final ProcessExecutor mockExecutor = mock(ProcessExecutor.class);
    final ProcessExecutorFactory factory = () -> mockExecutor;

    doReturn(new ProcessResult(0, null)).when(mockExecutor).execute();

    doAnswer(invocation -> {
      Future<ProcessResult> mockFuture = mock(Future.class);
      Callable<ProcessResult> callable = invocation.getArgument(0);
      doReturn(callable.call()).when(mockFuture).get();
      return mockFuture;
    })
        .when(executorService)
        .submit(any(Callable.class));

    try (ProcessRef ref = threadPoolProcessRunner.execute("test", factory)) {
      assertThat(ref.get().getExitValue()).isZero();
    }
  }
}