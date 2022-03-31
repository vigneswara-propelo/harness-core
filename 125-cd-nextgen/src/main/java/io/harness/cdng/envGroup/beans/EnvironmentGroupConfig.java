/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.envGroup.beans;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.WRAPPER_OBJECT;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.data.validator.Trimmed;
import io.harness.gitsync.beans.YamlDTO;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@OwnedBy(PIPELINE)
@Data
@Builder
@RecasterAlias("io.harness.cdng.envGroup.beans.EnvironmentGroupConfig")
@JsonTypeInfo(use = NAME, include = WRAPPER_OBJECT)
@JsonTypeName("environmentGroup")
public class EnvironmentGroupConfig implements YamlDTO {
  @EntityName String name;
  @EntityIdentifier String identifier;

  @NotNull @Trimmed String orgIdentifier;
  @NotNull @Trimmed String projectIdentifier;

  String description;
  String color;
  Map<String, String> tags;

  private List<String> envIdentifiers;
}
