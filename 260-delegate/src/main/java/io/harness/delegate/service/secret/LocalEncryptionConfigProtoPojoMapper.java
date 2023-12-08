/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.service.secret;

import io.harness.delegate.core.beans.EncryptionConfig;

import software.wings.beans.LocalEncryptionConfig;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {SecretManagerTypeProtoPojoMapper.class, EncryptionTypeProtoPojoMapperUsingMapStruct.class,
            LocalEncryptionConfigProtoPojoMapper.class},
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, unmappedSourcePolicy = ReportingPolicy.IGNORE,
    collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface LocalEncryptionConfigProtoPojoMapper {
  LocalEncryptionConfigProtoPojoMapper INSTANCE = Mappers.getMapper(LocalEncryptionConfigProtoPojoMapper.class);

  @Mapping(target = "uuid", source = "config.uuid",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
  @Mapping(target = "accountId", source = "config.accountId",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
  @Mapping(target = "name", source = "config.name",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)

  LocalEncryptionConfig
  map(EncryptionConfig config);
}
