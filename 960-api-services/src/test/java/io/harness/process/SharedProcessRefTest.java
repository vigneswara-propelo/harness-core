/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.process;

import static io.harness.rule.OwnerRule.ABOSII;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.SneakyThrows;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.zeroturnaround.exec.ProcessResult;

public class SharedProcessRefTest extends CategoryTest {
  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testClose() {
    Future<ProcessResult> mockFuture = mock(Future.class);
    AtomicInteger refCounter = new AtomicInteger(3);
    Runnable callback = mock(Runnable.class);

    runProcessRef(mockFuture, refCounter, callback);
    verify(callback, never()).run();
    assertThat(refCounter.get()).isEqualTo(2);

    runProcessRef(mockFuture, refCounter, callback);
    verify(callback, never()).run();
    assertThat(refCounter.get()).isEqualTo(1);

    runProcessRef(mockFuture, refCounter, callback);
    verify(callback, times(1)).run();
    assertThat(refCounter.get()).isZero();
  }

  @SneakyThrows
  private void runProcessRef(Future<ProcessResult> future, AtomicInteger refCounter, Runnable callback) {
    try (SharedProcessRef processRef = new SharedProcessRef(future, refCounter, callback)) {
      processRef.get();
    }
  }
}