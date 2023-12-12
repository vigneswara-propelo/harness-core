/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.k8s.model.istio;

import static io.harness.rule.OwnerRule.BUHA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class MatchTest extends CategoryTest {
  private static final String VALUE = "value";
  private static final String NAME = "name";

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testCreateUriMatch() {
    Match match = Match.createMatch("URI", NAME, VALUE, "EXACT", null);
    assertThat(match).isEqualTo(
        URIMatch.builder().name(NAME).ignoreUriCase(true).uri(MatchDetails.builder().exact(VALUE).build()).build());
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testCreateSchemeMatch() {
    Match match = Match.createMatch("SCHEME", NAME, VALUE, "PREFIX", null);
    assertThat(match).isEqualTo(
        SchemeMatch.builder().name(NAME).scheme(MatchDetails.builder().prefix(VALUE).build()).build());
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testCreateMethodMatch() {
    Match match = Match.createMatch("METHOD", NAME, "GET", "REGEX", null);
    assertThat(match).isEqualTo(
        MethodMatch.builder().name(NAME).method(MatchDetails.builder().regex("GET").build()).build());
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testCreateAuthorityMatch() {
    Match match = Match.createMatch("AUTHORITY", NAME, VALUE, "EXACT", null);
    assertThat(match).isEqualTo(
        AuthorityMatch.builder().name(NAME).authority(MatchDetails.builder().exact(VALUE).build()).build());
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testCreateHeaderMatch() {
    Match match = Match.createMatch("HEADER", NAME, VALUE, "EXACT",
        Map.of("key1", Pair.of("value1", "EXACT"), "key2", Pair.of("value2", "PREFIX")));
    assertThat(match).isEqualTo(HeaderMatch.builder()
                                    .name(NAME)
                                    .header(Map.of("key1", MatchDetails.builder().exact("value1").build(), "key2",
                                        MatchDetails.builder().prefix("value2").build()))
                                    .build());
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testCreatePortMatch() {
    Match match = Match.createMatch("PORT", NAME, "9090", null, null);
    assertThat(match).isEqualTo(PortMatch.builder().name(NAME).port(9090).build());
  }

  @Test(expected = IllegalArgumentException.class)
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testCreateMatchInvalidArg() {
    Match.createMatch("SOME_RULE", null, null, null, null);
  }
}
