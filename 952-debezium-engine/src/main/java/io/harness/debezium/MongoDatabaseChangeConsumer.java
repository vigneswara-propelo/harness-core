package io.harness.debezium;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;

public interface MongoDatabaseChangeConsumer extends DebeziumEngine.ChangeConsumer<ChangeEvent<String, String>> {
  String getDatabase();
}
