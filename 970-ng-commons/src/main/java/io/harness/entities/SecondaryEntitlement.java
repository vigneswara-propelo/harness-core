/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */
package io.harness;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.GTM)
public enum SecondaryEntitlement {
  NUMBER_OF_AGENTS("numberOfAgents"), // CET

  NUMBER_OF_COMMITTERS("numberOfCommitters"), // CI

  NUMBER_OF_CONTRIBUTORS("numberOfContributors"), // SEI

  NUMBER_OF_CLIENT_MAUS("numberOfClientMAUs"), // FF

  NUMBER_OF_DEVELOPERS("numberOfDevelopers"), // IDP

  NUMBER_OF_EXECUTION_APPLIES("numberOfExecutionApplies"), // IACM

  NUMBER_OF_SECURITY_ASSESSMENT("numberOfSecurityAssessment"), // SSCA

  NUMBER_OF_SECURITY_SCANS("numberOfSecurityScans"), // STO
  NUMBER_OF_SERVICES("numberOfServices"), // CD service type license, SRM, CHAOS

  NUMBER_OF_USERS("numberOfUsers"); // FF

  String displayName;

  SecondaryEntitlement(String displayName) {
    this.displayName = displayName;
  }
}