/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.events.handler;

import static io.harness.annotations.dev.HarnessTeam.SSCA;
import static io.harness.ssca.events.utils.SSCAOutboxEvents.SSCA_ARTIFACT_CREATED_EVENT;
import static io.harness.ssca.events.utils.SSCAOutboxEvents.SSCA_ARTIFACT_UPDATED_EVENT;

import static io.serializer.HObjectMapper.NG_DEFAULT_OBJECT_MAPPER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidArgumentsException;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxEventHandler;
import io.harness.ssca.events.SSCAArtifactCreatedEvent;
import io.harness.ssca.events.SSCAArtifactUpdatedEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(SSCA)
@Slf4j
public class SSCAArtifactEventHandler implements OutboxEventHandler {
  private final ObjectMapper objectMapper;

  @Inject
  public SSCAArtifactEventHandler() {
    this.objectMapper = NG_DEFAULT_OBJECT_MAPPER;
  }

  @Override
  public boolean handle(OutboxEvent outboxEvent) {
    try {
      switch (outboxEvent.getEventType()) {
        case SSCA_ARTIFACT_CREATED_EVENT:
          return handleSSCAArtifactCreatedEvent(outboxEvent);
        case SSCA_ARTIFACT_UPDATED_EVENT:
          return handleSSCAArtifactUpdatedEvent(outboxEvent);
        default:
          throw new InvalidArgumentsException(String.format("Not supported event type %s", outboxEvent.getEventType()));
      }
    } catch (IOException exception) {
      log.error("Couldn't handle ssca artifact outboxevent {}", outboxEvent, exception);
      return false;
    }
  }

  private boolean handleSSCAArtifactCreatedEvent(OutboxEvent outboxEvent) throws IOException {
    SSCAArtifactCreatedEvent sscaArtifactCreatedEvent =
        objectMapper.readValue(outboxEvent.getEventData(), SSCAArtifactCreatedEvent.class);

    // Publish to ELK
    return true;
  }

  private boolean handleSSCAArtifactUpdatedEvent(OutboxEvent outboxEvent) throws IOException {
    SSCAArtifactUpdatedEvent sscaArtifactUpdatedEvent =
        objectMapper.readValue(outboxEvent.getEventData(), SSCAArtifactUpdatedEvent.class);

    // Publish to ELK
    return true;
  }
}
