/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.telemetry.helpers;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.telemetry.helpers.InstrumentationConstants.PIPELINE_ID;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;

import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.CDP)
public class DeploymentsInstrumentationHelper extends InstrumentationHelper {
  public static final String ORG_ID = "org_id";
  public static final String PROJECT_ID = "project_id";
  public static final String STEP_EXECUTION_EVENT_NAME_PREFIX = "step_execution_";
  public static final String STAGE_EXECUTION_ID = "stage_execution_id";
  public static final String PIPELINE_EXECUTION_ID = "pipeline_execution_id";
  public static final String MANIFEST_TYPES = "manifest_types";
  public static final String K8S_SKIP_DRY_RUN = "skip_dry_run";

  public CompletableFuture<Void> publishStepEvent(Ambiance ambiance, StepExecutionTelemetryEventDTO telemetryEventDTO) {
    if (isNotEmpty(telemetryEventDTO.getStepType())) {
      String eventName = STEP_EXECUTION_EVENT_NAME_PREFIX + telemetryEventDTO.getStepType();
      try {
        HashMap<String, Object> eventPropertiesMap =
            telemetryEventDTO.getProperties() != null ? telemetryEventDTO.getProperties() : new HashMap<>();
        eventPropertiesMap.put(ORG_ID, AmbianceUtils.getOrgIdentifier(ambiance));
        eventPropertiesMap.put(PROJECT_ID, AmbianceUtils.getProjectIdentifier(ambiance));
        eventPropertiesMap.put(STAGE_EXECUTION_ID, ambiance.getStageExecutionId());
        eventPropertiesMap.put(PIPELINE_EXECUTION_ID, ambiance.getPlanExecutionId());
        eventPropertiesMap.put(PIPELINE_ID, AmbianceUtils.getPipelineIdentifier(ambiance));
        return sendEvent(eventName, AmbianceUtils.getAccountId(ambiance), eventPropertiesMap);
      } catch (Exception e) {
        log.error(eventName + " event failed for accountID = " + AmbianceUtils.getAccountId(ambiance), e);
      }
    }
    return null;
  }
}