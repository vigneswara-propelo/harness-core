/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.remote.mapper.anomaly;

import static io.harness.rule.OwnerRule.ABHIJEET;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.remote.beans.anomaly.AnomalyFilterProperties;
import io.harness.ccm.remote.beans.anomaly.AnomalyFilterPropertiesDTO;
import io.harness.filter.FilterType;
import io.harness.filter.dto.FilterPropertiesDTO;
import io.harness.filter.entity.FilterProperties;
import io.harness.rule.Owner;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class AnomalyFilterPropertiesMapperTest extends CategoryTest {
  List<String> k8sClusterNames = Arrays.asList("k8sCluster1");
  List<String> k8sNamespaces = Arrays.asList("k8sNamespace1");
  List<String> k8sWorkloadNames = Arrays.asList("k8sWorkloadName1");
  List<String> gcpProjects = Arrays.asList("gcpProject1");
  List<String> gcpProducts = Arrays.asList("gcpProduct1");
  List<String> gcpSKUDescriptions = Arrays.asList("gcpSKUDescription1");
  List<String> awsAccounts = Arrays.asList("awsAccount1");
  List<String> awsServices = Arrays.asList("awsService1");
  List<String> awsUsageTypes = Arrays.asList("awsUsageType1");
  List<String> azureSubscriptions = Arrays.asList("azureSubscription1");
  List<String> azureServiceNames = Arrays.asList("azureServiceName1");
  List<String> azureResources = Arrays.asList("azureResource1");
  Double minActualAmount = 123.45;
  Double minAnomalousSpend = 1234.5;
  @InjectMocks AnomalyFilterPropertiesMapper anomalyFilterPropertiesMapper;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHIJEET)
  @Category(UnitTests.class)
  public void testWriteDTO() {
    FilterProperties filterProperties = AnomalyFilterProperties.builder()
                                            .k8sClusterNames(k8sClusterNames)
                                            .k8sNamespaces(k8sNamespaces)
                                            .k8sWorkloadNames(k8sWorkloadNames)
                                            .gcpProjects(gcpProjects)
                                            .gcpProducts(gcpProducts)
                                            .gcpSKUDescriptions(gcpSKUDescriptions)
                                            .awsAccounts(awsAccounts)
                                            .awsServices(awsServices)
                                            .awsUsageTypes(awsUsageTypes)
                                            .azureSubscriptions(azureSubscriptions)
                                            .azureServiceNames(azureServiceNames)
                                            .azureResources(azureResources)
                                            .minActualAmount(minActualAmount)
                                            .minAnomalousSpend(minAnomalousSpend)
                                            .build();
    AnomalyFilterPropertiesDTO filterPropertiesDTO =
        (AnomalyFilterPropertiesDTO) anomalyFilterPropertiesMapper.writeDTO(filterProperties);
    assertThat(filterPropertiesDTO.getFilterType()).isEqualTo(FilterType.ANOMALY);
    assertThat(filterPropertiesDTO.getK8sClusterNames()).isEqualTo(k8sClusterNames);
    assertThat(filterPropertiesDTO.getK8sNamespaces()).isEqualTo(k8sNamespaces);
    assertThat(filterPropertiesDTO.getK8sWorkloadNames()).isEqualTo(k8sWorkloadNames);
    assertThat(filterPropertiesDTO.getGcpProjects()).isEqualTo(gcpProjects);
    assertThat(filterPropertiesDTO.getGcpProducts()).isEqualTo(gcpProducts);
    assertThat(filterPropertiesDTO.getGcpSKUDescriptions()).isEqualTo(gcpSKUDescriptions);
    assertThat(filterPropertiesDTO.getAwsAccounts()).isEqualTo(awsAccounts);
    assertThat(filterPropertiesDTO.getAwsServices()).isEqualTo(awsServices);
    assertThat(filterPropertiesDTO.getAwsUsageTypes()).isEqualTo(awsUsageTypes);
    assertThat(filterPropertiesDTO.getAzureSubscriptions()).isEqualTo(azureSubscriptions);
    assertThat(filterPropertiesDTO.getAzureServiceNames()).isEqualTo(azureServiceNames);
    assertThat(filterPropertiesDTO.getAzureResources()).isEqualTo(azureResources);
    assertThat(filterPropertiesDTO.getMinActualAmount()).isEqualTo(minActualAmount);
    assertThat(filterPropertiesDTO.getMinAnomalousSpend()).isEqualTo(minAnomalousSpend);
  }

  @Test
  @Owner(developers = ABHIJEET)
  @Category(UnitTests.class)
  public void testToEntity() {
    FilterPropertiesDTO filterPropertiesDTO = AnomalyFilterPropertiesDTO.builder()
                                                  .k8sClusterNames(k8sClusterNames)
                                                  .k8sNamespaces(k8sNamespaces)
                                                  .k8sWorkloadNames(k8sWorkloadNames)
                                                  .gcpProjects(gcpProjects)
                                                  .gcpProducts(gcpProducts)
                                                  .gcpSKUDescriptions(gcpSKUDescriptions)
                                                  .awsAccounts(awsAccounts)
                                                  .awsServices(awsServices)
                                                  .awsUsageTypes(awsUsageTypes)
                                                  .azureSubscriptions(azureSubscriptions)
                                                  .azureServiceNames(azureServiceNames)
                                                  .azureResources(azureResources)
                                                  .minActualAmount(minActualAmount)
                                                  .minAnomalousSpend(minAnomalousSpend)
                                                  .build();
    AnomalyFilterProperties filterProperties =
        (AnomalyFilterProperties) anomalyFilterPropertiesMapper.toEntity(filterPropertiesDTO);
    assertThat(filterProperties.getK8sClusterNames()).isEqualTo(k8sClusterNames);
    assertThat(filterProperties.getK8sNamespaces()).isEqualTo(k8sNamespaces);
    assertThat(filterProperties.getK8sWorkloadNames()).isEqualTo(k8sWorkloadNames);
    assertThat(filterProperties.getGcpProjects()).isEqualTo(gcpProjects);
    assertThat(filterProperties.getGcpProducts()).isEqualTo(gcpProducts);
    assertThat(filterProperties.getGcpSKUDescriptions()).isEqualTo(gcpSKUDescriptions);
    assertThat(filterProperties.getAwsAccounts()).isEqualTo(awsAccounts);
    assertThat(filterProperties.getAwsServices()).isEqualTo(awsServices);
    assertThat(filterProperties.getAwsUsageTypes()).isEqualTo(awsUsageTypes);
    assertThat(filterProperties.getAzureSubscriptions()).isEqualTo(azureSubscriptions);
    assertThat(filterProperties.getAzureServiceNames()).isEqualTo(azureServiceNames);
    assertThat(filterProperties.getAzureResources()).isEqualTo(azureResources);
    assertThat(filterProperties.getMinActualAmount()).isEqualTo(minActualAmount);
    assertThat(filterProperties.getMinAnomalousSpend()).isEqualTo(minAnomalousSpend);
  }
}
