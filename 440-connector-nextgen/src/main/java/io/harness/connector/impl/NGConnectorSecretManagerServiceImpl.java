/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.impl;

import static io.harness.ConnectorConstants.CONNECTOR_DECORATOR_SERVICE;
import static io.harness.NGConstants.HARNESS_SECRET_MANAGER_IDENTIFIER;
import static io.harness.NGConstants.HARNESS_SECRET_MANAGER_NAME;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.beans.connector.ConnectorType.CUSTOM_SECRET_MANAGER;
import static io.harness.helpers.GlobalSecretManagerUtils.GLOBAL_ACCOUNT_ID;
import static io.harness.security.encryption.EncryptionType.CUSTOM_NG;
import static io.harness.security.encryption.EncryptionType.LOCAL;
import static io.harness.utils.IdentifierRefHelper.IDENTIFIER_REF_DELIMITER;

import static java.util.Objects.isNull;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.beans.IdentifierRef;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.helper.CustomSecretManagerHelper;
import io.harness.connector.helper.HarnessManagedConnectorHelper;
import io.harness.connector.services.ConnectorService;
import io.harness.connector.services.NGConnectorSecretManagerService;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretRefHelper;
import io.harness.encryptors.CustomEncryptor;
import io.harness.encryptors.CustomEncryptorsRegistry;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.interceptor.GitSyncBranchContext;
import io.harness.manage.GlobalContextManager;
import io.harness.ng.SecretManagerConfigDTOMapper;
import io.harness.ng.core.api.SecretCrudService;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.dto.secrets.SecretFileSpecDTO;
import io.harness.ng.core.dto.secrets.SecretResponseWrapper;
import io.harness.ng.core.dto.secrets.SecretTextSpecDTO;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.dto.CustomSecretManagerConfigDTO;
import io.harness.secretmanagerclient.dto.LocalConfigDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.security.encryption.EncryptedDataParams;
import io.harness.utils.IdentifierRefHelper;

import software.wings.beans.CustomSecretNGManagerConfig;
import software.wings.beans.NameValuePairWithDefault;
import software.wings.service.impl.security.NGEncryptorService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
@Singleton
public class NGConnectorSecretManagerServiceImpl implements NGConnectorSecretManagerService {
  private final ConnectorService connectorService;
  private final NGEncryptorService ngEncryptorService;
  private final HarnessManagedConnectorHelper harnessManagedConnectorHelper;
  private final CustomSecretManagerHelper customSecretManagerHelper;
  private final CustomEncryptorsRegistry customEncryptorsRegistry;
  private final SecretCrudService ngSecretService;

  @Inject
  public NGConnectorSecretManagerServiceImpl(@Named(CONNECTOR_DECORATOR_SERVICE) ConnectorService connectorService,
      NGEncryptorService ngEncryptorService, HarnessManagedConnectorHelper harnessManagedConnectorHelper,
      CustomSecretManagerHelper customSecretManagerHelper, CustomEncryptorsRegistry customEncryptorsRegistry,
      SecretCrudService ngSecretService) {
    this.connectorService = connectorService;
    this.ngEncryptorService = ngEncryptorService;
    this.harnessManagedConnectorHelper = harnessManagedConnectorHelper;
    this.customSecretManagerHelper = customSecretManagerHelper;
    this.customEncryptorsRegistry = customEncryptorsRegistry;
    this.ngSecretService = ngSecretService;
  }

  @Override
  public SecretManagerConfigDTO getUsingIdentifier(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String identifier, boolean maskSecrets) {
    // abstract scope from secret identifier
    String[] identifierRefStringSplit = identifier.split(IDENTIFIER_REF_DELIMITER);

    if (identifierRefStringSplit.length > 1) {
      IdentifierRef connectorRef = IdentifierRefHelper.getConnectorIdentifierRef(
          identifier, accountIdentifier, orgIdentifier, projectIdentifier);
      accountIdentifier = connectorRef.getAccountIdentifier();
      orgIdentifier = connectorRef.getOrgIdentifier();
      projectIdentifier = connectorRef.getProjectIdentifier();
      identifier = connectorRef.getIdentifier();
    }
    ConnectorDTO connectorDTO = getConnectorDTO(accountIdentifier, orgIdentifier, projectIdentifier, identifier);

    if (!maskSecrets) {
      connectorDTO = decrypt(accountIdentifier, orgIdentifier, projectIdentifier, connectorDTO);
    }

    return SecretManagerConfigDTOMapper.fromConnectorDTO(
        accountIdentifier, connectorDTO, connectorDTO.getConnectorInfo().getConnectorConfig());
  }

