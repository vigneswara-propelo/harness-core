/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.idp.utils;

import static io.harness.beans.serializer.RunTimeInputHandler.resolveJsonNodeMapParameter;
import static io.harness.beans.serializer.RunTimeInputHandler.resolveStringParameterV2;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.idp.Constants.BRANCH;
import static io.harness.idp.Constants.CODE_DIRECTORY;
import static io.harness.idp.Constants.CODE_OUTPUT_DIRECTORY;
import static io.harness.idp.Constants.CONNECTOR_TYPE;
import static io.harness.idp.Constants.DEFAULT_BRANCH_FOR_REPO;
import static io.harness.idp.Constants.DESCRIPTION_FOR_CREATING_REPO;
import static io.harness.idp.Constants.FILE_PATH;
import static io.harness.idp.Constants.IS_PRIVATE_REPO;
import static io.harness.idp.Constants.IS_PUBLIC_TEMPLATE;
import static io.harness.idp.Constants.ORG_NAME;
import static io.harness.idp.Constants.OUTPUT_DIRECTORY_COOKIE_CUTTER;
import static io.harness.idp.Constants.PATH_FOR_TEMPLATE;
import static io.harness.idp.Constants.PREFIX_FOR_COOKIECUTTER_ENV_VARIABLES;
import static io.harness.idp.Constants.PUBLIC_TEMPLATE_URL;
import static io.harness.idp.Constants.REPO_NAME;
import static io.harness.idp.Constants.REPO_PROJECT;
import static io.harness.idp.Constants.REPO_WORKSPACE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ci.execution.serializer.SerializerUtils;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.idp.steps.Constants;
import io.harness.idp.steps.beans.stepinfo.IdpCodePushStepInfo;
import io.harness.idp.steps.beans.stepinfo.IdpCookieCutterStepInfo;
import io.harness.idp.steps.beans.stepinfo.IdpCreateRepoStepInfo;
import io.harness.idp.steps.beans.stepinfo.IdpRegisterCatalogStepInfo;
import io.harness.plugin.service.PluginServiceImpl;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@OwnedBy(HarnessTeam.IDP)
public class IDPStepUtils extends PluginServiceImpl {
  public Map<String, String> getIDPCookieCutterStepInfoEnvVariables(
      IdpCookieCutterStepInfo stepInfo, String identifier) {
    Map<String, String> envVarMap = new HashMap<>();

    Map<String, JsonNode> cookieCutterVariables = resolveJsonNodeMapParameter(
        "cookieCutterVariables", Constants.IDP_COOKIECUTTER, identifier, stepInfo.getCookieCutterVariables(), false);

    if (!isEmpty(cookieCutterVariables)) {
      for (Map.Entry<String, JsonNode> entry : cookieCutterVariables.entrySet()) {
        envVarMap.put(PREFIX_FOR_COOKIECUTTER_ENV_VARIABLES + entry.getKey(),
            SerializerUtils.convertJsonNodeToString(entry.getKey(), entry.getValue()));
      }
    }

    PluginServiceImpl.setMandatoryEnvironmentVariable(envVarMap, IS_PUBLIC_TEMPLATE,
        resolveStringParameterV2(
            "isPublicTemplate", Constants.IDP_COOKIECUTTER, identifier, stepInfo.getIsPublicTemplate(), true));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, PUBLIC_TEMPLATE_URL,
        resolveStringParameterV2(
            "publicTemplateUrl", Constants.IDP_COOKIECUTTER, identifier, stepInfo.getPublicTemplateUrl(), false));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, PATH_FOR_TEMPLATE,
        resolveStringParameterV2(
            "pathForTemplate", Constants.IDP_COOKIECUTTER, identifier, stepInfo.getPathForTemplate(), false));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, OUTPUT_DIRECTORY_COOKIE_CUTTER,
        resolveStringParameterV2(
            "outputDirectory", Constants.IDP_COOKIECUTTER, identifier, stepInfo.getOutputDirectory(), false));

    envVarMap.values().removeAll(Collections.singleton(null));
    return envVarMap;
  }

  public Map<String, String> getIDPCreateRepoStepInfoEnvVariables(
      IdpCreateRepoStepInfo stepInfo, ConnectorDetails gitConnector, String identifier) {
    Map<String, String> envVarMap = new HashMap<>();

    envVarMap.putAll(getGitEnvVars(gitConnector, stepInfo.getRepoName().getValue()));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, ORG_NAME,
        resolveStringParameterV2("orgName", Constants.IDP_CREATE_REPO, identifier, stepInfo.getOrgName(), false));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, DEFAULT_BRANCH_FOR_REPO,
        resolveStringParameterV2(
            "defaultBranch", Constants.IDP_CREATE_REPO, identifier, stepInfo.getDefaultBranch(), false));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, REPO_NAME,
        resolveStringParameterV2("repoName", Constants.IDP_CREATE_REPO, identifier, stepInfo.getRepoName(), false));

    PluginServiceImpl.setMandatoryEnvironmentVariable(envVarMap, IS_PRIVATE_REPO,
        resolveStringParameterV2(
            "isPrivateRepo", Constants.IDP_CREATE_REPO, identifier, stepInfo.getIsPrivateRepo(), true));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, DESCRIPTION_FOR_CREATING_REPO,
        resolveStringParameterV2(
            "description", Constants.IDP_CREATE_REPO, identifier, stepInfo.getDescription(), false));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, REPO_PROJECT,
        resolveStringParameterV2("project", Constants.IDP_CREATE_REPO, identifier, stepInfo.getProject(), false));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, REPO_WORKSPACE,
        resolveStringParameterV2("workspace", Constants.IDP_CREATE_REPO, identifier, stepInfo.getWorkspace(), false));

    envVarMap.put(CONNECTOR_TYPE, gitConnector.getConnectorType().getDisplayName());

    envVarMap.values().removeAll(Collections.singleton(null));
    return envVarMap;
  }

  public Map<String, String> getIDPCodePushStepInfoEnvVariables(
      IdpCodePushStepInfo stepInfo, ConnectorDetails gitConnector, String identifier) {
    Map<String, String> envVarMap = new HashMap<>();

    envVarMap.putAll(getGitEnvVars(gitConnector, stepInfo.getRepoName().getValue()));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, REPO_NAME,
        resolveStringParameterV2("repoName", Constants.IDP_CODE_PUSH, identifier, stepInfo.getRepoName(), false));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, ORG_NAME,
        resolveStringParameterV2("orgName", Constants.IDP_CODE_PUSH, identifier, stepInfo.getOrgName(), false));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, REPO_PROJECT,
        resolveStringParameterV2("project", Constants.IDP_CODE_PUSH, identifier, stepInfo.getProject(), false));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, REPO_WORKSPACE,
        resolveStringParameterV2("workspace", Constants.IDP_CODE_PUSH, identifier, stepInfo.getWorkspace(), false));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, CODE_DIRECTORY,
        resolveStringParameterV2(
            "codeDirectory", Constants.IDP_CODE_PUSH, identifier, stepInfo.getCodeDirectory(), true));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, CODE_OUTPUT_DIRECTORY,
        resolveStringParameterV2(
            "codeOutputDirectory", Constants.IDP_CODE_PUSH, identifier, stepInfo.getCodeOutputDirectory(), false));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, BRANCH,
        resolveStringParameterV2("branch", Constants.IDP_CODE_PUSH, identifier, stepInfo.getBranch(), true));

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
        resolveStringParameterV2("org", Constants.REGISTER_CATALOG, identifier, stepInfo.getOrganization(), false));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, BRANCH,
        resolveStringParameterV2("branch", Constants.REGISTER_CATALOG, identifier, stepInfo.getBranch(), false));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, REPO_NAME,
        resolveStringParameterV2("repo", Constants.REGISTER_CATALOG, identifier, stepInfo.getRepository(), false));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, REPO_PROJECT,
        resolveStringParameterV2("project", Constants.REGISTER_CATALOG, identifier, stepInfo.getProject(), false));

    PluginServiceImpl.setOptionalEnvironmentVariable(envVarMap, REPO_WORKSPACE,
        resolveStringParameterV2("workspace", Constants.REGISTER_CATALOG, identifier, stepInfo.getWorkspace(), false));

    envVarMap.put(CONNECTOR_TYPE, gitConnector.getConnectorType().getDisplayName());

    envVarMap.values().removeAll(Collections.singleton(null));
    return envVarMap;
  }
}
