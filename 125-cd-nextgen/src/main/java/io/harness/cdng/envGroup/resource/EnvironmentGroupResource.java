/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.envGroup.resource;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.ng.core.utils.NGUtils.validate;
import static io.harness.utils.PageUtils.getNGPageResponse;
import static io.harness.utils.PageUtils.getPageRequest;

import static java.lang.Long.parseLong;
import static javax.ws.rs.core.HttpHeaders.IF_MATCH;
import static org.apache.commons.lang3.StringUtils.isNumeric;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.OrgIdentifier;
import io.harness.accesscontrol.ProjectIdentifier;
import io.harness.accesscontrol.ResourceIdentifier;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SortOrder;
import io.harness.cdng.envGroup.beans.EnvironmentGroupEntity;
import io.harness.cdng.envGroup.beans.EnvironmentGroupEntity.EnvironmentGroupKeys;
import io.harness.cdng.envGroup.mappers.EnvironmentGroupMapper;
import io.harness.cdng.envGroup.services.EnvironmentGroupService;
import io.harness.gitsync.interceptor.GitEntityDeleteInfoDTO;
import io.harness.gitsync.interceptor.GitEntityFindInfoDTO;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.envGroup.dto.EnvironmentGroupDeleteResponse;
import io.harness.ng.core.envGroup.dto.EnvironmentGroupResponse;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.dto.EnvironmentResponse;
import io.harness.ng.core.environment.mappers.EnvironmentMapper;
import io.harness.ng.core.environment.services.EnvironmentService;
import io.harness.ng.core.utils.CoreCriteriaUtils;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.query.Criteria;

@NextGenManagerAuth
@Api("/environmentGroup")
@Path("/environmentGroup")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
// TODO: Need to uncomment this for customer to use the apis: @Tag(name = "EnvironmentGroup", description = "This
// contains APIs related to EnvironmentGroup")
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = NGCommonEntityConstants.BAD_REQUEST_CODE,
    description = NGCommonEntityConstants.BAD_REQUEST_PARAM_MESSAGE,
    content =
    {
      @Content(mediaType = NGCommonEntityConstants.APPLICATION_JSON_MEDIA_TYPE,
          schema = @Schema(implementation = FailureDTO.class))
      ,
          @Content(mediaType = NGCommonEntityConstants.APPLICATION_YAML_MEDIA_TYPE,
              schema = @Schema(implementation = FailureDTO.class))
    })
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = NGCommonEntityConstants.INTERNAL_SERVER_ERROR_CODE,
    description = NGCommonEntityConstants.INTERNAL_SERVER_ERROR_MESSAGE,
    content =
    {
      @Content(mediaType = NGCommonEntityConstants.APPLICATION_JSON_MEDIA_TYPE,
          schema = @Schema(implementation = ErrorDTO.class))
      ,
          @Content(mediaType = NGCommonEntityConstants.APPLICATION_YAML_MEDIA_TYPE,
              schema = @Schema(implementation = ErrorDTO.class))
    })
@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
public class EnvironmentGroupResource {
  private final EnvironmentGroupService environmentGroupService;
  private final EnvironmentService environmentService;
  public static final String ENVIRONMENT_GROUP_PARAM_MESSAGE = "Environment Group Identifier for the entity";

