/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl;

import static io.harness.rule.OwnerRule.ABOSII;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;

import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;
import software.wings.beans.yaml.GitFetchFilesRequest;
import software.wings.service.intfc.yaml.GitClient;

import io.vavr.collection.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class GitServiceImplTest extends CategoryTest {
  @Mock private GitClient gitClient;

  @InjectMocks private GitServiceImpl gitService;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testDownloadFiles() {
    final GitConfig gitConfig = GitConfig.builder().build();
    final String commitRef = "test-01";
    final String filePath = "test-app";
    final GitFileConfig gitFileConfig = GitFileConfig.builder().commitId(commitRef).filePath(filePath).build();
    final String destinationDirectory = "./test/directory";
    final LogCallback logCallback = mock(LogCallback.class);

    doReturn(commitRef).when(gitClient).downloadFiles(
        eq(gitConfig), any(GitFetchFilesRequest.class), eq(destinationDirectory), eq(true), eq(logCallback));

    String resultRef = gitService.downloadFiles(gitConfig, gitFileConfig, destinationDirectory, true, logCallback);
    assertThat(resultRef).isEqualTo(commitRef);

    ArgumentCaptor<GitFetchFilesRequest> requestCaptor = ArgumentCaptor.forClass(GitFetchFilesRequest.class);
    verify(gitClient).downloadFiles(
        eq(gitConfig), requestCaptor.capture(), eq(destinationDirectory), eq(true), eq(logCallback));
    GitFetchFilesRequest request = requestCaptor.getValue();
    assertThat(request.getCommitId()).isEqualTo(commitRef);
    assertThat(request.getBranch()).isNull();
    assertThat(request.getFilePaths()).containsExactlyElementsOf(List.of(filePath));
    assertThat(request.isRecursive()).isTrue();
    assertThat(request.isCloneWithCheckout()).isTrue();
  }
}