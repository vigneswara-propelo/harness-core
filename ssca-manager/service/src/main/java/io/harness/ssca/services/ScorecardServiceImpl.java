/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.services;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.exception.InvalidRequestException;
import io.harness.repositories.ScorecardRepo;
import io.harness.spec.server.ssca.v1.model.CategoryScorecard;
import io.harness.spec.server.ssca.v1.model.CategoryScorecardChecks;
import io.harness.spec.server.ssca.v1.model.SbomDetailsForScorecard;
import io.harness.spec.server.ssca.v1.model.SbomScorecardRequestBody;
import io.harness.spec.server.ssca.v1.model.SbomScorecardResponseBody;
import io.harness.spec.server.ssca.v1.model.ScorecardInfo;
import io.harness.ssca.entities.ScorecardEntity;
import io.harness.ssca.entities.ScorecardEntity.Checks;

import com.google.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.NotFoundException;

public class ScorecardServiceImpl implements ScorecardService {
  @Inject ScorecardRepo scorecardRepo;

  @Override
  public void save(SbomScorecardRequestBody body) {
    validateRequest(body);

    ScorecardEntity scorecardEntity = scorecardRequestToEntity(body);
    scorecardRepo.save(scorecardEntity);
  }

  @Override
  public SbomScorecardResponseBody getByOrchestrationId(
      String accountId, String orgId, String projectId, String orchestrateId) {
    ScorecardEntity scorecardEntity = scorecardRepo.getByOrchestrationId(accountId, orgId, projectId, orchestrateId);
    SbomScorecardResponseBody sbomScorecardResponseBody = null;
    if (scorecardEntity != null) {
      sbomScorecardResponseBody = scorecardEntityToResponse(scorecardEntity);
    } else {
      throw new NotFoundException(String.format("Scorecard not found for orchestrationId [%s]", orchestrateId));
    }

    return sbomScorecardResponseBody;
  }

  public static ScorecardEntity scorecardRequestToEntity(SbomScorecardRequestBody body) {
    return ScorecardEntity.builder()
        .accountId(body.getAccountId())
        .orgId(body.getOrgId())
        .projectId(body.getProjectId())
        .orchestrationId(body.getOrchestrationId())
        .creationOn(body.getCreationOn())
        .avgScore(body.getAvgScore())
        .maxScore(body.getMaxScore())
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
        .categories(body.getCategory()
                        .stream()
                        .map(category
                            -> ScorecardEntity.Category.builder()
                                   .score(category.getScore())
                                   .maxScore(category.getMaxScore())
                                   .name(category.getName())
                                   .weightage(category.getWeightage())
                                   .isEnabled(category.getIsEnabled())
                                   .checks(getChecksFromRequestBody(category.getChecks()))
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
        .maxScore(scorecardEntity.getMaxScore())
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
        .category(scorecardEntity.getCategories()
                      .stream()
                      .map(category
                          -> new CategoryScorecard()
                                 .score(category.getScore())
                                 .maxScore(category.getMaxScore())
                                 .weightage(category.getWeightage())
                                 .isEnabled(category.getIsEnabled())
                                 .name(category.getName())
                                 .checks(getChecksFromEntity(category.getChecks())))
                      .collect(Collectors.toList()));
  }

  private static List<Checks> getChecksFromRequestBody(List<CategoryScorecardChecks> checks) {
    return checks.stream()
        .map(check
            -> Checks.builder()
                   .score(check.getScore())
                   .maxScore(check.getMaxScore())
                   .description(check.getDescription())
                   .isEnabled(check.getIsEnabled())
                   .name(check.getName())
                   .build())
        .collect(Collectors.toList());
  }

  private static List<CategoryScorecardChecks> getChecksFromEntity(List<Checks> checks) {
    return checks.stream()
        .map(check
            -> new CategoryScorecardChecks()
                   .description(check.getDescription())
                   .name(check.getName())
                   .score(check.getScore())
                   .maxScore(check.getMaxScore())
                   .isEnabled(check.getIsEnabled()))
        .collect(Collectors.toList());
  }

  private void validateRequest(SbomScorecardRequestBody body) {
    if (isEmpty(body.getAccountId())) {
      throw new InvalidRequestException("Account Id should not be null or empty");
    }
    if (isEmpty(body.getOrgId())) {
      throw new InvalidRequestException("Org Id should not be null or empty");
    }
    if (isEmpty(body.getProjectId())) {
      throw new InvalidRequestException("Project Id should not be null or empty");
    }
    if (isEmpty(body.getOrchestrationId())) {
      throw new InvalidRequestException("Orchestration Id should not be null or empty");
    }
  }
}
