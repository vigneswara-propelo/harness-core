/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.servicelevelobjective.services.impl;

import static io.harness.cvng.core.services.CVNextGenConstants.SLI_RECORD_BUCKET_SIZE;

import io.harness.cvng.servicelevelobjective.beans.CompositeSLOFormulaType;
import io.harness.cvng.servicelevelobjective.beans.SLIMissingDataType;
import io.harness.cvng.servicelevelobjective.beans.slospec.CompositeSLOEvaluator;
import io.harness.cvng.servicelevelobjective.entities.CompositeSLORecordBucket;
import io.harness.cvng.servicelevelobjective.entities.CompositeServiceLevelObjective;
import io.harness.cvng.servicelevelobjective.entities.SLIRecordBucket;
import io.harness.cvng.servicelevelobjective.entities.SLIState;

import com.google.common.collect.ImmutableSortedSet;
import com.google.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Builder;
import lombok.Data;

public class WindowCompositeSLORecordBucketServiceImpl {
  @Inject Map<CompositeSLOFormulaType, CompositeSLOEvaluator> formulaTypeCompositeSLOEvaluatorMap;
  List<CompositeSLORecordBucket> upsertWindowCompositeSLORecordBuckets(
      Map<CompositeServiceLevelObjective.ServiceLevelObjectivesDetail, List<SLIRecordBucket>>
          serviceLevelObjectivesDetailCompositeSLORecordMap,
      Map<CompositeServiceLevelObjective.ServiceLevelObjectivesDetail, SLIMissingDataType>
          objectivesDetailSLIMissingDataTypeMap,
      double previousRunningGoodCount, double previousRunningBadCount,
      Map<Instant, CompositeSLORecordBucket> sloRecordBucketMap,
      CompositeServiceLevelObjective compositeServiceLevelObjective) {
    int sloVersion = compositeServiceLevelObjective.getVersion();
    String verificationTaskId = compositeServiceLevelObjective.getUuid();
    CompositeSLOFormulaType sloFormulaType = compositeServiceLevelObjective.getCompositeSLOFormulaType();
    List<CompositeSLORecordBucket> updateOrCreateSLORecords = new ArrayList<>();
    GoodBadTotalWeightage goodBadTotalWeightage = getTimeStampMapsForGoodBadTotal(
        serviceLevelObjectivesDetailCompositeSLORecordMap, objectivesDetailSLIMissingDataTypeMap);
    int minute = 0;
    for (Instant instant : ImmutableSortedSet.copyOf(
             goodBadTotalWeightage.getTimeStampToTotalValueWeightage().keySet())) { // maybe we need a better iteration
      if (isAllSLIsPresent(serviceLevelObjectivesDetailCompositeSLORecordMap, instant,
              goodBadTotalWeightage.timeStampToTotalValueWeightage)) {
        org.apache.commons.math3.util.Pair<Double, Double> currentCountPair =
            getCurrentCount(instant, goodBadTotalWeightage, sloFormulaType);
        double currentGoodCount = currentCountPair.getFirst();
        double currentBadCount = currentCountPair.getSecond();
        previousRunningGoodCount += currentGoodCount;
        previousRunningBadCount += currentBadCount;
      }
      if ((minute + 1) % SLI_RECORD_BUCKET_SIZE == 0) {
        CompositeSLORecordBucket sloRecordBucket = sloRecordBucketMap.get(
            instant.minus(SLI_RECORD_BUCKET_SIZE - 1, ChronoUnit.MINUTES)); // map is of bucket startTime
        if (Objects.nonNull(sloRecordBucket)) {
          sloRecordBucket.setRunningGoodCount(previousRunningGoodCount);
          sloRecordBucket.setRunningBadCount(previousRunningBadCount);
          sloRecordBucket.setSloVersion(sloVersion);
        } else {
          sloRecordBucket = CompositeSLORecordBucket.builder()
                                .runningBadCount(previousRunningBadCount)
                                .runningGoodCount(previousRunningGoodCount)
                                .sloVersion(sloVersion)
                                .verificationTaskId(verificationTaskId)
                                .bucketStartTime(instant)
                                .build();
        }
        updateOrCreateSLORecords.add(sloRecordBucket);
      }
      minute++;
    }
    return updateOrCreateSLORecords;
  }

