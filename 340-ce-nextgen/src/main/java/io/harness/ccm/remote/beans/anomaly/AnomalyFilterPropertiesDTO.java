/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.remote.beans.anomaly;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.filter.FilterConstants.ANOMALY_FILTER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.filter.FilterType;
import io.harness.filter.dto.FilterPropertiesDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(ANOMALY_FILTER)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("AnomalyFilterProperties")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@OwnedBy(CE)
@Schema(name = "AnomalyFilterProperties", description = "Properties of the Anomaly Filter defined in Harness")
public class AnomalyFilterPropertiesDTO extends FilterPropertiesDTO {
  List<String> k8sClusterNames;
  List<String> k8sNamespaces;
  List<String> k8sWorkloadNames;

  List<String> gcpProjects;
  List<String> gcpProducts;
  List<String> gcpSKUDescriptions;

  List<String> awsAccounts;
  List<String> awsServices;
  List<String> awsUsageTypes;

  List<String> azureSubscriptions;
  List<String> azureServiceNames;
  List<String> azureResources;

  Double minActualAmount;
  Double minAnomalousSpend;

  @Override
  public FilterType getFilterType() {
    return FilterType.ANOMALY;
  }
}
