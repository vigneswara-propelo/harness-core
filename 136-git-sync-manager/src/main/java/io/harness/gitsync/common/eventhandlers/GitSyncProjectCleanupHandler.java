package io.harness.gitsync.common.eventhandlers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.service.GitEntityService;
import io.harness.gitsync.common.service.GitSyncSettingsService;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.core.fullsync.GitFullSyncConfigService;
import io.harness.gitsync.core.fullsync.GitFullSyncEntityService;
import io.harness.gitsync.core.fullsync.service.FullSyncJobService;
import io.harness.gitsync.gitsyncerror.service.GitSyncErrorService;

import com.google.inject.Inject;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PL)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class GitSyncProjectCleanupHandler {
  private final YamlGitConfigService yamlGitConfigService;
  private final GitSyncSettingsService gitSyncSettingsService;
  private final GitEntityService gitEntityService;
  private final GitSyncErrorService gitSyncErrorService;
  private final FullSyncJobService fullSyncJobService;
  private final GitFullSyncConfigService gitFullSyncConfigService;
  private final GitFullSyncEntityService gitFullSyncEntityService;

  public boolean deleteAssociatedGitEntities(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    log.info("Starting the deletion of git entities");
    try {
      removeYamlGitConfigAndSetupUsages(accountIdentifier, orgIdentifier, projectIdentifier);
      removeGitSyncSettings(accountIdentifier, orgIdentifier, projectIdentifier);
      removeGitFileLocation(accountIdentifier, orgIdentifier, projectIdentifier);
      removeScopeFromGitSyncError(accountIdentifier, orgIdentifier, projectIdentifier);
      removeGitFullSyncJob(accountIdentifier, orgIdentifier, projectIdentifier);
      removeGitFullSyncConfig(accountIdentifier, orgIdentifier, projectIdentifier);
      removeGitFullSyncEntityInfo(accountIdentifier, orgIdentifier, projectIdentifier);
      log.info("Successfully deleted all the git entities for projectId : {}", projectIdentifier);
      return true;
    } catch (Exception ex) {
      log.error("Failed to delete all the git entities for projectId : {}", projectIdentifier, ex);
      return false;
    }
  }

  private void removeYamlGitConfigAndSetupUsages(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    yamlGitConfigService.deleteAllEntities(accountIdentifier, orgIdentifier, projectIdentifier);
  }

  private void removeGitSyncSettings(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    gitSyncSettingsService.delete(accountIdentifier, orgIdentifier, projectIdentifier);
  }

  private void removeGitFileLocation(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    gitEntityService.deleteAll(accountIdentifier, orgIdentifier, projectIdentifier);
  }

  private void removeScopeFromGitSyncError(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    gitSyncErrorService.removeScope(accountIdentifier, orgIdentifier, projectIdentifier);
  }

  private void removeGitFullSyncJob(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    fullSyncJobService.deleteAll(accountIdentifier, orgIdentifier, projectIdentifier);
  }

  private void removeGitFullSyncConfig(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    gitFullSyncConfigService.deleteAll(accountIdentifier, orgIdentifier, projectIdentifier);
  }

  private void removeGitFullSyncEntityInfo(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    gitFullSyncEntityService.deleteAll(accountIdentifier, orgIdentifier, projectIdentifier);
  }
}
