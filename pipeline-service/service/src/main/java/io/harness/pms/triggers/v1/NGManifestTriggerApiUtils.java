/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pms.triggers.v1;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.exception.InvalidRequestException;
import io.harness.ngtriggers.beans.source.ManifestType;
import io.harness.ngtriggers.beans.source.NGTriggerSpecV2;
import io.harness.ngtriggers.beans.source.artifact.BuildStoreType;
import io.harness.ngtriggers.beans.source.artifact.BuildStoreTypeSpec;
import io.harness.ngtriggers.beans.source.artifact.HelmManifestSpec;
import io.harness.ngtriggers.beans.source.artifact.ManifestTriggerConfig;
import io.harness.ngtriggers.beans.source.artifact.ManifestTypeSpec;
import io.harness.ngtriggers.beans.source.artifact.store.BuildStore;
import io.harness.ngtriggers.beans.source.artifact.store.GcsBuildStoreTypeSpec;
import io.harness.ngtriggers.beans.source.artifact.store.HttpBuildStoreTypeSpec;
import io.harness.ngtriggers.beans.source.artifact.store.S3BuildStoreTypeSpec;
import io.harness.ngtriggers.beans.source.artifact.version.HelmVersion;
import io.harness.ngtriggers.beans.source.webhook.v2.TriggerEventDataCondition;
import io.harness.ngtriggers.conditionchecker.ConditionOperator;
import io.harness.spec.server.pipeline.v1.model.GcsBuildStore;
import io.harness.spec.server.pipeline.v1.model.GcsBuildStoreSpec;
import io.harness.spec.server.pipeline.v1.model.HelmChartManifestTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.HttpBuildStore;
import io.harness.spec.server.pipeline.v1.model.HttpBuildStoreSpec;
import io.harness.spec.server.pipeline.v1.model.ManifestTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.S3BuildStore;
import io.harness.spec.server.pipeline.v1.model.S3BuildStoreSpec;
import io.harness.spec.server.pipeline.v1.model.TriggerConditions;

import java.util.stream.Collectors;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_TRIGGERS})
@OwnedBy(HarnessTeam.PIPELINE)
public class NGManifestTriggerApiUtils {
  ManifestType toManifestTriggerType(ManifestTriggerSpec.TypeEnum typeEnum) {
    switch (typeEnum) {
      case HELMCHART:
        return ManifestType.HELM_MANIFEST;
      default:
        throw new InvalidRequestException("Manifest Trigger Type " + typeEnum + " is invalid");
    }
  }
  ManifestTypeSpec toManifestTypeSpec(ManifestTriggerSpec spec) {
    switch (spec.getType()) {
      case HELMCHART:
        return HelmManifestSpec.builder()
            .helmVersion(toHelmVersion(spec.getSpec().getHelmVersion()))
            .chartVersion(spec.getSpec().getChartVersion())
            .chartName(spec.getSpec().getChartName())
            .store(toBuildStore(spec.getSpec().getStore()))
            .eventConditions(spec.getSpec()
                                 .getEventConditions()
                                 .stream()
                                 .map(this::toTriggerEventDataCondition)
                                 .collect(Collectors.toList()))
            .build();
      default:
        throw new InvalidRequestException("Manifest Trigger Type " + spec.getType() + " is invalid");
    }
  }

  HelmVersion toHelmVersion(HelmChartManifestTriggerSpec.HelmVersionEnum helmVersionEnum) {
    switch (helmVersionEnum) {
      case V2:
        return HelmVersion.V2;
      case V3:
        return HelmVersion.V3;
      case V380:
        return HelmVersion.V380;
      default:
        throw new InvalidRequestException("Helm Version " + helmVersionEnum + " is invalid");
    }
  }

  BuildStore toBuildStore(io.harness.spec.server.pipeline.v1.model.BuildStore buildStore) {
    return BuildStore.builder().type(toBuildStoreType(buildStore.getType())).spec(toBuildStoreSpec(buildStore)).build();
  }

  BuildStoreType toBuildStoreType(io.harness.spec.server.pipeline.v1.model.BuildStore.TypeEnum typeEnum) {
    switch (typeEnum) {
      case S3:
        return BuildStoreType.S3;
      case GCS:
        return BuildStoreType.GCS;
      case HTTP:
        return BuildStoreType.HTTP;
      default:
        throw new InvalidRequestException("Build store type " + typeEnum + " is invalid");
    }
  }

