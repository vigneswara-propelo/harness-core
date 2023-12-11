# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

import sys
import base64
import datetime
import json
import os
import re
from billing_helper import BillingHelper
from util import print_, send_event, set_account_id_log, \
    CEINTERNALDATASET, GCPCONNECTORINFOTABLE, CURRENCYCONVERSIONFACTORUSERINPUT, LABELKEYSTOCOLUMNMAPPING
from google.cloud import bigquery
from google.cloud import bigquery_datatransfer_v1
from google.cloud import pubsub_v1
from google.oauth2 import service_account
from google.api_core.exceptions import BadRequest
from billing_bigquery_helper import BillingBigQueryHelper
from k8s_job.billing_clickhouse_helper import BillingClickHouseHelper

"""
## SAAS events

This is the event when batch ingests:
{
    "accountId": "ustest_gcp_ng",
    "serviceAccount": "harness-ce-harness-kmpys@ccm-play.iam.gserviceaccount.com",
    "sourceGcpProjectId": "ce-qa-274307",
    "sourceDataSetId": "BillingReport_zeaak_fls425ieo7olzmug",
    "sourceDataSetRegion": "eu",
    "connectorId": "1234",
    "sourceGcpTableName": "gcp_billing_export_v1_01E207_52C4CA_2CF8E2",
    "triggerHistoricalCostUpdateInPreferredCurrency": False,
    "deployMode": "ONPREM", # "SAAS"
    "useWorkloadIdentity": "False"
}

Below is the event we send from batch for triggering historical update if required.
{
    "accountId": "ustest_gcp_ng",
    "triggerHistoricalCostUpdateInPreferredCurrency": "True"
}

Below is the event when BQ Data Transfer finishes for non us regions
{
	'dataSourceId': 'cross_region_copy',
	'destinationDatasetId': 'BillingReport_ustest_gcp_ng',
	'emailPreferences': {},
	'endTime': '2021-08-11T18:57:24.670486Z',
	'errorStatus': {},
	'name': 'projects/199539700734/locations/us/transferConfigs/615b6cc0-0000-2807-aed0-001a1143233a/runs/615b6cc2-0000-2807-aed0-001a1143233a',
	'notificationPubsubTopic': 'projects/ccm-play/topics/ce-gcp-billing-cf',
	'params': {
		'overwrite_destination_table': True,
		'source_dataset_id': 'BillingReport_zeaak_fls425ieo7olzmug',
		'source_project_id': 'ce-qa-274307'
	},
	'runTime': '2021-08-11T18:54:54.253699Z',
	'scheduleTime': '2021-08-11T18:54:54.312522Z',
	'state': 'SUCCEEDED',
	'updateTime': '2021-08-11T18:57:24.670501Z',
	'userId': '6106063769767823872'
}

## OnPrem events
{
    "accountId": "ustest_gcp_ng",
    "sourceGcpProjectId": "ce-qa-274307",
    "sourceDataSetId": "BillingReport_zeaak_fls425ieo7olzmug",
    "sourceDataSetRegion": "eu",
    "connectorId": "1234",
    "sourceGcpTableName": "gcp_billing_export_v1_01E207_52C4CA_2CF8E2",
    "replayIntervalInDays": "180" (Optional)
}
"""

client = None
dt_client = None
publisher = None
gcp_cf_topic = None
PROJECTID = os.environ.get('GCP_PROJECT', 'ccm-play')
COSTCATEGORIESUPDATETOPIC = os.environ.get('COSTCATEGORIESUPDATETOPIC', 'ccm-bigquery-batch-update')
CLICKHOUSE_ENABLED = os.environ.get('CLICKHOUSE_ENABLED', 'false')
SERVICE_ACCOUNT_CREDENTIALS = os.environ.get('SERVICE_ACCOUNT_CREDENTIALS', '')

def is_clickhouse_enabled():
    return CLICKHOUSE_ENABLED == "true"

def init_billing_helper():
    return BillingClickHouseHelper() if is_clickhouse_enabled() else BillingBigQueryHelper()

def get_service_account_credentials():
    try:
        return service_account.Credentials.from_service_account_info(
            json.loads(SERVICE_ACCOUNT_CREDENTIALS), scopes=['https://www.googleapis.com/auth/cloud-platform'],
        )
    except Exception as e:
        print_(e)
        raise e

