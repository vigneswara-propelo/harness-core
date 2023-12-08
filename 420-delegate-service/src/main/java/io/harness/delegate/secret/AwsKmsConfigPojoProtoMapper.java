/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.secret;

import io.harness.delegate.core.beans.EncryptionConfig;

import software.wings.beans.KmsConfig;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {SecretManagerTypePojoProtoMapper.class, EncryptionTypePojoProtoMapper.class,
            AwsKmsConfigPojoProtoMapper.class},
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT,
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, unmappedSourcePolicy = ReportingPolicy.IGNORE,
    collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface AwsKmsConfigPojoProtoMapper {
  AwsKmsConfigPojoProtoMapper INSTANCE = Mappers.getMapper(AwsKmsConfigPojoProtoMapper.class);

  @Mapping(target = "secretManagerType", source = "type")
  @Mapping(target = "isGlobalKms", source = "globalKms")
  @Mapping(target = "awsKmsConfig.accessKey", source = "accessKey")
  @Mapping(target = "awsKmsConfig.secretKey", source = "secretKey")
  @Mapping(target = "awsKmsConfig.kmsArn", source = "kmsArn")
  @Mapping(target = "awsKmsConfig.region", source = "region")

  @Mapping(target = "awsKmsConfig.assumeIamRoleOnDelegate", source = "assumeIamRoleOnDelegate")

  @Mapping(target = "awsKmsConfig.assumeStsRoleOnDelegate", source = "assumeStsRoleOnDelegate")
  @Mapping(target = "awsKmsConfig.assumeStsRoleDuration", source = "assumeStsRoleDuration")
  @Mapping(target = "awsKmsConfig.roleArn", source = "roleArn")
  @Mapping(target = "awsKmsConfig.externalName", source = "externalName")
  EncryptionConfig map(KmsConfig config);
}
