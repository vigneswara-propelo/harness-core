package io.harness.gitsync.common.dtos;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.sdk.GitSyncApiConstants;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "GitBranchDetails", description = "This contains details of the Git Branch")
@OwnedBy(PL)
public class GitBranchDetailsDTO {
  @Schema(description = GitSyncApiConstants.BRANCH_PARAM_MESSAGE) String name;
}
