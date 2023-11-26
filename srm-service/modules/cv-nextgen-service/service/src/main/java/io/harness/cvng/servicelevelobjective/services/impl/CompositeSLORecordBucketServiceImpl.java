/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.servicelevelobjective.services.impl;

import static io.harness.cvng.core.services.CVNextGenConstants.SLI_RECORD_BUCKET_SIZE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.persistence.HQuery.excludeAuthorityCount;

import io.harness.SRMPersistence;
import io.harness.annotations.retry.RetryOnException;
import io.harness.cvng.servicelevelobjective.beans.SLIEvaluationType;
import io.harness.cvng.servicelevelobjective.beans.SLIMissingDataType;
import io.harness.cvng.servicelevelobjective.entities.CompositeSLORecordBucket;
import io.harness.cvng.servicelevelobjective.entities.CompositeSLORecordBucket.CompositeSLORecordBucketKeys;
import io.harness.cvng.servicelevelobjective.entities.CompositeServiceLevelObjective;
import io.harness.cvng.servicelevelobjective.entities.CompositeServiceLevelObjective.ServiceLevelObjectivesDetail;
import io.harness.cvng.servicelevelobjective.entities.SLIRecordBucket;
import io.harness.cvng.servicelevelobjective.services.api.CompositeSLORecordBucketService;
import io.harness.cvng.servicelevelobjective.services.api.SLIRecordBucketService;
import io.harness.cvng.servicelevelobjective.services.api.SLOHealthIndicatorService;
import io.harness.exception.InvalidArgumentsException;

import com.google.inject.Inject;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Sort;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@Slf4j
public class CompositeSLORecordBucketServiceImpl implements CompositeSLORecordBucketService {
  private static final int RETRY_COUNT = 3;
  @Inject private SRMPersistence hPersistence;
  @Inject SLIRecordBucketService sliRecordBucketService;
  @Inject SLOHealthIndicatorService sloHealthIndicatorService;

  @Inject RequestCompositeSLORecordBucketServiceImpl requestCompositeSLORecordBucketService;
  @Inject WindowCompositeSLORecordBucketServiceImpl windowCompositeSLORecordBucketService;
  @Override
  public void create(CompositeServiceLevelObjective compositeServiceLevelObjective, Instant startTime, Instant endTime,
      String verificationTaskId) {
    Pair<Map<ServiceLevelObjectivesDetail, List<SLIRecordBucket>>,
        Map<ServiceLevelObjectivesDetail, SLIMissingDataType>> sloDetailsSLIRecordsAndSLIMissingDataType =
        sliRecordBucketService.getSLODetailsSLIRecordsAndSLIMissingDataType(
            compositeServiceLevelObjective.getServiceLevelObjectivesDetails(), startTime, endTime);
    if (isDataForAllSLIsPresent(compositeServiceLevelObjective, sloDetailsSLIRecordsAndSLIMissingDataType)) {
      Map<ServiceLevelObjectivesDetail, List<SLIRecordBucket>> serviceLevelObjectivesDetailCompositeSLORecordMap =
          sloDetailsSLIRecordsAndSLIMissingDataType.getKey();
      Map<ServiceLevelObjectivesDetail, SLIMissingDataType> objectivesDetailSLIMissingDataTypeMap =
          sloDetailsSLIRecordsAndSLIMissingDataType.getValue();
      CompositeSLORecordBucket lastCompositeSLORecord =
          getLastCompositeSLORecordBucket(compositeServiceLevelObjective.getUuid(), startTime);
      double runningGoodCount =
          Objects.nonNull(lastCompositeSLORecord) && Objects.nonNull(lastCompositeSLORecord.getRunningGoodCount())
          ? lastCompositeSLORecord.getRunningGoodCount()
          : 0;
      double runningBadCount =
          Objects.nonNull(lastCompositeSLORecord) && Objects.nonNull(lastCompositeSLORecord.getRunningGoodCount())
          ? lastCompositeSLORecord.getRunningBadCount()
          : 0;
      if (isWindowStartAfterLatestRecord(startTime, compositeServiceLevelObjective)) {
        updateCompositeSLORecords(serviceLevelObjectivesDetailCompositeSLORecordMap,
            objectivesDetailSLIMissingDataTypeMap, compositeServiceLevelObjective, runningGoodCount, runningBadCount,
            startTime, endTime);
      } else {
        List<CompositeSLORecordBucket> compositeSLORecords = createCompositeSLORecordsFromSLIsDetails(
            serviceLevelObjectivesDetailCompositeSLORecordMap, objectivesDetailSLIMissingDataTypeMap, runningGoodCount,
            runningBadCount, compositeServiceLevelObjective);
        hPersistence.saveBatch(compositeSLORecords);
      }
      sloHealthIndicatorService.upsert(compositeServiceLevelObjective);
    }
  }