  @Override
  public ConnectorDTO decrypt(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, ConnectorDTO connectorDTO) {
    if (HARNESS_SECRET_MANAGER_IDENTIFIER.equals(connectorDTO.getConnectorInfo().getIdentifier())
        || harnessManagedConnectorHelper.isHarnessManagedSecretManager(connectorDTO.getConnectorInfo())) {
      projectIdentifier = null;
      orgIdentifier = null;
      accountIdentifier = GLOBAL_ACCOUNT_ID;
      connectorDTO =
          getConnectorDTO(accountIdentifier, orgIdentifier, projectIdentifier, HARNESS_SECRET_MANAGER_IDENTIFIER);
    }

    if (CUSTOM_SECRET_MANAGER.equals(connectorDTO.getConnectorInfo().getConnectorType())) {
      checkIfDecryptionIsPossible(accountIdentifier, connectorDTO.getConnectorInfo(), false);
      Set<String> secretIdentifiers = customSecretManagerHelper.extractSecretsUsed(accountIdentifier, connectorDTO);
      validateSecretManagerCredentialsAreInHarnessSM(accountIdentifier, connectorDTO, secretIdentifiers, false);
    }

    ngEncryptorService.decryptEncryptionConfigSecrets(connectorDTO.getConnectorInfo().getConnectorConfig(),
        accountIdentifier, projectIdentifier, orgIdentifier, false);
    return connectorDTO;
  }