  @GET
  @Path("{envGroupIdentifier}")
  @ApiOperation(value = "Gets a Environment Group by identifier", nickname = "getEnvironmentGroup")
  @Operation(operationId = "getEnvironmentGroup", summary = "Gets an Environment Group by identifier",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "The saved Environment Group")
      })
  public ResponseDTO<EnvironmentGroupResponse>
  get(@Parameter(description = ENVIRONMENT_GROUP_PARAM_MESSAGE) @NotNull @PathParam(
          NGCommonEntityConstants.ENVIRONMENT_GROUP_KEY) String envGroupId,
      @Parameter(description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) @NotNull @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @Parameter(description = NGCommonEntityConstants.ORG_PARAM_MESSAGE) @NotNull @QueryParam(
          NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @Parameter(description = NGCommonEntityConstants.PROJECT_PARAM_MESSAGE) @NotNull @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @Parameter(description = "Specify whether Environment is deleted or not") @QueryParam(
          NGCommonEntityConstants.DELETED_KEY) @DefaultValue("false") boolean deleted,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo) {
    // TODO: NEED TO ADD RBAC
    Optional<EnvironmentGroupEntity> environmentGroupEntity =
        environmentGroupService.get(accountId, orgIdentifier, projectIdentifier, envGroupId, deleted);

    // fetching Environments from list of identifiers
    if (environmentGroupEntity.isPresent()) {
      List<EnvironmentResponse> envResponseList = getEnvironmentResponses(environmentGroupEntity.get());
      return ResponseDTO.newResponse(
          EnvironmentGroupMapper.toResponseWrapper(environmentGroupEntity.get(), envResponseList));
    }

    return null;
  }

  // Api to create an Environment Group
  @POST
  @ApiOperation(value = "Create an Environment Group", nickname = "createEnvironmentGroup")
  @Operation(operationId = "postEnvironmentGroup", summary = "Create an Environment Group",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "default",
            description =
                "If the YAML is valid, returns created Environment Group. If not, it sends what is wrong with the YAML")
      })
  public ResponseDTO<EnvironmentGroupResponse>
  create(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @Parameter(
             description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier @Parameter(
          description = NGCommonEntityConstants.ORG_PARAM_MESSAGE) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier @Parameter(
          description = NGCommonEntityConstants.PROJECT_PARAM_MESSAGE) String projectIdentifier,
      @RequestBody(required = true,
          description =
              "Environment Group YAML to be created. The Account, Org,  and Project identifiers inside the YAML should match the query parameters.")
      @NotNull String yaml,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo) {
    // TODO(PRASHANTSHARMA): need to add rbac and also check the validity of env identifiers passed in yaml
    EnvironmentGroupEntity entity =
        EnvironmentGroupMapper.toEnvironmentEntity(accountId, orgIdentifier, projectIdentifier, yaml);
    // Validate the fields of the Entity
    validate(entity);
    EnvironmentGroupEntity savedEntity = environmentGroupService.create(entity);

    // fetching Environments from list of identifiers
    List<EnvironmentResponse> envResponseList = null;
    envResponseList = getEnvironmentResponses(savedEntity);

    return ResponseDTO.newResponse(EnvironmentGroupMapper.toResponseWrapper(savedEntity, envResponseList));
  }

  @GET
  @Path("/list")
  @ApiOperation(value = "Gets Environment Group list", nickname = "getEnvironmentGroupList")
  @Operation(operationId = "getEnvironmentGroupList", summary = "Gets Environment Group list for a Project",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns the list of Environment Group for a Project")
      })
  public ResponseDTO<PageResponse<EnvironmentGroupResponse>>
  listEnvironmentGroup(@Parameter(description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) @NotNull @QueryParam(
                           NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @Parameter(description = NGCommonEntityConstants.ORG_PARAM_MESSAGE) @NotNull @QueryParam(
          NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @Parameter(description = NGCommonEntityConstants.PROJECT_PARAM_MESSAGE) @NotNull @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) @ResourceIdentifier String projectIdentifier,
      @Parameter(description = "The word to be searched and included in the list response") @QueryParam(
          NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm,
      @BeanParam PageRequest pageRequest, @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo) {
    Criteria criteria = CoreCriteriaUtils.createCriteriaForGetList(accountId, orgIdentifier, projectIdentifier, false);

    if (isEmpty(pageRequest.getSortOrders())) {
      SortOrder order = SortOrder.Builder.aSortOrder()
                            .withField(EnvironmentGroupKeys.lastModifiedAt, SortOrder.OrderType.DESC)
                            .build();
      pageRequest.setSortOrders(ImmutableList.of(order));
    }
    Page<EnvironmentGroupEntity> envGroupEntities = environmentGroupService.list(
        criteria, getPageRequest(pageRequest), projectIdentifier, orgIdentifier, accountId);

    return ResponseDTO.newResponse(getNGPageResponse(envGroupEntities.map(
        envGroup -> EnvironmentGroupMapper.toResponseWrapper(envGroup, getEnvironmentResponses(envGroup)))));
  }

  @DELETE
  @Path("{envGroupIdentifier}")
  @ApiOperation(value = "Delete en Environment Group by Identifier", nickname = "deleteEnvironmentGroup")
  @Operation(operationId = "deleteEnvironmentGroup", summary = "Delete en Environment Group by Identifier",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns true if the Environment Group is deleted")
      })
  public ResponseDTO<EnvironmentGroupDeleteResponse>
  delete(@HeaderParam(IF_MATCH) String ifMatch,
      @Parameter(description = ENVIRONMENT_GROUP_PARAM_MESSAGE) @PathParam(
          NGCommonEntityConstants.ENVIRONMENT_GROUP_KEY) String envGroupId,
      @Parameter(description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) @NotNull @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @Parameter(description = NGCommonEntityConstants.ORG_PARAM_MESSAGE) @NotNull @QueryParam(
          NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @Parameter(description = NGCommonEntityConstants.PROJECT_PARAM_MESSAGE) @NotNull @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @BeanParam GitEntityDeleteInfoDTO entityDeleteInfo) {
    // TODO: set up usages of env group as well as env linked with it. RBAC
    log.info(String.format("Delete Environment group Api %s", envGroupId));
    EnvironmentGroupEntity deletedEntity = environmentGroupService.delete(
        accountId, orgIdentifier, projectIdentifier, envGroupId, isNumeric(ifMatch) ? parseLong(ifMatch) : null);
    return ResponseDTO.newResponse(EnvironmentGroupMapper.toDeleteResponseWrapper(deletedEntity));
  }

  private List<EnvironmentResponse> getEnvironmentResponses(EnvironmentGroupEntity groupEntity) {
    List<EnvironmentResponse> envResponseList = null;

    List<Environment> envList =
        environmentService.fetchesNonDeletedEnvironmentFromListOfIdentifiers(groupEntity.getAccountId(),
            groupEntity.getOrgIdentifier(), groupEntity.getProjectIdentifier(), groupEntity.getEnvIdentifiers());
    envResponseList = EnvironmentMapper.toResponseWrapper(envList);

    return envResponseList;
  }
}
