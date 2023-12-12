/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.encryption;

import io.harness.beans.DecryptableEntity;
import io.harness.exception.UnexpectedException;
import io.harness.ng.core.BaseNGAccess;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class SecretRefHelper {
  public SecretRefData createSecretRef(String secretConfigString) {
    return new SecretRefData(secretConfigString);
  }

  public SecretRefData createSecretRef(String secretConfigString, Scope scope, char[] decryptedValue) {
    return new SecretRefData(secretConfigString, scope, decryptedValue);
  }

  public String getSecretConfigString(SecretRefData secretRefData) {
    if (secretRefData == null) {
      return null;
    }
    return secretRefData.toSecretRefStringValue();
  }

  public BaseNGAccess getScopeIdentifierForSecretRef(
      SecretRefData secretRefData, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    Scope secretRefScope = secretRefData.getScope();
    String orgIdBasedOnScope = secretRefScope.equals(Scope.ACCOUNT) ? null : orgIdentifier;
    String projectIdBasedOnScope = secretRefScope.equals(Scope.PROJECT) ? projectIdentifier : null;
    return BaseNGAccess.builder()
        .identifier(secretRefData.getIdentifier())
        .accountIdentifier(accountIdentifier)
        .orgIdentifier(orgIdBasedOnScope)
        .projectIdentifier(projectIdBasedOnScope)
        .build();
  }

  public static Map<String, SecretRefData> getDecryptableFieldsData(List<DecryptableEntity> decryptableEntities) {
    Map<String, SecretRefData> secrets = new HashMap<>();
    for (DecryptableEntity decryptableEntity : decryptableEntities) {
      List<Field> secretFields = decryptableEntity.getSecretReferenceFields();
      for (Field secretField : secretFields) {
        SecretRefData secretRefData = null;
        try {
          secretField.setAccessible(true);
          secretRefData = (SecretRefData) secretField.get(decryptableEntity);
        } catch (IllegalAccessException ex) {
          log.info("Error reading the secret data", ex);
          throw new UnexpectedException("Error processing the data");
        }
        secrets.put(secretField.getName(), secretRefData);
      }
    }
    return secrets;
  }
}
