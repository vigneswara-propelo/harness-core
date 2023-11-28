/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.services;

import static io.harness.rule.OwnerRule.SHASHWAT_SACHAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import io.harness.BuilderFactory;
import io.harness.SSCAManagerTestBase;
import io.harness.category.element.UnitTests;
import io.harness.repositories.ConfigRepo;
import io.harness.rule.Owner;
import io.harness.spec.server.ssca.v1.model.ConfigResponseBody;
import io.harness.ssca.entities.ConfigEntity;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

public class ConfigServiceImplTest extends SSCAManagerTestBase {
  @Inject ConfigService configService;

  @Inject ConfigRepo configRepo;

  private BuilderFactory builderFactory;

  private final String CONFIG_ID = "configId";

  private final String CONFIG_NAME = "sbomqs";

  private final String CONFIG_TYPE = "scorecard";

  @Before
  public void setup() throws IllegalAccessException {
    MockitoAnnotations.initMocks(this);
    builderFactory = BuilderFactory.getDefault();
  }

  @Test
  @Owner(developers = SHASHWAT_SACHAN)
  @Category(UnitTests.class)
  public void testGetConfigById() {
    Mockito.when(configRepo.findOne(any(), any(), any(), any()))
        .thenReturn(builderFactory.getConfigEntityBuilder().build());
    ConfigResponseBody configResponseBody = configService.getConfigById(builderFactory.getContext().getOrgIdentifier(),
        builderFactory.getContext().getProjectIdentifier(), CONFIG_ID, builderFactory.getContext().getAccountId());

    assertThat(configResponseBody.getAccountId()).isEqualTo(builderFactory.getContext().getAccountId());
    assertThat(configResponseBody.getOrgId()).isEqualTo(builderFactory.getContext().getOrgIdentifier());
    assertThat(configResponseBody.getProjectId()).isEqualTo(builderFactory.getContext().getProjectIdentifier());
    assertThat(configResponseBody.getConfigId()).isEqualTo(CONFIG_ID);
    assertThat(configResponseBody.getName()).isEqualTo(CONFIG_NAME);
    assertThat(configResponseBody.getType()).isEqualTo(CONFIG_TYPE);
    assertThat(configResponseBody.getConfigInfo()).hasSize(1);
  }

  @Test
  @Owner(developers = SHASHWAT_SACHAN)
  @Category(UnitTests.class)
  public void testGetConfigByNameAndType() {
    Mockito.when(configRepo.findByAccountIdAndProjectIdAndOrgIdAndNameAndType(any(), any(), any(), any(), any()))
        .thenReturn(builderFactory.getConfigEntityBuilder().build());
    ConfigResponseBody configResponseBody = configService.getConfigByNameAndType(
        builderFactory.getContext().getOrgIdentifier(), builderFactory.getContext().getProjectIdentifier(), CONFIG_NAME,
        CONFIG_TYPE, builderFactory.getContext().getAccountId());

    assertThat(configResponseBody.getAccountId()).isEqualTo(builderFactory.getContext().getAccountId());
    assertThat(configResponseBody.getOrgId()).isEqualTo(builderFactory.getContext().getOrgIdentifier());
    assertThat(configResponseBody.getProjectId()).isEqualTo(builderFactory.getContext().getProjectIdentifier());
    assertThat(configResponseBody.getConfigId()).isEqualTo(CONFIG_ID);
    assertThat(configResponseBody.getName()).isEqualTo(CONFIG_NAME);
    assertThat(configResponseBody.getType()).isEqualTo(CONFIG_TYPE);
    assertThat(configResponseBody.getConfigInfo()).hasSize(1);
  }

  @Test
  @Owner(developers = SHASHWAT_SACHAN)
  @Category(UnitTests.class)
  public void testListConfigs() {
    List<ConfigEntity> configEntities =
        Arrays.asList(builderFactory.getConfigEntityBuilder().configId("configId1").build(),
            builderFactory.getConfigEntityBuilder().configId("configId2").build(),
            builderFactory.getConfigEntityBuilder().configId("configId3").build());

    Page<ConfigEntity> configEntityPage = new PageImpl<>(configEntities, Pageable.ofSize(2).withPage(0), 5);

    Mockito.when(configRepo.findAll(any(), any(), any(), any())).thenReturn(configEntityPage);

    Page<ConfigResponseBody> pageConfigs = configService.listConfigs(builderFactory.getContext().getOrgIdentifier(),
        builderFactory.getContext().getProjectIdentifier(), builderFactory.getContext().getAccountId(),
        Pageable.ofSize(3).withPage(0));
    List<ConfigResponseBody> listConfigs = pageConfigs.get().collect(Collectors.toList());
    assertThat(listConfigs.size()).isEqualTo(3);
  }
}
