/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.search.mapper;

import static io.harness.ssca.search.framework.Constants.SBOM_COMPONENT_ENTITY;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ssca.entities.NormalizedSBOMComponentEntity;
import io.harness.ssca.search.beans.RelationshipType;
import io.harness.ssca.search.entities.Component;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.SSCA)
@UtilityClass
public class ComponentMapper {
  public Component toComponent(String parent, NormalizedSBOMComponentEntity normalizedSBOMComponentEntity) {
    return Component.builder()
        .uuid(normalizedSBOMComponentEntity.getUuid())
        .orchestrationId(normalizedSBOMComponentEntity.getOrchestrationId())
        .sbomVersion(normalizedSBOMComponentEntity.getSbomVersion())
        .artifactUrl(normalizedSBOMComponentEntity.getArtifactUrl())
        .artifactId(normalizedSBOMComponentEntity.getArtifactId())
        .artifactName(normalizedSBOMComponentEntity.getArtifactName())
        .tags(normalizedSBOMComponentEntity.getTags())
        .createdOn(normalizedSBOMComponentEntity.getCreatedOn().toEpochMilli())
        .toolVersion(normalizedSBOMComponentEntity.getToolVersion())
        .toolName(normalizedSBOMComponentEntity.getToolName())
        .toolVendor(normalizedSBOMComponentEntity.getToolVendor())
        .packageId(normalizedSBOMComponentEntity.getPackageId())
        .packageName(normalizedSBOMComponentEntity.getPackageName())
        .packageDescription(normalizedSBOMComponentEntity.getPackageDescription())
        .packageLicense(normalizedSBOMComponentEntity.getPackageLicense())
        .packageSourceInfo(normalizedSBOMComponentEntity.getPackageSourceInfo())
        .packageVersion(normalizedSBOMComponentEntity.getPackageVersion())
        .packageOriginatorName(normalizedSBOMComponentEntity.getPackageOriginatorName())
        .originatorType(normalizedSBOMComponentEntity.getOriginatorType())
        .packageType(normalizedSBOMComponentEntity.getPackageType())
        .packageCpe(normalizedSBOMComponentEntity.getPackageCpe())
        .packageProperties(normalizedSBOMComponentEntity.getPackageProperties())
        .purl(normalizedSBOMComponentEntity.getPurl())
        .packageManager(normalizedSBOMComponentEntity.getPackageManager())
        .packageNamespace(normalizedSBOMComponentEntity.getPackageNamespace())
        .majorVersion(normalizedSBOMComponentEntity.getMajorVersion())
        .minorVersion(normalizedSBOMComponentEntity.getMinorVersion())
        .patchVersion(normalizedSBOMComponentEntity.getPatchVersion())
        .pipelineIdentifier(normalizedSBOMComponentEntity.getPipelineIdentifier())
        .projectIdentifier(normalizedSBOMComponentEntity.getProjectIdentifier())
        .orgIdentifier(normalizedSBOMComponentEntity.getOrgIdentifier())
        .sequenceId(normalizedSBOMComponentEntity.getSequenceId())
        .accountId(normalizedSBOMComponentEntity.getAccountId())
        .relation_type(RelationshipType.builder().name(SBOM_COMPONENT_ENTITY).parent(parent).build())
        .build();
  }
}
