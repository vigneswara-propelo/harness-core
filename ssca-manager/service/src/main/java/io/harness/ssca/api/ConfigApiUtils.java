/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.api;

import io.harness.ssca.entities.ConfigEntity.ConfigEntityKeys;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConfigApiUtils {
  public static String getSortFieldMapping(String field) {
    switch (field) {
      case "creation_on":
        return ConfigEntityKeys.creationOn;
      case "config_id":
        return ConfigEntityKeys.configId;
      case "name":
        return ConfigEntityKeys.name;
      case "type":
        return ConfigEntityKeys.type;
      default:
        log.info(String.format("Mapping not found for field: %s", field));
    }
    return field;
  }
}
