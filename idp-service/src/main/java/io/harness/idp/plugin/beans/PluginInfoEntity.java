/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.plugin.beans;

import io.harness.annotations.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.PersistentEntity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@StoreIn(DbAliases.IDP)
@FieldNameConstants(innerTypeName = "PluginInfoEntityKeys")
@Entity(value = "pluginInfo", noClassnameStored = true)
@Document("pluginInfo")
@Persistent
@OwnedBy(HarnessTeam.IDP)
public class PluginInfoEntity implements PersistentEntity {
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
  @Builder.Default private boolean core = false;
  private String source;
  private String config;
  @JsonProperty("environmentVariables") private List<String> envVariables;
  private String iconUrl;
  private String imageUrl;
  private String documentation;
  @JsonProperty("exports") private ExportsData exports;
}
