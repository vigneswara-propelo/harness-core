/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.entities.migration;

import static io.harness.annotations.dev.HarnessTeam.SSCA;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.DbAliases;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.morphia.annotations.Entity;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(SSCA)
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "ELKMigrationKeys")
@StoreIn(DbAliases.SSCA)
@Entity(value = "migrationSummary", noClassnameStored = true)
@Document("migrationSummary")
@TypeAlias("migrationSummary")
@HarnessEntity(exportable = true)
public class MigrationEntity {
  public enum MigrationStatus { SUCCESS, FAILURE }

  String name;
  MigrationStatus status;
  @CreatedDate private Long createdAt;
  @LastModifiedDate private Long lastModifiedAt;
}
