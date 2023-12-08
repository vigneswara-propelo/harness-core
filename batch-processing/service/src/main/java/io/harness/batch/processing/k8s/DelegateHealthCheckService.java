/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.batch.processing.k8s;

import static io.harness.batch.processing.svcmetrics.BatchProcessingMetricName.CLUSTER_HEALTH;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.batch.processing.svcmetrics.ConnectorHealthContext;
import io.harness.ccm.health.LastReceivedPublishedMessageDao;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.DelegateInstanceStatus;
import io.harness.metrics.service.api.MetricService;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.perpetualtask.internal.PerpetualTaskRecordDao;

import software.wings.service.intfc.instance.CloudToHarnessMappingService;

import com.google.common.collect.Lists;
import com.google.inject.Singleton;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@OwnedBy(HarnessTeam.CE)
@Slf4j
@Singleton
@Service
public class DelegateHealthCheckService {
  @Autowired private PerpetualTaskRecordDao perpetualTaskRecordDao;
  @Autowired private LastReceivedPublishedMessageDao lastReceivedPublishedMessageDao;
  @Autowired private CloudToHarnessMappingService cloudToHarnessMappingService;
  @Autowired private MetricService metricService;

  private static final int BATCH_SIZE = 20;
  private static final long DELAY_IN_MINUTES_FOR_LAST_RECEIVED_MSG = 90;
  private static final long MINUTES_FOR_HEALTHY_DELEGATE_HEARTBEAT = 5;
  private static final String HEALTHY_STATUS = "HEALTHY";
  private static final String UNHEALTHY_STATUS = "UNHEALTHY";

  public void run(String accountId) {
    Instant startTime = Instant.now();
    List<PerpetualTaskRecord> perpetualTasks =
        perpetualTaskRecordDao.listValidK8sWatchPerpetualTasksForAccount(accountId);
    List<String> clusterIds = new ArrayList<>();
    Map<String, String> clusterIdToDelegateIdMap = new HashMap<>();
    for (PerpetualTaskRecord perpetualTask : perpetualTasks) {
      String clusterId;
      if (perpetualTask.getTaskDescription() == null || !perpetualTask.getTaskDescription().equals("NG")) {
        clusterId = perpetualTask.getClientContext().getClientParams().get("clusterId");
      } else {
        String clientId = perpetualTask.getClientContext().getClientId();
        clusterId = clientId.substring(clientId.lastIndexOf('/') + 1);
      }
      clusterIds.add(clusterId);
      clusterIdToDelegateIdMap.put(clusterId, perpetualTask.getDelegateId());
    }
    Instant allowedTime = startTime.minus(Duration.ofMinutes(DELAY_IN_MINUTES_FOR_LAST_RECEIVED_MSG));
    for (List<String> clusterIdsBatch : Lists.partition(clusterIds, BATCH_SIZE)) {
      List<String> delegateIds =
          clusterIdsBatch.stream().map(clusterIdToDelegateIdMap::get).collect(Collectors.toList());
      List<Delegate> delegates = cloudToHarnessMappingService.obtainDelegateDetails(accountId, delegateIds);
      Set<String> healthyDelegates = delegates.stream()
                                         .filter(delegate -> isDelegateHealthy(delegate, startTime))
                                         .map(Delegate::getUuid)
                                         .collect(Collectors.toSet());
      List<String> healthyClusters =
          clusterIdsBatch.stream()
              .filter(clusterId -> healthyDelegates.contains(clusterIdToDelegateIdMap.get(clusterId)))
              .collect(Collectors.toList());
      Map<String, Long> lastReceivedTimeForClusters =
          lastReceivedPublishedMessageDao.getLastReceivedTimeForClusters(accountId, healthyClusters);
      for (String clusterId : healthyClusters) {
        String healthStatus = HEALTHY_STATUS;
        if (!lastReceivedTimeForClusters.containsKey(clusterId)
            || Instant.ofEpochMilli(lastReceivedTimeForClusters.get(clusterId)).isBefore(allowedTime)) {
          log.info("Delegate health check failed for clusterId: {}, delegateId: {}", clusterId,
              clusterIdToDelegateIdMap.get(clusterId));
          healthStatus = UNHEALTHY_STATUS;
        }
        try (ConnectorHealthContext x = new ConnectorHealthContext(accountId, clusterId, healthStatus)) {
          metricService.incCounter(CLUSTER_HEALTH);
        }
      }
    }
  }

  private boolean isDelegateHealthy(Delegate delegate, Instant now) {
    return delegate.getStatus().equals(DelegateInstanceStatus.ENABLED)
        && Instant.ofEpochMilli(delegate.getLastHeartBeat())
               .isAfter(now.minus(Duration.ofMinutes(MINUTES_FOR_HEALTHY_DELEGATE_HEARTBEAT)));
  }
}
