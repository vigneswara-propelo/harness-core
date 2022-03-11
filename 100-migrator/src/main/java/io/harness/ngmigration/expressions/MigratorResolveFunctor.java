package io.harness.ngmigration.expressions;

import io.harness.expression.ExpressionEvaluator;
import io.harness.expression.ExpressionResolveFunctor;

import com.google.inject.Inject;
import java.util.Map;

public class MigratorResolveFunctor implements ExpressionResolveFunctor {
  private final Map<String, Object> context;

  @Inject private ExpressionEvaluator expressionEvaluator;

  public MigratorResolveFunctor(Map<String, Object> context) {
    this.context = context;
  }

  @Override
  public String processString(String expression) {
    return expressionEvaluator.substitute(expression, context);
  }
}
