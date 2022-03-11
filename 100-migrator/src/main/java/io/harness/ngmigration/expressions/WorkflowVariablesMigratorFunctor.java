package io.harness.ngmigration.expressions;

import io.harness.expression.LateBindingMap;

public class WorkflowVariablesMigratorFunctor extends LateBindingMap {
  @Override
  public synchronized Object get(Object key) {
    return "<+stage.variables." + key + ">";
  }
}
