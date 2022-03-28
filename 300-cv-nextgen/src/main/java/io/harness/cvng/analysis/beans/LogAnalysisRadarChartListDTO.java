/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.analysis.beans;

import java.util.Comparator;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LogAnalysisRadarChartListDTO {
  String message;
  int label;
  Risk risk;
  double radius;
  double angle;
  DeploymentLogAnalysisDTO.ClusterType clusterType;
  int count;
  List<Double> frequencyData;
  LogAnalysisRadarChartListDTO baseline;

  public boolean hasControlData() {
    if (baseline != null) {
      return true;
    }
    return false;
  }

  public static class LogAnalysisRadarChartListDTOComparator implements Comparator<LogAnalysisRadarChartListDTO> {
    @Override
    public int compare(LogAnalysisRadarChartListDTO o1, LogAnalysisRadarChartListDTO o2) {
      if (o1.getRisk().equals(o2.getRisk())) {
        return o1.getMessage().compareTo(o2.getMessage());
      } else if (o1.getRisk().isGreaterThan(o2.getRisk())) {
        return 1;
      } else {
        return -1;
      }
    }
  }
}
