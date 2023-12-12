/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.containerStepGroup;

import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;

import io.harness.cdng.pipeline.steps.CdAbstractStepNode;
import io.harness.connector.services.ConnectorService;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.plan.PluginCreationResponse;
import io.harness.pms.contracts.plan.PluginCreationResponseWrapper;
import io.harness.pms.contracts.plan.PluginDetails.Builder;
import io.harness.pms.contracts.plan.StepInfoProto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ContainerStepGroupHelper {
  @Named(DEFAULT_CONNECTOR_SERVICE) @Inject private ConnectorService connectorService;
  private static final String NG_SECRET_MANAGER = "ngSecretManager";

  public String getCompleteStepIdentifier(Ambiance ambiance, String stepIdentifier) {
    StringBuilder identifier = new StringBuilder();
    for (Level level : ambiance.getLevelsList()) {
      if (level.getStepType().getType().equals("STEP_GROUP")) {
        identifier.append(level.getIdentifier());
        identifier.append('_');
      }
    }
    identifier.append(stepIdentifier);
    return identifier.toString();
  }

  public Map<String, String> validateEnvVariables(Map<String, String> environmentVariables) {
    if (isEmpty(environmentVariables)) {
      return environmentVariables;
    }

    List<String> envVarsWithNullValue = environmentVariables.entrySet()
                                            .stream()
                                            .filter(entry -> entry.getValue() == null)
                                            .map(Map.Entry::getKey)
                                            .collect(Collectors.toList());
    if (isNotEmpty(envVarsWithNullValue)) {
      throw new InvalidArgumentsException(format("Not found value for environment variable%s: %s",
          envVarsWithNullValue.size() == 1 ? "" : "s", String.join(",", envVarsWithNullValue)));
    }

    return environmentVariables;
  }

  public Map<String, String> getEnvVarsWithSecretRef(Map<String, String> envVars) {
    return envVars.entrySet()
        .stream()
        .filter(entry -> entry.getValue() != null && entry.getValue().contains(NG_SECRET_MANAGER))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public Map<String, String> removeAllEnvVarsWithSecretRef(Map<String, String> envVars) {
    final Map<String, String> secretEnvVariables = getEnvVarsWithSecretRef(envVars);
    envVars.entrySet().removeAll(secretEnvVariables.entrySet());

    return secretEnvVariables;
  }

  public PluginCreationResponseWrapper getPluginCreationResponseWrapper(
      CdAbstractStepNode cdAbstractStepNode, Builder pluginDetailsBuilder) {
    PluginCreationResponse response =
        PluginCreationResponse.newBuilder().setPluginDetails(pluginDetailsBuilder.build()).build();
    StepInfoProto stepInfoProto = StepInfoProto.newBuilder()
                                      .setIdentifier(cdAbstractStepNode.getIdentifier())
                                      .setName(cdAbstractStepNode.getName())
                                      .setUuid(cdAbstractStepNode.getUuid())
                                      .build();
    return PluginCreationResponseWrapper.newBuilder().setResponse(response).setStepInfo(stepInfoProto).build();
  }

  public String convertToJson(Map<String, String> files) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(files);
    } catch (JsonProcessingException e) {
      throw new InvalidRequestException("Not able to convert list of files in Harness Store to json", e);
    }
  }
}
