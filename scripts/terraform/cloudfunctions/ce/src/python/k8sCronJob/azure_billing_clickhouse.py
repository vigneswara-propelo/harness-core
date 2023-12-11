# Copyright 2023 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

"""
K8s scheduled job for Azure data pipeline
"""
import ch_ddl_queries
import clickhouse_connect
import datetime
import json
import os
import re
import time
from tzlocal import get_localzone
from azure.storage.blob import BlobServiceClient


LOCAL_TIMEZONE = get_localzone()

# Clickhouse related configuration
CLICKHOUSE_URL = os.environ.get('CLICKHOUSE_URL', 'localhost')
CLICKHOUSE_PORT = os.environ.get('CLICKHOUSE_PORT', 8123)
CLICKHOUSE_USERNAME = os.environ.get('CLICKHOUSE_USERNAME', None)
CLICKHOUSE_PASSWORD = os.environ.get('CLICKHOUSE_PASSWORD', '')
CLICKHOUSE_SEND_RECEIVE_TIMEOUT = os.environ.get('CLICKHOUSE_SEND_RECEIVE_TIMEOUT', 86400)
CLICKHOUSE_QUERY_RETRIES = os.environ.get('CLICKHOUSE_QUERY_RETRIES', 10)
clickhouse_client = clickhouse_connect.get_client(host=CLICKHOUSE_URL, port=CLICKHOUSE_PORT, username=CLICKHOUSE_USERNAME, password=CLICKHOUSE_PASSWORD, send_receive_timeout=CLICKHOUSE_SEND_RECEIVE_TIMEOUT, query_retries=CLICKHOUSE_QUERY_RETRIES)


# Azure related configuration
AZURE_STORAGE_ACCOUNT = os.environ.get('AZURE_STORAGE_ACCOUNT', 'storageAccount')
AZURE_CONTAINER = os.environ.get('AZURE_CONTAINER', 'container')
AZURE_SAS_TOKEN = os.environ.get('AZURE_SAS_TOKEN', 'sasToken')
AZURE_REPORT_RETRIES = int(os.environ.get('AZURE_REPORT_RETRIES', '10'))
TIME_DELTA = int(os.environ.get('TIME_DELTA', '1'))
CONNECT_STR = 'BlobEndpoint=https://' + AZURE_STORAGE_ACCOUNT + '.blob.core.windows.net;SharedAccessSignature=' + AZURE_SAS_TOKEN


# Fixed Azure Cost Table Related variable
AZURE_COST_TABLE_COLUMN_NAME = ["azureSubscriptionGuid", "azureResourceGroup", "ResourceLocation", "startTime", "MeterCategory", "MeterSubcategory", "MeterId", "MeterName", "MeterRegion", "UsageQuantity", "azureResourceRate", "cost", "ConsumedService", "ResourceType", "azureInstanceId", "Tags", "OfferId", "AdditionalInfo", "ServiceInfo1", "ServiceInfo2", "ServiceName", "ServiceTier", "Currency", "UnitOfMeasure"]

ACCOUNT_ID = ""

def print_(message, severity="INFO"):
    try:
        print(json.dumps({
            "currentTime":str(datetime.datetime.now(LOCAL_TIMEZONE).strftime("%Y-%m-%d %H:%M:%S.%f")),
            "accountId":ACCOUNT_ID,
            "severity":severity,
            "message": message
        }))
    except:
        print(message)


def run_clickhouse_command(query, retries=1):
    if retries == 1:
        retries_str = ""
    else:
        retries_str = "with retries"
    print_("Running command %s on clickhouse : %s" % (retries_str, query))

    for attempt in range(1, retries + 1):
        try:
            clickhouse_client.command(query)
            break
        except Exception as e:
            if retries > 1:
                print_(f"Attempt {attempt} failed to run command: {query}", "ERROR")
                print_("Exception %s" % e, "ERROR")
                if attempt < retries:
                    print_(f"Retrying in 5 seconds...")
                    time.sleep(5)
            else:
                print_("Failed to run command %s" % query, "ERROR")
                print_("Exception %s" % e, "ERROR")
                raise e


