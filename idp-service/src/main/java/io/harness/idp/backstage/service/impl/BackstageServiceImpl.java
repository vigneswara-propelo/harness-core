/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.backstage.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.idp.common.JacksonUtils.convert;
import static io.harness.idp.common.JacksonUtils.readValueForObject;
import static io.harness.idp.common.YamlUtils.writeObjectAsYaml;
import static io.harness.remote.client.NGRestUtils.getGeneralResponse;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.clients.BackstageResourceClient;
import io.harness.exception.UnexpectedException;
import io.harness.idp.backstage.entities.BackstageCatalogEntity;
import io.harness.idp.backstage.repositories.BackstageCatalogEntityRepository;
import io.harness.idp.backstage.service.BackstageService;
import io.harness.idp.events.producers.IdpEntityCrudStreamProducer;
import io.harness.idp.events.producers.IdpServiceMiscRedisProducer;
import io.harness.idp.namespace.service.NamespaceService;
import io.harness.spec.server.idp.v1.model.BackstageHarnessSyncRequest;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
@OwnedBy(HarnessTeam.IDP)
public class BackstageServiceImpl implements BackstageService {
  private static final String ENTITY_NOT_FOUND_MAPPED_ERROR = "HTTP Error Status (404 - Resource Not Found) received. ";

  @Inject NamespaceService namespaceService;
  @Inject BackstageResourceClient backstageResourceClient;
  @Inject BackstageCatalogEntityRepository backstageCatalogEntityRepository;
  @Inject IdpEntityCrudStreamProducer idpEntityCrudStreamProducer;
  @Inject IdpServiceMiscRedisProducer idpServiceMiscRedisProducer;

  @Override
  public void sync() {
    List<String> accountIdentifiers = namespaceService.getAccountIds();
    log.info("Fetched {} IDP active accounts for IdpCatalogEntitiesAsHarnessEntities sync", accountIdentifiers.size());
    accountIdentifiers.forEach(this::sync);
  }

  @Override
  public boolean sync(String accountIdentifier) {
    try {
      log.info("Syncing IDP catalog entities as Harness entities for accountIdentifier = {}", accountIdentifier);
      String url = String.format("%s/idp/api/catalog/entities", accountIdentifier);
      Object response = getGeneralResponse(backstageResourceClient.getCatalogEntities(url));
      List<BackstageCatalogEntity> backstageCatalogEntities = convert(response, BackstageCatalogEntity.class);
      syncInternal(
          accountIdentifier, "", BackstageHarnessSyncRequest.ActionEnum.UPSERT.value(), backstageCatalogEntities);
    } catch (Exception ex) {
      log.error("Error in IdpCatalogEntitiesAsHarnessEntities sync for accountIdentifier = {} Error = {}",
          accountIdentifier, ex.getMessage(), ex);
      return false;
    }
    return true;
  }

  @Override
  public boolean sync(String accountIdentifier, String entityUid, String action, String syncMode) {
    switch (BackstageHarnessSyncRequest.SyncModeEnum.fromValue(syncMode)) {
      case SYNC:
        return syncInSynchronousMode(accountIdentifier, entityUid, action);
      case ASYNC:
        return syncInAsynchronousMode(accountIdentifier, entityUid, action);
      default:
        throw new UnexpectedException("Unsupported sync mode for syncing IdpCatalogEntitiesAsHarnessEntities");
    }
  }

  private void syncInternal(String accountIdentifier, String entityUid, String action,
      List<BackstageCatalogEntity> backstageCatalogEntities) {
    log.info(
        "Fetched {} catalog entities in IdpCatalogEntitiesAsHarnessEntities sync for accountIdentifier = {} EntityUid = {} Action = {}",
        backstageCatalogEntities.size(), accountIdentifier, entityUid, action);
    prepareEntitiesForSave(accountIdentifier, backstageCatalogEntities);
    Map<BackstageCatalogEntity, String> entitiesActions = categorizeEntitiesActions(backstageCatalogEntities);
    Iterable<BackstageCatalogEntity> savedBackstageCatalogEntities =
        backstageCatalogEntityRepository.saveAll(backstageCatalogEntities);
    log.info(
        "Saved {} catalog entities into DB in IdpCatalogEntitiesAsHarnessEntities sync for accountIdentifier = {} EntityUid = {} Action = {}",
        backstageCatalogEntities.size(), accountIdentifier, entityUid, action);
    publishBackstageCatalogEntityChange(accountIdentifier, savedBackstageCatalogEntities, action);
    log.info("Synced IDP catalog entities as Harness entities for accountIdentifier = {} EntityUid = {} Action = {}",
        accountIdentifier, entityUid, action);
  }

  private boolean syncInSynchronousMode(String accountIdentifier, String entityUid, String action) {
    try {
      log.info("Syncing IDP catalog entities as Harness entities for accountIdentifier = {} EntityUid = {} Action = {}",
          accountIdentifier, entityUid, action);
      switch (BackstageHarnessSyncRequest.ActionEnum.fromValue(action)) {
        case CREATE:
        case UPDATE:
        case UPSERT:
          handleCreateOrUpdateOrUpsertAction(accountIdentifier, entityUid, action);
          break;
        case DELETE:
          handleDeleteAction(accountIdentifier, entityUid);
          break;
        default:
          throw new UnexpectedException(
              "Unsupported action for syncing IdpCatalogEntitiesAsHarnessEntities in synchronous mode");
      }
    } catch (Exception ex) {
      log.error(
          "Error in IdpCatalogEntitiesAsHarnessEntities sync for accountIdentifier = {} EntityUid = {} Action = {} Error = {}",
          accountIdentifier, entityUid, action, ex.getMessage(), ex);
      return false;
    }
    return true;
  }

