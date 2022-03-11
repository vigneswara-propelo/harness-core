package io.harness.ngmigration.expressions;

import io.harness.expression.ExpressionFunctor;

public class SecretMigratorFunctor implements ExpressionFunctor {
  public Object getValue(String secretIdentifier) {
    return "<+secrets.getValue(\"" + secretIdentifier + "\")>";
  }
}
