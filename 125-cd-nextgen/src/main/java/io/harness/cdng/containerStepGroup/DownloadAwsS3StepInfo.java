/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.containerStepGroup;

import static io.harness.beans.SwaggerConstants.STRING_CLASSPATH;
import static io.harness.beans.SwaggerConstants.STRING_LIST_CLASSPATH;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.runtime;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.cdng.pipeline.steps.CDAbstractStepInfo;
import io.harness.cdng.visitor.helpers.cdstepinfo.containerStepGroup.DownloadAwsS3StepInfoVisitorHelper;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlNode;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.YamlSchemaTypes;
import io.harness.yaml.extended.ci.container.ContainerResource;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDP)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SimpleVisitorHelper(helperClass = DownloadAwsS3StepInfoVisitorHelper.class)
@JsonTypeName(StepSpecTypeConstants.DOWNLOAD_AWS_S3)
@TypeAlias("downloadAwsS3StepInfo")
@RecasterAlias("io.harness.cdng.containerStepGroup.DownloadAwsS3StepInfo")
public class DownloadAwsS3StepInfo extends StepGroupContainerBaseStepInfo implements CDAbstractStepInfo, Visitable {
  @JsonProperty(YamlNode.UUID_FIELD_NAME)
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
  @ApiModelProperty(hidden = true)
  private String uuid;
  // For Visitor Framework Impl
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> connectorRef;

  @ApiModelProperty(dataType = STRING_CLASSPATH) ParameterField<String> downloadPath;

  @ApiModelProperty(dataType = STRING_LIST_CLASSPATH) ParameterField<List<String>> outputFilePathsContent;

  @ApiModelProperty(dataType = STRING_CLASSPATH) ParameterField<String> bucketName;

  @ApiModelProperty(dataType = STRING_CLASSPATH) ParameterField<String> region;

  @YamlSchemaTypes(value = {runtime})
  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH)
  private ParameterField<List<String>> paths;

  @Builder(builderMethodName = "infoBuilder")
  public DownloadAwsS3StepInfo(ParameterField<List<TaskSelectorYaml>> delegateSelectors,
      ParameterField<String> connectorRef, ContainerResource resources, ParameterField<List<String>> paths,
      ParameterField<Integer> runAsUser, ParameterField<String> downloadPath, ParameterField<String> bucketName,
      ParameterField<String> region, ParameterField<List<String>> outputFilePathsContent) {
    super(delegateSelectors, runAsUser, resources);
    this.connectorRef = connectorRef;
    this.downloadPath = downloadPath;
    this.bucketName = bucketName;
    this.paths = paths;
    this.region = region;
    this.outputFilePathsContent = outputFilePathsContent;
  }

  @Override
  public StepType getStepType() {
    return DownloadAwsS3Step.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.ASYNC;
  }

  @Override
  public SpecParameters getSpecParameters() {
    return DownloadAwsS3StepParameters.infoBuilder()
        .delegateSelectors(this.getDelegateSelectors())
        .connectorRef(this.getConnectorRef())
        .resources(this.getResources())
        .runAsUser(this.getRunAsUser())
        .bucketName(this.getBucketName())
        .region(this.getRegion())
        .downloadPath(this.getDownloadPath())
        .paths(this.getPaths())
        .outputFilePathsContent(this.getOutputFilePathsContent())
        .build();
  }

  @Override
  public ParameterField<List<TaskSelectorYaml>> fetchDelegateSelectors() {
    return getDelegateSelectors();
  }
}
