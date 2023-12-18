/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.backstage.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.reinert.jjschema.SchemaIgnore;
import com.google.common.collect.ImmutableList;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "BackstageCatalogKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(HarnessTeam.IDP)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
  @JsonSubTypes.Type(value = BackstageCatalogApiEntity.class, name = "API")
  , @JsonSubTypes.Type(value = BackstageCatalogComponentEntity.class, name = "Component"),
      @JsonSubTypes.Type(value = BackstageCatalogDomainEntity.class, name = "Domain"),
      @JsonSubTypes.Type(value = BackstageCatalogGroupEntity.class, name = "Group"),
      @JsonSubTypes.Type(value = BackstageCatalogLocationEntity.class, name = "Location"),
      @JsonSubTypes.Type(value = BackstageCatalogResourceEntity.class, name = "Resource"),
      @JsonSubTypes.Type(value = BackstageCatalogSystemEntity.class, name = "System"),
      @JsonSubTypes.Type(value = BackstageCatalogTemplateEntity.class, name = "Template"),
      @JsonSubTypes.Type(value = BackstageCatalogUserEntity.class, name = "User")
})
@StoreIn(DbAliases.IDP)
@Entity(value = "backstageCatalog", noClassnameStored = true)
@Document("backstageCatalog")
@HarnessEntity(exportable = true)
public abstract class BackstageCatalogEntity
    implements PersistentEntity, CreatedAtAware, UpdatedAtAware, CreatedByAware, UpdatedByAware {
  @JsonIgnore @Id private String id;
  @JsonIgnore private String accountIdentifier;
  @JsonIgnore private String entityUid;
  private String apiVersion = "backstage.io/v1alpha1";
  private Metadata metadata;
  @JsonIgnore private String kind;
  private Object relations;
  private Object status;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldNameConstants(innerTypeName = "BackstageCatalogEntityMetadataKeys")
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Metadata {
    @JsonIgnore private String identifier;
    @JsonIgnore private String absoluteIdentifier;
    private String name;
    private String title;
    private String namespace;
    private String description;
    private List<String> tags;
    private String uid;
    private String etag;
    @JsonInclude(JsonInclude.Include.NON_EMPTY) private Map<String, String> annotations;
    private Object links;
    private Map<String, String> labels;

    public void setMetadata(String identifier, String absoluteIdentifier, String name, String title, String description,
        List<String> tags, Map<String, String> annotations) {
      this.identifier = identifier;
      this.absoluteIdentifier = absoluteIdentifier;
      this.name = name;
      this.title = title;
      this.description = description;
      this.tags = tags;
      this.annotations = annotations;
    }
  }

  @JsonIgnore private String yaml;

  @JsonIgnore @CreatedDate private long createdAt;
  @JsonIgnore @SchemaIgnore @CreatedBy private EmbeddedUser createdBy;
  @JsonIgnore @LastModifiedDate private long lastUpdatedAt;
  @JsonIgnore @SchemaIgnore @LastModifiedBy private EmbeddedUser lastUpdatedBy;

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("unique_accountIdentifier_entityUid")
                 .field(BackstageCatalogKeys.accountIdentifier)
                 .field(BackstageCatalogKeys.entityUid)
                 .unique(true)
                 .build())
        .build();
  }
}
