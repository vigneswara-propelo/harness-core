package io.harness.accesscontrol.scopes.core;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import javax.validation.constraints.NotNull;
import javax.validation.executable.ValidateOnExecution;
import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
@ValidateOnExecution
public class ScopeHelper {
  public static Scope toParentScope(@NotNull Scope scope, String parentScopeLevel) {
    if (parentScopeLevel == null) {
      return null;
    }
    Scope currentScope = scope;
    while (currentScope != null) {
      ScopeLevel scopeLevel = currentScope.getLevel();
      if (scopeLevel.toString().equals(parentScopeLevel)) {
        return currentScope;
      }
      currentScope = currentScope.getParentScope();
    }
    return null;
  }
}
