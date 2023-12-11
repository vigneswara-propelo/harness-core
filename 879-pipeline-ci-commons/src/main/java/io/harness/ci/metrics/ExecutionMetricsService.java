/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ci.metrics;

public interface ExecutionMetricsService {
  void recordStepExecutionCount(String status, String metricName, String accountId, String type);
  void recordStepStatusExecutionTime(String status, double time, String metricName, String accountId, String type);
  void recordStageExecutionCount(String status, String metricName, String accountId, String type);
  void recordStageStatusExecutionTime(String status, double time, String metricName, String accountId, String type);
  void recordSecretErrorCount(String accountId, String metricName);
  void recordSecretLatency(String accountId, String metricName, double time);
  void recordConnectorErrorCount(String accountId, String metricName);
  void recordConnectorLatency(String accountId, String metricName, double time);
}
