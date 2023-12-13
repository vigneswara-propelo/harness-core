/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngtriggers.instrumentation;

import static io.harness.telemetry.helpers.InstrumentationConstants.ACCOUNT;
import static io.harness.telemetry.helpers.InstrumentationConstants.COUNT;
import static io.harness.telemetry.helpers.InstrumentationConstants.ORG;
import static io.harness.telemetry.helpers.InstrumentationConstants.PIPELINE_ID;
import static io.harness.telemetry.helpers.InstrumentationConstants.PROJECT;
import static io.harness.telemetry.helpers.InstrumentationConstants.TIME_TAKEN;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.dto.BulkTriggersRequestDTO;
import io.harness.ngtriggers.beans.dto.BulkTriggersResponseDTO;
import io.harness.telemetry.helpers.InstrumentationHelper;

import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class TriggerTelemetryHelper extends InstrumentationHelper {
  public static final String TRIGGER_TYPE = "trigger_type";
  public static final String TRIGGER_TOGGLE = "trigger_toggle";

  public CompletableFuture<Void> sendBulkToggleTriggersApiEvent(String accountId,
      BulkTriggersRequestDTO bulkTriggersRequestDTO, BulkTriggersResponseDTO bulkTriggersResponseDTO, long timeTaken) {
    String orgId = null;
    String projectId = null;
    String pipelineId = null;
    String type = null;
    boolean enable = false;
    long modifiedCount = 0;

    // Filters and Data from the RequestBody
    if (bulkTriggersRequestDTO != null && bulkTriggersRequestDTO.getFilters() != null) {
      orgId = bulkTriggersRequestDTO.getFilters().getOrgIdentifier();
      projectId = bulkTriggersRequestDTO.getFilters().getProjectIdentifier();
      pipelineId = bulkTriggersRequestDTO.getFilters().getPipelineIdentifier();
      type = bulkTriggersRequestDTO.getFilters().getType();
    }

    if (bulkTriggersRequestDTO.getData() != null) {
      enable = bulkTriggersRequestDTO.getData().isEnable();
    }

    if (bulkTriggersResponseDTO != null) {
      modifiedCount = bulkTriggersResponseDTO.getCount();
    }

    return publishBulkToggleTriggersApiInfo(
        "bulk_toggle_triggers_api", accountId, orgId, projectId, pipelineId, type, enable, timeTaken, modifiedCount);
  }

  private CompletableFuture<Void> publishBulkToggleTriggersApiInfo(String eventName, String accountId, String orgId,
      String projectId, String pipelineId, String type, boolean enable, long timeTaken, long modifiedCount) {
    HashMap<String, Object> eventPropertiesMap = new HashMap<>();
    eventPropertiesMap.put(ACCOUNT, accountId);
    eventPropertiesMap.put(ORG, orgId);
    eventPropertiesMap.put(PROJECT, projectId);
    eventPropertiesMap.put(PIPELINE_ID, pipelineId);
    eventPropertiesMap.put(TRIGGER_TYPE, type);
    eventPropertiesMap.put(TRIGGER_TOGGLE, enable);
    eventPropertiesMap.put(TIME_TAKEN, timeTaken);
    eventPropertiesMap.put(COUNT, modifiedCount);

    return sendEvent(eventName, accountId, eventPropertiesMap);
  }
}
