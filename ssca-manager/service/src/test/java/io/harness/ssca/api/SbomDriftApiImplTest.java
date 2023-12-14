/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.api;

import static io.harness.rule.OwnerRule.INDER;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.BuilderFactory;
import io.harness.SSCAManagerTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.spec.server.ssca.v1.model.ArtifactSbomDriftRequestBody;
import io.harness.spec.server.ssca.v1.model.ArtifactSbomDriftResponse;
import io.harness.spec.server.ssca.v1.model.OrchestrationStepDriftRequestBody;
import io.harness.ssca.beans.drift.ComponentDrift;
import io.harness.ssca.beans.drift.ComponentDriftResults;
import io.harness.ssca.beans.drift.ComponentDriftStatus;
import io.harness.ssca.beans.drift.ComponentSummary;
import io.harness.ssca.beans.drift.DriftBase;
import io.harness.ssca.beans.drift.LicenseDrift;
import io.harness.ssca.beans.drift.LicenseDriftResults;
import io.harness.ssca.beans.drift.LicenseDriftStatus;
import io.harness.ssca.services.drift.SbomDriftService;

import java.util.List;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.PageRequest;

@OwnedBy(HarnessTeam.SSCA)
public class SbomDriftApiImplTest extends SSCAManagerTestBase {
  @InjectMocks SbomDriftApiImpl sbomDriftApi;
  @Mock SbomDriftService sbomDriftService;

  private final BuilderFactory builderFactory = BuilderFactory.getDefault();
  private static String ACCOUNT_ID;
  private static String ORG_ID;
  private static String PROJECT_ID;
  private static final String ARTIFACT_ID = "artifactId";
  private static final String DRIFT_ID = "uuid";

  @Before
  public void setup() {
    ACCOUNT_ID = builderFactory.getContext().getAccountId();
    ORG_ID = builderFactory.getContext().getOrgIdentifier();
    PROJECT_ID = builderFactory.getContext().getProjectIdentifier();
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testCalculateDriftForArtifact() {
    ArtifactSbomDriftRequestBody requestBody = new ArtifactSbomDriftRequestBody().tag("tag").baseTag("baseTag");
    ArtifactSbomDriftResponse responseBody =
        new ArtifactSbomDriftResponse().artifactName("artifactName").driftId(DRIFT_ID).tag("tag").baseTag("baseTag");
    when(sbomDriftService.calculateSbomDrift(ACCOUNT_ID, ORG_ID, PROJECT_ID, ARTIFACT_ID, requestBody))
        .thenReturn(responseBody);

    Response response =
        sbomDriftApi.calculateDriftForArtifact(ORG_ID, PROJECT_ID, ARTIFACT_ID, requestBody, ACCOUNT_ID);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getEntity()).isEqualTo(responseBody);
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testGetComponentDrift() {
    when(sbomDriftService.getComponentDrifts(
             ACCOUNT_ID, ORG_ID, PROJECT_ID, DRIFT_ID, ComponentDriftStatus.ADDED, PageRequest.of(0, 10)))
        .thenReturn(
            ComponentDriftResults.builder().totalComponentDrifts(1).componentDrifts(getComponentDrifts()).build());

    Response response = sbomDriftApi.getComponentDrift(ORG_ID, PROJECT_ID, DRIFT_ID, ACCOUNT_ID, "added", 0, 10);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getHeaderString("X-Total-Elements")).isEqualTo("1");
    assertThat(response.getHeaderString("X-Page-Number")).isEqualTo("0");
    assertThat(response.getHeaderString("X-Page-Size")).isEqualTo("10");
    assertThat(response.getEntity())
        .isEqualTo(List.of(new io.harness.spec.server.ssca.v1.model.ComponentDrift().status("added").newComponent(
            new io.harness.spec.server.ssca.v1.model.ComponentSummary().packageName("name"))));
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testGetLicenseDrift() {
    when(sbomDriftService.getLicenseDrifts(
             ACCOUNT_ID, ORG_ID, PROJECT_ID, DRIFT_ID, LicenseDriftStatus.ADDED, PageRequest.of(0, 10)))
        .thenReturn(LicenseDriftResults.builder().totalLicenseDrifts(1).licenseDrifts(getLicenseDrifts()).build());

    Response response = sbomDriftApi.getLicenseDrift(ORG_ID, PROJECT_ID, DRIFT_ID, ACCOUNT_ID, "added", 0, 10);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getHeaderString("X-Total-Elements")).isEqualTo("1");
    assertThat(response.getHeaderString("X-Page-Number")).isEqualTo("0");
    assertThat(response.getHeaderString("X-Page-Size")).isEqualTo("10");
    assertThat(response.getEntity())
        .isEqualTo(
            List.of(new io.harness.spec.server.ssca.v1.model.LicenseDrift()
                        .status("added")
                        .components(List.of(
                            new io.harness.spec.server.ssca.v1.model.ComponentSummary().packageLicense("l1, l2")))
                        .license("license")));
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testCalculateDriftForOrchestrationStep() {
    OrchestrationStepDriftRequestBody requestBody = new OrchestrationStepDriftRequestBody().base("last_generated_sbom");
    ArtifactSbomDriftResponse responseBody =
        new ArtifactSbomDriftResponse().artifactName("artifactName").driftId(DRIFT_ID).tag("tag").baseTag("baseTag");
    when(sbomDriftService.calculateSbomDriftForOrchestration(
             ACCOUNT_ID, ORG_ID, PROJECT_ID, "orchestration", DriftBase.LAST_GENERATED_SBOM))
        .thenReturn(responseBody);

    Response response =
        sbomDriftApi.calculateDriftForOrchestrationStep(ORG_ID, PROJECT_ID, "orchestration", requestBody, ACCOUNT_ID);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getEntity()).isEqualTo(responseBody);
  }

  private List<ComponentDrift> getComponentDrifts() {
    ComponentDrift componentDrift = ComponentDrift.builder()
                                        .status(ComponentDriftStatus.ADDED)
                                        .newComponent(ComponentSummary.builder().packageName("name").build())
                                        .build();
    return List.of(componentDrift);
  }

  private List<LicenseDrift> getLicenseDrifts() {
    LicenseDrift licenseDrift =
        LicenseDrift.builder()
            .name("license")
            .status(LicenseDriftStatus.ADDED)
            .components(List.of(ComponentSummary.builder().packageLicense(List.of("l1", "l2")).build()))
            .build();
    return List.of(licenseDrift);
  }
}
