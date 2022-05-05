/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.filestore.api;

import io.harness.EntityType;
import io.harness.ng.core.beans.SearchPageParams;
import io.harness.ng.core.entities.NGFile;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;

import org.springframework.data.domain.Page;

public interface FileReferenceService {
  /**
   * Check whether the file is references by other entities
   *
   * @param file the file
   * @return if the file is referenced by other entities
   */
  boolean isFileReferencedByOtherEntities(NGFile file);

  /**
   * The list of entities where the file is referenced
   *
   * @param pageParams the search page params
   * @param file the file to check reference
   * @param entityType the entity type
   * @return the list of usage DTOs where the file is referenced by
   */
  Page<EntitySetupUsageDTO> getReferencedBy(SearchPageParams pageParams, NGFile file, EntityType entityType);
}
