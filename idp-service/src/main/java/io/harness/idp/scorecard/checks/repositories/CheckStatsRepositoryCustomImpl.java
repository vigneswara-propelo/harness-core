/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.scorecard.checks.repositories;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.backstagebeans.BackstageCatalogEntity;
import io.harness.idp.backstagebeans.BackstageCatalogEntityTypes;
import io.harness.idp.scorecard.checks.entity.CheckStatsEntity;
import io.harness.idp.scorecard.checks.entity.CheckStatsEntity.CheckStatsKeys;
import io.harness.idp.scorecard.scorecards.beans.StatsMetadata;
import io.harness.spec.server.idp.v1.model.CheckStatus;

import com.google.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@OwnedBy(HarnessTeam.IDP)
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class CheckStatsRepositoryCustomImpl implements CheckStatsRepositoryCustom {
  private MongoTemplate mongoTemplate;
  @Override
  public CheckStatsEntity findOneOrConstructStats(CheckStatus checkStatus, BackstageCatalogEntity backstageCatalog,
      String accountIdentifier, String entityIdentifier) {
    Criteria criteria = Criteria.where(CheckStatsKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(CheckStatsKeys.entityIdentifier)
                            .is(entityIdentifier)
                            .and(CheckStatsKeys.checkIdentifier)
                            .is(checkStatus.getIdentifier())
                            .and(CheckStatsKeys.isCustom)
                            .is(checkStatus.isCustom());

    CheckStatsEntity entity = mongoTemplate.findOne(Query.query(criteria), CheckStatsEntity.class);
    if (entity == null) {
      return CheckStatsEntity.builder()
          .accountIdentifier(accountIdentifier)
          .entityIdentifier(entityIdentifier)
          .checkIdentifier(checkStatus.getIdentifier())
          .isCustom(checkStatus.isCustom())
          .status(String.valueOf(checkStatus.getStatus()))
          .metadata(buildMetadata(backstageCatalog))
          .build();
    }
    entity.setStatus(String.valueOf(checkStatus.getStatus()));
    entity.setMetadata(buildMetadata(backstageCatalog));
    return entity;
  }

  private StatsMetadata buildMetadata(BackstageCatalogEntity backstageCatalog) {
    return StatsMetadata.builder()
        .kind(backstageCatalog.getKind())
        .namespace(backstageCatalog.getMetadata().getNamespace())
        .name(backstageCatalog.getMetadata().getName())
        .type(BackstageCatalogEntityTypes.getEntityType(backstageCatalog))
        .owner(BackstageCatalogEntityTypes.getEntityOwner(backstageCatalog))
        .system(BackstageCatalogEntityTypes.getEntitySystem(backstageCatalog))
        .build();
  }
}
