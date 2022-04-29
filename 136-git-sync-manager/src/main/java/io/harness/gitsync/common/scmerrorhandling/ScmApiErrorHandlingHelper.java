package io.harness.gitsync.common.scmerrorhandling;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.gitsync.common.beans.ScmApis;
import io.harness.gitsync.common.dtos.RepoProviders;
import io.harness.gitsync.common.helper.RepoProviderHelper;
import io.harness.gitsync.common.scmerrorhandling.handlers.DefaultScmApiErrorHandler;
import io.harness.gitsync.common.scmerrorhandling.handlers.ScmApiErrorHandler;

import com.google.common.annotations.VisibleForTesting;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
@OwnedBy(PL)
public class ScmApiErrorHandlingHelper {
  public void processAndThrowError(ScmApis scmAPI, ConnectorType connectorType, int statusCode, String errorMessage) {
    if (statusCode < 300) {
      return;
    }
    ScmApiErrorHandler scmAPIErrorHandler = getScmAPIErrorHandler(scmAPI, connectorType);
    scmAPIErrorHandler.handleError(statusCode, errorMessage);
  }

  @VisibleForTesting
  protected ScmApiErrorHandler getScmAPIErrorHandler(ScmApis scmApi, ConnectorType connectorType) {
    RepoProviders repoProvider = RepoProviderHelper.getRepoProviderFromConnectorType(connectorType);
    ScmApiErrorHandler scmApiErrorHandler = ScmApiErrorHandlerFactory.getHandler(scmApi, repoProvider);
    if (scmApiErrorHandler == null) {
      log.error(String.format("No scm API handler registered for API: %s, providerType: %s, connectorType: %s",
          scmApi.toString(), repoProvider, connectorType));
      return new DefaultScmApiErrorHandler();
    }
    return scmApiErrorHandler;
  }
}