  BuildStoreTypeSpec toBuildStoreSpec(io.harness.spec.server.pipeline.v1.model.BuildStore buildStore) {
    switch (buildStore.getType()) {
      case HTTP:
        HttpBuildStoreSpec httpBuildStoreSpec = ((HttpBuildStore) buildStore).getSpec();
        return HttpBuildStoreTypeSpec.builder().connectorRef(httpBuildStoreSpec.getConnectorRef()).build();
      case GCS:
        GcsBuildStoreSpec gcsBuildStoreSpec = ((GcsBuildStore) buildStore).getSpec();
        return GcsBuildStoreTypeSpec.builder()
            .bucketName(gcsBuildStoreSpec.getBucketName())
            .connectorRef(gcsBuildStoreSpec.getConnectorRef())
            .folderPath(gcsBuildStoreSpec.getFolderPath())
            .build();
      case S3:
        S3BuildStoreSpec s3BuildStoreSpec = ((S3BuildStore) buildStore).getSpec();
        return S3BuildStoreTypeSpec.builder()
            .bucketName(s3BuildStoreSpec.getBucketName())
            .connectorRef(s3BuildStoreSpec.getConnectorRef())
            .folderPath(s3BuildStoreSpec.getFolderPath())
            .region(s3BuildStoreSpec.getRegion())
            .build();
      default:
        throw new InvalidRequestException("Build store type " + buildStore.getType() + " is invalid");
    }
  }

  TriggerEventDataCondition toTriggerEventDataCondition(TriggerConditions triggerConditions) {
    return TriggerEventDataCondition.builder()
        .key(triggerConditions.getKey())
        .operator(toConditionOperator(triggerConditions.getOperator()))
        .value(triggerConditions.getValue())
        .build();
  }

  ConditionOperator toConditionOperator(TriggerConditions.OperatorEnum operatorEnum) {
    switch (operatorEnum) {
      case IN:
        return ConditionOperator.IN;
      case NOTIN:
        return ConditionOperator.NOT_IN;
      case EQUALS:
        return ConditionOperator.EQUALS;
      case NOTEQUALS:
        return ConditionOperator.NOT_EQUALS;
      case REGEX:
        return ConditionOperator.REGEX;
      case CONTAINS:
        return ConditionOperator.CONTAINS;
      case DOESNOTCONTAIN:
        return ConditionOperator.DOES_NOT_CONTAIN;
      case ENDSWITH:
        return ConditionOperator.ENDS_WITH;
      case STARTSWITH:
        return ConditionOperator.STARTS_WITH;
      default:
        throw new InvalidRequestException("Conditional Operator " + operatorEnum + " is invalid");
    }
  }

  ManifestTriggerSpec.TypeEnum toManifestTriggerTypeEnum(ManifestType manifestType) {
    switch (manifestType) {
      case HELM_MANIFEST:
        return ManifestTriggerSpec.TypeEnum.HELMCHART;
      default:
        throw new InvalidRequestException("Manifest Trigger Type  " + manifestType + " is invalid");
    }
  }

  TriggerConditions toTriggerCondition(TriggerEventDataCondition triggerEventDataCondition) {
    TriggerConditions triggerConditions = new TriggerConditions();
    triggerConditions.setKey(triggerEventDataCondition.getKey());
    triggerConditions.setOperator(toOperatorEnum(triggerEventDataCondition.getOperator()));
    triggerConditions.setValue(triggerEventDataCondition.getValue());
    return triggerConditions;
  }

  TriggerConditions.OperatorEnum toOperatorEnum(ConditionOperator conditionOperator) {
    switch (conditionOperator) {
      case DOES_NOT_CONTAIN:
        return TriggerConditions.OperatorEnum.DOESNOTCONTAIN;
      case CONTAINS:
        return TriggerConditions.OperatorEnum.CONTAINS;
      case REGEX:
        return TriggerConditions.OperatorEnum.REGEX;
      case NOT_IN:
        return TriggerConditions.OperatorEnum.NOTIN;
      case EQUALS:
        return TriggerConditions.OperatorEnum.EQUALS;
      case IN:
        return TriggerConditions.OperatorEnum.IN;
      case ENDS_WITH:
        return TriggerConditions.OperatorEnum.ENDSWITH;
      case NOT_EQUALS:
        return TriggerConditions.OperatorEnum.NOTEQUALS;
      case STARTS_WITH:
        return TriggerConditions.OperatorEnum.STARTSWITH;
      default:
        throw new InvalidRequestException("Conditional Operator " + conditionOperator + " is invalid");
    }
  }

