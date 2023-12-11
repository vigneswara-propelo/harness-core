/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.search.utils;

import static io.harness.rule.OwnerRule.REETIKA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.SSCAManagerTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.SSCA)
public class ElasticSearchUtilsTest extends SSCAManagerTestBase {
  String DEFAULT_INDEX_MAPPING = "ssca/search/ssca-schema.json";

  @Test
  @Owner(developers = REETIKA)
  @Category(UnitTests.class)
  public void testGetTypeMappingFromFile() {
    TypeMapping typeMapping = ElasticSearchUtils.getTypeMappingFromFile(DEFAULT_INDEX_MAPPING);
    assertThat(typeMapping).isInstanceOf(TypeMapping.class);
  }
}
