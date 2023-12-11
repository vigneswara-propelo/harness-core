/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service;

import static io.harness.k8s.K8sConstants.HARNESS_KUBERNETES_REVISION_LABEL_KEY;
import static io.harness.rule.OwnerRule.BRETT;
import static io.harness.rule.OwnerRule.PRATYUSH;

import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.CLUSTER_NAME;
import static software.wings.utils.WingsTestConstants.ECS_SERVICE_NAME;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.k8s.K8sServiceMetadata;
import io.harness.k8s.KubernetesContainerService;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.instance.info.ContainerInfo;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.ContainerServiceImpl;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.intfc.ContainerService;
import software.wings.service.intfc.security.EncryptionService;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ecs.model.ListTasksResult;
import com.amazonaws.services.ecs.model.Service;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodBuilder;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class ContainerServiceTest extends WingsBaseTest {
  private static final String KUBERNETES_REPLICATION_CONTROLLER_NAME = "kubernetes-rc-name.0";
  private static final String KUBERNETES_SERVICE_NAME = "kubernetes-service-name";

  @Mock private GkeClusterService gkeClusterService;
  @Mock private KubernetesContainerService kubernetesContainerService;
  @Mock private AwsClusterService awsClusterService;
  @Mock private AwsHelperService awsHelperService;
  @Mock private EncryptionService encryptionService;

  @InjectMocks private ContainerService containerService = new ContainerServiceImpl();

  @Mock private ListTasksResult listTasksResult;

  private KubernetesConfig kubernetesConfig = KubernetesConfig.builder()
                                                  .masterUrl("masterUrl")
                                                  .namespace("default")
                                                  .username("user".toCharArray())
                                                  .password("pass".toCharArray())
                                                  .accountId(ACCOUNT_ID)
                                                  .build();

  private KubernetesClusterConfig kubernetesClusterConfig = KubernetesClusterConfig.builder()
                                                                .masterUrl("masterUrl")
                                                                .username("user".toCharArray())
                                                                .password("pass".toCharArray())
                                                                .accountId(ACCOUNT_ID)
                                                                .build();

  private ContainerServiceParams gcpParams =
      ContainerServiceParams.builder()
          .settingAttribute(SettingAttribute.Builder.aSettingAttribute()
                                .withValue(GcpConfig.builder()
                                               .serviceAccountKeyFileContent("keyFileContent".toCharArray())
                                               .accountId(ACCOUNT_ID)
                                               .build())
                                .withAccountId(ACCOUNT_ID)
                                .build()
                                .toDTO())
          .encryptionDetails(emptyList())
          .clusterName(CLUSTER_NAME)
          .namespace("default")
          .containerServiceName(KUBERNETES_REPLICATION_CONTROLLER_NAME)
          .build();

  private ContainerServiceParams awsParams =
      ContainerServiceParams.builder()
          .settingAttribute(SettingAttribute.Builder.aSettingAttribute()
                                .withValue(AwsConfig.builder()
                                               .accessKey("accessKey".toCharArray())
                                               .secretKey("secretKey".toCharArray())
                                               .accountId(ACCOUNT_ID)
                                               .build())
                                .build()
                                .toDTO())
          .encryptionDetails(emptyList())
          .clusterName(CLUSTER_NAME)
          .containerServiceName(ECS_SERVICE_NAME)
          .region("us-east-1")
          .build();

  private ContainerServiceParams kubernetesConfigParams =
      ContainerServiceParams.builder()
          .settingAttribute(SettingAttribute.Builder.aSettingAttribute()
                                .aSettingAttribute()
                                .withValue(kubernetesClusterConfig)
                                .withAccountId(ACCOUNT_ID)
                                .build()
                                .toDTO())
          .encryptionDetails(emptyList())
          .clusterName(CLUSTER_NAME)
          .namespace("default")
          .containerServiceName(KUBERNETES_REPLICATION_CONTROLLER_NAME)
          .build();

  @Before
  public void setup() {
    V1Pod pod = new V1PodBuilder()
                    .withApiVersion("v1")
                    .withNewStatus()
                    .withPhase("Running")
                    .endStatus()
                    .withNewMetadata()
                    .addToLabels("app", "MyApp")
                    .endMetadata()
                    .build();
    when(gkeClusterService.getCluster(gcpParams.getSettingAttribute(), emptyList(), CLUSTER_NAME, "default", false))
        .thenReturn(kubernetesConfig);
    when(kubernetesContainerService.getRunningPodsWithLabels(eq(kubernetesConfig), anyString(), anyMap()))
        .thenReturn(singletonList(pod));
    when(kubernetesContainerService.getControllerPodCount(any(ReplicationController.class))).thenReturn(2);
    doReturn(null).when(encryptionService).decrypt(eq(kubernetesClusterConfig), any(), eq(false));

    Service ecsService = new Service();
    ecsService.setServiceName(ECS_SERVICE_NAME);
    ecsService.setCreatedAt(new Date());
    ecsService.setDesiredCount(2);
    AwsConfig awsConfig = AwsConfig.builder()
                              .accessKey("accessKey".toCharArray())
                              .secretKey("secretKey".toCharArray())
                              .accountId(ACCOUNT_ID)
                              .build();
    SettingAttribute awsSettingAttribute = aSettingAttribute().withValue(awsConfig).build();
    when(awsClusterService.getServices(
             Regions.US_EAST_1.getName(), awsSettingAttribute.toDTO(), Collections.emptyList(), CLUSTER_NAME))
        .thenReturn(singletonList(ecsService));
    when(awsHelperService.validateAndGetAwsConfig(eq(awsSettingAttribute.toDTO()), any(), eq(false)))
        .thenReturn(awsConfig);
    when(awsHelperService.listTasks(eq("us-east-1"), eq(awsConfig), any(), any(), eq(false)))
        .thenReturn(listTasksResult);
    when(listTasksResult.getTaskArns()).thenReturn(emptyList());
  }

  @Test
  @Owner(developers = {BRETT, PRATYUSH})
  @Category(UnitTests.class)
  public void shouldGetContainerInfosFabric8_Gcp() {
    setupMockForFabric8();
    List<ContainerInfo> result = containerService.getContainerInfos(gcpParams, false);

    assertThat(result.size()).isEqualTo(1);
  }
  @Test
  @Owner(developers = {BRETT, PRATYUSH})
  @Category(UnitTests.class)
  public void shouldGetContainerInfosFabric8_Aws() {
    setupMockForFabric8();
    List<ContainerInfo> result = containerService.getContainerInfos(awsParams, false);

    assertThat(result.size()).isEqualTo(0);
  }
  @Test
  @Owner(developers = {BRETT, PRATYUSH})
  @Category(UnitTests.class)
  public void shouldGetContainerInfosFabric8_DirectKube() {
    setupMockForFabric8();
    List<ContainerInfo> result = containerService.getContainerInfos(kubernetesConfigParams, false);

    assertThat(result.size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = PRATYUSH)
  @Category(UnitTests.class)
  public void shouldGetContainerInfos_Gcp() {
    setupMockForJavaClient();
    List<ContainerInfo> result = containerService.getContainerInfos(gcpParams, false);

    assertThat(result.size()).isEqualTo(1);
  }
  @Test
  @Owner(developers = PRATYUSH)
  @Category(UnitTests.class)
  public void shouldGetContainerInfos_Aws() {
    setupMockForJavaClient();
    List<ContainerInfo> result = containerService.getContainerInfos(awsParams, false);

    assertThat(result.size()).isEqualTo(0);
  }
  @Test
  @Owner(developers = PRATYUSH)
  @Category(UnitTests.class)
  public void shouldGetContainerInfos_DirectKube() {
    setupMockForJavaClient();
    List<ContainerInfo> result = containerService.getContainerInfos(kubernetesConfigParams, false);

    assertThat(result.size()).isEqualTo(1);
  }

  private void setupMockForFabric8() {
    Map<String, String> map = ImmutableMap.of("app", "MyApp");
    K8sServiceMetadata k8sServiceMetadata =
        K8sServiceMetadata.builder().name(KUBERNETES_SERVICE_NAME).labels(map).build();
    when(kubernetesContainerService.getK8sServiceMetadataUsingK8sClient(
             eq(kubernetesConfig), eq(KUBERNETES_REPLICATION_CONTROLLER_NAME), eq(ACCOUNT_ID)))
        .thenReturn(K8sServiceMetadata.builder().build());
    when(kubernetesContainerService.getK8sServiceMetadataUsingFabric8(
             eq(kubernetesConfig), eq(KUBERNETES_REPLICATION_CONTROLLER_NAME), eq(ACCOUNT_ID)))
        .thenReturn(k8sServiceMetadata);
  }

  private void setupMockForJavaClient() {
    Map<String, String> map = new HashMap<>();
    map.put("key", "value");
    map.put(HARNESS_KUBERNETES_REVISION_LABEL_KEY, "value2");
    K8sServiceMetadata k8sServiceMetadata = K8sServiceMetadata.builder().name("serviceName").labels(map).build();

    when(kubernetesContainerService.getK8sServiceMetadataUsingK8sClient(
             eq(kubernetesConfig), anyString(), eq(ACCOUNT_ID)))
        .thenReturn(k8sServiceMetadata);
  }
}
