/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.batch.processing.billingdataverification;

import io.harness.aws.AwsClientImpl;
import io.harness.batch.processing.cloudevents.aws.ecs.service.tasklet.support.ng.NGConnectorHelper;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.shard.AccountShardService;
import io.harness.ccm.billingDataVerification.dto.CCMBillingDataVerificationCost;
import io.harness.ccm.billingDataVerification.dto.CCMBillingDataVerificationKey;
import io.harness.ccm.billingDataVerification.utils.AwsBillingDataVerificationService;
import io.harness.ccm.service.billingDataVerification.service.BillingDataVerificationSQLService;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.delegate.beans.connector.CEFeatures;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.awsconnector.CrossAccountAccessDTO;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsConnectorDTO;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.google.inject.Singleton;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Singleton
@Slf4j
public class BillingDataVerificationService {
  @Autowired private AccountShardService accountShardService;
  @Autowired private AwsBillingDataVerificationService awsBillingDataVerificationService;
  @Autowired private NGConnectorHelper ngConnectorHelper;
  @Autowired BillingDataVerificationSQLService billingDataVerificationSQLService;
  @Autowired BatchMainConfig configuration;
  @Autowired AwsClientImpl awsClient;

  public void verifyBillingData() {
    List<String> accountIds = accountShardService.getCeEnabledAccountIds();
    accountIds.forEach(accountId -> { verifyAwsBillingDataForAccount(accountId); });
  }

  private void verifyAwsBillingDataForAccount(String accountId) {
    if (!configuration.getBillingDataVerificationJobConfig().getAwsBillingDataVerificationEnabled()) {
      log.info("Billing-Data verification is disabled for AWS. Skipping AWS billing-data verification.");
      return;
    }
    String startDate =
        LocalDate.now(ZoneOffset.UTC)
            .withDayOfMonth(1)
            .minusMonths(
                configuration.getBillingDataVerificationJobConfig().getAwsMaximumHistoricalMonthsForVerificationJob())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    String endDate =
        LocalDate.now(ZoneOffset.UTC).plusMonths(1).withDayOfMonth(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    log.info("Identified from config - startDate: {}, endDate: {}", startDate, endDate);
    Map<CCMBillingDataVerificationKey, CCMBillingDataVerificationCost> billingData = new HashMap<>();
    List<ConnectorResponseDTO> awsBillingConnectors = ngConnectorHelper.getNextGenConnectors(
        accountId, Arrays.asList(ConnectorType.CE_AWS), Arrays.asList(CEFeatures.BILLING), Collections.emptyList());
    for (ConnectorResponseDTO awsBillingConnector : awsBillingConnectors) {
      try {
        CrossAccountAccessDTO crossAccountAccessDTO =
            ((CEAwsConnectorDTO) awsBillingConnector.getConnector().getConnectorConfig()).getCrossAccountAccess();
        awsBillingDataVerificationService.fetchAndUpdateBillingDataForConnector(accountId, awsBillingConnector,
            startDate, endDate, getAssumedCredentialProvider(crossAccountAccessDTO), billingData);
      } catch (Exception ex) {
        log.error("Exception while fetching AWS billing data for connector: {}",
            awsBillingConnector.getConnector().getIdentifier(), ex);
      }
    }
    // insert billingData in ccmBillingDataVerificationTable in BQ/CH
    try {
      billingDataVerificationSQLService.ingestAWSCostsIntoBillingDataVerificationTable(accountId, billingData);
      log.info(
          "Billing data verification job completed successfully for cloudProvider: 'AWS', accountId: {}", accountId);
    } catch (Exception ex) {
      log.error(
          "Exception while ingesting data into CCMBillingDataVerificationTable! billingData: {}", billingData, ex);
    }
  }

  public AWSCredentialsProvider getAssumedCredentialProvider(CrossAccountAccessDTO crossAccountAccessDTO) {
    final AWSCredentialsProvider BasicAwsCredentials = awsClient.constructStaticBasicAwsCredentials(
        configuration.getAwsS3SyncConfig().getAwsAccessKey(), configuration.getAwsS3SyncConfig().getAwsSecretKey());
    final AWSCredentialsProvider credentialsProvider = awsClient.getAssumedCredentialsProviderWithRegion(
        BasicAwsCredentials, crossAccountAccessDTO.getCrossAccountRoleArn(), crossAccountAccessDTO.getExternalId(),
        configuration.getAwsS3SyncConfig().getRegion(), configuration.getCeAwsServiceEndpointConfig());
    credentialsProvider.getCredentials();
    return credentialsProvider;
  }
}
