package io.harness.debezium;

import io.harness.exception.InvalidRequestException;

import com.google.inject.Inject;

public class ChangeConsumerFactory {
  @Inject private DebeziumProducerFactory producerFactory;

  @SuppressWarnings("unchecked")
  public <T extends MongoDatabaseChangeConsumer> T get(String databaseName, ChangeConsumerConfig changeConsumerConfig) {
    ConsumerType consumerType = changeConsumerConfig.getConsumerType();
    switch (consumerType) {
      case EVENTS_FRAMEWORK:
        return (T) new EventsFrameworkChangeConsumer(databaseName, producerFactory);
      default:
        throw new InvalidRequestException("Change Consumer not Supported for " + consumerType.toString());
    }
  }
}
