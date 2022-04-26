/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.mappers.azurerepoconnectormapper;

import static io.harness.delegate.beans.connector.scm.GitAuthType.HTTP;
import static io.harness.delegate.beans.connector.scm.azurerepo.AzureRepoApiAccessType.TOKEN;
import static io.harness.rule.OwnerRule.MANKRIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.azurerepoconnector.AzureRepoConnector;
import io.harness.connector.entities.embedded.azurerepoconnector.AzureRepoHttpAuthentication;
import io.harness.connector.entities.embedded.azurerepoconnector.AzureRepoSshAuthentication;
import io.harness.connector.entities.embedded.azurerepoconnector.AzureRepoTokenApiAccess;
import io.harness.connector.entities.embedded.azurerepoconnector.AzureRepoUsernameToken;
import io.harness.connector.mappers.azurerepomapper.AzureRepoDTOToEntity;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.azurerepo.AzureRepoApiAccessDTO;
import io.harness.delegate.beans.connector.scm.azurerepo.AzureRepoApiAccessType;
import io.harness.delegate.beans.connector.scm.azurerepo.AzureRepoAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.azurerepo.AzureRepoConnectorDTO;
import io.harness.delegate.beans.connector.scm.azurerepo.AzureRepoHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.azurerepo.AzureRepoHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.azurerepo.AzureRepoSshCredentialsDTO;
import io.harness.delegate.beans.connector.scm.azurerepo.AzureRepoTokenSpecDTO;
import io.harness.delegate.beans.connector.scm.azurerepo.AzureRepoUsernameTokenDTO;
import io.harness.encryption.SecretRefHelper;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class AzureRepoDTOToEntityTest extends CategoryTest {
  @InjectMocks AzureRepoDTOToEntity azureRepoDTOToEntity;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = MANKRIT)
  @Category(UnitTests.class)
  public void testToConnectorEntity_0() {
    final String url = "url";
    final String tokenRef = "tokenRef";
    final String validationRepo = "validationRepo";

    final AzureRepoAuthenticationDTO azureRepoAuthenticationDTO =
        AzureRepoAuthenticationDTO.builder()
            .authType(HTTP)
            .credentials(
                AzureRepoHttpCredentialsDTO.builder()
                    .type(AzureRepoHttpAuthenticationType.USERNAME_AND_TOKEN)
                    .httpCredentialsSpec(
                        AzureRepoUsernameTokenDTO.builder().tokenRef(SecretRefHelper.createSecretRef(tokenRef)).build())
                    .build())
            .build();

    final AzureRepoApiAccessDTO azureRepoApiAccessDTO =
        AzureRepoApiAccessDTO.builder()
            .type(TOKEN)
            .spec(AzureRepoTokenSpecDTO.builder().tokenRef(SecretRefHelper.createSecretRef(tokenRef)).build())
            .build();

    final AzureRepoConnectorDTO azureRepoConnectorDTO = AzureRepoConnectorDTO.builder()
                                                            .url(url)
                                                            .validationRepo(validationRepo)
                                                            .connectionType(GitConnectionType.ACCOUNT)
                                                            .authentication(azureRepoAuthenticationDTO)
                                                            .apiAccess(azureRepoApiAccessDTO)
                                                            .build();

    final AzureRepoConnector azureRepoConnector = azureRepoDTOToEntity.toConnectorEntity(azureRepoConnectorDTO);
    assertThat(azureRepoConnector).isNotNull();
    assertThat(azureRepoConnector.getUrl()).isEqualTo(url);
    assertThat(azureRepoConnector.getValidationRepo()).isEqualTo(validationRepo);
    assertThat(azureRepoConnector.getApiAccessType()).isEqualTo(TOKEN);
    assertThat(azureRepoConnector.getAuthType()).isEqualTo(HTTP);
    assertThat(azureRepoConnector.getAuthenticationDetails())
        .isEqualTo(AzureRepoHttpAuthentication.builder()
                       .type(AzureRepoHttpAuthenticationType.USERNAME_AND_TOKEN)
                       .auth(AzureRepoUsernameToken.builder().tokenRef(tokenRef).build())
                       .build());
    assertThat(azureRepoConnector.getAzureRepoApiAccess())
        .isEqualTo(AzureRepoTokenApiAccess.builder().tokenRef(tokenRef).build());
  }

  @Test
  @Owner(developers = MANKRIT)
  @Category(UnitTests.class)
  public void testToConnectorEntity_1() {
    final String url = "url";
    final String sshKeyRef = "sshKeyRef";
    final AzureRepoAuthenticationDTO azureRepoAuthenticationDTO =
        AzureRepoAuthenticationDTO.builder()
            .authType(GitAuthType.SSH)
            .credentials(
                AzureRepoSshCredentialsDTO.builder().sshKeyRef(SecretRefHelper.createSecretRef(sshKeyRef)).build())
            .build();

    final AzureRepoConnectorDTO azureRepoConnectorDTO = AzureRepoConnectorDTO.builder()
                                                            .url(url)
                                                            .connectionType(GitConnectionType.ACCOUNT)
                                                            .authentication(azureRepoAuthenticationDTO)
                                                            .build();

    final AzureRepoConnector azureRepoConnector = azureRepoDTOToEntity.toConnectorEntity(azureRepoConnectorDTO);
    assertThat(azureRepoConnector).isNotNull();
    assertThat(azureRepoConnector.getUrl()).isEqualTo(url);
    assertThat(azureRepoConnector.getAuthType()).isEqualTo(GitAuthType.SSH);
    assertThat(azureRepoConnector.getAuthenticationDetails())
        .isEqualTo(AzureRepoSshAuthentication.builder().sshKeyRef(sshKeyRef).build());
    assertThat(azureRepoConnector.getAzureRepoApiAccess()).isNull();
  }

  @Test
  @Owner(developers = MANKRIT)
  @Category(UnitTests.class)
  public void testToConnectorEntity_2() {
    final String url = "url";
    final String tokenRef = "tokenRef";
    final AzureRepoAuthenticationDTO azureRepoAuthenticationDTO =
        AzureRepoAuthenticationDTO.builder()
            .authType(HTTP)
            .credentials(
                AzureRepoHttpCredentialsDTO.builder()
                    .type(AzureRepoHttpAuthenticationType.USERNAME_AND_TOKEN)
                    .httpCredentialsSpec(
                        AzureRepoUsernameTokenDTO.builder().tokenRef(SecretRefHelper.createSecretRef(tokenRef)).build())
                    .build())
            .build();

    final AzureRepoApiAccessDTO azureRepoApiAccessDTO =
        AzureRepoApiAccessDTO.builder()
            .type(AzureRepoApiAccessType.TOKEN)
            .spec(AzureRepoTokenSpecDTO.builder().tokenRef(SecretRefHelper.createSecretRef(tokenRef)).build())
            .build();

    final AzureRepoConnectorDTO azureRepoConnectorDTO = AzureRepoConnectorDTO.builder()
                                                            .url(url)
                                                            .connectionType(GitConnectionType.REPO)
                                                            .authentication(azureRepoAuthenticationDTO)
                                                            .apiAccess(azureRepoApiAccessDTO)
                                                            .build();

    final AzureRepoConnector azureRepoConnector = azureRepoDTOToEntity.toConnectorEntity(azureRepoConnectorDTO);
    assertThat(azureRepoConnector).isNotNull();
    assertThat(azureRepoConnector.getUrl()).isEqualTo(url);
    assertThat(azureRepoConnector.getApiAccessType()).isEqualTo(TOKEN);
    assertThat(azureRepoConnector.getAuthType()).isEqualTo(HTTP);
    assertThat(azureRepoConnector.getAuthenticationDetails())
        .isEqualTo(AzureRepoHttpAuthentication.builder()
                       .type(AzureRepoHttpAuthenticationType.USERNAME_AND_TOKEN)
                       .auth(AzureRepoUsernameToken.builder().tokenRef(tokenRef).build())
                       .build());
    assertThat(azureRepoConnector.getAzureRepoApiAccess())
        .isEqualTo(AzureRepoTokenApiAccess.builder().tokenRef(tokenRef).build());
  }

  @Test
  @Owner(developers = MANKRIT)
  @Category(UnitTests.class)
  public void testToConnectorEntity_3() {
    final String url = "url";
    final String sshKeyRef = "sshKeyRef";
    final AzureRepoAuthenticationDTO azureRepoAuthenticationDTO =
        AzureRepoAuthenticationDTO.builder()
            .authType(GitAuthType.SSH)
            .credentials(
                AzureRepoSshCredentialsDTO.builder().sshKeyRef(SecretRefHelper.createSecretRef(sshKeyRef)).build())
            .build();

    final AzureRepoConnectorDTO azureRepoConnectorDTO = AzureRepoConnectorDTO.builder()
                                                            .url(url)
                                                            .connectionType(GitConnectionType.REPO)
                                                            .authentication(azureRepoAuthenticationDTO)
                                                            .build();

    final AzureRepoConnector azureRepoConnector = azureRepoDTOToEntity.toConnectorEntity(azureRepoConnectorDTO);
    assertThat(azureRepoConnector).isNotNull();
    assertThat(azureRepoConnector.getUrl()).isEqualTo(url);
    assertThat(azureRepoConnector.getAuthType()).isEqualTo(GitAuthType.SSH);
    assertThat(azureRepoConnector.getAuthenticationDetails())
        .isEqualTo(AzureRepoSshAuthentication.builder().sshKeyRef(sshKeyRef).build());
    assertThat(azureRepoConnector.getAzureRepoApiAccess()).isNull();
  }
}