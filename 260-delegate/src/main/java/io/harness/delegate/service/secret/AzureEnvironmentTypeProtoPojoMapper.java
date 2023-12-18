/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.service.secret;

import static io.harness.azure.AzureEnvironmentType.AZURE;
import static io.harness.azure.AzureEnvironmentType.AZURE_US_GOVERNMENT;

import io.harness.azure.AzureEnvironmentType;
import io.harness.delegate.core.beans.AzureVaultConfig;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AzureEnvironmentTypeProtoPojoMapper {
  public static AzureEnvironmentType map(AzureVaultConfig.AzureEnvironmentType type) {
    if (type == AzureVaultConfig.AzureEnvironmentType.AZURE_US_GOVERNMENT) {
      return AZURE_US_GOVERNMENT;
    }
    return AZURE;
  }
}