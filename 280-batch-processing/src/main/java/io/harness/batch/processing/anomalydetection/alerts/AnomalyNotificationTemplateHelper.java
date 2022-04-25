package io.harness.batch.processing.anomalydetection.alerts;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AnomalyNotificationTemplateHelper {
  public static final String anomalyDetailsSlackTemplateBlock = "{"
      + "\"type\": \"section\","
      + "\"text\": {"
      + "\"type\": \"mrkdwn\","
      + "\"text\": \"%s\""
      + "}\n"
      + "}";

  public static final String anomalyDetailsEmailTemplateBlock =
      "<div style=\"color: #77787b; line-height: 30px; font-weight: normal;\">\n"
      + "%s"
      + "</div>";
}
