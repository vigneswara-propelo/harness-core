/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.entities;

import static io.harness.annotations.dev.HarnessTeam.SSCA;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.DbAliases;
import io.harness.persistence.PersistentEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.morphia.annotations.Entity;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(SSCA)
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "ConfigEntityKeys")
@StoreIn(DbAliases.SSCA)
@Entity(value = "sscaConfig", noClassnameStored = true)
@Document("sscaConfig")
@TypeAlias("sscaConfig")
@HarnessEntity(exportable = true)
public class ConfigEntity implements PersistentEntity {
  @Id String id;
  String accountId;
  String orgId;
  String projectId;
  String configId;
  String creationOn;
  String userId;
  String name;
  String type;
  List<ConfigInfo> configInfos;

  @Value
  @Builder
  public static class ConfigInfo {
    String id;
    String categoryName;
    Map<String, String> config;
  }
}
