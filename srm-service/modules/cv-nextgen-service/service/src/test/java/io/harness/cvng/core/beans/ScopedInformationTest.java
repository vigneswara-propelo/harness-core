/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.beans;

import static io.harness.rule.OwnerRule.VARSHA_LALWANI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.core.beans.params.ResourceParams;
import io.harness.cvng.utils.ScopedInformation;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ScopedInformationTest extends CvNextGenTestBase {
  @Test
  @Owner(developers = VARSHA_LALWANI)
  @Category(UnitTests.class)
  public void testGetResourceParamsFromScopedIdentifier() {
    ResourceParams resourceParams = ScopedInformation.getResourceParamsFromScopedIdentifier(
        "PROJECT.26TRmCAhRRKVB3UUD_q11g.jago.Partnership.partner_gateway_account.prod");
    assertThat(resourceParams.getAccountIdentifier()).isEqualTo("26TRmCAhRRKVB3UUD_q11g");
    assertThat(resourceParams.getOrgIdentifier()).isEqualTo("jago");
    assertThat(resourceParams.getProjectIdentifier()).isEqualTo("Partnership");
    assertThat(resourceParams.getIdentifier()).isEqualTo("partner_gateway_account.prod");

    resourceParams = ScopedInformation.getResourceParamsFromScopedIdentifier(
        "ORG.26TRmCAhRRKVB3UUD_q11g.jago.partner_gateway_account.prod");
    assertThat(resourceParams.getAccountIdentifier()).isEqualTo("26TRmCAhRRKVB3UUD_q11g");
    assertThat(resourceParams.getOrgIdentifier()).isEqualTo("jago");
    assertThat(resourceParams.getIdentifier()).isEqualTo("partner_gateway_account.prod");

    resourceParams = ScopedInformation.getResourceParamsFromScopedIdentifier(
        "ACCOUNT.26TRmCAhRRKVB3UUD_q11g.partner_gateway_account.prod");
    assertThat(resourceParams.getAccountIdentifier()).isEqualTo("26TRmCAhRRKVB3UUD_q11g");
    assertThat(resourceParams.getIdentifier()).isEqualTo("partner_gateway_account.prod");
  }
}
