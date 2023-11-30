/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.process;

import static io.harness.rule.OwnerRule.ABOSII;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.PumpStreamHandler;

public class AbstractProcessRunnerTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private ProcessRef processRef;
  @Mock private OutputStream outputStream;
  @Spy private AbstractProcessRunner abstractProcessRunner;

  @Before
  public void setup() {
    doReturn(processRef).when(abstractProcessRunner).execute(anyString(), any(ProcessExecutorFactory.class));
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testRun() {
    final String pwd = "pwd";
    final String cmd = "cmd test";
    final Map<String, String> envs = Map.of("k1", "v1");
    final RunProcessRequest request = RunProcessRequest.builder()
                                          .pwd("pwd")
                                          .command(cmd)
                                          .environment(envs)
                                          .timeout(10)
                                          .timeoutTimeUnit(TimeUnit.HOURS)
                                          .readOutput(true)
                                          .outputStream(outputStream)
                                          .errorStream(outputStream)
                                          .build();

    ProcessRef result = abstractProcessRunner.run(request);
    ArgumentCaptor<ProcessExecutorFactory> factoryArgumentCaptor =
        ArgumentCaptor.forClass(ProcessExecutorFactory.class);
    ArgumentCaptor<String> processKeyCaptor = ArgumentCaptor.forClass(String.class);

    assertThat(result).isEqualTo(processRef);
    verify(abstractProcessRunner).execute(processKeyCaptor.capture(), factoryArgumentCaptor.capture());
    String processKey = processKeyCaptor.getValue();
    ProcessExecutorFactory factory = factoryArgumentCaptor.getValue();
    ProcessExecutor executor = factory.create();

    assertThat(processKey).isEqualTo(cmd);
    assertThat(executor.getCommand()).containsExactly("cmd", "test");
    assertThat(executor.getEnvironment()).isEqualTo(envs);
    assertThat(executor.getDirectory().getPath()).isEqualTo(pwd);
    assertThat(executor.streams()).isInstanceOf(PumpStreamHandler.class);
    PumpStreamHandler streamHandler = (PumpStreamHandler) executor.streams();
    assertThat(streamHandler.getErr()).isEqualTo(outputStream);
    assertThat(streamHandler.getOut()).isEqualTo(outputStream);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testRunNullValues() {
    final String cmd = "cmd test";
    final Map<String, String> envs = Map.of("k1", "v1");
    final RunProcessRequest request = RunProcessRequest.builder()
                                          .command(cmd)
                                          .environment(envs)
                                          .timeout(10)
                                          .timeoutTimeUnit(TimeUnit.MILLISECONDS)
                                          .readOutput(true)
                                          .build();

    ProcessRef result = abstractProcessRunner.run(request);
    ArgumentCaptor<ProcessExecutorFactory> factoryArgumentCaptor =
        ArgumentCaptor.forClass(ProcessExecutorFactory.class);
    ArgumentCaptor<String> processKeyCaptor = ArgumentCaptor.forClass(String.class);

    assertThat(result).isEqualTo(processRef);
    verify(abstractProcessRunner).execute(processKeyCaptor.capture(), factoryArgumentCaptor.capture());
    String processKey = processKeyCaptor.getValue();
    ProcessExecutorFactory factory = factoryArgumentCaptor.getValue();
    ProcessExecutor executor = factory.create();

    assertThat(processKey).isEqualTo(cmd);
    assertThat(executor.getCommand()).containsExactly("cmd", "test");
    assertThat(executor.getDirectory()).isNull();
  }
}