def run_clickhouse_query(query, retries=1):
    if retries == 1:
        retries_str = ""
    else:
        retries_str = "with retries"
    print_("Running query %s on clickhouse : %s" % (retries_str, query))

    for attempt in range(1, retries + 1):
        try:
            query_job = clickhouse_client.query(query)
            results = query_job.result_rows
            return results
        except Exception as e:
            if retries > 1:
                print_(f"Attempt {attempt} failed to run query: {query}", "ERROR")
                print_("Exception %s" % e, "ERROR")
                if attempt < retries:
                    print_(f"Retrying in 5 seconds...")
                    time.sleep(5)
            else:
                print_("Failed to run query %s" % query, "ERROR")
                print_("Exception %s" % e, "ERROR")
                raise e


def create_db_and_tables(database):
    run_clickhouse_command(ch_ddl_queries.create_database % database)
    print_("Default DB %s is created if it does not exist" % database)
    run_clickhouse_command(ch_ddl_queries.create_unified_table % database)
    print_("Table %s.unifiedTable is created if it does not exist" % database)
    run_clickhouse_command(ch_ddl_queries.create_pre_aggregated_table % database)
    print_("Table %s.preAggregated is created if it does not exist" % database)
    run_clickhouse_command(ch_ddl_queries.create_cost_aggregated_table % database)
    print_("Table %s.costAggregated is created if it does not exist" % database)
    run_clickhouse_command(ch_ddl_queries.create_connector_data_sync_status_table % database)
    print_("Table %s.connectorDataSyncStatus is created if it does not exist" % database)
    print_("Default DB and Tables required for Azure Data Ingestion created if it does not exist")


def get_blob_path_to_be_ingested():
    print_("Working on getting the reports to be ingested...")

    blob_service_client = BlobServiceClient.from_connection_string(CONNECT_STR)
    container_client = blob_service_client.get_container_client(AZURE_CONTAINER)

    blobs_after_this_time = datetime.datetime.utcnow().replace(tzinfo=datetime.timezone.utc) - datetime.timedelta(days=TIME_DELTA)

    blobs = container_client.list_blobs()
    latest_blobs = []
    for blob in blobs:
        if blob.last_modified >= blobs_after_this_time:
            latest_blobs.append(blob)

    sorted_by_size_blobs = sorted(latest_blobs, key=lambda x: x.size, reverse=True)
    reports_to_ingest = []
    for blob in sorted_by_size_blobs:
        blob_name_splitted = (blob.name).split("/")
        blob_name_len = len(blob_name_splitted)
        if blob_name_len == 6:
            concatenated_path = '/'.join(blob_name_splitted[:-1])
            exists = any(e.startswith(concatenated_path) for e in reports_to_ingest)
            if exists == False:
                reports_to_ingest.append(blob.name)
    print_("\nFollowing absolute path reports will be ingested: %s" % reports_to_ingest)

    sorted_by_name_blobs = sorted(latest_blobs, key=lambda x: x.name, reverse=True)
    partitioned_report_to_ingest = []
    for blob in sorted_by_name_blobs:
        blob_name_splitted = (blob.name).split("/")
        blob_name_len = len(blob_name_splitted)
        if blob_name_len == 8:
            month_folder = blob_name_splitted[-4]
            year_month = month_folder.split("-")[0][:4] + month_folder.split("-")[0][4:6]
            if (not blob_name_splitted[-3].startswith(year_month)):
                continue
            concatenated_path = '/'.join(blob_name_splitted[:-3])
            exists = any(e.startswith(concatenated_path) for e in partitioned_report_to_ingest)
            if exists == False:
                partitioned_report_to_ingest.append(concatenated_path)
                reports_to_ingest.append('/'.join(blob_name_splitted[:-1]) + "/*.csv")
    print_("\nFollowing partitioned reports will be ingested: %s" % partitioned_report_to_ingest)
    print_("\nFollowing are the final reports to be ingested: %s" % reports_to_ingest)

    return reports_to_ingest


