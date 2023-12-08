/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.service.secret;

import io.harness.delegate.core.beans.EncryptionConfig;

import software.wings.beans.KmsConfig;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {SecretManagerTypeProtoPojoMapper.class, EncryptionTypeProtoPojoMapperUsingMapStruct.class,
            AwsKmsConfigProtoPojoMapper.class},
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT,
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, unmappedSourcePolicy = ReportingPolicy.IGNORE,
    collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface AwsKmsConfigProtoPojoMapper {
  AwsKmsConfigProtoPojoMapper INSTANCE = Mappers.getMapper(AwsKmsConfigProtoPojoMapper.class);

  @Mapping(target = "uuid", source = "config.uuid")
  @Mapping(target = "accountId", source = "config.accountId")
  @Mapping(target = "name", source = "config.name")

  @Mapping(target = "accessKey", source = "config.awsKmsConfig.accessKey")
  @Mapping(target = "secretKey", source = "config.awsKmsConfig.secretKey")
  @Mapping(target = "kmsArn", source = "config.awsKmsConfig.kmsArn")
  @Mapping(target = "region", source = "config.awsKmsConfig.region")

  @Mapping(target = "assumeIamRoleOnDelegate", source = "config.awsKmsConfig.assumeIamRoleOnDelegate")

  @Mapping(target = "assumeStsRoleOnDelegate", source = "config.awsKmsConfig.assumeStsRoleOnDelegate")
  @Mapping(target = "roleArn", source = "config.awsKmsConfig.roleArn")
  @Mapping(target = "externalName", source = "config.awsKmsConfig.externalName")

  KmsConfig map(EncryptionConfig config);
}
