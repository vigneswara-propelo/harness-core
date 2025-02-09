/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.ngpipeline.inputset.beans.resource;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitaware.helper.MoveConfigOperationType;
import io.harness.gitsync.sdk.GitSyncApiConstants;
import io.harness.pms.pipeline.PipelineResourceConstants;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Getter
@Builder
@FieldNameConstants(innerTypeName = "InputSetMoveConfigRequestKeys")
@Schema(name = "InputSetMoveConfigRequestDTO",
    description = "Details to move input set from inline to remote or remote to inline")
@OwnedBy(PIPELINE)
@NoArgsConstructor
@AllArgsConstructor
public class InputSetMoveConfigRequestDTO {
  @Parameter(description = GitSyncApiConstants.GIT_CONNECTOR_REF_PARAM_MESSAGE)
  @QueryParam(GitSyncApiConstants.CONNECTOR_REF)
  String connectorRef;
  @Parameter(description = GitSyncApiConstants.REPO_NAME_PARAM_MESSAGE)
  @QueryParam(GitSyncApiConstants.REPO_NAME)
  String repoName;
  @Parameter(description = GitSyncApiConstants.BRANCH_PARAM_MESSAGE)
  @QueryParam(GitSyncApiConstants.BRANCH_KEY)
  String branch;
  @Parameter(description = GitSyncApiConstants.FILEPATH_PARAM_MESSAGE)
  @QueryParam(GitSyncApiConstants.FILE_PATH_KEY)
  String filePath;
  @Parameter(description = GitSyncApiConstants.FILEPATH_PARAM_MESSAGE)
  @QueryParam(GitSyncApiConstants.COMMIT_MSG_KEY)
  String commitMsg;
  @Parameter(description = "Checks the new branch")
  @DefaultValue("false")
  @QueryParam(GitSyncApiConstants.NEW_BRANCH)
  Boolean isNewBranch;
  @Parameter(description = GitSyncApiConstants.DEFAULT_BRANCH_PARAM_MESSAGE)
  @QueryParam(GitSyncApiConstants.BASE_BRANCH)
  String baseBranch;

  @Parameter(description = GitSyncApiConstants.MOVE_CONFIG_PARAM_MESSAGE)
  @QueryParam(GitSyncApiConstants.MOVE_CONFIG_KEY)
  MoveConfigOperationType moveConfigOperationType;
  @Parameter(description = PipelineResourceConstants.PIPELINE_ID_PARAM_MESSAGE)
  @QueryParam(NGCommonEntityConstants.PIPELINE_KEY)
  String pipelineIdentifier;
  @Parameter(description = PipelineResourceConstants.INPUT_SET_ID_PARAM_MESSAGE)
  @QueryParam(NGCommonEntityConstants.INPUT_SET_IDENTIFIER_KEY)
  String inputSetIdentifier;
}
