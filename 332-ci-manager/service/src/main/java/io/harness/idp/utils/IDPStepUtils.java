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
import static io.harness.idp.Constants.IS_PUBLIC_TEMPLATE;
import static io.harness.idp.Constants.OUTPUT_DIRECTORY_COOKIE_CUTTER;
import static io.harness.idp.Constants.PATH_FOR_TEMPLATE;
import static io.harness.idp.Constants.PREFIX_FOR_COOKIECUTTER_ENV_VARIABLES;
import static io.harness.idp.Constants.PUBLIC_TEMPLATE_URL;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ci.execution.serializer.SerializerUtils;
import io.harness.idp.steps.Constants;
import io.harness.idp.steps.beans.stepinfo.IdpCookieCutterStepInfo;
import io.harness.plugin.service.PluginServiceImpl;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@OwnedBy(HarnessTeam.IDP)
public class IDPStepUtils extends PluginServiceImpl {
  public static Map<String, String> getIDPCookieCuterStepInfoEnvVariables(
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
}
