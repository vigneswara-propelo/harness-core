/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.beans.drift;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;

@OwnedBy(HarnessTeam.SSCA)
public enum LicenseDriftStatus {
  @JsonProperty("added") ADDED("added"),
  @JsonProperty("deleted") DELETED("deleted");

  private String status;

  LicenseDriftStatus(String status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return this.status;
  }
}
