/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.k8s.model.smi;

import static io.harness.rule.OwnerRule.BUHA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class MatchTest extends CategoryTest {
  private static final String VALUE = "value";
  private static final String NAME = "name";
  private static final Map<String, String> HEADER_CONFIG = Map.of("key1", "value1", "key2", "value2");

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testCreateUriMatch() {
    Match match = Match.createMatch("URI", NAME, VALUE, null);
    assertThat(match).isEqualTo(URIMatch.builder().name(NAME).pathRegex(VALUE).build());
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testCreateMethodMatch() {
    Match match = Match.createMatch("METHOD", NAME, "GET", null);
    assertThat(match).isEqualTo(MethodMatch.builder().name(NAME).methods(List.of("GET")).build());
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testCreateHeaderMatch() {
    Match match = Match.createMatch("HEADER", NAME, null, HEADER_CONFIG);
    assertThat(match).isEqualTo(HeaderMatch.builder().name(NAME).headers(HEADER_CONFIG).build());
  }

  @Test(expected = IllegalArgumentException.class)
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testCreateMatchInvalidArg() {
    Match.createMatch("SOME_RULE", null, null, null);
  }
}
