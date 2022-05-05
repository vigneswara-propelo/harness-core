/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.variable.resources;

import static io.harness.NGCommonEntityConstants.ACCOUNT_KEY;
import static io.harness.NGCommonEntityConstants.ORG_KEY;
import static io.harness.NGCommonEntityConstants.PROJECT_KEY;
import static io.harness.NGResourceFilterConstants.PAGE_KEY;
import static io.harness.NGResourceFilterConstants.SEARCH_TERM_KEY;
import static io.harness.NGResourceFilterConstants.SIZE_KEY;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.variable.dto.VariableRequestDTO;
import io.harness.ng.core.variable.dto.VariableResponseDTO;
import io.harness.ng.core.variable.entity.Variable;
import io.harness.ng.core.variable.mappers.VariableMapper;
import io.harness.ng.core.variable.services.VariableService;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Hidden;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import lombok.AllArgsConstructor;

@OwnedBy(HarnessTeam.PL)
@Api("/variables")
@Path("/variables")
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON})
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class VariableResource {
  private static final String INCLUDE_VARIABLES_FROM_EVERY_SUB_SCOPE = "includeVariablesFromEverySubScope";
  private final VariableService variableService;
  private final VariableMapper variableMapper;

  @POST
  @ApiOperation(value = "Create a Variable", nickname = "createVariable")
  @Hidden
  public ResponseDTO<VariableResponseDTO> create(@QueryParam(ACCOUNT_KEY) @NotNull String accountIdentifier,
      @NotNull @Valid VariableRequestDTO variableRequestDTO) {
    // TODO: access control check
    Variable createdVariable = variableService.create(accountIdentifier, variableRequestDTO.getVariable());
    return ResponseDTO.newResponse(variableMapper.toResponseWrapper(createdVariable));
  }

  @GET
  @ApiOperation(value = "Gets Variable list", nickname = "getVariablesList")
  @Hidden
  public ResponseDTO<PageResponse<VariableResponseDTO>> list(@NotNull @QueryParam(ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(ORG_KEY) String orgIdentifier, @QueryParam(PROJECT_KEY) String projectIdentifier,
      @QueryParam(PAGE_KEY) @DefaultValue("0") int page, @QueryParam(SIZE_KEY) @DefaultValue("100") int size,
      @QueryParam(SEARCH_TERM_KEY) String searchTerm,
      @QueryParam(INCLUDE_VARIABLES_FROM_EVERY_SUB_SCOPE) @DefaultValue(
          "false") boolean includeVariablesFromEverySubScope) {
    return ResponseDTO.newResponse(variableService.list(accountIdentifier, orgIdentifier, projectIdentifier, page, size,
        searchTerm, includeVariablesFromEverySubScope));
  }
}
