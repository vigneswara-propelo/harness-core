package io.harness.batch.processing.service.impl;

import static io.harness.batch.processing.tasklet.util.InstanceMetaDataUtils.getValueForKeyFromInstanceMetaData;
import static io.harness.ccm.commons.constants.Constants.ZONE_OFFSET;
import static io.harness.ccm.commons.utils.TimeUtils.offsetDateTimeNow;
import static io.harness.ccm.commons.utils.TimeUtils.toOffsetDateTime;
import static io.harness.ccm.commons.utils.TimescaleUtils.isAliveAtInstant;
import static io.harness.timescaledb.Tables.NODE_INFO;
import static io.harness.timescaledb.Tables.POD_INFO;
import static io.harness.timescaledb.Tables.WORKLOAD_INFO;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

import io.harness.annotations.retry.RetryOnException;
import io.harness.batch.processing.ccm.InstanceEvent;
import io.harness.batch.processing.ccm.InstanceInfo;
import io.harness.batch.processing.service.intfc.InstanceInfoTimescaleDAO;
import io.harness.ccm.commons.beans.JobConstants;
import io.harness.ccm.commons.beans.Resource;
import io.harness.ccm.commons.constants.InstanceMetaDataConstants;
import io.harness.event.payloads.Lifecycle;
import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.k8s.watch.K8sWorkloadSpec;
import io.harness.timescaledb.Keys;
import io.harness.timescaledb.tables.records.NodeInfoRecord;
import io.harness.timescaledb.tables.records.PodInfoRecord;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.InsertSetStep;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.UpdateSetMoreStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class InstanceInfoTimescaleDAOImpl implements InstanceInfoTimescaleDAO {
  private static final int RETRY_COUNT = 3;
  private static final int SLEEP_DURATION = 100;
  private final DSLContext dslContext;

  @Autowired
  public InstanceInfoTimescaleDAOImpl(DSLContext dslContext) {
    this.dslContext = dslContext;
  }

  @Override
  @RetryOnException(retryCount = RETRY_COUNT, sleepDurationInMilliseconds = SLEEP_DURATION)
  public void insertIntoNodeInfo(@NotNull InstanceInfo instanceInfo) {
    final String nodePoolName =
        getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.NODE_POOL_NAME, instanceInfo.getMetaData());

    dslContext.insertInto(NODE_INFO)
        .set(NODE_INFO.ACCOUNTID, instanceInfo.getAccountId())
        .set(NODE_INFO.CLUSTERID, instanceInfo.getClusterId())
        .set(NODE_INFO.INSTANCEID, instanceInfo.getInstanceId())
        .set(NODE_INFO.STARTTIME, instantToOffsetDateTime(instanceInfo.getUsageStartTime()))
        .set(NODE_INFO.NODEPOOLNAME, nodePoolName)
        .onConflictOnConstraint(Keys.NODE_INFO_UNIQUE_RECORD_INDEX)
        .doUpdate()
        .set(NODE_INFO.STARTTIME, instantToOffsetDateTime(instanceInfo.getUsageStartTime()))
        .set(NODE_INFO.UPDATEDAT, offsetDateTimeNow())
        .set(NODE_INFO.NODEPOOLNAME, nodePoolName)
        .execute();
  }

  @Override
  public void insertIntoNodeInfo(@NotNull List<InstanceInfo> instanceInfoList) {
    instanceInfoList.forEach(this::insertIntoNodeInfo);
  }

  @Override
  @RetryOnException(retryCount = RETRY_COUNT, sleepDurationInMilliseconds = SLEEP_DURATION)
  public void insertIntoWorkloadInfo(@NotNull String accountId, @NotNull K8sWorkloadSpec workloadSpec) {
    int replicas = Integer.parseInt(String.valueOf(workloadSpec.getReplicas()));

    dslContext.insertInto(WORKLOAD_INFO)
        .set(WORKLOAD_INFO.ACCOUNTID, accountId)
        .set(WORKLOAD_INFO.CLUSTERID, workloadSpec.getClusterId())
        .set(WORKLOAD_INFO.WORKLOADID, workloadSpec.getUid())
        .set(WORKLOAD_INFO.REPLICAS, replicas)
        .set(WORKLOAD_INFO.TYPE, workloadSpec.getWorkloadKind())
        .set(WORKLOAD_INFO.NAME, workloadSpec.getWorkloadName())
        .set(WORKLOAD_INFO.NAMESPACE, workloadSpec.getNamespace())
        .onConflictOnConstraint(Keys.WORKLOAD_INFO_UNIQUE_RECORD_INDEX)
        .doUpdate()
        .set(WORKLOAD_INFO.UPDATEDAT, offsetDateTimeNow())
        .set(WORKLOAD_INFO.REPLICAS, replicas)
        .set(WORKLOAD_INFO.TYPE, workloadSpec.getWorkloadKind())
        .set(WORKLOAD_INFO.NAME, workloadSpec.getWorkloadName())
        .set(WORKLOAD_INFO.NAMESPACE, workloadSpec.getNamespace())
        .execute();
  }

  @Override
  public void insertIntoPodInfo(@NotNull List<InstanceInfo> instanceInfoList) {
    instanceInfoList.forEach(this::insertIntoPodInfo);
  }

  @Override
  @RetryOnException(retryCount = RETRY_COUNT, sleepDurationInMilliseconds = SLEEP_DURATION)
  public void insertIntoPodInfo(@NotNull InstanceInfo instanceInfo) {
    final String workloadId =
        getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.WORKLOAD_ID, instanceInfo.getMetaData());
    final String namespace =
        getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.NAMESPACE, instanceInfo.getMetaData());
    final String parentId = getValueForKeyFromInstanceMetaData(
        InstanceMetaDataConstants.ACTUAL_PARENT_RESOURCE_ID, instanceInfo.getMetaData());
    final Resource resource = firstNonNull(instanceInfo.getResource(), Resource.builder().build());

    dslContext.insertInto(POD_INFO)
        .set(POD_INFO.ACCOUNTID, instanceInfo.getAccountId())
        .set(POD_INFO.CLUSTERID, instanceInfo.getClusterId())
        .set(POD_INFO.INSTANCEID, instanceInfo.getInstanceId())
        .set(POD_INFO.STARTTIME, instantToOffsetDateTime(instanceInfo.getUsageStartTime()))
        .set(POD_INFO.NAMESPACE, namespace)
        .set(POD_INFO.NAME, instanceInfo.getInstanceName())
        .set(POD_INFO.WORKLOADID, workloadId)
        .set(POD_INFO.CPUREQUEST, resource.getCpuUnits())
        .set(POD_INFO.MEMORYREQUEST, resource.getMemoryMb())
        .set(POD_INFO.PARENTNODEID, parentId)
        .onConflictOnConstraint(Keys.POD_INFO_UNIQUE_RECORD_INDEX)
        .doUpdate()
        .set(POD_INFO.STARTTIME, instantToOffsetDateTime(instanceInfo.getUsageStartTime()))
        .set(POD_INFO.NAMESPACE, namespace)
        .set(POD_INFO.NAME, instanceInfo.getInstanceName())
        .set(POD_INFO.WORKLOADID, workloadId)
        .set(POD_INFO.CPUREQUEST, resource.getCpuUnits())
        .set(POD_INFO.MEMORYREQUEST, resource.getMemoryMb())
        .set(POD_INFO.PARENTNODEID, parentId)
        .set(POD_INFO.UPDATEDAT, offsetDateTimeNow())
        .execute();
  }

  @Override
  @RetryOnException(retryCount = RETRY_COUNT, sleepDurationInMilliseconds = SLEEP_DURATION)
  public void updatePodStopEvent(@NotNull List<InstanceEvent> instanceEventList) {
    for (InstanceEvent instanceEvent : instanceEventList) {
      dslContext.update(POD_INFO)
          .set(POD_INFO.STOPTIME, instantToOffsetDateTime(instanceEvent.getTimestamp()))
          .set(POD_INFO.UPDATEDAT, offsetDateTimeNow())
          .where(POD_INFO.ACCOUNTID.eq(instanceEvent.getAccountId()),
              POD_INFO.CLUSTERID.eq(instanceEvent.getClusterId()),
              POD_INFO.INSTANCEID.eq(instanceEvent.getInstanceId()))
          .execute();
    }
  }

  @Override
  @RetryOnException(retryCount = RETRY_COUNT, sleepDurationInMilliseconds = SLEEP_DURATION)
  public void updatePodLifecycleEvent(@NotNull String accountId, @NotNull List<Lifecycle> lifecycleList) {
    for (Lifecycle lifecycle : lifecycleList) {
      UpdateSetMoreStep<PodInfoRecord> updateSetMoreStep =
          dslContext.update(POD_INFO).set(POD_INFO.UPDATEDAT, offsetDateTimeNow());

      if (Lifecycle.EventType.EVENT_TYPE_START.equals(lifecycle.getType())) {
        updateSetMoreStep =
            updateSetMoreStep
                .set(POD_INFO.STARTTIME, HTimestamps.toInstant(lifecycle.getTimestamp()).atOffset(ZONE_OFFSET))
                .setNull(POD_INFO.STOPTIME);
      } else {
        updateSetMoreStep = updateSetMoreStep.set(
            POD_INFO.STOPTIME, HTimestamps.toInstant(lifecycle.getTimestamp()).atOffset(ZONE_OFFSET));
      }

      updateSetMoreStep
          .where(POD_INFO.ACCOUNTID.eq(accountId), POD_INFO.CLUSTERID.eq(lifecycle.getClusterId()),
              POD_INFO.INSTANCEID.eq(lifecycle.getInstanceId()))
          .execute();
    }
  }

  @Override
  @RetryOnException(retryCount = RETRY_COUNT, sleepDurationInMilliseconds = SLEEP_DURATION)
  public void updateNodeStopEvent(@NotNull List<InstanceEvent> instanceEventList) {
    for (InstanceEvent instanceEvent : instanceEventList) {
      dslContext.update(NODE_INFO)
          .set(NODE_INFO.STOPTIME, instantToOffsetDateTime(instanceEvent.getTimestamp()))
          .set(NODE_INFO.UPDATEDAT, offsetDateTimeNow())
          .where(NODE_INFO.ACCOUNTID.eq(instanceEvent.getAccountId()),
              NODE_INFO.CLUSTERID.eq(instanceEvent.getClusterId()),
              NODE_INFO.INSTANCEID.eq(instanceEvent.getInstanceId()))
          .execute();
    }
  }

  @Override
  @RetryOnException(retryCount = RETRY_COUNT, sleepDurationInMilliseconds = SLEEP_DURATION)
  public void updateNodeLifecycleEvent(@NotNull String accountId, @NotNull List<Lifecycle> lifecycleList) {
    for (Lifecycle lifecycle : lifecycleList) {
      UpdateSetMoreStep<NodeInfoRecord> updateSetMoreStep =
          dslContext.update(NODE_INFO).set(NODE_INFO.UPDATEDAT, offsetDateTimeNow());

      if (Lifecycle.EventType.EVENT_TYPE_START.equals(lifecycle.getType())) {
        updateSetMoreStep =
            updateSetMoreStep
                .set(NODE_INFO.STARTTIME, HTimestamps.toInstant(lifecycle.getTimestamp()).atOffset(ZONE_OFFSET))
                .setNull(NODE_INFO.STOPTIME);
      } else {
        updateSetMoreStep = updateSetMoreStep.set(
            NODE_INFO.STOPTIME, HTimestamps.toInstant(lifecycle.getTimestamp()).atOffset(ZONE_OFFSET));
      }
      updateSetMoreStep.set(NODE_INFO.UPDATEDAT, offsetDateTimeNow())
          .where(NODE_INFO.ACCOUNTID.eq(accountId), NODE_INFO.CLUSTERID.eq(lifecycle.getClusterId()),
              NODE_INFO.INSTANCEID.eq(lifecycle.getInstanceId()))
          .execute();
    }
  }

  @Override
  @RetryOnException(retryCount = RETRY_COUNT, sleepDurationInMilliseconds = SLEEP_DURATION)
  public void stopInactiveNodesAtTime(@NotNull JobConstants jobConstants, @NotNull String clusterId,
      @NotNull Instant syncEventTimestamp, @NotNull List<String> activeNodeUidsList) {
    dslContext.update(NODE_INFO)
        .set(NODE_INFO.STOPTIME, toOffsetDateTime(syncEventTimestamp))
        .set(NODE_INFO.UPDATEDAT, offsetDateTimeNow())
        .where(NODE_INFO.ACCOUNTID.eq(jobConstants.getAccountId()), NODE_INFO.CLUSTERID.eq(clusterId),
            NODE_INFO.INSTANCEID.notIn(activeNodeUidsList),
            isAliveAtInstant(NODE_INFO.STARTTIME, NODE_INFO.STOPTIME, syncEventTimestamp))
        .execute();
  }

  @Override
  @RetryOnException(retryCount = RETRY_COUNT, sleepDurationInMilliseconds = SLEEP_DURATION)
  public void stopInactivePodsAtTime(@NotNull JobConstants jobConstants, @NotNull String clusterId,
      @NotNull Instant syncEventTimestamp, @NotNull List<String> activePodUidsList) {
    dslContext.update(POD_INFO)
        .set(POD_INFO.STOPTIME, toOffsetDateTime(syncEventTimestamp))
        .set(POD_INFO.UPDATEDAT, offsetDateTimeNow())
        .where(POD_INFO.ACCOUNTID.eq(jobConstants.getAccountId()), POD_INFO.CLUSTERID.eq(clusterId),
            POD_INFO.INSTANCEID.notIn(activePodUidsList),
            isAliveAtInstant(POD_INFO.STARTTIME, POD_INFO.STOPTIME, syncEventTimestamp))
        .execute();
  }

  private static <R extends Record> int bulkInsert(
      @NotNull final Table<R> table, @NotNull final List<R> records, @NotNull final DSLContext dslContext) {
    if (records.isEmpty()) {
      return 0;
    }

    InsertSetStep<R> insertStep = dslContext.insertInto(table);
    for (int i = 0; i < records.size() - 1; i++) {
      insertStep = insertStep.set(records.get(i)).newRecord();
    }
    final R lastRecord = records.get(records.size() - 1);

    return insertStep.set(lastRecord).onConflictDoNothing().execute();
  }

  private static OffsetDateTime instantToOffsetDateTime(@Nullable Instant instant) {
    return ofNullable(instant).map(x -> x.atOffset(ZONE_OFFSET)).orElse(null);
  }
}
