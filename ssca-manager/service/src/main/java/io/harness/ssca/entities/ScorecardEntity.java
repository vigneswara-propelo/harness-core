/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.entities;

import static io.harness.annotations.dev.HarnessTeam.SSCA;

import io.harness.annotations.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.DbAliases;
import io.harness.persistence.PersistentEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.morphia.annotations.Entity;
import java.util.List;
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
@FieldNameConstants(innerTypeName = "ScorecardEntityKeys")
@StoreIn(DbAliases.SSCA)
@Entity(value = "sbomScorecard", noClassnameStored = true)
@Document("sbomScorecard")
@TypeAlias("sbomScorecard")
public class ScorecardEntity implements PersistentEntity {
  @Id String id;
  String accountId;
  String orgId;
  String projectId;
  String orchestrationId;
  String creationOn;
  String avgScore;
  String maxScore;
  SBOM sbom;
  ScorecardInfo scorecardInfo;
  List<Category> categories;

  @Value
  @Builder
  public static class SBOM {
    String toolName;
    String toolVersion;
    String sbomFileName;
    String sbomFormat;
    String sbomVersion;
    String fileFormat;
  }

  @Value
  @Builder
  public static class ScorecardInfo {
    String toolName;
    String toolVersion;
  }

  @Value
  @Builder
  public static class Category {
    String name;
    String isEnabled;
    String weightage;
    String score;
    String maxScore;
    List<Checks> checks;
  }

  @Value
  @Builder
  public static class Checks {
    String name;
    String isEnabled;
    String score;
    String maxScore;
    String description;
  }
}
