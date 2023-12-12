/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.metrics.observers;

import static io.harness.rule.OwnerRule.TMACARI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.engine.observers.NodeUpdateInfo;
import io.harness.execution.NodeExecution;
import io.harness.metrics.PipelineMetricUtils;
import io.harness.pms.contracts.execution.Status;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.PIPELINE)
public class StepExecutionMetricsObserverTest extends CategoryTest {
  @Mock PipelineMetricUtils pipelineMetricUtils;
  @InjectMocks StepExecutionMetricsObserver stepExecutionMetricsObserver;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testOnEnd() {
    stepExecutionMetricsObserver.onNodeStatusUpdate(
        NodeUpdateInfo.builder().nodeExecution(NodeExecution.builder().status(Status.SUCCEEDED).build()).build());
    verify(pipelineMetricUtils)
        .publishStepExecutionMetrics(eq("step_execution_end_count"), any(), eq(Status.SUCCEEDED));
  }
}