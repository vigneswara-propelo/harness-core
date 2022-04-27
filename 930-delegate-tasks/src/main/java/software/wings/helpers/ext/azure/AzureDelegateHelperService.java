/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.helpers.ext.azure;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.encoding.EncodingUtils.decodeBase64ToString;
import static io.harness.eraro.ErrorCode.AZURE_SERVICE_EXCEPTION;
import static io.harness.eraro.ErrorCode.CLUSTER_NOT_FOUND;
import static io.harness.exception.WingsException.USER;
import static io.harness.network.Http.getOkHttpClientBuilder;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.azure.AzureEnvironmentType;
import io.harness.exception.AzureServiceException;
import io.harness.exception.ClusterNotFoundException;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.k8s.KubeConfigHelper;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.network.Http;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AzureConfig;
import software.wings.beans.AzureKubernetesCluster;
import software.wings.service.intfc.security.EncryptionService;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.microsoft.aad.adal4j.AuthenticationException;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.containerservice.KubernetesCluster;
import com.microsoft.rest.LogLevel;
import io.kubernetes.client.util.KubeConfig;
import java.io.StringReader;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.http.HttpStatus;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

@OwnedBy(CDC)
@Singleton
@Slf4j
public class AzureDelegateHelperService {
  private static final int CONNECT_TIMEOUT = 5; // TODO:: read from config
  private static final int READ_TIMEOUT = 10;

  @Inject private EncryptionService encryptionService;

  public boolean isValidKubernetesCluster(AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails,
      String subscriptionId, String resourceGroup, String clusterName) {
    encryptionService.decrypt(azureConfig, encryptionDetails, false);
    KubernetesCluster cluster =
        getAzureClient(azureConfig, subscriptionId).kubernetesClusters().getByResourceGroup(resourceGroup, clusterName);
    return cluster != null;
  }

  public KubernetesConfig getKubernetesClusterConfig(AzureConfig azureConfig,
      List<EncryptedDataDetail> encryptionDetails, AzureKubernetesCluster azureKubernetesCluster, String namespace,
      boolean isInstanceSync) {
    return getKubernetesClusterConfig(azureConfig, encryptionDetails, azureKubernetesCluster.getSubscriptionId(),
        azureKubernetesCluster.getResourceGroup(), azureKubernetesCluster.getName(), namespace, isInstanceSync);
  }

  public KubernetesConfig getKubernetesClusterConfig(AzureConfig azureConfig,
      List<EncryptedDataDetail> encryptionDetails, String subscriptionId, String resourceGroup, String clusterName,
      String namespace, boolean isInstanceSync) {
    encryptionService.decrypt(azureConfig, encryptionDetails, isInstanceSync);
    try {
      Response<AksGetCredentialsResponse> response =
          getAzureManagementRestClient(azureConfig.getAzureEnvironmentType())
              .getAdminCredentials(getAzureBearerAuthToken(azureConfig), subscriptionId, resourceGroup, clusterName)
              .execute();

      if (response.isSuccessful()) {
        return parseConfig(
            response.body().getProperties().getKubeConfig(), isNotBlank(namespace) ? namespace : "default");
      } else {
        String errorMessage =
            "Error occurred while getting KubernetesClusterConfig from subscriptionId/resourceGroup/clusterName :"
            + subscriptionId + "/" + resourceGroup + "/" + clusterName + response.raw();
        log.error(errorMessage);
        int statusCode = response.code();
        if (statusCode == HttpStatus.SC_NOT_FOUND) {
          throw new ClusterNotFoundException(errorMessage, CLUSTER_NOT_FOUND, USER);
        } else {
          throw new AzureServiceException(response.message(), AZURE_SERVICE_EXCEPTION, USER);
        }
      }
    } catch (Exception e) {
      handleAzureAuthenticationException(e);
    }
    return null;
  }

