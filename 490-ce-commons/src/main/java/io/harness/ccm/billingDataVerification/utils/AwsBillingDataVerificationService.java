/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.billingDataVerification.utils;

import static software.wings.service.impl.aws.model.AwsConstants.AWS_DEFAULT_REGION;

import io.harness.ccm.billingDataVerification.dto.CCMBillingDataVerificationCost;
import io.harness.ccm.billingDataVerification.dto.CCMBillingDataVerificationKey;
import io.harness.ccm.service.billingDataVerification.service.BillingDataVerificationSQLService;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsConnectorDTO;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.costexplorer.AWSCostExplorer;
import com.amazonaws.services.costexplorer.AWSCostExplorerClientBuilder;
import com.amazonaws.services.costexplorer.model.DateInterval;
import com.amazonaws.services.costexplorer.model.GetCostAndUsageRequest;
import com.amazonaws.services.costexplorer.model.GetCostAndUsageResult;
import com.amazonaws.services.costexplorer.model.Granularity;
import com.amazonaws.services.costexplorer.model.GroupDefinition;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.LocalDate;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Singleton
@Slf4j
public class AwsBillingDataVerificationService {
  @Inject BillingDataVerificationSQLService billingDataVerificationSQLService;

  public void fetchAndUpdateBillingDataForConnector(String accountId, ConnectorResponseDTO connector, String startDate,
      String endDate, AWSCredentialsProvider awsAssumedCredentialsProvider,
      Map<CCMBillingDataVerificationKey, CCMBillingDataVerificationCost> billingData) throws Exception {
    // startDate is inclusive, endDate is exclusive
    ConnectorInfoDTO connectorInfo = connector.getConnector();
    CEAwsConnectorDTO ceAwsConnectorDTO = (CEAwsConnectorDTO) connectorInfo.getConnectorConfig();
    if (ceAwsConnectorDTO != null && ceAwsConnectorDTO.getCrossAccountAccess() != null) {
      // for the connector, fetch data from different sources and update results in billingData Map

      fetchBillingDataFromAWSCostExplorerAPI(
          accountId, connector, startDate, endDate, awsAssumedCredentialsProvider, billingData);

      fetchBillingDataFromAWSBillingTables(accountId, connector, startDate, endDate, billingData);

      fetchAWSBillingDataFromUnifiedTable(accountId, connector, startDate, endDate, billingData);
    }
  }

  public CCMBillingDataVerificationCost mergeCostDTOs(
      CCMBillingDataVerificationCost c1, CCMBillingDataVerificationCost c2) {
    c1.setCostFromRawBillingTable(
        c2.getCostFromRawBillingTable() != null ? c2.getCostFromRawBillingTable() : c1.getCostFromRawBillingTable());
    c1.setCostFromUnifiedTable(
        c2.getCostFromUnifiedTable() != null ? c2.getCostFromUnifiedTable() : c1.getCostFromUnifiedTable());
    c1.setCostFromCloudProviderAPI(
        c2.getCostFromCloudProviderAPI() != null ? c2.getCostFromCloudProviderAPI() : c1.getCostFromCloudProviderAPI());
    return c1;
  }

  public void mergeAWSBillingResultsIntoBillingData(
      Map<CCMBillingDataVerificationKey, CCMBillingDataVerificationCost> queryResults,
      Map<CCMBillingDataVerificationKey, CCMBillingDataVerificationCost> billingData) {
    queryResults.forEach(
        (key, value) -> { billingData.put(key, mergeCostDTOs(value, billingData.getOrDefault(key, value))); });
  }

