/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.containerStepGroup;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.pipeline.steps.CdAbstractStepNode;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.yaml.core.StepSpecType;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDP)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(StepSpecTypeConstants.DOWNLOAD_AWS_S3)
@TypeAlias("downloadAwsS3StepNode")
@RecasterAlias("io.harness.cdng.containerStepGroup.DownloadAwsS3StepNode")
public class DownloadAwsS3StepNode extends CdAbstractStepNode {
  @JsonProperty("type") @NotNull DownloadAwsS3StepNode.StepType type = StepType.DownloadAwsS3;
  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  DownloadAwsS3StepInfo downloadAwsS3StepInfo;

  @Override
  public String getType() {
    return StepSpecTypeConstants.DOWNLOAD_AWS_S3;
  }

  @Override
  public StepSpecType getStepSpecType() {
    return downloadAwsS3StepInfo;
  }

  enum StepType {
    DownloadAwsS3(StepSpecTypeConstants.DOWNLOAD_AWS_S3);
    @Getter String name;
    StepType(String name) {
      this.name = name;
    }
  }
}