  @Override
  public ConnectorDTO getConnectorDTO(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    Optional<ConnectorResponseDTO> connectorResponseDTO = Optional.empty();
    final GitEntityInfo emptyInfo = GitEntityInfo.builder().build();
    final GitSyncBranchContext gitSyncBranchContext =
        GlobalContextManager.get(GitSyncBranchContext.NG_GIT_SYNC_CONTEXT);
    try (GlobalContextManager.GlobalContextGuard guard = GlobalContextManager.ensureGlobalContextGuard()) {
      GlobalContextManager.upsertGlobalContextRecord(GitSyncBranchContext.builder().gitBranchInfo(emptyInfo).build());
      connectorResponseDTO = connectorService.get(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
      if (!connectorResponseDTO.isPresent()) {
        throw new NotFoundException(String.format("Connector with identifier [%s] in project [%s], org [%s] not found",
            identifier, projectIdentifier, orgIdentifier));
      }
    } finally {
      if (gitSyncBranchContext != null) {
        GlobalContextManager.upsertGlobalContextRecord(gitSyncBranchContext);
      }
    }

    ConnectorInfoDTO connectorInfoDTO = connectorResponseDTO.get().getConnector();
    return ConnectorDTO.builder().connectorInfo(connectorInfoDTO).build();
  }

  @Override
  public void resolveSecretManagerScriptSecrets(String accountIdentifier, String path,
      CustomSecretNGManagerConfig encryptionConfig, SecretManagerConfigDTO secretManagerConfigDTO) {
    // use this path to replace secret var input
    CustomSecretManagerConfigDTO customNGSecretManagerConfigDTO = (CustomSecretManagerConfigDTO) secretManagerConfigDTO;
    Map<String, List<NameValuePairWithDefault>> inputValues = customSecretManagerHelper.mergeStringInInputValues(
        path, customNGSecretManagerConfigDTO.getTemplate().getTemplateInputs());
    customNGSecretManagerConfigDTO.getTemplate().setTemplateInputs(inputValues);
    // Preparing encrypted data for custom secret manager.
    Set<EncryptedDataParams> encryptedDataParamsSet =
        customSecretManagerHelper.prepareEncryptedDataParamsSet(customNGSecretManagerConfigDTO);
    encryptionConfig.setEncryptionType(CUSTOM_NG);
    CustomEncryptor customEncryptor = customEncryptorsRegistry.getCustomEncryptor(CUSTOM_NG);
    String script =
        customEncryptor.resolveSecretManagerConfig(accountIdentifier, encryptedDataParamsSet, encryptionConfig);
    encryptionConfig.setScript(script);
  }

  public String getPerpetualTaskId(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    return connectorService.getHeartbeatPerpetualTaskId(
        accountIdentifier, orgIdentifier, projectIdentifier, identifier);
  }

  public void resetHeartBeatTask(String accountId, String taskId) {
    connectorService.resetHeartBeatTask(accountId, taskId);
  }

  @Override
  public SecretManagerConfigDTO getLocalConfigDTO(String accountIdentifier) {
    return LocalConfigDTO.builder()
        .encryptionType(LOCAL)
        .identifier(HARNESS_SECRET_MANAGER_IDENTIFIER)
        .harnessManaged(true)
        .accountIdentifier(accountIdentifier)
        .isDefault(false)
        .build();
  }

  @Override
  public Map<String, SecretRefData> getSecretsForDecryptableEntities(List<DecryptableEntity> decryptableEntities) {
    if (isEmpty(decryptableEntities)) {
      return new HashMap<>();
    }
    return SecretRefHelper.getDecryptableFieldsData(decryptableEntities);
  }

  @Override
  public Optional<SecretResponseWrapper> getSecretOptionalFromSecretRef(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, SecretRefData secretRefData) {
    if (secretRefData == null) {
      return Optional.empty();
    }
    Scope scope = secretRefData.getScope();
    IdentifierRef secretIdentifierRef = IdentifierRefHelper.getIdentifierRef(
        scope, secretRefData.getIdentifier(), accountIdentifier, orgIdentifier, projectIdentifier, null);

    return ngSecretService.get(accountIdentifier, secretIdentifierRef.getOrgIdentifier(),
        secretIdentifierRef.getProjectIdentifier(), secretRefData.getIdentifier());
  }

  private void throwIfNotHarnessSM(String secretManagerIdentifier, String secretIdentifier) {
    Set<String> harnessSecretManagers = Set.of(
        Scope.ACCOUNT.getYamlRepresentation() + "." + HARNESS_SECRET_MANAGER_IDENTIFIER,
        Scope.ORG.getYamlRepresentation() + "." + HARNESS_SECRET_MANAGER_IDENTIFIER, HARNESS_SECRET_MANAGER_IDENTIFIER);
    if (!harnessSecretManagers.contains(secretManagerIdentifier)) {
      throw new InvalidRequestException(String.format(
          "Secret [%s] is stored in secret manager [%s]. Secret manager credentials should be stored in [%s]",
          secretIdentifier, secretManagerIdentifier, HARNESS_SECRET_MANAGER_NAME));
    }
  }

  @Override
  public void checkIfDecryptionIsPossible(
      String accountIdentifier, ConnectorInfoDTO connectorInfoDTO, boolean validateSMCredentialsStoredInHarnessSM) {
    Map<String, SecretRefData> secrets =
        getSecretsForDecryptableEntities(connectorInfoDTO.getConnectorConfig().getDecryptableEntities());
    if (isEmpty(secrets)) {
      return;
    }

    secrets.forEach((key, secretRefData) -> {
      if (isNull(secretRefData) || isEmpty(secretRefData.getIdentifier())) {
        return;
      }

      Optional<SecretResponseWrapper> secretResponseWrapperOptional = getSecretOptionalFromSecretRef(accountIdentifier,
          connectorInfoDTO.getOrgIdentifier(), connectorInfoDTO.getProjectIdentifier(), secretRefData);

      if (secretResponseWrapperOptional.isPresent()) {
        String secretManagerIdentifier = null;
        SecretDTOV2 secretDTO = secretResponseWrapperOptional.get().getSecret();
        if (SecretType.SecretText.equals(secretDTO.getType())) {
          secretManagerIdentifier = ((SecretTextSpecDTO) secretDTO.getSpec()).getSecretManagerIdentifier();
        } else if (SecretType.SecretFile.equals(secretDTO.getType())) {
          secretManagerIdentifier = ((SecretFileSpecDTO) secretDTO.getSpec()).getSecretManagerIdentifier();
        } else if (SecretType.SSHKey.equals(secretDTO.getType())) {
          Optional<List<DecryptableEntity>> sshKeyDecryptableEntitiesOptional =
              secretDTO.getSpec().getDecryptableEntities();
          if (sshKeyDecryptableEntitiesOptional.isPresent()) {
            validateSSHKeySecretRefsAreFromHarnessSM(sshKeyDecryptableEntitiesOptional.get(),
                IdentifierRef.builder()
                    .accountIdentifier(accountIdentifier)
                    .orgIdentifier(connectorInfoDTO.getOrgIdentifier())
                    .projectIdentifier(connectorInfoDTO.getProjectIdentifier())
                    .identifier(connectorInfoDTO.getIdentifier())
                    .build(),
                validateSMCredentialsStoredInHarnessSM);
            return;
          }
        } // TODO- add handling for WinRM creds

        IdentifierRef connectorIdentifierRef = IdentifierRef.builder()
                                                   .accountIdentifier(accountIdentifier)
                                                   .orgIdentifier(connectorInfoDTO.getOrgIdentifier())
                                                   .projectIdentifier(connectorInfoDTO.getProjectIdentifier())
                                                   .identifier(connectorInfoDTO.getIdentifier())
                                                   .build();
        IdentifierRef secretIdentifierRef = IdentifierRef.builder()
                                                .accountIdentifier(accountIdentifier)
                                                .orgIdentifier(secretDTO.getOrgIdentifier())
                                                .projectIdentifier(secretDTO.getProjectIdentifier())
                                                .identifier(secretDTO.getIdentifier())
                                                .build();
        throwIfDecryptionNotPossible(secretManagerIdentifier, secretRefData, connectorIdentifierRef,
            secretIdentifierRef, validateSMCredentialsStoredInHarnessSM);
      }
    });
  }

  private void validateSSHKeySecretRefsAreFromHarnessSM(List<DecryptableEntity> decryptableEntities,
      IdentifierRef identifierRef, boolean validateSMCredentialsStoredInHarnessSM) {
    Map<String, SecretRefData> secrets = getSecretsForDecryptableEntities(decryptableEntities);
    if (isEmpty(secrets)) {
      return;
    }

    secrets.forEach((key, secretRefData) -> {
      if (isNull(secretRefData) || isEmpty(secretRefData.getIdentifier())) {
        return;
      }

      Optional<SecretResponseWrapper> secretResponseWrapperOptional =
          getSecretOptionalFromSecretRef(identifierRef.getAccountIdentifier(), identifierRef.getOrgIdentifier(),
              identifierRef.getProjectIdentifier(), secretRefData);

      String secretManagerIdentifier = null;
      if (secretResponseWrapperOptional.isPresent()) {
        SecretDTOV2 secretDTO = secretResponseWrapperOptional.get().getSecret();
        if (SecretType.SecretText.equals(secretDTO.getType())) {
          secretManagerIdentifier = ((SecretTextSpecDTO) secretDTO.getSpec()).getSecretManagerIdentifier();
        } else if (SecretType.SecretFile.equals(secretDTO.getType())) {
          secretManagerIdentifier = ((SecretFileSpecDTO) secretDTO.getSpec()).getSecretManagerIdentifier();
        }

        IdentifierRef connectorIdentifierRef = IdentifierRef.builder()
                                                   .accountIdentifier(identifierRef.getAccountIdentifier())
                                                   .orgIdentifier(identifierRef.getOrgIdentifier())
                                                   .projectIdentifier(identifierRef.getProjectIdentifier())
                                                   .identifier(identifierRef.getIdentifier())
                                                   .build();
        IdentifierRef secretIdentifierRef = IdentifierRef.builder()
                                                .accountIdentifier(identifierRef.getAccountIdentifier())
                                                .orgIdentifier(secretDTO.getOrgIdentifier())
                                                .projectIdentifier(secretDTO.getProjectIdentifier())
                                                .identifier(secretDTO.getIdentifier())
                                                .build();
        throwIfDecryptionNotPossible(secretManagerIdentifier, secretRefData, connectorIdentifierRef,
            secretIdentifierRef, validateSMCredentialsStoredInHarnessSM);
      }
    });
  }

  public void validateSecretManagerCredentialsAreInHarnessSM(String accountIdentifier, ConnectorDTO connectorDTO,
      Set<String> credentialSecretIdentifiers, boolean validateSMCredentialsStoredInHarnessSM) {
    ConnectorInfoDTO connectorInfoDTO = connectorDTO.getConnectorInfo();
    if (isEmpty(credentialSecretIdentifiers)) {
      return;
    }
    List<IdentifierRef> secretRefs =
        credentialSecretIdentifiers.stream()
            .map(scopedIdentifier
                -> IdentifierRefHelper.getIdentifierRef(scopedIdentifier, accountIdentifier,
                    connectorInfoDTO.getOrgIdentifier(), connectorInfoDTO.getProjectIdentifier()))
            .toList();
    secretRefs.forEach(secretRef -> {
      Optional<SecretResponseWrapper> secret = ngSecretService.get(secretRef.getAccountIdentifier(),
          secretRef.getOrgIdentifier(), secretRef.getProjectIdentifier(), secretRef.getIdentifier());
      if (secret.isEmpty()) {
        return;
      }
      String secretManagerIdentifierFromSecret =
          getSecretManagerIdentifierFromSecret(accountIdentifier, secret.get().getSecret());
      if (validateSMCredentialsStoredInHarnessSM
          && !HARNESS_SECRET_MANAGER_IDENTIFIER.equals(secretManagerIdentifierFromSecret)) {
        throw new InvalidRequestException(String.format(
            "Secret [%s] specified in template is stored in secret manager [%s]. Secrets used in the template should be stored in [%s]",
            secretRef.getIdentifier(), secretManagerIdentifierFromSecret, HARNESS_SECRET_MANAGER_NAME));
      }

      String connectorScopedIdentifier =
          IdentifierRefHelper.getRefFromIdentifierOrRef(accountIdentifier, connectorInfoDTO.getOrgIdentifier(),
              connectorInfoDTO.getProjectIdentifier(), connectorInfoDTO.getIdentifier());

      String secretManagerScopedIdentifier =
          getScopedSecretManagerIdentifierFromSecret(accountIdentifier, secret.get().getSecret());

      if (connectorScopedIdentifier.equals(secretManagerScopedIdentifier)) {
        throw new InvalidRequestException(String.format(
            "Detected cyclic dependency in credentials of secret manager- [%s]", secretManagerIdentifierFromSecret));
      }
    });
  }

  private String getSecretManagerIdentifierFromSecret(String accountIdentifier, SecretDTOV2 secretDTO) {
    String scopedIdentifier = getScopedSecretManagerIdentifierFromSecret(accountIdentifier, secretDTO);
    if (scopedIdentifier != null) {
      return IdentifierRefHelper.getIdentifier(scopedIdentifier);
    }
    return null;
  }

  private String getScopedSecretManagerIdentifierFromSecret(String accountIdentifier, SecretDTOV2 secretDTO) {
    String identifier;
    switch (secretDTO.getType()) {
      case SecretText:
        identifier = ((SecretTextSpecDTO) secretDTO.getSpec()).getSecretManagerIdentifier();
        break;
      case SecretFile:
        identifier = ((SecretFileSpecDTO) secretDTO.getSpec()).getSecretManagerIdentifier();
        break;
      default:
        return null;
    }
    if (identifier != null) {
      return IdentifierRefHelper.getRefFromIdentifierOrRef(
          accountIdentifier, secretDTO.getOrgIdentifier(), secretDTO.getProjectIdentifier(), identifier);
    }
    return null;
  }

  private void throwIfDecryptionNotPossible(String secretManagerIdentifier, SecretRefData secretRefData,
      IdentifierRef connectorIdentifierRef, IdentifierRef secretIdentifierRef,
      boolean validateSMCredentialsStoredInHarnessSM) {
    if (validateSMCredentialsStoredInHarnessSM) {
      throwIfNotHarnessSM(secretManagerIdentifier, secretRefData.getIdentifier());
    }

    String scopedConnectorIdentifier = IdentifierRefHelper.getRefFromIdentifierOrRef(
        connectorIdentifierRef.getAccountIdentifier(), connectorIdentifierRef.getOrgIdentifier(),
        connectorIdentifierRef.getProjectIdentifier(), connectorIdentifierRef.getIdentifier());

    String scopedSecretManagerIdentifier = IdentifierRefHelper.getRefFromIdentifierOrRef(
        secretIdentifierRef.getAccountIdentifier(), secretIdentifierRef.getOrgIdentifier(),
        secretIdentifierRef.getProjectIdentifier(), secretManagerIdentifier);

    if (scopedSecretManagerIdentifier.equals(scopedConnectorIdentifier)) {
      throw new InvalidRequestException(
          String.format("Detected cyclic dependency in credentials of secret manager- [%s]", secretManagerIdentifier));
    }
  }
}