def init_bigquery_client(project_id):
    global client
    if client is None:
        if not is_clickhouse_enabled() or project_id is None:
            client = bigquery.Client(PROJECTID)
        else:
            client = bigquery.Client(credentials=get_service_account_credentials(), project=project_id)

def init_dt_client():
    global dt_client
    if dt_client is None and not is_clickhouse_enabled():
        dt_client = bigquery_datatransfer_v1.DataTransferServiceClient()

def init_publisher():
    global publisher
    if publisher is None and not is_clickhouse_enabled():
        publisher = pubsub_v1.PublisherClient()

def init_gcp_cf_topic():
    global gcp_cf_topic
    if gcp_cf_topic is None and not is_clickhouse_enabled():
        gcp_cf_topic = publisher.topic_path(PROJECTID, os.environ.get('GCPCFTOPIC', 'ce-gcp-billing-cf'))

def syncDataset(billing_helper: BillingHelper, jsonData):
    if jsonData["sourceDataSetRegion"].lower() != "us" and not is_clickhouse_enabled():
        doBQTransfer(jsonData)
        return
    print_("Loading into %s" % jsonData["tableName"])
    # for US region
    destination = "%s.%s.%s" % (PROJECTID, jsonData["datasetName"], jsonData["tableName"])
    billing_helper.compute_sync_interval(jsonData)

    try:
        billing_helper.ingest_into_gcp_billing_export_table(destination, jsonData)
    except BadRequest as e:
        print_(e)
        # Try doing fresh sync here. Mostly it is schema mismatch
        if jsonData.get("syncRetried"):
            raise e
        jsonData["isFreshSync"] = True
        jsonData["syncRetried"] = True
        print_("Retrying with fresh sync")
        syncDataset(billing_helper, jsonData)
    except Exception as e:
        raise e
    
    billing_helper.get_unique_billingaccount_id(jsonData)

    # currency preferences specific methods
    billing_helper.insert_currencies_with_unit_conversion_factors_in_bq(jsonData)
    billing_helper.initialize_fx_rates_dict(jsonData)
    billing_helper.fetch_default_conversion_factors_from_API(jsonData)
    billing_helper.fetch_default_conversion_factors_from_billing_export(jsonData)
    billing_helper.fetch_custom_conversion_factors(jsonData)
    billing_helper.verify_existence_of_required_conversion_factors(jsonData)
    billing_helper.update_fx_rate_column_in_raw_table(jsonData)

    ingest_into_gcp_cost_export_table(billing_helper, jsonData)
    billing_helper.ingest_into_preaggregated(jsonData)
    billing_helper.ingest_into_unified(jsonData)
    billing_helper.update_connector_data_sync_status(jsonData)
    billing_helper.ingest_data_to_costagg(jsonData)


def doBQTransfer(jsonData):
    print_("Doing bq data transfer operation as the region is not 'US'")
    dtDisplayName = "ccm_gcp_dataset_copy_%s_%s" % (jsonData["accountId"], jsonData["connectorId"])

    # Get the full path to your project.
    parent = dt_client.common_project_path(PROJECTID)
    print_("  Supported Data Sources in US region: %s, dt job display name: %s" % (parent, dtDisplayName))

    # Iterate over all possible data transfer configs.
    # This only lists configs where destination dataset in is US.
    # https://github.com/googleapis/google-cloud-python/issues/8466
    for data_source in dt_client.list_transfer_configs(parent=parent):
        if data_source.data_source_id == "cross_region_copy":
            if data_source.display_name == dtDisplayName:
                print_(data_source.name)
                jsonData["dtName"] = data_source.name
                print_("  Existing data transfer name: %s, display name: %s" % (jsonData["dtName"], dtDisplayName))
                break

    if jsonData["deployMode"] == "ONPREM":
        # Uses Google ADC
        imdt_client = bigquery_datatransfer_v1.DataTransferServiceClient()
    else:
        imdt_client = bigquery_datatransfer_v1.DataTransferServiceClient(credentials=jsonData["credentials"])

    if jsonData.get("dtName") == None:
        # Fresh sync
        print_("  Creating transfer config for the first time")
        transfer_config = bigquery_datatransfer_v1.TransferConfig(
            destination_dataset_id=jsonData["datasetName"],
            display_name=dtDisplayName,
            data_source_id="cross_region_copy",
            schedule_options={
                "disable_auto_scheduling": True
            },
            notification_pubsub_topic=gcp_cf_topic,
            params={
                "overwrite_destination_table": True,
                "source_project_id": jsonData["sourceGcpProjectId"],
                "source_dataset_id": jsonData["sourceDataSetId"]
            }
        )
        # print_(transfer_config)
        resp = imdt_client.create_transfer_config(transfer_config=transfer_config, parent=parent)
        print_("  Created transfer config %s" % resp)
        jsonData["dtName"] = resp.name
        update_datatransfer_job_config(jsonData)
    # Trigger manual transfer run
    print_("  Triggering manual transfer run")
    now = datetime.datetime.now(datetime.timezone.utc)
    request = bigquery_datatransfer_v1.types.StartManualTransferRunsRequest(
        {"parent": jsonData["dtName"], "requested_run_time": now})
    print_(imdt_client.start_manual_transfer_runs(request))
    print_("  Triggered manual transfer run")