  AzureManagementRestClient getAzureManagementRestClient(AzureEnvironmentType azureEnvironmentType) {
    String url = getAzureEnvironment(azureEnvironmentType).resourceManagerEndpoint();
    OkHttpClient okHttpClient = getOkHttpClientBuilder()
                                    .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                                    .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                                    .proxy(Http.checkAndGetNonProxyIfApplicable(url))
                                    .retryOnConnectionFailure(true)
                                    .build();
    Retrofit retrofit = new Retrofit.Builder()
                            .client(okHttpClient)
                            .baseUrl(url)
                            .addConverterFactory(JacksonConverterFactory.create())
                            .build();
    return retrofit.create(AzureManagementRestClient.class);
  }

  private AzureEnvironment getAzureEnvironment(AzureEnvironmentType azureEnvironmentType) {
    if (azureEnvironmentType == null) {
      return AzureEnvironment.AZURE;
    }

    switch (azureEnvironmentType) {
      case AZURE_US_GOVERNMENT:
        return AzureEnvironment.AZURE_US_GOVERNMENT;

      case AZURE:
      default:
        return AzureEnvironment.AZURE;
    }
  }

  @VisibleForTesting
  String getAzureBearerAuthToken(AzureConfig azureConfig) {
    try {
      AzureEnvironment azureEnvironment = getAzureEnvironment(azureConfig.getAzureEnvironmentType());
      ApplicationTokenCredentials credentials = new ApplicationTokenCredentials(
          azureConfig.getClientId(), azureConfig.getTenantId(), new String(azureConfig.getKey()), azureEnvironment);

      String token = credentials.getToken(azureEnvironment.managementEndpoint());
      return "Bearer " + token;
    } catch (Exception e) {
      handleAzureAuthenticationException(e);
    }
    return null;
  }

  @VisibleForTesting
  protected Azure getAzureClient(AzureConfig azureConfig, String subscriptionId) {
    try {
      ApplicationTokenCredentials credentials =
          new ApplicationTokenCredentials(azureConfig.getClientId(), azureConfig.getTenantId(),
              new String(azureConfig.getKey()), getAzureEnvironment(azureConfig.getAzureEnvironmentType()));

      return Azure.configure().withLogLevel(LogLevel.NONE).authenticate(credentials).withSubscription(subscriptionId);
    } catch (Exception e) {
      handleAzureAuthenticationException(e);
    }
    return null;
  }

  private KubernetesConfig parseConfig(String configContent, String namespace) {
    try {
      KubeConfig kubeConfig = KubeConfig.loadKubeConfig(new StringReader(decodeBase64ToString(configContent)));
      String masterUrl = kubeConfig.getServer();
      String certificateAuthorityData = kubeConfig.getCertificateAuthorityData();
      String username = KubeConfigHelper.getCurrentUser(kubeConfig);
      String clientCertificateData = kubeConfig.getClientCertificateData();
      String clientKeyData = kubeConfig.getClientKeyData();

      return KubernetesConfig.builder()
          .namespace(namespace)
          .masterUrl(masterUrl)
          .caCert(certificateAuthorityData.toCharArray())
          .username(username != null ? username.toCharArray() : null)
          .clientCert(clientCertificateData.toCharArray())
          .clientKey(clientKeyData.toCharArray())
          .build();
    } catch (Exception e) {
      throw new AzureServiceException(
          "Failed to create kubernetes configuration " + ExceptionUtils.getMessage(e), AZURE_SERVICE_EXCEPTION, USER);
    }
  }

  public void handleAzureAuthenticationException(Exception e) {
    log.error("HandleAzureAuthenticationException: Exception:" + e);

    Throwable e1 = e;
    while (e1.getCause() != null) {
      e1 = e1.getCause();
      if (e1 instanceof AuthenticationException) {
        throw new InvalidRequestException("Invalid Azure credentials.", USER);
      }
    }

    throw new InvalidRequestException("Failed to connect to Azure cluster. " + ExceptionUtils.getMessage(e), USER);
  }
}
