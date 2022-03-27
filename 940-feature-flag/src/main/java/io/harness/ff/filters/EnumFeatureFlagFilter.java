/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ff.filters;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;

import java.util.EnumMap;
import java.util.Set;

@OwnedBy(CDP)
public class EnumFeatureFlagFilter extends AbstractFeatureFlagFilter<Enum<?>> {
  private final EnumMap<FeatureName, Set<Enum<?>>> enumTypeFeatureFlagMap = new EnumMap<>(FeatureName.class);

  public void put(FeatureName featureName, Set<Enum<?>> enums) {
    Set<Enum<?>> existingEnums = enumTypeFeatureFlagMap.get(featureName);
    if (existingEnums != null) {
      existingEnums.addAll(enums);
    }
    enumTypeFeatureFlagMap.put(featureName, enums);
  }

  @Override
  public EnumMap<FeatureName, Set<Enum<?>>> getFeatureFlagMap() {
    return enumTypeFeatureFlagMap;
  }
}
