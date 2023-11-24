/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.plancreator.exports;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.jackson.JsonNodeUtils;
import io.harness.pms.yaml.YAMLFieldNameConstants;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

@OwnedBy(PIPELINE)
public class ExportMapDeserialiser extends StdDeserializer<ExportsMap<String, ExportConfig>> {
  public ExportMapDeserialiser() {
    super(ExportMapDeserialiser.class);
  }

  @Override
  public ExportsMap<String, ExportConfig> deserialize(
      JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
    JsonNode currentJsonNode = jsonParser.getCodec().readTree(jsonParser);
    if (currentJsonNode == null) {
      return null;
    }
    ExportsMap<String, ExportConfig> exports = new ExportsMap<>();
    for (Iterator<Map.Entry<String, JsonNode>> it = currentJsonNode.fields(); it.hasNext();) {
      Map.Entry<String, JsonNode> field = it.next();
      if (YAMLFieldNameConstants.UUID.equals(field.getKey())) {
        continue;
      }
      exports.put(field.getKey(),
          ExportConfig.builder()
              .value(JsonNodeUtils.getValueFromJsonNode(field.getValue().get(YAMLFieldNameConstants.VALUE)))
              .desc(field.getValue().get(YAMLFieldNameConstants.DESC).asText())
              .build());
    }
    return exports;
  }
}
