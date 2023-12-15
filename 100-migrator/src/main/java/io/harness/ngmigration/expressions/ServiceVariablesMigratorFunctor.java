/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.expressions;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.ngmigration.utils.MigratorUtility.isEnabled;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.expression.LateBindingMap;
import io.harness.ngmigration.dto.Flag;

import java.util.HashMap;
import java.util.Map;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_MIGRATOR})
public class ServiceVariablesMigratorFunctor extends LateBindingMap {
  private Map<String, String> overrides = new HashMap<>();
  protected ServiceVariablesMigratorFunctor(Map<String, String> overrides) {
    if (isNotEmpty(overrides)) {
      this.overrides = overrides;
    }
  }

  @Override
  public synchronized Object get(Object key) {
    if (overrides.containsKey(key)) {
      return overrides.get(key);
    }
    if (isEnabled(Flag.PREFER_SERVICE_VARIABLE_OVERRIDES)) {
      return "<+serviceVariableOverrides." + key + ">";
    }
    return "<+serviceVariables." + key + ">";
  }
}
