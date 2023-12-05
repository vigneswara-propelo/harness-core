/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.entities.drift;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.UuidAware;
import io.harness.ssca.beans.drift.ComponentDrift;
import io.harness.ssca.beans.drift.DriftBase;

import dev.morphia.annotations.Entity;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import lombok.Builder;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Value
@Builder
@StoreIn(DbAliases.SSCA)
@Entity(value = "drifts", noClassnameStored = true)
@Document("drifts")
@TypeAlias("drifts")
@HarnessEntity(exportable = true)
@OwnedBy(HarnessTeam.SSCA)
public class DriftEntity implements UuidAware {
  @NonFinal @Setter @Id String uuid; // uuid of the drift entity.
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  String orchestrationId; // stores orchestration id of current execution
  String baseOrchestrationId; // store orchestration id of sbom against which drift ran.
  String artifactId;
  String tag;
  String baseTag;
  DriftBase base; // mode showing what was the base sbom
  List<ComponentDrift> componentDrifts; // will be in sorted order.
  @FdIndex @CreatedDate long createdAt;
  @Builder.Default @NonFinal @FdTtlIndex Date validUntil = Date.from(OffsetDateTime.now().plusMonths(6).toInstant());
}
