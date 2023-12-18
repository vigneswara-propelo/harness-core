/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.service.secret;

import io.harness.delegate.core.beans.EncryptionConfig;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {AzureVaultConfigProtoPojoMapper.class, EncryptionTypeProtoPojoMapper.class,
            AzureManagedIdentityTypeProtoPojoMapper.class, AzureEnvironmentTypeProtoPojoMapper.class},
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, unmappedSourcePolicy = ReportingPolicy.IGNORE,
    collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface AzureVaultConfigProtoPojoMapper {
  AzureVaultConfigProtoPojoMapper INSTANCE = Mappers.getMapper(AzureVaultConfigProtoPojoMapper.class);

  @Mapping(target = "uuid", source = "uuid",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
  @Mapping(target = "accountId", source = "accountId",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
  @Mapping(target = "name", source = "name",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
  @Mapping(target = "clientId", source = "azureVaultConfig.clientId",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
  @Mapping(target = "secretKey", source = "azureVaultConfig.secretKey",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
  @Mapping(target = "tenantId", source = "azureVaultConfig.tenantId",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
  @Mapping(target = "vaultName", source = "azureVaultConfig.vaultName",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
  @Mapping(target = "subscription", source = "azureVaultConfig.subscription",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
  @Mapping(target = "useManagedIdentity", source = "azureVaultConfig.useManagedIdentity",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "managedClientId", source = "azureVaultConfig.managedClientId",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  software.wings.beans.AzureVaultConfig
  map(EncryptionConfig config);
}