def get_table_schema(absolutePath):
    get_table_schema_query = """
    DESCRIBE TABLE
    (select * from azureBlobStorage('%s','%s','%s'))
    """ % (CONNECT_STR, AZURE_CONTAINER, absolutePath)

    # Running query with manual retries for schema detection query
    # due to inconsistent query failures while testing
    # DB::Exception: Cannot extract table structure from CSV format file
    results = run_clickhouse_query(get_table_schema_query, AZURE_REPORT_RETRIES)

    schema = []
    pattern = r'\((.*?)\)'
    columns = set()
    for row in results:
        column_name = row[0].replace("\ufeff", "")
        data_type_column = row[1]
        match = re.search(pattern, data_type_column)
        if match:
            dataType = match.group(1)
        else:
            dataType = 'String'
        schema.append(f"{column_name.lower()} {dataType}")
        columns.add(column_name.lower())
    table_schema = ', '.join(schema)
    print_("Retrieved table_schema: %s" % table_schema)
    print_("Retrieved columns: %s" % columns)
    return columns, table_schema


def set_available_columns(jsonData):
    # Ref: https://docs.microsoft.com/en-us/azure/cost-management-billing/understand/mca-understand-your-usage
    # Clickhouse column names are case sensitive.
    azure_column_mapping = {
        "startTime": "",
        "azureResourceRate": "",
        "cost": "",
        "azureSubscriptionGuid": "",
        "azureInstanceId": "",
        "currency": "",
    }

    columns = jsonData["columns"]

    # startTime
    if "date" in columns:  # This is O(1) for python sets
        azure_column_mapping["startTime"] = "date"
    elif "usagedatetime" in columns:
        azure_column_mapping["startTime"] = "usagedatetime"
    else:
        raise Exception("No mapping found for startTime column")

    # azureResourceRate
    if "effectiveprice" in columns:
        azure_column_mapping["azureResourceRate"] = "effectiveprice"
    elif "resourcerate" in columns:
        azure_column_mapping["azureResourceRate"] = "resourcerate"
    else:
        raise Exception("No mapping found for azureResourceRate column")

    # currency
    if "billingcurrency" in columns:
        azure_column_mapping["currency"] = "billingcurrency"
    elif "currency" in columns:
        azure_column_mapping["currency"] = "currency"
    elif "billingcurrencycode" in columns:
        azure_column_mapping["currency"] = "billingcurrencycode"
    else:
        raise Exception("No mapping found for currency column")

    # cost
    if "costinbillingcurrency" in columns:
        azure_column_mapping["cost"] = "costinbillingcurrency"
    elif "pretaxcost" in columns:
        azure_column_mapping["cost"] = "pretaxcost"
    elif "cost" in columns:
        azure_column_mapping["cost"] = "cost"
    else:
        raise Exception("No mapping found for cost column")

    # azureSubscriptionGuid
    if "subscriptionid" in columns:
        azure_column_mapping["azureSubscriptionGuid"] = "subscriptionid"
    elif "subscriptionguid" in columns:
        azure_column_mapping["azureSubscriptionGuid"] = "subscriptionguid"
    else:
        raise Exception("No mapping found for azureSubscriptionGuid column")

    # azureInstanceId
    if "resourceid" in columns:
        azure_column_mapping["azureInstanceId"] = "resourceid"
    elif "instanceid" in columns:
        azure_column_mapping["azureInstanceId"] = "instanceid"
    elif "instancename" in columns:
        azure_column_mapping["azureInstanceId"] = "instancename"
    else:
        raise Exception("No mapping found for azureInstanceId column")

    # azureResourceGroup
    if "resourcegroup" in columns:
        azure_column_mapping["azureResourceGroup"] = "resourcegroup"
    elif "resourcegroupname" in columns:
        azure_column_mapping["azureResourceGroup"] = "resourcegroupname"
    else:
        raise Exception("No mapping found for azureResourceGroup column")

    print_("azure_column_mapping: %s" % azure_column_mapping)
    return azure_column_mapping


