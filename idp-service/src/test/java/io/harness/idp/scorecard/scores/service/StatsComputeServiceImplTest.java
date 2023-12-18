/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.scorecard.scores.service;

import static io.harness.rule.OwnerRule.VIGNESWARA;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.idp.backstage.entities.BackstageCatalogComponentEntity;
import io.harness.idp.backstage.entities.BackstageCatalogEntity;
import io.harness.idp.namespace.service.NamespaceService;
import io.harness.idp.scorecard.checks.entity.CheckStatsEntity;
import io.harness.idp.scorecard.checks.repositories.CheckStatsRepository;
import io.harness.idp.scorecard.scorecards.entity.ScorecardEntity;
import io.harness.idp.scorecard.scorecards.entity.ScorecardStatsEntity;
import io.harness.idp.scorecard.scorecards.repositories.ScorecardRepository;
import io.harness.idp.scorecard.scorecards.repositories.ScorecardStatsRepository;
import io.harness.idp.scorecard.scores.entity.ScoreEntity;
import io.harness.idp.scorecard.scores.repositories.ScoreEntityByEntityIdentifier;
import io.harness.idp.scorecard.scores.repositories.ScoreRepository;
import io.harness.rule.Owner;
import io.harness.spec.server.idp.v1.model.CheckStatus;
import io.harness.spec.server.idp.v1.model.ScorecardFilter;
import io.harness.springdata.TransactionHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.IDP)
public class StatsComputeServiceImplTest extends CategoryTest {
  @Mock NamespaceService namespaceService;
  @Mock ScorecardRepository scorecardRepository;
  @Mock ScoreComputerService scoreComputerService;
  @Mock ScoreRepository scoreRepository;
  @Mock ScorecardStatsRepository scorecardStatsRepository;
  @Mock CheckStatsRepository checkStatsRepository;
  @InjectMocks StatsComputeServiceImpl statsComputeServiceImpl;
  @Mock TransactionHelper transactionHelper;

  private static final String ACCOUNT_ID = "123";
  private static final String SERVICE_MATURITY_SCORECARD_ID = "service_maturity";
  private static final String SERVICE_MATURITY_SCORECARD_NAME = "Service Maturity";
  private static final String DEVOPS_MATURITY_SCORECARD_ID = "devops_maturity";
  private static final String DEVOPS_MATURITY_SCORECARD_NAME = "Devops Maturity";
  private static final String GITHUB_CHECK_ID = "github_checks";
  private static final String MTTR_CHECK_ID = "mttr_checks";
  private static final String TECH_DOCS_CHECK_ID = "tech_docs_check";
  private static final String DEPENDABOT_ALERT_CHECK_ID = "dependabot_alert_check";
  private static final String WORKFLOW_CHECK_ID = "workflow_check";
  private static final String COMPONENT = "component";
  private static final String KIND = "service";
  private static final String DEFAULT = "default";
  private static final String IDP_SERVICE_ENTITY_ID = "03bc314a-437b-4d15-b75b-b819179e7859";
  private static final String IDP_SERVICE_ENTITY_NAME = "idp-service";
  private static final String PMS_SERVICE_ENTITY_ID = "34dqefes-437b-4d15-8343-35253452324r";
  private static final String PMS_SERVICE_ENTITY_NAME = "idp-service";
  private static final String PASS = "PASS";
  private static final String FAIL = "FAIL";

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  @Owner(developers = VIGNESWARA)
  @Category(UnitTests.class)
  public void testPopulateStatsData() {
    when(namespaceService.getAccountIds()).thenReturn(List.of(ACCOUNT_ID));
    when(scorecardRepository.findByAccountIdentifier(ACCOUNT_ID)).thenReturn(getScorecardEntities());
    when(scoreComputerService.getAllEntities(any(), any(), any())).thenReturn(getBackstageCatalogEntities());
    List<ScoreEntityByEntityIdentifier> scoreEntityByEntityIdentifiers = getScoreEntitiesByEntityIdentifier();
    when(scoreRepository.getLatestScoresForScorecard(ACCOUNT_ID, SERVICE_MATURITY_SCORECARD_ID))
        .thenReturn(List.of(scoreEntityByEntityIdentifiers.get(0), scoreEntityByEntityIdentifiers.get(2)));
    when(scoreRepository.getLatestScoresForScorecard(ACCOUNT_ID, DEVOPS_MATURITY_SCORECARD_ID))
        .thenReturn(List.of(scoreEntityByEntityIdentifiers.get(1), scoreEntityByEntityIdentifiers.get(3)));
    when(scorecardStatsRepository.findOneOrConstructStats(any(), any()))
        .thenReturn(ScorecardStatsEntity.builder().build());
    when(transactionHelper.performTransaction(any())).thenReturn(null);
    mockCheckStatsSaveOrUpdate();
    statsComputeServiceImpl.populateStatsData();
    verify(scorecardStatsRepository, times(4)).findOneOrConstructStats(any(), any());
    verify(checkStatsRepository, times(10)).findOneOrConstructStats(any(), any(), any(), any());
  }

