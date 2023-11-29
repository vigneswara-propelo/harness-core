/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.licensing.outbox;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.licensing.LicenseConstant.UNLIMITED;
import static io.harness.licensing.LicenseTestConstant.ACCOUNT_IDENTIFIER;
import static io.harness.licensing.LicenseTestConstant.DEFAULT_MODULE_TYPE;
import static io.harness.licensing.ModuleLicenseConstants.MODULE_LICENSE_CREATED;
import static io.harness.licensing.ModuleLicenseConstants.MODULE_LICENSE_DELETED;
import static io.harness.licensing.ModuleLicenseConstants.MODULE_LICENSE_UPDATED;
import static io.harness.rule.OwnerRule.KAPIL;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.Action;
import io.harness.audit.ResourceTypeConstants;
import io.harness.audit.beans.AuditEntry;
import io.harness.audit.client.api.AuditClientService;
import io.harness.category.element.UnitTests;
import io.harness.licensing.Edition;
import io.harness.licensing.LicenseStatus;
import io.harness.licensing.beans.modules.CIModuleLicenseDTO;
import io.harness.licensing.event.ModuleLicenseCreateEvent;
import io.harness.licensing.event.ModuleLicenseDeleteEvent;
import io.harness.licensing.event.ModuleLicenseEvent;
import io.harness.licensing.event.ModuleLicenseUpdateEvent;
import io.harness.licensing.event.ModuleLicenseYamlDTO;
import io.harness.ng.core.AccountScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceScope;
import io.harness.outbox.OutboxEvent;
import io.harness.rule.Owner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.serializer.HObjectMapper;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;

@OwnedBy(HarnessTeam.GTM)
public class ModuleLicenseOutboxEventHandlerTest extends CategoryTest {
  private ModuleLicenseOutboxEventHandler moduleLicenseOutboxEventHandler;
  private AuditClientService auditClientService;
  private String accountIdentifier;
  @Nullable private ObjectMapper objectMapper;

  @Before
  public void setup() throws IllegalAccessException {
    accountIdentifier = generateUuid();
    objectMapper = HObjectMapper.NG_DEFAULT_OBJECT_MAPPER;
    auditClientService = mock(AuditClientService.class);
    moduleLicenseOutboxEventHandler = new ModuleLicenseOutboxEventHandler(auditClientService);
  }

  @Test
  @Owner(developers = KAPIL)
  @Category(UnitTests.class)
  public void testHandle_ModuleLicenseCreateEvent() throws JsonProcessingException {
    ModuleLicenseCreateEvent moduleLicenseCreateEvent =
        ModuleLicenseCreateEvent.builder()
            .accountIdentifier(accountIdentifier)
            .newModuleLicenseYamlDTO(
                ModuleLicenseYamlDTO.builder().moduleLicenseDTO(getCIModuleLicenseDTO(100)).build())
            .build();
    OutboxEvent outboxEvent = getOutboxEvent(MODULE_LICENSE_CREATED, moduleLicenseCreateEvent);
    ArgumentCaptor<AuditEntry> auditEntryArgumentCaptor = ArgumentCaptor.forClass(AuditEntry.class);
    when(auditClientService.publishAudit(any(), any())).thenReturn(true);

    assertTrue(moduleLicenseOutboxEventHandler.handle(outboxEvent));
    verify(auditClientService, times(1)).publishAudit(auditEntryArgumentCaptor.capture(), any());

    AuditEntry auditEntry = auditEntryArgumentCaptor.getValue();
    assertEquals(Action.CREATE, auditEntry.getAction());
  }

  @Test
  @Owner(developers = KAPIL)
  @Category(UnitTests.class)
  public void testHandle_ModuleLicenseUpdateEvent() throws JsonProcessingException {
    ModuleLicenseUpdateEvent moduleLicenseUpdateEvent =
        ModuleLicenseUpdateEvent.builder()
            .accountIdentifier(accountIdentifier)
            .oldModuleLicenseYamlDTO(
                ModuleLicenseYamlDTO.builder().moduleLicenseDTO(getCIModuleLicenseDTO(100)).build())
            .newModuleLicenseYamlDTO(
                ModuleLicenseYamlDTO.builder().moduleLicenseDTO(getCIModuleLicenseDTO(200)).build())
            .build();
    OutboxEvent outboxEvent = getOutboxEvent(MODULE_LICENSE_UPDATED, moduleLicenseUpdateEvent);
    ArgumentCaptor<AuditEntry> auditEntryArgumentCaptor = ArgumentCaptor.forClass(AuditEntry.class);
    when(auditClientService.publishAudit(any(), any())).thenReturn(true);

    assertTrue(moduleLicenseOutboxEventHandler.handle(outboxEvent));
    verify(auditClientService, times(1)).publishAudit(auditEntryArgumentCaptor.capture(), any());

    AuditEntry auditEntry = auditEntryArgumentCaptor.getValue();
    assertEquals(Action.UPDATE, auditEntry.getAction());
  }

