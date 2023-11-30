/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.cvng.notification.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ErrorTrackingEventType {
  @JsonProperty("Exceptions") EXCEPTION("Exceptions"),
  @JsonProperty("LogErrors") LOG("Log Errors"),
  @JsonProperty("HttpErrors") HTTP("Http Errors"),
  @JsonProperty("CustomErrors") CUSTOM("Custom Errors"),
  @JsonProperty("TimeoutErrors") TIMER("Timeout Errors"),
  @JsonProperty("SwallowedExceptions") SWALLOWED_EXCEPTION("Swallowed Exceptions"),
  @JsonProperty("CaughtExceptions") CAUGHT_EXCEPTION("Caught Exceptions"),
  @JsonProperty("UncaughtExceptions") UNCAUGHT_EXCEPTION("Uncaught Exceptions"),
  @JsonProperty("LogWarnings") LOG_WARNING("Log Warnings");

  private final String displayName;

  ErrorTrackingEventType(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return this.displayName;
  }
}