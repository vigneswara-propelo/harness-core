package io.harness.repositories.pipeline;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.pipeline.PipelineMetadataV2;

import java.util.Optional;

@OwnedBy(HarnessTeam.PIPELINE)
public interface PipelineMetadataV2RepositoryCustom {
  PipelineMetadataV2 incCounter(String accountId, String orgId, String projectIdentifier, String pipelineId);

  Optional<PipelineMetadataV2> getPipelineMetadata(
      String accountId, String orgId, String projectIdentifier, String identifier);

  Optional<PipelineMetadataV2> cloneFromPipelineMetadata(
      String accountId, String orgId, String projectIdentifier, String identifier);
}