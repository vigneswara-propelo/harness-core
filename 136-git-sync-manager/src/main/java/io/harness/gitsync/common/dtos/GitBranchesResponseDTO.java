package io.harness.gitsync.common.dtos;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Schema(name = "GitBranches", description = "This contains list of Git Branches for a given Git Repository.")
@OwnedBy(PL)
public class GitBranchesResponseDTO {
  @Schema(description = "This contains details of branches of given repo.") List<GitBranchDetailsDTO> branches;
  @Schema(description = "This contains details of the default branch.") GitBranchDetailsDTO defaultBranch;
}
