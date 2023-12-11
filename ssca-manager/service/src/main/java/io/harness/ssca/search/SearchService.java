/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.search;

import io.harness.ssca.entities.ArtifactEntity;
import io.harness.ssca.entities.NormalizedSBOMComponentEntity;

import co.elastic.clients.elasticsearch._types.Result;
import java.util.List;

public interface SearchService {
  Result saveArtifact(ArtifactEntity artifactEntity);

  Result saveComponent(NormalizedSBOMComponentEntity component);

  boolean bulkSaveComponents(String accountId, List<NormalizedSBOMComponentEntity> components);

  boolean bulkSaveArtifacts(String accountId, List<ArtifactEntity> artifactEntities);

  boolean deleteMigrationIndex();
}
