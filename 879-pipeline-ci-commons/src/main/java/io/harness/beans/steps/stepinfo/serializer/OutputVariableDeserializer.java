/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.beans.steps.stepinfo.serializer;

import static io.harness.pms.yaml.YAMLFieldNameConstants.NAME;
import static io.harness.pms.yaml.YAMLFieldNameConstants.TYPE;
import static io.harness.pms.yaml.YAMLFieldNameConstants.VALUE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.encryption.SecretRefHelper;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.variables.NGVariable;
import io.harness.yaml.core.variables.NGVariableType;
import io.harness.yaml.core.variables.SecretNGVariable;
import io.harness.yaml.core.variables.StringNGVariable;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@OwnedBy(HarnessTeam.CI)
public class OutputVariableDeserializer extends StdDeserializer<ParameterField<List<NGVariable>>> {
  public OutputVariableDeserializer() {
    super(NGVariable.class);
  }

  protected OutputVariableDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public ParameterField<List<NGVariable>> deserialize(JsonParser jp, DeserializationContext deserializationContext)
      throws IOException {
    List<NGVariable> ngVariables = new ArrayList<>();
    JsonNode currentJsonNode = jp.getCodec().readTree(jp);
    for (JsonNode childNode : currentJsonNode) {
      if (childNode.get(TYPE) == null) {
        NGVariable ngVariable = StringNGVariable.builder()
                                    .name(childNode.get(NAME).asText())
                                    .type(NGVariableType.STRING)
                                    .value(ParameterField.createValueField(childNode.get(NAME).asText()))
                                    .build();
        ngVariables.add(ngVariable);
      } else {
        if (childNode.get(TYPE).asText().equals("String")) {
          NGVariable ngVariable = StringNGVariable.builder()
                                      .name(childNode.get(NAME).asText())
                                      .type(NGVariableType.STRING)
                                      .value(ParameterField.createValueField(childNode.get(VALUE).asText()))
                                      .build();
          ngVariables.add(ngVariable);
        } else if (childNode.get(TYPE).asText().equals("Secret")) {
          NGVariable ngVariable = SecretNGVariable.builder()
                                      .name(childNode.get(NAME).asText())
                                      .type(NGVariableType.SECRET)
                                      .value(ParameterField.createValueField(
                                          SecretRefHelper.createSecretRef(childNode.get(VALUE).asText())))
                                      .build();
          ngVariables.add(ngVariable);
        } else {
          throw new CIStageExecutionException("Unsupported type for the output variable");
        }
      }
    }
    return ParameterField.createValueField(ngVariables);
  }
}