  private List<ScorecardEntity> getScorecardEntities() {
    List<ScorecardEntity> scorecardEntities = new ArrayList<>();
    ScorecardEntity.Check git = ScorecardEntity.Check.builder().identifier(GITHUB_CHECK_ID).isCustom(true).build();
    ScorecardEntity.Check mttr = ScorecardEntity.Check.builder().identifier(MTTR_CHECK_ID).isCustom(false).build();
    ScorecardEntity.Check techDocs =
        ScorecardEntity.Check.builder().identifier(TECH_DOCS_CHECK_ID).isCustom(true).build();
    ScorecardEntity.Check alert =
        ScorecardEntity.Check.builder().identifier(DEPENDABOT_ALERT_CHECK_ID).isCustom(false).build();
    ScorecardEntity.Check workflow =
        ScorecardEntity.Check.builder().identifier(WORKFLOW_CHECK_ID).isCustom(true).build();
    scorecardEntities.add(ScorecardEntity.builder()
                              .accountIdentifier(ACCOUNT_ID)
                              .identifier(SERVICE_MATURITY_SCORECARD_ID)
                              .name(SERVICE_MATURITY_SCORECARD_NAME)
                              .checks(List.of(git, mttr, techDocs, alert))
                              .filter(new ScorecardFilter().kind(COMPONENT).type(KIND))
                              .published(true)
                              .build());
    scorecardEntities.add(ScorecardEntity.builder()
                              .accountIdentifier(ACCOUNT_ID)
                              .identifier(DEVOPS_MATURITY_SCORECARD_ID)
                              .name(DEVOPS_MATURITY_SCORECARD_NAME)
                              .checks(List.of(git, mttr, techDocs, workflow))
                              .filter(new ScorecardFilter().kind(COMPONENT).type(KIND))
                              .published(true)
                              .build());
    return scorecardEntities;
  }

  private Set<BackstageCatalogEntity> getBackstageCatalogEntities() {
    BackstageCatalogComponentEntity entity1 = new BackstageCatalogComponentEntity();
    BackstageCatalogEntity.Metadata metadata1 = new BackstageCatalogEntity.Metadata();
    metadata1.setUid(IDP_SERVICE_ENTITY_ID);
    metadata1.setName(IDP_SERVICE_ENTITY_NAME);
    metadata1.setNamespace(DEFAULT);
    entity1.setMetadata(metadata1);

    BackstageCatalogComponentEntity.Spec spec = new BackstageCatalogComponentEntity.Spec();
    spec.setType(KIND);
    spec.setLifecycle("experimental");
    spec.setOwner("team-a");
    spec.setSystem("Unknown");
    entity1.setSpec(spec);

    BackstageCatalogComponentEntity entity2 = new BackstageCatalogComponentEntity();
    BackstageCatalogEntity.Metadata metadata2 = new BackstageCatalogEntity.Metadata();
    metadata2.setUid(PMS_SERVICE_ENTITY_ID);
    metadata2.setName(PMS_SERVICE_ENTITY_NAME);
    metadata2.setNamespace(DEFAULT);
    entity2.setMetadata(metadata2);
    entity2.setSpec(spec);
    return Set.of(entity1, entity2);
  }

