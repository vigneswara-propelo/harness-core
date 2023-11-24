/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.scorecard.scores.service;

import static io.harness.idp.common.Constants.DOT_SEPARATOR;
import static io.harness.idp.common.DateUtils.yesterdayInMilliseconds;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.clients.BackstageResourceClient;
import io.harness.idp.backstagebeans.BackstageCatalogEntity;
import io.harness.idp.namespace.service.NamespaceService;
import io.harness.idp.scorecard.checks.entity.CheckStatsEntity;
import io.harness.idp.scorecard.checks.entity.CheckStatusEntity;
import io.harness.idp.scorecard.checks.repositories.CheckStatsRepository;
import io.harness.idp.scorecard.checks.repositories.CheckStatusRepository;
import io.harness.idp.scorecard.scorecards.entity.ScorecardEntity;
import io.harness.idp.scorecard.scorecards.entity.ScorecardStatsEntity;
import io.harness.idp.scorecard.scorecards.repositories.ScorecardRepository;
import io.harness.idp.scorecard.scorecards.repositories.ScorecardStatsRepository;
import io.harness.idp.scorecard.scorecards.service.ScorecardService;
import io.harness.idp.scorecard.scores.entity.ScoreEntity;
import io.harness.idp.scorecard.scores.repositories.ScoreEntityByEntityIdentifier;
import io.harness.idp.scorecard.scores.repositories.ScoreRepository;
import io.harness.spec.server.idp.v1.model.CheckStatus;
import io.harness.springdata.TransactionHelper;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @com.google.inject.Inject }))
@Slf4j
@OwnedBy(HarnessTeam.IDP)
public class StatsComputeServiceImpl implements StatsComputeService {
  @Inject ScorecardService scorecardService;
  @Inject BackstageResourceClient backstageResourceClient;
  @Inject ScorecardRepository scorecardRepository;
  @Inject ScoreRepository scoreRepository;
  @Inject NamespaceService namespaceService;
  @Inject ScoreComputerService scoreComputerService;
  @Inject ScorecardStatsRepository scorecardStatsRepository;
  @Inject CheckStatsRepository checkStatsRepository;
  @Inject CheckStatusRepository checkStatusRepository;
  @Inject TransactionHelper transactionHelper;

  @Override
  public void populateStatsData() {
    List<String> accountIds = namespaceService.getAccountIds();
    accountIds.forEach(accountId -> {
      log.info("Stats Computation for account - {} started at - {}", accountId, System.currentTimeMillis());
      List<ScorecardEntity> scorecardEntities = scorecardRepository.findByAccountIdentifier(accountId);
      List<ScorecardStatsEntity> scorecardStatsEntities = new ArrayList<>();
      List<CheckStatsEntity> checkStatsEntities = new ArrayList<>();
      Map<String, CheckStatusEntity> checkStatusEntityMap = new HashMap<>();
      Set<String> checkForEntityAlreadySeen = new HashSet<>();
      for (ScorecardEntity scorecardEntity : scorecardEntities) {
        Set<BackstageCatalogEntity> backstageCatalogs =
            scoreComputerService.getAllEntities(accountId, null, List.of(scorecardEntity.getFilter()));
        Map<String, BackstageCatalogEntity> backstageCatalogMap = backstageCatalogs.stream().collect(
            Collectors.toMap(catalog -> catalog.getMetadata().getUid(), catalog -> catalog));
        String scorecardId = scorecardEntity.getIdentifier();
        log.info("{} - Backstage catalogs matching filter for scorecard - {} for account - {}",
            backstageCatalogs.size(), scorecardId, accountId);
        List<ScoreEntityByEntityIdentifier> scoreByEntityIds =
            scoreRepository.getLatestScoresForScorecard(accountId, scorecardId);
        for (ScoreEntityByEntityIdentifier scoreByEntity : scoreByEntityIds) {
          String entityIdentifier = scoreByEntity.getEntityIdentifier();
          ScoreEntity scoreEntity = scoreByEntity.getScoreEntity();
          if (!backstageCatalogMap.containsKey(entityIdentifier)) {
            log.info("Could not find entityId - {} for scorecard - {} in backstage catalogs map", entityIdentifier,
                scorecardId);
            continue;
          }
          scorecardStatsEntities.add(
              scorecardStatsRepository.findOneOrConstructStats(scoreEntity, backstageCatalogMap.get(entityIdentifier)));
          for (CheckStatus checkStatus : scoreEntity.getCheckStatus()) {
            String key =
                entityIdentifier + DOT_SEPARATOR + checkStatus.getIdentifier() + DOT_SEPARATOR + checkStatus.isCustom();
            if (checkForEntityAlreadySeen.contains(key)) {
              continue;
            }
            checkForEntityAlreadySeen.add(key);
            checkStatsEntities.add(checkStatsRepository.findOneOrConstructStats(
                checkStatus, backstageCatalogMap.get(entityIdentifier), accountId, entityIdentifier));
          }
        }
      }
      populateCheckStatus(checkStatusEntityMap, checkStatsEntities);
      log.info("Total scorecardStats entries - {} for account - {}", scorecardStatsEntities.size(), accountId);
      log.info("Total checkStats entries - {} for account - {}", checkStatsEntities.size(), accountId);
      log.info("Total checkStatus entries - {} for account - {}", checkStatusEntityMap.size(), accountId);
      transactionHelper.performTransaction(() -> {
        checkStatusRepository.saveAll(new ArrayList<>(checkStatusEntityMap.values()));
        scorecardStatsRepository.saveAll(scorecardStatsEntities);
        checkStatsRepository.saveAll(checkStatsEntities);
        return null;
      });
      log.info("Stats Computation for account - {} completed at - {}", accountId, System.currentTimeMillis());
    });
  }

  private void populateCheckStatus(
      Map<String, CheckStatusEntity> checkStatusEntityMap, List<CheckStatsEntity> checkStatsEntities) {
    for (CheckStatsEntity checkStats : checkStatsEntities) {
      String key = checkStats.getCheckIdentifier() + DOT_SEPARATOR + checkStats.isCustom();
      if (checkStatusEntityMap.containsKey(key)) {
        CheckStatusEntity checkStatusEntity = checkStatusEntityMap.get(key);
        int totalPassed = checkStatusEntity.getPassCount()
            + (CheckStatus.StatusEnum.PASS.toString().equals(checkStats.getStatus()) ? 1 : 0);
        int total = checkStatusEntity.getTotal() + 1;
        checkStatusEntity.setPassCount(totalPassed);
        checkStatusEntity.setTotal(total);
        checkStatusEntityMap.put(key, checkStatusEntity);
      } else {
        checkStatusEntityMap.put(key,
            CheckStatusEntity.builder()
                .accountIdentifier(checkStats.getAccountIdentifier())
                .identifier(checkStats.getCheckIdentifier())
                .isCustom(checkStats.isCustom())
                .passCount(CheckStatus.StatusEnum.PASS.toString().equals(checkStats.getStatus()) ? 1 : 0)
                .total(1)
                .timestamp(yesterdayInMilliseconds())
                .build());
      }
    }
  }
}
