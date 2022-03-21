package io.harness.debezium;

import io.harness.eventsframework.EventsFrameworkConfiguration;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChangeConsumerConfig {
  // If add consumer type the we need to introduce fields here
  // And the config should be
  // oneOf { EventsFrameworkConfiguration, HttpConfig }
  ConsumerType consumerType;
  EventsFrameworkConfiguration eventsFrameworkConfiguration;
}
