/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.idp.utils;

import static io.harness.rule.OwnerRule.DEVESH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.idp.steps.beans.stepinfo.IdpCookieCutterStepInfo;
import io.harness.idp.steps.beans.stepinfo.IdpCreateRepoStepInfo;
import io.harness.idp.steps.beans.stepinfo.IdpDirectPushStepInfo;
import io.harness.idp.steps.beans.stepinfo.IdpRegisterCatalogStepInfo;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.utils.CiCodebaseUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.IDP)
public class IDPStepUtilsTest extends CategoryTest {
  @InjectMocks IDPStepUtils idpStepUtils;

  @Mock CiCodebaseUtils ciCodebaseUtils;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = DEVESH)
  @Category(UnitTests.class)
  public void testIdpCookieCutterStepEnvVariables() {
    String testName = "test-name";
    String testIdentifier = "test-identifier";
    String testTemplateType = "public";
    String testPath = "test-path";
    String testCookieCutterVarName = "testVarName";
    String testCookieCutterVarValue = "testVarValue";
    Map<String, JsonNode> cookiecutterVariables = new HashMap<>();
    cookiecutterVariables.put(testCookieCutterVarName, JsonNodeFactory.instance.textNode(testCookieCutterVarValue));

    IdpCookieCutterStepInfo idpCookieCutterStepInfo =
        IdpCookieCutterStepInfo.builder()
            .cookieCutterVariables(ParameterField.createValueField(cookiecutterVariables))
            .templateType(ParameterField.createValueField(testTemplateType))
            .pathForTemplate(ParameterField.createValueField(testPath))
            .name(testName)
            .identifier(testIdentifier)
            .build();

    Map<String, String> expected = new HashMap<>();
    expected.put("IDP_COOKIECUTTER_" + testCookieCutterVarName, testCookieCutterVarValue);
    expected.put("TEMPLATE_TYPE", testTemplateType);
    expected.put("PATH_FOR_TEMPLATE", testPath);
    Map<String, String> actual =
        idpStepUtils.getCookieCutterStepInfoEnvVariables(idpCookieCutterStepInfo, "test-identifier");
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @Owner(developers = DEVESH)
  @Category(UnitTests.class)
  public void testIdpCreateRepoStepEnvVariables() {
    String testName = "test-name";

    String repoType = "private";
    String repoName = "test-repo-name";
    String orgName = "test-org-name";

    IdpCreateRepoStepInfo idpCreateRepoStepInfo = IdpCreateRepoStepInfo.builder()
                                                      .repoType(ParameterField.createValueField(repoType))
                                                      .repository(ParameterField.createValueField(repoName))
                                                      .name(testName)
                                                      .organization(ParameterField.createValueField(orgName))
                                                      .connectorRef(ParameterField.createValueField("myConnectorRef"))
                                                      .build();

    Map<String, String> expected = new HashMap<>();
    expected.put("IDP_ORG_NAME", orgName);
    expected.put("REPO_TYPE", repoType);
    expected.put("IDP_REPO_NAME", repoName);
    expected.put("CONNECTOR_TYPE", ConnectorType.GITHUB.getDisplayName());

    ConnectorDetails connectorDetails = ConnectorDetails.builder().connectorType(ConnectorType.GITHUB).build();

    when(ciCodebaseUtils.getGitEnvVariables(any(), any())).thenReturn(new HashMap<>());

    Map<String, String> actual =
        idpStepUtils.getCreateRepoStepInfoEnvVariables(idpCreateRepoStepInfo, connectorDetails, "test-id");

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @Owner(developers = DEVESH)
  @Category(UnitTests.class)
  public void testIDPCodePushStepInfoEnvVariables() {
    String testName = "test-name";

    String repoName = "test-repo-name";
    String orgName = "test-org-name";
    String workspace = "test-workspace-name";
    String project = "test-project-name";
    String codeDirectory = "test-code-directory";
    String branch = "test-branch-name";

    IdpDirectPushStepInfo idpCodePushStepInfo = IdpDirectPushStepInfo.builder()
                                                    .repository(ParameterField.createValueField(repoName))
                                                    .branch(ParameterField.createValueField(branch))
                                                    .codeDirectory(ParameterField.createValueField(codeDirectory))
                                                    .name(testName)
                                                    .organization(ParameterField.createValueField(orgName))
                                                    .workspace(ParameterField.createValueField(workspace))
                                                    .connectorRef(ParameterField.createValueField("myConnectorRef"))
                                                    .build();

    Map<String, String> expected = new HashMap<>();
    expected.put("IDP_ORG_NAME", orgName);
    expected.put("IDP_REPO_NAME", repoName);
    expected.put("IDP_WORKSPACE_NAME", workspace);
    expected.put("CODE_DIRECTORY", codeDirectory);
    expected.put("BRANCH", branch);
    expected.put("CONNECTOR_TYPE", ConnectorType.GITHUB.getDisplayName());

    ConnectorDetails connectorDetails = ConnectorDetails.builder().connectorType(ConnectorType.GITHUB).build();

    when(ciCodebaseUtils.getGitEnvVariables(any(), any())).thenReturn(new HashMap<>());

    Map<String, String> actual =
        idpStepUtils.getDirectPushStepInfoEnvVariables(idpCodePushStepInfo, connectorDetails, "test-id");

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @Owner(developers = DEVESH)
  @Category(UnitTests.class)
  public void testRegisterCatalogStepInfoEnvVariables() {
    String testName = "test-name";
    String repoName = "test-repo-name";
    String orgName = "test-org-name";
    String workspace = "test-workspace-name";
    String filePath = "test-file-path";
    String branch = "test-branch-name";

    IdpRegisterCatalogStepInfo idpRegisterCatalogStepInfo =
        IdpRegisterCatalogStepInfo.builder()
            .repository(ParameterField.createValueField(repoName))
            .branch(ParameterField.createValueField(branch))
            .filePath(ParameterField.createValueField(filePath))
            .name(testName)
            .organization(ParameterField.createValueField(orgName))
            .workspace(ParameterField.createValueField(workspace))
            .connectorRef(ParameterField.createValueField("myConnectorRef"))
            .build();

    Map<String, String> expected = new HashMap<>();
    expected.put("IDP_ORG_NAME", orgName);
    expected.put("IDP_REPO_NAME", repoName);
    expected.put("IDP_WORKSPACE_NAME", workspace);
    expected.put("FILE_PATH", filePath);
    expected.put("BRANCH", branch);
    expected.put("CONNECTOR_TYPE", ConnectorType.GITHUB.getDisplayName());

    ConnectorDetails connectorDetails = ConnectorDetails.builder().connectorType(ConnectorType.GITHUB).build();

    when(ciCodebaseUtils.getGitEnvVariables(any(), any())).thenReturn(new HashMap<>());

    Map<String, String> actual =
        idpStepUtils.getRegisterCatalogStepInfoEnvVariables(idpRegisterCatalogStepInfo, connectorDetails, "test-id");

    assertThat(actual).isEqualTo(expected);
  }
}
