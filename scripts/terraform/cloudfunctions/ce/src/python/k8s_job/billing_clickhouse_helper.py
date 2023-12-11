# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

import os
import json
import datetime
import clickhouse_connect
from tzlocal import get_localzone
from google.cloud import storage
from google.cloud import bigquery
from google.cloud.client import Client
from google.oauth2 import service_account
from clickhouse_driver.errors import ServerException, ErrorCodes
from typing import Optional, Any, Union, Sequence, Dict, Final
from billing_helper import BillingHelper
from clickhouse_connect.driver.query import QueryResult
from google.cloud.bigquery.table import TableReference
from k8sCronJob.ch_ddl_queries import create_unified_table, create_pre_aggregated_table, \
    create_connector_data_sync_status_table, create_cost_aggregated_table, create_gcp_billing_export_table, create_gcp_cost_export_table
from util import print__, update_connector_data_sync_status_clickhouse, run_bq_query_with_retries, \
    COSTAGGREGATED, UNIFIED, CONNECTORDATASYNCSTATUSTABLE, GCPCOSTPREFIX, PREAGGREGATED

class BillingClickHouseHelper(BillingHelper):
    # TODO: Replace print statement with logging
    DATABASE: str = 'ccm'
    TABLES_SUPPORTED: Final[str] = (UNIFIED, PREAGGREGATED, COSTAGGREGATED, CONNECTORDATASYNCSTATUSTABLE)
    PREFIX_TABLES_SUPPORTED: Final[str] = (GCPCOSTPREFIX)
    SUB_FOLDER_PATTERN: str = '%d-%m-%Y_%H:%M:%S'
    DATA_FILE_PATH: str = '%s/%s/data-*.parquet'
    LOCAL_TIMEZONE: str = get_localzone()
    # https://cloud.google.com/billing/docs/how-to/export-data-bigquery-tables/detailed-usage
    GCP_STANDARD_EXPORT_BIGQUERY_COLUMNS: Final[str] = ["billing_account_id", "usage_start_time", "usage_end_time", "export_time",
                                "cost", "currency", "currency_conversion_rate", "cost_type", "labels",
                                "system_labels", "credits", "usage", "invoice", "adjustment_info",
                                "service", "sku", "project", "location", "cost_at_list", "transaction_type", "seller_name", "tags"]
    GCP_DETAILED_EXPORT_BIGQUERY_COLUMNS: Final[str] = GCP_STANDARD_EXPORT_BIGQUERY_COLUMNS + ["resource", "price"]
    GCP_STANDARD_EXPORT_CLICKHOUSE_COLUMNS: Final[str] = ["billing_account_id", "usage_start_time", "usage_end_time", "export_time",
                               "cost", "currency", "currency_conversion_rate", "cost_type", "labels.key", "labels.value",
                               "system_labels.key", "system_labels.value", "credits.name", "credits.amount", "credits.full_name", 
                               "credits.id", "credits.type", "usage", "invoice", "adjustment_info",
                               "service", "sku", "project", "location", "cost_at_list", "transaction_type", "seller_name",
                               "tags.key", "tags.value", "tags.inherited", "tags.namespace"]
    GCP_DETAILED_EXPORT_CLICKHOUSE_COLUMNS: Final[str] = GCP_STANDARD_EXPORT_CLICKHOUSE_COLUMNS + ["resource", "price"]

    # K8s Job Environment Variables
    CLICKHOUSE_URL: str = os.environ.get('CLICKHOUSE_URL', 'localhost')
    CLICKHOUSE_PORT: str = os.environ.get('CLICKHOUSE_PORT', 8123)
    CLICKHOUSE_USERNAME: str = os.environ.get('CLICKHOUSE_USERNAME', 'default')
    CLICKHOUSE_PASSWORD: str = os.environ.get('CLICKHOUSE_PASSWORD', 'password')
    CLICKHOUSE_SEND_RECEIVE_TIMEOUT: str = os.environ.get('CLICKHOUSE_SEND_RECEIVE_TIMEOUT', 86400)
    CLICKHOUSE_QUERY_RETRIES: str = os.environ.get('CLICKHOUSE_QUERY_RETRIES', 3)

    HMAC_ACCESS_KEY: str = os.environ.get('HMAC_ACCESS_KEY', '')
    HMAC_SECRET_KEY: str = os.environ.get('HMAC_SECRET_KEY', '')
    SERVICE_ACCOUNT_CREDENTIALS: str = os.environ.get('SERVICE_ACCOUNT_CREDENTIALS', '')

    BUCKET_NAME_PREFIX: str = os.environ.get('BUCKET_NAME_PREFIX', 'harness-ccm-%s-%s')

    def __init__(self):
        self.client = clickhouse_connect.get_client(
                        host = self.CLICKHOUSE_URL, 
                        port = self.CLICKHOUSE_PORT, 
                        username = self.CLICKHOUSE_USERNAME, 
                        password = self.CLICKHOUSE_PASSWORD, 
                        send_receive_timeout = self.CLICKHOUSE_SEND_RECEIVE_TIMEOUT, 
                        query_retries = self.CLICKHOUSE_QUERY_RETRIES
                    )

    def __get_service_account_credentials(self) -> Any:
        try:
            return service_account.Credentials.from_service_account_info(
                json.loads(self.SERVICE_ACCOUNT_CREDENTIALS), scopes=['https://www.googleapis.com/auth/cloud-platform'],
            )
        except Exception as e:
            print__(e)
            raise e

    def __get_bigquery_client(self, project_id: str) -> Client:
        return bigquery.Client(credentials=self.__get_service_account_credentials(), project=project_id)
    
    def __get_storage_client(self, project_id: str) -> Client:
        return storage.Client(credentials=self.__get_service_account_credentials(), project=project_id)

    def __execute_command(self, query: str, parameters: Optional[Union[Sequence, Dict[str, Any]]] = None, print_query = True):
        if print_query:
            print__(query)
        try:
            self.client.command(query, parameters)
        except Exception as e:
            print__(e)

    def __execute_query(self, query: str, parameters: Optional[Union[Sequence, Dict[str, Any]]] = None, print_query = True) -> QueryResult:
        if print_query:
            print__(query)
        try:
            return self.client.query(query, parameters)
        except Exception as e:
            print__(e)
        
    def __execute_command_with_retries(self, query: str, parameters: Optional[Union[Sequence, Dict[str, Any]]] = None, retry_count = 3):
        print__(query)
        for attempt in range(1, retry_count + 1):
            try:
                self.client.command(query, parameters)
                return
            except ServerException as e:
                if e.code == ErrorCodes.NETWORK_ERROR:
                    print__(f"Attempt {attempt}: Network error occurred: {e}. Retrying...")
                else:
                    print__(f"Attempt {attempt}: ClickHouse server error: {e}")
                    break
            except Exception as e:
                print(f"Attempt {attempt}: An unexpected error occurred: {e}")
                break
        print__("Max retry attempts reached. Aborting.")
    
    def __is_table_supported(self, table_name: str) -> bool:
        if table_name in self.TABLES_SUPPORTED:
            return True
        else:
            print__(f'Table {table_name} is not supported in OnPrem')
            return False
        
    def __is_prefix_table_supported(self, table_name: str) -> bool:
        for prefix_table in self.PREFIX_TABLES_SUPPORTED:
            if table_name.startswith(prefix_table):
                return True
        else:
            print__(f'Table {table_name} is not supported in OnPrem')
            return False
        
    def __get_table_name_from_table_id(self, table_id: str) -> str:
        return table_id.split('.')[-1]
    
    def __create_gcs_bucket(self, bucket_name: str, jsonData: Any) -> None:
        try:
            print__(f'Checking {bucket_name} exists or not...')
            bucket = self.__get_storage_client(jsonData['sourceGcpProjectId']).bucket(bucket_name)
            if not bucket.exists():
                print__(f'Creating GCS bucket {bucket_name}')
                bucket.create(location=jsonData['sourceDataSetRegion'])
                print__(f'GCS bucket {bucket_name} created')
            else:
                print__(f'Bucket {bucket_name} already exists')
        except Exception as e:
            print__(e)

    def __export_bigquery_data_to_gcs_bucket(self, bucket_name: str, file_name: str, jsonData: Any) -> None:
        billing_export_columns = self.GCP_DETAILED_EXPORT_BIGQUERY_COLUMNS if jsonData['isBillingExportDetailed'] else self.GCP_STANDARD_EXPORT_BIGQUERY_COLUMNS
        columns_query = ', '.join(f'{column}' for column in billing_export_columns)
        query = """EXPORT DATA
            OPTIONS (
                uri = 'gs://%s/%s',
                format = 'PARQUET',
                overwrite = true,
                compression = 'GZIP')
            AS (
                SELECT %s
                FROM %s.%s.%s
                WHERE DATE(usage_start_time) >= DATE_SUB(@run_date, INTERVAL %s DAY)
            )""" % (bucket_name, file_name, columns_query, jsonData['sourceGcpProjectId'], jsonData['sourceDataSetId'], 
                    jsonData['sourceGcpTableName'], str(int(jsonData.get("interval", 180))))
        # Configure the query job.
        job_config = bigquery.QueryJobConfig(
            query_parameters=[
                bigquery.ScalarQueryParameter(
                    "run_date",
                    "DATE",
                    datetime.datetime.utcnow().date(),
                )
            ]
        )
        print__(query)
        try:
            query_job = self.__get_bigquery_client(jsonData['sourceGcpProjectId']).query(query, job_config=job_config)
            query_job.result()  # wait for job to complete
        except Exception as e:
            print__(e)

    def __create_gcp_billing_table(self, table_name: str) -> None:
        self.__execute_command(create_gcp_billing_export_table % (self.DATABASE, table_name))

    def __delete_data_from_billing_table(self, table_name: str, jsonData: Any) -> None:
        query = """
            DELETE
            FROM `%s`.`%s`
            WHERE toDate(usage_start_time) >= today() - toIntervalDay(%s)
        """ % (self.DATABASE, table_name, str(int(jsonData.get("interval", 180))))
        self.__execute_command(query)

    def __enable_parallel_distributed_insert_select(self) -> None:
        query = 'SET parallel_distributed_insert_select = 1'
        self.__execute_command(query)

    def __insert_gcs_data_to_clickhouse_table(self, table_name: str, bucket_name: str, file_name: str, jsonData: Any) -> None:
        self.__enable_parallel_distributed_insert_select()
        billing_export_columns = self.GCP_DETAILED_EXPORT_CLICKHOUSE_COLUMNS if jsonData['isBillingExportDetailed'] else self.GCP_STANDARD_EXPORT_CLICKHOUSE_COLUMNS
        columns_query = ', '.join(f'{column}' for column in billing_export_columns)
        query = """
            INSERT INTO %s.%s (%s)
            SELECT
                %s
            FROM gcs(
                'https://storage.googleapis.com/%s/%s',
                '%s',
                '%s'
            )
            """ % (self.DATABASE, table_name, columns_query, columns_query, bucket_name, file_name,
                   self.HMAC_ACCESS_KEY, self.HMAC_SECRET_KEY)
        print__('Ingesting data from GCS bucket to ClickHouse billing table...')
        self.__execute_command(query, None, False)

    def create_dataset(self, datasetName: Any, accountId: str = "") -> None:
        pass
        
    def if_tbl_exists(self, table_ref: TableReference) -> bool:
        table_name = table_ref.table_id
        if not (self.__is_table_supported(table_name) or self.__is_prefix_table_supported(table_name)):
            return True # Returning True as we don't want to create unsupported tables
        query = """
            SELECT COUNT(*)
            FROM system.tables
            WHERE database = %s
            AND name = %s
        """
        parameters = (self.DATABASE, table_name)
        result = self.__execute_query(query, parameters)
        return result.first_row[0] == 1
    
    def createTable(self, table_ref: TableReference) -> Any:
        table_ddl_query =  None
        table_name = table_ref.table_id
        if table_name == UNIFIED:
            table_ddl_query = create_unified_table % self.DATABASE
        elif table_name == PREAGGREGATED:
            table_ddl_query = create_pre_aggregated_table % self.DATABASE
        elif table_name == COSTAGGREGATED:
            table_ddl_query = create_cost_aggregated_table % self.DATABASE
        elif table_name == CONNECTORDATASYNCSTATUSTABLE:
            table_ddl_query = create_connector_data_sync_status_table % self.DATABASE
        elif table_name.startswith(GCPCOSTPREFIX):
            table_ddl_query = create_gcp_cost_export_table % (self.DATABASE, table_name)
        else:
            print__(f'Table {table_name} is not supported in OnPrem')
        if table_ddl_query is not None:
            self.__execute_command(table_ddl_query)

    def alter_unified_table(self, jsonData: Any) -> None:
        # TODO: Remove the ALTER command after successful deployment to CBP
        print__('Altering unifiedTable Table')
        query = f"""ALTER TABLE {self.DATABASE}.unifiedTable \
            ADD COLUMN IF NOT EXISTS gcpResource Tuple(name String, global_name String), \
            ADD COLUMN IF NOT EXISTS gcpSystemLabels Array(Tuple(key String, value String)), \
            ADD COLUMN IF NOT EXISTS gcpCostAtList Float64, \
            ADD COLUMN IF NOT EXISTS gcpProjectNumber String, \
            ADD COLUMN IF NOT EXISTS gcpProjectName String, \
            ADD COLUMN IF NOT EXISTS gcpPrice Tuple(effective_price Decimal(18, 6), tier_start_amount Decimal(18, 6), unit String, pricing_unit_quantity Decimal(18, 6)), \
            ADD COLUMN IF NOT EXISTS gcpUsage Tuple(amount Float64, unit String, amount_in_pricing_units Float64, pricing_unit String), \
            ADD COLUMN IF NOT EXISTS gcpCredits Array(Tuple(name String, amount Float64, full_name String, id String, type String))"""

        self.__execute_command(query)
        print__('Finished altering unifiedTable with costCategory and gcpColumns')

    def add_currency_preferences_columns_to_schema(self, table_ids: Any) -> None:
        # Currency preferences are not supported in OnPrem mode
        pass

    def get_preferred_currency(self, jsonData: Any) -> None:
        # Currency preferences are not supported in OnPrem mode
        pass

    def trigger_historical_cost_update_in_preferred_currency(self, jsonData: Any) -> None:
        # Currency preferences are not supported in OnPrem mode
        pass

    def get_impersonated_credentials(self, jsonData: Any) -> None:
        # Impersonation not required in OnPrem mode
        pass

    def send_cost_category_update_event(self, jsonData: Any) -> None:
        # Cost category in dashboard is not supported in OnPrem mode
        pass

    def isFreshSync(self, jsonData: Any) -> Any:
        self.__create_gcp_billing_table(jsonData['tableName'])
        print__('Determining if we need to do fresh sync')
        query = f"""SELECT COUNT(*)
                    FROM {self.DATABASE}.{jsonData['tableName']}
                    WHERE toDate(usage_start_time) >= today() - toIntervalDay(180)"""
        try:
            result = self.__execute_query(query)
            row_count = result.first_row[0]
            print__(f'Number of GCP records existing on our side : {row_count}')
            return not row_count > 0
        except Exception as e:
            print__(e)
            print__('Fresh sync is needed')
            return True
        
    def compute_sync_interval(self, jsonData: Any) -> None:
        if 'replayIntervalInDays' in jsonData:
            jsonData['interval'] = jsonData['replayIntervalInDays']
            print__(f'Sync Interval: {jsonData["interval"]} days')
            return
        if jsonData.get('isFreshSync'):
            jsonData['interval'] = '180'
            print__(f'Sync Interval: {jsonData["interval"]} days')
            return

        # find last_synced_export_date in our side of gcp_cost_export table
        # (not using gcp_billing_export table on our side since it will have currently synced data in case of cross-region)
        last_synced_export_date = ''
        intermediary_table_name = jsonData['tableName'].replace('gcp_billing_export', 'gcp_cost_export', 1) \
                                    if jsonData['tableName'].startswith('gcp_billing_export') \
                                    else f'gcp_cost_export_{jsonData["tableName"]}'
        gcp_cost_export_table_name = '`%s`.`%s`' % (self.DATABASE, intermediary_table_name)
        query = f"""
            SELECT toDate(max(export_time)) - toIntervalDay(1) AS last_synced_export_date 
            FROM {gcp_cost_export_table_name}
            WHERE usage_start_time >= today() - toIntervalDay(180)"""
        result = self.__execute_query(query)
        last_synced_export_date = str(result.first_row[0])
        print__(f'last_synced_export_date: {last_synced_export_date}')

        # find syncInterval based on minimum usage_start_time at source for data after last_synced_export_date
        # for querying source table, assuming that date(_PARTITIONTIME) is same as date(export_time) - using the former.
        query = """
            SELECT DATE_DIFF(CURRENT_DATE(), DATE(MIN(usage_start_time)), DAY) as sync_interval
            FROM `%s.%s.%s`
            WHERE DATE(_PARTITIONTIME) >= DATE('%s')
        """ % (jsonData["sourceGcpProjectId"], jsonData["sourceDataSetId"], jsonData["sourceGcpTableName"],
            last_synced_export_date)

        results = run_bq_query_with_retries(self.__get_bigquery_client(jsonData['sourceGcpProjectId']), query)
        for row in results:
            sync_interval = row.sync_interval
        print__(f'sync_interval: {sync_interval}')

        # setting the sync-interval in jsonData. Will not sync more than 180 days data in any case.
        if 'ccmPreferredCurrency' in jsonData:
            jsonData['interval'] = str(min(max(datetime.datetime.utcnow().date().day - 1, sync_interval), 180))
        else:
            jsonData['interval'] = str(min(sync_interval, 180))
        print__('Sync Interval: %s days' % jsonData['interval'])

    def check_if_billing_export_is_detailed(self, jsonData: Any) -> bool:
        print__(f'Checking if raw billing export ({jsonData["tableName"]}) is detailed / has resource column at source')
        query = """SELECT column_name FROM `%s.%s.INFORMATION_SCHEMA.COLUMNS` 
                WHERE table_name = '%s' and column_name = "resource";
                """ % (jsonData['sourceGcpProjectId'], jsonData['sourceDataSetId'], jsonData['sourceGcpTableName'])
        print__(query)
        try:
            query_job = self.__get_bigquery_client(jsonData['sourceGcpProjectId']).query(query)
            results = query_job.result()  # wait for job to complete
            for row in results:
                if row.column_name == 'resource':
                    return True
        except Exception as e:
            print__(e)
            print__('Failed to retrieve columns from the ingested billing_export table', 'WARN')
        return False

    def ingest_into_gcp_billing_export_table(self, destination: str, jsonData: Any) -> None:
        try:
            bucket_name = self.BUCKET_NAME_PREFIX % (jsonData['sourceGcpProjectId'], jsonData['sourceDataSetRegion'].lower())
            formatted_datetime = datetime.datetime.now(self.LOCAL_TIMEZONE).strftime(self.SUB_FOLDER_PATTERN)
            file_name = self.DATA_FILE_PATH % (jsonData['accountId'], formatted_datetime)
            jsonData['isBillingExportDetailed'] = self.check_if_billing_export_is_detailed(jsonData)
            table_name = jsonData['tableName']
            self.__create_gcs_bucket(bucket_name, jsonData)
            self.__export_bigquery_data_to_gcs_bucket(bucket_name, file_name, jsonData)
            self.__create_gcp_billing_table(table_name)
            self.__delete_data_from_billing_table(table_name, jsonData)
            self.__insert_gcs_data_to_clickhouse_table(table_name, bucket_name, file_name, jsonData)
        except Exception as e:
            print__('Failed to ingest gcp billing export data to clickhouse', 'ERROR')
            raise e

    def get_unique_billingaccount_id(self, jsonData: Any) -> None:
        # Get unique billingAccountIds from main gcp table
        print__("Getting unique billingAccountIds from %s" % jsonData["tableName"])
        query = """SELECT DISTINCT billing_account_id as billing_account_id FROM `%s`.`%s`
                WHERE toDate(usage_start_time) >= today() - toIntervalDay(%s)
                """ % (self.DATABASE, jsonData["tableName"], str(int(jsonData.get("interval", 180))))
        try:
            result = self.__execute_query(query)
            billingAccountIds = []
            for row in result.result_rows:
                billingAccountIds.append(row[0])
            jsonData['billingAccountIds'] = ", ".join(f"'{w}'" for w in billingAccountIds)
            jsonData['billingAccountIdsList'] = billingAccountIds
        except Exception as e:
            print__('Failed to retrieve distinct billingAccountIds', 'WARN')
            jsonData['billingAccountIds'] = ''
            jsonData['billingAccountIdsList'] = []
            raise e
        print__(f'Found unique billingAccountIds {jsonData.get("billingAccountIds")}')

    def insert_currencies_with_unit_conversion_factors_in_bq(self, jsonData: Any) -> None:
        pass

    def initialize_fx_rates_dict(self, jsonData: Any) -> None:
        pass

    def fetch_default_conversion_factors_from_API(self, jsonData: Any) -> None:
        pass

    def fetch_default_conversion_factors_from_billing_export(self, jsonData: Any) -> None:
        pass

    def fetch_custom_conversion_factors(self, jsonData: Any) -> None:
        pass

    def verify_existence_of_required_conversion_factors(self, jsonData: Any) -> None:
        pass

    def update_fx_rate_column_in_raw_table(self, jsonData: Any) -> None:
        pass

    def alter_cost_export_table(self, jsonData: Any) -> None:
        pass
    
    def ingest_into_gcp_cost_export_table(self, gcp_cost_export_table_name: str, jsonData: Any) -> None:
        print__(f'Loading into {gcp_cost_export_table_name} table...')
        billing_export_columns = self.GCP_DETAILED_EXPORT_CLICKHOUSE_COLUMNS if jsonData['isBillingExportDetailed'] else self.GCP_STANDARD_EXPORT_CLICKHOUSE_COLUMNS
        columns_query = ', '.join(f'{column}' for column in billing_export_columns)
        table_name = self.__get_table_name_from_table_id(gcp_cost_export_table_name)
        billing_account_ids = jsonData['billingAccountIds'] if jsonData['billingAccountIds'] else '\'\''
        delete_query = """DELETE FROM `%s`.`%s` WHERE toDate(usage_start_time) >= today() - toIntervalDay(%s)
                AND billing_account_id IN (%s)
        """ % (self.DATABASE, table_name, jsonData['interval'], billing_account_ids)
        insert_query = """INSERT INTO `%s`.`%s` (%s)
                SELECT %s
                FROM `%s`.`%s`
                WHERE toDate(usage_start_time) >= today() - toIntervalDay(%s)
        """ % (self.DATABASE, table_name, columns_query, columns_query, self.DATABASE, jsonData['tableName'], jsonData['interval'])
        try:
            self.__execute_command(delete_query)
            self.__execute_command(insert_query)
        except Exception as e:
            raise e
        print__('Loaded into intermediary gcp_cost_export table.')

    def ingest_into_preaggregated(self, jsonData: Any) -> None:
        print__('Loading into preaggregated table...')
        billing_account_ids = jsonData['billingAccountIds'] if jsonData['billingAccountIds'] else '\'\''
        delete_query = """DELETE FROM `%s`.preAggregated WHERE toDate(startTime) >= today() - toIntervalDay(%s) AND cloudProvider = 'GCP'
                    AND gcpBillingAccountId IN (%s)""" % (self.DATABASE, jsonData['interval'], billing_account_ids)
        insert_query = """INSERT INTO `%s`.preAggregated (cost, gcpProduct, gcpSkuId, gcpSkuDescription,
                startTime, gcpProjectId, region, zone, gcpBillingAccountId, cloudProvider, discount) 
                SELECT SUM(cost) AS cost, service.description AS gcpProduct,
                sku.id AS gcpSkuId, sku.description AS gcpSkuDescription, date_trunc('day', usage_start_time) AS startTime, project.id AS gcpProjectId,
                location.region AS region, location.zone AS zone, billing_account_id AS gcpBillingAccountId, 'GCP' AS cloudProvider, SUM(arraySum(coalesce(credits.amount, 0))) AS discount
            FROM `%s`.`%s`
            WHERE toDate(usage_start_time) >= today() - toIntervalDay(%s)
            GROUP BY service.description, sku.id, sku.description, startTime, project.id, location.region, location.zone, billing_account_id;
            """ % (self.DATABASE, self.DATABASE, jsonData['tableName'], jsonData['interval'])
        try:
            self.__execute_command(delete_query)
            self.__execute_command(insert_query)
        except Exception as e:
            raise e
        print__('Loaded into preAggregated table.')

    def ingest_into_unified(self, jsonData: Any) -> None:
        print__('Loading into unifiedTable table...')
        insert_columns = """product, cost, gcpProduct, gcpSkuId, gcpSkuDescription, startTime, endtime, gcpProjectId,
                    gcpProjectName, gcpProjectNumber,
                    region, zone, gcpBillingAccountId, cloudProvider, discount, labels,
                    gcpInvoiceMonth, gcpCostType, gcpCredits.name, gcpCredits.amount, gcpCredits.full_name, gcpCredits.id, gcpCredits.type, 
                    gcpUsage, gcpSystemLabels.key, gcpSystemLabels.value, gcpCostAtList"""
        select_columns = """service.description AS product, cost AS cost, service.description AS gcpProduct, sku.id AS gcpSkuId,
                        sku.description AS gcpSkuDescription, date_trunc('day', usage_start_time) AS startTime, date_trunc('day', usage_end_time) AS endtime, project.id AS gcpProjectId, project.name AS gcpProjectName, project.number AS gcpProjectNumber,
                        location.region AS region, location.zone AS zone, billing_account_id AS gcpBillingAccountId, 'GCP' AS cloudProvider, arraySum(coalesce(credits.amount, 0)) AS discount, arrayMap((k, v) -> (k, v), labels.key, labels.value) AS labels,
                        invoice.month as gcpInvoiceMonth, cost_type as gcpCostType, credits.name AS "gcpCredits.name", 
                        credits.amount AS "gcpCredits.amount", credits.full_name AS "gcpCredits.full_name", credits.id AS "gcpCredits.id", 
                        credits.type AS "gcpCredits.type", usage AS gcpUsage, system_labels.key AS "gcpSystemLabels.key", 
                        system_labels.value AS "gcpSystemLabels.value", cost_at_list AS gcpCostAtList"""

        # supporting additional fields in unifiedTable
        if jsonData.get("isBillingExportDetailed", False):
            for additionalColumn in ["resource", "price"]:
                insert_columns = insert_columns + ", gcp%s" % (additionalColumn.capitalize())
                select_columns = select_columns + ", %s as gcp%s" % (additionalColumn, additionalColumn.capitalize())

        billing_account_ids = jsonData['billingAccountIds'] if jsonData['billingAccountIds'] else '\'\''
        delete_query = """DELETE FROM `%s`.unifiedTable WHERE toDate(startTime) >= today() - toIntervalDay(%s) AND cloudProvider = 'GCP' 
                    AND gcpBillingAccountId IN (%s)""" % (self.DATABASE, jsonData['interval'], billing_account_ids)
        insert_query = """INSERT INTO `%s`.unifiedTable (%s)
                        SELECT %s 
                        FROM `%s`.`%s`
                        WHERE toDate(usage_start_time) >= today() - toIntervalDay(%s)
            """ % (self.DATABASE, insert_columns, select_columns, self.DATABASE, jsonData["tableName"], jsonData['interval'])
        try:
            self.__execute_command_with_retries(delete_query)
            self.__execute_command_with_retries(insert_query)
        except Exception as e:
            raise e
        print__('Loaded into unifiedTable table.')

    def update_connector_data_sync_status(self, jsonData: Any) -> None:
        update_connector_data_sync_status_clickhouse(jsonData, self.DATABASE, self.client)

    def ingest_data_to_costagg(self, jsonData: Any) -> None:
        print__('Loading into %s table...' % COSTAGGREGATED)
        delete_query = """DELETE 
                            FROM `%s`.`%s` 
                            WHERE toDate(day) >= today() - toIntervalDay(%s) AND cloudProvider = 'GCP' AND accountId = '%s'
        """ % (self.DATABASE, COSTAGGREGATED, jsonData['interval'], jsonData['accountId'])
        insert_query = """INSERT INTO `%s`.`%s`  (day, cost, cloudProvider, accountId)
                    SELECT date_trunc('day', startTime) AS day, SUM(cost) AS cost, 'GCP' AS cloudProvider, '%s' as accountId
                    FROM `%s`.`%s`
                    WHERE toDate(startTime) >= today() - toIntervalDay(%s) and cloudProvider = 'GCP' 
                    GROUP BY day
        """ % (self.DATABASE, COSTAGGREGATED, jsonData['accountId'], self.DATABASE, UNIFIED, jsonData['interval'])

        try:
            self.__execute_command(delete_query)
            self.__execute_command(insert_query)
        except Exception as e:
            raise e
        print__('Loaded into %s table.' % COSTAGGREGATED)