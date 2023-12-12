/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.metrics.observers;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.engine.observers.NodeStatusUpdateObserver;
import io.harness.engine.observers.NodeUpdateInfo;
import io.harness.metrics.PipelineMetricUtils;
import io.harness.pms.execution.utils.StatusUtils;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_PIPELINE})
@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class StepExecutionMetricsObserver implements NodeStatusUpdateObserver {
  private static final String STEP_EXECUTION_END_COUNT = "step_execution_end_count";

  @Inject PipelineMetricUtils pipelineMetricUtils;

  @Override
  public void onNodeStatusUpdate(NodeUpdateInfo nodeUpdateInfo) {
    if (StatusUtils.isFinalStatus(nodeUpdateInfo.getStatus())) {
      pipelineMetricUtils.publishStepExecutionMetrics(
          STEP_EXECUTION_END_COUNT, nodeUpdateInfo.getNodeExecution().getStepType(), nodeUpdateInfo.getStatus());
    }
  }
}