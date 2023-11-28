/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.shell;

import static io.harness.rule.OwnerRule.HINGER;
import static io.harness.rule.OwnerRule.SARTHAK_KASAT;
import static io.harness.rule.OwnerRule.VED;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;
import io.harness.shell.ssh.SshClientManager;
import io.harness.shell.ssh.connection.ExecRequest;
import io.harness.shell.ssh.connection.ExecResponse;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.powermock.api.mockito.PowerMockito;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

@RunWith(MockitoJUnitRunner.class)
public class ScriptSshExecutorTest extends CategoryTest {
  private static final String ENV_VAR_VALUE = "/some/valid/path";

  @Mock private SshSessionConfig sshSessionConfig;

  private ScriptSshExecutor scriptSshExecutor;

  @Mock private LogCallback logCallback;

  String APP_ID = "APP_ID";
  String ACCOUNT_ID = "ACCOUNT_ID";
  String ACTIVITY_ID = "ACTIVITY_ID";
  Map<String, String> ENV_VARS = Map.ofEntries(Map.entry("Path", ENV_VAR_VALUE), Map.entry("HOME", ENV_VAR_VALUE));

  @Before
  public void setup() throws Exception {
    when(sshSessionConfig.getExecutionId()).thenReturn("ID");
    scriptSshExecutor = spy(new ScriptSshExecutor(logCallback, true, sshSessionConfig));
  }

  @Test
  @Owner(developers = VED)
  @Category(UnitTests.class)
  public void shouldRecognizeEnvVarsInPathAndReplaceWithValuesExtractedFromSystem() throws Exception {
    new EnvironmentVariables(ENV_VARS).execute(() -> {
      assertThat(scriptSshExecutor.resolveEnvVarsInPath("$HOME")).isEqualTo(ENV_VAR_VALUE);
      assertThat(scriptSshExecutor.resolveEnvVarsInPath("$HOME/abc/$Path"))
          .isEqualTo(ENV_VAR_VALUE + "/abc" + ENV_VAR_VALUE);
      assertThat(scriptSshExecutor.resolveEnvVarsInPath("$HOME/work$Path/abc"))
          .isEqualTo(ENV_VAR_VALUE + "/work" + ENV_VAR_VALUE + "/abc");
    });
  }

  @Test
  @Owner(developers = VED)
  @Category(UnitTests.class)
  public void testGetMethods() {
    SshSessionConfig config = new SshSessionConfig();
    config.setAccountId(ACCOUNT_ID);
    config.setAppId(APP_ID);
    config.setExecutionId(ACTIVITY_ID);
    config.setWorkingDirectory("/tmp");
    config.setCommandUnitName("MyCommandUnit");
    config.setHost("host");

    ScriptSshExecutor executor = new ScriptSshExecutor(logCallback, false, config);

    assertThat(executor.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(executor.getExecutionId()).isEqualTo(ACTIVITY_ID);
    assertThat(executor.getHost()).isEqualTo("host");
    assertThat(executor.getCommandUnitName()).isEqualTo("MyCommandUnit");
  }

  @Test
  @Owner(developers = SARTHAK_KASAT)
  @Category(UnitTests.class)
  public void testExecuteCommandString() {
    String workingDirectory = "$HOME/tmp";
    String command = "echo 1";
    String commandWithWorkingDirectory = "cd \"$HOME/tmp\"\necho 1";

    SshSessionConfig config = new SshSessionConfig();
    config.setAccountId(ACCOUNT_ID);
    config.setAppId(APP_ID);
    config.setExecutionId(ACTIVITY_ID);
    config.setWorkingDirectory(workingDirectory);
    config.setCommandUnitName("MyCommandUnit");
    config.setHost("host");

    ScriptSshExecutor executor = new ScriptSshExecutor(logCallback, false, config);
    ExecRequest request = ExecRequest.builder().command(commandWithWorkingDirectory).displayCommand(false).build();
    ExecResponse response =
        ExecResponse.builder().status(CommandExecutionStatus.SUCCESS).exitCode(0).output("1").build();
    Mockito.mockStatic(SshClientManager.class);
    ArgumentCaptor<ExecRequest> requestArgumentCaptor = ArgumentCaptor.forClass(ExecRequest.class);
    when(SshClientManager.exec(any(), any(), any())).thenReturn(response);
    ExecuteCommandResponse executeCommandResponse = executor.executeCommandString(
        command, Collections.emptyList(), Collections.emptyList(), null, workingDirectory);
    PowerMockito.verifyStatic(SshClientManager.class, times(1));
    SshClientManager.exec(requestArgumentCaptor.capture(), any(), any());
    assertThat(executeCommandResponse).isNotNull();
    assertThat(executeCommandResponse.getStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    assertThat(requestArgumentCaptor.getValue().getCommand()).isEqualTo(commandWithWorkingDirectory);
  }

  @Test
  @Owner(developers = HINGER)
  @Category(UnitTests.class)
  public void testBuildingEnvironmentVars() {
    Map<String, String> envVars = new HashMap<>();
    envVars.put("var", "value");
    String e1 = scriptSshExecutor.buildExportForEnvironmentVariables(envVars);
    assertThat(e1).isEqualTo("export var=\"value\"\n");

    envVars.put("var", "spaced value");
    e1 = scriptSshExecutor.buildExportForEnvironmentVariables(envVars);
    assertThat(e1).isEqualTo("export var=\"spaced value\"\n");

    envVars.put("var", "\"#1$010");
    e1 = scriptSshExecutor.buildExportForEnvironmentVariables(envVars);
    assertThat(e1).isEqualTo("export var=\"\"#1$010\"\n");
  }
}
