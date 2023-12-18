/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.search;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ssca.beans.ElasticSearchConfig;

import com.google.inject.Inject;
import com.google.inject.name.Named;

@OwnedBy(HarnessTeam.SSCA)
public class SSCAIndexManager extends ElasticSearchIndexManagerImpl {
  @Inject @Named("elasticsearch") ElasticSearchConfig elasticSearchConfig;
  @Override
  protected String getIndexName(String accountId) {
    return elasticSearchConfig.getIndexName();
  }

  @Override
  protected String getIndexMapping() {
    return "ssca/search/ssca-schema.json";
  }
}
