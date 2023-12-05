/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.api;

import io.harness.annotations.SSCAServiceAuth;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SortOrder;
import io.harness.ng.beans.PageRequest;
import io.harness.spec.server.ssca.v1.ConfigApi;
import io.harness.spec.server.ssca.v1.model.ConfigRequestBody;
import io.harness.spec.server.ssca.v1.model.ConfigResponseBody;
import io.harness.spec.server.ssca.v1.model.SaveResponse;
import io.harness.ssca.services.ConfigService;
import io.harness.ssca.utils.PageResponseUtils;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import java.util.Arrays;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.core.Response;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@OwnedBy(HarnessTeam.SSCA)
@SSCAServiceAuth
public class ConfigApiImpl implements ConfigApi {
  @Inject ConfigService configService;
  @Override
  public Response deleteConfigById(String org, String project, String configId, String harnessAccount) {
    configService.deleteConfigById(org, project, configId, harnessAccount);
    return Response.status(Response.Status.NO_CONTENT).build();
  }

  @Override
  public Response getConfigById(String org, String project, String configId, String harnessAccount) {
    ConfigResponseBody response = configService.getConfigById(org, project, configId, harnessAccount);
    return Response.status(Response.Status.OK).entity(response).build();
  }

  @Override
  public Response getConfigByNameAndType(String org, String project, String name, String type, String harnessAccount) {
    ConfigResponseBody response = configService.getConfigByNameAndType(org, project, name, type, harnessAccount);
    return Response.status(Response.Status.OK).entity(response).build();
  }

  @Override
  public Response listConfigs(String org, String project, String harnessAccount, @Min(1L) @Max(1000L) Integer limit,
      @Min(0L) Integer page, String order, String sort) {
    SortOrder sortOrder = new SortOrder();
    sort = ConfigApiUtils.getSortFieldMapping(sort);
    sortOrder.setFieldName(sort);
    sortOrder.setOrderType(SortOrder.OrderType.valueOf(order));
    Pageable pageable = PageUtils.getPageRequest(new PageRequest(page, limit, Arrays.asList(sortOrder)));

    Page<ConfigResponseBody> listConfigs = configService.listConfigs(org, project, harnessAccount, pageable);

    return PageResponseUtils.getPagedResponse(listConfigs);
  }

  @Override
  public Response saveConfig(String org, String project, @Valid ConfigRequestBody body, String harnessAccount) {
    configService.saveConfig(org, project, body, harnessAccount);
    return Response.status(Response.Status.CREATED).entity(new SaveResponse().status("SUCCESS")).build();
  }

  @Override
  public Response updateConfigById(
      String org, String project, String configId, @Valid ConfigRequestBody body, String harnessAccount) {
    configService.updateConfigById(org, project, configId, body, harnessAccount);
    return Response.status(Response.Status.OK).entity(new SaveResponse().status("SUCCESS")).build();
  }
}
