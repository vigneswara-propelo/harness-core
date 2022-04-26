/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.mappers.azurerepoconnectormapper;

import static io.harness.delegate.beans.connector.scm.GitAuthType.HTTP;
import static io.harness.rule.OwnerRule.MANKRIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.azurerepoconnector.AzureRepoConnector;
import io.harness.connector.entities.embedded.azurerepoconnector.AzureRepoHttpAuthentication;
import io.harness.connector.entities.embedded.azurerepoconnector.AzureRepoTokenApiAccess;
import io.harness.connector.entities.embedded.azurerepoconnector.AzureRepoUsernameToken;
import io.harness.connector.mappers.azurerepomapper.AzureRepoEntityToDTO;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.azurerepo.AzureRepoApiAccessDTO;
import io.harness.delegate.beans.connector.scm.azurerepo.AzureRepoApiAccessType;
import io.harness.delegate.beans.connector.scm.azurerepo.AzureRepoAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.azurerepo.AzureRepoConnectorDTO;
import io.harness.delegate.beans.connector.scm.azurerepo.AzureRepoHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.azurerepo.AzureRepoHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.azurerepo.AzureRepoTokenSpecDTO;
import io.harness.delegate.beans.connector.scm.azurerepo.AzureRepoUsernameTokenDTO;
import io.harness.encryption.SecretRefHelper;
import io.harness.rule.Owner;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class AzureRepoEntityToDTOTest extends CategoryTest {
  @InjectMocks AzureRepoEntityToDTO azureRepoEntityToDTO;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = MANKRIT)
  @Category(UnitTests.class)
  public void testToConnectorEntity_0() throws IOException {
    final String url = "url";
    final String username = "username";
    final String tokenRef = "tokenRef";
    final String validationRepo = "validationRepo";

    final AzureRepoAuthenticationDTO azureRepoAuthenticationDTO =
        AzureRepoAuthenticationDTO.builder()
            .authType(HTTP)
            .credentials(AzureRepoHttpCredentialsDTO.builder()
                             .type(AzureRepoHttpAuthenticationType.USERNAME_AND_TOKEN)
                             .httpCredentialsSpec(AzureRepoUsernameTokenDTO.builder()
                                                      .username(username)
                                                      .tokenRef(SecretRefHelper.createSecretRef(tokenRef))
                                                      .build())
                             .build())
            .build();

    final AzureRepoApiAccessDTO azureRepoApiAccessDTO =
        AzureRepoApiAccessDTO.builder()
            .type(AzureRepoApiAccessType.TOKEN)
            .spec(AzureRepoTokenSpecDTO.builder().tokenRef(SecretRefHelper.createSecretRef(tokenRef)).build())
            .build();
    final AzureRepoConnectorDTO azureRepoConnectorDTO = AzureRepoConnectorDTO.builder()
                                                            .url(url)
                                                            .validationRepo(validationRepo)
                                                            .connectionType(GitConnectionType.ACCOUNT)
                                                            .authentication(azureRepoAuthenticationDTO)
                                                            .apiAccess(azureRepoApiAccessDTO)
                                                            .build();

    final AzureRepoConnector azureRepoConnector =
        AzureRepoConnector.builder()
            .hasApiAccess(true)
            .url(url)
            .validationRepo(validationRepo)
            .azureRepoApiAccess(AzureRepoTokenApiAccess.builder().tokenRef(tokenRef).build())
            .apiAccessType(AzureRepoApiAccessType.TOKEN)
            .connectionType(GitConnectionType.ACCOUNT)
            .authType(HTTP)
            .authenticationDetails(
                AzureRepoHttpAuthentication.builder()
                    .type(AzureRepoHttpAuthenticationType.USERNAME_AND_TOKEN)
                    .auth(AzureRepoUsernameToken.builder().username(username).tokenRef(tokenRef).build())
                    .build())
            .build();
    final AzureRepoConnectorDTO azureRepoConnectorDTO1 = azureRepoEntityToDTO.createConnectorDTO(azureRepoConnector);
    ObjectMapper objectMapper = new ObjectMapper();
    assertThat(objectMapper.readTree(objectMapper.writeValueAsString(azureRepoConnectorDTO1)))
        .isEqualTo(objectMapper.readTree(objectMapper.writeValueAsString(azureRepoConnectorDTO)));
  }
}