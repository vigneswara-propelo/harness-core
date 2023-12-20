/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service.manifestspec;

import static io.harness.delegate.beans.connector.ConnectorType.HTTP_HELM_REPO;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.delegate.beans.connector.ConnectorType;

import com.google.common.collect.ImmutableMap;
import java.util.Map;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_MIGRATOR})
@OwnedBy(HarnessTeam.CDC)
public class ManifestSpecFactory {
  private static final ManifestSpecMapper httpHelmMapper = new HttpHelmManifestSpec();
  private static final Map<ConnectorType, ManifestSpecMapper> MANIFEST_MAPPER =
      ImmutableMap.<ConnectorType, ManifestSpecMapper>builder().put(HTTP_HELM_REPO, httpHelmMapper).build();

  public static ManifestSpecMapper getManifestSpecMapper(ConnectorType connectorType) {
    return MANIFEST_MAPPER.get(connectorType);
  }
}