def create_and_ingest_data_into_billing_table(jsonData, azure_column_mapping):
    print_("Processing data for %s table..." % jsonData["tableName"])

    create_billing_table_query = """
    create table if not exists `%s`.`%s`
    (%s)
    ENGINE = MergeTree ORDER BY tuple(%s) SETTINGS allow_nullable_key = 1;
    """ % (jsonData["database"], jsonData["tableName"], jsonData["tableSchema"], azure_column_mapping["startTime"])

    insert_billing_table_query = """
    insert into `%s`.`%s`
    select *
    from azureBlobStorage('%s', '%s', '%s')
    """ % (jsonData["database"], jsonData["tableName"], CONNECT_STR, AZURE_CONTAINER, absolutePath)

    run_clickhouse_command(f"DROP TABLE IF EXISTS `{jsonData['database']}`.`{jsonData['tableName']}`")
    print_("Dropped %s table if existed..." % jsonData["tableName"])

    run_clickhouse_command(create_billing_table_query)
    print_("Created empty %s table..." % jsonData["tableName"])

    # Adding manual retries for insertion of data from csv to billing tanle
    # due to inconsistent query failures while testing
    # Azure::Core::Http::TransportException, e.what() = Error while polling for socket ready read
    run_clickhouse_command("SET input_format_csv_skip_first_lines=1")
    run_clickhouse_command("SET max_memory_usage=1000000000000")
    run_clickhouse_command(insert_billing_table_query, AZURE_REPORT_RETRIES)
    run_clickhouse_command("SET input_format_csv_skip_first_lines=0")
    print_("Inserted raw data into %s table..." % jsonData["tableName"])

    print_("Data successfully processed for %s table..." % jsonData["tableName"])


def get_start_date_select_query(azure_billing_table_column):
    # By default column 'date' might be treated as string by CH (n) in billing data which would be in 12/30/2021 format
    # By default column 'usagedatetime' would be in 2023-11-01 format which would be automatically treated by CH as date type
    if azure_billing_table_column == 'date':
        return f"toDate(parseDateTime({azure_billing_table_column},'%m/%d/%Y'))"
    else:
        return f"toDate({azure_billing_table_column})"


def update_query_strings_azure_cost_table(azure_cost_table_column, azure_billing_table_column, insert_query, select_query):
    if (not jsonData["columns"]) or (azure_billing_table_column in jsonData["columns"]):
        if azure_cost_table_column == 'startTime':
            azure_billing_table_column = get_start_date_select_query(azure_billing_table_column)

        insert_query += (", " if insert_query else "")
        insert_query += azure_cost_table_column

        select_query += (", " if select_query else "")
        select_query += f"{azure_billing_table_column} as {azure_cost_table_column}"

    return insert_query, select_query


def create_and_ingest_data_into_cost_table(jsonData, azure_column_mapping):
    azure_cost_table_name = jsonData["tableName"].replace("azureBilling_", "azureCost_")
    print_("Processing data for %s table..." % azure_cost_table_name)

    create_azure_cost_table_query = ch_ddl_queries.create_azure_cost_table % (jsonData["database"], azure_cost_table_name)

    # insert query for azure cost table
    select_columns_string = ""
    insert_columns_string = ""
    for column in AZURE_COST_TABLE_COLUMN_NAME:
        if column in azure_column_mapping:
            azure_billing_table_column = azure_column_mapping[column]
            print_(f"Column {column} is mapped to {azure_billing_table_column}, searching for {azure_billing_table_column} in {jsonData['tableName']} table.")
            insert_columns_string, select_columns_string = update_query_strings_azure_cost_table(column, azure_billing_table_column,
                                                                                                insert_columns_string,
                                                                                                select_columns_string)
        else:
            print_(f"No mapping found in azure_column_mapping for column: {column}. Will search for {column.lower()} in {jsonData['tableName']} table.")
            azure_billing_table_column = column.lower()
            insert_columns_string, select_columns_string = update_query_strings_azure_cost_table(column, azure_billing_table_column,
                                                                                                insert_columns_string,
                                                                                                select_columns_string)
    insert_into_azure_cost_table_query = """
    INSERT INTO `%s`.`%s` (%s)
    SELECT %s FROM `%s`.`%s`
    """ % (jsonData["database"], azure_cost_table_name, insert_columns_string, select_columns_string, jsonData["database"], jsonData["tableName"])

    run_clickhouse_command(f"DROP TABLE IF EXISTS `{jsonData['database']}`.`{azure_cost_table_name}`")
    print_("Dropped %s table if existed..." % azure_cost_table_name)

    run_clickhouse_command(create_azure_cost_table_query)
    print_("Created empty %s table..." % azure_cost_table_name)

    run_clickhouse_command(insert_into_azure_cost_table_query)
    print_("Inserted fresh data into %s table..." % azure_cost_table_name)

    print_("Data successfully processed for %s table..." % azure_cost_table_name)


