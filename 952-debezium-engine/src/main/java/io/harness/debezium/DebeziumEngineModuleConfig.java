package io.harness.debezium;

import io.harness.eventsframework.EventsFrameworkConfiguration;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DebeziumEngineModuleConfig {
  EventsFrameworkConfiguration eventsFrameworkConfiguration;
}
