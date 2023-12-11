/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.search.utils;

import static java.nio.charset.StandardCharsets.UTF_8;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;

import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

@Getter
@Setter
@Slf4j
@Singleton
@OwnedBy(HarnessTeam.SSCA)
public class ElasticSearchUtils {
  private static final ObjectMapper objectMapper = new ObjectMapper();

  public static String toJson(Object object) throws IOException {
    return objectMapper.writeValueAsString(object);
  }

  public static TypeMapping readTypeMapping(String mappingSource) {
    return TypeMapping.of(tm -> tm.withJson(new StringReader(mappingSource)));
  }

  public static TypeMapping getTypeMappingFromFile(String file) {
    String mappingSource = "";
    ClassLoader classLoader = ClassLoader.getSystemClassLoader();
    try (InputStream inputStream = classLoader.getResourceAsStream(file)) {
      if (inputStream != null) {
        mappingSource = IOUtils.toString(inputStream, UTF_8);
      }
      return readTypeMapping(mappingSource);
    } catch (IOException e) {
      throw new InvalidRequestException("Error occurred while reading file: ", e);
    }
  }

  public static boolean handleBulkErrors(BulkResponse bulkResponse) {
    if (bulkResponse.errors()) {
      // Log details of failed operations
      for (BulkResponseItem itemResponse : bulkResponse.items()) {
        if (itemResponse.error() != null) {
          // Log or handle the failure details
          log.error(
              "Failed to index document with ID: {}, Reason: {}", itemResponse.id(), itemResponse.error().reason());
        }
      }
      return false;
    }
    return true;
  }
}
