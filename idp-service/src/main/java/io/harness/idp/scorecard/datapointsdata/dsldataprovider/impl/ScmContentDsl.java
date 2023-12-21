/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.scorecard.datapointsdata.dsldataprovider.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.idp.common.Constants.MESSAGE_KEY;
import static io.harness.idp.scorecard.datapoints.constants.DataPoints.INVALID_CONDITIONAL_INPUT;
import static io.harness.idp.scorecard.datapoints.constants.DataPoints.INVALID_FILE_NAME_ERROR;
import static io.harness.idp.scorecard.datapoints.constants.Inputs.BRANCH_NAME;
import static io.harness.idp.scorecard.datapoints.constants.Inputs.FILE_PATH;
import static io.harness.idp.scorecard.datasourcelocations.constants.DataSourceLocations.AUTHORIZATION_HEADER;
import static io.harness.idp.scorecard.datasourcelocations.constants.DataSourceLocations.REPOSITORY_BRANCH;
import static io.harness.idp.scorecard.datasourcelocations.constants.DataSourceLocations.REPOSITORY_NAME;
import static io.harness.idp.scorecard.datasourcelocations.constants.DataSourceLocations.REPOSITORY_OWNER;
import static io.harness.idp.scorecard.datasourcelocations.constants.DataSourceLocations.REPOSITORY_SUB_FOLDER;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ResponseMessage;
import io.harness.exception.InvalidRequestException;
import io.harness.idp.common.GsonUtils;
import io.harness.idp.scorecard.datasourcelocations.beans.ApiRequestDetails;
import io.harness.idp.scorecard.datasourcelocations.client.DslClient;
import io.harness.idp.scorecard.datasourcelocations.client.DslClientFactory;
import io.harness.spec.server.idp.v1.model.InputValue;
import io.harness.spec.server.idp.v1.model.ScmConfig;

import com.google.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;

@OwnedBy(HarnessTeam.IDP)
public abstract class ScmContentDsl {
  String FILE_PATH_REPLACER = "{FILE_PATH_REPLACER}";
  @Inject DslClientFactory dslClientFactory;

  Map<String, Object> fetchData(ScmConfig scmConfig, String accountIdentifier, String baseUrl) {
    DslClient client = dslClientFactory.getClient(accountIdentifier, scmConfig.getRepoScm());
    Map<String, String> headers = getAuthHeaders(scmConfig.getToken());
    ApiRequestDetails apiRequestDetails = this.getApiRequestDetails(baseUrl, headers, 0);
    apiRequestDetails.setUrl(replacePlaceholders(apiRequestDetails.getUrl(), scmConfig, null));
    if (apiRequestDetails.getRequestBody() != null) {
      apiRequestDetails.setRequestBody(replacePlaceholders(apiRequestDetails.getRequestBody(), scmConfig, null));
    }
    Response response = client.call(accountIdentifier, apiRequestDetails);
    String matchingFile = this.getFileName(response, scmConfig);
    if (matchingFile != null) {
      apiRequestDetails = this.getApiRequestDetails(baseUrl, headers, 1);
      apiRequestDetails.setUrl(replacePlaceholders(apiRequestDetails.getUrl(), scmConfig, matchingFile));
      if (apiRequestDetails.getRequestBody() != null) {
        apiRequestDetails.setRequestBody(
            replacePlaceholders(apiRequestDetails.getRequestBody(), scmConfig, matchingFile));
      }
      response = client.call(accountIdentifier, apiRequestDetails);
    }
    return this.processResponse(response);
  }

  public abstract ApiRequestDetails getApiRequestDetails(String baseUrl, Map<String, String> authHeaders, int index);
  public abstract String getFileName(Response response, ScmConfig scmConfig);

  String iterateAndFetchMatchingFile(List<Map<String, Object>> nodes, ScmConfig scmConfig, String field) {
    Pattern pattern = getFileNamePattern(scmConfig);
    String matchingFile = null;
    for (Map<String, Object> node : nodes) {
      String path = (String) node.get(field);
      int lastSlash = path.lastIndexOf("/");
      String fileName = (lastSlash != -1) ? path.substring(lastSlash + 1) : path;
      Matcher matcher = pattern.matcher(fileName);
      if (matcher.matches()) {
        matchingFile = fileName;
        break;
      }
    }
    if (matchingFile == null) {
      throw new BadRequestException(INVALID_FILE_NAME_ERROR);
    }
    return matchingFile;
  }

