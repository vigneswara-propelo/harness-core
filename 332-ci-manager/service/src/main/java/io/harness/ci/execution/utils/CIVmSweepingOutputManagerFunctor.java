/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ci.execution.utils;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.functors.ExpressionFunctor;
import io.harness.security.SimpleEncryption;

import software.wings.expression.SecretManagerMode;
import software.wings.expression.SweepingOutputSecretFunctor;

import java.util.HashSet;
import java.util.Set;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Builder
@Slf4j
@OwnedBy(HarnessTeam.CI)
public class CIVmSweepingOutputManagerFunctor implements ExpressionFunctor {
  @Builder.Default private Set<String> secrets = new HashSet<>();

  public Set<String> getSecrets() {
    return secrets;
  }

  public Object obtain(String secretKey, String secretValue) {
    SweepingOutputSecretFunctor sweepingOutputSecretFunctor = SweepingOutputSecretFunctor.builder()
                                                                  .mode(SecretManagerMode.APPLY)
                                                                  .simpleEncryption(new SimpleEncryption())
                                                                  .build();
    String decryptedSecret = (String) sweepingOutputSecretFunctor.obtain(secretKey, secretValue);
    secrets.add(decryptedSecret);
    return decryptedSecret;
  }
}