  private boolean syncInAsynchronousMode(String accountIdentifier, String entityUid, String action) {
    idpServiceMiscRedisProducer.publishIDPCatalogEntitiesSyncCaptureToRedis(accountIdentifier, entityUid, action);
    return true;
  }

  private void prepareEntitiesForSave(String accountIdentifier, List<BackstageCatalogEntity> backstageCatalogEntities) {
    backstageCatalogEntities.forEach(backstageCatalogEntity -> {
      String entityUid = getEntityUniqueId(backstageCatalogEntity);
      Optional<BackstageCatalogEntity> optionalBackstageCatalogEntity =
          backstageCatalogEntityRepository.findByAccountIdentifierAndEntityUid(accountIdentifier, entityUid);
      optionalBackstageCatalogEntity.ifPresent(backstageCatalogEntityExisting -> {
        backstageCatalogEntity.setId(backstageCatalogEntityExisting.getId());
        backstageCatalogEntity.setCreatedAt(backstageCatalogEntityExisting.getCreatedAt());
      });
      backstageCatalogEntity.setAccountIdentifier(accountIdentifier);
      backstageCatalogEntity.setEntityUid(entityUid);
      backstageCatalogEntity.setYaml(writeObjectAsYaml(backstageCatalogEntity));
    });
  }

  private Map<BackstageCatalogEntity, String> categorizeEntitiesActions(
      List<BackstageCatalogEntity> backstageCatalogEntities) {
    Map<BackstageCatalogEntity, String> entitiesActions = new HashMap<>();
    backstageCatalogEntities.forEach(backstageCatalogEntity -> {
      if (backstageCatalogEntity.getCreatedAt() == 0) {
        entitiesActions.put(backstageCatalogEntity, BackstageHarnessSyncRequest.ActionEnum.CREATE.value());
      } else {
        entitiesActions.put(backstageCatalogEntity, BackstageHarnessSyncRequest.ActionEnum.UPDATE.value());
      }
    });
    return entitiesActions;
  }

  private String getEntityUniqueId(BackstageCatalogEntity backstageCatalogEntity) {
    String namespace = isEmpty(backstageCatalogEntity.getMetadata().getNamespace())
        ? "default"
        : backstageCatalogEntity.getMetadata().getNamespace();
    return backstageCatalogEntity.getKind() + "/" + namespace + "/" + backstageCatalogEntity.getMetadata().getName();
  }

  private void publishBackstageCatalogEntityChange(
      String accountIdentifier, Iterable<BackstageCatalogEntity> backstageCatalogEntities, String action) {
    backstageCatalogEntities.forEach(backstageCatalogEntity -> {
      boolean producerResult = idpEntityCrudStreamProducer.publishBackstageCatalogEntityChangeEventToRedis(
          accountIdentifier, backstageCatalogEntity.getEntityUid(), action);
      if (!producerResult) {
        log.error(
            "Error in producing entity change event for backstage catalog. AccountIdentifier = {} BackstageCatalogEntityUid = {} Action = {}",
            accountIdentifier, backstageCatalogEntity.getEntityUid(), action);
      }
    });
  }

  private void handleCreateOrUpdateOrUpsertAction(String accountIdentifier, String entityUid, String action) {
    Object response = null;
    try {
      response = getGeneralResponse(backstageResourceClient.getCatalogEntityByName(accountIdentifier, entityUid));
    } catch (Exception ex) {
      log.error("Error in fetching catalog entity by name for account = {} entityUid = {} Error = {}",
          accountIdentifier, entityUid, ex.getMessage(), ex);
      if (ex.getMessage().equals(ENTITY_NOT_FOUND_MAPPED_ERROR)) {
        syncInSynchronousMode(accountIdentifier, entityUid, BackstageHarnessSyncRequest.ActionEnum.DELETE.value());
        return;
      }
    }
    BackstageCatalogEntity backstageCatalogEntity = readValueForObject(response, BackstageCatalogEntity.class);
    List<BackstageCatalogEntity> backstageCatalogEntities = Collections.singletonList(backstageCatalogEntity);
    syncInternal(accountIdentifier, entityUid, action, backstageCatalogEntities);
  }

  private void handleDeleteAction(String accountIdentifier, String entityUid) {
    log.info(
        "Delete action received in IdpCatalogEntitiesAsHarnessEntities sync for accountIdentifier = {}, entityUid = {}",
        accountIdentifier, entityUid);
    Optional<BackstageCatalogEntity> optionalBackstageCatalogEntity =
        backstageCatalogEntityRepository.findByAccountIdentifierAndEntityUid(accountIdentifier, entityUid);
    optionalBackstageCatalogEntity.ifPresent(backstageCatalogEntityExisting -> {
      log.info("Found BackstageCatalogEntity for delete, deleting it. AccountIdentifier = {}, entityUid = {}",
          accountIdentifier, entityUid);
      backstageCatalogEntityRepository.delete(backstageCatalogEntityExisting);
    });
  }
}
