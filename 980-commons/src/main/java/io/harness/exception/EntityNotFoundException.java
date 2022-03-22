package io.harness.exception;

import static io.harness.eraro.ErrorCode.ENTITY_NOT_FOUND;

import io.harness.eraro.Level;

public class EntityNotFoundException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public EntityNotFoundException(String message) {
    super(message, null, ENTITY_NOT_FOUND, Level.ERROR, null, null);
    param(MESSAGE_KEY, message);
  }

  public EntityNotFoundException(String message, Throwable cause) {
    super(message, cause, ENTITY_NOT_FOUND, Level.ERROR, null, null);
  }
}
