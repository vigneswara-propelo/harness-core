/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */
package io.harness.licensing.services;

import io.harness.ModuleType;
import io.harness.SecondaryEntitlement;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.licensing.beans.modules.DeveloperMappingDTO;
import io.harness.licensing.entities.developer.DeveloperMapping;
import io.harness.licensing.mappers.DeveloperMappingObjectConverter;
import io.harness.repositories.DeveloperMappingRepository;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.security.dto.Principal;
import io.harness.security.dto.UserPrincipal;

import com.google.inject.Inject;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.GTM)
@Slf4j
public class DeveloperMappingServiceImpl implements DeveloperMappingService {
  public static final String GLOBAL_ACCOUNT_ID = "__GLOBAL_ACCOUNT_ID__";
  private final DeveloperMappingRepository developerMappingRepository;
  private final DeveloperMappingObjectConverter developerMappingObjectConverter;

  @Inject
  public DeveloperMappingServiceImpl(DeveloperMappingRepository developerMappingRepository,
      DeveloperMappingObjectConverter developerMappingObjectConverter) {
    this.developerMappingRepository = developerMappingRepository;
    this.developerMappingObjectConverter = developerMappingObjectConverter;
  }

  @Override
  public List<DeveloperMappingDTO> getDeveloperMapping(String accountIdentifier) {
    List<DeveloperMapping> developerMappings = developerMappingRepository.findByAccountIdentifier(accountIdentifier);
    return developerMappings.stream().map(developerMappingObjectConverter::toDTO).collect(Collectors.toList());
  }

  @Override
  public DeveloperMappingDTO createDeveloperMapping(String accountIdentifier, DeveloperMappingDTO developerMappingDTO) {
    validateDTO(accountIdentifier, developerMappingDTO);
    DeveloperMapping developerMapping = developerMappingObjectConverter.toEntity(developerMappingDTO);
    DeveloperMapping savedDeveloperMapping = developerMappingRepository.save(developerMapping);
    return developerMappingObjectConverter.toDTO(savedDeveloperMapping);
  }

  @Override
  public DeveloperMappingDTO updateDeveloperMapping(String accountIdentifier, DeveloperMappingDTO developerMappingDTO) {
    validateAccountInfo(accountIdentifier, developerMappingDTO);
    if (developerMappingDTO.getId() == null) {
      throw new InvalidRequestException("Missing mandatory field of developer mapping id");
    }
    DeveloperMapping updatedDeveloperMapping =
        updateDeveloperMappingInDB(developerMappingObjectConverter.toEntity(developerMappingDTO));
    return developerMappingObjectConverter.toDTO(updatedDeveloperMapping);
  }

  private DeveloperMapping updateDeveloperMappingInDB(DeveloperMapping developerMapping) {
    Optional<DeveloperMapping> maybeDeveloperMapping = developerMappingRepository.findById(developerMapping.getId());
    if (!maybeDeveloperMapping.isPresent()) {
      throw new NotFoundException(
          String.format("Developer mapping with identifier [%s] not found", developerMapping.getId()));
    }
    DeveloperMapping existingDeveloperMapping = maybeDeveloperMapping.get();
    developerMapping.setId(existingDeveloperMapping.getId());
    developerMapping.setCreatedAt(existingDeveloperMapping.getCreatedAt());
    developerMapping.setCreatedBy(existingDeveloperMapping.getCreatedBy());
    developerMapping.setLastUpdatedBy(EmbeddedUser.builder().email(getEmailFromPrincipal()).build());
    developerMapping.setLastUpdatedAt(Instant.now().toEpochMilli());
    return developerMappingRepository.save(developerMapping);
  }

  private String getEmailFromPrincipal() {
    Principal principal = SourcePrincipalContextBuilder.getSourcePrincipal();
    String email = "";
    if (principal instanceof UserPrincipal) {
      email = ((UserPrincipal) principal).getEmail();
    }
    return email;
  }

  @Override
  public DeveloperMapping getModuleDefaultDeveloperToSecondaryEntitlementMapping(
      ModuleType moduleType, SecondaryEntitlement secondaryEntitlement) {
    return developerMappingRepository.findByAccountIdentifierAndModuleTypeAndSecondaryEntitlement(
        GLOBAL_ACCOUNT_ID, moduleType, secondaryEntitlement);
  }

  @Override
  public void deleteDeveloperMapping(String developerMappingId) throws InvalidRequestException {
    Optional<DeveloperMapping> maybeDeveloperMapping = developerMappingRepository.findById(developerMappingId);
    if (!maybeDeveloperMapping.isPresent()) {
      throw new NotFoundException(
          String.format("Developer mapping with identifier [%s] not found", developerMappingId));
    }
    developerMappingRepository.delete(maybeDeveloperMapping.get());
  }

  private void validateAccountInfo(String accountIdentifier, DeveloperMappingDTO developerMappingDTO) {
    if (!accountIdentifier.equals(developerMappingDTO.getAccountIdentifier())) {
      String errorMessage =
          String.format("Account Identifier: [%s] did not match with the developer mapping DTO account identifier [%s]",
              accountIdentifier, developerMappingDTO.getAccountIdentifier());
      log.error(errorMessage);
      throw new InvalidRequestException(errorMessage);
    }
  }

  private void validateDTO(String accountIdentifier, DeveloperMappingDTO developerMappingDTO) {
    validateAccountInfo(accountIdentifier, developerMappingDTO);
    DeveloperMapping existingDeveloperMapping =
        developerMappingRepository.findByAccountIdentifierAndModuleTypeAndSecondaryEntitlement(
            accountIdentifier, developerMappingDTO.getModuleType(), developerMappingDTO.getSecondaryEntitlement());
    if (existingDeveloperMapping != null) {
      throw new DuplicateFieldException(String.format(
          "Developer Mapping with accountIdentifier [%s], moduleType [%s] and secondary entitlement [%s] already exists",
          accountIdentifier, developerMappingDTO.getModuleType(), developerMappingDTO.getSecondaryEntitlement()));
    }
  }
}