  @Test
  @Owner(developers = KAPIL)
  @Category(UnitTests.class)
  public void testHandle_ModuleLicenseDeleteEvent() throws JsonProcessingException {
    ModuleLicenseDeleteEvent moduleLicenseDeleteEvent =
        ModuleLicenseDeleteEvent.builder()
            .accountIdentifier(accountIdentifier)
            .oldModuleLicenseYamlDTO(
                ModuleLicenseYamlDTO.builder().moduleLicenseDTO(getCIModuleLicenseDTO(100)).build())
            .build();
    OutboxEvent outboxEvent = getOutboxEvent(MODULE_LICENSE_DELETED, moduleLicenseDeleteEvent);
    ArgumentCaptor<AuditEntry> auditEntryArgumentCaptor = ArgumentCaptor.forClass(AuditEntry.class);
    when(auditClientService.publishAudit(any(), any())).thenReturn(true);

    assertTrue(moduleLicenseOutboxEventHandler.handle(outboxEvent));
    verify(auditClientService, times(1)).publishAudit(auditEntryArgumentCaptor.capture(), any());

    AuditEntry auditEntry = auditEntryArgumentCaptor.getValue();
    assertEquals(Action.DELETE, auditEntry.getAction());
  }

  @Test
  @Owner(developers = KAPIL)
  @Category(UnitTests.class)
  public void testHandle_ModuleLicenseCreateEvent_FailureScenario() throws JsonProcessingException {
    String createEventString = objectMapper.writeValueAsString(null);
    ResourceScope resourceScope = new AccountScope(accountIdentifier);
    OutboxEvent outboxEvent = OutboxEvent.builder()
                                  .eventType(MODULE_LICENSE_CREATED)
                                  .resourceScope(resourceScope)
                                  .eventData(createEventString)
                                  .createdAt(System.currentTimeMillis())
                                  .resource(Resource.builder().type(ResourceTypeConstants.MODULE_LICENSE).build())
                                  .build();

    assertThatThrownBy(() -> moduleLicenseOutboxEventHandler.handle(outboxEvent))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @Owner(developers = KAPIL)
  @Category(UnitTests.class)
  public void testHandle_ModuleLicenseUpdateEvent_FailureScenarioTwo() {
    ResourceScope resourceScope = new AccountScope(accountIdentifier);
    OutboxEvent outboxEvent = OutboxEvent.builder()
                                  .eventType(MODULE_LICENSE_UPDATED)
                                  .resourceScope(resourceScope)
                                  .eventData(null)
                                  .createdAt(System.currentTimeMillis())
                                  .resource(Resource.builder().type(ResourceTypeConstants.MODULE_LICENSE).build())
                                  .build();

    assertThatThrownBy(() -> moduleLicenseOutboxEventHandler.handle(outboxEvent))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private OutboxEvent getOutboxEvent(String eventType, ModuleLicenseEvent moduleLicenseEvent)
      throws JsonProcessingException {
    String createEventString = objectMapper.writeValueAsString(moduleLicenseEvent);
    ResourceScope resourceScope = new AccountScope(accountIdentifier);

    return OutboxEvent.builder()
        .eventType(eventType)
        .resourceScope(resourceScope)
        .eventData(createEventString)
        .createdAt(System.currentTimeMillis())
        .resource(Resource.builder().type(ResourceTypeConstants.MODULE_LICENSE).build())
        .build();
  }
  private CIModuleLicenseDTO getCIModuleLicenseDTO(Integer numberOfCommitters) {
    return CIModuleLicenseDTO.builder()
        .id("id")
        .numberOfCommitters(numberOfCommitters)
        .accountIdentifier(ACCOUNT_IDENTIFIER)
        .moduleType(DEFAULT_MODULE_TYPE)
        .edition(Edition.FREE)
        .status(LicenseStatus.ACTIVE)
        .startTime(1)
        .expiryTime(Long.valueOf(UNLIMITED))
        .createdAt(0L)
        .lastModifiedAt(0L)
        .build();
  }
}
