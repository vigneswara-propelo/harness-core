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
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class BaselineServiceImplTest extends SSCAManagerTestBase {
  @Inject BaselineService baselineService;
  @Inject BaselineRepository baselineRepository;
  private BuilderFactory builderFactory;
  @Before
  public void setup() throws IllegalAccessException {
    MockitoAnnotations.initMocks(this);
    builderFactory = BuilderFactory.getDefault();
  }

  @Test
  @Owner(developers = SHASHWAT_SACHAN)
  @Category(UnitTests.class)
  public void testSetBaselineForArtifact() {
    boolean response = baselineService.setBaselineForArtifact(builderFactory.getContext().getAccountId(),
        builderFactory.getContext().getOrgIdentifier(), builderFactory.getContext().getProjectIdentifier(),
        "artifactId", "tag");

    ArgumentCaptor<BaselineEntity> argument = ArgumentCaptor.forClass(BaselineEntity.class);
    Mockito.verify(baselineRepository, Mockito.times(1)).upsert(argument.capture());
    assertThat(response).isTrue();
  }

  @Test
  @Owner(developers = SHASHWAT_SACHAN)
  @Category(UnitTests.class)
  public void testSetBaselineForArtifactInvalidAccount() {
    boolean response = baselineService.setBaselineForArtifact("", builderFactory.getContext().getOrgIdentifier(),
        builderFactory.getContext().getProjectIdentifier(), "artifact", "tag");

    assertThat(response).isFalse();
  }

  @Test
  @Owner(developers = SHASHWAT_SACHAN)
  @Category(UnitTests.class)
  public void testSetBaselineForArtifactInvalidTag() {
    boolean response = baselineService.setBaselineForArtifact(builderFactory.getContext().getAccountId(),
        builderFactory.getContext().getOrgIdentifier(), builderFactory.getContext().getProjectIdentifier(), "artifact",
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
        builderFactory.getContext().getOrgIdentifier(), builderFactory.getContext().getProjectIdentifier(), "artifact");

    assertThat(baselineDTO.getArtifactId()).isEqualTo("artifact");
    assertThat(baselineDTO.getTag()).isEqualTo("tag");
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
                            builderFactory.getContext().getProjectIdentifier(), "artifact"))
        .withMessage("Baseline for artifact with artifactId [artifact] not found");
  }
}
