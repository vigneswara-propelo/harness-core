package io.harness.delegate;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

public class DelegateAgentCommonVariables {
  private static volatile String delegateId;

  public static void setDelegateId(String registeredDelegateId) {
    delegateId = registeredDelegateId;
  }

  public static String getDelegateId() {
    return isEmpty(delegateId) ? "Unregistered" : delegateId;
  }
}
