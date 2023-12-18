/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.events.producers;

import static io.harness.annotations.dev.HarnessTeam.IDP;
import static io.harness.eventsframework.EventsFrameworkConstants.IDP_CATALOG_ENTITIES_SYNC_CAPTURE_EVENT;
import static io.harness.eventsframework.EventsFrameworkConstants.IDP_MODULE_LICENSE_USAGE_CAPTURE_EVENT;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.CREATE_ACTION;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.eventsframework.schemas.idp.IdpCatalogEntitiesSyncCaptureEvent;
import io.harness.eventsframework.schemas.idp.IdpLicenseUsageCaptureEvent;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(IDP)
public class IdpServiceMiscRedisProducer {
  private final Producer idpModuleLicenseUsageCaptureEventProducer;
  private final Producer idpCatalogEntitiesSyncCaptureEventProducer;

  @Inject
  public IdpServiceMiscRedisProducer(
      @Named(IDP_MODULE_LICENSE_USAGE_CAPTURE_EVENT) Producer idpModuleLicenseUsageCaptureEventProducer,
      @Named(IDP_CATALOG_ENTITIES_SYNC_CAPTURE_EVENT) Producer idpCatalogEntitiesSyncCaptureEventProducer) {
    this.idpModuleLicenseUsageCaptureEventProducer = idpModuleLicenseUsageCaptureEventProducer;
    this.idpCatalogEntitiesSyncCaptureEventProducer = idpCatalogEntitiesSyncCaptureEventProducer;
  }

  public void publishIDPLicenseUsageUserCaptureDTOToRedis(
      String accountIdentifier, String userIdentifier, String email, String userName, long accessedAt) {
    try {
      String eventId = idpModuleLicenseUsageCaptureEventProducer.send(
          Message.newBuilder()
              .putAllMetadata(
                  Map.of("accountIdentifier", accountIdentifier, EventsFrameworkMetadataConstants.ENTITY_TYPE,
                      IDP_MODULE_LICENSE_USAGE_CAPTURE_EVENT, EventsFrameworkMetadataConstants.ACTION, CREATE_ACTION))
              .setData(
                  getIdpLicenseUsageCaptureEventData(accountIdentifier, userIdentifier, email, userName, accessedAt))
              .build());
      log.info(
          "Produced event {} to redis for IDPLicenseUsageUserCapture accountIdentifier {} userIdentifier {}, email {}, userName {}",
          eventId, accountIdentifier, userIdentifier, email, userName);
    } catch (Exception ex) {
      log.error(
          "Failed to produce event to redis for IDPLicenseUsageUserCapture accountIdentifier {} userIdentifier {}, email {}, userName {}. Error = {}",
          accountIdentifier, userIdentifier, email, userName, ex.getMessage(), ex);
      throw ex;
    }
  }

  public void publishIDPCatalogEntitiesSyncCaptureToRedis(String accountIdentifier, String entityUid, String action) {
    try {
      String eventId = idpCatalogEntitiesSyncCaptureEventProducer.send(
          Message.newBuilder()
              .putAllMetadata(
                  Map.of("accountIdentifier", accountIdentifier, EventsFrameworkMetadataConstants.ENTITY_TYPE,
                      IDP_CATALOG_ENTITIES_SYNC_CAPTURE_EVENT, EventsFrameworkMetadataConstants.ACTION, action))
              .setData(getIdpCatalogEntitiesSyncCaptureEventData(accountIdentifier, entityUid, action))
              .build());
      log.info(
          "Produced event {} to redis for IDPCatalogEntitiesSyncCapture accountIdentifier {} entityUid {}, action {}",
          eventId, accountIdentifier, entityUid, action);
    } catch (Exception ex) {
      log.error(
          "Failed to produce event to redis for IDPCatalogEntitiesSyncCapture accountIdentifier {} entityUid {}, action {}. Error = {}",
          accountIdentifier, entityUid, action, ex.getMessage(), ex);
      throw ex;
    }
  }

  private ByteString getIdpLicenseUsageCaptureEventData(
      String accountIdentifier, String userIdentifier, String email, String userName, long accessedAt) {
    return IdpLicenseUsageCaptureEvent.newBuilder()
        .setAccountIdentifier(accountIdentifier)
        .setUserIdentifier(userIdentifier)
        .setEmail(email)
        .setUserName(userName)
        .setAccessedAt(accessedAt)
        .build()
        .toByteString();
  }

  private ByteString getIdpCatalogEntitiesSyncCaptureEventData(
      String accountIdentifier, String entityUid, String action) {
    return IdpCatalogEntitiesSyncCaptureEvent.newBuilder()
        .setAccountIdentifier(accountIdentifier)
        .setEntityUid(entityUid)
        .setAction(action)
        .setSyncMode("sync")
        .build()
        .toByteString();
  }
}
