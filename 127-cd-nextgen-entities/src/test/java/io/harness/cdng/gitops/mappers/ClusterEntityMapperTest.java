package io.harness.cdng.gitops.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.gitops.beans.ClusterRequest;
import io.harness.cdng.gitops.beans.ClusterResponse;
import io.harness.cdng.gitops.entity.Cluster;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ClusterEntityMapperTest extends CategoryTest {
  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void testToEntity() {
    ClusterRequest request = ClusterRequest.builder()
                                 .identifier("id")
                                 .name("name")
                                 .envRef("env")
                                 .orgIdentifier("orgId")
                                 .projectIdentifier("orgId")
                                 .build();

    Cluster entity = ClusterEntityMapper.toEntity("accountId", request);

    assertThat(entity.getAccountId()).isEqualTo("accountId");
    assertThat(entity.getOrgIdentifier()).isEqualTo("orgId");
    assertThat(entity.getProjectIdentifier()).isEqualTo("orgId");
    assertThat(entity.getIdentifier()).isEqualTo("id");
    assertThat(entity.getName()).isEqualTo("name");
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void testWriteDTO() {
    Cluster request = Cluster.builder()
                          .accountId("accountId")
                          .identifier("id")
                          .name("name")
                          .envRef("env")
                          .orgIdentifier("orgId")
                          .projectIdentifier("orgId")
                          .build();

    ClusterResponse entity = ClusterEntityMapper.writeDTO(request);

    assertThat(entity.getOrgIdentifier()).isEqualTo("orgId");
    assertThat(entity.getProjectIdentifier()).isEqualTo("orgId");
    assertThat(entity.getIdentifier()).isEqualTo("id");
    assertThat(entity.getName()).isEqualTo("name");
  }
}