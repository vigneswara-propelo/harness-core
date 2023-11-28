/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.scorecard.datasourcelocations.locations.scm;

import static io.harness.idp.scorecard.datapoints.constants.Inputs.BRANCH_NAME;
import static io.harness.idp.scorecard.datapoints.constants.Inputs.FILE_PATH;
import static io.harness.idp.scorecard.datasourcelocations.constants.DataSourceLocations.REPOSITORY_BRANCH;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.spec.server.idp.v1.model.InputValue;

import java.util.List;
import java.util.Optional;

@OwnedBy(HarnessTeam.IDP)
public interface ScmContentsDsl {
  String FILE_PATH_REPLACER = "{FILE_PATH_REPLACER}";
  default String replaceInputValuePlaceholders(String requestBody, List<InputValue> inputValues) {
    Optional<InputValue> inputValueOpt =
        inputValues.stream().filter(inputValue -> inputValue.getKey().equals(FILE_PATH)).findFirst();
    if (inputValueOpt.isPresent()) {
      String inputValue = inputValueOpt.get().getValue();
      if (!inputValue.isEmpty()) {
        inputValue = inputValue.replace("\"", "");
        requestBody = requestBody.replace(FILE_PATH_REPLACER, inputValue);
      }
    }

    inputValueOpt = inputValues.stream().filter(inputValue -> inputValue.getKey().equals(BRANCH_NAME)).findFirst();
    if (inputValueOpt.isPresent()) {
      String inputValue = inputValueOpt.get().getValue();
      if (!inputValue.isEmpty()) {
        inputValue = inputValue.replace("\"", "");
        requestBody = requestBody.replace(REPOSITORY_BRANCH, inputValue);
      }
    }
    return requestBody;
  }
}
