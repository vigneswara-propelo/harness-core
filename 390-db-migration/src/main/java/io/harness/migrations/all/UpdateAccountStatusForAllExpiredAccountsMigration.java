/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.remote.client.NGRestUtils.getResponse;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.beans.response.CheckExpiryResultDTO;
import io.harness.licensing.remote.NgLicenseHttpClient;
import io.harness.logging.MdcContextSetter;
import io.harness.logging.ResponseTimeRecorder;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.Account;
import software.wings.beans.Account.AccountKeys;
import software.wings.beans.account.AccountStatus;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;

import com.google.inject.Inject;
import dev.morphia.query.Query;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.GTM)
public class UpdateAccountStatusForAllExpiredAccountsMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private NgLicenseHttpClient ngLicenseHttpClient;
  @Inject private AccountService accountService;

  private static final String UPDATE_ACCOUNT_STATUS_MIGRATION_LOGS_KEY = "updateAccountStatusMigration";
  private static final int maxDocumentsToBeFetched = Integer.MAX_VALUE;

  @Override
  public void migrate() {
    log.info(
        "Starting the migration to mark all the EXPIRED accounts as ACTIVE which have at-least 1 active NG license");

    Query<Account> accountsQuery =
        wingsPersistence.createQuery(Account.class).field(AccountKeys.accountStatusKey).equal(AccountStatus.EXPIRED);
    accountsQuery.limit(maxDocumentsToBeFetched);
    log.info("Migration will run for total {} expired accounts", accountsQuery.count());
    Set<String> accountsLeftForMigration = new HashSet<>();

    try (ResponseTimeRecorder ignore = new ResponseTimeRecorder(
             "Update licenseInfo.accountStatus field for all the expired accounts BG Migration Job");
         MdcContextSetter ignore1 =
             new MdcContextSetter(Map.of(UPDATE_ACCOUNT_STATUS_MIGRATION_LOGS_KEY, generateUuid()));) {
      try (HIterator<Account> records = new HIterator<>(accountsQuery.fetch())) {
        while (records.hasNext()) {
          Account account = null;
          try {
            account = records.next();
            CheckExpiryResultDTO ngLicenseDecision = getResponse(ngLicenseHttpClient.checkExpiry(account.getUuid()));
            if (ngLicenseDecision.isNgAccountActive()) {
              accountService.updateAccountStatus(account.getUuid(), AccountStatus.ACTIVE);

              log.info("licenseInfo.accountStatus field is successfully updated from EXPIRED to ACTIVE for account: {}",
                  account.getUuid());
            }
          } catch (Exception e) {
            accountsLeftForMigration.add(account.getUuid());
            log.error("Error while updating licenseInfo.accountStatus field for account: {}", account.getUuid(), e);
          }
        }
      } finally {
        log.info("Total accounts with failed migration= {} and the list= {}", accountsLeftForMigration.size(),
            accountsLeftForMigration);
      }
    }

    log.info("Migration to mark all the EXPIRED accounts as ACTIVE which have at-least 1 active NG license finished");
  }
}
