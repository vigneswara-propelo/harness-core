/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.migration.list;

import io.harness.SRMPersistence;
import io.harness.cvng.migration.CVNGMigration;
import io.harness.cvng.migration.beans.CVNGSchema;
import io.harness.cvng.migration.beans.CVNGSchema.CVNGSchemaKeys;
import io.harness.cvng.migration.beans.ChecklistItem;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelObjectiveType;
import io.harness.cvng.servicelevelobjective.entities.AbstractServiceLevelObjective;
import io.harness.cvng.servicelevelobjective.entities.CompositeSLORecord;
import io.harness.cvng.servicelevelobjective.entities.CompositeSLORecord.CompositeSLORecordKeys;
import io.harness.cvng.servicelevelobjective.entities.CompositeSLORecordBucket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import dev.morphia.query.Query;
import dev.morphia.query.Sort;
import dev.morphia.query.UpdateOperations;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CompositeSLORecordBucketMigration implements CVNGMigration {
  public static final String EMPTY_JSON_ARRAY = "[]";
  @Inject SRMPersistence hPersistence;

  private static final int BATCH_SIZE = 1000;

  List<CompositeSLORecordBucket> sloRecordBucketsToSave;

  ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void migrate() {
    CVNGSchema cvngSchema = hPersistence.createQuery(CVNGSchema.class).get();
    try {
      if (cvngSchema.getObject() == null || EMPTY_JSON_ARRAY.equals(cvngSchema.getObject())) {
        addSLOIdsInCVNGSchemaObject();
      }
      List<String> sloIds = getSLOIdsFromSchemaObject();
      while (!sloIds.isEmpty()) {
        Query<CompositeSLORecord> queryToAddToBucket =
            hPersistence.createQuery(CompositeSLORecord.class)
                .filter(CompositeSLORecordKeys.verificationTaskId, sloIds.get(0))
                .order(Sort.ascending(CompositeSLORecordKeys.timestamp));
        List<CompositeSLORecord> currentBucket = new ArrayList<>();
        sloRecordBucketsToSave = new ArrayList<>();
        int offset = 0;
        while (true) {
          Query<CompositeSLORecord> query = queryToAddToBucket.offset(offset).limit(BATCH_SIZE);
          List<CompositeSLORecord> records = query.find().toList();
          if (records.isEmpty()) {
            break;
          }
          for (CompositeSLORecord compositeSLORecord : records) {
            currentBucket = processList(currentBucket, compositeSLORecord);
            currentBucket = processListAndCreateBucket(currentBucket);
          }
          offset += BATCH_SIZE;
        }
        saveBucketsInBatch();
        sloIds.remove(0);
        updateSLOIdsInSchemaObject(sloIds);
      }
      log.info("[Composite SLO Bucket Migration] Saved all sli bucket records");
    } catch (Exception exception) {
      log.error("[Composite SLO Bucket Migration] Failed to migrate SLI Records {}", exception);
    }
  }

  private void updateSLOIdsInSchemaObject(List<String> sloIds) throws JsonProcessingException {
    String jsonString = objectMapper.writeValueAsString(sloIds);
    final UpdateOperations<CVNGSchema> updateOperations = hPersistence.createUpdateOperations(CVNGSchema.class);
    updateOperations.set(CVNGSchemaKeys.object, jsonString);
    hPersistence.update(hPersistence.createQuery(CVNGSchema.class), updateOperations);
  }

  private void addSLOIdsInCVNGSchemaObject() throws JsonProcessingException {
    List<String> sloIds = fetchAllSLOIds();
    String jsonString = objectMapper.writeValueAsString(sloIds);
    final UpdateOperations<CVNGSchema> updateOperations = hPersistence.createUpdateOperations(CVNGSchema.class);
    updateOperations.set(CVNGSchemaKeys.object, jsonString);
    hPersistence.update(hPersistence.createQuery(CVNGSchema.class), updateOperations);
  }

  private List<String> getSLOIdsFromSchemaObject() throws JsonProcessingException {
    CVNGSchema cvngSchema = hPersistence.createQuery(CVNGSchema.class).get();
    String retrievedJsonString = (String) cvngSchema.getObject();
    return objectMapper.readValue(retrievedJsonString, new TypeReference<>() {});
  }

  private List<String> fetchAllSLOIds() {
    List<AbstractServiceLevelObjective> serviceLevelIndicators =
        hPersistence.createQuery(AbstractServiceLevelObjective.class)
            .filter(AbstractServiceLevelObjective.ServiceLevelObjectiveV2Keys.type, ServiceLevelObjectiveType.COMPOSITE)
            .asList();
    return serviceLevelIndicators.stream().map(AbstractServiceLevelObjective::getUuid).collect(Collectors.toList());
  }
  private List<CompositeSLORecord> processList(List<CompositeSLORecord> currentBucket, CompositeSLORecord sloRecord) {
    if (!currentBucket.isEmpty()
        && (currentBucket.get(currentBucket.size() - 1).getEpochMinute() + 1 == sloRecord.getEpochMinute())
        && (currentBucket.get(currentBucket.size() - 1).getSloId().equals(sloRecord.getSloId()))) {
      currentBucket.add(sloRecord);
      return currentBucket;
    }
    currentBucket = new ArrayList<>();
    if (sloRecord.getEpochMinute() % 5 == 0) {
      currentBucket.add(sloRecord);
    }
    return currentBucket;
  }

  private List<CompositeSLORecord> processListAndCreateBucket(List<CompositeSLORecord> currentBucket) {
    if (currentBucket.size() == 5) {
      CompositeSLORecordBucket compositeSLORecordBucket =
          CompositeSLORecordBucket.getCompositeSLORecordBucketFromCompositeSLORecords(currentBucket);
      sloRecordBucketsToSave.add(compositeSLORecordBucket);
      currentBucket = new ArrayList<>();
    }
    if (sloRecordBucketsToSave.size() >= BATCH_SIZE) {
      saveBucketsInBatch();
    }
    return currentBucket;
  }

  private void saveBucketsInBatch() {
    try {
      if (!sloRecordBucketsToSave.isEmpty()) {
        hPersistence.upsertBatch(CompositeSLORecordBucket.class, sloRecordBucketsToSave, new ArrayList<>());
      }
      sloRecordBucketsToSave = new ArrayList<>();
    } catch (IllegalAccessException e) {
      throw new RuntimeException("[SLI Record Bucketing Error]", e);
    }
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
