package io.harness.repositories.pipeline;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.persistance.GitSyncableHarnessRepo;
import io.harness.pms.pipeline.PipelineMetadataV2;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

@GitSyncableHarnessRepo
@Transactional
@OwnedBy(PIPELINE)
public interface PipelineMetadataV2Repository
    extends PagingAndSortingRepository<PipelineMetadataV2, String>, PipelineMetadataV2RepositoryCustom {
  Optional<PipelineMetadataV2> findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier);
}
