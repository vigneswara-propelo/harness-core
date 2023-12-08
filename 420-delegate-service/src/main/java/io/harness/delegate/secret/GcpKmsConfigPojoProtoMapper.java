/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.secret;

import io.harness.delegate.core.beans.EncryptionConfig;

import software.wings.beans.GcpKmsConfig;

import com.google.protobuf.ByteString;
import java.nio.charset.StandardCharsets;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {SecretManagerTypePojoProtoMapper.class, EncryptionTypePojoProtoMapper.class,
            GcpKmsConfigPojoProtoMapper.class},
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, unmappedSourcePolicy = ReportingPolicy.IGNORE,
    collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface GcpKmsConfigPojoProtoMapper {
  GcpKmsConfigPojoProtoMapper INSTANCE = Mappers.getMapper(GcpKmsConfigPojoProtoMapper.class);

  @Mapping(target = "secretManagerType", source = "type")
  @Mapping(target = "isGlobalKms", source = "globalKms",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)

  // Below fields cannot be null because they are must have for GCP KMS hence not specifying any
  // nullValuePropertyMappingStrategy for them.
  @Mapping(target = "gcpKmsConfig.projectId", source = "projectId")
  @Mapping(target = "gcpKmsConfig.region", source = "region")
  @Mapping(target = "gcpKmsConfig.keyRing", source = "keyRing")
  @Mapping(target = "gcpKmsConfig.keyName", source = "keyName")
  @Mapping(target = "gcpKmsConfig.gcpServiceAccountCredentials", source = "config")
  EncryptionConfig
  map(GcpKmsConfig config);

  default ByteString credentialsToBytes(GcpKmsConfig config) {
    return ByteString.copyFrom(String.valueOf(config.getCredentials()).getBytes(StandardCharsets.UTF_8));
  }
}
