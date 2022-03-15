package io.harness.repositories.envGroup;

import static io.harness.rule.OwnerRule.PRASHANTSHARMA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.envGroup.beans.EnvironmentGroupEntity;
import io.harness.gitsync.persistance.GitAwarePersistence;
import io.harness.rule.Owner;

import java.util.Arrays;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.query.Criteria;

public class EnvironmentGroupRepositoryCustomImplTest extends CategoryTest {
  private String ACC_ID = "accId";
  private String ORG_ID = "orgId";
  private String PRO_ID = "proId";
  @Mock private GitAwarePersistence gitAwarePersistence;

  @InjectMocks private EnvironmentGroupRepositoryCustomImpl environmentGroupRepositoryCustom;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testFindByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot() {
    Criteria criteria = Criteria.where(EnvironmentGroupEntity.EnvironmentGroupKeys.deleted)
                            .is(false)
                            .and(EnvironmentGroupEntity.EnvironmentGroupKeys.projectIdentifier)
                            .is(PRO_ID)
                            .and(EnvironmentGroupEntity.EnvironmentGroupKeys.orgIdentifier)
                            .is(ORG_ID)
                            .and(EnvironmentGroupEntity.EnvironmentGroupKeys.accountId)
                            .is(ACC_ID)
                            .and(EnvironmentGroupEntity.EnvironmentGroupKeys.identifier)
                            .is("envGroup");
    Optional<EnvironmentGroupEntity> environmentGroupEntity =
        Optional.ofNullable(EnvironmentGroupEntity.builder()
                                .accountId(ACC_ID)
                                .orgIdentifier(ORG_ID)
                                .projectIdentifier(PRO_ID)
                                .identifier("envGroup")
                                .name("envGroup")
                                .envIdentifiers(Arrays.asList("env1", "env2"))
                                .color("col")
                                .createdAt(1L)
                                .lastModifiedAt(2L)
                                .yaml("yaml")
                                .build());
    doReturn(environmentGroupEntity)
        .when(gitAwarePersistence)
        .findOne(criteria, PRO_ID, ORG_ID, ACC_ID, EnvironmentGroupEntity.class);
    Optional<EnvironmentGroupEntity> environmentGroup =
        environmentGroupRepositoryCustom.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
            ACC_ID, ORG_ID, PRO_ID, "envGroup", true);
    EnvironmentGroupEntity resultedEntity = environmentGroup.get();
    assertThat(resultedEntity.getEnvIdentifiers().size()).isEqualTo(2);
    assertThat(resultedEntity.getAccountId()).isEqualTo(ACC_ID);
    assertThat(resultedEntity.getOrgIdentifier()).isEqualTo(ORG_ID);
    assertThat(resultedEntity.getProjectIdentifier()).isEqualTo(PRO_ID);
    assertThat(resultedEntity.getIdentifier()).isEqualTo("envGroup");
  }
}
