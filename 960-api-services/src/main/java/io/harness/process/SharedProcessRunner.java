/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.process;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;

import com.google.common.annotations.VisibleForTesting;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

@Slf4j
@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_K8S})
public class SharedProcessRunner extends AbstractProcessRunner {
  private static final Pattern CMD_ARGS_PATTERN = Pattern.compile("\\s(?:--|-)[A-z0-9-_.]+(?:\\s|=)(.+?)(?=\\s|$)");

  private final ExecutorService executorService;

  @Getter private final Map<String, RunningProcessHandler> runningProcesses = new ConcurrentHashMap<>();

  public SharedProcessRunner(ExecutorService executorService) {
    this.executorService = executorService;
  }

  @Override
  protected ProcessRef execute(String processKey, ProcessExecutorFactory processFactory) {
    RunningProcessHandler processHandler =
        runningProcesses.compute(processKey, (ignore, current) -> submitProcess(current, processFactory));
    return new SharedProcessRef(processHandler.getProcessFeature(), processHandler.getRefCounter(),
        createCloseRunnable(processKey, processHandler));
  }

  private RunningProcessHandler submitProcess(
      final RunningProcessHandler current, final ProcessExecutorFactory factory) {
    if (current == null) {
      Future<ProcessResult> processFeature = executorService.submit(() -> {
        ProcessExecutor exec = factory.create();
        return exec.execute();
      });

      return new RunningProcessHandler(processFeature, new AtomicInteger(1));
    }

    current.getRefCounter().incrementAndGet();
    return current;
  }

  private Runnable createCloseRunnable(final String processKey, final RunningProcessHandler handler) {
    return () -> close(processKey, handler);
  }

  private void close(final String processKey, final RunningProcessHandler handler) {
    if (handler == null) {
      return;
    }

    // prevent other threads to execute further logic
    if (!handler.getRefCounter().compareAndSet(0, -1)) {
      log.warn(
          "Either close method is called multiple times or while calling close other thread acquired process ref. Current ref count {}",
          handler.getRefCounter().get());
      return;
    }

    String loggableProcessKey = getLoggableProcessKey(processKey);
    if (!runningProcesses.remove(processKey, handler)) {
      log.warn("Process [{}] is already removed from the running processes map", loggableProcessKey);
    }

    if (!handler.getProcessFeature().isDone() && !handler.getProcessFeature().isCancelled()) {
      log.info("Interrupt process {}", loggableProcessKey);
      handler.getProcessFeature().cancel(true);
    }
  }

  @VisibleForTesting
  String getLoggableProcessKey(String processKey) {
    // mask all the process key arguments as it may contain sensitive information
    Matcher argsMatcher = CMD_ARGS_PATTERN.matcher(processKey);
    String loggableProcessKey = processKey;
    while (argsMatcher.find()) {
      if (argsMatcher.groupCount() > 0) {
        String argumentValue = argsMatcher.group(1);
        loggableProcessKey = loggableProcessKey.replace(argumentValue, "***");
      }
    }

    return loggableProcessKey;
  }

  @Value
  @AllArgsConstructor
  private static class RunningProcessHandler {
    Future<ProcessResult> processFeature;
    AtomicInteger refCounter;
  }
}
