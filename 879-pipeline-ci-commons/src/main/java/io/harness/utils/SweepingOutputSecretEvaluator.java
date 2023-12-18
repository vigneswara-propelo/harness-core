/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.utils;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.ExpressionEvaluator;
import io.harness.expression.ExpressionEvaluatorUtils;
import io.harness.expression.ExpressionResolveFunctor;
import io.harness.security.SimpleEncryption;

import software.wings.expression.SecretManagerMode;
import software.wings.expression.SweepingOutputSecretFunctor;

import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Builder
@Slf4j
@OwnedBy(HarnessTeam.CI)
public class SweepingOutputSecretEvaluator {
  public void resolve(Object o) {
    SweepingOutputSecretFunctor sweepingOutputSecretFunctor = SweepingOutputSecretFunctor.builder()
                                                                  .mode(SecretManagerMode.APPLY)
                                                                  .simpleEncryption(new SimpleEncryption())
                                                                  .build();

    SweepingOutputSecretEvaluator.ResolveFunctorImpl resolveFunctor =
        new SweepingOutputSecretEvaluator.ResolveFunctorImpl(new ExpressionEvaluator(), sweepingOutputSecretFunctor);

    ExpressionEvaluatorUtils.updateExpressions(o, resolveFunctor);
  }

  public SweepingOutputSecretEvaluator() {}

  public class ResolveFunctorImpl implements ExpressionResolveFunctor {
    private final ExpressionEvaluator expressionEvaluator;
    final Map<String, Object> evaluatorResponseContext = new HashMap<>(1);

    public ResolveFunctorImpl(
        ExpressionEvaluator expressionEvaluator, SweepingOutputSecretFunctor sweepingOutputSecretFunctor) {
      this.expressionEvaluator = expressionEvaluator;
      evaluatorResponseContext.put("sweepingOutputSecrets", sweepingOutputSecretFunctor);
    }

    @Override
    public String processString(String expression) {
      return expressionEvaluator.substitute(expression, evaluatorResponseContext);
    }
  }
}
