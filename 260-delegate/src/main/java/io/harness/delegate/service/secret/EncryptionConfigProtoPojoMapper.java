/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.service.secret;

import io.harness.delegate.core.beans.EncryptionConfig;
import io.harness.delegate.core.beans.EncryptionType;
import io.harness.exception.InvalidRequestException;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, unmappedSourcePolicy = ReportingPolicy.ERROR,
    collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface EncryptionConfigProtoPojoMapper {
  EncryptionConfigProtoPojoMapper INSTANCE = Mappers.getMapper(EncryptionConfigProtoPojoMapper.class);

  default io.harness.security.encryption.EncryptionConfig map(EncryptionConfig config) {
    if (config.getEncryptionType().equals(EncryptionType.GCP_KMS)) {
      return GcpKmsConfigProtoPojoMapper.INSTANCE.map(config);
    } else if (config.getEncryptionType().equals(EncryptionType.AWS_KMS)) {
      return AwsKmsConfigProtoPojoMapper.INSTANCE.map(config);
    } else if (config.getEncryptionType().equals(EncryptionType.LOCAL)) {
      return LocalEncryptionConfigProtoPojoMapper.INSTANCE.map(config);
    }
    throw new InvalidRequestException("Secret Manager not implemented!");
  }
}
