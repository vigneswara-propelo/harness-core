/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pms.events.base;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.data.structure.CollectionUtils;
import io.harness.logging.AutoLogContext;
import io.harness.logging.AutoLogContext.OverrideBehavior;
import io.harness.monitoring.EventMonitoringService;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.events.PmsEventFrameworkConstants;
import io.harness.pms.events.PmsEventMonitoringConstants;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.gitsync.PmsGitSyncBranchContextGuard;
import io.harness.pms.gitsync.PmsGitSyncHelper;
import io.harness.pms.sdk.execution.events.PmsCommonsBaseEventHandler;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.protobuf.Message;
import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_PIPELINE})
@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public abstract class PmsBaseEventHandler<T extends Message> implements PmsCommonsBaseEventHandler<T> {
  public static String EVENT_PROCESS_TIME = "event_process_time";
  public static String EVENT_QUEUED_TIME = "event_queued_time";

  @Inject private PmsGitSyncHelper pmsGitSyncHelper;
  @Inject private EventMonitoringService eventMonitoringService;

  protected PmsGitSyncBranchContextGuard gitSyncContext(T event) {
    return pmsGitSyncHelper.createGitSyncBranchContextGuard(extractAmbiance(event), true);
  }

  @NonNull protected abstract Map<String, String> extraLogProperties(T event);

  protected abstract Ambiance extractAmbiance(T event);

  public Map<String, String> extractMetricContext(Map<String, String> metadataMap, T event, String stream) {
    return ImmutableMap.<String, String>builder()
        .put(PmsEventMonitoringConstants.MODULE, getModuleName(metadataMap))
        .put(PmsEventMonitoringConstants.EVENT_TYPE, getEventType(event))
        .put(PmsEventMonitoringConstants.STREAM_NAME, stream)
        .build();
  }

  protected abstract String getEventType(T message);

  public void handleEvent(T event, Map<String, String> metadataMap, Map<String, Object> metricInfo) {
    try (PmsGitSyncBranchContextGuard ignore1 = gitSyncContext(event); AutoLogContext ignore2 = autoLogContext(event);
         PmsMetricContextGuard metricContext = new PmsMetricContextGuard(extractMetricContext(
             metadataMap, event, (String) metricInfo.get(PmsEventMonitoringConstants.STREAM_NAME)))) {
      log.debug("[PMS_MESSAGE_LISTENER] Starting to process {} event ", event.getClass().getSimpleName());
      eventMonitoringService.sendMetric(EVENT_QUEUED_TIME,
          (Long) metricInfo.get(PmsEventMonitoringConstants.EVENT_RECEIVE_TS)
              - (Long) metricInfo.get(PmsEventMonitoringConstants.EVENT_SEND_TS));
      handleEventWithContext(event);
      eventMonitoringService.sendMetric(EVENT_PROCESS_TIME,
          System.currentTimeMillis() - (Long) metricInfo.get(PmsEventMonitoringConstants.EVENT_SEND_TS));
    } catch (Exception ex) {
      try (AutoLogContext autoLogContext = autoLogContext(event)) {
        log.error("Exception occurred while handling {}", event.getClass().getSimpleName(), ex);
      }
      throw ex;
    } finally {
      try (AutoLogContext autoLogContext = autoLogContext(event)) {
        log.info(
            "[PMS_MESSAGE_LISTENER] Event Handler Processing Finished for {} event", event.getClass().getSimpleName());
      }
    }
  }

  private String getModuleName(Map<String, String> metadataMap) {
    return metadataMap.get(PmsEventFrameworkConstants.SERVICE_NAME) != null
        ? metadataMap.get(PmsEventFrameworkConstants.SERVICE_NAME)
        : "pms";
  }

  protected abstract void handleEventWithContext(T event);

  protected AutoLogContext autoLogContext(T event) {
    Map<String, String> logContext = new HashMap<>();
    logContext.putAll(AmbianceUtils.logContextMap(extractAmbiance(event)));
    logContext.putAll(CollectionUtils.emptyIfNull(extraLogProperties(event)));
    return new AutoLogContext(logContext, OverrideBehavior.OVERRIDE_NESTS);
  }
}
