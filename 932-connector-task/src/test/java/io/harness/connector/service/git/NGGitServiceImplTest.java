/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.connector.service.git;

import static io.harness.rule.OwnerRule.ABHINAV;
import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.SATHISH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.MockableTestMixin;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.encryption.SecretRefData;
import io.harness.git.GitClientV2;
import io.harness.git.model.DownloadFilesRequest;
import io.harness.git.model.GitBaseRequest;
import io.harness.git.model.GitRepositoryType;
import io.harness.rule.Owner;
import io.harness.shell.SshSessionConfig;

import com.google.inject.Inject;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class NGGitServiceImplTest extends CategoryTest implements MockableTestMixin {
  @Mock GitClientV2 gitClientV2;
  @InjectMocks @Inject NGGitServiceImpl ngGitService;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testGetGitBaseRequest() {
    final String accountId = "accountId";
    GitConfigDTO gitConfigDTO = GitConfigDTO.builder()
                                    .gitAuth(GitHTTPAuthenticationDTO.builder()
                                                 .username("username")
                                                 .passwordRef(SecretRefData.builder().build())
                                                 .build())
                                    .gitAuthType(GitAuthType.HTTP)
                                    .build();
    final GitBaseRequest gitBaseRequest = GitBaseRequest.builder().build();
    ngGitService.setGitBaseRequest(gitConfigDTO, accountId, gitBaseRequest, GitRepositoryType.YAML, null, true);
    assertThat(gitBaseRequest).isNotNull();
    assertThat(gitBaseRequest.getRepoType()).isNotNull();
    assertThat(gitBaseRequest.getAccountId()).isNotNull();
    assertThat(gitBaseRequest.getAuthRequest()).isNotNull();
  }

  @Test
  @Owner(developers = SATHISH)
  @Category(UnitTests.class)
  public void testGetGitBaseRequestWithoutGitConfigOverride() {
    final String accountId = "accountId";
    GitConfigDTO gitConfigDTO = GitConfigDTO.builder()
                                    .gitAuth(GitHTTPAuthenticationDTO.builder()
                                                 .username("username")
                                                 .passwordRef(SecretRefData.builder().build())
                                                 .build())
                                    .gitAuthType(GitAuthType.HTTP)
                                    .url("https://gitconfig.com")
                                    .branchName("git-config-test-branch")
                                    .build();
    final GitBaseRequest gitBaseRequest =
        GitBaseRequest.builder().repoUrl("https://gitBaserequest.com/repo").branch("test-branch").build();
    ngGitService.setGitBaseRequest(gitConfigDTO, accountId, gitBaseRequest, GitRepositoryType.YAML, null, false);
    assertThat(gitBaseRequest).isNotNull();
    assertThat(gitBaseRequest.getRepoType()).isNotNull();
    assertThat(gitBaseRequest.getAccountId()).isNotNull();
    assertThat(gitBaseRequest.getAuthRequest()).isNotNull();
    assertThat(gitBaseRequest.getRepoUrl()).isEqualTo("https://gitBaserequest.com/repo");
    assertThat(gitBaseRequest.getBranch()).isEqualTo("test-branch");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  @SneakyThrows
  public void testDownloadFiles() {
    final String destinationDirectory = "./test/file-path";
    final String accountId = "account-id";
    final String repoUrl = "git@git.repo";
    final List<String> paths = List.of("path1", "path2");
    final GitStoreDelegateConfig gitStoreDelegateConfig = GitStoreDelegateConfig.builder().paths(paths).build();
    final SshSessionConfig sshSessionConfig = SshSessionConfig.Builder.aSshSessionConfig().build();
    final GitConfigDTO gitConfigDTO = GitConfigDTO.builder().gitAuthType(GitAuthType.SSH).url(repoUrl).build();

    ngGitService.downloadFiles(
        gitStoreDelegateConfig, destinationDirectory, accountId, sshSessionConfig, gitConfigDTO, true);

    ArgumentCaptor<DownloadFilesRequest> requestArgumentCaptor = ArgumentCaptor.forClass(DownloadFilesRequest.class);

    verify(gitClientV2).downloadFiles(requestArgumentCaptor.capture());
    DownloadFilesRequest downloadRequest = requestArgumentCaptor.getValue();

    assertThat(downloadRequest.getAccountId()).isEqualTo(accountId);
    assertThat(downloadRequest.getDestinationDirectory()).isEqualTo(destinationDirectory);
    assertThat(downloadRequest.isCloneWithCheckout()).isTrue();
    assertThat(downloadRequest.getRepoType()).isEqualTo(GitRepositoryType.YAML);
    assertThat(downloadRequest.getRepoUrl()).isEqualTo(repoUrl);
    assertThat(downloadRequest.isRecursive()).isTrue();
    assertThat(downloadRequest.getFilePaths()).isEqualTo(paths);
    assertThat(downloadRequest.isMayHaveMultipleFolders()).isTrue();
  }
}
