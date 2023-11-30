# Copyright 2023 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

"""
This has all clickhouse DDL Queries
"""

create_database = """
CREATE DATABASE IF NOT EXISTS `%s`
"""

create_unified_table = """
CREATE TABLE IF NOT EXISTS `%s`.`unifiedTable`
(
    `startTime` DateTime('UTC') NOT NULL,
    `cost` Float NULL,
    `gcpProduct` String NULL,
    `gcpSkuId` String NULL,
    `gcpSkuDescription` String NULL,
    `gcpProjectId` String NULL,
    `gcpInvoiceMonth` String NULL,
    `gcpCostType` String NULL,
    `region` String NULL,
    `zone` String NULL,
    `gcpBillingAccountId` String NULL,
    `cloudProvider` String NULL,
    `awsBlendedRate` String NULL,
    `awsBlendedCost` Float NULL,
    `awsUnblendedRate` String NULL,
    `awsUnblendedCost` Float NULL,
    `awsEffectiveCost` Float NULL,
    `awsAmortisedCost` Float NULL,
    `awsNetAmortisedCost` Float NULL,
    `awsLineItemType` String NULL,
    `awsServicecode` String NULL,
    `awsAvailabilityzone` String NULL,
    `awsUsageaccountid` String NULL,
    `awsInstancetype` String NULL,
    `awsUsagetype` String NULL,
    `awsBillingEntity` String NULL,
    `discount` Float NULL,
    `endtime` DateTime('UTC') NULL,
    `accountid` String NULL,
    `instancetype` String NULL,
    `clusterid` String NULL,
    `clustername` String NULL,
    `appid` String NULL,
    `serviceid` String NULL,
    `envid` String NULL,
    `cloudproviderid` String NULL,
    `launchtype` String NULL,
    `clustertype` String NULL,
    `workloadname` String NULL,
    `workloadtype` String NULL,
    `namespace` String NULL,
    `cloudservicename` String NULL,
    `taskid` String NULL,
    `clustercloudprovider` String NULL,
    `billingamount` Float NULL,
    `cpubillingamount` Float NULL,
    `memorybillingamount` Float NULL,
    `idlecost` Float NULL,
    `maxcpuutilization` Float NULL,
    `avgcpuutilization` Float NULL,
    `systemcost` Float NULL,
    `actualidlecost` Float NULL,
    `unallocatedcost` Float NULL,
    `networkcost` Float NULL,
    `product` String NULL,
    `azureMeterCategory` String NULL,
    `azureMeterSubcategory` String NULL,
    `azureMeterId` String NULL,
    `azureMeterName` String NULL,
    `azureResourceType` String NULL,
    `azureServiceTier` String NULL,
    `azureInstanceId` String NULL,
    `azureResourceGroup` String NULL,
    `azureSubscriptionGuid` String NULL,
    `azureAccountName` String NULL,
    `azureFrequency` String NULL,
    `azurePublisherType` String NULL,
    `azurePublisherName` String NULL,
    `azureServiceName` String NULL,
    `azureSubscriptionName` String NULL,
    `azureReservationId` String NULL,
    `azureReservationName` String NULL,
    `azureResource` String NULL,
    `azureVMProviderId` String NULL,
    `azureTenantId` String NULL,
    `azureBillingCurrency` String NULL,
    `azureCustomerName` String NULL,
    `azureResourceRate` Float NULL,
    `orgIdentifier` String NULL,
    `projectIdentifier` String NULL,
    `labels` Map(String, String)
)
ENGINE = MergeTree
ORDER BY tuple(startTime)
SETTINGS allow_nullable_key = 1
"""

create_pre_aggregated_table = """
CREATE TABLE IF NOT EXISTS `%s`.`preAggregated`
(
    `cost` Float NULL,
    `gcpProduct` String NULL,
    `gcpSkuId` String NULL,
    `gcpSkuDescription` String NULL,
    `startTime` DateTime('UTC') NULL,
    `gcpProjectId` String NULL,
    `region` String NULL,
    `zone` String NULL,
    `gcpBillingAccountId` String NULL,
    `cloudProvider` String NULL,
    `awsBlendedRate` String NULL,
    `awsBlendedCost` Float NULL,
    `awsUnblendedRate` String NULL,
    `awsUnblendedCost` Float NULL,
    `awsServicecode` String NULL,
    `awsAvailabilityzone` String NULL,
    `awsUsageaccountid` String NULL,
    `awsInstancetype` String NULL,
    `awsUsagetype` String NULL,
    `discount` Float NULL,
    `azureServiceName` String NULL,
    `azureResourceRate` Float NULL,
    `azureSubscriptionGuid` String NULL,
    `azureTenantId` String NULL
)
ENGINE = MergeTree
ORDER BY tuple(startTime)
SETTINGS allow_nullable_key = 1
"""

create_cost_aggregated_table = """
CREATE TABLE IF NOT EXISTS `%s`.`costAggregated`
(
    `accountId` String NULL,
    `cloudProvider` String NOT NULL,
    `cost` Float NOT NULL,
    `day` DateTime('UTC') NOT NULL
)
ENGINE = MergeTree
ORDER BY tuple(day)
SETTINGS allow_nullable_key = 1
"""

create_connector_data_sync_status_table = """
CREATE TABLE IF NOT EXISTS `%s`.`connectorDataSyncStatus`
(
    `accountId` String NULL,
    `connectorId` String NOT NULL,
    `jobType` String NULL,
    `cloudProviderId` String NULL,
    `lastSuccessfullExecutionAt` DateTime('UTC') NOT NULL
)
ENGINE = MergeTree
ORDER BY tuple(lastSuccessfullExecutionAt)
SETTINGS allow_nullable_key = 1
"""

create_azure_cost_table = """
CREATE TABLE `%s`.`%s`
(
    `azureSubscriptionGuid` String,
    `azureResourceGroup` String,
    `ResourceLocation` String,
    `startTime` Date,
    `MeterCategory` String,
    `MeterSubcategory` String,
    `MeterId` String,
    `MeterName` String,
    `MeterRegion` String,
    `UsageQuantity` Float64,
    `azureResourceRate` Float64,
    `cost` Float64,
    `ConsumedService` String,
    `ResourceType` String,
    `azureInstanceId` String,
    `Tags` String,
    `OfferId` String,
    `AdditionalInfo` String,
    `ServiceInfo1` String,
    `ServiceInfo2` String,
    `ServiceName` String,
    `ServiceTier` String,
    `Currency` String,
    `UnitOfMeasure` String
)
ENGINE = MergeTree
ORDER BY tuple(startTime)
SETTINGS allow_nullable_key = 1
"""
