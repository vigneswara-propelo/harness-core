/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.servicenow;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EnvironmentType;
import io.harness.beans.IdentifierRef;
import io.harness.common.NGTaskType;
import io.harness.common.NGTimeConversionHelper;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorResourceClient;
import io.harness.delegate.TaskSelector;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.servicenow.ServiceNowConnectorDTO;
import io.harness.delegate.task.servicenow.ServiceNowTaskNGParameters;
import io.harness.delegate.task.servicenow.ServiceNowTaskNGParameters.ServiceNowTaskNGParametersBuilder;
import io.harness.delegate.task.servicenow.ServiceNowTaskNGResponse;
import io.harness.encryption.Scope;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.ng.core.NGAccess;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.tasks.TaskCategory;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.remote.client.NGRestUtils;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepUtils;
import io.harness.steps.servicenow.ServiceNowStepHelperService;
import io.harness.steps.servicenow.ServiceNowTicketOutcome;
import io.harness.supplier.ThrowingSupplier;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

@OwnedBy(CDC)
public class ServiceNowStepHelperServiceImpl implements ServiceNowStepHelperService {
  private final ConnectorResourceClient connectorResourceClient;
  private final SecretManagerClientService secretManagerClientService;
  private final KryoSerializer kryoSerializer;

  @Inject
  public ServiceNowStepHelperServiceImpl(ConnectorResourceClient connectorResourceClient,
      @Named("PRIVILEGED") SecretManagerClientService secretManagerClientService, KryoSerializer kryoSerializer) {
    this.connectorResourceClient = connectorResourceClient;
    this.secretManagerClientService = secretManagerClientService;
    this.kryoSerializer = kryoSerializer;
  }

  @Override
  public TaskRequest prepareTaskRequest(ServiceNowTaskNGParametersBuilder paramsBuilder, Ambiance ambiance,
      String connectorRef, String timeStr, String taskName) {
    NGAccess ngAccess = AmbianceUtils.getNgAccess(ambiance);
    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(
        connectorRef, ngAccess.getAccountIdentifier(), ngAccess.getOrgIdentifier(), ngAccess.getProjectIdentifier());
    Optional<ConnectorDTO> connectorDTOOptional = NGRestUtils.getResponse(
        connectorResourceClient.get(identifierRef.getIdentifier(), identifierRef.getAccountIdentifier(),
            identifierRef.getOrgIdentifier(), identifierRef.getProjectIdentifier()));
    if (!connectorDTOOptional.isPresent()) {
      throw new InvalidRequestException(
          String.format("Connector not found for identifier: [%s]", connectorRef), WingsException.USER);
    }

    ConnectorConfigDTO configDTO = connectorDTOOptional.get().getConnectorInfo().getConnectorConfig();
    if (!(configDTO instanceof ServiceNowConnectorDTO)) {
      throw new InvalidRequestException(
          String.format("Connector [%s] is not a ServiceNow connector", connectorRef), WingsException.USER);
    }

    ServiceNowConnectorDTO connectorDTO = (ServiceNowConnectorDTO) configDTO;
    paramsBuilder.serviceNowConnectorDTO(connectorDTO);
    paramsBuilder.encryptionDetails(secretManagerClientService.getEncryptionDetails(ngAccess, connectorDTO));
    ServiceNowTaskNGParameters params = paramsBuilder.build();

    TaskData taskData = TaskData.builder()
                            .async(true)
                            .timeout(NGTimeConversionHelper.convertTimeStringToMilliseconds(timeStr))
                            .taskType(NGTaskType.SERVICENOW_TASK_NG.name())
                            .parameters(new Object[] {params})
                            .build();
    return StepUtils.prepareTaskRequest(ambiance, taskData, kryoSerializer, TaskCategory.DELEGATE_TASK_V2,
        Collections.emptyList(), false, taskName,
        params.getDelegateSelectors()
            .stream()
            .map(s -> TaskSelector.newBuilder().setSelector(s).build())
            .collect(Collectors.toList()),
        Scope.PROJECT, EnvironmentType.ALL);
  }

  @Override
  public StepResponse prepareStepResponse(ThrowingSupplier<ServiceNowTaskNGResponse> responseSupplier)
      throws Exception {
    ServiceNowTaskNGResponse taskResponse = responseSupplier.get();
    return StepResponse.builder()
        .status(Status.SUCCEEDED)
        .stepOutcome(StepResponse.StepOutcome.builder()
                         .name("ticket")
                         .outcome(new ServiceNowTicketOutcome(
                             taskResponse.getTicket().getUrl(), taskResponse.getTicket().getNumber()))
                         .build())
        .build();
  }
}
