/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.services;

import io.harness.spec.server.ssca.v1.model.ConfigRequestBody;
import io.harness.spec.server.ssca.v1.model.ConfigResponseBody;

import javax.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ConfigService {
  void deleteConfigById(String orgId, String projectId, String configId, String accountId);

  ConfigResponseBody getConfigById(String orgId, String projectId, String configId, String accountId);

  ConfigResponseBody getConfigByNameAndType(String orgId, String projectId, String name, String type, String accountId);

  void saveConfig(String orgId, String projectId, @Valid ConfigRequestBody body, String accountId);

  void updateConfigById(
      String orgId, String projectId, String configId, @Valid ConfigRequestBody body, String accountId);

  Page<ConfigResponseBody> listConfigs(String orgId, String projectId, String accountId, Pageable pageable);
}