def get_unique_subs_id(jsonData, azure_column_mapping):
    # Get unique subsids from main azureBilling table
    get_unique_subs_id_query = """
    SELECT DISTINCT(%s) as subscriptionid FROM `%s`.`%s`
    """ % (azure_column_mapping["azureSubscriptionGuid"], jsonData["database"], jsonData["tableName"])

    results = run_clickhouse_query(get_unique_subs_id_query)

    subsids = []
    for row in results:
        subsids.append(row[0])
    jsonData["subsIdsList"] = subsids
    jsonData["subsId"] = ", ".join(f"'{w}'" for w in subsids)
    print_("Found unique subsids %s" % subsids)


def ingest_data_into_preagg(jsonData, azure_column_mapping):
    table_name = "`%s`.preAggregated" % (jsonData["database"])
    year, month = jsonData["reportYear"], jsonData["reportMonth"]
    date_start = "%s-%s-01" % (year, month)
    print_("Processing data for %s table..." % table_name)

    delete_query = """
    DELETE from %s
    WHERE DATE_TRUNC('month', startTime) = DATE_TRUNC('month', toDateTime('%s 00:00:00'))
    AND cloudProvider = 'AZURE' AND azureSubscriptionGuid IN (%s)
    """ % (table_name, date_start, jsonData["subsId"])

    start_date_query = get_start_date_select_query(azure_column_mapping["startTime"])

    insert_query = """
    INSERT INTO %s (startTime, azureResourceRate, cost,
                    azureServiceName, region, azureSubscriptionGuid,
                    cloudProvider, azureTenantId)
    SELECT %s as startTime,
        min(%s) AS azureResourceRate, sum(%s) AS cost,
        metercategory AS azureServiceName, resourcelocation as region, %s as azureSubscriptionGuid,
        'AZURE' AS cloudProvider, '%s' as azureTenantId
    FROM `%s`.`%s`
    WHERE %s IN (%s)
    AND DATE_TRUNC('month', %s) = DATE_TRUNC('month', toDateTime('%s 00:00:00'))
    GROUP BY azureServiceName, region, azureSubscriptionGuid, startTime
    """ % (table_name, start_date_query,
           azure_column_mapping["azureResourceRate"],
           azure_column_mapping["cost"], azure_column_mapping["azureSubscriptionGuid"], jsonData["tenant_id"],
           jsonData["database"], jsonData["tableName"],
           azure_column_mapping["azureSubscriptionGuid"], jsonData["subsId"],
           start_date_query, date_start)

    run_clickhouse_command(delete_query)
    print_("Deleted outdated data from %s table..." % table_name)

    run_clickhouse_command(insert_query)
    print_("Inserted fresh data into %s table..." % table_name)

    print_("Data successfully processed for %s table..." % table_name)


