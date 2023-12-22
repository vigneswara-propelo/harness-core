/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.license.enforcement;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.enforcement.beans.metadata.LicenseStaticLimitRestrictionMetadataDTO;
import io.harness.enforcement.client.usage.RestrictionUsageInterface;
import io.harness.idp.license.usage.service.IDPModuleLicenseUsage;

import com.google.inject.Inject;

@OwnedBy(HarnessTeam.IDP)
public class ActiveDevelopersRestrictionUsageImpl
    implements RestrictionUsageInterface<LicenseStaticLimitRestrictionMetadataDTO> {
  @Inject IDPModuleLicenseUsage idpModuleLicenseUsage;

  @Override
  public long getCurrentValue(
      String accountIdentifier, LicenseStaticLimitRestrictionMetadataDTO restrictionMetadataDTO) {
    return idpModuleLicenseUsage.getActiveDevelopers(accountIdentifier);
  }
}
