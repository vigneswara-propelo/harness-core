/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.gitxwebhooks.helper;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.FeatureName;
import io.harness.beans.Scope;
import io.harness.gitsync.gitxwebhooks.entity.GitXWebhook;
import io.harness.gitsync.gitxwebhooks.service.GitXWebhookService;
import io.harness.gitsync.gitxwebhooks.utils.GitXWebhookUtils;
import io.harness.gitsync.interceptor.GitSyncConstants;
import io.harness.ngsettings.client.remote.NGSettingsClient;
import io.harness.remote.client.NGRestUtils;
import io.harness.utils.NGFeatureFlagHelperService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true,
    components = {HarnessModuleComponent.CDS_GITX, HarnessModuleComponent.CDS_PIPELINE})
@Slf4j
@OwnedBy(PIPELINE)
@Singleton
public class GitXWebhookHelper {
  @Inject private NGSettingsClient ngSettingsClient;
  @Inject private GitXWebhookService gitXWebhookService;
  @Inject private NGFeatureFlagHelperService ngFeatureFlagHelperService;

  public boolean isBiDirectionalSyncApplicable(Scope scope, String repoName, String filePath) {
    if (ngFeatureFlagHelperService.isEnabled(scope.getAccountIdentifier(), FeatureName.PIE_GIT_BI_DIRECTIONAL_SYNC)
        && isBiDirectionalSyncEnabledInSettings(scope.getAccountIdentifier())) {
      List<GitXWebhook> gitXWebhookList = gitXWebhookService.getGitXWebhookForAllScopes(scope, repoName);
      if (isNotEmpty(gitXWebhookList)) {
        return GitXWebhookUtils.isBiDirectionalSyncEnabled(scope, gitXWebhookList, filePath);
      }
    }
    return false;
  }

  public boolean isBiDirectionalSyncEnabledInSettings(String accountId) {
    try {
      String isBiDirectionalSyncEnabledString =
          NGRestUtils
              .getResponse(
                  ngSettingsClient.getSetting(GitSyncConstants.ENABLE_BI_DIRECTIONAL_SYNC, accountId, null, null))
              .getValue();
      return GitSyncConstants.TRUE_VALUE.equals(isBiDirectionalSyncEnabledString);
    } catch (Exception exception) {
      log.error(String.format("Faced exception while fetching account setting for Bi-Directional sync %s", exception));
      return false;
    }
  }
}
