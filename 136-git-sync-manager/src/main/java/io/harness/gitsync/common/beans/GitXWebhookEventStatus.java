/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.beans;
import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.exception.InvalidRequestException;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_GITX})
@OwnedBy(PIPELINE)
public enum GitXWebhookEventStatus {
  QUEUED,
  PROCESSING,
  FAILED,
  SUCCESSFUL,
  SKIPPED;

  public static GitXWebhookEventStatus getGitXWebhookEventStatus(String eventStatus) {
    switch (eventStatus) {
      case "FAILED":
        return GitXWebhookEventStatus.FAILED;
      case "SKIPPED":
        return GitXWebhookEventStatus.SKIPPED;
      case "SUCCESSFUL":
        return GitXWebhookEventStatus.SUCCESSFUL;
      case "QUEUED":
        return GitXWebhookEventStatus.QUEUED;
      case "PROCESSING":
        return GitXWebhookEventStatus.PROCESSING;
      default:
        throw new InvalidRequestException(String.format("Invalid event status %s provided", eventStatus));
    }
  }
}
