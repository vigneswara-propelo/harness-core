/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.beans.connector.scm.adapter;

import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.azurerepo.AzureRepoConnectorDTO;
import io.harness.delegate.beans.connector.scm.azurerepo.AzureRepoSshCredentialsDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.exception.InvalidRequestException;

public class AzureRepoTOGitMapper {
  public static GitConfigDTO mapToGitConfigDTO(AzureRepoConnectorDTO azureRepoConnectorDTO) {
    GitAuthType authType = azureRepoConnectorDTO.getAuthentication().getAuthType();
    if (authType == GitAuthType.HTTP) {
      return mapToGitConfigDTOForHttpAuth(azureRepoConnectorDTO);
    } else if (authType == GitAuthType.SSH) {
      return mapToGitConfigDTOForSshAuth(azureRepoConnectorDTO);
    }
    throw new InvalidRequestException("Unknown auth type: " + authType);
  }

  private static GitConfigDTO mapToGitConfigDTOForHttpAuth(AzureRepoConnectorDTO azureRepoConnectorDTO) {
    final AzureRepoSshCredentialsDTO sshCredentials =
        (AzureRepoSshCredentialsDTO) azureRepoConnectorDTO.getAuthentication().getCredentials();

    return GitConfigCreater.getGitConfigForSsh(azureRepoConnectorDTO.getConnectionType(),
        azureRepoConnectorDTO.getUrl(), azureRepoConnectorDTO.getValidationRepo(), sshCredentials.getSshKeyRef(),
        azureRepoConnectorDTO.getDelegateSelectors());
  }

  private static GitConfigDTO mapToGitConfigDTOForSshAuth(AzureRepoConnectorDTO azureRepoConnectorDTO) {
    final AzureRepoSshCredentialsDTO sshCredentials =
        (AzureRepoSshCredentialsDTO) azureRepoConnectorDTO.getAuthentication().getCredentials();

    return GitConfigCreater.getGitConfigForSsh(azureRepoConnectorDTO.getConnectionType(),
        azureRepoConnectorDTO.getUrl(), azureRepoConnectorDTO.getValidationRepo(), sshCredentials.getSshKeyRef(),
        azureRepoConnectorDTO.getDelegateSelectors());
  }
}
