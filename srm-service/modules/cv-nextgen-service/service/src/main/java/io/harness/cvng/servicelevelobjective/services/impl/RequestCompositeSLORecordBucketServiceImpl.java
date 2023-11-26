/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.servicelevelobjective.services.impl;

import io.harness.cvng.servicelevelobjective.entities.CompositeSLORecordBucket;
import io.harness.cvng.servicelevelobjective.entities.CompositeServiceLevelObjective;
import io.harness.cvng.servicelevelobjective.entities.SLIRecordBucket;
import io.harness.cvng.servicelevelobjective.entities.SLIState;
import io.harness.cvng.servicelevelobjective.services.api.ServiceLevelObjectiveV2Service;

import com.google.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RequestCompositeSLORecordBucketServiceImpl {
  @Inject ServiceLevelObjectiveV2Service serviceLevelObjectiveV2Service;
  List<CompositeSLORecordBucket> upsertRequestCompositeSLORecordBuckets(
      Map<CompositeServiceLevelObjective.ServiceLevelObjectivesDetail, List<SLIRecordBucket>>
          serviceLevelObjectivesDetailCompositeSLORecordMap,
      int sloVersion, String verificationTaskId, Map<Instant, CompositeSLORecordBucket> toBeUpdatedSLORecordMap) {
    List<CompositeSLORecordBucket> updateOrCreateSLORecords = new ArrayList<>();
    // A Map of  instant and simple slo id  and its bucket
    Map<Instant, Map<String, SLIRecordBucket>> timeStampToSLIRecordBucketsMap =
        getTimeStampToSLIRecordBucketMap(serviceLevelObjectivesDetailCompositeSLORecordMap);
    CompositeSLORecordBucket compositeSLORecordBucket = null;
    for (Instant instant : timeStampToSLIRecordBucketsMap.keySet()) {
      if (timeStampToSLIRecordBucketsMap.get(instant).size()
          == serviceLevelObjectivesDetailCompositeSLORecordMap.size()) {
        compositeSLORecordBucket = toBeUpdatedSLORecordMap.get(instant);
      }
      if (Objects.nonNull(compositeSLORecordBucket)) {
        compositeSLORecordBucket.setSloVersion(sloVersion);
        compositeSLORecordBucket.setScopedIdentifierSLIRecordBucketMap(timeStampToSLIRecordBucketsMap.get(instant));
      } else {
        compositeSLORecordBucket = CompositeSLORecordBucket.builder()
                                       .sloVersion(sloVersion)
                                       .verificationTaskId(verificationTaskId)
                                       .bucketStartTime(instant)
                                       .scopedIdentifierSLIRecordBucketMap(timeStampToSLIRecordBucketsMap.get(instant))
                                       .build();
      }
      updateOrCreateSLORecords.add(compositeSLORecordBucket);
    }
    return updateOrCreateSLORecords;
  }

  List<CompositeSLORecordBucket> createRequestCompositeSLORecordsFromSLIsDetails(
      Map<CompositeServiceLevelObjective.ServiceLevelObjectivesDetail, List<SLIRecordBucket>>
          serviceLevelObjectivesDetailCompositeSLORecordMap,
      CompositeServiceLevelObjective compositeServiceLevelObjective) {
    String verificationTaskId = compositeServiceLevelObjective.getUuid();
    int sloVersion = compositeServiceLevelObjective.getVersion();
    List<CompositeSLORecordBucket> sloRecordList = new ArrayList<>();
    Map<Instant, Map<String, SLIRecordBucket>> timeStampToSLIRecordBucketMap =
        getTimeStampToSLIRecordBucketMap(serviceLevelObjectivesDetailCompositeSLORecordMap);
    for (Instant instant : timeStampToSLIRecordBucketMap.keySet()) {
      if (timeStampToSLIRecordBucketMap.get(instant).size()
          == serviceLevelObjectivesDetailCompositeSLORecordMap.size()) {
        CompositeSLORecordBucket sloRecord =
            CompositeSLORecordBucket.builder()
                .sloVersion(sloVersion)
                .verificationTaskId(verificationTaskId)
                .bucketStartTime(instant)
                .scopedIdentifierSLIRecordBucketMap(timeStampToSLIRecordBucketMap.get(instant))
                .build();
        sloRecordList.add(sloRecord);
      }
    }
    return sloRecordList;
  }
  private Map<Instant, Map<String, SLIRecordBucket>> getTimeStampToSLIRecordBucketMap(
      Map<CompositeServiceLevelObjective.ServiceLevelObjectivesDetail, List<SLIRecordBucket>>
          serviceLevelObjectivesDetailCompositeSLORecordMap) {
    Map<Instant, Map<String, SLIRecordBucket>> timeStampToSLIRecordsMap = new HashMap<>();
    for (CompositeServiceLevelObjective.ServiceLevelObjectivesDetail objectivesDetail :
        serviceLevelObjectivesDetailCompositeSLORecordMap.keySet()) { // iterate for each of simple SLO
      for (SLIRecordBucket sliRecordBucket :
          serviceLevelObjectivesDetailCompositeSLORecordMap.get(objectivesDetail)) { // iterate for each of the buckets
        if (sliRecordBucket.getSliStates().stream().anyMatch(state -> state != SLIState.SKIP_DATA)) {
          Map<String, SLIRecordBucket> serviceLevelObjectivesDetailSLIRecordMap =
              timeStampToSLIRecordsMap.getOrDefault(sliRecordBucket.getBucketStartTime(), new HashMap<>());
          serviceLevelObjectivesDetailSLIRecordMap.put(
              serviceLevelObjectiveV2Service.getScopedIdentifier(objectivesDetail), sliRecordBucket);
          timeStampToSLIRecordsMap.put(sliRecordBucket.getBucketStartTime(), serviceLevelObjectivesDetailSLIRecordMap);
        }
      }
    }
    return timeStampToSLIRecordsMap;
  }
}
