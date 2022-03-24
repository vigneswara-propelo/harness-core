package io.harness.gitsync.common.helper;

import static io.harness.utils.FilePathUtils.addStartingSlashIfMissing;

import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnsupportedOperationException;
import io.harness.gitsync.common.dtos.RepoProviders;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class RepoProviderHelper {
  public static String getTheFilePathUrl(
      String repoUrl, String branch, RepoProviders repoProviders, String completeFilePath) {
    if (repoProviders == null) {
      throw new InvalidRequestException("Repo provider should not be null when finding the entity file path");
    }
    String completeFilePathWithSlash = addStartingSlashIfMissing(completeFilePath);
    switch (repoProviders) {
      case GITHUB:
        return String.format("%s/blob/%s%s", repoUrl, branch, completeFilePathWithSlash);
      case BITBUCKET:
        return String.format("%s/src/%s%s", repoUrl, branch, completeFilePathWithSlash);
      default:
        throw new UnsupportedOperationException(
            "The operation to get the file path url is not supported for " + repoProviders);
    }
  }

  public RepoProviders getRepoProviderFromConnectorType(ConnectorType connectorType) {
    switch (connectorType) {
      case GITHUB:
        return RepoProviders.GITHUB;
      case BITBUCKET:
        return RepoProviders.BITBUCKET;
      case GITLAB:
        return RepoProviders.GITLAB;
      default:
        throw new InvalidRequestException("Unknown connector type " + connectorType);
    }
  }

  public static RepoProviders getRepoProviderFromTheUrl(String repoUrl) {
    if (repoUrl.contains("bitbucket.org")) {
      return RepoProviders.BITBUCKET;
    }
    return RepoProviders.GITHUB;
  }
}
