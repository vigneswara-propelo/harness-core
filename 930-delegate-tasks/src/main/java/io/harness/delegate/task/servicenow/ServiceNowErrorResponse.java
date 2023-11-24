/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.servicenow;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ServiceNowException;
import io.harness.exception.WingsException;
import io.harness.jackson.JsonNodeUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(CDC)
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class ServiceNowErrorResponse {
  String status;
  String errorMessage;

  public ServiceNowErrorResponse(JsonNode node) throws JsonProcessingException {
    String status = JsonNodeUtils.getString(node, "status");
    JsonNode jsonNode2 = node.get("error");
    if (jsonNode2 == null) {
      throw new ServiceNowException(
          "error response doesn't have \"error\" field", ErrorCode.SERVICENOW_ERROR, WingsException.USER);
    }
    ServiceNowError serviceNowErrorResponse = new ServiceNowError(jsonNode2);
    String parsedErrorDetails = serviceNowErrorResponse.getErrorMessage();
    if (!StringUtils.isBlank(status)) {
      this.status = status;
      this.errorMessage = parsedErrorDetails;
      return;
    }

    throw new ServiceNowException("error response doesn't have \"status\" or \"errorMessage\" field",
        ErrorCode.SERVICENOW_ERROR, WingsException.USER);
  }

  public String getFormattedError() {
    String formattedError = this.getStatus();
    if (!StringUtils.isBlank(this.getErrorMessage())) {
      formattedError = String.format("[%s] : %s", this.getStatus(), this.getErrorMessage());
    }
    return formattedError;
  }
}