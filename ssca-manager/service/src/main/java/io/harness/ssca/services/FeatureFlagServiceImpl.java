/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.services;

import io.harness.account.AccountClient;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.network.SafeHttpCall;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.SSCA)
@Slf4j
public class FeatureFlagServiceImpl implements FeatureFlagService {
  @Inject private AccountClient accountClient;

  @Override
  public boolean isFeatureFlagEnabled(String accountId, String featureFlagName) {
    Preconditions.checkNotNull(accountId);
    Preconditions.checkNotNull(featureFlagName);
    try {
      return SafeHttpCall.executeWithExceptions(accountClient.isFeatureFlagEnabled(featureFlagName, accountId))
          .getResource();
    } catch (IOException e) {
      log.error("Exception while evaluating feature flag {} for accountId {}", featureFlagName, accountId, e);
      throw new RuntimeException(e);
    }
  }
}
