/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.monitoring;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.metrics.service.api.MetricService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_PIPELINE})
@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
public class EventMonitoringServiceImpl implements EventMonitoringService {
  @Inject MetricService metricService;

  public void sendMetric(String metricName, Long metricValue) {
    try {
      metricService.recordMetric(metricName, metricValue);
    } catch (Exception ex) {
      log.error(String.format("Exception Occurred while recording metrics: [%s]", ex.getMessage()), ex);
    }
  }
}
