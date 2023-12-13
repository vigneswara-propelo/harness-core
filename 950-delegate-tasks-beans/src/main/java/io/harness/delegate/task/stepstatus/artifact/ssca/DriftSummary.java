/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.stepstatus.artifact.ssca;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.SSCA)
public class DriftSummary {
  String base;
  String baseTag;
  String driftId;
  int totalDrifts;
  int componentDrifts;
  int licenseDrifts;
  int componentsAdded;
  int componentsModified;
  int componentsDeleted;
  int licenseAdded;
  int licenseDeleted;
}
