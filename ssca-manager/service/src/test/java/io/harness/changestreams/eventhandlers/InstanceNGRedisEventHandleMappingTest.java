/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.changestreams.eventhandlers;

import static io.harness.rule.OwnerRule.ARPITJ;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.SSCAManagerTestBase;
import io.harness.category.element.UnitTests;
import io.harness.debezium.DebeziumChangeEvent;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.ssca.beans.instance.InstanceDTO;

import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;

public class InstanceNGRedisEventHandleMappingTest extends SSCAManagerTestBase {
  @Inject InstanceNGRedisEventHandler handler;
  private DebeziumChangeEvent event;

  @Before
  public void setup() throws IllegalAccessException, InvalidProtocolBufferException {
    MockitoAnnotations.initMocks(this);
    String messageString = readFile("debezium-create-event-message.txt");
    event = DebeziumChangeEvent.parseFrom(Base64.getDecoder().decode(messageString));
  }

  @Test
  @Owner(developers = ARPITJ)
  @Category(UnitTests.class)
  public void testCreateInstance() {
    InstanceDTO instanceDTO = handler.createEntity(event.getValue());
    assertThat(instanceDTO.getAccountIdentifier()).isEqualTo("kmpySmUISimoRrJL6NL73w");
    assertThat(instanceDTO.getOrgIdentifier()).isEqualTo("default");
    assertThat(instanceDTO.getProjectIdentifier()).isEqualTo("SSCADebeziumTest");
    assertThat(instanceDTO.getEnvIdentifier()).isEqualTo("preEnv2");
    assertThat(instanceDTO.getEnvName()).isEqualTo("pre-Env-2");
    assertThat(instanceDTO.getEnvType()).isEqualTo("PreProduction");
    assertThat(instanceDTO.getId()).isEqualTo("65707d6122d048e7e5691786");
    assertThat(instanceDTO.getLastPipelineExecutionId()).isEqualTo("jAfvNJMTR0mhK24tDZtmIQ");
    assertThat(instanceDTO.getLastPipelineExecutionName()).isEqualTo("UltimateTestPipeline");
    assertThat(instanceDTO.getStageNodeExecutionId()).isEqualTo("Lgy-ay7xRmmKVkjpleQLsA");
    assertThat(instanceDTO.getStageSetupId()).isEqualTo("Lgy-ay7xRmmKVkjpleQLsA");
    assertThat(instanceDTO.getLastDeployedAt()).isEqualTo(1695728401638l);
    assertThat(instanceDTO.getLastDeployedByName()).isEqualTo("Admin");
    assertThat(instanceDTO.getLastDeployedById()).isEqualTo("lv0euRhKRCyiXWzS7pOg6g");
    assertThat(instanceDTO.getPrimaryArtifact().getDisplayName()).isEqualTo("library/nginx:latest");
    assertThat(instanceDTO.getPrimaryArtifact().getArtifactId()).isEqualTo("primary");
    assertThat(instanceDTO.getPrimaryArtifact().getTag()).isEqualTo("latest");
    assertThat(instanceDTO.getPrimaryArtifact().getArtifactIdentity().getImage())
        .isEqualTo("index.docker.com/arpit/image-new:tag-1");
    assertThat(instanceDTO.isDeleted()).isEqualTo(false);
    assertThat(instanceDTO.getCreatedAt()).isEqualTo(1695728475984l);
    assertThat(instanceDTO.getLastModifiedAt()).isEqualTo(1695728590738l);
  }

  private String readFile(String filename) {
    ClassLoader classLoader = getClass().getClassLoader();
    try {
      return Resources.toString(Objects.requireNonNull(classLoader.getResource(filename)), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new InvalidRequestException("Could not read resource file: " + filename);
    }
  }
}
