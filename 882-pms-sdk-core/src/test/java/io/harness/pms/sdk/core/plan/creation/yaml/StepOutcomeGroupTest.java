package io.harness.pms.sdk.core.plan.creation.yaml;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class StepOutcomeGroupTest extends CategoryTest {
  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testEnum() {
    assertThat(StepOutcomeGroup.values().length).isEqualTo(7);
  }
}