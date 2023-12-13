/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.ssca.entities.remediation_tracker;

import io.harness.ssca.beans.EnvType;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EnvironmentInfo {
  @NotNull EnvType envType;
  @NotNull String envIdentifier;
  @NotNull String envName; // ideally we shouldn't store this here and fetch this from Ng manager
  @NotNull String tag;
  boolean isPatched;

  Pipeline deploymentPipeline; // we need to store this as with new deployment old info will be lost.
}
