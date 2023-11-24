/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.beans.monitoredService;

import io.harness.cvng.core.entities.MonitoredService;
import io.harness.cvng.core.utils.template.MonitoredServiceValidator;

public enum ReconciliationStatus {
  // Enum for Reconciliation Status
  NO_RECONCILIATION_REQUIRED,
  INPUT_REQUIRED_FOR_RECONCILIATION,
  NO_INPUT_REQUIRED_FOR_RECONCILIATION;

  public static ReconciliationStatus determineStatus(
      MonitoredService monitoredService, Long versionNumber, String templateInput) {
    if (monitoredService.getTemplateMetadata().getTemplateVersionNumber() == versionNumber) {
      return NO_RECONCILIATION_REQUIRED;
    } else if (MonitoredServiceValidator.validateTemplateInputs(
                   monitoredService.getTemplateMetadata().getTemplateInputs(), templateInput)) {
      return NO_INPUT_REQUIRED_FOR_RECONCILIATION;
    } else {
      return INPUT_REQUIRED_FOR_RECONCILIATION;
    }
  }
}
