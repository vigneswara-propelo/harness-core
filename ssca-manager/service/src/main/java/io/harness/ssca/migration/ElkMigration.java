/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.migration;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.migration.NGMigration;
import io.harness.ssca.entities.ArtifactEntity;
import io.harness.ssca.entities.NormalizedSBOMComponentEntity;
import io.harness.ssca.search.SearchService;

import com.google.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.util.CloseableIterator;

@Slf4j
@OwnedBy(HarnessTeam.SSCA)
public class ElkMigration implements NGMigration {
  @Inject SearchService searchService;
  @Inject private MongoTemplate mongoTemplate;
  public static final int BATCH_SIZE = 1000;
  private static final String ELK_DEBUG_LOG = "[ElkMigration]: ";

  private <T> CloseableIterator<T> runQueryWithBatch(int batchSize, Class<T> entityClass) {
    Query query = new Query();
    query.cursorBatchSize(batchSize);
    return mongoTemplate.stream(query, entityClass);
  }

  private <T> void processEntitiesInBatches(CloseableIterator<T> iterator, int batchSize,
      Function<T, String> groupingFunction, BiConsumer<String, List<T>> processBatchFunction) {
    while (iterator.hasNext()) {
      Map<String, List<T>> batch =
          StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false)
              .limit(batchSize)
              .filter(entity -> groupingFunction.apply(entity) != null)
              .collect(Collectors.groupingBy(groupingFunction, Collectors.toList()));

      if (!batch.isEmpty()) {
        for (Map.Entry<String, List<T>> entry : batch.entrySet()) {
          processBatchFunction.accept(entry.getKey(), entry.getValue());
        }
      }
    }
  }

  private void bulkSyncArtifacts() {
    processEntitiesInBatches(runQueryWithBatch(BATCH_SIZE, ArtifactEntity.class), BATCH_SIZE,
        ArtifactEntity::getAccountId, this::processArtifactGroup);
  }

  private void bulkSyncComponents() {
    processEntitiesInBatches(runQueryWithBatch(BATCH_SIZE, NormalizedSBOMComponentEntity.class), BATCH_SIZE,
        NormalizedSBOMComponentEntity::getAccountId, this::processComponents);
  }

  @Override
  public void migrate() {
    log.info(ELK_DEBUG_LOG + "starting elk migration....");

    try {
      bulkSyncArtifacts();
      bulkSyncComponents();
    } catch (Exception e) {
      searchService.deleteMigrationIndex();
      log.error(ELK_DEBUG_LOG + "elk migration failed....", e);
      throw e;
    }
    log.info(ELK_DEBUG_LOG + "elk migration successful....");
  }

  private void processArtifactGroup(String accountId, List<ArtifactEntity> artifactEntities) {
    if (!searchService.bulkSaveArtifacts(accountId, artifactEntities)) {
      throw new InvalidRequestException(ELK_DEBUG_LOG + "Unable to save bulk artifacts for accountId: " + accountId);
    }
  }

  private void processComponents(
      String accountId, List<NormalizedSBOMComponentEntity> normalizedSBOMComponentEntities) {
    if (!searchService.bulkSaveComponents(accountId, normalizedSBOMComponentEntities)) {
      throw new InvalidRequestException(ELK_DEBUG_LOG + "Unable to save bulk components for accountId: " + accountId);
    }
  }
}
