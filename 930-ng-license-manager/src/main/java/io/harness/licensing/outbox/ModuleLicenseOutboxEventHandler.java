/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.licensing.outbox;

import static io.harness.licensing.ModuleLicenseConstants.MODULE_LICENSE_CREATED;
import static io.harness.licensing.ModuleLicenseConstants.MODULE_LICENSE_DELETED;
import static io.harness.licensing.ModuleLicenseConstants.MODULE_LICENSE_UPDATED;
import static io.harness.ng.core.utils.NGYamlUtils.getYamlString;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.Action;
import io.harness.audit.beans.AuditEntry;
import io.harness.audit.beans.ResourceDTO;
import io.harness.audit.beans.ResourceScopeDTO;
import io.harness.audit.client.api.AuditClientService;
import io.harness.context.GlobalContext;
import io.harness.licensing.event.ModuleLicenseCreateEvent;
import io.harness.licensing.event.ModuleLicenseDeleteEvent;
import io.harness.licensing.event.ModuleLicenseUpdateEvent;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxEventHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.serializer.HObjectMapper;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.GTM)
@Slf4j
public class ModuleLicenseOutboxEventHandler implements OutboxEventHandler {
  private final AuditClientService auditClientService;
  private final ObjectMapper objectMapper;

  @Inject
  public ModuleLicenseOutboxEventHandler(AuditClientService auditClientService) {
    this.auditClientService = auditClientService;
    this.objectMapper = HObjectMapper.NG_DEFAULT_OBJECT_MAPPER;
  }

  @Override
  public boolean handle(OutboxEvent outboxEvent) {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    try {
      switch (outboxEvent.getEventType()) {
        case MODULE_LICENSE_CREATED:
          return handleModuleLicenseCreateEvent(outboxEvent, globalContext);
        case MODULE_LICENSE_UPDATED:
          return handleModuleLicenseUpdateEvent(outboxEvent, globalContext);
        case MODULE_LICENSE_DELETED:
          return handleModuleLicenseDeleteEvent(outboxEvent, globalContext);
        default:
          log.error(outboxEvent.getEventType() + " event is unidentified and not handled");
          return false;
      }
    } catch (IOException exception) {
      log.error("Failed to handle " + outboxEvent.getEventType() + " event with exception: ", exception);
      return false;
    }
  }

  private boolean handleModuleLicenseCreateEvent(OutboxEvent outboxEvent, GlobalContext globalContext)
      throws IOException {
    ModuleLicenseCreateEvent moduleLicenseCreateEvent =
        objectMapper.readValue(outboxEvent.getEventData(), ModuleLicenseCreateEvent.class);
    AuditEntry auditEntry = getAuditEntry(outboxEvent, Action.CREATE,
        moduleLicenseCreateEvent.getNewModuleLicenseYamlDTO().getModuleLicenseDTO().getModuleType(),
        getYamlString(moduleLicenseCreateEvent.getOldModuleLicenseYamlDTO()),
        getYamlString(moduleLicenseCreateEvent.getNewModuleLicenseYamlDTO()));
    log.info("NG Auth Audits: for account {} and outboxEventId {} publishing the audit for ModuleLicenseCreated",
        moduleLicenseCreateEvent.getAccountIdentifier(), outboxEvent.getId());

    return auditClientService.publishAudit(auditEntry, globalContext);
  }

  private boolean handleModuleLicenseUpdateEvent(OutboxEvent outboxEvent, GlobalContext globalContext)
      throws IOException {
    ModuleLicenseUpdateEvent moduleLicenseUpdateEvent =
        objectMapper.readValue(outboxEvent.getEventData(), ModuleLicenseUpdateEvent.class);
    AuditEntry auditEntry = getAuditEntry(outboxEvent, Action.UPDATE,
        moduleLicenseUpdateEvent.getNewModuleLicenseYamlDTO().getModuleLicenseDTO().getModuleType(),
        getYamlString(moduleLicenseUpdateEvent.getOldModuleLicenseYamlDTO()),
        getYamlString(moduleLicenseUpdateEvent.getNewModuleLicenseYamlDTO()));
    log.info("NG Auth Audits: for account {} and outboxEventId {} publishing the audit for ModuleLicenseUpdated",
        moduleLicenseUpdateEvent.getAccountIdentifier(), outboxEvent.getId());

    return auditClientService.publishAudit(auditEntry, globalContext);
  }

  private boolean handleModuleLicenseDeleteEvent(OutboxEvent outboxEvent, GlobalContext globalContext)
      throws IOException {
    ModuleLicenseDeleteEvent moduleLicenseDeleteEvent =
        objectMapper.readValue(outboxEvent.getEventData(), ModuleLicenseDeleteEvent.class);
    AuditEntry auditEntry = getAuditEntry(outboxEvent, Action.DELETE,
        moduleLicenseDeleteEvent.getOldModuleLicenseYamlDTO().getModuleLicenseDTO().getModuleType(),
        getYamlString(moduleLicenseDeleteEvent.getOldModuleLicenseYamlDTO()),
        getYamlString(moduleLicenseDeleteEvent.getNewModuleLicenseYamlDTO()));
    log.info("NG Auth Audits: for account {} and outboxEventId {} publishing the audit for ModuleLicenseDeleted",
        moduleLicenseDeleteEvent.getAccountIdentifier(), outboxEvent.getId());

    return auditClientService.publishAudit(auditEntry, globalContext);
  }

  private AuditEntry getAuditEntry(
      OutboxEvent outboxEvent, Action action, ModuleType moduleType, String oldYaml, String newYaml) {
    return AuditEntry.builder()
        .action(action)
        .module(moduleType)
        .oldYaml(oldYaml)
        .newYaml(newYaml)
        .timestamp(outboxEvent.getCreatedAt())
        .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
        .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
        .insertId(outboxEvent.getId())
        .build();
  }
}
