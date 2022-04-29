package io.harness.gitsync.common.scmerrorhandling;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.ScmApis;
import io.harness.gitsync.common.dtos.RepoProviders;
import io.harness.gitsync.common.scmerrorhandling.handlers.DefaultScmApiErrorHandler;
import io.harness.gitsync.common.scmerrorhandling.handlers.ScmApiErrorHandler;
import io.harness.gitsync.common.scmerrorhandling.handlers.bitbucket.BitbucketListRepoScmApiErrorHandler;
import io.harness.gitsync.common.scmerrorhandling.handlers.github.GithubListRepoScmApiErrorHandler;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.Map;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@UtilityClass
@Slf4j
@OwnedBy(PL)
class ScmApiErrorHandlerFactory {
  @Inject DefaultScmApiErrorHandler defaultScmApiErrorHandler;
  private final Map<Pair<ScmApis, RepoProviders>, Class<? extends ScmApiErrorHandler>> scmApiErrorHandlerMap =
      ImmutableMap.<Pair<ScmApis, RepoProviders>, Class<? extends ScmApiErrorHandler>>builder()
          .put(Pair.of(ScmApis.LIST_REPOSITORIES, RepoProviders.BITBUCKET), BitbucketListRepoScmApiErrorHandler.class)
          .put(Pair.of(ScmApis.LIST_REPOSITORIES, RepoProviders.GITHUB), GithubListRepoScmApiErrorHandler.class)
          .build();

  public ScmApiErrorHandler getHandler(ScmApis scmApi, RepoProviders repoProvider) {
    try {
      return scmApiErrorHandlerMap.get(Pair.of(scmApi, repoProvider)).newInstance();
    } catch (Exception ex) {
      log.error(String.format("Error while getting handler for scmApi [%s] and repoProvider [%s]"), ex);
    }
    return defaultScmApiErrorHandler;
  }
}
