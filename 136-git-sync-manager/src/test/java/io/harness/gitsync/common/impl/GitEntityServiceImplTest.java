package io.harness.gitsync.common.impl;

import static io.harness.rule.OwnerRule.MEET;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.EntityType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.category.element.UnitTests;
import io.harness.common.EntityReference;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.gitsync.GitSyncTestBase;
import io.harness.gitsync.common.service.GitEntityService;
import io.harness.ng.core.EntityDetail;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.PL)
public class GitEntityServiceImplTest extends GitSyncTestBase {
  @Inject GitEntityService gitEntityService;
  EntityDetail entityDetail;
  EntityReference entityReference;
  YamlGitConfigDTO yamlGitConfigDTO;
  public static final String ACCOUNT = "account";
  public static final String ORG = "org";
  public static final String PROJECT = "project";
  public static final String FOLDER_PATH = "folderPath";
  public static final String FILE_PATH = "filePath";
  public static final String BRANCH_NAME = "branchName";
  public static final String COMMIT_ID = "commitId";
  public static final String IDENTIFIER = "identifier";
  public static final String REPO_IDENTIFIER = "repoIdentifier";
  public static final String NAME = "name";
  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = MEET)
  @Category(UnitTests.class)
  public void testDeleteAll() {
    entityReference = IdentifierRef.builder()
                          .repoIdentifier(REPO_IDENTIFIER)
                          .projectIdentifier(PROJECT)
                          .orgIdentifier(ORG)
                          .accountIdentifier(ACCOUNT)
                          .identifier(IDENTIFIER)
                          .isDefault(true)
                          .build();
    entityDetail = EntityDetail.builder().entityRef(entityReference).type(EntityType.CONNECTORS).name(NAME).build();
    yamlGitConfigDTO = YamlGitConfigDTO.builder()
                           .identifier(IDENTIFIER)
                           .accountIdentifier(ACCOUNT)
                           .organizationIdentifier(ORG)
                           .projectIdentifier(PROJECT)
                           .branch(BRANCH_NAME)
                           .build();
    gitEntityService.save(ACCOUNT, entityDetail, yamlGitConfigDTO, FOLDER_PATH, FILE_PATH, COMMIT_ID, BRANCH_NAME);
    gitEntityService.deleteAll(ACCOUNT, ORG, PROJECT);
    assertThat(gitEntityService.getDefaultEntities(ACCOUNT, ORG, PROJECT, IDENTIFIER)).hasSize(0);
  }
}