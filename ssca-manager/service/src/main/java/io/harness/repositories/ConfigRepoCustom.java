/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories;

import io.harness.ssca.entities.ConfigEntity;

import com.google.inject.ImplementedBy;
import com.mongodb.client.result.DeleteResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@ImplementedBy(ConfigRepoCustomImpl.class)
public interface ConfigRepoCustom {
  ConfigEntity saveOrUpdate(ConfigEntity configEntity);
  ConfigEntity update(ConfigEntity configEntity, String configId);
  DeleteResult delete(String accountId, String orgId, String projectId, String configId);
  ConfigEntity findOne(String accountId, String orgId, String projectId, String configId);

  ConfigEntity findByAccountIdAndProjectIdAndOrgIdAndNameAndType(
      String accountId, String orgId, String projectId, String name, String type);
  Page<ConfigEntity> findAll(String accountId, String orgId, String projectId, Pageable pageable);
}