  private static boolean isAllSLIsPresent(
      Map<CompositeServiceLevelObjective.ServiceLevelObjectivesDetail, List<SLIRecordBucket>>
          serviceLevelObjectivesDetailCompositeSLORecordMap,
      Instant instant, Map<Instant, Integer> timeStampToTotalValue) {
    return timeStampToTotalValue.get(instant).equals(serviceLevelObjectivesDetailCompositeSLORecordMap.size());
  }

  List<CompositeSLORecordBucket> createWindowCompositeSLORecordsFromSLIsDetails(
      Map<CompositeServiceLevelObjective.ServiceLevelObjectivesDetail, List<SLIRecordBucket>>
          serviceLevelObjectivesDetailCompositeSLORecordMap,
      Map<CompositeServiceLevelObjective.ServiceLevelObjectivesDetail, SLIMissingDataType>
          objectivesDetailSLIMissingDataTypeMap,
      double runningGoodCount, double runningBadCount, CompositeServiceLevelObjective compositeServiceLevelObjective) {
    int sloVersion = compositeServiceLevelObjective.getVersion();
    String verificationTaskId = compositeServiceLevelObjective.getUuid();
    CompositeSLOFormulaType sloFormulaType = compositeServiceLevelObjective.getCompositeSLOFormulaType();
    GoodBadTotalWeightage goodBadTotalWeightage = getTimeStampMapsForGoodBadTotal(
        serviceLevelObjectivesDetailCompositeSLORecordMap, objectivesDetailSLIMissingDataTypeMap);
    List<CompositeSLORecordBucket> sloRecordList = new ArrayList<>();
    int minute = 0;
    for (Instant instant :
        ImmutableSortedSet.copyOf(goodBadTotalWeightage.getTimeStampToTotalValueWeightage().keySet())) {
      if (isAllSLIsPresent(serviceLevelObjectivesDetailCompositeSLORecordMap, instant,
              goodBadTotalWeightage.getTimeStampToTotalValueWeightage())) {
        CountsWithWeightages goodCountsWithWeightages =
            getCountWithWeightage(instant, goodBadTotalWeightage, SLIState.GOOD);
        CountsWithWeightages badCountsWithWeightages =
            getCountWithWeightage(instant, goodBadTotalWeightage, SLIState.BAD);
        org.apache.commons.math3.util.Pair<Double, Double> currentCount =
            formulaTypeCompositeSLOEvaluatorMap.get(sloFormulaType)
                .evaluate(goodCountsWithWeightages.getWeightages(), goodCountsWithWeightages.getCounts(),
                    badCountsWithWeightages.getCounts());
        double currentGoodCount = currentCount.getFirst();
        double currentBadCount = currentCount.getSecond();
        runningGoodCount += currentGoodCount;
        runningBadCount += currentBadCount;
        if ((minute + 1) % SLI_RECORD_BUCKET_SIZE == 0) {
          CompositeSLORecordBucket sloRecordBucket =
              CompositeSLORecordBucket.builder()
                  .runningBadCount(runningBadCount)
                  .runningGoodCount(runningGoodCount)
                  .sloVersion(sloVersion)
                  .verificationTaskId(verificationTaskId)
                  .bucketStartTime(instant.minus(SLI_RECORD_BUCKET_SIZE - 1, ChronoUnit.MINUTES)) // TODO date check
                  .build();
          sloRecordList.add(sloRecordBucket);
        }
      }
      minute++;
    }
    return sloRecordList;
  }

