package io.harness.gitsync.common.dtos;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Schema(name = "GitRepositoryDetails", description = "This contains details of a Git Repository")
@OwnedBy(PL)
public class GitRepositoryResponseDTO {
  @Schema(description = "Name of Git Repository") String name;
}
