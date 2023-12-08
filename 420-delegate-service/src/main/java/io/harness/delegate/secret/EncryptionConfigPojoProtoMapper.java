/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.secret;

import io.harness.delegate.core.beans.EncryptionConfig;

import software.wings.beans.GcpKmsConfig;
import software.wings.beans.KmsConfig;

import org.mapstruct.BeanMapping;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.SubclassMapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {SecretManagerTypePojoProtoMapper.class, EncryptionTypePojoProtoMapper.class,
            GcpKmsConfigPojoProtoMapper.class, AwsKmsConfigPojoProtoMapper.class},
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, unmappedSourcePolicy = ReportingPolicy.ERROR,
    collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface EncryptionConfigPojoProtoMapper {
  EncryptionConfigPojoProtoMapper INSTANCE = Mappers.getMapper(EncryptionConfigPojoProtoMapper.class);

  @Mapping(target = "secretManagerType", source = "type")
  @Mapping(target = "uuid", source = "uuid",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
  @Mapping(target = "accountId", source = "accountId",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
  @Mapping(target = "name", source = "name",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
  @Mapping(target = "encryptionServiceUrl", source = "encryptionServiceUrl",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
  @Mapping(target = "isGlobalKms", source = "globalKms",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
  @BeanMapping(ignoreUnmappedSourceProperties = {"numOfEncryptedValue", "default", "validationCriteria"})
  @SubclassMapping(target = EncryptionConfig.class, source = GcpKmsConfig.class)
  @SubclassMapping(target = EncryptionConfig.class, source = KmsConfig.class)
  EncryptionConfig
  map(io.harness.security.encryption.EncryptionConfig config);
}
