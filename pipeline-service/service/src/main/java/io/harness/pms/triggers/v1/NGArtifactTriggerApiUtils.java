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
import io.harness.delegate.task.artifacts.ami.AMITag;
import io.harness.exception.InvalidRequestException;
import io.harness.ngtriggers.beans.source.artifact.AMIRegistrySpec;
import io.harness.ngtriggers.beans.source.artifact.AcrSpec;
import io.harness.ngtriggers.beans.source.artifact.AmazonS3RegistrySpec;
import io.harness.ngtriggers.beans.source.artifact.ArtifactType;
import io.harness.ngtriggers.beans.source.artifact.ArtifactTypeSpec;
import io.harness.ngtriggers.beans.source.artifact.ArtifactoryRegistrySpec;
import io.harness.ngtriggers.beans.source.artifact.AzureArtifactsRegistrySpec;
import io.harness.ngtriggers.beans.source.artifact.BambooRegistrySpec;
import io.harness.ngtriggers.beans.source.artifact.DockerRegistrySpec;
import io.harness.ngtriggers.beans.source.artifact.EcrSpec;
import io.harness.ngtriggers.beans.source.artifact.GarSpec;
import io.harness.ngtriggers.beans.source.artifact.GcrSpec;
import io.harness.ngtriggers.beans.source.artifact.GithubPackagesSpec;
import io.harness.ngtriggers.beans.source.artifact.GoolgeCloudStorageRegistrySpec;
import io.harness.ngtriggers.beans.source.artifact.JenkinsRegistrySpec;
import io.harness.ngtriggers.beans.source.artifact.Nexus2RegistrySpec;
import io.harness.ngtriggers.beans.source.artifact.NexusRegistrySpec;
import io.harness.ngtriggers.beans.source.webhook.v2.TriggerEventDataCondition;
import io.harness.ngtriggers.conditionchecker.ConditionOperator;
import io.harness.pms.yaml.ParameterField;
import io.harness.spec.server.pipeline.v1.model.AMIFilter;
import io.harness.spec.server.pipeline.v1.model.AcrArtifactSpec;
import io.harness.spec.server.pipeline.v1.model.AcrArtifactTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.AmazonMachineImageArtifactSpec;
import io.harness.spec.server.pipeline.v1.model.AmazonMachineImageArtifactTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.AmazonS3ArtifactSpec;
import io.harness.spec.server.pipeline.v1.model.AmazonS3ArtifactTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.ArtifactTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.ArtifactoryRegistryArtifactSpec;
import io.harness.spec.server.pipeline.v1.model.ArtifactoryRegistryArtifactTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.AzureArtifactsArtifactSpec;
import io.harness.spec.server.pipeline.v1.model.AzureArtifactsArtifactTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.BambooArtifactSpec;
import io.harness.spec.server.pipeline.v1.model.BambooArtifactTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.CustomArtifactSpec;
import io.harness.spec.server.pipeline.v1.model.CustomArtifactTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.DockerRegistryArtifactSpec;
import io.harness.spec.server.pipeline.v1.model.DockerRegistryArtifactTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.EcrArtifactSpec;
import io.harness.spec.server.pipeline.v1.model.EcrArtifactTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.GcrArtifactSpec;
import io.harness.spec.server.pipeline.v1.model.GcrArtifactTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.GithubPackageRegistryArtifactSpec;
import io.harness.spec.server.pipeline.v1.model.GithubPackageRegistryArtifactTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.GoogleArtifactRegistryArtifactSpec;
import io.harness.spec.server.pipeline.v1.model.GoogleArtifactRegistryArtifactTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.GoogleCloudStorageArtifactSpec;
import io.harness.spec.server.pipeline.v1.model.GoogleCloudStorageArtifactTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.JenkinsArtifactSpec;
import io.harness.spec.server.pipeline.v1.model.JenkinsArtifactTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.Nexus2RegistryArtifactSpec;
import io.harness.spec.server.pipeline.v1.model.Nexus2RegistryArtifactTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.Nexus3RegistryArtifactSpec;
import io.harness.spec.server.pipeline.v1.model.Nexus3RegistryArtifactTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.TriggerConditions;
import io.harness.yaml.core.variables.NGVariable;
import io.harness.yaml.core.variables.NGVariableType;
import io.harness.yaml.core.variables.NumberNGVariable;
import io.harness.yaml.core.variables.StringNGVariable;

