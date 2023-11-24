/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.services;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.repositories.ScorecardRepo;
import io.harness.spec.server.ssca.v1.model.SbomDetailsForScorecard;
import io.harness.spec.server.ssca.v1.model.SbomScorecardRequestBody;
import io.harness.spec.server.ssca.v1.model.SbomScorecardResponseBody;
import io.harness.spec.server.ssca.v1.model.Score;
import io.harness.spec.server.ssca.v1.model.ScorecardInfo;
import io.harness.ssca.entities.ScorecardEntity;

import com.google.inject.Inject;
import java.util.stream.Collectors;

public class ScorecardServiceImpl implements ScorecardService {
  @Inject ScorecardRepo scorecardRepo;

  @Override
  public boolean save(SbomScorecardRequestBody body) {
    if (validateRequest(body)) {
      ScorecardEntity scorecardEntity = scorecardRequestToEntity(body);
      scorecardRepo.save(scorecardEntity);
      return true;
    }
    return false;
  }

  @Override
  public SbomScorecardResponseBody getByOrchestrationId(
      String accountId, String orgId, String projectId, String orchestrateId) {
    ScorecardEntity scorecardEntity = scorecardRepo.getByOrchestrationId(accountId, orgId, projectId, orchestrateId);
    SbomScorecardResponseBody sbomScorecardResponseBody = null;
    if (scorecardEntity != null) {
      sbomScorecardResponseBody = scorecardEntityToResponse(scorecardEntity);
    }

    return sbomScorecardResponseBody;
  }

  private boolean validateRequest(SbomScorecardRequestBody body) {
    return body != null && isNotEmpty(body.getAccountId()) && isNotEmpty(body.getOrgId())
        && isNotEmpty(body.getProjectId()) && isNotEmpty(body.getOrchestrationId()) && body.getSbomDetails() != null
        && body.getScores() != null && body.getScoreCardInfo() != null;
  }

  public static ScorecardEntity scorecardRequestToEntity(SbomScorecardRequestBody body) {
    return ScorecardEntity.builder()
        .accountId(body.getAccountId())
        .orgId(body.getOrgId())
        .projectId(body.getProjectId())
        .orchestrationId(body.getOrchestrationId())
        .creationOn(body.getCreationOn())
        .avgScore(body.getAvgScore())
        .sbom(ScorecardEntity.SBOM.builder()
                  .toolName(body.getSbomDetails().getToolName())
                  .toolVersion(body.getSbomDetails().getToolVersion())
                  .sbomFileName(body.getSbomDetails().getSbomFileName())
                  .sbomFormat(body.getSbomDetails().getSbomFormat())
                  .sbomVersion(body.getSbomDetails().getSbomVersion())
                  .fileFormat(body.getSbomDetails().getFileFormat())
                  .build())
        .scorecardInfo(ScorecardEntity.ScorecardInfo.builder()
                           .toolName(body.getScoreCardInfo().getToolName())
                           .toolVersion(body.getScoreCardInfo().getToolVersion())
                           .build())
        .scores(body.getScores()
                    .stream()
                    .map(score
                        -> ScorecardEntity.Score.builder()
                               .score(score.getScore())
                               .maxScore(score.getMaxScore())
                               .category(score.getCategory())
                               .description(score.getDescription())
                               .feature(score.getFeature())
                               .ignored(score.getIgnored())
                               .build())
                    .collect(Collectors.toList()))
        .build();
  }

  public static SbomScorecardResponseBody scorecardEntityToResponse(ScorecardEntity scorecardEntity) {
    return new SbomScorecardResponseBody()
        .accountId(scorecardEntity.getAccountId())
        .orchestrationId(scorecardEntity.getOrchestrationId())
        .orgId(scorecardEntity.getOrgId())
        .projectId(scorecardEntity.getProjectId())
        .avgScore(scorecardEntity.getAvgScore())
        .creationOn(scorecardEntity.getCreationOn())
        .sbomDetails(new SbomDetailsForScorecard()
                         .toolName(scorecardEntity.getSbom().getToolName())
                         .toolVersion(scorecardEntity.getSbom().getToolVersion())
                         .sbomFileName(scorecardEntity.getSbom().getSbomFileName())
                         .sbomFormat(scorecardEntity.getSbom().getSbomFormat())
                         .sbomVersion(scorecardEntity.getSbom().getSbomVersion())
                         .fileFormat(scorecardEntity.getSbom().getFileFormat()))
        .scoreCardInfo(new ScorecardInfo()
                           .toolName(scorecardEntity.getScorecardInfo().getToolName())
                           .toolVersion(scorecardEntity.getScorecardInfo().getToolVersion()))
        .scores(scorecardEntity.getScores()
                    .stream()
                    .map(score
                        -> new Score()
                               .score(score.getScore())
                               .maxScore(score.getMaxScore())
                               .category(score.getCategory())
                               .description(score.getDescription())
                               .feature(score.getFeature())
                               .ignored(score.getIgnored()))
                    .collect(Collectors.toList()));
  }
}
