/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.filestore.utils;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.ng.core.EntityDetail.EntityDetailKeys;
import static io.harness.ng.core.Resource.ResourceKeys;
import static io.harness.ng.core.entitysetupusage.entity.EntitySetupUsage.EntitySetupUsageKeys;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.exception.UnexpectedException;
import io.harness.ng.core.beans.SearchPageParams;
import io.harness.ng.core.entities.NGFile;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.ng.core.entitysetupusage.service.EntitySetupUsageService;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

@OwnedBy(CDP)
@Slf4j
@Singleton
public class FileReferencedByHelper {
  protected static final String REFFERED_BY_IDENTIFIER_KEY =
      EntitySetupUsageKeys.referredByEntity + "." + EntityDetailKeys.entityRef + "." + ResourceKeys.identifier;
  private final EntitySetupUsageService entitySetupUsageService;

  @Inject
  public FileReferencedByHelper(EntitySetupUsageService entitySetupUsageService) {
    this.entitySetupUsageService = entitySetupUsageService;
  }

  public boolean isFileReferencedByOtherEntities(NGFile file) {
    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRefFromEntityIdentifiers(
        file.getIdentifier(), file.getAccountIdentifier(), file.getOrgIdentifier(), file.getProjectIdentifier());
    String referredEntityFQN = identifierRef.getFullyQualifiedName();
    try {
      return entitySetupUsageService.isEntityReferenced(
          file.getAccountIdentifier(), referredEntityFQN, EntityType.FILES);
    } catch (Exception ex) {
      log.error("Encountered exception while requesting the Entity Reference records of [{}], with exception.",
          file.getIdentifier(), ex);
      throw new UnexpectedException("Error while verifying file is referenced by other entities.", ex);
    }
  }

  public Page<EntitySetupUsageDTO> getReferencedBy(SearchPageParams pageParams, NGFile file, EntityType entityType) {
    IdentifierRef identifierRef = IdentifierRef.builder()
                                      .accountIdentifier(file.getAccountIdentifier())
                                      .orgIdentifier(file.getOrgIdentifier())
                                      .projectIdentifier(file.getProjectIdentifier())
                                      .identifier(file.getIdentifier())
                                      .build();
    String referredEntityFQN = identifierRef.getFullyQualifiedName();
    return entitySetupUsageService.listAllEntityUsage(pageParams.getPage(), pageParams.getSize(),
        file.getAccountIdentifier(), referredEntityFQN, EntityType.FILES, entityType, pageParams.getSearchTerm(),
        Sort.by(Sort.Direction.ASC, REFFERED_BY_IDENTIFIER_KEY));
  }
}
