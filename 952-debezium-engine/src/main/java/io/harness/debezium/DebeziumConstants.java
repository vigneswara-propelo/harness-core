package io.harness.debezium;

import lombok.experimental.UtilityClass;

@UtilityClass
public class DebeziumConstants {
  public static final String DEBEZIUM_PREFIX = "DEBEZIUM_";
  public static final int DEBEZIUM_TOPIC_SIZE = 10_000;

  public static final String DEBEZIUM_LOCK_PREFIX = "DEBEZIUM_LOCKER_";
}
