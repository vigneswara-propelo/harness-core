/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.service.secret;

import static io.harness.delegate.beans.connector.azureconnector.AzureManagedIdentityType.SYSTEM_ASSIGNED_MANAGED_IDENTITY;
import static io.harness.delegate.beans.connector.azureconnector.AzureManagedIdentityType.USER_ASSIGNED_MANAGED_IDENTITY;

import io.harness.delegate.beans.connector.azureconnector.AzureManagedIdentityType;
import io.harness.delegate.core.beans.AzureVaultConfig;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AzureManagedIdentityTypeProtoPojoMapper {
  public static AzureManagedIdentityType map(AzureVaultConfig.AzureManagedIdentityType type) {
    if (type == AzureVaultConfig.AzureManagedIdentityType.USER_ASSIGNED_MANAGED_IDENTITY) {
      return USER_ASSIGNED_MANAGED_IDENTITY;
    }
    return SYSTEM_ASSIGNED_MANAGED_IDENTITY;
  }
}