def ingest_data_into_unified(jsonData, azure_column_mapping):
    table_name = "`%s`.unifiedTable" % (jsonData["database"])
    year, month = jsonData["reportYear"], jsonData["reportMonth"]
    date_start = "%s-%s-01" % (year, month)
    print_("Processing data for %s table..." % table_name)

    delete_query = """
    DELETE from %s
    WHERE DATE_TRUNC('month', startTime) = DATE_TRUNC('month', toDateTime('%s 00:00:00'))
    AND cloudProvider = 'AZURE' AND azureSubscriptionGuid IN (%s)
    """ % (table_name, date_start, jsonData["subsId"])

    # Prepare default columns
    insert_columns = """product, startTime, cost,
                        azureMeterCategory, azureMeterSubcategory, azureMeterId,
                        azureMeterName,
                        azureInstanceId, region, azureResourceGroup,
                        azureSubscriptionGuid, azureServiceName,
                        cloudProvider, labels, azureResource, azureVMProviderId, azureTenantId,
                        azureResourceRate
                    """

    start_date_query = get_start_date_select_query(azure_column_mapping["startTime"])

    azure_vm_provider_id_select_statement = """
        multiIf({azureInstanceId} LIKE '%virtualMachineScaleSets%', LOWER(CONCAT('azure://', {azureInstanceId}, '/virtualMachines/', REGEXP_REPLACE(JSONExtractString(additionalinfo, 'VMName'), '.*_([0-9]+)$', '\\1'))),
                {azureInstanceId} LIKE '%virtualMachines%', LOWER(CONCAT('azure://', {azureInstanceId})),
                NULL)
        """.format(azureInstanceId=azure_column_mapping["azureInstanceId"])

    select_columns = """metercategory AS product, 
                        (%s) as startTime,
                        (%s) AS cost,
                        metercategory as azureMeterCategory, metersubcategory as azureMeterSubcategory, meterid as azureMeterId,
                        metername as azureMeterName,
                        %s as azureInstanceId, resourcelocation as region,  %s as azureResourceGroup,
                        %s as azureSubscriptionGuid, metercategory as azureServiceName,
                        '%s' AS cloudProvider,
                        JSONExtract(tags, 'Map(String, String)') as labels,
                        extract(%s, 'providers/(.*)') as azureResource,
                        %s as azureVMProviderId,
                        '%s' as azureTenantId,
                        (%s) AS azureResourceRate
                     """ % (start_date_query,
                            azure_column_mapping["cost"],
                            azure_column_mapping["azureInstanceId"], azure_column_mapping["azureResourceGroup"],
                            azure_column_mapping["azureSubscriptionGuid"],
                            "AZURE",
                            azure_column_mapping["azureInstanceId"],
                            azure_vm_provider_id_select_statement,
                            jsonData["tenant_id"],
                            azure_column_mapping["azureResourceRate"])

    # Amend query as per columns availability
    for additionalColumn in ["AccountName", "Frequency", "PublisherType", "ServiceTier", "ResourceType",
                             "SubscriptionName", "ReservationId", "ReservationName", "PublisherName",
                             "CustomerName", "BillingCurrency"]:
        if additionalColumn.lower() in jsonData["columns"]:
            insert_columns = insert_columns + ", azure%s" % additionalColumn
            select_columns = select_columns + ", %s as azure%s" % (additionalColumn.lower(), additionalColumn)

    insert_query = """
    INSERT INTO %s (%s)
    SELECT %s
    FROM `%s`.`%s` 
    WHERE %s IN (%s)
    AND DATE_TRUNC('month', %s) = DATE_TRUNC('month', toDateTime('%s 00:00:00'))
    """ % (table_name, insert_columns,
            select_columns,
            jsonData["database"], jsonData["tableName"],
            azure_column_mapping["azureSubscriptionGuid"], jsonData["subsId"],
            start_date_query, date_start)
    
    run_clickhouse_command(delete_query)
    print_("Deleted outdated data from %s table..." % table_name)

    run_clickhouse_command(insert_query)
    print_("Inserted fresh data into %s table..." % table_name)

    print_("Data successfully processed for %s table..." % table_name)


def update_connector_data_sync_status(jsonData):
    pattern_format = "%Y-%m-%d %H:%M:%S"
    current_instant = datetime.datetime.utcnow().strftime(pattern_format)
    table_name = "`%s`.connectorDataSyncStatus" % (jsonData["database"])

    insert_query = """
    INSERT INTO %s (accountId, connectorId, lastSuccessfullExecutionAt, jobType, cloudProviderId)
    VALUES ('%s', '%s', '%s', 'cloudfunction', 'AZURE')
    """ % (table_name, jsonData["accountId"], jsonData["connectorId"], current_instant)

    run_clickhouse_command(insert_query)
    print_("Inserted connector sync data into %s table..." % table_name)


