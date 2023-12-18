/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.secret;

import io.harness.delegate.beans.connector.azureconnector.AzureManagedIdentityType;
import io.harness.delegate.core.beans.AzureVaultConfig;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.ValueMapping;
import org.mapstruct.ValueMappings;
import org.mapstruct.factory.Mappers;

@Mapper(collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, unmappedSourcePolicy = ReportingPolicy.ERROR,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AzureManagedIdentityTypePojoProtoMapper {
  AzureManagedIdentityTypePojoProtoMapper INSTANCE = Mappers.getMapper(AzureManagedIdentityTypePojoProtoMapper.class);

  @ValueMappings({
    @ValueMapping(source = MappingConstants.NULL, target = "SYSTEM_ASSIGNED_MANAGED_IDENTITY")
    , @ValueMapping(source = "SYSTEM_ASSIGNED_MANAGED_IDENTITY", target = "SYSTEM_ASSIGNED_MANAGED_IDENTITY"),
        @ValueMapping(source = "USER_ASSIGNED_MANAGED_IDENTITY", target = "USER_ASSIGNED_MANAGED_IDENTITY"),
        @ValueMapping(source = MappingConstants.ANY_REMAINING, target = "SYSTEM_ASSIGNED_MANAGED_IDENTITY")
  })
  AzureVaultConfig.AzureManagedIdentityType
  map(AzureManagedIdentityType type);
}