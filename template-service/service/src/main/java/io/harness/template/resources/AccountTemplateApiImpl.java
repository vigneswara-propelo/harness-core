/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.template.resources;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.ResourceIdentifier;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.spec.server.template.v1.AccountTemplateApi;
import io.harness.spec.server.template.v1.model.GitCreateDetails;
import io.harness.spec.server.template.v1.model.GitFindDetails;
import io.harness.spec.server.template.v1.model.GitUpdateDetails;
import io.harness.spec.server.template.v1.model.TemplateCreateRequestBody;
import io.harness.spec.server.template.v1.model.TemplateImportRequestBody;
import io.harness.spec.server.template.v1.model.TemplateUpdateGitMetadataRequest;
import io.harness.spec.server.template.v1.model.TemplateUpdateRequestBody;

import com.google.inject.Inject;
import java.util.List;
import javax.validation.Valid;
import javax.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_PIPELINE})
@OwnedBy(CDC)
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@NextGenManagerAuth
@Slf4j
public class AccountTemplateApiImpl implements AccountTemplateApi {
  private final TemplateResourceApiHelper templateResourceApiHelper;
  @Override
  public Response createTemplatesAcc(
      TemplateCreateRequestBody templateCreateRequestBody, @AccountIdentifier String account) {
    GitCreateDetails gitCreateDetails = templateCreateRequestBody.getGitDetails();
    TemplateRequestInfoDTO requestInfoDTO =
        templateResourceApiHelper.mapCreateToRequestInfoDTO(templateCreateRequestBody);
    Boolean isStable = Boolean.TRUE.equals(templateCreateRequestBody.isIsStable());
    return templateResourceApiHelper.createTemplate(
        account, null, null, gitCreateDetails, requestInfoDTO, isStable, templateCreateRequestBody.getComments());
  }

  @Override
  public Response deleteTemplateAcc(@ResourceIdentifier String templateIdentifier, String versionLabel,
      @AccountIdentifier String account, String comments, Boolean forceDelete) {
    return templateResourceApiHelper.deleteTemplate(
        account, null, null, templateIdentifier, versionLabel, comments, Boolean.TRUE == forceDelete);
  }

  @Override
  public Response getAccTemplatesInputsSchema(String template, String version, String harnessAccount) {
    return templateResourceApiHelper.getInputsSchema(harnessAccount, null, null, template, version);
  }

  @Override
  public Response getTemplateAcc(@ResourceIdentifier String templateIdentifier, String versionLabel,
      @AccountIdentifier String account, Boolean getInputYaml, String branch, String parentConnectorRef,
      String parentRepoName, String parentAccountId, String parentOrgId, String parentProjectId) {
    return templateResourceApiHelper.getTemplate(account, null, null, templateIdentifier, versionLabel, false, branch,
        parentConnectorRef, parentRepoName, parentAccountId, parentOrgId, parentProjectId, getInputYaml);
  }

  @Override
  public Response getTemplateStableAcc(@ResourceIdentifier String templateIdentifier, @AccountIdentifier String account,
      Boolean getInputYaml, String branch, String parentConnectorRef, String parentRepoName, String parentAccountId,
      String parentOrgId, String parentProjectId) {
    return templateResourceApiHelper.getTemplate(account, null, null, templateIdentifier, null, false, branch,
        parentConnectorRef, parentRepoName, parentAccountId, parentOrgId, parentProjectId, getInputYaml);
  }

  @Override
  public Response getTemplatesListAcc(@AccountIdentifier String account, Integer page, Integer limit, String sort,
      String order, String searchTerm, String listType, Boolean recursive, List<String> names, List<String> identifiers,
      String description, List<String> entityTypes, List<String> childTypes) {
    return templateResourceApiHelper.getTemplates(account, null, null, page, limit, sort, order, searchTerm, listType,
        recursive, names, identifiers, description, entityTypes, childTypes);
  }

  @Override
  public Response importTemplateAcc(@ResourceIdentifier String template, @Valid TemplateImportRequestBody body,
      @AccountIdentifier String harnessAccount) {
    return templateResourceApiHelper.importTemplate(
        harnessAccount, null, null, template, body.getGitImportDetails(), body.getTemplateImportRequest());
  }

  @Override
  public Response updateGitMetadataDetails(
      String templateIdentifier, @Valid List<TemplateUpdateGitMetadataRequest> body, String harnessAccount) {
    return templateResourceApiHelper.updateGitMetaData(harnessAccount, null, null, templateIdentifier, body);
  }

  @Override
  public Response updateTemplateAcc(@ResourceIdentifier String templateIdentifier, String versionLabel,
      TemplateUpdateRequestBody templateUpdateRequestBody, @AccountIdentifier String account) {
    GitUpdateDetails gitUpdateDetails = templateUpdateRequestBody.getGitDetails();
    TemplateRequestInfoDTO requestInfoDTO =
        templateResourceApiHelper.mapUpdateToRequestInfoDTO(templateUpdateRequestBody);
    return templateResourceApiHelper.updateTemplate(account, null, null, templateIdentifier, versionLabel,
        gitUpdateDetails, requestInfoDTO, false, templateUpdateRequestBody.getComments());
  }

  @Override
  public Response updateTemplateStableAcc(@ResourceIdentifier String templateIdentifier, String versionLabel,
      GitFindDetails gitFindDetails, @AccountIdentifier String account) {
    return templateResourceApiHelper.updateStableTemplate(
        account, null, null, templateIdentifier, versionLabel, gitFindDetails, gitFindDetails.getComments());
  }
}