def ingest_into_gcp_cost_export_table(billing_helper: BillingHelper, jsonData):
    # first, create gcp_cost_export table if not exists yet
    dataset = client.dataset(jsonData["datasetName"])
    intermediary_table_name = jsonData["tableName"].replace("gcp_billing_export", "gcp_cost_export", 1) if jsonData[
        "tableName"].startswith("gcp_billing_export") else f"gcp_cost_export_{jsonData['tableName']}"
    gcpCostExportTableRef = dataset.table(intermediary_table_name)
    gcpCostExportTableTableName = "%s.%s.%s" % (PROJECTID, jsonData["datasetName"], intermediary_table_name)
    jsonData["gcpCostExportTableTableName"] = gcpCostExportTableTableName
    if not billing_helper.if_tbl_exists(gcpCostExportTableRef):
        print_("%s table does not exists, creating table..." % gcpCostExportTableRef)
        billing_helper.createTable(gcpCostExportTableRef)
    else:
        billing_helper.alter_cost_export_table(jsonData)
        print_("%s table exists" % gcpCostExportTableTableName)

    # check whether the raw billing table is standard_export or detailed_export
    if "isBillingExportDetailed" not in jsonData:
        jsonData["isBillingExportDetailed"] = billing_helper.check_if_billing_export_is_detailed(jsonData)

    billing_helper.ingest_into_gcp_cost_export_table(gcpCostExportTableTableName, jsonData)


def update_datatransfer_job_config(jsonData):
    query = """INSERT INTO `%s.%s.%s` (accountId, connectorId, dataTransferConfig, createdAt, sourceGcpTableName) 
                VALUES ('%s', '%s', '%s', '%s', '%s')
            """ % (PROJECTID, CEINTERNALDATASET, GCPCONNECTORINFOTABLE,
                   jsonData["accountId"], jsonData["connectorId"], jsonData["dtName"], datetime.datetime.utcnow(),
                   jsonData["sourceGcpTableName"])

    try:
        print_(query)
        query_job = client.query(query)
        query_job.result()  # wait for job to complete
    except Exception as e:
        print_("  Failed to update connector info", "WARN")
        raise e


def fetch_acc_from_gcp_conn_info(jsonData):
    # 	'name': 'projects/199539700734/locations/us/transferConfigs/615b6cc0-0000-2807-aed0-001a1143233a/runs/615b6cc2-0000-2807-aed0-001a1143233a',
    dt_name = "/".join(jsonData["name"].split("/")[:-2])
    query = """SELECT accountId, connectorId, sourceGcpTableName FROM `%s.%s.%s` 
                WHERE dataTransferConfig='%s';
            """ % (PROJECTID, CEINTERNALDATASET, GCPCONNECTORINFOTABLE,
                   dt_name)

    try:
        print_(query)
        query_job = client.query(query)
        results = query_job.result()  # wait for job to complete
        for row in results:
            jsonData["accountId"] = row.accountId
            jsonData["connectorId"] = row.connectorId
            jsonData["sourceGcpTableName"] = row.sourceGcpTableName
            jsonData["tableName"] = row.sourceGcpTableName
            jsonData["sourceDataSetId"] = jsonData["destinationDatasetId"]
            jsonData["sourceGcpProjectId"] = PROJECTID
            break
    except Exception as e:
        raise e
    print_("retrieved info from gcpConnectrInfoTable")


