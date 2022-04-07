/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.core.fullsync;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.DEEPAK;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.gitsync.GitSyncTestBase;
import io.harness.gitsync.core.beans.GitFullSyncEntityInfo;
import io.harness.rule.Owner;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(PL)
public class GitFullSyncProcessorServiceImplTest extends GitSyncTestBase {
  @InjectMocks GitFullSyncProcessorServiceImpl fullSyncProcessorService;
  @Mock GitFullSyncEntityService gitFullSyncEntityService;
  String messageId = "messageId";
  String accountId = "accountId";
  String orgId = "orgId";
  String projectId = "projectId";

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void changeTheStatusOfFilesWhichSucceeded_Test() {
    GitFullSyncEntityInfo gitFullSyncEntityInfo = GitFullSyncEntityInfo.builder()
                                                      .accountIdentifier(accountId)
                                                      .orgIdentifier(orgId)
                                                      .projectIdentifier(projectId)
                                                      .filePath("filePath1")
                                                      .build();
    GitFullSyncEntityInfo gitFullSyncEntityInfo1 = GitFullSyncEntityInfo.builder()
                                                       .accountIdentifier(accountId)
                                                       .orgIdentifier(orgId)
                                                       .projectIdentifier(projectId)
                                                       .filePath("filePath2")
                                                       .build();
    when(gitFullSyncEntityService.getQueuedEntitiesFromPreviousJobs(accountId, orgId, projectId, messageId))
        .thenReturn(Arrays.asList(gitFullSyncEntityInfo, gitFullSyncEntityInfo1));

    GitFullSyncEntityInfo newEntityToBeSynced1 = GitFullSyncEntityInfo.builder()
                                                     .accountIdentifier(accountId)
                                                     .orgIdentifier(orgId)
                                                     .projectIdentifier(projectId)
                                                     .filePath("filePath3")
                                                     .build();
    GitFullSyncEntityInfo newEntityToBeSynced2 = GitFullSyncEntityInfo.builder()
                                                     .accountIdentifier(accountId)
                                                     .orgIdentifier(orgId)
                                                     .projectIdentifier(projectId)
                                                     .filePath("filePath4")
                                                     .build();
    GitFullSyncEntityInfo newEntityToBeSynced3 = GitFullSyncEntityInfo.builder()
                                                     .accountIdentifier(accountId)
                                                     .orgIdentifier(orgId)
                                                     .projectIdentifier(projectId)
                                                     .filePath("filePath1")
                                                     .build();
    fullSyncProcessorService.markAlreadyProcessedFilesAsSuccess(accountId, orgId, projectId, "messageId",
        Arrays.asList(newEntityToBeSynced1, newEntityToBeSynced2, newEntityToBeSynced3));
    verify(gitFullSyncEntityService, times(1)).updateStatus(any(), any(), any(), any());
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void changeTheStatusOfFilesWhichSucceeded_Test1() {
    when(gitFullSyncEntityService.getQueuedEntitiesFromPreviousJobs(accountId, orgId, projectId, "messageId"))
        .thenReturn(Collections.emptyList());
    GitFullSyncEntityInfo newEntityToBeSynced1 = GitFullSyncEntityInfo.builder()
                                                     .accountIdentifier(accountId)
                                                     .orgIdentifier(orgId)
                                                     .projectIdentifier(projectId)
                                                     .filePath("filePath3")
                                                     .build();
    fullSyncProcessorService.markAlreadyProcessedFilesAsSuccess(
        accountId, orgId, projectId, messageId, Arrays.asList(newEntityToBeSynced1));
    verify(gitFullSyncEntityService, never()).updateStatus(any(), any(), any(), any());
  }
}
