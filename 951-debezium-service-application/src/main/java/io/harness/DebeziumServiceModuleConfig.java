package io.harness;

import io.harness.eventsframework.EventsFrameworkConfiguration;
import io.harness.lock.DistributedLockImplementation;
import io.harness.redis.RedisConfig;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DebeziumServiceModuleConfig {
  RedisConfig redisLockConfig;
  DistributedLockImplementation lockImplementation;
  EventsFrameworkConfiguration eventsFrameworkConfiguration;
}
