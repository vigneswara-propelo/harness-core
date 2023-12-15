/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.entities.remediation_tracker;

import io.harness.ssca.beans.EnvType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class DeploymentsCount {
  long pendingProdCount;
  long patchedProdCount;
  long pendingNonProdCount;
  long patchedNonProdCount;

  public void update(EnvType envType, boolean isPatched) {
    if (envType == EnvType.Production) {
      if (isPatched) {
        this.setPatchedProdCount(this.getPatchedProdCount() + 1);
      } else {
        this.setPendingProdCount(this.getPendingProdCount() + 1);
      }
    } else {
      if (isPatched) {
        this.setPatchedNonProdCount(this.getPatchedNonProdCount() + 1);
      } else {
        this.setPendingNonProdCount(this.getPendingNonProdCount() + 1);
      }
    }
  }

  public void add(DeploymentsCount deploymentsCountToAdd) {
    this.setPatchedProdCount(this.getPatchedProdCount() + deploymentsCountToAdd.getPatchedProdCount());
    this.setPendingProdCount(this.getPendingProdCount() + deploymentsCountToAdd.getPendingProdCount());
    this.setPatchedNonProdCount(this.getPatchedNonProdCount() + deploymentsCountToAdd.getPatchedNonProdCount());
    this.setPendingNonProdCount(this.getPendingNonProdCount() + deploymentsCountToAdd.getPendingNonProdCount());
  }
}
