/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.envGroup.mappers;

import static io.harness.rule.OwnerRule.PRASHANTSHARMA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.EntityType;
import io.harness.beans.IdentifierRef;
import io.harness.category.element.UnitTests;
import io.harness.cdng.envGroup.beans.EnvironmentGroupEntity;
import io.harness.encryption.ScopeHelper;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.envGroup.dto.EnvironmentGroupResponse;
import io.harness.ng.core.envGroup.dto.EnvironmentGroupResponseDTO;
import io.harness.rule.Owner;

import java.util.Arrays;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class EnvironmentGroupMapperTest extends CategoryTest {
  private String ACC_ID = "accId";
  private String ORG_ID = "orgId";
  private String PRO_ID = "proId";
  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testWriteDto() {
    EnvironmentGroupEntity environmentGroupEntity = EnvironmentGroupEntity.builder()
                                                        .accountId(ACC_ID)
                                                        .orgIdentifier(ORG_ID)
                                                        .projectIdentifier(PRO_ID)
                                                        .identifier("id1")
                                                        .name("envGroup")
                                                        .envIdentifiers(Arrays.asList("env1", "env2"))
                                                        .color("col")
                                                        .createdAt(1L)
                                                        .lastModifiedAt(2L)
                                                        .yaml("yaml")
                                                        .build();
    EnvironmentGroupResponseDTO environmentGroupResponseDTO = EnvironmentGroupMapper.writeDTO(environmentGroupEntity);
    assertThat(environmentGroupResponseDTO.getName()).isEqualTo("envGroup");
    assertThat(environmentGroupResponseDTO.getAccountId()).isEqualTo(ACC_ID);
    assertThat(environmentGroupResponseDTO.getOrgIdentifier()).isEqualTo(ORG_ID);
    assertThat(environmentGroupResponseDTO.getProjectIdentifier()).isEqualTo(PRO_ID);
    assertThat(environmentGroupResponseDTO.getIdentifier()).isEqualTo("id1");
    assertThat(environmentGroupResponseDTO.getEnvIdentifiers().size()).isEqualTo(2);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void toResponseWrapper() {
    EnvironmentGroupEntity environmentGroupEntity = EnvironmentGroupEntity.builder()
                                                        .accountId(ACC_ID)
                                                        .orgIdentifier(ORG_ID)
                                                        .projectIdentifier(PRO_ID)
                                                        .identifier("id1")
                                                        .name("envGroup")
                                                        .envIdentifiers(Arrays.asList("env1", "env2"))
                                                        .color("col")
                                                        .createdAt(1L)
                                                        .lastModifiedAt(2L)
                                                        .yaml("yaml")
                                                        .build();
    EnvironmentGroupResponse environmentGroupResponse =
        EnvironmentGroupMapper.toResponseWrapper(environmentGroupEntity);
    assertThat(environmentGroupResponse.getCreatedAt()).isEqualTo(1L);
    assertThat(environmentGroupResponse.getLastModifiedAt()).isEqualTo(2L);
    assertThat(environmentGroupResponse.getEnvGroup().getIdentifier()).isEqualTo("id1");
    assertThat(environmentGroupResponse.getEnvGroup().getEnvIdentifiers().size()).isEqualTo(2);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetEntityDetail() {
    EnvironmentGroupEntity environmentGroupEntity = EnvironmentGroupEntity.builder()
                                                        .accountId(ACC_ID)
                                                        .orgIdentifier(ORG_ID)
                                                        .projectIdentifier(PRO_ID)
                                                        .identifier("id1")
                                                        .name("envGroup")
                                                        .envIdentifiers(Arrays.asList("env1", "env2"))
                                                        .color("col")
                                                        .createdAt(1L)
                                                        .lastModifiedAt(2L)
                                                        .yaml("yaml")
                                                        .build();

    IdentifierRef expectedIdentifierRef =
        IdentifierRef.builder()
            .accountIdentifier(environmentGroupEntity.getAccountIdentifier())
            .orgIdentifier(environmentGroupEntity.getOrgIdentifier())
            .projectIdentifier(environmentGroupEntity.getProjectIdentifier())
            .scope(ScopeHelper.getScope(environmentGroupEntity.getAccountIdentifier(),
                environmentGroupEntity.getOrgIdentifier(), environmentGroupEntity.getProjectIdentifier()))
            .identifier(environmentGroupEntity.getIdentifier())
            .build();

    EntityDetail entityDetail = EnvironmentGroupMapper.getEntityDetail(environmentGroupEntity);

    assertThat(entityDetail.getName()).isEqualTo(environmentGroupEntity.getName());
    assertThat(entityDetail.getType()).isEqualTo(EntityType.ENVIRONMENT_GROUP);
    assertThat(entityDetail.getEntityRef()).isEqualTo(expectedIdentifierRef);
  }
}
