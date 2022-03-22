package io.harness.repositories.envGroup;

import static io.harness.rule.OwnerRule.PRASHANTSHARMA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.envGroup.beans.EnvironmentGroupEntity;
import io.harness.exception.InvalidRequestException;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.persistance.GitAwarePersistence;
import io.harness.gitsync.persistance.GitSyncSdkService;
import io.harness.ng.core.environment.services.EnvironmentService;
import io.harness.rule.Owner;

import java.util.Arrays;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;

public class EnvironmentGroupRepositoryCustomImplTest extends CategoryTest {
  private String ACC_ID = "accId";
  private String ORG_ID = "orgId";
  private String PRO_ID = "proId";
  @Mock private GitSyncSdkService gitSyncSdkService;
  @Mock private EnvironmentService environmentService;
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

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testCreate() {
    EnvironmentGroupEntity environmentGroupEntity = EnvironmentGroupEntity.builder()
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
                                                        .build();

    ArgumentCaptor<EnvironmentGroupEntity> captorForEntity = ArgumentCaptor.forClass(EnvironmentGroupEntity.class);
    ArgumentCaptor<String> captorForYaml = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<ChangeType> captorForChangeEntityType = ArgumentCaptor.forClass(ChangeType.class);
    ArgumentCaptor<Class> captorForClassType = ArgumentCaptor.forClass(Class.class);

    doReturn(environmentGroupEntity)
        .when(gitAwarePersistence)
        .save(captorForEntity.capture(), captorForYaml.capture(), captorForChangeEntityType.capture(),
            captorForClassType.capture(), any());
    doReturn(Arrays.asList("env1", "env2"))
        .when(environmentService)
        .fetchesNonDeletedEnvIdentifiersFromList(environmentGroupEntity.getAccountId(),
            environmentGroupEntity.getOrgIdentifier(), environmentGroupEntity.getProjectIdentifier(),
            environmentGroupEntity.getEnvIdentifiers());
    EnvironmentGroupEntity resultedEntity = environmentGroupRepositoryCustom.create(environmentGroupEntity);

    // capture assertions
    assertThat(captorForEntity.getValue()).isEqualTo(environmentGroupEntity);
    assertThat(captorForYaml.getValue()).isEqualTo(environmentGroupEntity.getYaml());
    assertThat(captorForChangeEntityType.getValue()).isEqualTo(ChangeType.ADD);
    assertThat(captorForClassType.getValue()).isEqualTo(EnvironmentGroupEntity.class);

    // entity assertion
    assertThat(resultedEntity.getEnvIdentifiers().size()).isEqualTo(2);
    assertThat(resultedEntity.getAccountId()).isEqualTo(ACC_ID);
    assertThat(resultedEntity.getOrgIdentifier()).isEqualTo(ORG_ID);
    assertThat(resultedEntity.getProjectIdentifier()).isEqualTo(PRO_ID);
    assertThat(resultedEntity.getIdentifier()).isEqualTo("envGroup");
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testValidateNotExistentEnvIdentifiers() {
    EnvironmentGroupEntity environmentGroupEntity = EnvironmentGroupEntity.builder()
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
                                                        .build();

    doReturn(Arrays.asList("env1", "env2"))
        .when(environmentService)
        .fetchesNonDeletedEnvIdentifiersFromList(environmentGroupEntity.getAccountId(),
            environmentGroupEntity.getOrgIdentifier(), environmentGroupEntity.getProjectIdentifier(),
            environmentGroupEntity.getEnvIdentifiers());

    assertThatCode(() -> environmentGroupRepositoryCustom.validateNotExistentEnvIdentifiers(environmentGroupEntity))
        .doesNotThrowAnyException();

    doReturn(Arrays.asList("env1"))
        .when(environmentService)
        .fetchesNonDeletedEnvIdentifiersFromList(environmentGroupEntity.getAccountId(),
            environmentGroupEntity.getOrgIdentifier(), environmentGroupEntity.getProjectIdentifier(),
            environmentGroupEntity.getEnvIdentifiers());
    assertThatThrownBy(() -> environmentGroupRepositoryCustom.validateNotExistentEnvIdentifiers(environmentGroupEntity))
        .isInstanceOf(InvalidRequestException.class);

    doReturn(Arrays.asList("env2"))
        .when(environmentService)
        .fetchesNonDeletedEnvIdentifiersFromList(environmentGroupEntity.getAccountId(),
            environmentGroupEntity.getOrgIdentifier(), environmentGroupEntity.getProjectIdentifier(),
            environmentGroupEntity.getEnvIdentifiers());
    assertThatThrownBy(() -> environmentGroupRepositoryCustom.validateNotExistentEnvIdentifiers(environmentGroupEntity))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testList() {
    EnvironmentGroupEntity environmentGroupEntity = EnvironmentGroupEntity.builder()
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
                                                        .build();

    Criteria criteria = new Criteria();
    Pageable pageRequest =
        PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, EnvironmentGroupEntity.EnvironmentGroupKeys.createdAt));

    doReturn(Arrays.asList(environmentGroupEntity))
        .when(gitAwarePersistence)
        .find(criteria, pageRequest, PRO_ID, ORG_ID, ACC_ID, EnvironmentGroupEntity.class);
    doReturn(1L).when(gitAwarePersistence).count(criteria, PRO_ID, ORG_ID, ACC_ID, EnvironmentGroupEntity.class);
    Page<EnvironmentGroupEntity> page =
        environmentGroupRepositoryCustom.list(criteria, pageRequest, PRO_ID, ORG_ID, ACC_ID);
    assertThat(page.get().count()).isEqualTo(1L);
  }
}
