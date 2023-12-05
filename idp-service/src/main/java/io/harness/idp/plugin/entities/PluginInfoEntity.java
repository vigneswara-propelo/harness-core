/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.plugin.entities;

import io.harness.annotations.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.plugin.beans.ExportsData;
import io.harness.idp.plugin.enums.ExportType;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.PersistentEntity;
import io.harness.spec.server.idp.v1.model.PluginInfo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import java.util.List;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@StoreIn(DbAliases.IDP)
@FieldNameConstants(innerTypeName = "PluginInfoEntityKeys")
@Entity(value = "pluginInfo", noClassnameStored = true)
@Document("pluginInfo")
@Persistent
@OwnedBy(HarnessTeam.IDP)
public abstract class PluginInfoEntity implements PersistentEntity {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("unique_account_identifier")
                 .unique(true)
                 .field(PluginInfoEntityKeys.accountIdentifier)
                 .field(PluginInfoEntityKeys.identifier)
                 .build())
        .build();
  }

  @Id @org.mongodb.morphia.annotations.Id private String id;
  private String identifier;
  private String accountIdentifier;
  private String name;
  private String description;
  private String creator;
  private String category;
  private String source;
  private String config;
  @JsonProperty("environmentVariables") private List<String> envVariables;
  private String iconUrl;
  private String imageUrl;
  private String documentation;
  @JsonProperty("exports") private ExportsData exports;
  private PluginInfo.PluginTypeEnum type;

  public static int getExportTypeCount(PluginInfoEntity pluginInfoEntity, ExportType exportType) {
    return (int) pluginInfoEntity.getExports()
        .getExportDetails()
        .stream()
        .filter(exportDetails -> exportDetails.getType().equals(exportType))
        .count();
  }
}