import java.util.stream.Collectors;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_TRIGGERS})
@OwnedBy(HarnessTeam.PIPELINE)
public class NGArtifactTriggerApiUtils {
  ArtifactType toArtifactTriggerType(ArtifactTriggerSpec.TypeEnum typeEnum) {
    switch (typeEnum) {
      case ACR:
        return ArtifactType.ACR;
      case ECR:
        return ArtifactType.ECR;
      case GCR:
        return ArtifactType.GCR;
      case BAMBOO:
        return ArtifactType.BAMBOO;
      case JENKINS:
        return ArtifactType.JENKINS;
      case AMAZONS3:
        return ArtifactType.AMAZON_S3;
      case AZUREARTIFACTS:
        return ArtifactType.AZURE_ARTIFACTS;
      case CUSTOMARTIFACT:
        return ArtifactType.CUSTOM_ARTIFACT;
      case DOCKERREGISTRY:
        return ArtifactType.DOCKER_REGISTRY;
      case NEXUS2REGISTRY:
        return ArtifactType.NEXUS2_REGISTRY;
      case NEXUS3REGISTRY:
        return ArtifactType.NEXUS3_REGISTRY;
      case AMAZONMACHINEIMAGE:
        return ArtifactType.AMI;
      case GOOGLECLOUDSTORAGE:
        return ArtifactType.GOOGLE_CLOUD_STORAGE;
      case ARTIFACTORYREGISTRY:
        return ArtifactType.ARTIFACTORY_REGISTRY;
      case GITHUBPACKAGEREGISTRY:
        return ArtifactType.GITHUB_PACKAGES;
      case GOOGLEARTIFACTREGISTRY:
        return ArtifactType.GoogleArtifactRegistry;
      default:
        throw new InvalidRequestException("Artifact Trigger Type " + typeEnum + " is invalid");
    }
  }
  ArtifactTypeSpec toArtifactTypeSpec(ArtifactTriggerSpec spec) {
    switch (spec.getType()) {
      case GCR:
        GcrArtifactTriggerSpec gcrArtifactSpec = ((GcrArtifactSpec) spec).getSpec();
        return GcrSpec.builder()
            .connectorRef(gcrArtifactSpec.getConnectorRef())
            .eventConditions(gcrArtifactSpec.getEventConditions()
                                 .stream()
                                 .map(this::toTriggerEventDataCondition)
                                 .collect(Collectors.toList()))
            .imagePath(gcrArtifactSpec.getImagePath())
            .jexlCondition(gcrArtifactSpec.getJexlCondition())
            .metaDataConditions(gcrArtifactSpec.getMetaDataConditions()
                                    .stream()
                                    .map(this::toTriggerEventDataCondition)
                                    .collect(Collectors.toList()))
            .registryHostname(gcrArtifactSpec.getRegistryHostname())
            .tag(gcrArtifactSpec.getTag())
            .build();
      case GOOGLEARTIFACTREGISTRY:
        GoogleArtifactRegistryArtifactTriggerSpec garArtifactSpec =
            ((GoogleArtifactRegistryArtifactSpec) spec).getSpec();
        return GarSpec.builder()
            .connectorRef(garArtifactSpec.getConnectorRef())
            .eventConditions(garArtifactSpec.getEventConditions()
                                 .stream()
                                 .map(this::toTriggerEventDataCondition)
                                 .collect(Collectors.toList()))
            .jexlCondition(garArtifactSpec.getJexlCondition())
            .metaDataConditions(garArtifactSpec.getMetaDataConditions()
                                    .stream()
                                    .map(this::toTriggerEventDataCondition)
                                    .collect(Collectors.toList()))
            .pkg(garArtifactSpec.getPkg())
            .project(garArtifactSpec.getProject())
            .region(garArtifactSpec.getRegion())
            .version(garArtifactSpec.getVersion())
            .repositoryName(garArtifactSpec.getRepositoryName())
            .build();
      case GITHUBPACKAGEREGISTRY:
        GithubPackageRegistryArtifactTriggerSpec gprArtifactSpec = ((GithubPackageRegistryArtifactSpec) spec).getSpec();
        return GithubPackagesSpec.builder()
            .connectorRef(gprArtifactSpec.getConnectorRef())
            .eventConditions(gprArtifactSpec.getEventConditions()
                                 .stream()
                                 .map(this::toTriggerEventDataCondition)
                                 .collect(Collectors.toList()))
            .jexlCondition(gprArtifactSpec.getJexlCondition())
            .metaDataConditions(gprArtifactSpec.getMetaDataConditions()
                                    .stream()
                                    .map(this::toTriggerEventDataCondition)
                                    .collect(Collectors.toList()))
            .org(gprArtifactSpec.getOrg())
            .packageType(gprArtifactSpec.getPackageType())
            .packageName(gprArtifactSpec.getPackageName())
            .build();
      case ARTIFACTORYREGISTRY:
        ArtifactoryRegistryArtifactTriggerSpec artifactoryRegistryArtifactTriggerSpec =
            ((ArtifactoryRegistryArtifactSpec) spec).getSpec();
        return ArtifactoryRegistrySpec.builder()
            .connectorRef(artifactoryRegistryArtifactTriggerSpec.getConnectorRef())
            .eventConditions(artifactoryRegistryArtifactTriggerSpec.getEventConditions()
                                 .stream()
                                 .map(this::toTriggerEventDataCondition)
                                 .collect(Collectors.toList()))
            .metaDataConditions(artifactoryRegistryArtifactTriggerSpec.getMetaDataConditions()
                                    .stream()
                                    .map(this::toTriggerEventDataCondition)
                                    .collect(Collectors.toList()))
            .jexlCondition(artifactoryRegistryArtifactTriggerSpec.getJexlCondition())
            .artifactDirectory(artifactoryRegistryArtifactTriggerSpec.getArtifactDirectory())
            .artifactFilter(artifactoryRegistryArtifactTriggerSpec.getArtifactFilter())
            .artifactPath(artifactoryRegistryArtifactTriggerSpec.getArtifactPath())
            .repository(artifactoryRegistryArtifactTriggerSpec.getRepository())
            .repositoryFormat(artifactoryRegistryArtifactTriggerSpec.getRepositoryFormat())
            .repositoryUrl(artifactoryRegistryArtifactTriggerSpec.getRepositoryUrl())
            .build();
      case GOOGLECLOUDSTORAGE:
        GoogleCloudStorageArtifactTriggerSpec gcsArtifactTriggerSpec =
            ((GoogleCloudStorageArtifactSpec) spec).getSpec();
        return GoolgeCloudStorageRegistrySpec.builder()
            .connectorRef(gcsArtifactTriggerSpec.getConnectorRef())
            .eventConditions(gcsArtifactTriggerSpec.getEventConditions()
                                 .stream()
                                 .map(this::toTriggerEventDataCondition)
                                 .collect(Collectors.toList()))
            .metaDataConditions(gcsArtifactTriggerSpec.getMetaDataConditions()
                                    .stream()
                                    .map(this::toTriggerEventDataCondition)
                                    .collect(Collectors.toList()))
            .jexlCondition(gcsArtifactTriggerSpec.getJexlCondition())
            .artifactPath(gcsArtifactTriggerSpec.getArtifactPath())
            .bucket(gcsArtifactTriggerSpec.getBucket())
            .project(gcsArtifactTriggerSpec.getProject())
            .build();
      case AMAZONMACHINEIMAGE:
        AmazonMachineImageArtifactTriggerSpec amiArtifactTriggerSpec =
            ((AmazonMachineImageArtifactSpec) spec).getSpec();
        return AMIRegistrySpec.builder()
            .connectorRef(amiArtifactTriggerSpec.getConnectorRef())
            .eventConditions(amiArtifactTriggerSpec.getEventConditions()
                                 .stream()
                                 .map(this::toTriggerEventDataCondition)
                                 .collect(Collectors.toList()))
            .metaDataConditions(amiArtifactTriggerSpec.getMetaDataConditions()
                                    .stream()
                                    .map(this::toTriggerEventDataCondition)
                                    .collect(Collectors.toList()))
            .jexlCondition(amiArtifactTriggerSpec.getJexlCondition())
            .filters(amiArtifactTriggerSpec.getFilters().stream().map(this::toAMIFilter).collect(Collectors.toList()))
            .region(amiArtifactTriggerSpec.getRegion())
            .tags(amiArtifactTriggerSpec.getTags().stream().map(this::toAMITag).collect(Collectors.toList()))
            .version(amiArtifactTriggerSpec.getVersion())
            .versionRegex(amiArtifactTriggerSpec.getVersionRegex())
            .build();
      case NEXUS3REGISTRY:
        Nexus3RegistryArtifactTriggerSpec nexus3ArtifactTriggerSpec = ((Nexus3RegistryArtifactSpec) spec).getSpec();
        return NexusRegistrySpec.builder()
            .connectorRef(nexus3ArtifactTriggerSpec.getConnectorRef())
            .eventConditions(nexus3ArtifactTriggerSpec.getEventConditions()
                                 .stream()
                                 .map(this::toTriggerEventDataCondition)
                                 .collect(Collectors.toList()))
            .metaDataConditions(nexus3ArtifactTriggerSpec.getMetaDataConditions()
                                    .stream()
                                    .map(this::toTriggerEventDataCondition)
                                    .collect(Collectors.toList()))
            .jexlCondition(nexus3ArtifactTriggerSpec.getJexlCondition())
            .artifactId(nexus3ArtifactTriggerSpec.getArtifactId())
            .classifier(nexus3ArtifactTriggerSpec.getClassifier())
            .extension(nexus3ArtifactTriggerSpec.getExtension())
            .group(nexus3ArtifactTriggerSpec.getGroup())
            .groupId(nexus3ArtifactTriggerSpec.getGroupId())
            .imagePath(nexus3ArtifactTriggerSpec.getImagePath())
            .packageName(nexus3ArtifactTriggerSpec.getPackageName())
            .repository(nexus3ArtifactTriggerSpec.getRepository())
            .repositoryFormat(nexus3ArtifactTriggerSpec.getRepositoryFormat())
            .repositoryUrl(nexus3ArtifactTriggerSpec.getRepositoryUrl())
            .tag(nexus3ArtifactTriggerSpec.getTag())
            .build();
      case DOCKERREGISTRY:
        DockerRegistryArtifactTriggerSpec dockerRegistryArtifactTriggerSpec =
            ((DockerRegistryArtifactSpec) spec).getSpec();
        return DockerRegistrySpec.builder()
            .connectorRef(dockerRegistryArtifactTriggerSpec.getConnectorRef())
            .eventConditions(dockerRegistryArtifactTriggerSpec.getEventConditions()
                                 .stream()
                                 .map(this::toTriggerEventDataCondition)
                                 .collect(Collectors.toList()))
            .metaDataConditions(dockerRegistryArtifactTriggerSpec.getMetaDataConditions()
                                    .stream()
                                    .map(this::toTriggerEventDataCondition)
                                    .collect(Collectors.toList()))
            .jexlCondition(dockerRegistryArtifactTriggerSpec.getJexlCondition())
            .imagePath(dockerRegistryArtifactTriggerSpec.getImagePath())
            .tag(dockerRegistryArtifactTriggerSpec.getTag())
            .build();
      case CUSTOMARTIFACT:
        CustomArtifactTriggerSpec customArtifactTriggerSpec = ((CustomArtifactSpec) spec).getSpec();
        return io.harness.ngtriggers.beans.source.artifact.CustomArtifactSpec.builder()
            .eventConditions(customArtifactTriggerSpec.getEventConditions()
                                 .stream()
                                 .map(this::toTriggerEventDataCondition)
                                 .collect(Collectors.toList()))
            .metaDataConditions(customArtifactTriggerSpec.getMetaDataConditions()
                                    .stream()
                                    .map(this::toTriggerEventDataCondition)
                                    .collect(Collectors.toList()))
            .jexlCondition(customArtifactTriggerSpec.getJexlCondition())
            .artifactsArrayPath(customArtifactTriggerSpec.getArtifactsArrayPath())
            .versionPath(customArtifactTriggerSpec.getVersionPath())
            .inputs(customArtifactTriggerSpec.getInputs().stream().map(this::toNGVariable).collect(Collectors.toList()))
            .version(customArtifactTriggerSpec.getVersion())
            .metadata(customArtifactTriggerSpec.getMetadata())
            .script(customArtifactTriggerSpec.getScript())
            .build();
      case NEXUS2REGISTRY:
        Nexus2RegistryArtifactTriggerSpec nexus2ArtifactTriggerSpec = ((Nexus2RegistryArtifactSpec) spec).getSpec();
        return Nexus2RegistrySpec.builder()
            .connectorRef(nexus2ArtifactTriggerSpec.getConnectorRef())
            .eventConditions(nexus2ArtifactTriggerSpec.getEventConditions()
                                 .stream()
                                 .map(this::toTriggerEventDataCondition)
                                 .collect(Collectors.toList()))
            .metaDataConditions(nexus2ArtifactTriggerSpec.getMetaDataConditions()
                                    .stream()
                                    .map(this::toTriggerEventDataCondition)
                                    .collect(Collectors.toList()))
            .jexlCondition(nexus2ArtifactTriggerSpec.getJexlCondition())
            .artifactId(nexus2ArtifactTriggerSpec.getArtifactId())
            .classifier(nexus2ArtifactTriggerSpec.getClassifier())
            .extension(nexus2ArtifactTriggerSpec.getExtension())
            .groupId(nexus2ArtifactTriggerSpec.getGroupId())
            .packageName(nexus2ArtifactTriggerSpec.getPackageName())
            .repositoryFormat(nexus2ArtifactTriggerSpec.getRepositoryFormat())
            .repositoryUrl(nexus2ArtifactTriggerSpec.getRepositoryUrl())
            .tag(nexus2ArtifactTriggerSpec.getTag())
            .repositoryName(nexus2ArtifactTriggerSpec.getRepositoryName())
            .build();
      case AZUREARTIFACTS:
        AzureArtifactsArtifactTriggerSpec azureArtifactTriggerSpec = ((AzureArtifactsArtifactSpec) spec).getSpec();
        return AzureArtifactsRegistrySpec.builder()
            .connectorRef(azureArtifactTriggerSpec.getConnectorRef())
            .eventConditions(azureArtifactTriggerSpec.getEventConditions()
                                 .stream()
                                 .map(this::toTriggerEventDataCondition)
                                 .collect(Collectors.toList()))
            .metaDataConditions(azureArtifactTriggerSpec.getMetaDataConditions()
                                    .stream()
                                    .map(this::toTriggerEventDataCondition)
                                    .collect(Collectors.toList()))
            .jexlCondition(azureArtifactTriggerSpec.getJexlCondition())
            .packageName(azureArtifactTriggerSpec.getPackageName())
            .feed(azureArtifactTriggerSpec.getFeed())
            .packageType(azureArtifactTriggerSpec.getPackageType())
            .project(azureArtifactTriggerSpec.getProject())
            .version(azureArtifactTriggerSpec.getVersion())
            .versionRegex(azureArtifactTriggerSpec.getVersionRegex())
            .build();
      case AMAZONS3:
        AmazonS3ArtifactTriggerSpec amazonS3ArtifactTriggerSpec = ((AmazonS3ArtifactSpec) spec).getSpec();
        return AmazonS3RegistrySpec.builder()
            .connectorRef(amazonS3ArtifactTriggerSpec.getConnectorRef())
            .eventConditions(amazonS3ArtifactTriggerSpec.getEventConditions()
                                 .stream()
                                 .map(this::toTriggerEventDataCondition)
                                 .collect(Collectors.toList()))
            .metaDataConditions(amazonS3ArtifactTriggerSpec.getMetaDataConditions()
                                    .stream()
                                    .map(this::toTriggerEventDataCondition)
                                    .collect(Collectors.toList()))
            .jexlCondition(amazonS3ArtifactTriggerSpec.getJexlCondition())
            .bucketName(amazonS3ArtifactTriggerSpec.getBucketName())
            .filePathRegex(amazonS3ArtifactTriggerSpec.getFilePathRegex())
            .region(amazonS3ArtifactTriggerSpec.getRegion())
            .build();
      case JENKINS:
        JenkinsArtifactTriggerSpec jenkinsArtifactTriggerSpec = ((JenkinsArtifactSpec) spec).getSpec();
        return JenkinsRegistrySpec.builder()
            .connectorRef(jenkinsArtifactTriggerSpec.getConnectorRef())
            .eventConditions(jenkinsArtifactTriggerSpec.getEventConditions()
                                 .stream()
                                 .map(this::toTriggerEventDataCondition)
                                 .collect(Collectors.toList()))
            .metaDataConditions(jenkinsArtifactTriggerSpec.getMetaDataConditions()
                                    .stream()
                                    .map(this::toTriggerEventDataCondition)
                                    .collect(Collectors.toList()))
            .jexlCondition(jenkinsArtifactTriggerSpec.getJexlCondition())
            .artifactPath(jenkinsArtifactTriggerSpec.getArtifactPath())
            .jobName(jenkinsArtifactTriggerSpec.getJobName())
            .build(jenkinsArtifactTriggerSpec.getBuild())
            .build();
      case BAMBOO:
        BambooArtifactTriggerSpec bambooArtifactTriggerSpec = ((BambooArtifactSpec) spec).getSpec();
        return BambooRegistrySpec.builder()
            .connectorRef(bambooArtifactTriggerSpec.getConnectorRef())
            .eventConditions(bambooArtifactTriggerSpec.getEventConditions()
                                 .stream()
                                 .map(this::toTriggerEventDataCondition)
                                 .collect(Collectors.toList()))
            .metaDataConditions(bambooArtifactTriggerSpec.getMetaDataConditions()
                                    .stream()
                                    .map(this::toTriggerEventDataCondition)
                                    .collect(Collectors.toList()))
            .jexlCondition(bambooArtifactTriggerSpec.getJexlCondition())
            .planKey(bambooArtifactTriggerSpec.getPlanKey())
            .artifactPaths(bambooArtifactTriggerSpec.getArtifactPaths())
            .build(bambooArtifactTriggerSpec.getBuild())
            .build();
      case ECR:
        EcrArtifactTriggerSpec ecrArtifactTriggerSpec = ((EcrArtifactSpec) spec).getSpec();
        return EcrSpec.builder()
            .connectorRef(ecrArtifactTriggerSpec.getConnectorRef())
            .eventConditions(ecrArtifactTriggerSpec.getEventConditions()
                                 .stream()
                                 .map(this::toTriggerEventDataCondition)
                                 .collect(Collectors.toList()))
            .metaDataConditions(ecrArtifactTriggerSpec.getMetaDataConditions()
                                    .stream()
                                    .map(this::toTriggerEventDataCondition)
                                    .collect(Collectors.toList()))
            .jexlCondition(ecrArtifactTriggerSpec.getJexlCondition())
            .imagePath(ecrArtifactTriggerSpec.getImagePath())
            .region(ecrArtifactTriggerSpec.getRegion())
            .tag(ecrArtifactTriggerSpec.getTag())
            .registryId(ecrArtifactTriggerSpec.getRegistryId())
            .build();
      case ACR:
        AcrArtifactTriggerSpec acrArtifactTriggerSpec = ((AcrArtifactSpec) spec).getSpec();
        return AcrSpec.builder()
            .connectorRef(acrArtifactTriggerSpec.getConnectorRef())
            .eventConditions(acrArtifactTriggerSpec.getEventConditions()
                                 .stream()
                                 .map(this::toTriggerEventDataCondition)
                                 .collect(Collectors.toList()))
            .metaDataConditions(acrArtifactTriggerSpec.getMetaDataConditions()
                                    .stream()
                                    .map(this::toTriggerEventDataCondition)
                                    .collect(Collectors.toList()))
            .jexlCondition(acrArtifactTriggerSpec.getJexlCondition())
            .tag(acrArtifactTriggerSpec.getTag())
            .registry(acrArtifactTriggerSpec.getRegistry())
            .repository(acrArtifactTriggerSpec.getRepository())
            .subscriptionId(acrArtifactTriggerSpec.getSubscriptionId())
            .build();
      default:
        throw new InvalidRequestException("Artifact Trigger Type " + spec.getType() + " is invalid");
    }
  }

