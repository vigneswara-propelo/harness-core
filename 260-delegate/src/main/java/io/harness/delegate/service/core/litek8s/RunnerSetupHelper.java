/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.service.core.litek8s;

import static java.lang.String.format;

import io.harness.delegate.service.core.k8s.K8SService;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

@Slf4j
public class RunnerSetupHelper {
  private static final String MANAGER_URL = "https://host.docker.internal:9090/api/";
  private static final String LOG_SERVICE_URL = "http://host.docker.internal:8079";
  private static final String LOCAL_DEV_SKIP_SSL_VERIFY = "LE_SKIP_VERIFY_MANAGER";

  public static void portForward(String serviceName, String namespace) {
    StartedProcess process = null;
    try {
      log.info("Starting port forwarding for local bijou setup");
      process = new ProcessExecutor()
                    .command("nohup", "kubectl", "port-forward", String.format("service/%s", serviceName),
                        "20001:20001", "-n", namespace)
                    .redirectError(Slf4jStream.ofCaller().asError())
                    .redirectOutput(Slf4jStream.ofCaller().asInfo())
                    .readOutput(true)
                    .setMessageLogger((log, format, arguments) -> log.info(format, arguments))
                    .start();

    } catch (IOException | RuntimeException e) {
      log.error("Exception while doing port forward", e);
      if (process != null) {
        try {
          process.getProcess().destroy();
          process.getProcess().waitFor();
        } catch (Exception ex) {
          // ignore
        }
        try {
          if (process.getProcess().isAlive()) {
            process.getProcess().destroyForcibly();
            if (process.getProcess() != null) {
              process.getProcess().waitFor();
            }
          }
        } catch (Exception ex) {
          log.error("ALERT: Couldn't kill forcibly", ex);
        }
      }
    }
  }

  public static String fetchLogServiceUrl(String originalLogServiceUrl, boolean isLocalBijouRunner) {
    return isLocalBijouRunner ? LOG_SERVICE_URL : originalLogServiceUrl;
  }

  public static String fetchManagerUrl(String originalManagerUrl, boolean isLocalBijouRunner) {
    return isLocalBijouRunner ? MANAGER_URL : originalManagerUrl;
  }

  public static String fetchServiceTarget(
      String infraId, String namespace, int reservedLePort, boolean isLocalBijouRunner) {
    String RemoteServiceTarget = K8SService.buildK8sServiceUrl(infraId, namespace, Integer.toString(reservedLePort));
    String localServiceTarget = format("%s:%d", "127.0.0.1", reservedLePort);
    return isLocalBijouRunner ? localServiceTarget : RemoteServiceTarget;
  }

  public static void populateLocalLeEnvVars(ImmutableMap.Builder<String, String> envVars) {
    envVars.put(LOCAL_DEV_SKIP_SSL_VERIFY, "true");
  }
}