  HelmChartManifestTriggerSpec.HelmVersionEnum toHelmVersionEnum(HelmVersion helmVersion) {
    switch (helmVersion) {
      case V380:
        return HelmChartManifestTriggerSpec.HelmVersionEnum.V380;
      case V3:
        return HelmChartManifestTriggerSpec.HelmVersionEnum.V3;
      case V2:
        return HelmChartManifestTriggerSpec.HelmVersionEnum.V2;
      default:
        throw new InvalidRequestException("Helm Version " + helmVersion + " is invalid");
    }
  }

  io.harness.spec.server.pipeline.v1.model.BuildStore toApiBuildStore(BuildStore buildStore) {
    switch (buildStore.getType()) {
      case S3:
        S3BuildStoreTypeSpec s3BuildStoreTypeSpec = (S3BuildStoreTypeSpec) buildStore.getSpec();
        S3BuildStore s3BuildStore = new S3BuildStore();
        s3BuildStore.setType(io.harness.spec.server.pipeline.v1.model.BuildStore.TypeEnum.S3);
        S3BuildStoreSpec s3BuildStoreSpec = new S3BuildStoreSpec();
        s3BuildStoreSpec.setBucketName(s3BuildStoreTypeSpec.getBucketName());
        s3BuildStoreSpec.setConnectorRef(s3BuildStoreTypeSpec.getConnectorRef());
        s3BuildStoreSpec.setFolderPath(s3BuildStoreTypeSpec.getFolderPath());
        s3BuildStoreSpec.setRegion(s3BuildStoreTypeSpec.getRegion());
        s3BuildStore.setSpec(s3BuildStoreSpec);
        return s3BuildStore;
      case GCS:
        GcsBuildStoreTypeSpec gcsBuildStoreTypeSpec = (GcsBuildStoreTypeSpec) buildStore.getSpec();
        GcsBuildStore gcsBuildStore = new GcsBuildStore();
        gcsBuildStore.setType(io.harness.spec.server.pipeline.v1.model.BuildStore.TypeEnum.GCS);
        GcsBuildStoreSpec gcsBuildStoreSpec = new GcsBuildStoreSpec();
        gcsBuildStoreSpec.setBucketName(gcsBuildStoreTypeSpec.getBucketName());
        gcsBuildStoreSpec.setConnectorRef(gcsBuildStoreTypeSpec.getConnectorRef());
        gcsBuildStoreSpec.setFolderPath(gcsBuildStoreTypeSpec.getFolderPath());
        gcsBuildStore.setSpec(gcsBuildStoreSpec);
        return gcsBuildStore;
      case HTTP:
        HttpBuildStoreTypeSpec httpBuildStoreTypeSpec = (HttpBuildStoreTypeSpec) buildStore.getSpec();
        HttpBuildStore httpBuildStore = new HttpBuildStore();
        httpBuildStore.setType(io.harness.spec.server.pipeline.v1.model.BuildStore.TypeEnum.HTTP);
        HttpBuildStoreSpec httpBuildStoreSpec = new HttpBuildStoreSpec();
        httpBuildStoreSpec.setConnectorRef(httpBuildStoreTypeSpec.fetchConnectorRef());
        httpBuildStore.setSpec(httpBuildStoreSpec);
        return httpBuildStore;
      default:
        throw new InvalidRequestException("Build store type " + buildStore.getType() + " is invalid");
    }
  }

  ManifestTriggerSpec toManifestTriggerSpec(NGTriggerSpecV2 config) {
    ManifestTriggerConfig manifestTriggerConfig = (ManifestTriggerConfig) config;
    ManifestTriggerSpec manifestTriggerSpec = new ManifestTriggerSpec();
    manifestTriggerSpec.setType(toManifestTriggerTypeEnum(manifestTriggerConfig.getType()));
    HelmChartManifestTriggerSpec helmChartManifestTriggerSpec = new HelmChartManifestTriggerSpec();
    HelmManifestSpec helmManifestSpec = (HelmManifestSpec) manifestTriggerConfig.getSpec();
    helmChartManifestTriggerSpec.setChartName(helmManifestSpec.getChartName());
    helmChartManifestTriggerSpec.setChartVersion(helmManifestSpec.getChartVersion());
    helmChartManifestTriggerSpec.setHelmVersion(toHelmVersionEnum(helmManifestSpec.getHelmVersion()));
    helmChartManifestTriggerSpec.setEventConditions(
        helmManifestSpec.getEventConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    helmChartManifestTriggerSpec.setStore(toApiBuildStore(helmManifestSpec.getStore()));
    manifestTriggerSpec.setSpec(helmChartManifestTriggerSpec);
    return manifestTriggerSpec;
  }
}