  private List<ScoreEntityByEntityIdentifier> getScoreEntitiesByEntityIdentifier() {
    CheckStatus gitPassed = new CheckStatus();
    gitPassed.setIdentifier(GITHUB_CHECK_ID);
    gitPassed.setCustom(true);
    gitPassed.setStatus(CheckStatus.StatusEnum.PASS);

    CheckStatus mttrPassed = new CheckStatus();
    mttrPassed.setIdentifier(MTTR_CHECK_ID);
    mttrPassed.setCustom(false);
    mttrPassed.setStatus(CheckStatus.StatusEnum.PASS);

    CheckStatus mttrFailed = new CheckStatus();
    mttrFailed.setIdentifier(MTTR_CHECK_ID);
    mttrFailed.setCustom(false);
    mttrFailed.setStatus(CheckStatus.StatusEnum.FAIL);

    CheckStatus techDocsPassed = new CheckStatus();
    techDocsPassed.setIdentifier(TECH_DOCS_CHECK_ID);
    techDocsPassed.setCustom(true);
    techDocsPassed.setStatus(CheckStatus.StatusEnum.PASS);

    CheckStatus techDocsFailed = new CheckStatus();
    techDocsFailed.setIdentifier(TECH_DOCS_CHECK_ID);
    techDocsFailed.setCustom(true);
    techDocsFailed.setStatus(CheckStatus.StatusEnum.FAIL);

    CheckStatus alertFailed = new CheckStatus();
    alertFailed.setIdentifier(DEPENDABOT_ALERT_CHECK_ID);
    alertFailed.setCustom(false);
    alertFailed.setStatus(CheckStatus.StatusEnum.FAIL);

    CheckStatus workflowPassed = new CheckStatus();
    workflowPassed.setIdentifier(WORKFLOW_CHECK_ID);
    workflowPassed.setCustom(true);
    workflowPassed.setStatus(CheckStatus.StatusEnum.PASS);

    CheckStatus workflowFailed = new CheckStatus();
    workflowFailed.setIdentifier(WORKFLOW_CHECK_ID);
    workflowFailed.setCustom(true);
    workflowFailed.setStatus(CheckStatus.StatusEnum.FAIL);

    List<ScoreEntityByEntityIdentifier> scoreEntityByEntityIdentifiers = new ArrayList<>();
    scoreEntityByEntityIdentifiers.add(
        ScoreEntityByEntityIdentifier.builder()
            .scoreEntity(ScoreEntity.builder()
                             .scorecardIdentifier(SERVICE_MATURITY_SCORECARD_ID)
                             .entityIdentifier(IDP_SERVICE_ENTITY_ID)
                             .accountIdentifier(ACCOUNT_ID)
                             .checkStatus(List.of(gitPassed, mttrPassed, techDocsFailed, alertFailed))
                             .score(50)
                             .build())
            .entityIdentifier(IDP_SERVICE_ENTITY_ID)
            .build());
    scoreEntityByEntityIdentifiers.add(
        ScoreEntityByEntityIdentifier.builder()
            .scoreEntity(ScoreEntity.builder()
                             .scorecardIdentifier(DEVOPS_MATURITY_SCORECARD_ID)
                             .entityIdentifier(IDP_SERVICE_ENTITY_ID)
                             .accountIdentifier(ACCOUNT_ID)
                             .checkStatus(List.of(gitPassed, mttrPassed, techDocsFailed, workflowPassed))
                             .score(75)
                             .build())
            .entityIdentifier(IDP_SERVICE_ENTITY_ID)
            .build());

    scoreEntityByEntityIdentifiers.add(
        ScoreEntityByEntityIdentifier.builder()
            .scoreEntity(ScoreEntity.builder()
                             .scorecardIdentifier(SERVICE_MATURITY_SCORECARD_ID)
                             .entityIdentifier(PMS_SERVICE_ENTITY_ID)
                             .accountIdentifier(ACCOUNT_ID)
                             .checkStatus(List.of(gitPassed, mttrFailed, techDocsPassed, alertFailed))
                             .score(50)
                             .build())
            .entityIdentifier(PMS_SERVICE_ENTITY_ID)
            .build());
    scoreEntityByEntityIdentifiers.add(
        ScoreEntityByEntityIdentifier.builder()
            .scoreEntity(ScoreEntity.builder()
                             .scorecardIdentifier(DEVOPS_MATURITY_SCORECARD_ID)
                             .entityIdentifier(PMS_SERVICE_ENTITY_ID)
                             .accountIdentifier(ACCOUNT_ID)
                             .checkStatus(List.of(gitPassed, mttrFailed, techDocsPassed, workflowFailed))
                             .score(50)
                             .build())
            .entityIdentifier(PMS_SERVICE_ENTITY_ID)
            .build());
    return scoreEntityByEntityIdentifiers;
  }

