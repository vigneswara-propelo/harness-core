/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.telemetry.helpers;

import static io.harness.telemetry.helpers.InstrumentationConstants.ACCOUNT;
import static io.harness.telemetry.helpers.InstrumentationConstants.DEPLOYMENT_TYPE;
import static io.harness.telemetry.helpers.InstrumentationConstants.ORG;
import static io.harness.telemetry.helpers.InstrumentationConstants.PIPELINE_ID;
import static io.harness.telemetry.helpers.InstrumentationConstants.PROJECT;
import static io.harness.telemetry.helpers.InstrumentationConstants.STAGE_IDENTIFIER;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.telemetry.entity.MultiSvcEnvTelemetryInfo;
import io.harness.telemetry.TelemetryReporter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.CDC)
public class MultiSvcEnvInstrumentationHelper extends InstrumentationHelper {
  private static final String SERVICE_COUNT = "service_count";
  private static final String ENVIRONMENT_COUNT = "environment_count";
  private static final String INFRASTRUCTURE_COUNT = "infrastructure_count";
  private static final String MULTI_SERVICE_DEPLOYMENT = "multi_service_deployment";
  private static final String MULTI_ENVIRONMENT_DEPLOYMENT = "multi_environment_deployment";
  private static final String ENVIRONMENT_GROUP_PRESENT = "environment_group_present";
  private static final String ENVIRONMENT_FILTERS_PRESENT = "environment_filters_present";
  private static final String MULTI_SVC_ENV_EVENT = "multi_svc_env";

  @Inject TelemetryReporter telemetryReporter;

  public CompletableFuture<Void> sendMultiSvcEnvEvent(MultiSvcEnvTelemetryInfo multiSvcEnvTelemetryInfo) {
    return publishMultiSvcEnvInfo(multiSvcEnvTelemetryInfo, MULTI_SVC_ENV_EVENT);
  }

  private CompletableFuture<Void> publishMultiSvcEnvInfo(
      MultiSvcEnvTelemetryInfo multiSvcEnvTelemetryInfo, String eventName) {
    HashMap<String, Object> eventPropertiesMap = new HashMap<>();
    eventPropertiesMap.put(ACCOUNT, multiSvcEnvTelemetryInfo.getAccountIdentifier());
    eventPropertiesMap.put(ORG, multiSvcEnvTelemetryInfo.getOrgIdentifier());
    eventPropertiesMap.put(PROJECT, multiSvcEnvTelemetryInfo.getProjectIdentifier());
    eventPropertiesMap.put(PIPELINE_ID, multiSvcEnvTelemetryInfo.getPipelineIdentifier());
    eventPropertiesMap.put(STAGE_IDENTIFIER, multiSvcEnvTelemetryInfo.getStageIdentifier());
    eventPropertiesMap.put(SERVICE_COUNT, multiSvcEnvTelemetryInfo.getServiceCount());
    eventPropertiesMap.put(ENVIRONMENT_COUNT, multiSvcEnvTelemetryInfo.getEnvironmentCount());
    eventPropertiesMap.put(INFRASTRUCTURE_COUNT, multiSvcEnvTelemetryInfo.getInfrastructureCount());
    eventPropertiesMap.put(DEPLOYMENT_TYPE, multiSvcEnvTelemetryInfo.getDeploymentType());
    eventPropertiesMap.put(MULTI_SERVICE_DEPLOYMENT, multiSvcEnvTelemetryInfo.getMultiEnvironmentDeployment());
    eventPropertiesMap.put(MULTI_ENVIRONMENT_DEPLOYMENT, multiSvcEnvTelemetryInfo.getMultiEnvironmentDeployment());
    eventPropertiesMap.put(ENVIRONMENT_GROUP_PRESENT, multiSvcEnvTelemetryInfo.getEnvironmentGroupPresent());
    eventPropertiesMap.put(ENVIRONMENT_FILTERS_PRESENT, multiSvcEnvTelemetryInfo.getEnvironmentFilterPresent());
    return sendEvent(eventName, multiSvcEnvTelemetryInfo.getAccountIdentifier(), eventPropertiesMap);
  }
}
