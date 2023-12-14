/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pms.triggers.v1;

import static io.harness.ngtriggers.Constants.ACR;
import static io.harness.ngtriggers.Constants.AMAZON_S3;
import static io.harness.ngtriggers.Constants.AMI;
import static io.harness.ngtriggers.Constants.ARTIFACTORY_REGISTRY;
import static io.harness.ngtriggers.Constants.AZURE_ARTIFACTS;
import static io.harness.ngtriggers.Constants.BAMBOO;
import static io.harness.ngtriggers.Constants.CUSTOM_ARTIFACT;
import static io.harness.ngtriggers.Constants.DOCKER_REGISTRY;
import static io.harness.ngtriggers.Constants.ECR;
import static io.harness.ngtriggers.Constants.GCR;
import static io.harness.ngtriggers.Constants.GITHUB_PACKAGES;
import static io.harness.ngtriggers.Constants.GOOGLE_ARTIFACT_REGISTRY;
import static io.harness.ngtriggers.Constants.GOOGLE_CLOUD_STORAGE;
import static io.harness.ngtriggers.Constants.JENKINS;
import static io.harness.ngtriggers.Constants.NEXUS2_REGISTRY;
import static io.harness.ngtriggers.Constants.NEXUS3_REGISTRY;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.delegate.task.artifacts.ami.AMITag;
import io.harness.exception.InvalidRequestException;
import io.harness.ngtriggers.beans.source.NGTriggerSpecV2;
import io.harness.ngtriggers.beans.source.artifact.AMIRegistrySpec;
import io.harness.ngtriggers.beans.source.artifact.AcrSpec;
import io.harness.ngtriggers.beans.source.artifact.AmazonS3RegistrySpec;
import io.harness.ngtriggers.beans.source.artifact.ArtifactType;
import io.harness.ngtriggers.beans.source.artifact.ArtifactTypeSpec;
import io.harness.ngtriggers.beans.source.artifact.ArtifactTypeSpecWrapper;
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
import io.harness.ngtriggers.beans.source.artifact.MultiRegionArtifactTriggerConfig;
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
import io.harness.spec.server.pipeline.v1.model.MultiRegionArtifactTriggerSpec;
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
  ArtifactType toArtifactTriggerType(io.harness.spec.server.pipeline.v1.model.ArtifactType typeEnum) {
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

  MultiRegionArtifactTriggerSpec toMultiRegionArtifactTriggerSpec(NGTriggerSpecV2 spec) {
    MultiRegionArtifactTriggerConfig multiRegionArtifactTriggerConfig = (MultiRegionArtifactTriggerConfig) spec;
    MultiRegionArtifactTriggerSpec multiRegionArtifactTriggerSpec = new MultiRegionArtifactTriggerSpec();
    multiRegionArtifactTriggerSpec.setEventConditions(multiRegionArtifactTriggerConfig.getEventConditions()
                                                          .stream()
                                                          .map(this::toTriggerCondition)
                                                          .collect(Collectors.toList()));
    multiRegionArtifactTriggerSpec.setJexlCondition(multiRegionArtifactTriggerConfig.getJexlCondition());
    multiRegionArtifactTriggerSpec.setSources(multiRegionArtifactTriggerConfig.getSources()
                                                  .stream()
                                                  .map(this::toArtifactTypeSpecWrapper)
                                                  .collect(Collectors.toList()));
    multiRegionArtifactTriggerSpec.setType(toArtifactType(multiRegionArtifactTriggerConfig.getType()));
    multiRegionArtifactTriggerSpec.setMetaDataConditions(multiRegionArtifactTriggerConfig.getMetaDataConditions()
                                                             .stream()
                                                             .map(this::toTriggerCondition)
                                                             .collect(Collectors.toList()));
    return multiRegionArtifactTriggerSpec;
  }

  io.harness.spec.server.pipeline.v1.model.ArtifactTypeSpecWrapper toArtifactTypeSpecWrapper(
      ArtifactTypeSpecWrapper artifactTypeSpecWrapper) {
    io.harness.spec.server.pipeline.v1.model.ArtifactTypeSpecWrapper artifactTypeSpecWrapper1 =
        new io.harness.spec.server.pipeline.v1.model.ArtifactTypeSpecWrapper();
    artifactTypeSpecWrapper1.setSpec(toArtifactTriggerSpec(artifactTypeSpecWrapper.getSpec()));
    return artifactTypeSpecWrapper1;
  }

  io.harness.spec.server.pipeline.v1.model.ArtifactType toArtifactType(ArtifactType type) {
    switch (type) {
      case GOOGLE_CLOUD_STORAGE:
        return io.harness.spec.server.pipeline.v1.model.ArtifactType.GOOGLECLOUDSTORAGE;
      case ARTIFACTORY_REGISTRY:
        return io.harness.spec.server.pipeline.v1.model.ArtifactType.ARTIFACTORYREGISTRY;
      case NEXUS3_REGISTRY:
        return io.harness.spec.server.pipeline.v1.model.ArtifactType.NEXUS3REGISTRY;
      case NEXUS2_REGISTRY:
        return io.harness.spec.server.pipeline.v1.model.ArtifactType.NEXUS2REGISTRY;
      case GITHUB_PACKAGES:
        return io.harness.spec.server.pipeline.v1.model.ArtifactType.GITHUBPACKAGEREGISTRY;
      case DOCKER_REGISTRY:
        return io.harness.spec.server.pipeline.v1.model.ArtifactType.DOCKERREGISTRY;
      case CUSTOM_ARTIFACT:
        return io.harness.spec.server.pipeline.v1.model.ArtifactType.CUSTOMARTIFACT;
      case AZURE_ARTIFACTS:
        return io.harness.spec.server.pipeline.v1.model.ArtifactType.AZUREARTIFACTS;
      case AMAZON_S3:
        return io.harness.spec.server.pipeline.v1.model.ArtifactType.AMAZONS3;
      case AMI:
        return io.harness.spec.server.pipeline.v1.model.ArtifactType.AMAZONMACHINEIMAGE;
      case GCR:
        return io.harness.spec.server.pipeline.v1.model.ArtifactType.GCR;
      case JENKINS:
        return io.harness.spec.server.pipeline.v1.model.ArtifactType.JENKINS;
      case BAMBOO:
        return io.harness.spec.server.pipeline.v1.model.ArtifactType.BAMBOO;
      case ECR:
        return io.harness.spec.server.pipeline.v1.model.ArtifactType.ECR;
      case ACR:
        return io.harness.spec.server.pipeline.v1.model.ArtifactType.ACR;
      case GoogleArtifactRegistry:
        return io.harness.spec.server.pipeline.v1.model.ArtifactType.GOOGLEARTIFACTREGISTRY;
      default:
        throw new InvalidRequestException("Artifact Trigger Type " + type + " is invalid");
    }
  }

  GoogleArtifactRegistryArtifactTriggerSpec toGoogleArtifactRegistryArtifactTriggerSpec(ArtifactTypeSpec spec) {
    GarSpec garSpec = (GarSpec) spec;
    GoogleArtifactRegistryArtifactTriggerSpec googleArtifactRegistryArtifactTriggerSpec =
        new GoogleArtifactRegistryArtifactTriggerSpec();
    googleArtifactRegistryArtifactTriggerSpec.setConnectorRef(garSpec.getConnectorRef());
    googleArtifactRegistryArtifactTriggerSpec.setEventConditions(
        garSpec.getEventConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    googleArtifactRegistryArtifactTriggerSpec.setJexlCondition(garSpec.getJexlCondition());
    googleArtifactRegistryArtifactTriggerSpec.setMetaDataConditions(
        garSpec.fetchMetaDataConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    googleArtifactRegistryArtifactTriggerSpec.setPkg(garSpec.getPkg());
    googleArtifactRegistryArtifactTriggerSpec.setProject(garSpec.getProject());
    googleArtifactRegistryArtifactTriggerSpec.setVersion(garSpec.getVersion());
    googleArtifactRegistryArtifactTriggerSpec.setRepositoryName(garSpec.getRepositoryName());
    googleArtifactRegistryArtifactTriggerSpec.setRegion(garSpec.getRegion());
    return googleArtifactRegistryArtifactTriggerSpec;
  }

  AcrArtifactTriggerSpec toAcrArtifactTriggerSpec(ArtifactTypeSpec spec) {
    AcrSpec acrSpec = (AcrSpec) spec;
    AcrArtifactTriggerSpec acrArtifactTriggerSpec = new AcrArtifactTriggerSpec();
    acrArtifactTriggerSpec.setConnectorRef(acrSpec.getConnectorRef());
    acrArtifactTriggerSpec.setEventConditions(
        acrSpec.getEventConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    acrArtifactTriggerSpec.setJexlCondition(acrSpec.getJexlCondition());
    acrArtifactTriggerSpec.setMetaDataConditions(
        acrSpec.fetchMetaDataConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    acrArtifactTriggerSpec.setRegistry(acrSpec.getRegistry());
    acrArtifactTriggerSpec.setTag(acrSpec.getTag());
    acrArtifactTriggerSpec.setSubscriptionId(acrSpec.getSubscriptionId());
    acrArtifactTriggerSpec.setRepository(acrSpec.getRepository());
    return acrArtifactTriggerSpec;
  }

  EcrArtifactTriggerSpec toEcrArtifactTriggerSpec(ArtifactTypeSpec spec) {
    EcrSpec ecrSpec = (EcrSpec) spec;
    EcrArtifactTriggerSpec ecrArtifactTriggerSpec = new EcrArtifactTriggerSpec();
    ecrArtifactTriggerSpec.setConnectorRef(ecrSpec.getConnectorRef());
    ecrArtifactTriggerSpec.setEventConditions(
        ecrSpec.getEventConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    ecrArtifactTriggerSpec.setJexlCondition(ecrSpec.getJexlCondition());
    ecrArtifactTriggerSpec.setMetaDataConditions(
        ecrSpec.fetchMetaDataConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    ecrArtifactTriggerSpec.setTag(ecrSpec.getTag());
    ecrArtifactTriggerSpec.setRegion(ecrSpec.getRegion());
    ecrArtifactTriggerSpec.setRegistryId(ecrSpec.getRegistryId());
    ecrArtifactTriggerSpec.imagePath(ecrSpec.getImagePath());
    return ecrArtifactTriggerSpec;
  }

  BambooArtifactTriggerSpec toBambooArtifactTriggerSpec(ArtifactTypeSpec spec) {
    BambooRegistrySpec bambooArtifactSpec = (BambooRegistrySpec) spec;
    BambooArtifactTriggerSpec bambooArtifactTriggerSpec = new BambooArtifactTriggerSpec();
    bambooArtifactTriggerSpec.setArtifactPaths(bambooArtifactSpec.getArtifactPaths());
    bambooArtifactTriggerSpec.setConnectorRef(bambooArtifactSpec.getConnectorRef());
    bambooArtifactTriggerSpec.setEventConditions(
        bambooArtifactSpec.getEventConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    bambooArtifactTriggerSpec.setJexlCondition(bambooArtifactSpec.getJexlCondition());
    bambooArtifactTriggerSpec.setMetaDataConditions(
        bambooArtifactSpec.getMetaDataConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    bambooArtifactTriggerSpec.setPlanKey(bambooArtifactSpec.getPlanKey());
    bambooArtifactTriggerSpec.setBuild(bambooArtifactSpec.getBuild());
    return bambooArtifactTriggerSpec;
  }

  JenkinsArtifactTriggerSpec toJenkinsArtifactTriggerSpec(ArtifactTypeSpec spec) {
    JenkinsRegistrySpec jenkinsRegistrySpec = (JenkinsRegistrySpec) spec;
    JenkinsArtifactTriggerSpec jenkinsArtifactTriggerSpec = new JenkinsArtifactTriggerSpec();
    jenkinsArtifactTriggerSpec.setArtifactPath(jenkinsRegistrySpec.getArtifactPath());
    jenkinsArtifactTriggerSpec.setBuild(jenkinsRegistrySpec.getBuild());
    jenkinsArtifactTriggerSpec.setConnectorRef(jenkinsRegistrySpec.getConnectorRef());
    jenkinsArtifactTriggerSpec.setEventConditions(
        jenkinsRegistrySpec.getEventConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    jenkinsArtifactTriggerSpec.setMetaDataConditions(jenkinsRegistrySpec.fetchMetaDataConditions()
                                                         .stream()
                                                         .map(this::toTriggerCondition)
                                                         .collect(Collectors.toList()));
    jenkinsArtifactTriggerSpec.setJobName(jenkinsRegistrySpec.getJobName());
    jenkinsArtifactTriggerSpec.setJexlCondition(jenkinsRegistrySpec.getJexlCondition());
    return jenkinsArtifactTriggerSpec;
  }

  GcrArtifactTriggerSpec toGcrArtifactTriggerSpec(ArtifactTypeSpec spec) {
    GcrSpec gcrSpec = (GcrSpec) spec;
    GcrArtifactTriggerSpec gcrArtifactTriggerSpec = new GcrArtifactTriggerSpec();
    gcrArtifactTriggerSpec.setConnectorRef(gcrSpec.getConnectorRef());
    gcrArtifactTriggerSpec.setEventConditions(
        gcrSpec.getEventConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    gcrArtifactTriggerSpec.setJexlCondition(gcrSpec.getJexlCondition());
    gcrArtifactTriggerSpec.setRegistryHostname(gcrSpec.getRegistryHostname());
    gcrArtifactTriggerSpec.setImagePath(gcrSpec.getImagePath());
    gcrArtifactTriggerSpec.setTag(gcrSpec.getTag());
    gcrArtifactTriggerSpec.setRegistryHostname(gcrSpec.getRegistryHostname());
    gcrArtifactTriggerSpec.setMetaDataConditions(
        gcrSpec.getMetaDataConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    return gcrArtifactTriggerSpec;
  }

  AmazonMachineImageArtifactTriggerSpec toAmazonMachineImageArtifactTriggerSpec(ArtifactTypeSpec spec) {
    AMIRegistrySpec amiRegistrySpec = (AMIRegistrySpec) spec;
    AmazonMachineImageArtifactTriggerSpec amazonMachineImageArtifactTriggerSpec =
        new AmazonMachineImageArtifactTriggerSpec();
    amazonMachineImageArtifactTriggerSpec.setConnectorRef(amiRegistrySpec.getConnectorRef());
    amazonMachineImageArtifactTriggerSpec.setEventConditions(
        amiRegistrySpec.getEventConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    amazonMachineImageArtifactTriggerSpec.setJexlCondition(amiRegistrySpec.getJexlCondition());
    amazonMachineImageArtifactTriggerSpec.setRegion(amiRegistrySpec.getRegion());
    amazonMachineImageArtifactTriggerSpec.setMetaDataConditions(
        amiRegistrySpec.fetchMetaDataConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    amazonMachineImageArtifactTriggerSpec.setFilters(
        amiRegistrySpec.getFilters().stream().map(this::toApiAMIFilter).collect(Collectors.toList()));
    amazonMachineImageArtifactTriggerSpec.setTags(
        amiRegistrySpec.getTags().stream().map(this::toApiAMIFilter).collect(Collectors.toList()));
    amazonMachineImageArtifactTriggerSpec.setVersion(amiRegistrySpec.getVersion());
    amazonMachineImageArtifactTriggerSpec.setVersionRegex(amiRegistrySpec.getVersionRegex());
    return amazonMachineImageArtifactTriggerSpec;
  }

  AMIFilter toApiAMIFilter(io.harness.delegate.task.artifacts.ami.AMIFilter amiFilter) {
    AMIFilter amiFilter1 = new AMIFilter();
    amiFilter1.setName(amiFilter.getName());
    amiFilter1.setValue(amiFilter.getValue());
    return amiFilter1;
  }

  AMIFilter toApiAMIFilter(io.harness.delegate.task.artifacts.ami.AMITag amiTag) {
    AMIFilter amiFilter1 = new AMIFilter();
    amiFilter1.setName(amiTag.getName());
    amiFilter1.setValue(amiTag.getValue());
    return amiFilter1;
  }

  AmazonS3ArtifactTriggerSpec toAmazonS3ArtifactTriggerSpec(ArtifactTypeSpec spec) {
    AmazonS3RegistrySpec amazonS3RegistrySpec = (AmazonS3RegistrySpec) spec;
    AmazonS3ArtifactTriggerSpec amazonS3ArtifactTriggerSpec = new AmazonS3ArtifactTriggerSpec();
    amazonS3ArtifactTriggerSpec.setConnectorRef(amazonS3RegistrySpec.getConnectorRef());
    amazonS3ArtifactTriggerSpec.setEventConditions(
        amazonS3RegistrySpec.getEventConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    amazonS3ArtifactTriggerSpec.setJexlCondition(amazonS3RegistrySpec.getJexlCondition());
    amazonS3ArtifactTriggerSpec.setRegion(amazonS3RegistrySpec.getRegion());
    amazonS3ArtifactTriggerSpec.setBucketName(amazonS3RegistrySpec.getBucketName());
    amazonS3ArtifactTriggerSpec.setFilePathRegex(amazonS3RegistrySpec.getFilePathRegex());
    amazonS3ArtifactTriggerSpec.setMetaDataConditions(amazonS3RegistrySpec.fetchMetaDataConditions()
                                                          .stream()
                                                          .map(this::toTriggerCondition)
                                                          .collect(Collectors.toList()));
    return amazonS3ArtifactTriggerSpec;
  }

  AzureArtifactsArtifactTriggerSpec toAzureArtifactsArtifactTriggerSpec(ArtifactTypeSpec spec) {
    AzureArtifactsRegistrySpec azureArtifactsRegistrySpec = (AzureArtifactsRegistrySpec) spec;
    AzureArtifactsArtifactTriggerSpec azureArtifactsArtifactTriggerSpec = new AzureArtifactsArtifactTriggerSpec();
    azureArtifactsArtifactTriggerSpec.setConnectorRef(azureArtifactsRegistrySpec.getConnectorRef());
    azureArtifactsArtifactTriggerSpec.setEventConditions(azureArtifactsRegistrySpec.getEventConditions()
                                                             .stream()
                                                             .map(this::toTriggerCondition)
                                                             .collect(Collectors.toList()));
    azureArtifactsArtifactTriggerSpec.setJexlCondition(azureArtifactsRegistrySpec.getJexlCondition());
    azureArtifactsArtifactTriggerSpec.setMetaDataConditions(azureArtifactsRegistrySpec.fetchMetaDataConditions()
                                                                .stream()
                                                                .map(this::toTriggerCondition)
                                                                .collect(Collectors.toList()));
    azureArtifactsArtifactTriggerSpec.setVersion(azureArtifactsRegistrySpec.getVersion());
    azureArtifactsArtifactTriggerSpec.setFeed(azureArtifactsRegistrySpec.getFeed());
    azureArtifactsArtifactTriggerSpec.setPackageName(azureArtifactsRegistrySpec.getPackageName());
    azureArtifactsArtifactTriggerSpec.setPackageType(azureArtifactsRegistrySpec.getPackageType());
    azureArtifactsArtifactTriggerSpec.setProject(azureArtifactsRegistrySpec.getProject());
    azureArtifactsArtifactTriggerSpec.setVersionRegex(azureArtifactsRegistrySpec.getVersionRegex());
    return azureArtifactsArtifactTriggerSpec;
  }

  CustomArtifactTriggerSpec toCustomArtifactTriggerSpec(ArtifactTypeSpec spec) {
    io.harness.ngtriggers.beans.source.artifact.CustomArtifactSpec customArtifactSpec =
        (io.harness.ngtriggers.beans.source.artifact.CustomArtifactSpec) spec;
    CustomArtifactTriggerSpec customArtifactTriggerSpec = new CustomArtifactTriggerSpec();
    customArtifactTriggerSpec.setArtifactsArrayPath(customArtifactSpec.getArtifactsArrayPath());
    customArtifactTriggerSpec.setEventConditions(
        customArtifactSpec.getEventConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    customArtifactTriggerSpec.setJexlCondition(customArtifactSpec.getJexlCondition());
    customArtifactTriggerSpec.setMetaDataConditions(customArtifactSpec.fetchMetaDataConditions()
                                                        .stream()
                                                        .map(this::toTriggerCondition)
                                                        .collect(Collectors.toList()));
    customArtifactTriggerSpec.setVersion(customArtifactSpec.getVersion());
    customArtifactTriggerSpec.setVersionPath(customArtifactSpec.getVersionPath());
    customArtifactTriggerSpec.setScript(customArtifactSpec.getScript());
    customArtifactTriggerSpec.setInputs(
        customArtifactSpec.getInputs().stream().map(this::toApiNGVariable).collect(Collectors.toList()));
    customArtifactTriggerSpec.setMetadata(customArtifactSpec.getMetadata());
    return customArtifactTriggerSpec;
  }

  io.harness.spec.server.pipeline.v1.model.NGVariable toApiNGVariable(NGVariable variable) {
    switch (variable.getType()) {
      case NUMBER:
        NumberNGVariable numberNGVariable1 = (NumberNGVariable) variable;
        io.harness.spec.server.pipeline.v1.model.NumberNGVariable numberNGVariable =
            new io.harness.spec.server.pipeline.v1.model.NumberNGVariable();
        numberNGVariable.setType(io.harness.spec.server.pipeline.v1.model.NGVariable.TypeEnum.NUMBER);
        numberNGVariable.setName(numberNGVariable1.getName());
        numberNGVariable.setMetadata(numberNGVariable1.getMetadata());
        numberNGVariable.setValue(numberNGVariable1.getDefaultValue());
        numberNGVariable.setRequired(numberNGVariable1.isRequired());
        numberNGVariable.setDescription(numberNGVariable1.getDescription());
        numberNGVariable.setDefaultValue(numberNGVariable1.getDefaultValue());
        return numberNGVariable;
      case STRING:
        StringNGVariable stringNGVariable1 = (StringNGVariable) variable;
        io.harness.spec.server.pipeline.v1.model.StringNGVariable stringNGVariable =
            new io.harness.spec.server.pipeline.v1.model.StringNGVariable();
        stringNGVariable.setType(io.harness.spec.server.pipeline.v1.model.NGVariable.TypeEnum.STRING);
        stringNGVariable.setName(stringNGVariable1.getName());
        stringNGVariable.setMetadata(stringNGVariable1.getMetadata());
        stringNGVariable.setValue(stringNGVariable1.getDefaultValue());
        stringNGVariable.setRequired(stringNGVariable1.isRequired());
        stringNGVariable.setDescription(stringNGVariable1.getDescription());
        stringNGVariable.setDefaultValue(stringNGVariable1.getDefaultValue());
        return stringNGVariable;
      default:
        throw new InvalidRequestException("Variable Type " + variable.getType() + " is invalid");
    }
  }

  DockerRegistryArtifactTriggerSpec toDockerRegistryArtifactTriggerSpec(ArtifactTypeSpec spec) {
    DockerRegistrySpec dockerRegistrySpec = (DockerRegistrySpec) spec;
    DockerRegistryArtifactTriggerSpec dockerRegistryArtifactTriggerSpec = new DockerRegistryArtifactTriggerSpec();
    dockerRegistryArtifactTriggerSpec.setConnectorRef(dockerRegistrySpec.getConnectorRef());
    dockerRegistryArtifactTriggerSpec.setEventConditions(
        dockerRegistrySpec.getEventConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    dockerRegistryArtifactTriggerSpec.setJexlCondition(dockerRegistrySpec.getJexlCondition());
    dockerRegistryArtifactTriggerSpec.setTag(dockerRegistrySpec.getTag());
    dockerRegistryArtifactTriggerSpec.setImagePath(dockerRegistrySpec.getImagePath());
    dockerRegistryArtifactTriggerSpec.setMetaDataConditions(dockerRegistrySpec.fetchMetaDataConditions()
                                                                .stream()
                                                                .map(this::toTriggerCondition)
                                                                .collect(Collectors.toList()));
    return dockerRegistryArtifactTriggerSpec;
  }

  GithubPackageRegistryArtifactTriggerSpec toGithubPackageRegistryArtifactTriggerSpec(ArtifactTypeSpec spec) {
    GithubPackagesSpec githubPackagesSpec = (GithubPackagesSpec) spec;
    GithubPackageRegistryArtifactTriggerSpec githubPackageRegistryArtifactTriggerSpec =
        new GithubPackageRegistryArtifactTriggerSpec();
    githubPackageRegistryArtifactTriggerSpec.setConnectorRef(githubPackagesSpec.getConnectorRef());
    githubPackageRegistryArtifactTriggerSpec.setPackageName(githubPackagesSpec.getPackageName());
    githubPackageRegistryArtifactTriggerSpec.setPackageType(githubPackagesSpec.getPackageType());
    githubPackageRegistryArtifactTriggerSpec.setEventConditions(
        githubPackagesSpec.getEventConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    githubPackageRegistryArtifactTriggerSpec.setOrg(githubPackagesSpec.getOrg());
    githubPackageRegistryArtifactTriggerSpec.setMetaDataConditions(githubPackagesSpec.fetchMetaDataConditions()
                                                                       .stream()
                                                                       .map(this::toTriggerCondition)
                                                                       .collect(Collectors.toList()));
    githubPackageRegistryArtifactTriggerSpec.setJexlCondition(githubPackagesSpec.getJexlCondition());
    return githubPackageRegistryArtifactTriggerSpec;
  }

  Nexus2RegistryArtifactTriggerSpec toNexus2RegistryArtifactTriggerSpec(ArtifactTypeSpec spec) {
    Nexus2RegistrySpec nexus2RegistrySpec = (Nexus2RegistrySpec) spec;
    Nexus2RegistryArtifactTriggerSpec nexus2RegistryArtifactTriggerSpec = new Nexus2RegistryArtifactTriggerSpec();
    nexus2RegistryArtifactTriggerSpec.setArtifactId(nexus2RegistrySpec.getArtifactId());
    nexus2RegistryArtifactTriggerSpec.setConnectorRef(nexus2RegistrySpec.getConnectorRef());
    nexus2RegistryArtifactTriggerSpec.setEventConditions(
        nexus2RegistrySpec.getEventConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    nexus2RegistryArtifactTriggerSpec.setJexlCondition(nexus2RegistrySpec.getJexlCondition());
    nexus2RegistryArtifactTriggerSpec.setMetaDataConditions(nexus2RegistrySpec.fetchMetaDataConditions()
                                                                .stream()
                                                                .map(this::toTriggerCondition)
                                                                .collect(Collectors.toList()));
    nexus2RegistryArtifactTriggerSpec.setClassifier(nexus2RegistrySpec.getClassifier());
    nexus2RegistryArtifactTriggerSpec.setExtension(nexus2RegistrySpec.getExtension());
    nexus2RegistryArtifactTriggerSpec.setGroupId(nexus2RegistrySpec.getGroupId());
    nexus2RegistryArtifactTriggerSpec.setPackageName(nexus2RegistrySpec.getPackageName());
    nexus2RegistryArtifactTriggerSpec.setRepositoryFormat(nexus2RegistrySpec.getRepositoryFormat());
    nexus2RegistryArtifactTriggerSpec.setRepositoryName(nexus2RegistrySpec.getRepositoryName());
    nexus2RegistryArtifactTriggerSpec.setRepositoryUrl(nexus2RegistrySpec.getRepositoryUrl());
    return nexus2RegistryArtifactTriggerSpec;
  }

  Nexus3RegistryArtifactTriggerSpec toNexus3RegistryArtifactTriggerSpec(ArtifactTypeSpec spec) {
    NexusRegistrySpec nexusRegistrySpec = (NexusRegistrySpec) spec;
    Nexus3RegistryArtifactTriggerSpec nexus3RegistryArtifactTriggerSpec = new Nexus3RegistryArtifactTriggerSpec();
    nexus3RegistryArtifactTriggerSpec.setArtifactId(nexusRegistrySpec.getArtifactId());
    nexus3RegistryArtifactTriggerSpec.setExtension(nexusRegistrySpec.getExtension());
    nexus3RegistryArtifactTriggerSpec.setConnectorRef(nexusRegistrySpec.getConnectorRef());
    nexus3RegistryArtifactTriggerSpec.setPackageName(nexusRegistrySpec.getPackageName());
    nexus3RegistryArtifactTriggerSpec.setEventConditions(
        nexusRegistrySpec.getEventConditions().stream().map(this::toTriggerCondition).collect(Collectors.toList()));
    nexus3RegistryArtifactTriggerSpec.setClassifier(nexusRegistrySpec.getClassifier());
    nexus3RegistryArtifactTriggerSpec.setGroup(nexusRegistrySpec.getGroup());
    nexus3RegistryArtifactTriggerSpec.setGroupId(nexusRegistrySpec.getGroupId());
    nexus3RegistryArtifactTriggerSpec.setImagePath(nexusRegistrySpec.getImagePath());
    nexus3RegistryArtifactTriggerSpec.setJexlCondition(nexusRegistrySpec.getJexlCondition());
    nexus3RegistryArtifactTriggerSpec.setMetaDataConditions(nexusRegistrySpec.fetchMetaDataConditions()
                                                                .stream()
                                                                .map(this::toTriggerCondition)
                                                                .collect(Collectors.toList()));
    nexus3RegistryArtifactTriggerSpec.setTag(nexusRegistrySpec.getTag());
    nexus3RegistryArtifactTriggerSpec.setRepositoryUrl(nexusRegistrySpec.getRepositoryUrl());
    nexus3RegistryArtifactTriggerSpec.setRepositoryFormat(nexusRegistrySpec.getRepositoryFormat());
    nexus3RegistryArtifactTriggerSpec.setRepository(nexusRegistrySpec.getRepository());
    return nexus3RegistryArtifactTriggerSpec;
  }

  ArtifactoryRegistryArtifactTriggerSpec toArtifactoryRegistryArtifactTriggerSpec(ArtifactTypeSpec spec) {
    ArtifactoryRegistrySpec artifactoryRegistrySpec = (ArtifactoryRegistrySpec) spec;
    ArtifactoryRegistryArtifactTriggerSpec artifactTriggerSpec = new ArtifactoryRegistryArtifactTriggerSpec();
    artifactTriggerSpec.setArtifactDirectory(artifactoryRegistrySpec.getArtifactDirectory());
    artifactTriggerSpec.setArtifactPath(artifactoryRegistrySpec.getArtifactPath());
    artifactTriggerSpec.setArtifactFilter(artifactoryRegistrySpec.getArtifactFilter());
    artifactTriggerSpec.setRepositoryUrl(artifactoryRegistrySpec.getRepositoryUrl());
    artifactTriggerSpec.setConnectorRef(artifactoryRegistrySpec.getConnectorRef());
    artifactTriggerSpec.setRepositoryFormat(artifactoryRegistrySpec.getRepositoryFormat());
    artifactTriggerSpec.setMetaDataConditions(artifactoryRegistrySpec.fetchMetaDataConditions()
                                                  .stream()
                                                  .map(this::toTriggerCondition)
                                                  .collect(Collectors.toList()));
    artifactTriggerSpec.setJexlCondition(artifactoryRegistrySpec.getJexlCondition());
    artifactTriggerSpec.setEventConditions(artifactoryRegistrySpec.getEventConditions()
                                               .stream()
                                               .map(this::toTriggerCondition)
                                               .collect(Collectors.toList()));
    artifactTriggerSpec.setRepository(artifactoryRegistrySpec.getRepository());
    return artifactTriggerSpec;
  }

  GoogleCloudStorageArtifactTriggerSpec toGoogleCloudStorageArtifactTriggerSpec(ArtifactTypeSpec spec) {
    GoolgeCloudStorageRegistrySpec goolgeCloudStorageRegistrySpec = (GoolgeCloudStorageRegistrySpec) spec;
    GoogleCloudStorageArtifactTriggerSpec googleCloudStorageArtifactTriggerSpec =
        new GoogleCloudStorageArtifactTriggerSpec();
    googleCloudStorageArtifactTriggerSpec.setArtifactPath(goolgeCloudStorageRegistrySpec.getArtifactPath());
    googleCloudStorageArtifactTriggerSpec.setConnectorRef(goolgeCloudStorageRegistrySpec.getConnectorRef());
    googleCloudStorageArtifactTriggerSpec.setEventConditions(goolgeCloudStorageRegistrySpec.getEventConditions()
                                                                 .stream()
                                                                 .map(this::toTriggerCondition)
                                                                 .collect(Collectors.toList()));
    googleCloudStorageArtifactTriggerSpec.setMetaDataConditions(goolgeCloudStorageRegistrySpec.fetchMetaDataConditions()
                                                                    .stream()
                                                                    .map(this::toTriggerCondition)
                                                                    .collect(Collectors.toList()));
    googleCloudStorageArtifactTriggerSpec.setProject(goolgeCloudStorageRegistrySpec.getProject());
    googleCloudStorageArtifactTriggerSpec.setBucket(goolgeCloudStorageRegistrySpec.getBucket());
    googleCloudStorageArtifactTriggerSpec.setJexlCondition(goolgeCloudStorageRegistrySpec.getJexlCondition());
    return googleCloudStorageArtifactTriggerSpec;
  }

  ArtifactTriggerSpec toArtifactTriggerSpec(ArtifactTypeSpec spec) {
    switch (spec.fetchBuildType()) {
      case GOOGLE_ARTIFACT_REGISTRY:
        GoogleArtifactRegistryArtifactSpec googleArtifactRegistryArtifactSpec =
            new GoogleArtifactRegistryArtifactSpec();
        googleArtifactRegistryArtifactSpec.setType(
            io.harness.spec.server.pipeline.v1.model.ArtifactType.GOOGLEARTIFACTREGISTRY);
        googleArtifactRegistryArtifactSpec.setSpec(toGoogleArtifactRegistryArtifactTriggerSpec(spec));
        return googleArtifactRegistryArtifactSpec;
      case ACR:
        AcrArtifactSpec acrArtifactSpec = new AcrArtifactSpec();
        acrArtifactSpec.setType(io.harness.spec.server.pipeline.v1.model.ArtifactType.ACR);
        acrArtifactSpec.setSpec(toAcrArtifactTriggerSpec(spec));
        return acrArtifactSpec;
      case ECR:
        EcrArtifactSpec ecrArtifactSpec = new EcrArtifactSpec();
        ecrArtifactSpec.setType(io.harness.spec.server.pipeline.v1.model.ArtifactType.ECR);
        ecrArtifactSpec.setSpec(toEcrArtifactTriggerSpec(spec));
        return ecrArtifactSpec;
      case BAMBOO:
        BambooArtifactSpec bambooArtifactSpec = new BambooArtifactSpec();
        bambooArtifactSpec.setType(io.harness.spec.server.pipeline.v1.model.ArtifactType.BAMBOO);
        bambooArtifactSpec.setSpec(toBambooArtifactTriggerSpec(spec));
        return bambooArtifactSpec;
      case JENKINS:
        JenkinsArtifactSpec jenkinsArtifactSpec = new JenkinsArtifactSpec();
        jenkinsArtifactSpec.setType(io.harness.spec.server.pipeline.v1.model.ArtifactType.JENKINS);
        jenkinsArtifactSpec.setSpec(toJenkinsArtifactTriggerSpec(spec));
        return jenkinsArtifactSpec;
      case GCR:
        GcrArtifactSpec gcrArtifactSpec = new GcrArtifactSpec();
        gcrArtifactSpec.setType(io.harness.spec.server.pipeline.v1.model.ArtifactType.GCR);
        gcrArtifactSpec.setSpec(toGcrArtifactTriggerSpec(spec));
        return gcrArtifactSpec;
      case AMI:
        AmazonMachineImageArtifactSpec amiArtifactSpec = new AmazonMachineImageArtifactSpec();
        amiArtifactSpec.setType(io.harness.spec.server.pipeline.v1.model.ArtifactType.AMAZONMACHINEIMAGE);
        amiArtifactSpec.setSpec(toAmazonMachineImageArtifactTriggerSpec(spec));
        return amiArtifactSpec;
      case AMAZON_S3:
        AmazonS3ArtifactSpec amazonS3ArtifactSpec = new AmazonS3ArtifactSpec();
        amazonS3ArtifactSpec.setType(io.harness.spec.server.pipeline.v1.model.ArtifactType.AMAZONS3);
        amazonS3ArtifactSpec.setSpec(toAmazonS3ArtifactTriggerSpec(spec));
        return amazonS3ArtifactSpec;
      case AZURE_ARTIFACTS:
        AzureArtifactsArtifactSpec azureArtifactsArtifactSpec = new AzureArtifactsArtifactSpec();
        azureArtifactsArtifactSpec.setType(io.harness.spec.server.pipeline.v1.model.ArtifactType.AMAZONMACHINEIMAGE);
        azureArtifactsArtifactSpec.setSpec(toAzureArtifactsArtifactTriggerSpec(spec));
        return azureArtifactsArtifactSpec;
      case CUSTOM_ARTIFACT:
        CustomArtifactSpec customArtifactSpec = new CustomArtifactSpec();
        customArtifactSpec.setType(io.harness.spec.server.pipeline.v1.model.ArtifactType.CUSTOMARTIFACT);
        customArtifactSpec.setSpec(toCustomArtifactTriggerSpec(spec));
        return customArtifactSpec;
      case DOCKER_REGISTRY:
        DockerRegistryArtifactSpec dockerRegistryArtifactSpec = new DockerRegistryArtifactSpec();
        dockerRegistryArtifactSpec.setType(io.harness.spec.server.pipeline.v1.model.ArtifactType.DOCKERREGISTRY);
        dockerRegistryArtifactSpec.setSpec(toDockerRegistryArtifactTriggerSpec(spec));
        return dockerRegistryArtifactSpec;
      case GITHUB_PACKAGES:
        GithubPackageRegistryArtifactSpec githubPackageRegistryArtifactSpec = new GithubPackageRegistryArtifactSpec();
        githubPackageRegistryArtifactSpec.setType(
            io.harness.spec.server.pipeline.v1.model.ArtifactType.GITHUBPACKAGEREGISTRY);
        githubPackageRegistryArtifactSpec.setSpec(toGithubPackageRegistryArtifactTriggerSpec(spec));
        return githubPackageRegistryArtifactSpec;
      case NEXUS2_REGISTRY:
        Nexus2RegistryArtifactSpec nexus2RegistryArtifactSpec = new Nexus2RegistryArtifactSpec();
        nexus2RegistryArtifactSpec.setType(io.harness.spec.server.pipeline.v1.model.ArtifactType.NEXUS2REGISTRY);
        nexus2RegistryArtifactSpec.setSpec(toNexus2RegistryArtifactTriggerSpec(spec));
        return nexus2RegistryArtifactSpec;
      case NEXUS3_REGISTRY:
        Nexus3RegistryArtifactSpec nexus3RegistryArtifactSpec = new Nexus3RegistryArtifactSpec();
        nexus3RegistryArtifactSpec.setType(io.harness.spec.server.pipeline.v1.model.ArtifactType.NEXUS3REGISTRY);
        nexus3RegistryArtifactSpec.setSpec(toNexus3RegistryArtifactTriggerSpec(spec));
        return nexus3RegistryArtifactSpec;
      case ARTIFACTORY_REGISTRY:
        ArtifactoryRegistryArtifactSpec artifactoryRegistryArtifactSpec = new ArtifactoryRegistryArtifactSpec();
        artifactoryRegistryArtifactSpec.setType(
            io.harness.spec.server.pipeline.v1.model.ArtifactType.GITHUBPACKAGEREGISTRY);
        artifactoryRegistryArtifactSpec.setSpec(toArtifactoryRegistryArtifactTriggerSpec(spec));
        return artifactoryRegistryArtifactSpec;
      case GOOGLE_CLOUD_STORAGE:
        GoogleCloudStorageArtifactSpec googleCloudStorageArtifactSpec = new GoogleCloudStorageArtifactSpec();
        googleCloudStorageArtifactSpec.setType(
            io.harness.spec.server.pipeline.v1.model.ArtifactType.GITHUBPACKAGEREGISTRY);
        googleCloudStorageArtifactSpec.setSpec(toGoogleCloudStorageArtifactTriggerSpec(spec));
        return googleCloudStorageArtifactSpec;
      default:
        throw new InvalidRequestException("Artifact Trigger Type " + spec.fetchBuildType() + " is invalid");
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
}
