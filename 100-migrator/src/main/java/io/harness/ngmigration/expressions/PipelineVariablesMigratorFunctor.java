package io.harness.ngmigration.expressions;

import io.harness.expression.LateBindingMap;

public class PipelineVariablesMigratorFunctor extends LateBindingMap {
  @Override
  public synchronized Object get(Object key) {
    return "<+pipeline.variables." + key + ">";
  }
}
