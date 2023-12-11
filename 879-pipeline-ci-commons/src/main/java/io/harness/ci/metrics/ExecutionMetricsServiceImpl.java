/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ci.metrics;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ci.metrics.beans.CIManagerStageMetricContext;
import io.harness.ci.metrics.beans.CIManagerStepMetricContext;
import io.harness.ci.metrics.beans.SecretAndConnectorMetricContext;
import io.harness.metrics.service.api.MetricService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
@Slf4j
@OwnedBy(HarnessTeam.CI)
public class ExecutionMetricsServiceImpl implements ExecutionMetricsService {
  private final MetricService metricService;

  public void recordStepExecutionCount(String status, String metricName, String accountId, String type) {
    try (CIManagerStepMetricContext ignore = new CIManagerStepMetricContext(status, accountId, type)) {
      metricService.incCounter(metricName);
    }
  }

  public void recordStepStatusExecutionTime(
      String status, double time, String metricName, String accountId, String type) {
    try (CIManagerStepMetricContext ignore = new CIManagerStepMetricContext(status, accountId, type)) {
      metricService.recordMetric(metricName, time);
    }
  }

  public void recordStageExecutionCount(String status, String metricName, String accountId, String type) {
    try (CIManagerStageMetricContext ignore = new CIManagerStageMetricContext(status, accountId, type)) {
      metricService.incCounter(metricName);
    }
  }
  public void recordStageStatusExecutionTime(
      String status, double time, String metricName, String accountId, String type) {
    try (CIManagerStageMetricContext ignore = new CIManagerStageMetricContext(status, accountId, type)) {
      metricService.recordMetric(metricName, time);
    }
  }

  public void recordSecretErrorCount(String accountId, String metricName) {
    try (SecretAndConnectorMetricContext ignore = new SecretAndConnectorMetricContext(accountId)) {
      metricService.incCounter(metricName);
    }
  }
  public void recordSecretLatency(String accountId, String metricName, double time) {
    try (SecretAndConnectorMetricContext ignore = new SecretAndConnectorMetricContext(accountId)) {
      metricService.recordMetric(metricName, time);
    }
  }
  public void recordConnectorErrorCount(String accountId, String metricName) {
    try (SecretAndConnectorMetricContext ignore = new SecretAndConnectorMetricContext(accountId)) {
      metricService.incCounter(metricName);
    }
  }
  public void recordConnectorLatency(String accountId, String metricName, double time) {
    try (SecretAndConnectorMetricContext ignore = new SecretAndConnectorMetricContext(accountId)) {
      metricService.recordMetric(metricName, time);
    }
  }
}
