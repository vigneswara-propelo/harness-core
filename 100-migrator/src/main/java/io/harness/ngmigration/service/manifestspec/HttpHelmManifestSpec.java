/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service.manifestspec;

import static software.wings.ngmigration.NGMigrationEntityType.SERVICE;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.ngmigration.beans.NGYamlFile;
import io.harness.ngtriggers.beans.source.artifact.BuildStoreType;
import io.harness.ngtriggers.beans.source.artifact.HelmManifestSpec;
import io.harness.ngtriggers.beans.source.artifact.ManifestTypeSpec;
import io.harness.ngtriggers.beans.source.artifact.store.BuildStore;
import io.harness.ngtriggers.beans.source.artifact.store.HttpBuildStoreTypeSpec;
import io.harness.ngtriggers.beans.source.artifact.version.HelmVersion;

import software.wings.beans.Service;
import software.wings.beans.trigger.Trigger;
import software.wings.ngmigration.CgEntityId;
import software.wings.ngmigration.CgEntityNode;

import java.util.Map;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_MIGRATOR})
@OwnedBy(HarnessTeam.CDC)
public class HttpHelmManifestSpec implements ManifestSpecMapper {
  @Override
  public ManifestTypeSpec getTriggerSpec(Map<CgEntityId, CgEntityNode> entities, ConnectorInfoDTO helmConnector,
      Map<CgEntityId, NGYamlFile> migratedEntities, Trigger trigger) {
    Service service =
        (Service) entities
            .get(CgEntityId.builder().id(trigger.getManifestSelections().get(0).getServiceId()).type(SERVICE).build())
            .getEntity();
    return HelmManifestSpec.builder()
        .helmVersion(HelmVersion.fromString(service.getHelmVersion().toString()))
        .eventConditions(getEventConditions(trigger))
        .store(BuildStore.builder()
                   .type(BuildStoreType.HTTP)
                   .spec(HttpBuildStoreTypeSpec.builder().connectorRef(helmConnector.getIdentifier()).build())
                   .build())
        .build();
  }
}