  private boolean isWindowStartAfterLatestRecord(
      Instant startTime, CompositeServiceLevelObjective compositeServiceLevelObjective) {
    String compositeSLOId = compositeServiceLevelObjective.getUuid();
    CompositeSLORecordBucket latestCompositeSLORecord = getLatestCompositeSLORecordBucket(compositeSLOId);
    return Objects.nonNull(latestCompositeSLORecord)
        && latestCompositeSLORecord.getBucketStartTime()
               .plus(SLI_RECORD_BUCKET_SIZE, ChronoUnit.MINUTES)
               .isAfter(startTime);
  }

  private static boolean isDataForAllSLIsPresent(CompositeServiceLevelObjective compositeServiceLevelObjective,
      Pair<Map<ServiceLevelObjectivesDetail, List<SLIRecordBucket>>,
          Map<ServiceLevelObjectivesDetail, SLIMissingDataType>> sloDetailsSLIRecordsAndSLIMissingDataType) {
    return !isEmpty(sloDetailsSLIRecordsAndSLIMissingDataType.getKey())
        && sloDetailsSLIRecordsAndSLIMissingDataType.getKey().size()
        == compositeServiceLevelObjective.getServiceLevelObjectivesDetails().size();
  }

  @Override
  public CompositeSLORecordBucket getLatestCompositeSLORecordBucket(String verificationTaskId) {
    return hPersistence.createQuery(CompositeSLORecordBucket.class, excludeAuthorityCount)
        .filter(CompositeSLORecordBucketKeys.verificationTaskId, verificationTaskId)
        .order(Sort.descending(CompositeSLORecordBucketKeys.bucketStartTime))
        .get();
  }

  @Override
  public CompositeSLORecordBucket getLastCompositeSLORecordBucket(String verificationTaskId, Instant startTimeStamp) {
    return hPersistence.createQuery(CompositeSLORecordBucket.class, excludeAuthorityCount)
        .filter(CompositeSLORecordBucketKeys.verificationTaskId, verificationTaskId)
        .field(CompositeSLORecordBucketKeys.bucketStartTime)
        .lessThan(startTimeStamp)
        .order(Sort.descending(CompositeSLORecordBucketKeys.bucketStartTime))
        .get();
  }

  @Override
  public List<CompositeSLORecordBucket> getSLORecordBuckets(
      String verificationTaskId, Instant startTimeStamp, Instant endTimeStamp) {
    return hPersistence.createQuery(CompositeSLORecordBucket.class, excludeAuthorityCount)
        .filter(CompositeSLORecordBucketKeys.verificationTaskId, verificationTaskId)
        .field(CompositeSLORecordBucketKeys.bucketStartTime)
        .greaterThanOrEq(startTimeStamp)
        .field(CompositeSLORecordBucketKeys.bucketStartTime)
        .lessThan(endTimeStamp)
        .order(Sort.ascending(CompositeSLORecordBucketKeys.bucketStartTime))
        .asList();
  }

  @Override
  public List<CompositeSLORecordBucket> getLatestCountSLORecords(String verificationTaskId, int count) {
    return hPersistence.createQuery(CompositeSLORecordBucket.class, excludeAuthorityCount)
        .filter(CompositeSLORecordBucketKeys.verificationTaskId, verificationTaskId)
        .order(Sort.descending(CompositeSLORecordBucketKeys.bucketStartTime))
        .asList(new FindOptions().limit(count));
  }

