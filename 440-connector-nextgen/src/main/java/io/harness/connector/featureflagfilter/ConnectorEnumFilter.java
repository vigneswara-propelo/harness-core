/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.featureflagfilter;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.ff.filters.EnumFeatureFlagFilter;

import com.google.common.collect.Sets;
import com.google.inject.Singleton;

@OwnedBy(CDP)
@Singleton
public class ConnectorEnumFilter extends EnumFeatureFlagFilter {
  public ConnectorEnumFilter() {
    put(FeatureName.SSH_NG, Sets.newHashSet(ConnectorType.PDC));
  }
}
