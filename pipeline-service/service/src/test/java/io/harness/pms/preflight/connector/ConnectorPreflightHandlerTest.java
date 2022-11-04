/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.preflight.connector;

import static io.harness.rule.OwnerRule.NAMAN;
import static io.harness.rule.OwnerRule.SHALINI;
import static io.harness.rule.OwnerRule.VIVEK_DIXIT;

import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.notIn;

import io.harness.CategoryTest;
import io.harness.EntityType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.InfraDefReference;
import io.harness.beans.NGTemplateReference;
import io.harness.category.element.UnitTests;
import io.harness.common.EntityReference;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.gitsync.sdk.EntityGitDetails;
import io.harness.gitsync.sdk.EntityValidityDetails;
import io.harness.ng.core.EntityDetail;
import io.harness.pms.merger.fqn.FQN;
import io.harness.pms.merger.helpers.FQNMapGenerator;
import io.harness.pms.preflight.PreFlightStatus;
import io.harness.pms.yaml.YamlUtils;
import io.harness.rule.Owner;

import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.PIPELINE)
public class ConnectorPreflightHandlerTest extends CategoryTest {
  Map<String, Object> fqnToObjectMapMergedYaml = new HashMap<>();

  @InjectMocks ConnectorPreflightHandler connectorPreflightHandler;

