package io.harness.debezium;

import io.harness.eventsframework.EventsFrameworkConfiguration;
import io.harness.eventsframework.impl.redis.RedisProducerFactory;
import io.harness.lock.PersistentLocker;
import io.harness.threading.ThreadPool;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class DebeziumEngineModule extends AbstractModule {
  private static DebeziumEngineModule instance;

  private final DebeziumEngineModuleConfig config;

  private DebeziumEngineModule(DebeziumEngineModuleConfig config) {
    this.config = config;
  }

  public static DebeziumEngineModule getInstance(DebeziumEngineModuleConfig config) {
    if (instance == null) {
      instance = new DebeziumEngineModule(config);
    }
    return instance;
  }

  @Override
  public void configure() {
    requireBinding(PersistentLocker.class);
    requireBinding(RedisProducerFactory.class);
  }

  @Provides
  @Singleton
  @Named("DebeziumExecutorService")
  public ExecutorService executorService() {
    return ThreadPool.create(
        1, 200, 120, TimeUnit.SECONDS, new ThreadFactoryBuilder().setNameFormat("debezium-thread-%d").build());
  }

  @Provides
  @Singleton
  public EventsFrameworkConfiguration eventsFrameworkConfiguration() {
    return config.getEventsFrameworkConfiguration();
  }
}
