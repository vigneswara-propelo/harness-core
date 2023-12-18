/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.scorecard.checks.repositories;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.backstage.entities.BackstageCatalogEntity;
import io.harness.idp.scorecard.checks.entity.CheckStatsEntity;
import io.harness.spec.server.idp.v1.model.CheckStatus;

@OwnedBy(HarnessTeam.IDP)
public interface CheckStatsRepositoryCustom {
  CheckStatsEntity findOneOrConstructStats(CheckStatus checkStatus, BackstageCatalogEntity backstageCatalog,
      String accountIdentifier, String entityIdentifier);
}
