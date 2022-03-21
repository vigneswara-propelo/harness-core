package io.harness;

import io.harness.debezium.ChangeConsumerConfig;
import io.harness.debezium.ConsumerType;
import io.harness.debezium.DebeziumConfig;
import io.harness.debezium.DebeziumEngineStarter;
import io.harness.lock.PersistentLocker;
import io.harness.maintenance.MaintenanceController;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.dropwizard.Application;
import io.dropwizard.setup.Environment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DebeziumServiceApplication extends Application<DebeziumServiceConfiguration> {
  public static void main(String[] args) throws Exception {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      log.info("Shutdown hook, entering maintenance...");
      MaintenanceController.forceMaintenance(true);
    }));

    new DebeziumServiceApplication().run(args);
  }

  @Override
  public void run(DebeziumServiceConfiguration appConfig, Environment environment) throws Exception {
    DebeziumServiceModuleConfig moduleConfig =
        DebeziumServiceModuleConfig.builder()
            .lockImplementation(appConfig.getDistributedLockImplementation())
            .redisLockConfig(appConfig.getRedisLockConfig())
            .eventsFrameworkConfiguration(appConfig.getEventsFrameworkConfiguration())
            .build();

    Injector injector = Guice.createInjector(DebeziumServiceModule.getInstance(moduleConfig));
    PersistentLocker locker = injector.getInstance(PersistentLocker.class);
    DebeziumEngineStarter starter = injector.getInstance(DebeziumEngineStarter.class);

    for (DebeziumConfig debeziumConfig : appConfig.getDebeziumConfigs()) {
      if (debeziumConfig.isEnabled()) {
        ChangeConsumerConfig changeConsumerConfig =
            ChangeConsumerConfig.builder()
                .consumerType(ConsumerType.EVENTS_FRAMEWORK)
                .eventsFrameworkConfiguration(appConfig.getEventsFrameworkConfiguration())
                .build();
        starter.startDebeziumEngine(debeziumConfig, changeConsumerConfig, locker);
      }
    }
  }
}