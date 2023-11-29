/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.containerStepGroup;

import static io.harness.ci.commonconstants.CIExecutionConstants.PATH_SEPARATOR;
import static io.harness.ci.commonconstants.ContainerExecutionConstants.STEP_MOUNT_PATH;
import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;

import io.harness.beans.IdentifierRef;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialSpecDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.NGAccess;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.utils.IdentifierRefHelper;
import io.harness.yaml.utils.NGVariablesUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

public class DownloadAwsS3StepHelper {
  @Named(DEFAULT_CONNECTOR_SERVICE) @Inject private ConnectorService connectorService;

  public Map<String, String> getEnvironmentVariables(
      Ambiance ambiance, DownloadAwsS3StepParameters downloadAwsS3StepParameters, String stepIdentifier) {
    String awsConnectorRef = downloadAwsS3StepParameters.getConnectorRef() == null
        ? null
        : downloadAwsS3StepParameters.getConnectorRef().getValue();
    String crossAccountRoleArn = null;
    String externalId = null;

    String awsAccessKey = null;
    String awsSecretKey = null;
    String region =
        downloadAwsS3StepParameters.getRegion() == null ? null : downloadAwsS3StepParameters.getRegion().getValue();

    if (awsConnectorRef != null) {
      NGAccess ngAccess = AmbianceUtils.getNgAccess(ambiance);

      Optional<ConnectorResponseDTO> connectorDTO = getOptionalConnectorResponseDTO(awsConnectorRef, ngAccess);

      if (connectorDTO.isEmpty()) {
        String errorMessage = "Aws connector configured in Download AWS S3 Step is empty";
        throw new InvalidRequestException(errorMessage);
      }
      ConnectorInfoDTO connectorInfoDTO = connectorDTO.get().getConnector();
      ConnectorConfigDTO connectorConfigDTO = connectorInfoDTO.getConnectorConfig();
      AwsConnectorDTO awsConnectorDTO = (AwsConnectorDTO) connectorConfigDTO;
      AwsCredentialDTO awsCredentialDTO = awsConnectorDTO.getCredential();
      AwsCredentialSpecDTO awsCredentialSpecDTO = awsCredentialDTO.getConfig();

      if (awsCredentialSpecDTO instanceof AwsManualConfigSpecDTO) {
        AwsManualConfigSpecDTO awsManualConfigSpecDTO = (AwsManualConfigSpecDTO) awsCredentialSpecDTO;

        if (!StringUtils.isEmpty(awsManualConfigSpecDTO.getAccessKey())) {
          awsAccessKey = awsManualConfigSpecDTO.getAccessKey();
        } else {
          awsAccessKey = NGVariablesUtils.fetchSecretExpressionWithExpressionToken(
              awsManualConfigSpecDTO.getAccessKeyRef().toSecretRefStringValue(), ambiance.getExpressionFunctorToken());
        }

        awsSecretKey = NGVariablesUtils.fetchSecretExpressionWithExpressionToken(
            awsManualConfigSpecDTO.getSecretKeyRef().toSecretRefStringValue(), ambiance.getExpressionFunctorToken());
      }

      if (awsCredentialDTO.getCrossAccountAccess() != null) {
        crossAccountRoleArn = awsCredentialDTO.getCrossAccountAccess().getCrossAccountRoleArn();
        externalId = awsCredentialDTO.getCrossAccountAccess().getExternalId();
      }
    }

    HashMap<String, String> envVarsMap = new HashMap<>();

    if (downloadAwsS3StepParameters.getBucketName() != null
        && EmptyPredicate.isNotEmpty(downloadAwsS3StepParameters.getBucketName().getValue())) {
      envVarsMap.put("PLUGIN_AWS_BUCKET_NAME", downloadAwsS3StepParameters.getBucketName().getValue());
    }

    if (downloadAwsS3StepParameters.getPaths() != null
        && EmptyPredicate.isNotEmpty(downloadAwsS3StepParameters.getPaths().getValue())) {
      if (downloadAwsS3StepParameters.getPaths().getValue().size() > 1) {
        throw new InvalidRequestException("Only one path is allowed in Download Aws S3 Step");
      } else {
        envVarsMap.put("PLUGIN_STORE_PATH", downloadAwsS3StepParameters.getPaths().getValue().get(0));
      }
    }

    if (downloadAwsS3StepParameters.getDownloadPath() != null
        && EmptyPredicate.isNotEmpty(downloadAwsS3StepParameters.getDownloadPath().getValue())) {
      envVarsMap.put("PLUGIN_DOWNLOAD_PATH", downloadAwsS3StepParameters.getDownloadPath().getValue());
    } else if (EmptyPredicate.isNotEmpty(stepIdentifier)) {
      envVarsMap.put("PLUGIN_DOWNLOAD_PATH", STEP_MOUNT_PATH + PATH_SEPARATOR + stepIdentifier);
    }

    if (downloadAwsS3StepParameters.getOutputFilePathsContent() != null
        && EmptyPredicate.isNotEmpty(downloadAwsS3StepParameters.getOutputFilePathsContent().getValue())) {
      envVarsMap.put("PLUGIN_OUTPUT_FILE_PATHS_CONTENT",
          String.join(",", downloadAwsS3StepParameters.getOutputFilePathsContent().getValue()));
    }

    if (awsAccessKey != null) {
      envVarsMap.put("PLUGIN_AWS_ACCESS_KEY", awsAccessKey);
    }

    if (awsSecretKey != null) {
      envVarsMap.put("PLUGIN_AWS_SECRET_KEY", awsSecretKey);
    }

    if (crossAccountRoleArn != null) {
      envVarsMap.put("PLUGIN_AWS_ROLE_ARN", crossAccountRoleArn);
    }

    if (externalId != null) {
      envVarsMap.put("PLUGIN_AWS_STS_EXTERNAL_ID", externalId);
    }

    if (region != null) {
      envVarsMap.put("PLUGIN_REGION", region);
    }

    return envVarsMap;
  }

  private Optional<ConnectorResponseDTO> getOptionalConnectorResponseDTO(String awsConnectorRef, NGAccess ngAccess) {
    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(
        awsConnectorRef, ngAccess.getAccountIdentifier(), ngAccess.getOrgIdentifier(), ngAccess.getProjectIdentifier());

    return connectorService.get(identifierRef.getAccountIdentifier(), identifierRef.getOrgIdentifier(),
        identifierRef.getProjectIdentifier(), identifierRef.getIdentifier());
  }
}
