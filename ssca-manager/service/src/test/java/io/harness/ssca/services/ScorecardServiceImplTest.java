/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.services;

import static io.harness.rule.OwnerRule.SHASHWAT_SACHAN;
import static io.harness.ssca.services.ScorecardServiceImpl.scorecardRequestToEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;

import io.harness.BuilderFactory;
import io.harness.SSCAManagerTestBase;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.repositories.ScorecardRepo;
import io.harness.rule.Owner;
import io.harness.spec.server.ssca.v1.model.SbomScorecardRequestBody;
import io.harness.spec.server.ssca.v1.model.SbomScorecardResponseBody;
import io.harness.ssca.entities.ScorecardEntity;

import com.google.inject.Inject;
import java.util.Optional;
import javax.ws.rs.NotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class ScorecardServiceImplTest extends SSCAManagerTestBase {
  @Inject ScorecardService scorecardService;

  @Inject ScorecardRepo scorecardRepository;

  @Inject ArtifactService artifactService;

  private BuilderFactory builderFactory;

  private final String ORCHESTRATION_ID = "orchestrationId";

  @Before
  public void setup() throws IllegalAccessException {
    MockitoAnnotations.initMocks(this);
    builderFactory = BuilderFactory.getDefault();
  }

  @Test
  @Owner(developers = SHASHWAT_SACHAN)
  @Category(UnitTests.class)
  public void testSave() {
    SbomScorecardRequestBody sbomScorecardRequestBody = builderFactory.getSbomScorecardRequestBody();

    Mockito
        .when(artifactService.getArtifact(sbomScorecardRequestBody.getAccountId(), sbomScorecardRequestBody.getOrgId(),
            sbomScorecardRequestBody.getProjectId(), sbomScorecardRequestBody.getOrchestrationId()))
        .thenReturn(Optional.ofNullable(builderFactory.getArtifactEntityBuilder().build()));

    scorecardService.save(sbomScorecardRequestBody);

    ArgumentCaptor<ScorecardEntity> argument = ArgumentCaptor.forClass(ScorecardEntity.class);

    Mockito.verify(scorecardRepository, Mockito.times(1)).save(argument.capture());

    ScorecardEntity capturedScorecardEntity = argument.getValue();

    assertThat(capturedScorecardEntity.getAccountId()).isEqualTo(builderFactory.getContext().getAccountId());
    assertThat(capturedScorecardEntity.getOrgId()).isEqualTo(builderFactory.getContext().getOrgIdentifier());
    assertThat(capturedScorecardEntity.getProjectId()).isEqualTo(builderFactory.getContext().getProjectIdentifier());
  }

  @Test
  @Owner(developers = SHASHWAT_SACHAN)
  @Category(UnitTests.class)
  public void testSaveArtifactNotFound() {
    SbomScorecardRequestBody sbomScorecardRequestBody = builderFactory.getSbomScorecardRequestBody();

    Mockito.when(artifactService.getArtifact(any(), any(), any(), any())).thenReturn(Optional.ofNullable(null));

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> scorecardService.save(sbomScorecardRequestBody))
        .withMessage(String.format("Could not find an associated artifact for orchestrationId [%s]",
            sbomScorecardRequestBody.getOrchestrationId()));
  }

  @Test
  @Owner(developers = SHASHWAT_SACHAN)
  @Category(UnitTests.class)
  public void testSaveInvalidAccountId() {
    SbomScorecardRequestBody sbomScorecardRequestBody = builderFactory.getSbomScorecardRequestBody();

    sbomScorecardRequestBody.setAccountId(null);

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> scorecardService.save(sbomScorecardRequestBody))
        .withMessage("Account Id should not be null or empty");
  }

  @Test
  @Owner(developers = SHASHWAT_SACHAN)
  @Category(UnitTests.class)
  public void testSaveInvalidOrgId() {
    SbomScorecardRequestBody sbomScorecardRequestBody = builderFactory.getSbomScorecardRequestBody();

    sbomScorecardRequestBody.setOrgId(null);

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> scorecardService.save(sbomScorecardRequestBody))
        .withMessage("Org Id should not be null or empty");
  }

  @Test
  @Owner(developers = SHASHWAT_SACHAN)
  @Category(UnitTests.class)
  public void testSaveInvalidProjectId() {
    SbomScorecardRequestBody sbomScorecardRequestBody = builderFactory.getSbomScorecardRequestBody();

    sbomScorecardRequestBody.setProjectId(null);

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> scorecardService.save(sbomScorecardRequestBody))
        .withMessage("Project Id should not be null or empty");
  }

  @Test
  @Owner(developers = SHASHWAT_SACHAN)
  @Category(UnitTests.class)
  public void testSaveInvalidOrchestrationId() {
    SbomScorecardRequestBody sbomScorecardRequestBody = builderFactory.getSbomScorecardRequestBody();
    sbomScorecardRequestBody.setOrchestrationId(null);

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> scorecardService.save(sbomScorecardRequestBody))
        .withMessage("Orchestration Id should not be null or empty");
  }

  @Test
  @Owner(developers = SHASHWAT_SACHAN)
  @Category(UnitTests.class)
  public void testGetScorecard() {
    SbomScorecardRequestBody sbomScorecardRequestBody = builderFactory.getSbomScorecardRequestBody();
    ScorecardEntity scorecardEntity = scorecardRequestToEntity(sbomScorecardRequestBody);
    Mockito.when(scorecardRepository.getByOrchestrationId(any(), any(), any(), any())).thenReturn(scorecardEntity);
    SbomScorecardResponseBody scorecardResponseBody =
        scorecardService.getByOrchestrationId(scorecardEntity.getAccountId(), scorecardEntity.getOrgId(),
            scorecardEntity.getProjectId(), scorecardEntity.getOrchestrationId());

    assertThat(scorecardResponseBody.getAccountId()).isEqualTo(scorecardEntity.getAccountId());
    assertThat(scorecardResponseBody.getOrgId()).isEqualTo(scorecardEntity.getOrgId());
    assertThat(scorecardResponseBody.getProjectId()).isEqualTo(scorecardEntity.getProjectId());
    assertThat(scorecardResponseBody.getOrchestrationId()).isEqualTo(scorecardEntity.getOrchestrationId());
    assertThat(scorecardResponseBody.getAvgScore()).isEqualTo(scorecardEntity.getAvgScore());
    assertThat(scorecardResponseBody.getMaxScore()).isEqualTo(scorecardEntity.getMaxScore());
  }

  @Test
  @Owner(developers = SHASHWAT_SACHAN)
  @Category(UnitTests.class)
  public void testGetScorecardNotFound() {
    Mockito.when(scorecardRepository.getByOrchestrationId(any(), any(), any(), any())).thenReturn(null);

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(()
                        -> scorecardService.getByOrchestrationId(builderFactory.getContext().getAccountId(),
                            builderFactory.getContext().getOrgIdentifier(),
                            builderFactory.getContext().getProjectIdentifier(), ORCHESTRATION_ID))
        .withMessage(String.format("Scorecard not found for orchestrationId [%s]", ORCHESTRATION_ID));
  }
}
