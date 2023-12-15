/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine.executions.plan;

import static io.harness.engine.executions.plan.PlanExecutionMonitorServiceImpl.PLAN_EXECUTION;
import static io.harness.rule.OwnerRule.SHALINI;
import static io.harness.rule.OwnerRule.YUVRAJ;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.metrics.service.api.MetricService;
import io.harness.monitoring.ExecutionCountWithAccountResult;
import io.harness.pms.events.PmsEventMonitoringConstants;
import io.harness.rule.Owner;

import com.google.common.cache.LoadingCache;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.cache.Cache;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.PIPELINE)
public class PlanExecutionMonitorServiceImplTest extends CategoryTest {
  @Mock PlanExecutionService planExecutionService;
  @Mock MetricService metricService;

  @Mock Cache<String, Integer> metricsCache;
  @Mock LoadingCache<String, Set<String>> metricsLoadingCache;
  PlanExecutionMonitorService planExecutionMonitorService;

  @Before
  public void beforeTest() {
    MockitoAnnotations.openMocks(this);
    planExecutionMonitorService =
        new PlanExecutionMonitorServiceImpl(planExecutionService, metricService, metricsCache, metricsLoadingCache);
  }
  @Test
  @Owner(developers = SHALINI)
  @Category(UnitTests.class)
  public void testRegisterActiveExecutionMetrics() throws ExecutionException {
    doReturn(true).when(metricsCache).putIfAbsent(any(), any());

    List<ExecutionCountWithAccountResult> result = new LinkedList<>();
    result.add(ExecutionCountWithAccountResult.builder().accountId("ABC").count(1).build());
    result.add(ExecutionCountWithAccountResult.builder().accountId("DEF").count(5).build());

    doReturn(result).when(planExecutionService).aggregateRunningExecutionCountPerAccount();
    doReturn(new HashSet<>()).when(metricsLoadingCache).get(PmsEventMonitoringConstants.ACCOUNT_ID + PLAN_EXECUTION);

    planExecutionMonitorService.registerActiveExecutionMetrics();
    verify(metricService, times(2)).recordMetric(anyString(), anyDouble());
  }

  @Test
  @Owner(developers = YUVRAJ)
  @Category(UnitTests.class)
  public void testRegisterActiveExecutionMetricsWithZeroCount() throws ExecutionException {
    doReturn(true).when(metricsCache).putIfAbsent(any(), any());
    List<ExecutionCountWithAccountResult> result = new LinkedList<>();
    doReturn(result).when(planExecutionService).aggregateRunningExecutionCountPerAccount();
    doReturn(new HashSet<>(Collections.singleton("ABC")))
        .when(metricsLoadingCache)
        .get(PmsEventMonitoringConstants.ACCOUNT_ID + PLAN_EXECUTION);
    planExecutionMonitorService.registerActiveExecutionMetrics();
    verify(metricService, times(1)).recordMetric(anyString(), eq(0.0d));
  }
}