  public void mergeUnifiedTableResultsIntoBillingData(
      Map<CCMBillingDataVerificationKey, CCMBillingDataVerificationCost> queryResults,
      Map<CCMBillingDataVerificationKey, CCMBillingDataVerificationCost> billingData) {
    billingData.replaceAll((key, value) -> {
      CCMBillingDataVerificationKey unifiedTableResultsKey =
          CCMBillingDataVerificationKey.builder()
              .harnessAccountId(key.getHarnessAccountId())
              .connectorId(null)
              .cloudProvider(key.getCloudProvider())
              .cloudProviderAccountId(key.getCloudProviderAccountId())
              .usageStartDate(key.getUsageStartDate())
              .usageEndDate(key.getUsageEndDate())
              .costType(key.getCostType())
              .build();
      return mergeCostDTOs(value, queryResults.getOrDefault(unifiedTableResultsKey, value));
    });
  }

  public void fetchAWSBillingDataFromUnifiedTable(String accountId, ConnectorResponseDTO connector, String startDate,
      String endDate, Map<CCMBillingDataVerificationKey, CCMBillingDataVerificationCost> billingData) throws Exception {
    Map<CCMBillingDataVerificationKey, CCMBillingDataVerificationCost> awsUnifiedTableResults =
        billingDataVerificationSQLService.fetchAWSCostsFromUnifiedTable(accountId, connector, startDate, endDate);
    mergeUnifiedTableResultsIntoBillingData(awsUnifiedTableResults, billingData);
  }

  public void fetchBillingDataFromAWSBillingTables(String accountId, ConnectorResponseDTO connector, String startDate,
      String endDate, Map<CCMBillingDataVerificationKey, CCMBillingDataVerificationCost> billingData) throws Exception {
    Map<CCMBillingDataVerificationKey, CCMBillingDataVerificationCost> awsBillingResults =
        billingDataVerificationSQLService.fetchAWSCostsFromAWSBillingTables(accountId, connector, startDate, endDate);
    mergeAWSBillingResultsIntoBillingData(awsBillingResults, billingData);
  }

  public CCMBillingDataVerificationCost createNewCCMBillingDataVerificationCost(
      Double costFromRawBillingTable, Double costFromUnifiedTable, Double costFromCloudProviderAPI) {
    return CCMBillingDataVerificationCost.builder()
        .costFromRawBillingTable(costFromRawBillingTable)
        .costFromUnifiedTable(costFromUnifiedTable)
        .costFromCloudProviderAPI(costFromCloudProviderAPI)
        .build();
  }

