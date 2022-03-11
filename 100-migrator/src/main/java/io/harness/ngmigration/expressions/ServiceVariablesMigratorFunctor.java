package io.harness.ngmigration.expressions;

import io.harness.expression.LateBindingMap;

public class ServiceVariablesMigratorFunctor extends LateBindingMap {
  @Override
  public synchronized Object get(Object key) {
    return "<+serviceConfig.serviceDefinition.spec.variables." + key + ">";
  }
}
