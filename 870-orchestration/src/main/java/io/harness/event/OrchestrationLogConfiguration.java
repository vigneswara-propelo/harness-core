package io.harness.event;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrchestrationLogConfiguration {
  boolean shouldUseBatching;
  @Builder.Default int orchestrationLogBatchSize = 5;
  boolean reduceOrchestrationLog;
}
