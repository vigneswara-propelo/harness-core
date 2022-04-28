/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.serverless;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.PIYUSH_BHUWALKA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.connector.service.git.NGGitServiceImpl;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.task.git.GitFetchFilesTaskHelper;
import io.harness.delegate.task.git.ScmFetchFilesHelperNG;
import io.harness.git.GitClientV2;
import io.harness.git.model.AuthInfo;
import io.harness.git.model.AuthRequest;
import io.harness.git.model.FetchFilesResult;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.rule.Owner;
import io.harness.security.encryption.SecretDecryptionService;
import io.harness.shell.SshSessionConfig;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(CDP)
public class ServerlessGitFetchTaskHelperTest extends CategoryTest {
  @Mock private GitClientV2 gitClientV2;
  @Mock private GitFetchFilesTaskHelper gitFetchFilesTaskHelper;
  @Mock private ScmFetchFilesHelperNG scmFetchFilesHelper;
  @Mock private NGGitServiceImpl ngGitService;
  @Mock private SecretDecryptionService secretDecryptionService;
  @InjectMocks private ServerlessGitFetchTaskHelper serverlessGitFetchTaskHelper;

  private static final String accountId = "accountId";
  private static final String url = "url";
  private static final String branch = "branch";
  private static final String commitId = "commitId";
  private static final String connectorName = "connectorName";
  private GitConfigDTO gitConfigDTO = GitConfigDTO.builder().url(url).build();
  private SSHKeySpecDTO sshKeySpecDTO = SSHKeySpecDTO.builder().build();
  private SshSessionConfig sshSessionConfig = SshSessionConfig.Builder.aSshSessionConfig().build();
  private GitStoreDelegateConfig gitStoreDelegateConfig = GitStoreDelegateConfig.builder()
                                                              .sshKeySpecDTO(sshKeySpecDTO)
                                                              .encryptedDataDetails(Arrays.asList())
                                                              .optimizedFilesFetch(false)
                                                              .branch(branch)
                                                              .commitId(commitId)
                                                              .connectorName(connectorName)
                                                              .build();
  private List<String> filePaths = Arrays.asList();
  private AuthRequest authRequest = new AuthRequest(AuthInfo.AuthType.SSH_KEY);
  private FetchFilesResult fetchFilesResult = FetchFilesResult.builder().build();

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void getCompleteFilePathTest() {
    assertThat(ServerlessGitFetchTaskHelper.getCompleteFilePath("a", "b")).isEqualTo("ab");
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void getCompleteFilePathTestWhenFolderEmpty() {
    assertThat(ServerlessGitFetchTaskHelper.getCompleteFilePath(null, "b")).isEqualTo("b");
  }
}