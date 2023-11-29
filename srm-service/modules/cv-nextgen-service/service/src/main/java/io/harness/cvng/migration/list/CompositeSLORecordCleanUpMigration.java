/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.migration.list;

import io.harness.SRMPersistence;
import io.harness.cvng.CVConstants;
import io.harness.cvng.core.utils.CVNGObjectUtils;
import io.harness.cvng.migration.CVNGMigration;
import io.harness.cvng.migration.beans.ChecklistItem;
import io.harness.cvng.servicelevelobjective.entities.CompositeSLORecord;
import io.harness.cvng.servicelevelobjective.entities.CompositeSLORecord.CompositeSLORecordKeys;
import io.harness.cvng.servicelevelobjective.entities.SLIRecordBucket;
import io.harness.cvng.servicelevelobjective.entities.SLIRecordBucket.SLIRecordBucketKeys;
import io.harness.persistence.UuidAware;

import com.google.inject.Inject;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CompositeSLORecordCleanUpMigration implements CVNGMigration {
  @Inject SRMPersistence hPersistence;

  private static final int BATCH_SIZE = 1000;
  @Override
  public void migrate() {
    cleanUpOlderCompositeSLORecords();
    cleanUpOlderSLIRecordBuckets();
  }

  private void cleanUpOlderSLIRecordBuckets() {
    log.info("Starting cleanup for sli Bucket records");
    Query<SLIRecordBucket> queryToDeleteOlderBucketRecords =
        hPersistence.createQuery(SLIRecordBucket.class)
            .field(SLIRecordBucketKeys.bucketStartTime)
            .lessThan(Date.from(OffsetDateTime.now().minusDays(CVConstants.SLO_RECORDS_TTL_DAYS).toInstant()))
            .project(UuidAware.UUID_KEY, true);
    while (true) {
      List<SLIRecordBucket> recordsToBeDeleted =
          queryToDeleteOlderBucketRecords.find(new FindOptions().limit(BATCH_SIZE)).toList();
      Set<?> recordIdsTobeDeleted = recordsToBeDeleted.stream()
                                        .map(recordToBeDeleted -> ((UuidAware) recordToBeDeleted).getUuid())
                                        .map(CVNGObjectUtils::convertToObjectIdIfRequired)
                                        .collect(Collectors.toSet());
      if (recordIdsTobeDeleted.isEmpty()) {
        break;
      }
      hPersistence.delete(
          hPersistence.createQuery(SLIRecordBucket.class).field(UuidAware.UUID_KEY).in(recordIdsTobeDeleted));
      log.info(
          "[CompositeSLORecord Bucket Migration] Deleted {} old sliRecordBucket records", recordsToBeDeleted.size());
    }
    log.info("[CompositeSLORecord Bucket Migration] Deleted all old sliRecordBucket records");
  }

  private void cleanUpOlderCompositeSLORecords() {
    log.info("Starting cleanup for older composite slo records and creating bucket for newer composite slo records");
    Query<CompositeSLORecord> queryToDeleteOlderRecords =
        hPersistence.createQuery(CompositeSLORecord.class)
            .field(CompositeSLORecordKeys.timestamp)
            .lessThan(Date.from(OffsetDateTime.now().minusDays(CVConstants.SLO_RECORDS_TTL_DAYS).toInstant()))
            .project(UuidAware.UUID_KEY, true);
    while (true) {
      List<CompositeSLORecord> recordsToBeDeleted =
          queryToDeleteOlderRecords.find(new FindOptions().limit(BATCH_SIZE)).toList();
      Set<?> recordIdsTobeDeleted = recordsToBeDeleted.stream()
                                        .map(recordToBeDeleted -> ((UuidAware) recordToBeDeleted).getUuid())
                                        .map(CVNGObjectUtils::convertToObjectIdIfRequired)
                                        .collect(Collectors.toSet());
      if (recordIdsTobeDeleted.isEmpty()) {
        break;
      }
      hPersistence.delete(
          hPersistence.createQuery(CompositeSLORecord.class).field(UuidAware.UUID_KEY).in(recordIdsTobeDeleted));
      log.info(
          "[CompositeSLORecord Bucket Migration] Deleted {} old CompositeSLORecord records", recordsToBeDeleted.size());
    }
    log.info("[CompositeSLORecord Bucket Migration] Deleted all old CompositeSLORecord records");
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
