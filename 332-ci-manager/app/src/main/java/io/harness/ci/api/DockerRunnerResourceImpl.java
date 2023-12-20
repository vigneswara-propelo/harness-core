/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.app.resources;

import static io.harness.annotations.dev.HarnessTeam.CI;

import io.harness.account.AccountClient;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.runner.DockerRunnerResource;
import io.harness.delegate.beans.DelegateType;
import io.harness.remote.client.CGRestUtils;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;

@Slf4j
@OwnedBy(CI)
@NextGenManagerAuth
public class DockerRunnerResourceImpl implements DockerRunnerResource {
  @Inject private AccountClient accountClient;
  final String DELEGATE_TOKEN_PATTERN = "-e DELEGATE_TOKEN=([^\\s]+)";
  final String DELEGATE_IMAGE_PATTERN = "/delegate:(\\S+)";
  final String DELEGATE_DEFAULT_VERSION = "23.12.81604";

  public RestResponse<String> get(String accountId, String os, String arch) throws Exception {
    Call<RestResponse<Map<String, String>>> req =
        accountClient.getInstallationCommand(accountId, DelegateType.DOCKER, os, arch);
    Map<String, String> res = CGRestUtils.getResponse(req);
    String delegateCommand = res.get("command");
    String env = System.getProperty("ENV");
    String scriptUrl = getScriptUrl(env);
    String token = extractToken(delegateCommand, accountId);
    String delegateVersion = extractDelegateVersion(delegateCommand);
    String command = String.format("wget %s -O script.sh\n", scriptUrl)
        + String.format("sh script.sh %s %s %s", accountId, token, delegateVersion);

    return new RestResponse(command);
  }

  private String getScriptUrl(String env) {
    return String.format(
        "https://raw.githubusercontent.com/harness/harness-docker-runner/master/scripts/script-%s.sh", env);
  }

  private String extractToken(String command, String accountId) {
    Pattern pattern = Pattern.compile(DELEGATE_TOKEN_PATTERN);
    Matcher matcher = pattern.matcher(command);
    String token = "";
    if (matcher.find()) {
      token = matcher.group(1);
    } else {
      log.error("DELEGATE_TOKEN not found in the input string for account " + accountId);
    }
    return token;
  }

  private String extractDelegateVersion(String command) {
    Pattern pattern = Pattern.compile(DELEGATE_IMAGE_PATTERN);
    Matcher matcher = pattern.matcher(command);
    String version = DELEGATE_DEFAULT_VERSION;
    if (matcher.find()) {
      version = matcher.group(1);
    } else {
      log.error("DELEGATE_VERSION not found in the command, using default version");
    }
    return version;
  }
}