  private static GoodBadTotalWeightage getTimeStampMapsForGoodBadTotal(
      Map<CompositeServiceLevelObjective.ServiceLevelObjectivesDetail, List<SLIRecordBucket>>
          serviceLevelObjectivesDetailCompositeSLORecordMap,
      Map<CompositeServiceLevelObjective.ServiceLevelObjectivesDetail, SLIMissingDataType>
          objectivesDetailSLIMissingDataTypeMap) {
    Map<Instant, CountsWithWeightages> timeStampToBadValue = new HashMap<>(); // weight with current total
    Map<Instant, CountsWithWeightages> timeStampToGoodValue = new HashMap<>();
    Map<Instant, Integer> timeStampToTotalValue = new HashMap<>();
    for (Map.Entry<CompositeServiceLevelObjective.ServiceLevelObjectivesDetail, List<SLIRecordBucket>>
             objectivesDetailListEntry : serviceLevelObjectivesDetailCompositeSLORecordMap.entrySet()) {
      CompositeServiceLevelObjective.ServiceLevelObjectivesDetail objectivesDetail = objectivesDetailListEntry.getKey();
      for (SLIRecordBucket sliRecordBucket :
          objectivesDetailListEntry.getValue()) { // Iterate for the particular simple SLO
        for (int i = 0; i < sliRecordBucket.getSliStates().size(); i++) {
          SLIState sliState = sliRecordBucket.getSliStates().get(i);
          Instant currentTime = sliRecordBucket.getBucketStartTime().plus(i, ChronoUnit.MINUTES);
          Double weightagePercentage = objectivesDetail.getWeightagePercentage();
          CountsWithWeightages badCountPair = timeStampToBadValue.getOrDefault(currentTime,
              new CountsWithWeightages(
                  new ArrayList<>(), new ArrayList<>())); // list of weights + is it of good/bad for simple SLO
          CountsWithWeightages goodCountPair = timeStampToGoodValue.getOrDefault(
              currentTime, new CountsWithWeightages(new ArrayList<>(), new ArrayList<>()));
          timeStampToBadValue.put(currentTime, badCountPair);
          timeStampToGoodValue.put(currentTime, goodCountPair);
          timeStampToTotalValue.put(currentTime, timeStampToTotalValue.getOrDefault(currentTime, 0) + 1);
          if (SLIState.GOOD.equals(sliState)
              || (SLIState.NO_DATA.equals(sliState)
                  && objectivesDetailSLIMissingDataTypeMap.get(objectivesDetail).equals(SLIMissingDataType.GOOD))) {
            addSLIStateToList(badCountPair, weightagePercentage, 0, goodCountPair, 1);
          } else if (SLIState.BAD.equals(sliState)
              || (SLIState.NO_DATA.equals(sliState)
                  && objectivesDetailSLIMissingDataTypeMap.get(objectivesDetail).equals(SLIMissingDataType.BAD))) {
            addSLIStateToList(badCountPair, weightagePercentage, 1, goodCountPair, 0);
          } else {
            addSLIStateToList(badCountPair, weightagePercentage, -1, goodCountPair, -1);
          }
        }
      }
    }
    return GoodBadTotalWeightage.builder()
        .timeStampToGoodValue(timeStampToGoodValue)
        .timeStampToBadValue(timeStampToBadValue)
        .timeStampToTotalValueWeightage(timeStampToTotalValue)
        .build();
  }

  private static void addSLIStateToList(CountsWithWeightages badCountPair, Double weightagePercentage, int badCount,
      CountsWithWeightages goodCountPair, int goodCount) {
    badCountPair.getWeightages().add(weightagePercentage);
    badCountPair.getCounts().add(badCount);
    goodCountPair.getWeightages().add(weightagePercentage);
    goodCountPair.getCounts().add(goodCount);
  }
  private org.apache.commons.math3.util.Pair<Double, Double> getCurrentCount(
      Instant instant, GoodBadTotalWeightage goodBadTotalWeightage, CompositeSLOFormulaType sloFormulaType) {
    CountsWithWeightages badCountsWithWeightages = getCountWithWeightage(instant, goodBadTotalWeightage, SLIState.BAD);
    CountsWithWeightages goodCountsWithWeightages =
        getCountWithWeightage(instant, goodBadTotalWeightage, SLIState.GOOD);
    return formulaTypeCompositeSLOEvaluatorMap.get(sloFormulaType)
        .evaluate(goodCountsWithWeightages.getWeightages(), goodCountsWithWeightages.getCounts(),
            badCountsWithWeightages.getCounts());
  }

  private static CountsWithWeightages getCountWithWeightage(
      Instant instant, GoodBadTotalWeightage goodBadTotalWeightage, SLIState sliState) {
    if (sliState == SLIState.GOOD) {
      return goodBadTotalWeightage.getTimeStampToGoodValue().get(instant);
    } else if (sliState == SLIState.BAD) {
      return goodBadTotalWeightage.getTimeStampToBadValue().get(instant);
    } else {
      return null;
    }
  }
  @Data
  @Builder
  public static class GoodBadTotalWeightage {
    private Map<Instant, CountsWithWeightages> timeStampToBadValue; // weight with current total
    private Map<Instant, CountsWithWeightages> timeStampToGoodValue;
    private Map<Instant, Integer> timeStampToTotalValueWeightage;
  }
  @Data
  @Builder
  public static class CountsWithWeightages {
    List<Double> weightages;
    List<Integer> counts;
  }
}
