/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.usage.utils;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.entities.InstanceType;
import io.harness.exception.InvalidArgumentsException;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDP)
public class LicenseUsageUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = OwnerRule.MANISH)
  @Category(UnitTests.class)
  public void testComputeLicenseConsumedNonLambda() {
    long licenseCount = LicenseUsageUtils.computeLicenseConsumed(10, InstanceType.K8S_INSTANCE);
    assertThat(licenseCount).isEqualTo(1);
  }

  @Test
  @Owner(developers = OwnerRule.MANISH)
  @Category(UnitTests.class)
  public void testComputeLicenseConsumedLambda() {
    long licenseCount = LicenseUsageUtils.computeLicenseConsumed(10, InstanceType.AWS_LAMBDA_INSTANCE);
    assertThat(licenseCount).isEqualTo(2);
  }

  @Test
  @Owner(developers = OwnerRule.MANISH)
  @Category(UnitTests.class)
  public void testComputeLicenseConsumedThrowsOnInvalidArguments() {
    assertThatThrownBy(() -> LicenseUsageUtils.computeLicenseConsumed(10, null))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessage("instanceType cannot be null for non-zero serviceInstanceCount");
  }
}