  @RetryOnException(retryCount = RETRY_COUNT, retryOn = ConcurrentModificationException.class)
  public void updateCompositeSLORecords(
      Map<ServiceLevelObjectivesDetail, List<SLIRecordBucket>> serviceLevelObjectivesDetailCompositeSLORecordMap,
      Map<ServiceLevelObjectivesDetail, SLIMissingDataType> objectivesDetailSLIMissingDataTypeMap,
      CompositeServiceLevelObjective serviceLevelObjective, double runningGoodCount, double runningBadCount,
      Instant startTime, Instant endTime) {
    String verificationTaskId = serviceLevelObjective.getUuid();
    SLIEvaluationType sliEvaluationType = serviceLevelObjective.getSliEvaluationType();
    List<CompositeSLORecordBucket> sloRecordBucketstoBeUpdated =
        getSLORecordBuckets(verificationTaskId, startTime, endTime.plus(1, ChronoUnit.MINUTES));
    Map<Instant, CompositeSLORecordBucket> sloRecordBucketstoBeUpdatedMap =
        sloRecordBucketstoBeUpdated.stream().collect(Collectors.toMap(
            CompositeSLORecordBucket::getBucketStartTime, Function.identity(), (sloRecordBucket1, sloRecordBucket2) -> {
              log.info("Duplicate SLO Key detected sloId: {}, timeStamp: {}", serviceLevelObjective.getUuid(),
                  sloRecordBucket1.getBucketStartTime());
              return sloRecordBucket1.getLastUpdatedAt() > sloRecordBucket2.getLastUpdatedAt() ? sloRecordBucket1
                                                                                               : sloRecordBucket2;
            }));
    List<CompositeSLORecordBucket> sloRecordBucketsToUpdateOrCreate;
    if (sliEvaluationType == SLIEvaluationType.WINDOW) {
      sloRecordBucketsToUpdateOrCreate = windowCompositeSLORecordBucketService.upsertWindowCompositeSLORecordBuckets(
          serviceLevelObjectivesDetailCompositeSLORecordMap, objectivesDetailSLIMissingDataTypeMap, runningGoodCount,
          runningBadCount, sloRecordBucketstoBeUpdatedMap, serviceLevelObjective);
    } else if (sliEvaluationType == SLIEvaluationType.REQUEST) {
      sloRecordBucketsToUpdateOrCreate = requestCompositeSLORecordBucketService.upsertRequestCompositeSLORecordBuckets(
          serviceLevelObjectivesDetailCompositeSLORecordMap, serviceLevelObjective.getVersion(), verificationTaskId,
          sloRecordBucketstoBeUpdatedMap);
    } else {
      throw new InvalidArgumentsException("Invalid Evaluation Type");
    }

    try {
      hPersistence.upsertBatch(CompositeSLORecordBucket.class, sloRecordBucketsToUpdateOrCreate, new ArrayList<>());
    } catch (IllegalAccessException exception) {
      log.error("SLO Records update failed through Bulk update {}", exception.getLocalizedMessage());
      hPersistence.save(sloRecordBucketsToUpdateOrCreate);
    }
  }

  public List<CompositeSLORecordBucket> createCompositeSLORecordsFromSLIsDetails(
      Map<ServiceLevelObjectivesDetail, List<SLIRecordBucket>> serviceLevelObjectivesDetailCompositeSLORecordMap,
      Map<ServiceLevelObjectivesDetail, SLIMissingDataType> objectivesDetailSLIMissingDataTypeMap,
      double runningGoodCount, double runningBadCount, CompositeServiceLevelObjective compositeServiceLevelObjective) {
    SLIEvaluationType sliEvaluationType = compositeServiceLevelObjective.getSliEvaluationType();
    if (sliEvaluationType == SLIEvaluationType.REQUEST) {
      return requestCompositeSLORecordBucketService.createRequestCompositeSLORecordsFromSLIsDetails(
          serviceLevelObjectivesDetailCompositeSLORecordMap, compositeServiceLevelObjective);
    } else {
      return windowCompositeSLORecordBucketService.createWindowCompositeSLORecordsFromSLIsDetails(
          serviceLevelObjectivesDetailCompositeSLORecordMap, objectivesDetailSLIMissingDataTypeMap, runningGoodCount,
          runningBadCount, compositeServiceLevelObjective);
    }
  }
}