  @Before
  public void setup() throws IOException {
    MockitoAnnotations.initMocks(this);
    ClassLoader classLoader = getClass().getClassLoader();
    String filename = "failure-strategy.yaml";
    String yaml = Resources.toString(Objects.requireNonNull(classLoader.getResource(filename)), StandardCharsets.UTF_8);

    Map<FQN, Object> fqnObjectMap =
        FQNMapGenerator.generateFQNMap(YamlUtils.readTree(yaml).getNode().getCurrJsonNode());
    fqnObjectMap.keySet().forEach(fqn -> fqnToObjectMapMergedYaml.put(fqn.getExpressionFqn(), fqnObjectMap.get(fqn)));
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testNonExistentConnectorsInPipeline() {
    Map<String, String> connectorIdentifierToFqn = new HashMap<>();
    connectorIdentifierToFqn.put(
        "my_git_connector", "pipeline.stages.qaStage.serviceConfig.manifests.baseValues.my_git_connector");

    List<ConnectorResponseDTO> connectorResponses = new ArrayList<>();
    List<ConnectorCheckResponse> connectorCheckResponse = connectorPreflightHandler.getConnectorCheckResponse(
        fqnToObjectMapMergedYaml, connectorResponses, connectorIdentifierToFqn);
    assertThat(connectorCheckResponse).isNotEmpty();
    ConnectorCheckResponse response = connectorCheckResponse.get(0);
    assertThat(response.getStatus()).isEqualTo(PreFlightStatus.FAILURE);
    assertThat(response.getConnectorIdentifier()).isEqualTo("my_git_connector");
    assertThat(response.getFqn())
        .isEqualTo("pipeline.stages.qaStage.serviceConfig.manifests.baseValues.my_git_connector");
    assertThat(response.getStageIdentifier()).isEqualTo("qaStage");
    assertThat(response.getStageName()).isEqualTo("qa stage");
    assertThat(response.getErrorInfo()).isNotNull();
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testInvalidConnectorsInPipeline() {
    Map<String, String> connectorIdentifierToFqn = new HashMap<>();
    connectorIdentifierToFqn.put(
        "my_git_connector", "pipeline.stages.qaStage.serviceConfig.manifests.baseValues.my_git_connector");

    List<ConnectorResponseDTO> connectorResponses =
        Collections.singletonList(ConnectorResponseDTO.builder()
                                      .connector(ConnectorInfoDTO.builder().identifier("my_git_connector").build())
                                      .entityValidityDetails(EntityValidityDetails.builder().valid(false).build())
                                      .build());
    List<ConnectorCheckResponse> connectorCheckResponse = connectorPreflightHandler.getConnectorCheckResponse(
        fqnToObjectMapMergedYaml, connectorResponses, connectorIdentifierToFqn);
    assertThat(connectorCheckResponse).isNotEmpty();
    ConnectorCheckResponse response = connectorCheckResponse.get(0);
    assertThat(response.getStatus()).isEqualTo(PreFlightStatus.FAILURE);
    assertThat(response.getConnectorIdentifier()).isEqualTo("my_git_connector");
    assertThat(response.getFqn())
        .isEqualTo("pipeline.stages.qaStage.serviceConfig.manifests.baseValues.my_git_connector");
    assertThat(response.getStageIdentifier()).isEqualTo("qaStage");
    assertThat(response.getStageName()).isEqualTo("qa stage");
    assertThat(response.getErrorInfo()).isNotNull();
  }

  @Test
  @Owner(developers = VIVEK_DIXIT)
  @Category(UnitTests.class)
  public void testFilterConnectorResponseForConnectorForGitFlowPipeline() {
    EntityReference entityReference1 = NGTemplateReference.builder()
                                           .branch("main")
                                           .identifier("con_for_oldGitRemote_pipeline")
                                           .repoIdentifier("repo")
                                           .build();
    EntityReference entityReference2 =
        NGTemplateReference.builder().identifier("con_for_newGitExpAndInline_pipeline").build();

    List<EntityDetail> connectorUsages = Arrays.asList(EntityDetail.builder().entityRef(entityReference1).build(),
        EntityDetail.builder().entityRef(entityReference2).build());

    ConnectorResponseDTO connectorResponseDTO1 =
        ConnectorResponseDTO.builder()
            .connector(ConnectorInfoDTO.builder().identifier("con_for_oldGitRemote_pipeline").build())
            .gitDetails(EntityGitDetails.builder().branch("main").repoIdentifier("repo").build())
            .build();
    ConnectorResponseDTO connectorResponseDTO2 =
        ConnectorResponseDTO.builder()
            .connector(ConnectorInfoDTO.builder().identifier("con_for_oldGitRemote_pipeline").build())
            .gitDetails(EntityGitDetails.builder().branch("another_branch").repoIdentifier("repo").build())
            .build();
    ConnectorResponseDTO connectorResponseDTO3 =
        ConnectorResponseDTO.builder()
            .connector(ConnectorInfoDTO.builder().identifier("con_for_newGitExpAndInline_pipeline").build())
            .build();

    List<ConnectorResponseDTO> unFilteredResponse =
        Arrays.asList(connectorResponseDTO1, connectorResponseDTO2, connectorResponseDTO3);
    List<ConnectorResponseDTO> filteredResponse =
        connectorPreflightHandler.filterConnectorResponse(unFilteredResponse, connectorUsages);

    assertThat(filteredResponse.size()).isEqualTo(2);
    assertThat(filteredResponse.contains(connectorResponseDTO1));
    notIn(filteredResponse, connectorResponseDTO2);
    assertThat(filteredResponse.contains(connectorResponseDTO3));
  }

  @Test
  @Owner(developers = VIVEK_DIXIT)
  @Category(UnitTests.class)
  public void testFilterConnectorResponseForNullParamsOfConnectorUsages() {
    EntityReference entityReference1 = NGTemplateReference.builder().branch("main").identifier("repo_null").build();
    EntityReference entityReference2 =
        NGTemplateReference.builder().identifier("branch_null").repoIdentifier("repo").build();

    List<EntityDetail> connectorUsages = Arrays.asList(EntityDetail.builder().entityRef(entityReference1).build(),
        EntityDetail.builder().entityRef(entityReference2).build());

    ConnectorResponseDTO connectorResponseDTO1 =
        ConnectorResponseDTO.builder()
            .connector(ConnectorInfoDTO.builder().identifier("con_for_oldGitRemote_pipeline").build())
            .gitDetails(EntityGitDetails.builder().branch("main").repoIdentifier("repo").build())
            .build();
    ConnectorResponseDTO connectorResponseDTO2 =
        ConnectorResponseDTO.builder()
            .connector(ConnectorInfoDTO.builder().identifier("con_for_oldGitRemote_pipeline").build())
            .gitDetails(EntityGitDetails.builder().branch("another_branch").repoIdentifier("repo").build())
            .build();

    List<ConnectorResponseDTO> unFilteredResponse = Arrays.asList(connectorResponseDTO1, connectorResponseDTO2);
    List<ConnectorResponseDTO> filteredResponse =
        connectorPreflightHandler.filterConnectorResponse(unFilteredResponse, connectorUsages);

    assertThat(filteredResponse.isEmpty());
  }

  @Test
  @Owner(developers = VIVEK_DIXIT)
  @Category(UnitTests.class)
  public void testFilterConnectorResponseForJunkValues() {
    EntityReference entityReference1 = NGTemplateReference.builder()
                                           .branch("main")
                                           .identifier("con_for_oldGitRemote_pipeline")
                                           .repoIdentifier("repo")
                                           .build();
    EntityReference entityReference2 =
        NGTemplateReference.builder().identifier("con_for_newGitExpAndInline_pipeline").build();

    List<EntityDetail> connectorUsages = Arrays.asList(EntityDetail.builder().entityRef(entityReference1).build(),
        EntityDetail.builder().entityRef(entityReference2).build());

    ConnectorResponseDTO connectorResponseDTO1 =
        ConnectorResponseDTO.builder()
            .connector(ConnectorInfoDTO.builder().identifier("repo_null").build())
            .gitDetails(EntityGitDetails.builder().branch("main").build())
            .build();
    ConnectorResponseDTO connectorResponseDTO2 =
        ConnectorResponseDTO.builder()
            .connector(ConnectorInfoDTO.builder().identifier("branch_null").build())
            .gitDetails(EntityGitDetails.builder().repoIdentifier("repo").build())
            .build();
    ConnectorResponseDTO connectorResponseDTO3 =
        ConnectorResponseDTO.builder()
            .connector(ConnectorInfoDTO.builder().identifier("junk_value").build())
            .gitDetails(EntityGitDetails.builder().branch("junk_value").repoIdentifier("junk_value").build())
            .build();
    ConnectorResponseDTO connectorResponseDTO4 = ConnectorResponseDTO.builder().build();

    List<ConnectorResponseDTO> unFilteredResponse =
        Arrays.asList(connectorResponseDTO1, connectorResponseDTO2, connectorResponseDTO3, connectorResponseDTO4);
    List<ConnectorResponseDTO> filteredResponse =
        connectorPreflightHandler.filterConnectorResponse(unFilteredResponse, connectorUsages);

    assertThat(filteredResponse.size()).isEqualTo(3);
    assertThat(filteredResponse.contains(connectorResponseDTO1));
    assertThat(filteredResponse.contains(connectorResponseDTO2));
    notIn(filteredResponse, connectorResponseDTO3);
    assertThat(filteredResponse.contains(connectorResponseDTO4));
  }

  @Test
  @Owner(developers = VIVEK_DIXIT)
  @Category(UnitTests.class)
  public void testFilterConnectorResponseForEmptyUsagesAndResponse() {
    EntityReference entityReference1 = NGTemplateReference.builder().branch("main").identifier("repo_null").build();
    EntityReference entityReference2 =
        NGTemplateReference.builder().identifier("branch_null").repoIdentifier("repo").build();

    List<EntityDetail> connectorUsages = Arrays.asList(EntityDetail.builder().entityRef(entityReference1).build(),
        EntityDetail.builder().entityRef(entityReference2).build());

    ConnectorResponseDTO connectorResponseDTO1 =
        ConnectorResponseDTO.builder()
            .connector(ConnectorInfoDTO.builder().identifier("con_for_oldGitRemote_pipeline").build())
            .gitDetails(EntityGitDetails.builder().branch("main").repoIdentifier("repo").build())
            .build();
    ConnectorResponseDTO connectorResponseDTO2 =
        ConnectorResponseDTO.builder()
            .connector(ConnectorInfoDTO.builder().identifier("con_for_oldGitRemote_pipeline").build())
            .gitDetails(EntityGitDetails.builder().branch("another_branch").repoIdentifier("repo").build())
            .build();

    List<ConnectorResponseDTO> unFilteredResponse = Arrays.asList(connectorResponseDTO1, connectorResponseDTO2);
    List<ConnectorResponseDTO> filteredResponse1 =
        connectorPreflightHandler.filterConnectorResponse(Collections.EMPTY_LIST, connectorUsages);
    List<ConnectorResponseDTO> filteredResponse2 =
        connectorPreflightHandler.filterConnectorResponse(unFilteredResponse, Collections.EMPTY_LIST);
    List<ConnectorResponseDTO> filteredResponse3 =
        connectorPreflightHandler.filterConnectorResponse(Collections.EMPTY_LIST, Collections.EMPTY_LIST);

    assertThat(filteredResponse1.isEmpty());
    assertThat(filteredResponse2.isEmpty());
    assertThat(filteredResponse3.isEmpty());
  }

  @Test
  @Owner(developers = SHALINI)
  @Category(UnitTests.class)
  public void testGetConnectorCheckResponseTemplate() {
    List<EntityDetail> entityDetails =
        Collections.singletonList(EntityDetail.builder()
                                      .type(EntityType.CONNECTORS)
                                      .entityRef(InfraDefReference.builder().identifier("infra").build())
                                      .build());
    List<ConnectorCheckResponse> connectorCheckResponses =
        connectorPreflightHandler.getConnectorCheckResponseTemplate(entityDetails);
    assertEquals(1, connectorCheckResponses.size());
    assertEquals("infra", connectorCheckResponses.get(0).connectorIdentifier);
    assertEquals(PreFlightStatus.UNKNOWN, connectorCheckResponses.get(0).status);
  }
}
