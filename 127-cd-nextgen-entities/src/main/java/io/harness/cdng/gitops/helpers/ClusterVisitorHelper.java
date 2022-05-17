package io.harness.cdng.gitops.helpers;

import io.harness.cdng.gitops.yaml.ClusterYaml;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class ClusterVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    return ClusterYaml.builder().build();
  }
}
