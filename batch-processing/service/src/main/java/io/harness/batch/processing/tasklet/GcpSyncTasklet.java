/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.batch.processing.tasklet;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.cloudevents.aws.ecs.service.tasklet.support.ng.NGConnectorHelper;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.config.BillingDataPipelineConfig;
import io.harness.batch.processing.config.GcpSyncSmpConfig;
import io.harness.batch.processing.dao.intfc.BatchJobScheduledDataDao;
import io.harness.ccm.commons.beans.JobConstants;
import io.harness.ccm.commons.entities.batch.BatchJobScheduledData;
import io.harness.ccm.config.GcpBillingAccount;
import io.harness.ccm.config.GcpServiceAccount;
import io.harness.configuration.DeployMode;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResourceClient;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.delegate.beans.connector.CEFeatures;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.gcpccm.GcpCloudCostConnectorDTO;

import software.wings.service.intfc.instance.CloudToHarnessMappingService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.api.core.ApiFuture;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.paging.Page;
import com.google.api.gax.rpc.FixedHeaderProvider;
import com.google.api.gax.rpc.HeaderProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ImpersonatedCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.Dataset;
import com.google.cloud.bigquery.DatasetId;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMapKeySelector;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1EnvVarSource;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobSpec;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import io.kubernetes.client.openapi.models.V1SecretKeySelector;
import io.kubernetes.client.util.ClientBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Singleton
public class GcpSyncTasklet implements Tasklet {
  private static final String K8S_API_VERSION = "batch/v1";
  private static final String K8S_JOB_KIND = "Job";
  private static final String RESTART_POLICY_NEVER = "Never";
  private static final String PYTHON_JOB_NAME_PREFIX = "gcp-sync-k8s-job-%s";
  private static final String CLICKHOUSE_ENABLED = "CLICKHOUSE_ENABLED";
  private static final String CLICKHOUSE_URL = "CLICKHOUSE_URL";
  private static final String CLICKHOUSE_URL_PYTHON = "CLICKHOUSE_URL_PYTHON";
  private static final String CLICKHOUSE_USERNAME = "CLICKHOUSE_USERNAME";
  private static final String CLICKHOUSE_PASSWORD = "CLICKHOUSE_PASSWORD";
  private static final String CLICKHOUSE_SEND_RECEIVE_TIMEOUT = "CLICKHOUSE_SEND_RECEIVE_TIMEOUT";
  private static final String CLICKHOUSE_PORT = "CLICKHOUSE_PORT";
  private static final String CLICKHOUSE_PORT_PYTHON = "CLICKHOUSE_PORT_PYTHON";
  private static final String CLICKHOUSE_QUERY_RETRIES = "CLICKHOUSE_QUERY_RETRIES";
  private static final String USER_AGENT_HEADER = "user-agent";
  private static final String USER_AGENT_HEADER_ENVIRONMENT_VARIABLE = "USER_AGENT_HEADER";
  private static final String DEFAULT_USER_AGENT = "default-user-agent";
  private static final String SERVICE_ACCOUNT = "serviceAccount";
  private static final String SOURCE_DATA_SET_ID = "sourceDataSetId";
  private static final String SOURCE_GCP_PROJECT_ID = "sourceGcpProjectId";
  private static final String SOURCE_DATA_SET_REGION = "sourceDataSetRegion";
  private static final String CONNECTOR_ID = "connectorId";
  private static final String ACCOUNT_ID = "accountId";
  private static final String GCP_BILLING_EXPORT_V_1 = "gcp_billing_export_v1";
  private static final String SOURCE_GCP_TABLE_NAME = "sourceGcpTableName";
  private static final String TRIGGER_HISTORICAL_COST_UPDATE_IN_PREFERRED_CURRENCY =
      "triggerHistoricalCostUpdateInPreferredCurrency";
  private static final String DEPLOY_MODE = "deployMode";
  private static final String USE_WORKLOAD_IDENTITY = "useWorkloadIdentity";
  private static final String NAMESPACE = System.getenv("NAMESPACE");
  private static final String GOOGLE_CREDENTIALS_PATH = "CE_GCP_CREDENTIALS_PATH";
  private static final String SERVICE_ACCOUNT_CREDENTIALS = "SERVICE_ACCOUNT_CREDENTIALS";
  private static final String JOB_TYPE = "job-type";
  private static final String GCP_SYNC_K8S_JOB = "gcp-sync-k8s-job";
  private static final int TTL_SECONDS_AFTER_FINISHED_7_DAYS = 604800;
  private final Cache<CacheKey, Boolean> gcpSyncInfo =
      Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).build();

  @Autowired private BatchMainConfig config;
  @Autowired private ConnectorResourceClient connectorResourceClient;
  @Autowired protected CloudToHarnessMappingService cloudToHarnessMappingService;
  @Autowired private BatchJobScheduledDataDao batchJobScheduledDataDao;
  @Autowired private NGConnectorHelper ngConnectorHelper;

  @Value
  private static class CacheKey {
    private String accountId;
    private String projectId;
    private String datasetId;
    private String tableId;
  }

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
    JobConstants jobConstants = CCMJobConstants.fromContext(chunkContext);
    String accountId = jobConstants.getAccountId();
    Long endTime = jobConstants.getJobEndTime();
    boolean firstSync = false;
    BatchJobScheduledData batchJobScheduledData =
        batchJobScheduledDataDao.fetchLastBatchJobScheduledData(accountId, BatchJobType.SYNC_BILLING_REPORT_GCP);
    if (null != batchJobScheduledData) {
      endTime = batchJobScheduledData.getEndAt().toEpochMilli();
    } else {
      firstSync = true;
    }
    BillingDataPipelineConfig billingDataPipelineConfig = config.getBillingDataPipelineConfig();
    if (billingDataPipelineConfig.isGcpSyncEnabled()) {
      List<ConnectorResponseDTO> nextGenGCPConnectorResponses = getNextGenGCPConnectorResponses(accountId);
      for (ConnectorResponseDTO connector : nextGenGCPConnectorResponses) {
        ConnectorInfoDTO connectorInfo = connector.getConnector();
        GcpCloudCostConnectorDTO gcpCloudCostConnectorDTO =
            (GcpCloudCostConnectorDTO) connectorInfo.getConnectorConfig();
        log.info("ServiceAccountEmail: {}, DatasetId: {}, "
                + "ProjectId: {}, TableId: {}, "
                + "ConnectorIdentifier: {}",
            gcpCloudCostConnectorDTO.getServiceAccountEmail(),
            gcpCloudCostConnectorDTO.getBillingExportSpec().getDatasetId(), gcpCloudCostConnectorDTO.getProjectId(),
            gcpCloudCostConnectorDTO.getBillingExportSpec().getTableId(), connectorInfo.getIdentifier());
        try {
          processGCPConnector(billingDataPipelineConfig, gcpCloudCostConnectorDTO.getServiceAccountEmail(),
              gcpCloudCostConnectorDTO.getBillingExportSpec().getDatasetId(), gcpCloudCostConnectorDTO.getProjectId(),
              gcpCloudCostConnectorDTO.getBillingExportSpec().getTableId(), accountId, connectorInfo.getIdentifier(),
              endTime, firstSync);
        } catch (Exception e) {
          log.error("Exception processing NG GCP Connector: {}", connectorInfo.getIdentifier(), e);
        }
      }

      boolean usingWorkloadIdentity = Boolean.parseBoolean(System.getenv("USE_WORKLOAD_IDENTITY"));
      GoogleCredentials sourceCredentials;
      if (!usingWorkloadIdentity) {
        log.info("WI: In execute. using older way");
        sourceCredentials = getCredentials(GOOGLE_CREDENTIALS_PATH);
      } else {
        log.info("WI: In execute. using Google ADC");
        sourceCredentials = GoogleCredentials.getApplicationDefault();
      }

      // TODO: Enable it if we plan to support currency in SMP
      if (!isOnPremAndClickhouseEnabled()) {
        publishMessage(sourceCredentials, billingDataPipelineConfig.getGcpProjectId(),
            billingDataPipelineConfig.getGcpSyncPubSubTopic(), "", "", "", "", accountId, "", "", "True",
            usingWorkloadIdentity);
      }

      List<GcpBillingAccount> gcpBillingAccounts =
          cloudToHarnessMappingService.listGcpBillingAccountUpdatedInDuration(accountId);
      log.info("Processing batch size of {} in GCP Sync Job for CG Connectors", gcpBillingAccounts.size());
      for (GcpBillingAccount gcpBillingAccount : gcpBillingAccounts) {
        GcpServiceAccount gcpServiceAccount = cloudToHarnessMappingService.getGcpServiceAccount(accountId);
        log.info("ServiceAccountEmail: {}, DatasetId: {}, "
                + "ProjectId: {}, "
                + "ConnectorIdentifier: {}",
            gcpServiceAccount.getEmail(), gcpBillingAccount.getBqDatasetId(), gcpBillingAccount.getBqProjectId(),
            gcpBillingAccount.getUuid());
        try {
          processGCPConnector(billingDataPipelineConfig, gcpServiceAccount.getEmail(),
              gcpBillingAccount.getBqDatasetId(), gcpBillingAccount.getBqProjectId(), "", accountId,
              gcpBillingAccount.getUuid(), endTime, firstSync);
        } catch (Exception e) {
          log.error("Exception processing CG GCP Connector: {}", gcpBillingAccount.getUuid(), e);
        }
      }
    }
    return null;
  }

  private void createK8SJobWithParameters(Map<String, String> customArgsMap) throws IOException {
    ApiClient client = ClientBuilder.cluster().build();
    Configuration.setDefaultApiClient(client);

    ObjectMapper objectMapper = new ObjectMapper();
    String argsMap = objectMapper.writeValueAsString(customArgsMap);

    log.info("Passed arguments to gcp sync k8s job: {}", argsMap);

    List<V1EnvVar> envVariablesToPass = createEnvironmentVariables();

    V1Container container = createContainer(argsMap, envVariablesToPass);

    V1PodSpec podSpec = new V1PodSpec().restartPolicy(RESTART_POLICY_NEVER).addContainersItem(container);

    V1PodTemplateSpec template = new V1PodTemplateSpec().spec(podSpec);

    V1JobSpec jobSpec = new V1JobSpec().template(template).ttlSecondsAfterFinished(TTL_SECONDS_AFTER_FINISHED_7_DAYS);

    V1ObjectMeta v1ObjectMeta = new V1ObjectMeta().name(generateJobName()).namespace(NAMESPACE);
    v1ObjectMeta.setLabels(Map.of(JOB_TYPE, GCP_SYNC_K8S_JOB));

    V1Job job = new V1Job().apiVersion(K8S_API_VERSION).kind(K8S_JOB_KIND).metadata(v1ObjectMeta).spec(jobSpec);

    BatchV1Api api = new BatchV1Api(client);
    try {
      api.createNamespacedJob(NAMESPACE, job, null, null, null, null);
      log.info("K8SJob successfully created with name: {}, NameSpace: {}", job.getMetadata().getName(), NAMESPACE);
    } catch (ApiException e) {
      log.error("Exception occurred when calling BatchV1Api#createNamespacedJob for namespace: {}, exception: ",
          NAMESPACE, e);
    }
  }

  @NotNull
  private static String generateJobName() {
    return String.format(PYTHON_JOB_NAME_PREFIX, System.currentTimeMillis());
  }

  private V1Container createContainer(String argsMap, List<V1EnvVar> envVariablesToPass) {
    return new V1Container()
        .name(config.getGcpSyncSmpConfig().getK8sJobContainerName())
        .image(config.getGcpSyncSmpConfig().getK8sJobPythonImage())
        .args(Arrays.asList(argsMap))
        .env(envVariablesToPass);
  }

  @NotNull
  private List<V1EnvVar> createEnvironmentVariables() {
    V1EnvVar clickhouseUsername =
        new V1EnvVar().name(CLICKHOUSE_USERNAME).value(config.getClickHouseConfig().getUsername());
    V1EnvVar clickhouseQueryRetries =
        new V1EnvVar().name(CLICKHOUSE_QUERY_RETRIES).value(config.getClickHouseConfig().getQueryRetries());

    V1EnvVar clickhouseSendReceiveTimeout =
        new V1EnvVar()
            .name(CLICKHOUSE_SEND_RECEIVE_TIMEOUT)
            .value(String.valueOf(config.getClickHouseConfig().getSendReceiveTimeout()));

    GcpSyncSmpConfig gcpSyncSmpConfig = config.getGcpSyncSmpConfig();

    V1EnvVar clickhouseUrl = createEnvVarFromConfigMap(
        CLICKHOUSE_URL, gcpSyncSmpConfig.getBatchProcessingConfigMapName(), CLICKHOUSE_URL_PYTHON);

    V1EnvVar clickhouseEnabled = createEnvVarFromConfigMap(
        CLICKHOUSE_ENABLED, gcpSyncSmpConfig.getBatchProcessingConfigMapName(), CLICKHOUSE_ENABLED);

    V1EnvVar clickhousePort = createEnvVarFromConfigMap(
        CLICKHOUSE_PORT, gcpSyncSmpConfig.getBatchProcessingConfigMapName(), CLICKHOUSE_PORT_PYTHON);

    V1EnvVar clickhousePassword = createEnvVariableFromSecret(
        CLICKHOUSE_PASSWORD, gcpSyncSmpConfig.getClickHouseSecretName(), gcpSyncSmpConfig.getClickHousePasswordKey());

    V1EnvVar hmacAccessKey = createEnvVariableFromSecret(gcpSyncSmpConfig.getHmacAccessKey(),
        gcpSyncSmpConfig.getBatchProcessingSecretName(), gcpSyncSmpConfig.getHmacAccessKey());

    V1EnvVar hmacSecretKey = createEnvVariableFromSecret(gcpSyncSmpConfig.getHmacSecretKey(),
        gcpSyncSmpConfig.getBatchProcessingSecretName(), gcpSyncSmpConfig.getHmacSecretKey());

    V1EnvVar serviceAccountCredential = createEnvVariableFromSecret(SERVICE_ACCOUNT_CREDENTIALS,
        gcpSyncSmpConfig.getBatchProcessingMountSecretName(), gcpSyncSmpConfig.getServiceAccountCredentialKey());

    return Arrays.asList(clickhouseEnabled, clickhouseUrl, clickhouseUsername, clickhousePassword,
        clickhouseQueryRetries, clickhouseSendReceiveTimeout, clickhousePort, hmacAccessKey, hmacSecretKey,
        serviceAccountCredential);
  }

  private V1EnvVar createEnvVariableFromSecret(String envVarName, String secretName, String secretKey) {
    return new V1EnvVar()
        .name(envVarName)
        .valueFrom(new V1EnvVarSource().secretKeyRef(
            new V1SecretKeySelector().name(secretName).key(secretKey).optional(false)));
  }

  private static V1EnvVar createEnvVarFromConfigMap(String name, String configMapName, String key) {
    return new V1EnvVar().name(name).valueFrom(
        new V1EnvVarSource().configMapKeyRef(new V1ConfigMapKeySelector().name(configMapName).key(key)));
  }

  private void processGCPConnector(BillingDataPipelineConfig billingDataPipelineConfig, String serviceAccountEmail,
      String datasetId, String projectId, String tableName, String accountId, String connectorId, Long endTime,
      boolean firstSync) throws IOException {
    boolean usingWorkloadIdentity = Boolean.parseBoolean(System.getenv("USE_WORKLOAD_IDENTITY"));
    GoogleCredentials sourceCredentials;
    if (!usingWorkloadIdentity) {
      log.info("WI: processGCPConnector older way");
      sourceCredentials = getCredentials(GOOGLE_CREDENTIALS_PATH);
    } else {
      log.info("WI: processGCPConnector using Google ADC");
      sourceCredentials = GoogleCredentials.getApplicationDefault();
    }
    Credentials credentials = getImpersonatedCredentials(sourceCredentials, serviceAccountEmail);
    BigQuery bigQuery = BigQueryOptions.newBuilder()
                            .setCredentials(credentials)
                            .setHeaderProvider(getHeaderProvider())
                            .setProjectId(projectId)
                            .build()
                            .getService();
    DatasetId datasetIdFullyQualified = DatasetId.of(projectId, datasetId);
    Dataset dataset = bigQuery.getDataset(datasetIdFullyQualified);
    log.info("dataset.getLocation(): {}", dataset.getLocation());
    if (isEmpty(tableName)) {
      // Older way to get the tableName
      Page<Table> tableList = dataset.list(BigQuery.TableListOption.pageSize(1000));
      tableList.getValues().forEach(table -> {
        if (table.getTableId().getTable().contains(GCP_BILLING_EXPORT_V_1)) {
          TableId tableId1 = TableId.of(projectId, datasetId, table.getTableId().getTable());
          Table tableGranularData = bigQuery.getTable(tableId1);
          Long lastModifiedTime = tableGranularData.getLastModifiedTime();
          lastModifiedTime = lastModifiedTime != null ? lastModifiedTime : table.getCreationTime();
          log.info("Sync condition {} {}", lastModifiedTime, endTime);
          if (lastModifiedTime > endTime || firstSync) {
            CacheKey cacheKey = new CacheKey(accountId, projectId, datasetId, table.getTableId().getTable());
            if (gcpSyncInfo.getIfPresent(cacheKey) == null) {
              boolean isExecuted;
              if (isOnPremAndClickhouseEnabled()) {
                isExecuted = createK8sJob(
                    dataset.getLocation(), datasetId, projectId, accountId, connectorId, table.getTableId().getTable());
              } else {
                isExecuted = publishMessage(sourceCredentials, billingDataPipelineConfig.getGcpProjectId(),
                    billingDataPipelineConfig.getGcpSyncPubSubTopic(), dataset.getLocation(), serviceAccountEmail,
                    datasetId, projectId, accountId, connectorId, table.getTableId().getTable(), "False",
                    usingWorkloadIdentity);
              }
              gcpSyncInfo.put(cacheKey, isExecuted);
            }
          }
        }
      });
    } else {
      log.info("tableName exists in config");
      TableId tableId = TableId.of(projectId, datasetId, tableName);
      Table tableGranularData = bigQuery.getTable(tableId);
      Long lastModifiedTime = tableGranularData.getLastModifiedTime();
      lastModifiedTime = lastModifiedTime != null ? lastModifiedTime : tableGranularData.getCreationTime();
      log.info("Sync condition {} {}", lastModifiedTime, endTime);
      if (lastModifiedTime > endTime || firstSync) {
        CacheKey cacheKey = new CacheKey(accountId, projectId, datasetId, tableName);
        if (gcpSyncInfo.getIfPresent(cacheKey) == null) {
          boolean isExecuted;
          if (isOnPremAndClickhouseEnabled()) {
            isExecuted = createK8sJob(dataset.getLocation(), datasetId, projectId, accountId, connectorId,
                tableGranularData.getTableId().getTable());
          } else {
            isExecuted = publishMessage(sourceCredentials, billingDataPipelineConfig.getGcpProjectId(),
                billingDataPipelineConfig.getGcpSyncPubSubTopic(), dataset.getLocation(), serviceAccountEmail,
                datasetId, projectId, accountId, connectorId, tableGranularData.getTableId().getTable(), "False",
                usingWorkloadIdentity);
          }
          gcpSyncInfo.put(cacheKey, isExecuted);
        }
      }
    }
  }

  private boolean createK8sJob(
      String location, String datasetId, String projectId, String accountId, String connectorId, String tableName) {
    Map<String, String> customAttributes = ImmutableMap.<String, String>builder()
                                               .put(SOURCE_DATA_SET_REGION, location)
                                               .put(SOURCE_DATA_SET_ID, datasetId)
                                               .put(SOURCE_GCP_PROJECT_ID, projectId)
                                               .put(ACCOUNT_ID, accountId)
                                               .put(CONNECTOR_ID, connectorId)
                                               .put(SOURCE_GCP_TABLE_NAME, tableName)
                                               .build();
    log.info("Creating a new K8S Job with custom attributes: {}", customAttributes);
    try {
      createK8SJobWithParameters(customAttributes);
    } catch (IOException e) {
      log.error("IOException occurred: {}", e.getMessage());
    } catch (Exception e) {
      log.error("Exception occurred: {}", e.getMessage());
    }
    return true;
  }

  private boolean isOnPremAndClickhouseEnabled() {
    return DeployMode.isOnPrem(config.getDeployMode().name()) && config.isClickHouseEnabled();
  }

  private HeaderProvider getHeaderProvider() {
    String userAgent = System.getenv(USER_AGENT_HEADER_ENVIRONMENT_VARIABLE);
    return FixedHeaderProvider.create(
        ImmutableMap.of(USER_AGENT_HEADER, Objects.nonNull(userAgent) ? userAgent : DEFAULT_USER_AGENT));
  }

  public List<ConnectorResponseDTO> getNextGenGCPConnectorResponses(String accountId) {
    List<ConnectorResponseDTO> nextGenConnectorResponses = ngConnectorHelper.getNextGenConnectors(accountId,
        Arrays.asList(ConnectorType.GCP_CLOUD_COST), Arrays.asList(CEFeatures.BILLING), Collections.emptyList());
    log.info("Processing batch size of {} in GCP Sync Job", nextGenConnectorResponses.size());
    return nextGenConnectorResponses;
  }

  // read the credential path from env variables
  public static ServiceAccountCredentials getCredentials(String googleCredentialPathSystemEnv) {
    String googleCredentialsPath = System.getenv(googleCredentialPathSystemEnv);
    Preconditions.checkArgument(!isEmpty(googleCredentialsPath), "Missing environment variable for GCP credentials.");
    File credentialsFile = new File(googleCredentialsPath);
    ServiceAccountCredentials credentials = null;
    try (FileInputStream serviceAccountStream = new FileInputStream(credentialsFile)) {
      credentials = ServiceAccountCredentials.fromStream(serviceAccountStream);
    } catch (FileNotFoundException e) {
      log.error("Failed to find Google credential file for the CE service account in the specified path.", e);
    } catch (IOException e) {
      log.error("Failed to get Google credential file for the CE service account.", e);
    }
    return credentials;
  }

  public static Credentials getImpersonatedCredentials(
      GoogleCredentials sourceCredentials, String impersonatedServiceAccount) {
    if (impersonatedServiceAccount == null) {
      return sourceCredentials;
    } else {
      return ImpersonatedCredentials.create(sourceCredentials, impersonatedServiceAccount, null,
          Arrays.asList("https://www.googleapis.com/auth/cloud-platform"), 300);
    }
  }

  public boolean publishMessage(GoogleCredentials sourceCredentials, String harnessProjectId, String topicId,
      String location, String serviceAccountEmail, String datasetId, String projectId, String accountId,
      String connectorId, String tableName, String isHistoricalCostUpdateTriggerRequired,
      boolean usingWorkloadIdentity) {
    TopicName topicName = TopicName.of(harnessProjectId, topicId);
    Publisher publisher = null;

    log.info("isDeploymentOnPrem: " + config.getDeployMode());
    try {
      // Create a publisher instance with default settings bound to the topic
      publisher = Publisher.newBuilder(topicName)
                      .setCredentialsProvider(FixedCredentialsProvider.create(sourceCredentials))
                      .build();
      ImmutableMap<String, String> customAttributes =
          ImmutableMap.<String, String>builder()
              .put(SERVICE_ACCOUNT, serviceAccountEmail)
              .put(SOURCE_DATA_SET_ID, datasetId)
              .put(SOURCE_GCP_PROJECT_ID, projectId)
              .put(SOURCE_DATA_SET_REGION, location)
              .put(ACCOUNT_ID, accountId)
              .put(CONNECTOR_ID, connectorId)
              .put(SOURCE_GCP_TABLE_NAME, tableName)
              .put(TRIGGER_HISTORICAL_COST_UPDATE_IN_PREFERRED_CURRENCY, isHistoricalCostUpdateTriggerRequired)
              .put(DEPLOY_MODE, config.getDeployMode().name())
              .put(USE_WORKLOAD_IDENTITY, usingWorkloadIdentity ? "True" : "False")
              .build();
      ObjectMapper objectMapper = new ObjectMapper();
      String message = objectMapper.writeValueAsString(customAttributes);
      log.info("Sending GCP Sync Pub Sub Event with data: {}", message);
      ByteString data = ByteString.copyFromUtf8(message);
      PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();
      // Once published, returns a server-assigned message id (unique within the topic)
      ApiFuture<String> messageIdFuture = publisher.publish(pubsubMessage);
      String messageId = messageIdFuture.get();
      log.info("Published a message with custom attributes: " + messageId);
    } catch (Exception ex) {
      log.error("Exception while publishing", ex);
    } finally {
      if (publisher != null) {
        // When finished with the publisher, shutdown to free up resources.
        publisher.shutdown();
        try {
          publisher.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
          log.error("InterruptedException ", e);
        }
      }
    }
    return true;
  }
}
