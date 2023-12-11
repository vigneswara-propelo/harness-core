# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

from typing import Any
from google.cloud.bigquery.table import TableReference
from abc import ABC, abstractmethod

class BillingHelper(ABC):
    @abstractmethod
    def create_dataset(self, datasetName: Any, accountId: str = "") -> None:
        pass
    
    @abstractmethod
    def if_tbl_exists(self, table_ref: TableReference) -> bool:
        pass

    @abstractmethod
    def createTable(self, table_ref: TableReference) -> Any:
        pass

    @abstractmethod
    def alter_unified_table(self, jsonData: Any) -> None:
        pass

    @abstractmethod
    def add_currency_preferences_columns_to_schema(self, table_ids: Any) -> None:
        pass

    @abstractmethod
    def get_preferred_currency(self, jsonData: Any) -> None:
        pass

    @abstractmethod
    def trigger_historical_cost_update_in_preferred_currency(self, jsonData: Any) -> None:
        pass

    @abstractmethod
    def get_impersonated_credentials(self, jsonData: Any) -> None:
        pass

    @abstractmethod
    def send_cost_category_update_event(self, jsonData: Any) -> None:
        pass

    @abstractmethod
    def isFreshSync(self, jsonData: Any) -> Any:
        pass

    @abstractmethod
    def compute_sync_interval(self, jsonData: Any) -> None:
        pass

    def check_if_billing_export_is_detailed(self, jsonData: Any) -> bool:
        pass

    @abstractmethod
    def ingest_into_gcp_billing_export_table(self, destination: str, jsonData: Any) -> None:
        pass

    @abstractmethod
    def get_unique_billingaccount_id(self, jsonData: Any) -> None:
        pass

    @abstractmethod
    def insert_currencies_with_unit_conversion_factors_in_bq(self, jsonData: Any) -> None:
        pass

    @abstractmethod
    def initialize_fx_rates_dict(self, jsonData: Any) -> None:
        pass

    @abstractmethod
    def fetch_default_conversion_factors_from_API(self, jsonData: Any) -> None:
        pass

    @abstractmethod
    def fetch_default_conversion_factors_from_billing_export(self, jsonData: Any) -> None:
        pass

    @abstractmethod
    def fetch_custom_conversion_factors(self, jsonData: Any) -> None:
        pass

    @abstractmethod
    def verify_existence_of_required_conversion_factors(self, jsonData: Any) -> None:
        pass

    @abstractmethod
    def update_fx_rate_column_in_raw_table(self, jsonData: Any) -> None:
        pass

    @abstractmethod
    def alter_cost_export_table(self, jsonData: Any) -> None:
        pass

    @abstractmethod
    def ingest_into_gcp_cost_export_table(self, gcp_cost_export_table_name: str, jsonData: Any) -> None:
        pass
    
    @abstractmethod
    def ingest_into_preaggregated(self, jsonData: Any) -> None:
        pass

    @abstractmethod
    def ingest_into_unified(self, jsonData: Any) -> None:
        pass

    @abstractmethod
    def update_connector_data_sync_status(self, jsonData: Any) -> None:
        pass

    @abstractmethod
    def ingest_data_to_costagg(self, jsonData: Any) -> None:
        pass