/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.idp.utils;

import static io.harness.beans.serializer.RunTimeInputHandler.resolveJsonNodeMapParameter;
import static io.harness.beans.serializer.RunTimeInputHandler.resolveStringParameter;
import static io.harness.beans.serializer.RunTimeInputHandler.resolveStringParameterV2;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.idp.Constants.BRANCH;
import static io.harness.idp.Constants.CODE_DIRECTORY;
import static io.harness.idp.Constants.CODE_OUTPUT_DIRECTORY;
import static io.harness.idp.Constants.CONNECTOR_TYPE;
import static io.harness.idp.Constants.DEFAULT_BRANCH_FOR_REPO;
import static io.harness.idp.Constants.DESCRIPTION_FOR_CREATING_REPO;
import static io.harness.idp.Constants.FILE_CONTENT;
import static io.harness.idp.Constants.FILE_NAME;
import static io.harness.idp.Constants.FILE_PATH;
import static io.harness.idp.Constants.MESSAGE_CONTENT;
import static io.harness.idp.Constants.ORG_NAME;
import static io.harness.idp.Constants.OUTPUT_DIRECTORY_COOKIE_CUTTER;
import static io.harness.idp.Constants.PATH_FOR_TEMPLATE;
import static io.harness.idp.Constants.PREFIX_FOR_COOKIECUTTER_ENV_VARIABLES;
import static io.harness.idp.Constants.PROJECT_NAME;
import static io.harness.idp.Constants.PUBLIC_TEMPLATE_URL;
import static io.harness.idp.Constants.REPO_NAME;
import static io.harness.idp.Constants.REPO_TYPE;
import static io.harness.idp.Constants.SLACK_ID;
import static io.harness.idp.Constants.SLACK_TOKEN;
import static io.harness.idp.Constants.TEMPLATE_TYPE;
import static io.harness.idp.Constants.WORKSPACE_NAME;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ci.execution.serializer.SerializerUtils;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretRefHelper;
import io.harness.idp.steps.Constants;
import io.harness.idp.steps.beans.stepinfo.IdpCookieCutterStepInfo;
import io.harness.idp.steps.beans.stepinfo.IdpCreateCatalogStepInfo;
import io.harness.idp.steps.beans.stepinfo.IdpCreateRepoStepInfo;
import io.harness.idp.steps.beans.stepinfo.IdpDirectPushStepInfo;
import io.harness.idp.steps.beans.stepinfo.IdpRegisterCatalogStepInfo;
import io.harness.idp.steps.beans.stepinfo.IdpSlackNotifyStepInfo;
import io.harness.plugin.service.PluginServiceImpl;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.variables.NGVariableType;
import io.harness.yaml.core.variables.SecretNGVariable;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@OwnedBy(HarnessTeam.IDP)
public class IDPStepUtils extends PluginServiceImpl {
  public Map<String, String> getCookieCutterStepInfoEnvVariables(IdpCookieCutterStepInfo stepInfo, String identifier) {
    Map<String, String> envVarMap = new HashMap<>();

    Map<String, JsonNode> cookieCutterVariables = resolveJsonNodeMapParameter(
        "cookieCutterVariables", Constants.COOKIECUTTER, identifier, stepInfo.getCookieCutterVariables(), false);

    if (!isEmpty(cookieCutterVariables)) {
      for (Map.Entry<String, JsonNode> entry : cookieCutterVariables.entrySet()) {
        envVarMap.put(PREFIX_FOR_COOKIECUTTER_ENV_VARIABLES + entry.getKey(),
            SerializerUtils.convertJsonNodeToString(entry.getKey(), entry.getValue()));
      }
    }

    PluginServiceImpl.setMandatoryEnvironmentVariable(envVarMap, TEMPLATE_TYPE,
        resolveStringParameterV2(
            "isPublicTemplate", Constants.COOKIECUTTER, identifier, stepInfo.getTemplateType(), true));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, PUBLIC_TEMPLATE_URL,
        resolveStringParameterV2(
            "publicTemplateUrl", Constants.COOKIECUTTER, identifier, stepInfo.getPublicTemplateUrl(), false));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, PATH_FOR_TEMPLATE,
        resolveStringParameterV2(
            "pathForTemplate", Constants.COOKIECUTTER, identifier, stepInfo.getPathForTemplate(), false));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, OUTPUT_DIRECTORY_COOKIE_CUTTER,
        resolveStringParameterV2(
            "outputDirectory", Constants.COOKIECUTTER, identifier, stepInfo.getOutputDirectory(), false));

    envVarMap.values().removeAll(Collections.singleton(null));
    return envVarMap;
  }

  public Map<String, String> getCreateRepoStepInfoEnvVariables(
      IdpCreateRepoStepInfo stepInfo, ConnectorDetails gitConnector, String identifier) {
    Map<String, String> envVarMap = new HashMap<>();

    envVarMap.putAll(getGitEnvVars(gitConnector, stepInfo.getRepository().getValue()));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, ORG_NAME,
        resolveStringParameterV2("organization", Constants.CREATE_REPO, identifier, stepInfo.getOrganization(), false));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, DEFAULT_BRANCH_FOR_REPO,
        resolveStringParameterV2(
            "defaultBranch", Constants.CREATE_REPO, identifier, stepInfo.getDefaultBranch(), false));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, REPO_NAME,
        resolveStringParameterV2("repository", Constants.CREATE_REPO, identifier, stepInfo.getRepository(), false));

    PluginServiceImpl.setMandatoryEnvironmentVariable(envVarMap, REPO_TYPE,
        resolveStringParameterV2("RepoType", Constants.CREATE_REPO, identifier, stepInfo.getRepoType(), true));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, DESCRIPTION_FOR_CREATING_REPO,
        resolveStringParameterV2("description", Constants.CREATE_REPO, identifier, stepInfo.getDescription(), false));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, PROJECT_NAME,
        resolveStringParameterV2("project", Constants.CREATE_REPO, identifier, stepInfo.getProject(), false));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, WORKSPACE_NAME,
        resolveStringParameterV2("workspace", Constants.CREATE_REPO, identifier, stepInfo.getWorkspace(), false));

    envVarMap.put(CONNECTOR_TYPE, gitConnector.getConnectorType().getDisplayName());

    envVarMap.values().removeAll(Collections.singleton(null));
    return envVarMap;
  }

  public Map<String, String> getDirectPushStepInfoEnvVariables(
      IdpDirectPushStepInfo stepInfo, ConnectorDetails gitConnector, String identifier) {
    Map<String, String> envVarMap = new HashMap<>();

    envVarMap.putAll(getGitEnvVars(gitConnector, stepInfo.getRepository().getValue()));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, REPO_NAME,
        resolveStringParameterV2("repository", Constants.DIRECT_PUSH, identifier, stepInfo.getRepository(), false));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, ORG_NAME,
        resolveStringParameterV2("organization", Constants.DIRECT_PUSH, identifier, stepInfo.getOrganization(), false));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, PROJECT_NAME,
        resolveStringParameterV2("project", Constants.DIRECT_PUSH, identifier, stepInfo.getProject(), false));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, WORKSPACE_NAME,
        resolveStringParameterV2("workspace", Constants.DIRECT_PUSH, identifier, stepInfo.getWorkspace(), false));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, CODE_DIRECTORY,
        resolveStringParameterV2(
            "codeDirectory", Constants.DIRECT_PUSH, identifier, stepInfo.getCodeDirectory(), true));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, CODE_OUTPUT_DIRECTORY,
        resolveStringParameterV2(
            "codeOutputDirectory", Constants.DIRECT_PUSH, identifier, stepInfo.getCodeOutputDirectory(), false));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, BRANCH,
        resolveStringParameterV2("branch", Constants.DIRECT_PUSH, identifier, stepInfo.getBranch(), true));

    envVarMap.put(CONNECTOR_TYPE, gitConnector.getConnectorType().getDisplayName());

    envVarMap.values().removeAll(Collections.singleton(null));
    return envVarMap;
  }

  public Map<String, String> getRegisterCatalogStepInfoEnvVariables(
      IdpRegisterCatalogStepInfo stepInfo, ConnectorDetails gitConnector, String identifier) {
    Map<String, String> envVarMap = new HashMap<>();

    envVarMap.putAll(getGitEnvVars(gitConnector, stepInfo.getRepository().getValue()));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, FILE_PATH,
        resolveStringParameterV2("filePath", Constants.REGISTER_CATALOG, identifier, stepInfo.getFilePath(), false));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, ORG_NAME,
        resolveStringParameterV2(
            "organization", Constants.REGISTER_CATALOG, identifier, stepInfo.getOrganization(), false));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, BRANCH,
        resolveStringParameterV2("branch", Constants.REGISTER_CATALOG, identifier, stepInfo.getBranch(), false));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, REPO_NAME,
        resolveStringParameterV2(
            "repository", Constants.REGISTER_CATALOG, identifier, stepInfo.getRepository(), false));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, PROJECT_NAME,
        resolveStringParameterV2("project", Constants.REGISTER_CATALOG, identifier, stepInfo.getProject(), false));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, WORKSPACE_NAME,
        resolveStringParameterV2("workspace", Constants.REGISTER_CATALOG, identifier, stepInfo.getWorkspace(), false));

    envVarMap.put(CONNECTOR_TYPE, gitConnector.getConnectorType().getDisplayName());

    envVarMap.values().removeAll(Collections.singleton(null));
    return envVarMap;
  }

  public Map<String, String> getCreateCatalogStepInfoEnvVariables(
      IdpCreateCatalogStepInfo stepInfo, String identifier) {
    Map<String, String> envVarMap = new HashMap<>();

    PluginServiceImpl.setMandatoryEnvironmentVariable(envVarMap, FILE_NAME,
        resolveStringParameterV2("fileName", Constants.CREATE_CATALOG, identifier, stepInfo.getFileName(), true));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, FILE_PATH,
        resolveStringParameterV2("filePath", Constants.CREATE_CATALOG, identifier, stepInfo.getFilePath(), false));

    PluginServiceImpl.setMandatoryEnvironmentVariable(envVarMap, FILE_CONTENT,
        resolveStringParameterV2("fileContent", Constants.CREATE_CATALOG, identifier, stepInfo.getFileContent(), true));

    return envVarMap;
  }

  public Map<String, String> getSlackNotifyStepInfoEnvVariables(IdpSlackNotifyStepInfo stepInfo, String identifier) {
    Map<String, String> envVarMap = new HashMap<>();

    PluginServiceImpl.setMandatoryEnvironmentVariable(envVarMap, SLACK_ID,
        resolveStringParameterV2("channelId", Constants.SLACK_NOTIFY, identifier, stepInfo.getSlackId(), true));

    PluginServiceImpl.setMandatoryEnvironmentVariable(envVarMap, MESSAGE_CONTENT,
        resolveStringParameterV2(
            "messageContent", Constants.CREATE_CATALOG, identifier, stepInfo.getMessageContent(), true));

    return envVarMap;
  }

  public Map<String, SecretNGVariable> getSlackNotifyStepInfoSecretVariables(
      IdpSlackNotifyStepInfo stepInfo, String identifier) {
    Map<String, SecretNGVariable> secretNGVariableMap = new HashMap<>();

    String token = resolveStringParameter("token", Constants.SLACK_NOTIFY, identifier, stepInfo.getToken(), true);
    SecretRefData secretRefData = SecretRefHelper.createSecretRef(token);
    secretNGVariableMap.put(SLACK_TOKEN,
        SecretNGVariable.builder()
            .type(NGVariableType.SECRET)
            .value(ParameterField.createValueField(secretRefData))
            .name(SLACK_TOKEN)
            .build());
    return secretNGVariableMap;
  }
}
