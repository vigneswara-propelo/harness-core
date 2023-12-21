/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.k8s.model.smi;

import static java.lang.String.format;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@CodePulse(module = ProductModule.CDS, unitCoverageRequired = false, components = {HarnessModuleComponent.CDS_K8S})
public abstract class Match {
  String name;

  public static Match createMatch(String ruleType, String name, String value, Map<String, String> headerConfig) {
    switch (ruleType) {
      case "URI": {
        return URIMatch.builder().pathRegex(value).name(name).build();
      }
      case "METHOD": {
        return MethodMatch.builder().name(name).methods(List.of(value)).build();
      }
      case "HEADER": {
        return HeaderMatch.builder().name(name).headers(headerConfig).build();
      }
      default: {
        throw new IllegalArgumentException((format("Unsupported Rule type for SMI HTTP provider %s", ruleType)));
      }
    }
  }
}
