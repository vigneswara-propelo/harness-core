/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine.executions.plan;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.metrics.service.api.MetricService;
import io.harness.monitoring.ExecutionCountWithAccountResult;
import io.harness.pms.events.PmsEventMonitoringConstants;
import io.harness.pms.events.base.PmsMetricContextGuard;

import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.cache.Cache;
import lombok.extern.slf4j.Slf4j;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_PIPELINE})
@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
@Slf4j
public class PlanExecutionMonitorServiceImpl implements PlanExecutionMonitorService {
  private static final String PIPELINE_EXECUTION_ACTIVE_COUNT = "pipeline_execution_active_count";
  public static final String PLAN_EXECUTION = "_plan_execution";
  private final PlanExecutionService planExecutionService;
  private final MetricService metricService;
  private final Cache<String, Integer> metricsCache;
  private final LoadingCache<String, Set<String>>
      metricsLoadingCache; // Cache to avoid retaining count in prometheus for a key in case count is 0 now

  @Inject
  public PlanExecutionMonitorServiceImpl(PlanExecutionService planExecutionService, MetricService metricService,
      @Named("pmsMetricsCache") Cache<String, Integer> metricsCache,
      @Named("pmsMetricsLoadingCache") LoadingCache<String, Set<String>> metricsLoadingCache) {
    this.planExecutionService = planExecutionService;
    this.metricService = metricService;
    this.metricsCache = metricsCache;
    this.metricsLoadingCache = metricsLoadingCache;
  }

  @Override
  public void registerActiveExecutionMetrics() {
    boolean alreadyMetricPublished = !metricsCache.putIfAbsent(PIPELINE_EXECUTION_ACTIVE_COUNT, 1);
    if (alreadyMetricPublished) {
      return;
    }

    List<ExecutionCountWithAccountResult> accountResultList =
        planExecutionService.aggregateRunningExecutionCountPerAccount();
    for (ExecutionCountWithAccountResult accountResult : accountResultList) {
      populateMetric(PmsEventMonitoringConstants.ACCOUNT_ID, accountResult.getAccountId(), accountResult.getCount());
    }
    populateCountForMissingKeysInCurrentExecutionStats(accountResultList);
  }

  private void populateCountForMissingKeysInCurrentExecutionStats(
      List<ExecutionCountWithAccountResult> accountResultList) {
    Set<String> currentAccountIds = new HashSet<>();
    for (ExecutionCountWithAccountResult accountResult : accountResultList) {
      currentAccountIds.add(accountResult.getAccountId());
    }
    populateZeroCount(currentAccountIds, PmsEventMonitoringConstants.ACCOUNT_ID);
  }

  private void populateZeroCount(Set<String> currentKeys, String metricKey) {
    try {
      Set<String> cachedKeys = metricsLoadingCache.get(metricKey + PLAN_EXECUTION);
      Set<String> zeroCountKeys = Sets.difference(cachedKeys, currentKeys);
      for (String key : zeroCountKeys) {
        populateMetric(metricKey, key, 0);
      }
      cachedKeys.addAll(currentKeys);
    } catch (Exception e) {
      log.error("Unable to populate zero count for metric {}", PIPELINE_EXECUTION_ACTIVE_COUNT);
    }
  }

  private void populateMetric(String key, String keyValue, Integer metricValue) {
    Map<String, String> metricContextMap = ImmutableMap.<String, String>builder().put(key, keyValue).build();

    try (PmsMetricContextGuard pmsMetricContextGuard = new PmsMetricContextGuard(metricContextMap)) {
      metricService.recordMetric(PIPELINE_EXECUTION_ACTIVE_COUNT, metricValue);
    }
  }
}
