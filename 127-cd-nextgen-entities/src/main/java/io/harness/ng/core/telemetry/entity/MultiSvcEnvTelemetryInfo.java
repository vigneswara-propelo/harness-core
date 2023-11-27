/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.telemetry.entity;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true,
    components = {HarnessModuleComponent.CDS_SERVICE_ENVIRONMENT})
@OwnedBy(CDC)
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class MultiSvcEnvTelemetryInfo {
  String pipelineIdentifier;
  String stageIdentifier;
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  String deploymentType;
  Integer serviceCount;
  Integer environmentCount;
  Integer infrastructureCount;
  Boolean multiServiceDeployment;
  Boolean multiEnvironmentDeployment;
  Boolean environmentGroupPresent;
  Boolean environmentFilterPresent;
}
