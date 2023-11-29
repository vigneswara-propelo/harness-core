/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.migration.list;

import io.harness.cvng.core.utils.CVNGObjectUtils;
import io.harness.cvng.migration.CVNGMigration;
import io.harness.cvng.migration.beans.ChecklistItem;
import io.harness.cvng.servicelevelobjective.entities.CompositeSLORecordBucket;
import io.harness.persistence.HPersistence;
import io.harness.persistence.UuidAware;

import com.google.inject.Inject;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CompositeSLORecordBucketCleanupMigration implements CVNGMigration {
  @Inject HPersistence hPersistence;

  private static final int BATCH_SIZE = 1000;
  @Override
  public void migrate() {
    log.info("Starting cleanup for composite slo Bucket records");
    Query<CompositeSLORecordBucket> queryToDeleteOlderBucketRecords =
        hPersistence.createQuery(CompositeSLORecordBucket.class).project(UuidAware.UUID_KEY, true);
    while (true) {
      List<CompositeSLORecordBucket> recordsToBeDeleted =
          queryToDeleteOlderBucketRecords.find(new FindOptions().limit(BATCH_SIZE)).toList();
      Set<?> recordIdsTobeDeleted = recordsToBeDeleted.stream()
                                        .map(recordToBeDeleted -> ((UuidAware) recordToBeDeleted).getUuid())
                                        .map(CVNGObjectUtils::convertToObjectIdIfRequired)
                                        .collect(Collectors.toSet());
      if (recordIdsTobeDeleted.isEmpty()) {
        break;
      }
      hPersistence.delete(
          hPersistence.createQuery(CompositeSLORecordBucket.class).field(UuidAware.UUID_KEY).in(recordIdsTobeDeleted));
      log.info("[Composite SLO Bucket Migration] Deleted {} composite slo bucket records", recordsToBeDeleted.size());
    }
    log.info("[Composite SLO Bucket Migration] Deleted all composite slo bucket records");
  }

  @Override
  public ChecklistItem whatHappensOnRollback() {
    return ChecklistItem.NA;
  }

  @Override
  public ChecklistItem whatHappensIfOldVersionIteratorPicksMigratedEntity() {
    return ChecklistItem.NA;
  }
}
