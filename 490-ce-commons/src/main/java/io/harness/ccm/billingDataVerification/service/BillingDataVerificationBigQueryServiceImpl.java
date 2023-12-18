/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.service.impl;

import static io.harness.ccm.commons.utils.BigQueryHelper.AWS_BILLING_RAW_TABLE;
import static io.harness.ccm.commons.utils.BigQueryHelper.CCM_BILLING_DATA_VERIFICATION_TABLE;
import static io.harness.ccm.commons.utils.BigQueryHelper.UNIFIED_TABLE;

import io.harness.ccm.bigQuery.BigQueryService;
import io.harness.ccm.billingDataVerification.dto.CCMBillingDataVerificationCost;
import io.harness.ccm.billingDataVerification.dto.CCMBillingDataVerificationKey;
import io.harness.ccm.commons.utils.BigQueryHelper;
import io.harness.ccm.service.billingDataVerification.service.BillingDataVerificationSQLService;
import io.harness.connector.ConnectorResponseDTO;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class BillingDataVerificationBigQueryServiceImpl implements BillingDataVerificationSQLService {
  private static final String AWS_UNIFIED_TABLE_COST_VERIFICATION_QUERY_TEMPLATE = String.join(" ",
      "SELECT DATE_TRUNC(DATE(startTime), MONTH) as day, awsUsageAccountId as cloudProviderAccountId, ",
      "sum(IFNULL(awsUnblendedCost, 0)) as unblendedCost, sum(IFNULL(awsBlendedCost, 0)) as blendedCost, ",
      "sum(IFNULL(awsAmortisedcost, 0)) as amortizedcost, sum(IFNULL(awsNetamortisedcost, 0)) as netamortizedcost ",
      "FROM `%s` ", "WHERE DATE_TRUNC(DATE(startTime), DAY) >= DATE('%s')",
      "AND DATE_TRUNC(DATE(startTime), DAY) < DATE('%s')", "AND cloudprovider='AWS'",
      "GROUP BY day, cloudProviderAccountId ;");

  private static final String AWS_BILLING_COST_VERIFICATION_QUERY_TEMPLATE = String.join(" ",
      "SELECT DATE_TRUNC(DATE(usagestartdate), MONTH) as day, usageAccountId as cloudProviderAccountId, ",
      "sum(IFNULL(unblendedCost, 0)) as unblendedCost, sum(IFNULL(blendedCost, 0)) as blendedCost ", "FROM `%s` ",
      "WHERE DATE_TRUNC(DATE(usagestartdate), DAY) >= DATE('%s')",
      "AND DATE_TRUNC(DATE(usagestartdate), DAY) < DATE('%s')", "GROUP BY day, cloudProviderAccountId ;");

  private static final String INSERT_INTO_BILLING_DATA_VERIFICATION_TABLE_QUERY_TEMPLATE = String.join(" ",
      "INSERT INTO %s ",
      "(harnessAccountId, connectorId, cloudProvider, cloudProviderAccountId, usageStartDate, usageEndDate, costType, costFromCloudProviderAPI, costFromRawBillingTable, costFromUnifiedTable, lastUpdatedAt) ",
      "VALUES %s ; ");

  private static final int INSERT_BATCH_SIZE_FOR_BILLING_DATA_VERIFICATION_TABLE = 10;

  @Inject BigQueryHelper bigQueryHelper;
  @Inject private BigQueryService bigQueryService;

  @Override
  public Map<CCMBillingDataVerificationKey, CCMBillingDataVerificationCost> fetchAWSCostsFromAWSBillingTables(
      String accountId, ConnectorResponseDTO connector, String startDate, String endDate) throws Exception {
    String awsBillingTableId = bigQueryHelper.getCloudProviderTableName(
        accountId, String.format(AWS_BILLING_RAW_TABLE, connector.getConnector().getIdentifier(), "*"));
    String selectQuery =
        String.format(AWS_BILLING_COST_VERIFICATION_QUERY_TEMPLATE, awsBillingTableId, startDate, endDate);
    TableResult result = executeQuery(selectQuery);
    Map<CCMBillingDataVerificationKey, CCMBillingDataVerificationCost> awsBillingResults = new HashMap<>();
    for (FieldValueList row : result.iterateAll()) {
      // unblended cost
      CCMBillingDataVerificationKey unblendedCostKey =
          CCMBillingDataVerificationKey.builder()
              .harnessAccountId(accountId)
              .connectorId(connector.getConnector().getIdentifier())
              .cloudProvider("AWS")
              .cloudProviderAccountId(row.get("cloudProviderAccountId").getStringValue())
              .usageStartDate(LocalDate.parse(row.get("day").getStringValue()))
              .usageEndDate(LocalDate.parse(row.get("day").getStringValue()).plusMonths(1))
              .costType("AWSUnblendedCost")
              .build();
      awsBillingResults.put(unblendedCostKey,
          CCMBillingDataVerificationCost.builder()
              .costFromRawBillingTable(row.get("unblendedCost").getDoubleValue())
              .build());

      // blended cost
      CCMBillingDataVerificationKey blendedCostKey =
          CCMBillingDataVerificationKey.builder()
              .harnessAccountId(accountId)
              .connectorId(connector.getConnector().getIdentifier())
              .cloudProvider("AWS")
              .cloudProviderAccountId(row.get("cloudProviderAccountId").getStringValue())
              .usageStartDate(LocalDate.parse(row.get("day").getStringValue()))
              .usageEndDate(LocalDate.parse(row.get("day").getStringValue()).plusMonths(1))
              .costType("AWSBlendedCost")
              .build();
      awsBillingResults.put(blendedCostKey,
          CCMBillingDataVerificationCost.builder()
              .costFromRawBillingTable(row.get("blendedCost").getDoubleValue())
              .build());
    }
    return awsBillingResults;
  }

  @Override
  public Map<CCMBillingDataVerificationKey, CCMBillingDataVerificationCost> fetchAWSCostsFromUnifiedTable(
      String accountId, ConnectorResponseDTO connector, String startDate, String endDate) throws Exception {
    String unifiedTableId = bigQueryHelper.getCloudProviderTableName(accountId, UNIFIED_TABLE);
    String selectQuery =
        String.format(AWS_UNIFIED_TABLE_COST_VERIFICATION_QUERY_TEMPLATE, unifiedTableId, startDate, endDate);
    TableResult result = executeQuery(selectQuery);
    Map<CCMBillingDataVerificationKey, CCMBillingDataVerificationCost> awsUnifiedTableResults = new HashMap<>();
    for (FieldValueList row : result.iterateAll()) {
      // unblended cost
      CCMBillingDataVerificationKey unblendedCostKey =
          CCMBillingDataVerificationKey.builder()
              .harnessAccountId(accountId)
              .connectorId(null)
              .cloudProvider("AWS")
              .cloudProviderAccountId(row.get("cloudProviderAccountId").getStringValue())
              .usageStartDate(LocalDate.parse(row.get("day").getStringValue()))
              .usageEndDate(LocalDate.parse(row.get("day").getStringValue()).plusMonths(1))
              .costType("AWSUnblendedCost")
              .build();
      awsUnifiedTableResults.put(unblendedCostKey,
          CCMBillingDataVerificationCost.builder()
              .costFromUnifiedTable(row.get("unblendedCost").getDoubleValue())
              .build());

      // blended cost
      CCMBillingDataVerificationKey blendedCostKey =
          CCMBillingDataVerificationKey.builder()
              .harnessAccountId(accountId)
              .connectorId(null)
              .cloudProvider("AWS")
              .cloudProviderAccountId(row.get("cloudProviderAccountId").getStringValue())
              .usageStartDate(LocalDate.parse(row.get("day").getStringValue()))
              .usageEndDate(LocalDate.parse(row.get("day").getStringValue()).plusMonths(1))
              .costType("AWSBlendedCost")
              .build();
      awsUnifiedTableResults.put(blendedCostKey,
          CCMBillingDataVerificationCost.builder()
              .costFromUnifiedTable(row.get("blendedCost").getDoubleValue())
              .build());

      // amortizedcost cost
      CCMBillingDataVerificationKey amortizedCostKey =
          CCMBillingDataVerificationKey.builder()
              .harnessAccountId(accountId)
              .connectorId(null)
              .cloudProvider("AWS")
              .cloudProviderAccountId(row.get("cloudProviderAccountId").getStringValue())
              .usageStartDate(LocalDate.parse(row.get("day").getStringValue()))
              .usageEndDate(LocalDate.parse(row.get("day").getStringValue()).plusMonths(1))
              .costType("AWSAmortizedCost")
              .build();
      awsUnifiedTableResults.put(amortizedCostKey,
          CCMBillingDataVerificationCost.builder()
              .costFromUnifiedTable(row.get("amortizedcost").getDoubleValue())
              .build());

      // netamortizedcost cost
      CCMBillingDataVerificationKey netAmortizedCostKey =
          CCMBillingDataVerificationKey.builder()
              .harnessAccountId(accountId)
              .connectorId(null)
              .cloudProvider("AWS")
              .cloudProviderAccountId(row.get("cloudProviderAccountId").getStringValue())
              .usageStartDate(LocalDate.parse(row.get("day").getStringValue()))
              .usageEndDate(LocalDate.parse(row.get("day").getStringValue()).plusMonths(1))
              .costType("AWSNetAmortizedCost")
              .build();
      awsUnifiedTableResults.put(netAmortizedCostKey,
          CCMBillingDataVerificationCost.builder()
              .costFromUnifiedTable(row.get("netamortizedcost").getDoubleValue())
              .build());
    }
    return awsUnifiedTableResults;
  }

  @Override
  public void ingestAWSCostsIntoBillingDataVerificationTable(String accountId,
      Map<CCMBillingDataVerificationKey, CCMBillingDataVerificationCost> billingData) throws Exception {
    List<String> rows = new ArrayList<>();
    for (var entry : billingData.entrySet()) {
      rows.add(String.format("('%s', ", accountId)
                   .concat(String.format("'%s', ", entry.getKey().getConnectorId()))
                   .concat(String.format("'%s', ", "AWS"))
                   .concat(String.format("'%s', ", entry.getKey().getCloudProviderAccountId()))
                   .concat(String.format("'%s', ", entry.getKey().getUsageStartDate()))
                   .concat(String.format("'%s', ", entry.getKey().getUsageEndDate()))
                   .concat(String.format("'%s', ", entry.getKey().getCostType()))

                   .concat(String.format("%s, ", entry.getValue().getCostFromCloudProviderAPI()))
                   .concat(String.format("%s, ", entry.getValue().getCostFromRawBillingTable()))
                   .concat(String.format("%s, ", entry.getValue().getCostFromUnifiedTable()))
                   .concat("CURRENT_TIMESTAMP() )"));
      if (rows.size() >= INSERT_BATCH_SIZE_FOR_BILLING_DATA_VERIFICATION_TABLE) {
        insertRowsIntoBillingDataVerificationTable(rows);
        rows.clear();
      }
    }
    insertRowsIntoBillingDataVerificationTable(rows);
  }

  private void insertRowsIntoBillingDataVerificationTable(List<String> rows) throws Exception {
    if (!rows.isEmpty()) {
      String ccmBillingDataVerificationTableId =
          bigQueryHelper.getCEInternalDatasetTable(CCM_BILLING_DATA_VERIFICATION_TABLE);
      String insertQuery = String.format(INSERT_INTO_BILLING_DATA_VERIFICATION_TABLE_QUERY_TEMPLATE,
          ccmBillingDataVerificationTableId, String.join(", ", rows));
      executeQuery(insertQuery);
    }
  }

  private TableResult executeQuery(final String query) throws Exception {
    final BigQuery bigQuery = bigQueryService.get();
    QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();
    TableResult result;
    try {
      log.info("Executing Query: {}", query);
      result = bigQuery.query(queryConfig);
    } catch (final InterruptedException e) {
      log.error("Failed to execute query: {}", queryConfig, e);
      Thread.currentThread().interrupt();
      throw new Exception();
    }
    return result;
  }
}
