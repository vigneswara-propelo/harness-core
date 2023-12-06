/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.app.beans.entities;

import static io.harness.annotations.dev.HarnessTeam.CI;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.RecasterAlias;
import io.harness.annotations.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import dev.morphia.annotations.Entity;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@OwnedBy(CI)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "StepExecutionParametersKeys")
@StoreIn(DbAliases.CIMANAGER)
@Entity(value = "stepexecutionparameters", noClassnameStored = true)
@Document("stepexecutionparameters")
@TypeAlias("stepexecutionparameters")
@RecasterAlias("io.harness.app.beans.entities.StepExecutionParameters")
@HarnessEntity(exportable = true)
public class StepExecutionParameters implements UuidAware, PersistentEntity {
  @Id @dev.morphia.annotations.Id String uuid;
  @NotBlank String stepParameters;
  String accountId;
  String runTimeId;
  String stageRunTimeId;

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("account_runtime")
                 .field(StepExecutionParametersKeys.accountId)
                 .field(StepExecutionParametersKeys.runTimeId)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("account_stageruntime")
                 .field(StepExecutionParametersKeys.accountId)
                 .field(StepExecutionParametersKeys.stageRunTimeId)
                 .build())
        .build();
  }
}
