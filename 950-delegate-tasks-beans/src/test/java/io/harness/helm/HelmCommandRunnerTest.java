/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.helm;

import static io.harness.rule.OwnerRule.ABOSII;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.process.ProcessRef;
import io.harness.process.ProcessRunner;
import io.harness.process.RunProcessRequest;
import io.harness.rule.Owner;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.zeroturnaround.exec.ProcessResult;

public class HelmCommandRunnerTest extends CategoryTest {
  private static final long TEST_TIMEOUT_MILLIS = 10000;
  private static final Set<HelmCliCommandType> HANDLED_CMD_TYPES = Set.of(HelmCliCommandType.REPO_ADD,
      HelmCliCommandType.REPO_UPDATE, HelmCliCommandType.FETCH_ALL_VERSIONS, HelmCliCommandType.FETCH);
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private ProcessRunner localProcessRunner;
  @Mock private ProcessRunner threadPoolProcessRunner;
  @Mock private ProcessRunner sharedProcessRunner;

  private HelmCommandRunner commandRunner;

  @Before
  public void setup() {
    commandRunner = new HelmCommandRunner(localProcessRunner, threadPoolProcessRunner, sharedProcessRunner);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteRepoAdd() {
    expectSharedCommand(HelmCliCommandType.REPO_ADD, "helm repo add test", "pwd", Map.of("k1", "v1"));
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteRepoUpdate() {
    expectSharedCommand(
        HelmCliCommandType.REPO_UPDATE, "helm repo update test", "working/dir", Map.of("k1", "v1", "k2", "v2"));
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteRepoFetchAllVersions() {
    expectSharedCommand(HelmCliCommandType.FETCH_ALL_VERSIONS, "helm search repo test", "pwd", new HashMap<>());
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteFetch() {
    expectThreadPoolCommand(HelmCliCommandType.FETCH, "helm pull test", "pwd", new HashMap<>());
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testNonHandledCommands() {
    Set<HelmCliCommandType> typesToTest = Set.of(HelmCliCommandType.values())
                                              .stream()
                                              .filter(type -> !HANDLED_CMD_TYPES.contains(type))
                                              .collect(Collectors.toSet());

    typesToTest.forEach(type -> expectLocalCommand(type, "helm command", "pwd", Map.of("k1", "v1")));
  }

  private void expectSharedCommand(HelmCliCommandType type, String command, String pwd, Map<String, String> envs) {
    expectCommandRun(sharedProcessRunner, type, command, pwd, envs);
  }

  private void expectThreadPoolCommand(HelmCliCommandType type, String command, String pwd, Map<String, String> envs) {
    expectCommandRun(threadPoolProcessRunner, type, command, pwd, envs);
  }

  private void expectLocalCommand(HelmCliCommandType type, String command, String pwd, Map<String, String> envs) {
    expectCommandRun(localProcessRunner, type, command, pwd, envs);
  }

  @SneakyThrows
  private void expectCommandRun(
      ProcessRunner runner, HelmCliCommandType type, String command, String pwd, Map<String, String> envs) {
    Mockito.reset(runner);
    final ProcessRef mockedRef = setupRunner(runner, new ProcessResult(0, null));
    ProcessResult result = commandRunner.execute(type, command, pwd, envs, TEST_TIMEOUT_MILLIS);

    assertThat(result.getExitValue()).isZero();
    verify(mockedRef, times(1)).close();
    validateRunProcess(runner, command, pwd, envs);
  }

  @SneakyThrows
  private ProcessRef setupRunner(ProcessRunner mockRunner, ProcessResult expectedResult) {
    ProcessRef mockRef = mock(ProcessRef.class);

    doReturn(expectedResult).when(mockRef).get();

    doReturn(mockRef).when(mockRunner).run(any(RunProcessRequest.class));
    return mockRef;
  }

  private void validateRunProcess(ProcessRunner runner, String command, String pwd, Map<String, String> envs) {
    final ArgumentCaptor<RunProcessRequest> requestArgumentCaptor = ArgumentCaptor.forClass(RunProcessRequest.class);

    verify(runner).run(requestArgumentCaptor.capture());
    RunProcessRequest runProcessRequest = requestArgumentCaptor.getValue();
    assertThat(runProcessRequest.getCommand()).isEqualTo(command);
    assertThat(runProcessRequest.getPwd()).isEqualTo(pwd);
    assertThat(runProcessRequest.getEnvironment()).isEqualTo(envs);
  }
}