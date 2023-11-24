/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.gitxwebhook;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.Scope;
import io.harness.gitsync.gitxwebhooks.dtos.GitXEventsListRequestDTO;
import io.harness.gitsync.gitxwebhooks.dtos.GitXEventsListResponseDTO;
import io.harness.gitsync.gitxwebhooks.mapper.GitXWebhookMapper;
import io.harness.gitsync.gitxwebhooks.service.GitXWebhookEventService;
import io.harness.spec.server.ng.v1.ProjectGitxWebhooksEventsApi;
import io.harness.spec.server.ng.v1.model.GitXWebhookEventResponse;
import io.harness.utils.ApiUtils;

import com.google.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.Max;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_GITX})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
public class ProjectGitXWebhookEventsApiImpl implements ProjectGitxWebhooksEventsApi {
  GitXWebhookEventService gitXWebhookEventService;

  @Override
  public Response listProjectGitxWebhookEvents(String org, String project, String harnessAccount, Integer page,
      @Max(1000L) Integer limit, String webhookIdentifier, Long eventStartTime, Long eventEndTime, String repoName,
      String filePath, String eventIdentifier, List<String> eventStatus) {
    GitXEventsListRequestDTO gitXEventsListRequestDTO =
        GitXWebhookMapper.buildEventsListGitXWebhookRequestDTO(Scope.of(harnessAccount, org, project),
            webhookIdentifier, eventStartTime, eventEndTime, repoName, filePath, eventIdentifier, eventStatus);
    GitXEventsListResponseDTO gitXEventsListResponseDTO = gitXWebhookEventService.listEvents(gitXEventsListRequestDTO);

    Page<GitXWebhookEventResponse> gitXWebhookEvents =
        GitXWebhookMapper.buildListGitXWebhookEventResponse(gitXEventsListResponseDTO, page, limit);
    ResponseBuilder responseBuilder = Response.ok();
    ResponseBuilder responseBuilderWithLinks =
        ApiUtils.addLinksHeader(responseBuilder, gitXWebhookEvents.getTotalElements(), page, limit);
    return responseBuilderWithLinks
        .entity(gitXWebhookEvents.getContent()
                    .stream()
                    .map(GitXWebhookMapper::buildGitXWebhookEventResponse)
                    .collect(Collectors.toList()))
        .build();
  }
}
