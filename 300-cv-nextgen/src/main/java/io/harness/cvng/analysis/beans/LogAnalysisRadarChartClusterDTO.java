/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.analysis.beans;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LogAnalysisRadarChartClusterDTO {
  private int label;
  String message;
  Risk risk;
  double radius;
  double angle;
  LogAnalysisRadarChartClusterDTO baseline;
  DeploymentLogAnalysisDTO.ClusterType clusterType;

  public boolean haveControlData() {
    if (baseline != null) {
      return true;
    }
    return false;
  }
}