  Map<String, String> getAuthHeaders(String token) {
    return Map.of(AUTHORIZATION_HEADER, token);
  }

  String replaceInputValuePlaceholders(String requestBody, List<InputValue> inputValues, String fileName) {
    Optional<InputValue> inputValueOpt =
        inputValues.stream().filter(inputValue -> inputValue.getKey().equals(FILE_PATH)).findFirst();
    if (inputValueOpt.isPresent()) {
      String inputValue = inputValueOpt.get().getValue();
      inputValue = inputValue.replace("\"", "");
      int lastSlash = inputValue.lastIndexOf("/");
      String path = (lastSlash != -1) ? inputValue.substring(0, lastSlash - 1) : "";
      if (!isEmpty(fileName)) {
        path = isEmpty(path) ? path : path + "/";
        requestBody = requestBody.replace(FILE_PATH_REPLACER, path + fileName);
      } else {
        requestBody = requestBody.replace(FILE_PATH_REPLACER, path);
      }
    }

    inputValueOpt = inputValues.stream().filter(inputValue -> inputValue.getKey().equals(BRANCH_NAME)).findFirst();
    if (inputValueOpt.isPresent()) {
      String inputValue = inputValueOpt.get().getValue();
      inputValue = inputValue.replace("\"", "");
      if (!inputValue.isEmpty()) {
        requestBody = requestBody.replace(REPOSITORY_BRANCH, inputValue);
      }
    }
    return requestBody;
  }

  String replacePlaceholders(String urlOrRequestBody, ScmConfig scmConfig, String fileName) {
    urlOrRequestBody = replaceInputValuePlaceholders(
        urlOrRequestBody, scmConfig.getDataSourceLocation().getDataPoints().get(0).getInputValues(), fileName);
    urlOrRequestBody = urlOrRequestBody.replace(REPOSITORY_OWNER, scmConfig.getRepoOwner());
    urlOrRequestBody = urlOrRequestBody.replace(REPOSITORY_NAME, scmConfig.getRepoName());
    if (scmConfig.getRepoBranch() != null) {
      urlOrRequestBody = urlOrRequestBody.replace(REPOSITORY_BRANCH, scmConfig.getRepoBranch());
    }
    if (scmConfig.getRepoSubFolder() != null) {
      urlOrRequestBody = urlOrRequestBody.replace(REPOSITORY_SUB_FOLDER, scmConfig.getRepoSubFolder());
    }
    return urlOrRequestBody;
  }

  Pattern getFileNamePattern(ScmConfig scmConfig) {
    Optional<InputValue> inputValueOpt = scmConfig.getDataSourceLocation()
                                             .getDataPoints()
                                             .get(0)
                                             .getInputValues()
                                             .stream()
                                             .filter(inputValue -> inputValue.getKey().equals(FILE_PATH))
                                             .findFirst();

    if (inputValueOpt.isPresent()) {
      String inputValue = inputValueOpt.get().getValue();
      inputValue = inputValue.replace("\"", "");
      int lastSlash = inputValue.lastIndexOf("/");
      String inputFile = (lastSlash != -1) ? inputValue.substring(lastSlash + 1) : inputValue;
      return Pattern.compile(inputFile);
    }
    throw new BadRequestException(INVALID_CONDITIONAL_INPUT);
  }

  Map<String, Object> processResponse(Response response) {
    if (response.getStatus() == 200) {
      return GsonUtils.convertJsonStringToObject((String) response.getEntity(), Map.class);
    } else if (response.getStatus() == 500) {
      throw new InvalidRequestException(((ResponseMessage) response.getEntity()).getMessage());
    } else {
      throw new BadRequestException(
          (String) GsonUtils.convertJsonStringToObject((String) response.getEntity(), Map.class).get(MESSAGE_KEY));
    }
  }
}
