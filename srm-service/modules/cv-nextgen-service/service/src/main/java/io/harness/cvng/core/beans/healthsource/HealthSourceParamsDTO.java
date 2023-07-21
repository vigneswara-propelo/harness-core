/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.beans.healthsource;

import io.harness.cvng.core.entities.HealthSourceParams;
import io.harness.cvng.utils.AggregationType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HealthSourceParamsDTO {
  String region;
  String metricName;
  String metricNamespace;
  AggregationType aggregationType;

  public static HealthSourceParamsDTO getHealthSourceParamsDTO(HealthSourceParams healthSourceParams) {
    if (healthSourceParams == null) {
      healthSourceParams = HealthSourceParams.builder().build();
    }
    return HealthSourceParamsDTO.builder()
        .region(healthSourceParams.getRegion())
        .metricName(healthSourceParams.getMetricName())
        .metricNamespace(healthSourceParams.getMetricName())
        .aggregationType(healthSourceParams.getAggregationType())
        .build();
  }

  @JsonIgnore
  public HealthSourceParams getHealthSourceParamsEntity() {
    return HealthSourceParams.builder()
        .region(region)
        .metricName(metricName)
        .metricNamespace(metricNamespace)
        .aggregationType(aggregationType)
        .build();
  }
}