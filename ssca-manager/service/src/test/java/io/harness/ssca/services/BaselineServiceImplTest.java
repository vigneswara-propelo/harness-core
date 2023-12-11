/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.services;

import static io.harness.rule.OwnerRule.SHASHWAT_SACHAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;

import io.harness.BuilderFactory;
import io.harness.SSCAManagerTestBase;
import io.harness.category.element.UnitTests;
import io.harness.repositories.BaselineRepository;
import io.harness.rule.Owner;
import io.harness.ssca.beans.BaselineDTO;
import io.harness.ssca.entities.BaselineEntity;

import com.google.inject.Inject;
import javax.ws.rs.NotFoundException;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class BaselineServiceImplTest extends SSCAManagerTestBase {
  @Mock ArtifactService artifactService;
  @Inject BaselineService baselineService;
  @Mock BaselineRepository baselineRepository;
  private BuilderFactory builderFactory;

  private final String ARTIFACT_ID = "artifactId";

  private final String TAG = "tag";
  @Before
  public void setup() throws IllegalAccessException {
    MockitoAnnotations.initMocks(this);
    FieldUtils.writeField(baselineService, "baselineRepository", baselineRepository, true);
    FieldUtils.writeField(baselineService, "artifactService", artifactService, true);
    builderFactory = BuilderFactory.getDefault();
  }

  @Test
  @Owner(developers = SHASHWAT_SACHAN)
  @Category(UnitTests.class)
  public void testSetBaselineForArtifact() {
    Mockito.when(artifactService.getLatestArtifact(any(), any(), any(), any(), any()))
        .thenReturn(builderFactory.getArtifactEntityBuilder().build());

    boolean response = baselineService.setBaselineForArtifact(builderFactory.getContext().getAccountId(),
        builderFactory.getContext().getOrgIdentifier(), builderFactory.getContext().getProjectIdentifier(), ARTIFACT_ID,
        TAG);

    ArgumentCaptor<BaselineEntity> argument = ArgumentCaptor.forClass(BaselineEntity.class);
    Mockito.verify(baselineRepository, Mockito.times(1)).upsert(argument.capture());
    assertThat(response).isTrue();
  }

  @Test
  @Owner(developers = SHASHWAT_SACHAN)
  @Category(UnitTests.class)
  public void testSetBaselineArtifactNotFound() {
    Mockito.when(artifactService.getLatestArtifact(any(), any(), any(), any(), any())).thenReturn(null);

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(()
                        -> baselineService.setBaselineForArtifact(builderFactory.getContext().getAccountId(),
                            builderFactory.getContext().getOrgIdentifier(),
                            builderFactory.getContext().getProjectIdentifier(), ARTIFACT_ID, TAG))
        .withMessage(
            String.format("Artifact does not exist with fields artifactId [%s] and tag [%s]", ARTIFACT_ID, TAG));
  }

  @Test
  @Owner(developers = SHASHWAT_SACHAN)
  @Category(UnitTests.class)
  public void testSetBaselineForArtifactInvalidAccount() {
    boolean response = baselineService.setBaselineForArtifact("", builderFactory.getContext().getOrgIdentifier(),
        builderFactory.getContext().getProjectIdentifier(), ARTIFACT_ID, TAG);

    assertThat(response).isFalse();
  }

  @Test
  @Owner(developers = SHASHWAT_SACHAN)
  @Category(UnitTests.class)
  public void testSetBaselineForArtifactInvalidTag() {
    boolean response = baselineService.setBaselineForArtifact(builderFactory.getContext().getAccountId(),
        builderFactory.getContext().getOrgIdentifier(), builderFactory.getContext().getProjectIdentifier(), ARTIFACT_ID,
        "");

    assertThat(response).isFalse();
  }

  @Test
  @Owner(developers = SHASHWAT_SACHAN)
  @Category(UnitTests.class)
  public void testGetBaselineForArtifact() {
    Mockito.when(baselineRepository.findOne(any(), any(), any(), any()))
        .thenReturn(builderFactory.getBaselineEntityBuilder().build());

    BaselineDTO baselineDTO = baselineService.getBaselineForArtifact(builderFactory.getContext().getAccountId(),
        builderFactory.getContext().getOrgIdentifier(), builderFactory.getContext().getProjectIdentifier(),
        ARTIFACT_ID);

    assertThat(baselineDTO.getArtifactId()).isEqualTo(ARTIFACT_ID);
    assertThat(baselineDTO.getTag()).isEqualTo(TAG);
  }

  @Test
  @Owner(developers = SHASHWAT_SACHAN)
  @Category(UnitTests.class)
  public void testGetBaselineForArtifactNotFound() {
    Mockito.when(baselineRepository.findOne(any(), any(), any(), any())).thenReturn(null);

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(()
                        -> baselineService.getBaselineForArtifact(builderFactory.getContext().getAccountId(),
                            builderFactory.getContext().getOrgIdentifier(),
                            builderFactory.getContext().getProjectIdentifier(), ARTIFACT_ID))
        .withMessage(String.format("Baseline for artifact with artifactId [%s] not found", ARTIFACT_ID));
  }
}