def ingest_data_to_costagg(jsonData):
    table_name = "`%s`.costAggregated" % (jsonData["database"])
    year, month = jsonData["reportYear"], jsonData["reportMonth"]
    date_start = "%s-%s-01" % (year, month)
    print_("Processing data for %s table..." % table_name)

    delete_query = """
    DELETE from %s
    WHERE DATE_TRUNC('month', day) = DATE_TRUNC('month', toDateTime('%s 00:00:00'))
    AND cloudProvider = 'AZURE'
    AND accountId='%s'
    """ % (table_name, date_start, jsonData.get("accountId"))

    insert_query = """
    INSERT INTO %s (day, cost, cloudProvider, accountId)
    SELECT date_trunc('day', startTime) AS day, SUM(cost) AS cost, 'AZURE' AS cloudProvider,
    '%s' as accountId
    FROM `%s`.unifiedTable
    WHERE DATE_TRUNC('month', day) = DATE_TRUNC('month', toDateTime('%s 00:00:00')) AND cloudProvider = 'AZURE'
    GROUP BY day
     """ % (table_name, jsonData.get("accountId"), jsonData["database"], date_start)

    run_clickhouse_command(delete_query)
    print_("Deleted outdated data from %s table..." % table_name)

    run_clickhouse_command(insert_query)
    print_("Inserted fresh data into %s table..." % table_name)

    print_("Data successfully processed for %s table..." % table_name)


if __name__=="__main__":
    """
    Triggered from a K8s scheduled job.
    """
    create_db_and_tables("ccm")

    paths = get_blob_path_to_be_ingested()

    errors = 0

    # paths will be in this format: <accountId>/<connectorId>/<tenantId>/<reportName>/<monthFolder>/<randomAzureId>.csv
    # or for partitioned report it will be in this format: <accountId>/<connectorId>/<tenantId>/<reportName>/<monthFolder>/<dateFolder>/<randomAzureId>/*.csv
    if len(paths) == 0:
        print_("No reports to ingest...")
    else :
        for absolutePath in paths:
            print_("Inserting data for %s" % absolutePath)

            try:
                jsonData = {}
                jsonData["database"] = "ccm"

                pathSplit = absolutePath.split("/")
                jsonData["tenant_id"] = pathSplit[2]
                if len(pathSplit) == 6:
                    monthfolder = pathSplit[-2]
                else:
                    monthfolder = pathSplit[-4]

                jsonData["reportYear"] = monthfolder.split("-")[0][:4]
                jsonData["reportMonth"] = monthfolder.split("-")[0][4:6]
                
                jsonData["accountId"] = pathSplit[0]
                jsonData["connectorId"] = pathSplit[1]
                ACCOUNT_ID = jsonData["accountId"]

                accountIdBQ = re.sub('[^0-9a-z]', '_', jsonData.get("accountId").lower())

                jsonData["tableSuffix"] = "%s_%s_%s" % (jsonData["reportYear"], jsonData["reportMonth"], jsonData["connectorId"])
                jsonData["tableName"] = f"azureBilling_{jsonData['tableSuffix']}"

                jsonData["columns"], jsonData["tableSchema"] = get_table_schema(absolutePath)
                azure_column_mapping = set_available_columns(jsonData)

                create_and_ingest_data_into_billing_table(jsonData, azure_column_mapping)
                create_and_ingest_data_into_cost_table(jsonData, azure_column_mapping)

                get_unique_subs_id(jsonData, azure_column_mapping)

                ingest_data_into_preagg(jsonData, azure_column_mapping)
                ingest_data_into_unified(jsonData, azure_column_mapping)

                update_connector_data_sync_status(jsonData)
                ingest_data_to_costagg(jsonData)

            except Exception as e:
                errors = errors + 1
                print_("Failed to insert data for %s " % absolutePath)
                print_("Exception: %s " % e)

            ACCOUNT_ID = ""

    if errors == 0:
        print_("K8S job executed successfully...")
    else:
        print_("K8S job executed with %s errors..." % errors)