def main(event, context):
    """Triggered from a message on a Cloud Pub/Sub topic.
    Args:
         event (dict): Event payload.
         context (google.cloud.functions.Context): Metadata for the event.
    """
    print(event)
    data = base64.b64decode(event['data']).decode('utf-8')
    jsonData = json.loads(data)
    jsonData["cloudProvider"] = "GCP"
    print(jsonData)

    billing_helper = init_billing_helper()
    init_bigquery_client(jsonData.get("sourceGcpProjectId"))
    init_dt_client()
    init_publisher()
    init_gcp_cf_topic()

    jsonData["gcpBillingExportTablePartitionColumnName"] = "_PARTITIONTIME"
    # This code won't execute in case of OnPrem deploMode and ClickHouse enabled
    if jsonData.get("dataSourceId") == "cross_region_copy":
        # Event is from BQ DT service. Ingest in gcp_cost_export, unified and preaggregated
        jsonData["datasetName"] = jsonData["destinationDatasetId"]
        set_account_id_log(jsonData["destinationDatasetId"].split("BillingReport_")[-1])
        billing_helper.get_preferred_currency(jsonData)
        fetch_acc_from_gcp_conn_info(jsonData)
        # obtain/set partition column from the gcp_billing_export table to be used for all subsequent queries
        gcp_billing_export_bq_table_name = "%s.%s.%s" % (PROJECTID, jsonData["datasetName"], jsonData["tableName"])
        try:
            gcp_billing_export_bq_table = client.get_table(gcp_billing_export_bq_table_name)
            jsonData[
                "gcpBillingExportTablePartitionColumnName"] = "_PARTITIONTIME" if gcp_billing_export_bq_table.time_partitioning.field is None else gcp_billing_export_bq_table.time_partitioning.field
        except:
            # table doesn't exist yet, use usage_start_time as partition column for new table
            # however, for cross_region_copy, table will always exist
            jsonData["gcpBillingExportTablePartitionColumnName"] = "usage_start_time"
        print_(f"Partition column for gcp_billing_export table: {jsonData['gcpBillingExportTablePartitionColumnName']}")

        billing_helper.get_unique_billingaccount_id(jsonData)
        jsonData["isFreshSync"] = billing_helper.isFreshSync(jsonData)
        billing_helper.compute_sync_interval(jsonData)

        # currency specific methods
        billing_helper.insert_currencies_with_unit_conversion_factors_in_bq(jsonData)
        billing_helper.initialize_fx_rates_dict(jsonData)
        billing_helper.fetch_default_conversion_factors_from_API(jsonData)
        billing_helper.fetch_default_conversion_factors_from_billing_export(jsonData)
        billing_helper.fetch_custom_conversion_factors(jsonData)
        billing_helper.verify_existence_of_required_conversion_factors(jsonData)
        billing_helper.update_fx_rate_column_in_raw_table(jsonData)

        ingest_into_gcp_cost_export_table(billing_helper, jsonData)
        billing_helper.ingest_into_preaggregated(jsonData)
        billing_helper.ingest_into_unified(jsonData)
        billing_helper.update_connector_data_sync_status(jsonData)
        billing_helper.ingest_data_to_costagg(jsonData)
        billing_helper.send_cost_category_update_event(jsonData)
        return
    # Set the accountId for GCP logging
    set_account_id_log(jsonData.get("accountId"))
    jsonData["accountIdBQ"] = re.sub('[^0-9a-z]', '_', jsonData.get("accountId").lower())
    jsonData["datasetName"] = "BillingReport_%s" % jsonData["accountIdBQ"]
    jsonData["tableName"] = "gcp_billing_export_%s" % jsonData["connectorId"]
    billing_helper.create_dataset(jsonData["datasetName"], jsonData.get("accountId"))
    dataset = client.dataset(jsonData["datasetName"])
    preAggragatedTableRef = dataset.table("preAggregated")
    preAggregatedTableTableName = "%s.%s.%s" % (PROJECTID, jsonData["datasetName"], "preAggregated")
    unifiedTableRef = dataset.table("unifiedTable")
    unifiedTableTableName = "%s.%s.%s" % (PROJECTID, jsonData["datasetName"], "unifiedTable")
    currencyConversionFactorUserInputTableRef = dataset.table(CURRENCYCONVERSIONFACTORUSERINPUT)
    currencyConversionFactorUserInputTableName = "%s.%s.%s" % (
        PROJECTID, jsonData["datasetName"], CURRENCYCONVERSIONFACTORUSERINPUT)
    label_keys_to_column_mapping_table_ref = dataset.table(LABELKEYSTOCOLUMNMAPPING)

    if not billing_helper.if_tbl_exists(unifiedTableRef):
        print_("%s table does not exists, creating table..." % unifiedTableRef)
        billing_helper.createTable(unifiedTableRef)
    else:
        billing_helper.alter_unified_table(jsonData)
        print_("%s table exists" % unifiedTableTableName)

    if not billing_helper.if_tbl_exists(preAggragatedTableRef):
        print_("%s table does not exists, creating table..." % preAggragatedTableRef)
        billing_helper.createTable(preAggragatedTableRef)
    else:
        print_("%s table exists" % preAggregatedTableTableName)

    if not billing_helper.if_tbl_exists(currencyConversionFactorUserInputTableRef):
        print_("%s table does not exists, creating table..." % currencyConversionFactorUserInputTableRef)
        billing_helper.createTable(currencyConversionFactorUserInputTableRef)
    else:
        print_("%s table exists" % currencyConversionFactorUserInputTableName)

    if not billing_helper.if_tbl_exists(label_keys_to_column_mapping_table_ref):
        print_("%s table does not exist, creating table..." % LABELKEYSTOCOLUMNMAPPING)
        billing_helper.createTable(label_keys_to_column_mapping_table_ref)
    else:
        print_("%s table exists" % LABELKEYSTOCOLUMNMAPPING)

    ds = f"{PROJECTID}.{jsonData['datasetName']}"
    table_ids = ["%s.%s" % (ds, "unifiedTable"),
                 "%s.%s" % (ds, "preAggregated")]
    # will be altering gcp_cost_export table after gcp_billing_export is created since exact table name might vary
    billing_helper.add_currency_preferences_columns_to_schema(table_ids)

    billing_helper.get_preferred_currency(jsonData)
    if jsonData.get("triggerHistoricalCostUpdateInPreferredCurrency") == "True" and jsonData["ccmPreferredCurrency"]:
        # trigger historical CF and exit
        billing_helper.trigger_historical_cost_update_in_preferred_currency(jsonData)
        return
    elif jsonData.get("triggerHistoricalCostUpdateInPreferredCurrency") == "True":
        # no historical costs to update since currency is not set. exit.
        return

    billing_helper.get_impersonated_credentials(jsonData)

    # obtain/set partition column from the gcp_billing_export table to be used for all subsequent queries
    gcp_billing_export_bq_table_name = "%s.%s.%s" % (PROJECTID, jsonData["datasetName"], jsonData["tableName"])
    try:
        gcp_billing_export_bq_table = client.get_table(gcp_billing_export_bq_table_name)
        jsonData[
            "gcpBillingExportTablePartitionColumnName"] = "_PARTITIONTIME" if gcp_billing_export_bq_table.time_partitioning.field is None else gcp_billing_export_bq_table.time_partitioning.field
    except:
        # table doesn't exist yet, use usage_start_time as partition column for new table
        jsonData["gcpBillingExportTablePartitionColumnName"] = "usage_start_time"
    print_(f"Partition column for gcp_billing_export table: {jsonData['gcpBillingExportTablePartitionColumnName']}")

    # Sync dataset
    jsonData["isFreshSync"] = billing_helper.isFreshSync(jsonData)
    syncDataset(billing_helper, jsonData)
    billing_helper.send_cost_category_update_event(jsonData)
    print_("Completed")
    return

# K8s Job will initiate this function
if __name__ == "__main__":
    # First argument is always the file name
    if len(sys.argv) > 2:
        print(f'Invalid arguments {sys.argv}. Not supported for OnPrem')
    else:
        # Accessing command-line arguments
        connector_data_args = sys.argv[1]
        print(f'connector_data_args: {connector_data_args}')

        # Parse the JSON string
        connector_data = json.loads(connector_data_args)
        print(f'connector_data: {connector_data}')

        event = {'data': (value := base64.b64encode(connector_data_args.encode('utf-8')).decode('utf-8'))}
        main(event, {})