  NGVariable toNGVariable(io.harness.spec.server.pipeline.v1.model.NGVariable ngVariable) {
    switch (ngVariable.getType()) {
      case STRING:
        io.harness.spec.server.pipeline.v1.model.StringNGVariable stringNGVariable =
            (io.harness.spec.server.pipeline.v1.model.StringNGVariable) ngVariable;
        return StringNGVariable.builder()
            .metadata(ngVariable.getMetadata())
            .description(ngVariable.getDescription())
            .name(ngVariable.getName())
            .required(ngVariable.isRequired())
            .type(toNGVariableType(ngVariable.getType()))
            .value(ParameterField.createValueField(stringNGVariable.getValue()))
            .defaultValue(stringNGVariable.getDefaultValue())
            .build();
      case NUMBER:
        io.harness.spec.server.pipeline.v1.model.NumberNGVariable numberNGVariable =
            (io.harness.spec.server.pipeline.v1.model.NumberNGVariable) ngVariable;
        return NumberNGVariable.builder()
            .metadata(ngVariable.getMetadata())
            .description(ngVariable.getDescription())
            .name(ngVariable.getName())
            .required(ngVariable.isRequired())
            .type(toNGVariableType(ngVariable.getType()))
            .value(ParameterField.createValueField(numberNGVariable.getValue()))
            .defaultValue(numberNGVariable.getDefaultValue())
            .build();
      default:
        throw new InvalidRequestException("Variable Type " + ngVariable.getType() + " is invalid");
    }
  }

  NGVariableType toNGVariableType(io.harness.spec.server.pipeline.v1.model.NGVariable.TypeEnum typeEnum) {
    switch (typeEnum) {
      case STRING:
        return NGVariableType.STRING;
      case NUMBER:
        return NGVariableType.NUMBER;
      default:
        throw new InvalidRequestException("Variable Type " + typeEnum + " is invalid");
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

  io.harness.delegate.task.artifacts.ami.AMIFilter toAMIFilter(AMIFilter amiFilter) {
    return io.harness.delegate.task.artifacts.ami.AMIFilter.builder()
        .name(amiFilter.getName())
        .value(amiFilter.getValue())
        .build();
  }

  AMITag toAMITag(AMIFilter amiFilter) {
    return AMITag.builder().name(amiFilter.getName()).value(amiFilter.getValue()).build();
  }
}