  private void mockCheckStatsSaveOrUpdate() {
    when(checkStatsRepository.findOneOrConstructStats(any(), any(), anyString(), anyString()))
        .thenReturn(CheckStatsEntity.builder()
                        .entityIdentifier(IDP_SERVICE_ENTITY_ID)
                        .checkIdentifier(GITHUB_CHECK_ID)
                        .isCustom(true)
                        .status(PASS)
                        .build())
        .thenReturn(CheckStatsEntity.builder()
                        .entityIdentifier(IDP_SERVICE_ENTITY_ID)
                        .checkIdentifier(MTTR_CHECK_ID)
                        .isCustom(false)
                        .status(PASS)
                        .build())
        .thenReturn(CheckStatsEntity.builder()
                        .entityIdentifier(IDP_SERVICE_ENTITY_ID)
                        .checkIdentifier(TECH_DOCS_CHECK_ID)
                        .isCustom(true)
                        .status(FAIL)
                        .build())
        .thenReturn(CheckStatsEntity.builder()
                        .entityIdentifier(IDP_SERVICE_ENTITY_ID)
                        .checkIdentifier(DEPENDABOT_ALERT_CHECK_ID)
                        .isCustom(false)
                        .status(FAIL)
                        .build())
        .thenReturn(CheckStatsEntity.builder()
                        .entityIdentifier(PMS_SERVICE_ENTITY_ID)
                        .checkIdentifier(GITHUB_CHECK_ID)
                        .isCustom(true)
                        .status(PASS)
                        .build())
        .thenReturn(CheckStatsEntity.builder()
                        .entityIdentifier(PMS_SERVICE_ENTITY_ID)
                        .checkIdentifier(MTTR_CHECK_ID)
                        .isCustom(false)
                        .status(FAIL)
                        .build())
        .thenReturn(CheckStatsEntity.builder()
                        .entityIdentifier(PMS_SERVICE_ENTITY_ID)
                        .checkIdentifier(TECH_DOCS_CHECK_ID)
                        .isCustom(true)
                        .status(PASS)
                        .build())
        .thenReturn(CheckStatsEntity.builder()
                        .entityIdentifier(PMS_SERVICE_ENTITY_ID)
                        .checkIdentifier(DEPENDABOT_ALERT_CHECK_ID)
                        .isCustom(false)
                        .status(FAIL)
                        .build())
        .thenReturn(CheckStatsEntity.builder()
                        .entityIdentifier(IDP_SERVICE_ENTITY_ID)
                        .checkIdentifier(WORKFLOW_CHECK_ID)
                        .isCustom(true)
                        .status(PASS)
                        .build())
        .thenReturn(CheckStatsEntity.builder()
                        .entityIdentifier(PMS_SERVICE_ENTITY_ID)
                        .checkIdentifier(WORKFLOW_CHECK_ID)
                        .isCustom(true)
                        .status(FAIL)
                        .build());
  }
}
