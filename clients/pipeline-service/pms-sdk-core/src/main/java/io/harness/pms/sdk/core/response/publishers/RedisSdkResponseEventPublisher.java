/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.sdk.core.response.publishers;

import static io.harness.pms.sdk.core.PmsSdkCoreEventsFrameworkConstants.SDK_RESPONSE_EVENT_PRODUCER;
import static io.harness.pms.sdk.core.PmsSdkCoreEventsFrameworkConstants.SDK_RESPONSE_SPAWN_EVENT_PRODUCER;
import static io.harness.pms.sdk.core.PmsSdkCoreEventsFrameworkConstants.SDK_STEP_RESPONSE_EVENT_PRODUCER;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.execution.utils.SdkResponseEventUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

@OwnedBy(HarnessTeam.PIPELINE)
public class RedisSdkResponseEventPublisher implements SdkResponseEventPublisher {
  private Producer sdkResponseEventProducer;
  private Producer sdkResponseSpawnEventProducer;
  private Producer sdkStepResponseEventProducer;
  private static final String CDS_DIVIDE_SDK_RESPONSE_EVENTS_IN_DIFF_STREAMS =
      "CDS_DIVIDE_SDK_RESPONSE_EVENTS_IN_DIFF_STREAMS";

  @Inject
  public RedisSdkResponseEventPublisher(@Named(SDK_RESPONSE_EVENT_PRODUCER) Producer sdkResponseEventProducer,
      @Named(SDK_RESPONSE_SPAWN_EVENT_PRODUCER) Producer sdkResponseSpawnEventProducer,
      @Named(SDK_STEP_RESPONSE_EVENT_PRODUCER) Producer sdkStepResponseEventProducer) {
    this.sdkResponseEventProducer = sdkResponseEventProducer;
    this.sdkResponseSpawnEventProducer = sdkResponseSpawnEventProducer;
    this.sdkStepResponseEventProducer = sdkStepResponseEventProducer;
  }

  @Override
  public void publishEvent(SdkResponseEventProto event) {
    Map<String, String> metadataMap = getMetadataMap(event);
    Producer producer = getProducer(event);
    producer.send(Message.newBuilder().putAllMetadata(metadataMap).setData(event.toByteString()).build());
  }

  private Producer getProducer(SdkResponseEventProto event) {
    if (AmbianceUtils.checkIfFeatureFlagEnabled(event.getAmbiance(), CDS_DIVIDE_SDK_RESPONSE_EVENTS_IN_DIFF_STREAMS)) {
      switch (event.getSdkResponseEventType()) {
        case HANDLE_STEP_RESPONSE:
        case QUEUE_TASK:
          return sdkStepResponseEventProducer;
        case SPAWN_CHILD:
        case SPAWN_CHILDREN:
          return sdkResponseSpawnEventProducer;
        default:
          return sdkResponseEventProducer;
      }
    } else {
      return sdkResponseEventProducer;
    }
  }

  @NotNull
  private Map<String, String> getMetadataMap(SdkResponseEventProto event) {
    Map<String, String> metadataMap = new HashMap<>();

    metadataMap.put("eventType", event.getSdkResponseEventType().name());
    metadataMap.put("nodeExecutionId", SdkResponseEventUtils.getNodeExecutionId(event));
    metadataMap.put("planExecutionId", SdkResponseEventUtils.getPlanExecutionId(event));
    return metadataMap;
  }
}
