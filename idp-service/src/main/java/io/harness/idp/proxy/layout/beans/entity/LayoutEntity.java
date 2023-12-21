/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */
package io.harness.idp.proxy.layout.beans.entity;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;

import com.google.common.collect.ImmutableList;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "LayoutsEntityKeys")
@StoreIn(DbAliases.IDP)
@HarnessEntity(exportable = true)
@Entity(value = "layouts", noClassnameStored = true)
@Document("layouts")
@Persistent
@OwnedBy(HarnessTeam.IDP)
public class LayoutEntity implements PersistentEntity, CreatedAtAware, UpdatedAtAware {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("unique_accountIdentifier_name_type")
                 .unique(true)
                 .field(LayoutsEntityKeys.accountIdentifier)
                 .field(LayoutsEntityKeys.name)
                 .field(LayoutsEntityKeys.type)
                 .build())
        .build();
  }

  @Id private String id;
  @NotNull private String accountIdentifier;
  @NotNull private String yaml;
  @NotNull private String defaultYaml;
  @NotNull private String displayName;
  @NotNull private String name;
  @NotNull private String type;
  @NotNull private String identifier;
  @CreatedDate private long createdAt;
  @LastModifiedDate private long lastUpdatedAt;
}
