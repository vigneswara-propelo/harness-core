package software.wings.service.intfc.verification;

import software.wings.verification.CVActivityLog;

public interface CVActivityLogger {
  default void info(String message, long... timestampParams) {
    this.appendLog(CVActivityLog.LogLevel.INFO, message, timestampParams);
  }
  default void warn(String message, long... timestampParams) {
    this.appendLog(CVActivityLog.LogLevel.WARN, message, timestampParams);
  }
  default void error(String message, long... timestampParams) {
    this.appendLog(CVActivityLog.LogLevel.ERROR, message, timestampParams);
  }

  /**
   * use %t in the log message with timestamp params to localize timestamp in the UI.
   * @param logLevel
   * @param message
   * @param timestampParams epoch timestamp in millis
   */
  void appendLog(CVActivityLog.LogLevel logLevel, String message, long... timestampParams);
}
