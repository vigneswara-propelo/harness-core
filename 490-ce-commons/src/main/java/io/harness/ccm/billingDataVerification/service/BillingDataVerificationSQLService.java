/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.service.billingDataVerification.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.billingDataVerification.dto.CCMBillingDataVerificationCost;
import io.harness.ccm.billingDataVerification.dto.CCMBillingDataVerificationKey;
import io.harness.connector.ConnectorResponseDTO;

import java.util.Map;

@OwnedBy(HarnessTeam.CE)
public interface BillingDataVerificationSQLService {
  Map<CCMBillingDataVerificationKey, CCMBillingDataVerificationCost> fetchAWSCostsFromAWSBillingTables(
      String accountId, ConnectorResponseDTO connector, String startDate, String endDate) throws Exception;
  Map<CCMBillingDataVerificationKey, CCMBillingDataVerificationCost> fetchAWSCostsFromUnifiedTable(
      String accountId, ConnectorResponseDTO connector, String startDate, String endDate) throws Exception;
  void ingestAWSCostsIntoBillingDataVerificationTable(String accountId,
      Map<CCMBillingDataVerificationKey, CCMBillingDataVerificationCost> billingData) throws Exception;
}
