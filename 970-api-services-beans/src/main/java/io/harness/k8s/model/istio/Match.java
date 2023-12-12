/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.k8s.model.istio;

import static java.lang.String.format;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.tuple.Pair;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@CodePulse(module = ProductModule.CDS, unitCoverageRequired = false, components = {HarnessModuleComponent.CDS_K8S})
public abstract class Match {
  String name;

  public static Match createMatch(
      String ruleType, String name, String value, String matchType, Map<String, Pair<String, String>> headerConfig) {
    switch (ruleType) {
      case "URI": {
        return URIMatch.builder().name(name).ignoreUriCase(true).uri(getMatchDetails(value, matchType)).build();
      }
      case "SCHEME": {
        return SchemeMatch.builder().name(name).scheme(getMatchDetails(value, matchType)).build();
      }
      case "METHOD": {
        return MethodMatch.builder().name(name).method(getMatchDetails(value, matchType)).build();
      }
      case "AUTHORITY": {
        return AuthorityMatch.builder().name(name).authority(getMatchDetails(value, matchType)).build();
      }
      case "HEADER": {
        Map<String, MatchDetails> headerMatchDetails = new HashMap<>();
        headerConfig.forEach(
            (key, value1) -> headerMatchDetails.put(key, getMatchDetails(value1.getLeft(), value1.getRight())));
        return HeaderMatch.builder().name(name).header(headerMatchDetails).build();
      }
      case "PORT": {
        return PortMatch.builder().name(name).port(Integer.parseInt(value)).build();
      }
      default: {
        throw new IllegalArgumentException(format("Invalid Rule type for traffic routing: %s", ruleType));
      }
    }
  }

  private static MatchDetails getMatchDetails(String value, String matchType) {
    switch (matchType) {
      case "EXACT": {
        return MatchDetails.builder().exact(value).build();
      }
      case "PREFIX": {
        return MatchDetails.builder().prefix(value).build();
      }
      case "REGEX": {
        return MatchDetails.builder().regex(value).build();
      }
      default:
        throw new IllegalArgumentException(format("Invalid Match type for traffic routing rule: %s", matchType));
    }
  }
}