  public void fetchBillingDataFromAWSCostExplorerAPI(String accountId, ConnectorResponseDTO connector, String startDate,
      String endDate, AWSCredentialsProvider awsAssumedCredentialsProvider,
      Map<CCMBillingDataVerificationKey, CCMBillingDataVerificationCost> billingData) {
    final GetCostAndUsageRequest awsCERequest =
        new GetCostAndUsageRequest()
            .withTimePeriod(new DateInterval().withStart(startDate).withEnd(endDate))
            .withGranularity(Granularity.MONTHLY)
            .withMetrics(new String[] {"UnblendedCost", "BlendedCost", "AmortizedCost", "NetAmortizedCost"})
            .withGroupBy(new GroupDefinition().withType("DIMENSION").withKey("LINKED_ACCOUNT"));

    try {
      AWSCostExplorer ce = AWSCostExplorerClientBuilder.standard()
                               .withRegion(AWS_DEFAULT_REGION)
                               .withCredentials(awsAssumedCredentialsProvider)
                               .build();
      boolean isNextPageAvailable = true;
      while (isNextPageAvailable) {
        GetCostAndUsageResult result = ce.getCostAndUsage(awsCERequest);
        result.getResultsByTime().forEach(resultByTime -> {
          resultByTime.getGroups().forEach(group -> {
            String awsUsageAccountId = group.getKeys().get(0);

            // UnblendedCost
            CCMBillingDataVerificationKey unblendedCostKey =
                CCMBillingDataVerificationKey.builder()
                    .harnessAccountId(accountId)
                    .connectorId(connector.getConnector().getIdentifier())
                    .cloudProvider("AWS")
                    .cloudProviderAccountId(awsUsageAccountId)
                    .usageStartDate(LocalDate.parse(resultByTime.getTimePeriod().getStart()))
                    .usageEndDate(LocalDate.parse(resultByTime.getTimePeriod().getEnd()))
                    .costType("AWSUnblendedCost")
                    .build();
            billingData.put(unblendedCostKey,
                mergeCostDTOs(billingData.getOrDefault(
                                  unblendedCostKey, createNewCCMBillingDataVerificationCost(null, null, null)),
                    createNewCCMBillingDataVerificationCost(
                        null, null, Double.parseDouble(group.getMetrics().get("UnblendedCost").getAmount()))));

            // BlendedCost
            CCMBillingDataVerificationKey blendedCostKey =
                CCMBillingDataVerificationKey.builder()
                    .harnessAccountId(accountId)
                    .connectorId(connector.getConnector().getIdentifier())
                    .cloudProvider("AWS")
                    .cloudProviderAccountId(awsUsageAccountId)
                    .usageStartDate(LocalDate.parse(resultByTime.getTimePeriod().getStart()))
                    .usageEndDate(LocalDate.parse(resultByTime.getTimePeriod().getEnd()))
                    .costType("AWSBlendedCost")
                    .build();
            billingData.put(blendedCostKey,
                mergeCostDTOs(
                    billingData.getOrDefault(blendedCostKey, createNewCCMBillingDataVerificationCost(null, null, null)),
                    createNewCCMBillingDataVerificationCost(
                        null, null, Double.parseDouble(group.getMetrics().get("BlendedCost").getAmount()))));

            // NetAmortizedCost
            CCMBillingDataVerificationKey netAmortizedCostKey =
                CCMBillingDataVerificationKey.builder()
                    .harnessAccountId(accountId)
                    .connectorId(connector.getConnector().getIdentifier())
                    .cloudProvider("AWS")
                    .cloudProviderAccountId(awsUsageAccountId)
                    .usageStartDate(LocalDate.parse(resultByTime.getTimePeriod().getStart()))
                    .usageEndDate(LocalDate.parse(resultByTime.getTimePeriod().getEnd()))
                    .costType("AWSNetAmortizedCost")
                    .build();
            billingData.put(netAmortizedCostKey,
                mergeCostDTOs(billingData.getOrDefault(
                                  netAmortizedCostKey, createNewCCMBillingDataVerificationCost(null, null, null)),
                    createNewCCMBillingDataVerificationCost(
                        null, null, Double.parseDouble(group.getMetrics().get("NetAmortizedCost").getAmount()))));

            // AmortizedCost
            CCMBillingDataVerificationKey amortizedCostKey =
                CCMBillingDataVerificationKey.builder()
                    .harnessAccountId(accountId)
                    .connectorId(connector.getConnector().getIdentifier())
                    .cloudProvider("AWS")
                    .cloudProviderAccountId(awsUsageAccountId)
                    .usageStartDate(LocalDate.parse(resultByTime.getTimePeriod().getStart()))
                    .usageEndDate(LocalDate.parse(resultByTime.getTimePeriod().getEnd()))
                    .costType("AWSAmortizedCost")
                    .build();
            billingData.put(amortizedCostKey,
                mergeCostDTOs(billingData.getOrDefault(
                                  amortizedCostKey, createNewCCMBillingDataVerificationCost(null, null, null)),
                    createNewCCMBillingDataVerificationCost(
                        null, null, Double.parseDouble(group.getMetrics().get("AmortizedCost").getAmount()))));
          });
        });
        if (result.getNextPageToken() == null) {
          isNextPageAvailable = false;
        }
        awsCERequest.withNextPageToken(result.getNextPageToken());
      }
      ce.shutdown();
    } catch (final Exception e) {
      log.error("Exception while fetching billing-data from AWS Cost Explorer (accountId: {}, connectorId: {})",
          accountId, connector.getConnector().getIdentifier(), e);
    }
  }
}
