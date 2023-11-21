/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.utils;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.logging.CommandExecutionStatus.RUNNING;

import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;

import java.io.Writer;
import javax.validation.constraints.NotNull;
import lombok.Builder;

@Builder
@OwnedBy(CDP)
public class ExecutionLogWriter extends Writer {
  private static final String WARNING = "WARNING";
  private static final String DEBUG = "DEBUG";
  private final LogCallback logCallback;
  @SuppressWarnings("PMD.AvoidStringBufferField") // This buffer is getting cleared on every newline.
  private final StringBuilder stringBuilder;
  private final LogLevel logLevel;
  private final String accountId;
  private final String appId;
  private final String executionId;
  private final String hostName;
  private final String commandUnitName;

  @Override
  public void write(@NotNull char[] cbuf, int off, int len) {
    stringBuilder.append(cbuf, off, len);
    char lastChar = cbuf[off + len - 1];
    if (lastChar == '\n') {
      logAndFlush();
    }
  }

  @Override
  public void flush() {
    logAndFlush();
  }

  @Override
  public void close() {
    logAndFlush();
  }

  private void logAndFlush() {
    String logLine = stringBuilder.toString();
    if (!logLine.isEmpty()) {
      LogLevel derivedLogLevel = getLogLevelFromLogLine(logLine);
      logCallback.saveExecutionLog(logLine.trim(), derivedLogLevel, RUNNING);
      stringBuilder.setLength(0);
    }
  }

  private LogLevel getLogLevelFromLogLine(String line) {
    if (line.startsWith(WARNING)) {
      return LogLevel.WARN;
    } else if (line.startsWith(DEBUG)) {
      return LogLevel.DEBUG;
    } else {
      return logLevel;
    }
  }
}
