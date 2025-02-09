/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.encoding.EncodingUtils.compressString;
import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static io.harness.data.encoding.EncodingUtils.encodeBase64ToByteArray;
import static io.harness.k8s.K8sConstants.HARNESS_KUBERNETES_REVISION_LABEL_KEY;
import static io.harness.k8s.K8sConstants.KUBECONFIG_FILENAME;
import static io.harness.k8s.KubernetesConvention.CompressedReleaseHistoryFlag;
import static io.harness.k8s.KubernetesConvention.ReleaseHistoryKeyName;
import static io.harness.k8s.model.KubernetesClusterAuthType.EXEC_OAUTH;
import static io.harness.k8s.model.KubernetesClusterAuthType.GCP_OAUTH;
import static io.harness.k8s.model.KubernetesClusterAuthType.OIDC;
import static io.harness.k8s.model.KubernetesClusterAuthType.USER_PASSWORD;
import static io.harness.rule.OwnerRule.ABHINAV2;
import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.ACASIAN;
import static io.harness.rule.OwnerRule.ACHYUTH;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.BOGDAN;
import static io.harness.rule.OwnerRule.BRETT;
import static io.harness.rule.OwnerRule.BUHA;
import static io.harness.rule.OwnerRule.PRATYUSH;
import static io.harness.rule.OwnerRule.TARUN_UBA;
import static io.harness.rule.OwnerRule.YOGESH;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Fail.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.concurent.HTimeLimiterMocker;
import io.harness.exception.InvalidRequestException;
import io.harness.filesystem.FileIo;
import io.harness.k8s.config.K8sGlobalConfigService;
import io.harness.k8s.model.GcpAccessTokenSupplier;
import io.harness.k8s.model.KubernetesAzureConfig;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.OidcGrantType;
import io.harness.k8s.model.kubeconfig.EnvVariable;
import io.harness.k8s.model.kubeconfig.Exec;
import io.harness.k8s.model.kubeconfig.InteractiveMode;
import io.harness.k8s.oidc.OidcTokenRetriever;
import io.harness.rule.Owner;

import com.github.scribejava.apis.openid.OpenIdOAuth2AccessToken;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
import io.fabric8.kubernetes.api.model.ReplicationControllerSpec;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetList;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.fabric8.kubernetes.api.model.autoscaling.v1.HorizontalPodAutoscaler;
import io.fabric8.kubernetes.api.model.autoscaling.v1.HorizontalPodAutoscalerList;
import io.fabric8.kubernetes.api.model.extensions.DaemonSet;
import io.fabric8.kubernetes.api.model.extensions.DaemonSetList;
import io.fabric8.kubernetes.api.model.extensions.DaemonSetSpec;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DeploymentList;
import io.fabric8.kubernetes.api.model.extensions.DeploymentSpec;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import io.fabric8.kubernetes.client.impl.AppsAPIGroupClient;
import io.fabric8.kubernetes.client.impl.ExtensionsAPIGroupClient;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigList;
import io.fabric8.openshift.api.model.DeploymentConfigSpec;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.dsl.DeployableScalableResource;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.ApiResponse;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.Pair;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ConfigMapBuilder;
import io.kubernetes.client.openapi.models.V1ConfigMapList;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.openapi.models.V1DaemonSetList;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.openapi.models.V1ListMetaBuilder;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1ObjectMetaBuilder;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1PodListBuilder;
import io.kubernetes.client.openapi.models.V1PodStatus;
import io.kubernetes.client.openapi.models.V1PodStatusBuilder;
import io.kubernetes.client.openapi.models.V1ReplicaSet;
import io.kubernetes.client.openapi.models.V1ReplicaSetList;
import io.kubernetes.client.openapi.models.V1ReplicationController;
import io.kubernetes.client.openapi.models.V1ReplicationControllerList;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1SecretBuilder;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceBuilder;
import io.kubernetes.client.openapi.models.V1ServiceList;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.openapi.models.V1StatefulSetList;
import io.kubernetes.client.openapi.models.V1Status;
import io.kubernetes.client.openapi.models.V1StatusBuilder;
import io.kubernetes.client.openapi.models.VersionInfo;
import io.kubernetes.client.openapi.models.VersionInfoBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import okhttp3.Call;
import okhttp3.internal.http2.ConnectionShutdownException;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * Created by brett on 2/10/17.
 */
@OwnedBy(CDP)
public class KubernetesContainerServiceImplTest extends CategoryTest {
  public static final String MASTER_URL = "masterUrl";
  public static final String DUMMY_RELEASE_HISTORY = "dummyReleaseHistory";
  public static final char[] USERNAME = "username".toCharArray();
  public static final char[] PASSWORD = "PASSWORD".toCharArray();
  final Path workingDir = Path.of("dir");
  final Path configFilePath = Path.of(workingDir.toString(), KUBECONFIG_FILENAME);
  private static final KubernetesConfig KUBERNETES_CONFIG = KubernetesConfig.builder()
                                                                .masterUrl(MASTER_URL)
                                                                .username(USERNAME)
                                                                .password(PASSWORD)
                                                                .namespace("default")
                                                                .build();

  @Mock private KubernetesClient kubernetesClient;
  @Mock private OpenShiftClient openShiftClient;
  @Mock
  private MixedOperation<ReplicationController, ReplicationControllerList,
      RollableScalableResource<ReplicationController>> replicationControllers;
  @Mock
  private NonNamespaceOperation<ReplicationController, ReplicationControllerList,
      RollableScalableResource<ReplicationController>> namespacedReplicationControllers;

  @Mock private MixedOperation<Service, ServiceList, ServiceResource<Service>> services;
  @Mock private MixedOperation<Secret, SecretList, Resource<Secret>> secrets;
  @Mock private MixedOperation<ConfigMap, ConfigMapList, Resource<ConfigMap>> configMaps;
  @Mock private NonNamespaceOperation<Service, ServiceList, ServiceResource<Service>> namespacedServices;

  @Mock private NonNamespaceOperation<Secret, SecretList, Resource<Secret>> namespacedSecrets;
  @Mock private NonNamespaceOperation<ConfigMap, ConfigMapList, Resource<ConfigMap>> namespacedConfigMaps;

  @Mock private RollableScalableResource<ReplicationController> scalableReplicationController;
  @Mock private ServiceResource<Service> serviceResource;
  @Mock private FilterWatchListDeletable<Service, ServiceList, ServiceResource<Service>> serviceFilteredList;
  @Mock private ServiceList serviceList;
  @Mock private Resource<Secret> secretResource;
  @Mock private Resource<ConfigMap> configMapResource;
  @Mock
  private MixedOperation<DeploymentConfig, DeploymentConfigList, DeployableScalableResource<DeploymentConfig>>
      deploymentConfigsOperation;
  @Mock
  private NonNamespaceOperation<DeploymentConfig, DeploymentConfigList, DeployableScalableResource<DeploymentConfig>>
      deploymentConfigs;
  @Mock

  // Deployments
  private MixedOperation<Deployment, DeploymentList, RollableScalableResource<Deployment>> deploymentOperations;
  @Mock
  private NonNamespaceOperation<Deployment, DeploymentList, RollableScalableResource<Deployment>> namespacedDeployments;
  @Mock private RollableScalableResource<Deployment> scalableDeployment;
  @Mock private FilterWatchListDeletable<Deployment, DeploymentList, Boolean> deploymentFilteredList;

  // Statefulsets
  @Mock
  private MixedOperation<StatefulSet, StatefulSetList, RollableScalableResource<StatefulSet>> statefulSetOperations;
  @Mock
  private NonNamespaceOperation<StatefulSet, StatefulSetList, RollableScalableResource<StatefulSet>>
      namespacedStatefulsets;
  @Mock private RollableScalableResource<StatefulSet> statefulSetResource;

  // DaemonSet
  @Mock private MixedOperation<DaemonSet, DaemonSetList, Resource<DaemonSet>> daemonSetOperations;
  @Mock private NonNamespaceOperation<DaemonSet, DaemonSetList, Resource<DaemonSet>> namespacedDaemonSet;
  @Mock private Resource<DaemonSet> daemonSetResource;

  // Namespaces
  @Mock private NonNamespaceOperation<Namespace, NamespaceList, Resource<Namespace>> namespacedNamespaces;
  @Mock private Resource<Namespace> namespaceResource;

  // Secrets
  @Mock private MixedOperation<Secret, SecretList, Resource<Secret>> secretOperations;

  // ConfigMaps
  @Mock private MixedOperation<ConfigMap, ConfigMapList, Resource<ConfigMap>> configMapOperations;

  // HPA
  @Mock
  private NonNamespaceOperation<HorizontalPodAutoscaler, HorizontalPodAutoscalerList, Resource<HorizontalPodAutoscaler>>
      namespacedHpa;

  @Mock
  private NonNamespaceOperation<io.fabric8.kubernetes.api.model.autoscaling.v2beta1.HorizontalPodAutoscaler,
      io.fabric8.kubernetes.api.model.autoscaling.v2beta1.HorizontalPodAutoscalerList,
      Resource<io.fabric8.kubernetes.api.model.autoscaling.v2beta1.HorizontalPodAutoscaler>> v2Beta1NamespacedHpa;
  @Mock private Resource<HorizontalPodAutoscaler> horizontalPodAutoscalerResource;
  @Mock
  private Resource<io.fabric8.kubernetes.api.model.autoscaling.v2beta1.HorizontalPodAutoscaler>
      v2Beta1HorizontalPodAutoscalerResource;

  @Mock private ExtensionsAPIGroupClient extensionsAPIGroupClient;
  @Mock private AppsAPIGroupClient appsAPIGroupClient;

  @Mock private ApiClient k8sApiClient;
  @Mock private Call k8sApiCall;

  @Mock private KubernetesHelperService kubernetesHelperService;
  @Mock private TimeLimiter timeLimiter;
  @Mock private Clock clock;
  @Mock private K8sGlobalConfigService k8sGlobalConfigService;
  @Spy @InjectMocks private OidcTokenRetriever oidcTokenRetriever;

  @InjectMocks private KubernetesContainerServiceImpl kubernetesContainerService;
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  ReplicationController replicationController;
  ReplicationControllerSpec spec;
  Secret releaseHistory = new SecretBuilder()
                              .withNewMetadata()
                              .withName("test-rn")
                              .withNamespace("default")
                              .endMetadata()
                              .withData(ImmutableMap.of(ReleaseHistoryKeyName, encodeBase64("test")))
                              .build();

  ConfigMap releaseHistoryConfigMap = new ConfigMapBuilder()
                                          .withNewMetadata()
                                          .withName("test-rn")
                                          .withNamespace("default")
                                          .endMetadata()
                                          .withData(ImmutableMap.of(ReleaseHistoryKeyName, "test"))
                                          .build();

  Deployment deployment;
  DeploymentSpec deploymentSpec;

  StatefulSet statefulSet;
  StatefulSetSpec statefulSetSpec;

  DaemonSet daemonSet;
  DaemonSetSpec daemonSetSpec;

  Namespace namespace;

  Secret secret;
  ConfigMap configMap;
  Service service;
  HorizontalPodAutoscaler horizontalPodAutoscaler;
  io.fabric8.kubernetes.api.model.autoscaling.v2beta1.HorizontalPodAutoscaler v2Beta1HorizontalPodAutoscaler;

  @Before
  public void setUp() throws Exception {
    when(kubernetesHelperService.getKubernetesClient(KUBERNETES_CONFIG)).thenReturn(kubernetesClient);
    when(kubernetesHelperService.getOpenShiftClient(KUBERNETES_CONFIG)).thenReturn(openShiftClient);

    when(kubernetesClient.services()).thenReturn(services);
    when(kubernetesClient.extensions()).thenReturn(extensionsAPIGroupClient);
    when(kubernetesClient.apps()).thenReturn(appsAPIGroupClient);
    when(kubernetesClient.namespaces()).thenReturn(namespacedNamespaces);

    when(kubernetesClient.replicationControllers()).thenReturn(replicationControllers);
    when(kubernetesClient.secrets()).thenReturn(secrets);
    when(kubernetesClient.configMaps()).thenReturn(configMaps);
    when(services.inNamespace("default")).thenReturn(namespacedServices);
    when(secrets.inNamespace("default")).thenReturn(namespacedSecrets);
    when(configMaps.inNamespace("default")).thenReturn(namespacedConfigMaps);
    when(replicationControllers.inNamespace("default")).thenReturn(namespacedReplicationControllers);
    when(services.inNamespace(anyString())).thenReturn(namespacedServices);
    when(namespacedReplicationControllers.withName(anyString())).thenReturn(scalableReplicationController);
    when(namespacedServices.withName(anyString())).thenReturn(serviceResource);
    when(namespacedServices.withLabels(anyMap())).thenReturn(serviceFilteredList);
    when(namespacedSecrets.withName(anyString())).thenReturn(secretResource);
    when(namespacedSecrets.createOrReplace(any(Secret.class))).thenReturn(releaseHistory);
    when(namespacedConfigMaps.withName(anyString())).thenReturn(configMapResource);
    when(namespacedConfigMaps.createOrReplace(any(ConfigMap.class))).thenReturn(releaseHistoryConfigMap);
    doReturn(releaseHistory).when(secretResource).get();
    doReturn(releaseHistoryConfigMap).when(configMapResource).get();

    when(extensionsAPIGroupClient.deployments()).thenReturn(deploymentOperations);
    when(deploymentOperations.inNamespace("default")).thenReturn(namespacedDeployments);
    when(namespacedDeployments.withName(anyString())).thenReturn(scalableDeployment);
    when(namespacedDeployments.withLabels(any(Map.class))).thenReturn(deploymentFilteredList);
    when(deploymentFilteredList.list()).thenReturn(new DeploymentList());
    when(serviceFilteredList.list()).thenReturn(serviceList);

    when(appsAPIGroupClient.statefulSets()).thenReturn(statefulSetOperations);
    when(statefulSetOperations.inNamespace("default")).thenReturn(namespacedStatefulsets);
    when(namespacedStatefulsets.withName(anyString())).thenReturn(statefulSetResource);

    when(extensionsAPIGroupClient.daemonSets()).thenReturn(daemonSetOperations);
    when(daemonSetOperations.inNamespace("default")).thenReturn(namespacedDaemonSet);
    when(namespacedDaemonSet.withName(anyString())).thenReturn(daemonSetResource);

    when(kubernetesClient.namespaces()).thenReturn(namespacedNamespaces);
    when(namespacedNamespaces.withName(anyString())).thenReturn(namespaceResource);

    when(kubernetesClient.secrets()).thenReturn(secretOperations);
    when(secretOperations.inNamespace(anyString())).thenReturn(namespacedSecrets);
    when(namespacedSecrets.withName(anyString())).thenReturn(secretResource);

    when(kubernetesClient.configMaps()).thenReturn(configMapOperations);
    when(configMapOperations.inNamespace(anyString())).thenReturn(namespacedConfigMaps);
    when(namespacedConfigMaps.withName(anyString())).thenReturn(configMapResource);
    when(namespacedHpa.withName(anyString())).thenReturn(horizontalPodAutoscalerResource);
    when(v2Beta1NamespacedHpa.withName(anyString())).thenReturn(v2Beta1HorizontalPodAutoscalerResource);

    when(kubernetesHelperService.getApiClient(KUBERNETES_CONFIG)).thenReturn(k8sApiClient);
    when(kubernetesHelperService.getApiClientWithReadTimeout(KUBERNETES_CONFIG)).thenReturn(k8sApiClient);
    when(k8sApiClient.buildCall(anyString(), anyString(), anyList(), anyList(), any(), anyMap(), anyMap(), anyMap(),
             any(String[].class), any()))
        .thenReturn(k8sApiCall);
    when(k8sApiClient.escapeString(anyString())).thenAnswer(invocation -> invocation.getArgument(0, String.class));
    when(k8sApiClient.parameterToPair(anyString(), any())).thenCallRealMethod();
    when(k8sApiClient.parameterToString(any())).thenCallRealMethod();

    replicationController = new ReplicationController();
    spec = new ReplicationControllerSpec();
    spec.setReplicas(8);
    replicationController.setSpec(spec);
    HTimeLimiterMocker.mockCallInterruptible(timeLimiter).thenReturn(replicationController);

    deployment = new Deployment();
    deploymentSpec = new DeploymentSpec();
    deploymentSpec.setReplicas(2);
    deployment.setSpec(deploymentSpec);

    statefulSet = new StatefulSet();
    statefulSetSpec = new StatefulSetSpec();
    statefulSet.setSpec(statefulSetSpec);

    daemonSet = new DaemonSet();
    daemonSetSpec = new DaemonSetSpec();
    daemonSet.setSpec(daemonSetSpec);

    namespace = new Namespace();
    service = new Service();
    configMap = new ConfigMap();
    horizontalPodAutoscaler = new HorizontalPodAutoscaler();
  }

  @After
  public void tearDown() throws Exception {
    FileIo.deleteDirectoryAndItsContentIfExists(workingDir.toString());
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetControllerPodCountUnhandledResource() {
    Service service = new Service();
    try {
      kubernetesContainerService.getControllerPodCount(service);
      fail("Should not reach here.");
    } catch (Exception ex) {
      assertThat(ex.getMessage()).isEqualTo("Unhandled kubernetes resource type [Service] for getting the pod count");
    }
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testNPEInGetContainerInfosWhenReady() throws Exception {
    HTimeLimiterMocker.mockCallInterruptible(timeLimiter).thenReturn(null);

    try {
      kubernetesContainerService.getContainerInfosWhenReady(
          KUBERNETES_CONFIG, "controllerName", 0, 0, 0, asList(), false, null, false, 0L, "default");
      fail("Should not reach here.");
    } catch (InvalidRequestException ex) {
      assertThat(ex.getMessage()).isEqualTo("Could not find a controller named controllerName");
    } catch (Exception ex) {
      fail("Should not reach here.");
    }

    HTimeLimiterMocker.mockCallInterruptible(timeLimiter).thenReturn(replicationController);
    kubernetesContainerService.getContainerInfosWhenReady(
        KUBERNETES_CONFIG, "controllerName", 0, 0, 0, asList(), false, null, false, 0L, "default");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetPodTemplateSpec() {
    testGetPodSpecForController();
  }

  private void testGetPodSpecForController() {
    DeploymentConfig controller = new DeploymentConfig();
    DeploymentConfigSpec spec = new DeploymentConfigSpec();
    spec.setTemplate(new PodTemplateSpec());
    controller.setSpec(spec);

    PodTemplateSpec podTemplateSpec = kubernetesContainerService.getPodTemplateSpec(controller);
    assertThat(podTemplateSpec).isNotNull();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetService() throws Exception {
    V1Service service = new V1ServiceBuilder().build();

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1Service.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), service));

    V1Service result = kubernetesContainerService.getService(KUBERNETES_CONFIG, "service");
    assertThat(result).isEqualTo(service);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetServiceExceptioon() throws Exception {
    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1Service.class).getType()))
        .thenThrow(new ApiException(403, "", emptyMap(), "{error: \"unable to get service\"}"));

    assertThatThrownBy(() -> kubernetesContainerService.getService(KUBERNETES_CONFIG, "service"))
        .hasMessageContaining(
            "Unable to get default/Service/service. Code: 403, message:  Response body: {error: \"unable to get service\"}");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetServiceWithNulls() throws Exception {
    assertThatCode(() -> kubernetesContainerService.getService(null, null)).doesNotThrowAnyException();
    assertThatCode(() -> kubernetesContainerService.getService(KUBERNETES_CONFIG, null)).doesNotThrowAnyException();

    verify(k8sApiClient, never()).execute(k8sApiCall, TypeToken.get(V1ServiceList.class).getType());
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetActiveServiceCounts() {
    ObjectMeta objectMeta = new ObjectMeta();
    objectMeta.setName("controller");
    deployment.setMetadata(objectMeta);

    DeploymentList deploymentList = new DeploymentList();
    deploymentList.setItems(asList(deployment));

    when(namespacedDeployments.list()).thenReturn(deploymentList);
    LinkedHashMap<String, Integer> activeServiceCounts =
        kubernetesContainerService.getActiveServiceCounts(KUBERNETES_CONFIG, "controller-1");
    assertThat(activeServiceCounts.get("controller")).isEqualTo(2);

    activeServiceCounts = kubernetesContainerService.getActiveServiceCounts(KUBERNETES_CONFIG, "ctlr-2");
    assertThat(activeServiceCounts).isEmpty();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetVersionAsString() throws Exception {
    VersionInfo versionInfo = new VersionInfoBuilder().withMajor("1").withMinor("16").build();
    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(VersionInfo.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), versionInfo));

    String result = kubernetesContainerService.getVersionAsString(KUBERNETES_CONFIG);
    assertThat(result).isEqualTo("1.16");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetConfigFileContent() throws InterruptedException, ExecutionException, IOException {
    String expected = "apiVersion: v1\n"
        + "clusters:\n"
        + "- cluster:\n"
        + "    server: masterUrl\n"
        + "    insecure-skip-tls-verify: true\n"
        + "  name: CLUSTER_NAME\n"
        + "contexts:\n"
        + "- context:\n"
        + "    cluster: CLUSTER_NAME\n"
        + "    user: HARNESS_USER\n"
        + "    namespace: namespace\n"
        + "  name: CURRENT_CONTEXT\n"
        + "current-context: CURRENT_CONTEXT\n"
        + "kind: Config\n"
        + "preferences: {}\n"
        + "users:\n"
        + "- name: HARNESS_USER\n"
        + "  user:\n"
        + "    auth-provider:\n"
        + "      config:\n"
        + "        client-id: clientId\n"
        + "        client-secret: secret\n"
        + "        id-token: id_token\n"
        + "        refresh-token: refresh_token\n"
        + "        idp-issuer-url: url\n"
        + "      name: oidc\n";

    OpenIdOAuth2AccessToken accessToken = mock(OpenIdOAuth2AccessToken.class);
    doReturn("id_token").when(accessToken).getOpenIdToken();
    doReturn(3600).when(accessToken).getExpiresIn();
    doReturn("bearer").when(accessToken).getTokenType();
    doReturn("refresh_token").when(accessToken).getRefreshToken();

    doReturn(accessToken).when(oidcTokenRetriever).getAccessToken(any());

    // Test generating KubernetesConfig from KubernetesClusterConfig
    final KubernetesConfig kubeConfig = KubernetesConfig.builder()
                                            .masterUrl("masterUrl")
                                            .namespace("namespace")
                                            .accountId("accId")
                                            .authType(OIDC)
                                            .oidcIdentityProviderUrl("url")
                                            .oidcUsername("user")
                                            .oidcGrantType(OidcGrantType.password)
                                            .oidcPassword("pwd".toCharArray())
                                            .oidcClientId("clientId".toCharArray())
                                            .oidcSecret("secret".toCharArray())
                                            .build();

    String configFileContent = kubernetesContainerService.getConfigFileContent(kubeConfig);
    assertThat(configFileContent).isEqualTo(expected);
  }

  @Test
  @Owner(developers = BOGDAN)
  @Category(UnitTests.class)
  public void testGetGcpKubeConfigContent() {
    // given
    String masterUrl = "myMasterUrl";
    String caData = "myCaData";
    String namespace = "myNamespace";
    KubernetesConfig kubernetesConfig = KubernetesConfig.builder()
                                            .masterUrl(masterUrl)
                                            .caCert(caData.toCharArray())
                                            .namespace(namespace)
                                            .authType(GCP_OAUTH)
                                            .build();

    String expectedConfigPattern = "apiVersion: v1\n"
        + "clusters:\n"
        + "- cluster:\n"
        + "    server: %s\n"
        + "    \n"
        + "    certificate-authority-data: %s\n"
        + "  name: CLUSTER_NAME\n"
        + "contexts:\n"
        + "- context:\n"
        + "    cluster: CLUSTER_NAME\n"
        + "    user: HARNESS_USER\n"
        + "    namespace: %s\n"
        + "  name: CURRENT_CONTEXT\n"
        + "current-context: CURRENT_CONTEXT\n"
        + "kind: Config\n"
        + "preferences: {}\n"
        + "users:\n"
        + "- name: HARNESS_USER\n"
        + "  user:\n"
        + "    auth-provider:\n"
        + "      name: gcp\n";
    String expectedConfig = String.format(expectedConfigPattern, masterUrl, caData, namespace);

    // when
    String configFileContent = kubernetesContainerService.getConfigFileContent(kubernetesConfig);

    // then
    assertThat(configFileContent).isEqualTo(expectedConfig);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetConfigFileContentForBasicAuth() {
    String expected = "apiVersion: v1\n"
        + "clusters:\n"
        + "- cluster:\n"
        + "    server: masterUrl\n"
        + "    insecure-skip-tls-verify: true\n"
        + "    \n"
        + "  name: CLUSTER_NAME\n"
        + "contexts:\n"
        + "- context:\n"
        + "    cluster: CLUSTER_NAME\n"
        + "    user: HARNESS_USER\n"
        + "    namespace: namespace\n"
        + "  name: CURRENT_CONTEXT\n"
        + "current-context: CURRENT_CONTEXT\n"
        + "kind: Config\n"
        + "preferences: {}\n"
        + "users:\n"
        + "- name: HARNESS_USER\n"
        + "  user:\n"
        + "    \n"
        + "    \n"
        + "    password: password\n"
        + "    username: username\n"
        + "    ";

    KubernetesConfig kubeConfig = KubernetesConfig.builder()
                                      .authType(USER_PASSWORD)
                                      .namespace("namespace")
                                      .masterUrl("masterUrl")
                                      .username("username".toCharArray())
                                      .password("password".toCharArray())
                                      .build();
    String configFileContent = kubernetesContainerService.getConfigFileContent(kubeConfig);
    assertThat(expected).isEqualTo(configFileContent);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetConfigFileWithCACert() {
    String expected = "apiVersion: v1\n"
        + "clusters:\n"
        + "- cluster:\n"
        + "    server: masterUrl\n"
        + "    \n"
        + "    certificate-authority-data: caCert\n"
        + "  name: CLUSTER_NAME\n"
        + "contexts:\n"
        + "- context:\n"
        + "    cluster: CLUSTER_NAME\n"
        + "    user: HARNESS_USER\n"
        + "    namespace: namespace\n"
        + "  name: CURRENT_CONTEXT\n"
        + "current-context: CURRENT_CONTEXT\n"
        + "kind: Config\n"
        + "preferences: {}\n"
        + "users:\n"
        + "- name: HARNESS_USER\n"
        + "  user:\n"
        + "    \n"
        + "    \n"
        + "    \n"
        + "    \n"
        + "    token: serviceAccountToken";

    KubernetesConfig kubeConfig = KubernetesConfig.builder()
                                      .authType(USER_PASSWORD)
                                      .namespace("namespace")
                                      .masterUrl("masterUrl")
                                      .caCert("caCert".toCharArray())
                                      .serviceAccountTokenSupplier(() -> "serviceAccountToken")
                                      .build();
    String configFileContent = kubernetesContainerService.getConfigFileContent(kubeConfig);
    assertThat(expected).isEqualTo(configFileContent);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetVersion() throws Exception {
    VersionInfo versionInfo = new VersionInfoBuilder().withMajor("1").withMinor("16").build();
    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(VersionInfo.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), versionInfo));
    VersionInfo result = kubernetesContainerService.getVersion(KUBERNETES_CONFIG);
    assertThat(result).isEqualTo(versionInfo);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetVersionException() throws Exception {
    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(VersionInfo.class).getType()))
        .thenThrow(new ApiException(409, "Unable to get cluster version"));
    assertThatThrownBy(() -> kubernetesContainerService.getVersion(KUBERNETES_CONFIG))
        .hasMessage("Unable to retrieve k8s version. Code: 409, message: Unable to get cluster version");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetRunningPodsWithLabels() throws Exception {
    V1PodStatus runningStatus = new V1PodStatusBuilder().withPhase("Running").build();
    V1PodStatus terminatedStatus = new V1PodStatusBuilder().withPhase("Terminated").build();
    Map<String, String> labels = ImmutableMap.of("label1", "value1", "label2", "value2");
    V1PodList v1PodList = new V1PodListBuilder()
                              .addNewItem() // With running status
                              .withNewMetadata()
                              .withName("pod-1")
                              .endMetadata()
                              .withStatus(runningStatus)
                              .endItem()
                              .addNewItem() // With deletion timestamp
                              .withNewMetadata()
                              .withName("pod-2")
                              .withDeletionTimestamp(OffsetDateTime.now())
                              .endMetadata()
                              .withStatus(terminatedStatus)
                              .endItem()
                              .addNewItem() // Without status
                              .withNewMetadata()
                              .withName("pod-3")
                              .endMetadata()
                              .endItem()
                              .addNewItem() // With terminated status
                              .withNewMetadata()
                              .withName("pod-4")
                              .endMetadata()
                              .withStatus(terminatedStatus)
                              .endItem()
                              .build();

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1PodList.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), v1PodList));

    List<V1Pod> result = kubernetesContainerService.getRunningPodsWithLabels(KUBERNETES_CONFIG, "default", labels);
    ArgumentCaptor<List<Pair>> queryParamsCaptor = ArgumentCaptor.forClass((Class) List.class);
    verify(k8sApiClient, times(1))
        .buildCall(anyString(), eq("GET"), queryParamsCaptor.capture(), anyList(), any(), anyMap(), anyMap(), anyMap(),
            any(String[].class), any());
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getMetadata().getName()).isEqualTo("pod-1");
    Optional<Pair> labelSelector =
        queryParamsCaptor.getValue().stream().filter(pair -> "labelSelector".equals(pair.getName())).findAny();
    assertThat(labelSelector).isPresent();
    assertThat(labelSelector.get().getValue()).isEqualTo("label1=value1,label2=value2");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetRunningPodsWithLabelsException() throws Exception {
    Map<String, String> labels = ImmutableMap.of("label1", "value1");
    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1PodList.class).getType()))
        .thenThrow(new ApiException(401, emptyMap(), "{\"error\": \"unauthorized\"}"));

    assertThatThrownBy(() -> kubernetesContainerService.getRunningPodsWithLabels(KUBERNETES_CONFIG, "default", labels))
        .hasMessageContaining(
            "Unable to get running pods. Code: 401, message:  Response body: {\"error\": \"unauthorized\"}");
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testGetConfigMap() throws Exception {
    V1ConfigMap configMap = new V1ConfigMapBuilder().build();

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1ConfigMap.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), configMap));

    V1ConfigMap result = kubernetesContainerService.getConfigMap(KUBERNETES_CONFIG, "configmap");
    assertThat(result).isEqualTo(configMap);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldGetNullWhenNoConfigMapExists() throws Exception {
    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1ConfigMap.class).getType()))
        .thenThrow(new ApiException(404, null, null, null));

    V1ConfigMap result = kubernetesContainerService.getConfigMap(KUBERNETES_CONFIG, "configmap");
    assertThat(result).isNull();
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testGetConfigMapException() throws Exception {
    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1ConfigMap.class).getType()))
        .thenThrow(new ApiException(403, "", emptyMap(), "{error: \"cluster not found\"}"));

    assertThatThrownBy(() -> kubernetesContainerService.getConfigMap(KUBERNETES_CONFIG, "configmap"))
        .hasMessageContaining(
            "Failed to get default/ConfigMap/configmap. Code: 403, message:  Response body: {error: \"cluster not found\"}");
  }

  @Test
  @Owner(developers = ACHYUTH)
  @Category(UnitTests.class)
  public void testGetConfigMapNestedException() throws Exception {
    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1ConfigMap.class).getType()))
        .thenThrow(new ApiException(
            null, new IOException("Unexpected response code for CONNECT: 403"), 403, emptyMap(), null));

    assertThatThrownBy(() -> kubernetesContainerService.getConfigMap(KUBERNETES_CONFIG, "configmap"))
        .hasMessageContaining(
            "Failed to get default/ConfigMap/configmap. Code: 403, message: Unexpected response code for CONNECT: 403");
  }

  @Test
  @Owner(developers = ACHYUTH)
  @Category(UnitTests.class)
  public void testGetConfigMapNestedExceptionWithRespnseBody() throws Exception {
    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1ConfigMap.class).getType()))
        .thenThrow(new ApiException(
            null, new IOException("Unexpected response code for CONNECT: 403"), 403, emptyMap(), "connection issue"));

    assertThatThrownBy(() -> kubernetesContainerService.getConfigMap(KUBERNETES_CONFIG, "configmap"))
        .hasMessageContaining(
            "Failed to get default/ConfigMap/configmap. Code: 403, message: Unexpected response code for CONNECT: 403 Response body: connection issue");
  }

  @Test
  @Owner(developers = ACHYUTH)
  @Category(UnitTests.class)
  public void testGetConfigMapNestedExceptionHasEmptyMessage() throws Exception {
    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1ConfigMap.class).getType()))
        .thenThrow(
            new ApiException("Unexpected response code for CONNECT: 403", new IOException(), 403, emptyMap(), null));

    assertThatThrownBy(() -> kubernetesContainerService.getConfigMap(KUBERNETES_CONFIG, "configmap"))
        .hasMessageContaining(
            "Failed to get default/ConfigMap/configmap. Code: 403, message: Unexpected response code for CONNECT: 403");
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testGetSecret() throws Exception {
    V1Secret secret = new V1SecretBuilder().build();

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1Secret.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), secret));

    V1Secret result = kubernetesContainerService.getSecret(KUBERNETES_CONFIG, "secret");
    assertThat(result).isEqualTo(secret);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldGetNullWhenNoSecretExists() throws Exception {
    V1Secret secret = new V1SecretBuilder().build();

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1Secret.class).getType()))
        .thenThrow(new ApiException(404, null, null, null));

    V1Secret result = kubernetesContainerService.getSecret(KUBERNETES_CONFIG, "secret");
    assertThat(result).isNull();
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testGetSecretException() throws Exception {
    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1Secret.class).getType()))
        .thenThrow(new ApiException(403, "", emptyMap(), "{error: \"cluster not found\"}"));

    assertThatThrownBy(() -> kubernetesContainerService.getSecret(KUBERNETES_CONFIG, "secret"))
        .hasMessageContaining(
            "Failed to get default/Secret/secret. Code: 403, message:  Response body: {error: \"cluster not found\"}");
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldFetchCompressedReleaseHistoryFromSecrets() throws Exception {
    byte[] dummyReleaseHistory = compressString("test");
    V1Secret v1Secret = new V1SecretBuilder()
                            .withData(ImmutableMap.of(ReleaseHistoryKeyName, dummyReleaseHistory,
                                CompressedReleaseHistoryFlag, new byte[] {(byte) 1}))
                            .build();

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1Secret.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), v1Secret));

    String releaseHistory = kubernetesContainerService.fetchReleaseHistoryFromSecrets(KUBERNETES_CONFIG, "secret");

    assertThat(releaseHistory).isEqualTo("test");
  }

  @Test
  @Owner(developers = ABHINAV2)
  @Category(UnitTests.class)
  public void testShouldFetchUncompressedReleaseHistoryFromSecrets() throws Exception {
    V1Secret v1Secret =
        new V1SecretBuilder()
            .withData(ImmutableMap.of(ReleaseHistoryKeyName, DUMMY_RELEASE_HISTORY.getBytes(StandardCharsets.UTF_8)))
            .build();

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1Secret.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), v1Secret));

    String releaseHistory = kubernetesContainerService.fetchReleaseHistoryFromSecrets(KUBERNETES_CONFIG, "secret");

    assertThat(releaseHistory).isEqualTo(DUMMY_RELEASE_HISTORY);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldFetchEmptyReleaseHistoryFromSecrets() throws Exception {
    V1Secret secret = new V1SecretBuilder().build();

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1Secret.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), secret));

    String releaseHistory = kubernetesContainerService.fetchReleaseHistoryFromSecrets(KUBERNETES_CONFIG, "secret");

    assertThat(releaseHistory).isEmpty();
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldFetchEmptyReleaseHistoryWhenNoSecrets() throws Exception {
    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1Secret.class).getType()))
        .thenThrow(new ApiException(404, null, null, null));

    String releaseHistory = kubernetesContainerService.fetchReleaseHistoryFromSecrets(KUBERNETES_CONFIG, "secret");
    assertThat(releaseHistory).isEmpty();
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldFetchCompressedReleaseHistoryFromConfigMap() throws Exception {
    String dummyReleaseHistory = encodeBase64(compressString("test"));
    V1ConfigMap configMap =
        new V1ConfigMapBuilder()
            .withData(ImmutableMap.of(ReleaseHistoryKeyName, dummyReleaseHistory, CompressedReleaseHistoryFlag, "true"))
            .build();

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1ConfigMap.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), configMap));

    String releaseHistory = kubernetesContainerService.fetchReleaseHistoryFromConfigMap(KUBERNETES_CONFIG, "configmap");
    assertThat(releaseHistory).isEqualTo("test");
  }

  @Test
  @Owner(developers = ABHINAV2)
  @Category(UnitTests.class)
  public void testShouldFetchUncompressedReleaseHistoryFromConfigMap() throws Exception {
    V1ConfigMap configMap =
        new V1ConfigMapBuilder().withData(ImmutableMap.of(ReleaseHistoryKeyName, DUMMY_RELEASE_HISTORY)).build();

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1ConfigMap.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), configMap));

    String releaseHistory = kubernetesContainerService.fetchReleaseHistoryFromConfigMap(KUBERNETES_CONFIG, "configmap");
    assertThat(releaseHistory).isEqualTo(DUMMY_RELEASE_HISTORY);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldFetchEmptyReleaseHistoryFromConfigMap() throws Exception {
    V1ConfigMap configMap = new V1ConfigMapBuilder().build();

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1ConfigMap.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), configMap));

    String releaseHistory = kubernetesContainerService.fetchReleaseHistoryFromConfigMap(KUBERNETES_CONFIG, "configmap");
    assertThat(releaseHistory).isEmpty();
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldFetchEmptyReleaseHistoryWhenNoConfigMapExists() throws Exception {
    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1ConfigMap.class).getType()))
        .thenThrow(new ApiException(404, null, null, null));

    String releaseHistory = kubernetesContainerService.fetchReleaseHistoryFromConfigMap(KUBERNETES_CONFIG, "configmap");
    assertThat(releaseHistory).isEmpty();
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldUpdateReleaseHistoryInConfigMap() throws Exception {
    V1ConfigMap configMap =
        new V1ConfigMapBuilder()
            .withMetadata(
                new V1ObjectMetaBuilder().withNamespace(KUBERNETES_CONFIG.getNamespace()).withName("release").build())
            .withData(ImmutableMap.of(ReleaseHistoryKeyName, "test"))
            .build();

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1ConfigMap.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), configMap));

    configMap.getMetadata().setNamespace("test");
    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1ConfigMap.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), configMap));

    V1ObjectMeta result =
        kubernetesContainerService.saveReleaseHistory(KUBERNETES_CONFIG, "release", "version=1.0", false);
    assertThat(result).isNotNull();
    assertThat(result.getNamespace()).isEqualTo("test");
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldCreateReleaseHistoryInConfigMap() throws Exception {
    String dummyReleaseHistory = encodeBase64(compressString("version=2.0"));
    V1ConfigMap configMap =
        new V1ConfigMapBuilder()
            .withMetadata(
                new V1ObjectMetaBuilder().withNamespace(KUBERNETES_CONFIG.getNamespace()).withName("release").build())
            .withData(ImmutableMap.of(ReleaseHistoryKeyName, dummyReleaseHistory))
            .build();

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1ConfigMapList.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), new V1ConfigMapList()));

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1ConfigMap.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), configMap));

    V1ObjectMeta result =
        kubernetesContainerService.saveReleaseHistory(KUBERNETES_CONFIG, "release", "version=2.0", false);
    assertThat(result).isNotNull();
    assertThat(result.getNamespace()).isEqualTo("default");
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldSaveReleaseHistoryInConfigMapWhenFalse() throws Exception {
    V1ConfigMap configMap =
        new V1ConfigMapBuilder()
            .withMetadata(
                new V1ObjectMetaBuilder().withNamespace(KUBERNETES_CONFIG.getNamespace()).withName("release").build())
            .withData(ImmutableMap.of(ReleaseHistoryKeyName, "version=2.0"))
            .build();

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1ConfigMap.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), configMap));

    kubernetesContainerService.saveReleaseHistory(KUBERNETES_CONFIG, "release", "version=2.0", false);

    verify(k8sApiClient, times(2)).execute(k8sApiCall, TypeToken.get(V1ConfigMap.class).getType());
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldUpdateReleaseHistoryInSecret() throws Exception {
    V1Secret secret =
        new V1SecretBuilder()
            .withMetadata(
                new V1ObjectMetaBuilder().withNamespace(KUBERNETES_CONFIG.getNamespace()).withName("release").build())
            .withData(ImmutableMap.of(ReleaseHistoryKeyName, encodeBase64ToByteArray("test".getBytes())))
            .build();

    secret.getMetadata().setNamespace("test");
    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1Secret.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), secret));

    V1ObjectMeta secretMeta =
        kubernetesContainerService.saveReleaseHistory(KUBERNETES_CONFIG, "release", "version=1.0", true);
    assertThat(secretMeta).isNotNull();
    assertThat(secretMeta.getNamespace()).isEqualTo("test");
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldCreateReleaseHistoryInSecret() throws Exception {
    byte[] dummyReleaseHistory = encodeBase64ToByteArray(compressString("version=2.0"));
    V1Secret secret =
        new V1SecretBuilder()
            .withMetadata(
                new V1ObjectMetaBuilder().withNamespace(KUBERNETES_CONFIG.getNamespace()).withName("release").build())
            .withData(ImmutableMap.of(ReleaseHistoryKeyName, dummyReleaseHistory))
            .build();

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1Secret.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), secret));

    V1ObjectMeta result =
        kubernetesContainerService.saveReleaseHistory(KUBERNETES_CONFIG, "release", "version=2.0", true);
    assertThat(result).isNotNull();
    assertThat(result.getNamespace()).isEqualTo("default");
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldCreateReleaseHistoryInSecretWhenTrue() throws Exception {
    V1Secret secret =
        new V1SecretBuilder()
            .withMetadata(
                new V1ObjectMetaBuilder().withNamespace(KUBERNETES_CONFIG.getNamespace()).withName("release").build())
            .withData(ImmutableMap.of(ReleaseHistoryKeyName, encodeBase64ToByteArray("version=2.0".getBytes())))
            .build();

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1Secret.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), secret));

    kubernetesContainerService.saveReleaseHistory(KUBERNETES_CONFIG, "release", "version=2.0", true);

    verify(k8sApiClient, times(2)).execute(k8sApiCall, TypeToken.get(V1Secret.class).getType());
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldDeleteSecret() throws Exception {
    V1Status status = new V1StatusBuilder().withMetadata(new V1ListMetaBuilder().build()).build();
    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1Status.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), status));

    kubernetesContainerService.deleteSecret(KUBERNETES_CONFIG, "release");

    verify(k8sApiClient, times(1)).execute(k8sApiCall, TypeToken.get(V1Status.class).getType());
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldDeleteConfigMap() throws Exception {
    V1Status status = new V1StatusBuilder().withMetadata(new V1ListMetaBuilder().build()).build();
    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1Status.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), status));

    kubernetesContainerService.deleteConfigMap(KUBERNETES_CONFIG, "release");

    verify(k8sApiClient, times(1)).execute(k8sApiCall, TypeToken.get(V1Status.class).getType());
  }

  @Test
  @Owner(developers = ABHINAV2)
  @Category(UnitTests.class)
  public void testFetchReleaseHistoryFromUncompressedConfigMap() throws IOException {
    V1ConfigMap uncompressedConfigMap =
        new V1ConfigMapBuilder().withData(ImmutableMap.of(ReleaseHistoryKeyName, DUMMY_RELEASE_HISTORY)).build();

    assertThat(kubernetesContainerService.fetchReleaseHistoryValue(uncompressedConfigMap))
        .isEqualTo(DUMMY_RELEASE_HISTORY);
  }

  @Test
  @Owner(developers = ABHINAV2)
  @Category(UnitTests.class)
  public void testFetchReleaseHistoryFromCompressedConfigMap() throws IOException {
    String dummyReleaseHistory = encodeBase64(compressString(DUMMY_RELEASE_HISTORY));
    V1ConfigMap compressedConfigMap =
        new V1ConfigMapBuilder()
            .withData(ImmutableMap.of(ReleaseHistoryKeyName, dummyReleaseHistory, CompressedReleaseHistoryFlag, "true"))
            .build();

    assertThat(kubernetesContainerService.fetchReleaseHistoryValue(compressedConfigMap))
        .isEqualTo(DUMMY_RELEASE_HISTORY);
  }

  @Test
  @Owner(developers = ABHINAV2)
  @Category(UnitTests.class)
  public void testFetchReleaseHistoryFromUncompressedSecret() throws IOException {
    V1Secret uncompressedSecret =
        new V1SecretBuilder()
            .withMetadata(
                new V1ObjectMetaBuilder().withNamespace(KUBERNETES_CONFIG.getNamespace()).withName("release").build())
            .withData(ImmutableMap.of(ReleaseHistoryKeyName, DUMMY_RELEASE_HISTORY.getBytes(StandardCharsets.UTF_8)))
            .build();
    assertThat(kubernetesContainerService.fetchReleaseHistoryValue(uncompressedSecret))
        .isEqualTo(DUMMY_RELEASE_HISTORY);
  }

  @Test
  @Owner(developers = ABHINAV2)
  @Category(UnitTests.class)
  public void testFetchReleaseHistoryFromCompressedSecret() throws IOException {
    byte[] dummyReleaseHistory = compressString(DUMMY_RELEASE_HISTORY);
    V1Secret compressedSecret =
        new V1SecretBuilder()
            .withMetadata(
                new V1ObjectMetaBuilder().withNamespace(KUBERNETES_CONFIG.getNamespace()).withName("release").build())
            .withData(ImmutableMap.of(
                ReleaseHistoryKeyName, dummyReleaseHistory, CompressedReleaseHistoryFlag, new byte[] {(byte) 1}))
            .build();

    assertThat(kubernetesContainerService.fetchReleaseHistoryValue(compressedSecret)).isEqualTo(DUMMY_RELEASE_HISTORY);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldCreateService() throws Exception {
    V1Service service = new V1ServiceBuilder()
                            .withNewMetadata()
                            .withName("service1")
                            .withNamespace(KUBERNETES_CONFIG.getNamespace())
                            .endMetadata()
                            .build();

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1Service.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), service));

    V1Service result = kubernetesContainerService.createService(KUBERNETES_CONFIG, service);
    assertThat(result).isNotNull();
    assertThat(result.getMetadata().getName()).isEqualTo("service1");

    verify(k8sApiClient, times(1)).execute(k8sApiCall, TypeToken.get(V1Service.class).getType());
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testCreateServiceInvalidRequestException() throws Exception {
    V1Service service = new V1ServiceBuilder()
                            .withNewMetadata()
                            .withName("service1")
                            .withNamespace(KUBERNETES_CONFIG.getNamespace())
                            .endMetadata()
                            .build();

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1Service.class).getType()))
        .thenThrow(new ApiException(404, "Service not found"));
    kubernetesContainerService.createService(KUBERNETES_CONFIG, service);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldReplaceService() throws Exception {
    V1Service service = new V1ServiceBuilder()
                            .withNewMetadata()
                            .withName("service1")
                            .withNamespace(KUBERNETES_CONFIG.getNamespace())
                            .endMetadata()
                            .build();

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1Service.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), service));

    V1Service result = kubernetesContainerService.replaceService(KUBERNETES_CONFIG, service);
    assertThat(result).isNotNull();
    assertThat(result.getMetadata().getName()).isEqualTo("service1");

    verify(k8sApiClient, times(1)).execute(k8sApiCall, TypeToken.get(V1Service.class).getType());
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testReplaceServiceInvalidRequestException() throws Exception {
    V1Service service = new V1ServiceBuilder()
                            .withNewMetadata()
                            .withName("service1")
                            .withNamespace(KUBERNETES_CONFIG.getNamespace())
                            .endMetadata()
                            .build();

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1Service.class).getType()))
        .thenThrow(new ApiException(404, "Service not found"));
    kubernetesContainerService.replaceService(KUBERNETES_CONFIG, service);
  }

  @Test
  @Owner(developers = BOGDAN)
  @Category(UnitTests.class)
  public void shouldPersistK8sConfig() throws IOException {
    // given
    Path workingDir = Files.createTempDirectory("testWorkingDir");
    KubernetesConfig config = KubernetesConfig.builder().masterUrl("masterUrl").build();

    // when
    kubernetesContainerService.persistKubernetesConfig(config, workingDir.toString());

    // then
    byte[] configFile = Files.readAllBytes(workingDir.resolve(K8sConstants.KUBECONFIG_FILENAME));
    assertThat(configFile).isNotEmpty();
  }

  @Test
  @Owner(developers = BOGDAN)
  @Category(UnitTests.class)
  public void shouldPersistKubeGcpKubeConfig() throws IOException {
    // given
    Path workingDir = Files.createTempDirectory("testWorkingDir");
    KubernetesConfig config = KubernetesConfig.builder()
                                  .masterUrl("myMasterUrl")
                                  .caCert("myCaCert".toCharArray())
                                  .namespace("myNamespace")
                                  .authType(GCP_OAUTH)
                                  .build();

    // when
    kubernetesContainerService.persistKubernetesConfig(config, workingDir.toString());

    // then
    byte[] configFile = Files.readAllBytes(workingDir.resolve(K8sConstants.KUBECONFIG_FILENAME));
    assertThat(configFile).isNotEmpty();
  }

  @Test
  @Owner(developers = BOGDAN)
  @Category(UnitTests.class)
  public void shouldPersistGoogleAccountKeyInWorkingDir() throws IOException {
    // given
    GcpAccessTokenSupplier tokenSupplier = mock(GcpAccessTokenSupplier.class);
    String expectedGcpKeyJson = "dummy gcp json key file data";
    when(tokenSupplier.getServiceAccountJsonKey()).thenReturn(Optional.of(expectedGcpKeyJson));
    KubernetesConfig config =
        KubernetesConfig.builder().authType(GCP_OAUTH).serviceAccountTokenSupplier(tokenSupplier).build();

    // when
    Path workingDir = Files.createTempDirectory("testWorkingDir");
    kubernetesContainerService.persistKubernetesConfig(config, workingDir.toString());

    // then
    List<String> lines = Files.readAllLines(workingDir.resolve(K8sConstants.GCP_JSON_KEY_FILE_NAME));
    assertThat(lines.size()).isEqualTo(1);
    assertThat(lines.get(0)).isEqualTo(expectedGcpKeyJson);
  }

  @Test
  @Owner(developers = TARUN_UBA)
  @Category(UnitTests.class)
  public void testReplaceConfigMap() throws Exception {
    V1ConfigMap configMap =
        new V1ConfigMapBuilder()
            .withMetadata(
                new V1ObjectMetaBuilder().withNamespace(KUBERNETES_CONFIG.getNamespace()).withName("release").build())
            .withData(ImmutableMap.of(ReleaseHistoryKeyName, "version=2.0"))
            .build();

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1ConfigMap.class).getType()))
        .thenThrow(new ApiException(new ConnectionShutdownException()));

    try {
      kubernetesContainerService.replaceConfigMap(KUBERNETES_CONFIG, configMap);
    } catch (InvalidRequestException e) {
    }
    verify(k8sApiClient, times(3)).execute(k8sApiCall, TypeToken.get(V1ConfigMap.class).getType());
  }

  @Test
  @Owner(developers = PRATYUSH)
  @Category(UnitTests.class)
  public void testGetConfigFileForAzureExecFormat() {
    String expected = "apiVersion: v1\n"
        + "clusters:\n"
        + "- cluster:\n"
        + "    server: masterUrl\n"
        + "    \n"
        + "    certificate-authority-data: caCert\n"
        + "  name: CLUSTER_NAME\n"
        + "contexts:\n"
        + "- context:\n"
        + "    cluster: CLUSTER_NAME\n"
        + "    user: CLUSTER_USER\n"
        + "    namespace: namespace\n"
        + "  name: CURRENT_CONTEXT\n"
        + "current-context: CURRENT_CONTEXT\n"
        + "kind: Config\n"
        + "preferences: {}\n"
        + "users:\n"
        + "- name: CLUSTER_USER\n"
        + "  user:\n"
        + "    exec:\n"
        + "      apiVersion: client.authentication.k8s.io/v1beta1\n"
        + "      args:\n"
        + "      - args1\n"
        + "      - args2\n"
        + "      command: command\n"
        + "      env: null\n"
        + "      interactiveMode: Never\n"
        + "      provideClusterInfo: false\n"
        + "      installHint: hint";

    KubernetesAzureConfig kubernetesAzureConfig = KubernetesAzureConfig.builder()
                                                      .clusterName("CLUSTER_NAME")
                                                      .currentContext("CURRENT_CONTEXT")
                                                      .clusterUser("CLUSTER_USER")
                                                      .environment("ENVIRONMENT")
                                                      .tenantId("TENANT_ID")
                                                      .apiServerId("APISERVER_ID")
                                                      .clientId("CLIENT_ID")
                                                      .aadIdToken("TOKEN")
                                                      .build();

    Exec exec = Exec.builder()
                    .apiVersion("client.authentication.k8s.io/v1beta1")
                    .command("command")
                    .args(asList("args1", "args2"))
                    .env(null)
                    .provideClusterInfo(false)
                    .installHint("hint")
                    .interactiveMode(InteractiveMode.NEVER)
                    .build();

    KubernetesConfig kubeConfig = KubernetesConfig.builder()
                                      .authType(EXEC_OAUTH)
                                      .namespace("namespace")
                                      .masterUrl("masterUrl")
                                      .caCert("caCert".toCharArray())
                                      .clientKey("CLIENT_KEY".toCharArray())
                                      .azureConfig(kubernetesAzureConfig)
                                      .exec(exec)
                                      .build();
    String configFileContent = kubernetesContainerService.getConfigFileContent(kubeConfig);
    assertThat(expected).isEqualTo(configFileContent);
  }

  @Test
  @Owner(developers = PRATYUSH)
  @Category(UnitTests.class)
  public void testGetConfigFileForGcpExecFormat() {
    String expected = "apiVersion: v1\n"
        + "clusters:\n"
        + "- cluster:\n"
        + "    server: masterUrl\n"
        + "    \n"
        + "    certificate-authority-data: caCert\n"
        + "  name: CLUSTER_NAME\n"
        + "contexts:\n"
        + "- context:\n"
        + "    cluster: CLUSTER_NAME\n"
        + "    user: HARNESS_USER\n"
        + "    namespace: namespace\n"
        + "  name: CURRENT_CONTEXT\n"
        + "current-context: CURRENT_CONTEXT\n"
        + "kind: Config\n"
        + "preferences: {}\n"
        + "users:\n"
        + "- name: HARNESS_USER\n"
        + "  user:\n"
        + "    exec:\n"
        + "      apiVersion: client.authentication.k8s.io/v1beta1\n"
        + "      args: null\n"
        + "      command: command\n"
        + "      env:\n"
        + "      - name: name\n"
        + "        value: value\n"
        + "      interactiveMode: Never\n"
        + "      provideClusterInfo: false\n"
        + "      installHint: hint";

    Exec exec = Exec.builder()
                    .apiVersion("client.authentication.k8s.io/v1beta1")
                    .command("command")
                    .args(null)
                    .env(singletonList(EnvVariable.builder().name("name").value("value").build()))
                    .provideClusterInfo(false)
                    .installHint("hint")
                    .interactiveMode(InteractiveMode.NEVER)
                    .build();

    KubernetesConfig kubeConfig = KubernetesConfig.builder()
                                      .authType(EXEC_OAUTH)
                                      .namespace("namespace")
                                      .masterUrl("masterUrl")
                                      .caCert("caCert".toCharArray())
                                      .clientKey("CLIENT_KEY".toCharArray())
                                      .exec(exec)
                                      .build();
    String configFileContent = kubernetesContainerService.getConfigFileContent(kubeConfig);
    assertThat(expected).isEqualTo(configFileContent);
  }

  @Test
  @Owner(developers = TARUN_UBA)
  @Category(UnitTests.class)
  public void testKubeConfigFilePermissions() throws Exception {
    FileIo.createDirectoryIfDoesNotExist(workingDir);
    Files.createFile(configFilePath);
    Set<PosixFilePermission> initialPermissions = new HashSet<>();
    initialPermissions.add(PosixFilePermission.OWNER_READ);
    initialPermissions.add(PosixFilePermission.OWNER_WRITE);
    initialPermissions.add(PosixFilePermission.GROUP_READ);
    initialPermissions.add(PosixFilePermission.OTHERS_READ);
    Files.setPosixFilePermissions(configFilePath, initialPermissions);

    kubernetesContainerService.modifyKubeConfigReadableProperties(workingDir.toString());

    try {
      // Check if group and others read permissions are removed
      Set<PosixFilePermission> updatedPermissions = Files.getPosixFilePermissions(configFilePath);
      assertThat(updatedPermissions.contains(PosixFilePermission.GROUP_READ)).isFalse();
      assertThat(updatedPermissions.contains(PosixFilePermission.OTHERS_READ)).isFalse();
    } catch (Exception e) {
      Assertions.fail("Exception thrown while checking permissions: " + e.getMessage());
    }
  }

  @Test
  @Owner(developers = TARUN_UBA)
  @Category(UnitTests.class)
  public void testKubeConfigFilePermissionsWithFile() throws Exception {
    FileIo.createDirectoryIfDoesNotExist(workingDir);
    Files.createFile(configFilePath);
    Set<PosixFilePermission> initialPermissions = new HashSet<>();
    initialPermissions.add(PosixFilePermission.OWNER_READ);
    initialPermissions.add(PosixFilePermission.OWNER_WRITE);
    initialPermissions.add(PosixFilePermission.GROUP_READ);
    initialPermissions.add(PosixFilePermission.OTHERS_READ);
    Files.setPosixFilePermissions(configFilePath, initialPermissions);

    kubernetesContainerService.modifyFileReadableProperties(configFilePath.toString());

    try {
      // Check if group and others read permissions are removed
      Set<PosixFilePermission> updatedPermissions = Files.getPosixFilePermissions(configFilePath);
      assertThat(updatedPermissions.contains(PosixFilePermission.GROUP_READ)).isFalse();
      assertThat(updatedPermissions.contains(PosixFilePermission.OTHERS_READ)).isFalse();
    } catch (Exception e) {
      Assertions.fail("Exception thrown while checking permissions: " + e.getMessage());
    }
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetRunningPodsWithLabelsAsList() throws Exception {
    V1PodStatus runningStatus = new V1PodStatusBuilder().withPhase("Running").build();
    V1PodStatus terminatedStatus = new V1PodStatusBuilder().withPhase("Terminated").build();
    List<String> labels =
        ImmutableList.of("label1=value1", "label2=value2", "label3 in (value4, value5)", "label4 notin (label6)");
    V1PodList v1PodList = new V1PodListBuilder()
                              .addNewItem() // With running status
                              .withNewMetadata()
                              .withName("pod-1")
                              .endMetadata()
                              .withStatus(runningStatus)
                              .endItem()
                              .addNewItem() // With deletion timestamp
                              .withNewMetadata()
                              .withName("pod-2")
                              .withDeletionTimestamp(OffsetDateTime.now())
                              .endMetadata()
                              .withStatus(terminatedStatus)
                              .endItem()
                              .addNewItem() // Without status
                              .withNewMetadata()
                              .withName("pod-3")
                              .endMetadata()
                              .endItem()
                              .addNewItem() // With terminated status
                              .withNewMetadata()
                              .withName("pod-4")
                              .endMetadata()
                              .withStatus(terminatedStatus)
                              .endItem()
                              .build();

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1PodList.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), v1PodList));

    List<V1Pod> result = kubernetesContainerService.getRunningPodsWithLabels(KUBERNETES_CONFIG, "default", labels);
    ArgumentCaptor<List<Pair>> queryParamsCaptor = ArgumentCaptor.forClass((Class) List.class);
    verify(k8sApiClient, times(1))
        .buildCall(anyString(), eq("GET"), queryParamsCaptor.capture(), anyList(), any(), anyMap(), anyMap(), anyMap(),
            any(String[].class), any());
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getMetadata().getName()).isEqualTo("pod-1");
    Optional<Pair> labelSelector =
        queryParamsCaptor.getValue().stream().filter(pair -> "labelSelector".equals(pair.getName())).findAny();
    assertThat(labelSelector).isPresent();
    assertThat(labelSelector.get().getValue())
        .isEqualTo("label1=value1,label2=value2,label3 in (value4, value5),label4 notin (label6)");
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetRunningPodsWithLabelsAsListException() throws Exception {
    List<String> labels = ImmutableList.of("label1=value1");
    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1PodList.class).getType()))
        .thenThrow(new ApiException(401, emptyMap(), "{\"error\": \"unauthorized\"}"));

    assertThatThrownBy(() -> kubernetesContainerService.getRunningPodsWithLabels(KUBERNETES_CONFIG, "default", labels))
        .hasMessageContaining(
            "Unable to get running pods. Code: 401, message:  Response body: {\"error\": \"unauthorized\"}");
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetRunningPodsWithLabelsAsListIsEmpty() {
    List<String> labels = Collections.emptyList();

    List<V1Pod> result = kubernetesContainerService.getRunningPodsWithLabels(KUBERNETES_CONFIG, "default", labels);

    assertThat(result).isEmpty();
  }

  @Test
  @Owner(developers = PRATYUSH)
  @Category(UnitTests.class)
  public void testGetControllerWithoutKubeconfig() {
    V1ObjectMeta controller =
        kubernetesContainerService.getControllerUsingK8sClient(null, "metadata.name=controllerName");
    assertThat(controller).isNull();
  }

  @Test
  @Owner(developers = PRATYUSH)
  @Category(UnitTests.class)
  public void testGetControllerWithReplicaController() throws ApiException {
    V1ReplicationController v1ReplicationController = new V1ReplicationController();
    v1ReplicationController.setMetadata(new V1ObjectMeta());
    V1ReplicationControllerList v1ReplicationControllerList = new V1ReplicationControllerList();
    v1ReplicationControllerList.setItems(Collections.singletonList(v1ReplicationController));
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<V1ReplicationControllerList>() {}).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), v1ReplicationControllerList));
    V1ObjectMeta controller =
        kubernetesContainerService.getControllerUsingK8sClient(KUBERNETES_CONFIG, "metadata.name=controllerName");
    assertThat(controller).isNotNull();
  }

  @Test
  @Owner(developers = PRATYUSH)
  @Category(UnitTests.class)
  public void testGetControllerWithDeploymentController() throws ApiException {
    V1Deployment v1Deployment = new V1Deployment();
    v1Deployment.setMetadata(new V1ObjectMeta());
    V1DeploymentList v1DeploymentList = new V1DeploymentList();
    v1DeploymentList.setItems(Collections.singletonList(v1Deployment));
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<V1ReplicationControllerList>() {}).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), null));
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<V1DeploymentList>() {}).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), v1DeploymentList));
    V1ObjectMeta controller =
        kubernetesContainerService.getControllerUsingK8sClient(KUBERNETES_CONFIG, "metadata.name=controllerName");
    assertThat(controller).isNotNull();
  }

  @Test
  @Owner(developers = PRATYUSH)
  @Category(UnitTests.class)
  public void testGetControllerWithReplicaSetController() throws ApiException {
    V1ReplicaSet v1ReplicaSet = new V1ReplicaSet();
    v1ReplicaSet.setMetadata(new V1ObjectMeta());
    V1ReplicaSetList v1ReplicaSetList = new V1ReplicaSetList();
    v1ReplicaSetList.setItems(Collections.singletonList(v1ReplicaSet));
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<V1ReplicationControllerList>() {}).getType()))
        .thenThrow(new ApiException());
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<V1DeploymentList>() {}).getType()))
        .thenThrow(new ApiException());
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<V1ReplicaSetList>() {}).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), v1ReplicaSetList));
    V1ObjectMeta controller =
        kubernetesContainerService.getControllerUsingK8sClient(KUBERNETES_CONFIG, "metadata.name=controllerName");
    assertThat(controller).isNotNull();
  }

  @Test
  @Owner(developers = PRATYUSH)
  @Category(UnitTests.class)
  public void testGetControllerWithStatefulSetController() throws ApiException {
    V1StatefulSet v1StatefulSet = new V1StatefulSet();
    v1StatefulSet.setMetadata(new V1ObjectMeta());
    V1StatefulSetList v1StatefulSetList = new V1StatefulSetList();
    v1StatefulSetList.setItems(Collections.singletonList(v1StatefulSet));
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<V1ReplicationControllerList>() {}).getType()))
        .thenThrow(new ApiException());
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<V1DeploymentList>() {}).getType()))
        .thenThrow(new ApiException());
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<V1ReplicaSetList>() {}).getType()))
        .thenThrow(new ApiException());
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<V1StatefulSetList>() {}).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), v1StatefulSetList));
    V1ObjectMeta controller =
        kubernetesContainerService.getControllerUsingK8sClient(KUBERNETES_CONFIG, "metadata.name=controllerName");
    assertThat(controller).isNotNull();
  }

  @Test
  @Owner(developers = PRATYUSH)
  @Category(UnitTests.class)
  public void testGetControllerWithDaemonSetController() throws ApiException {
    V1DaemonSet v1DaemonSet = new V1DaemonSet();
    v1DaemonSet.setMetadata(new V1ObjectMeta());
    V1DaemonSetList v1DaemonSetList = new V1DaemonSetList();
    v1DaemonSetList.setItems(Collections.singletonList(v1DaemonSet));
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<V1ReplicationControllerList>() {}).getType()))
        .thenThrow(new ApiException());
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<V1DeploymentList>() {}).getType()))
        .thenThrow(new ApiException());
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<V1ReplicaSetList>() {}).getType()))
        .thenThrow(new ApiException());
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<V1StatefulSetList>() {}).getType()))
        .thenThrow(new ApiException());
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<V1DaemonSetList>() {}).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), v1DaemonSetList));
    V1ObjectMeta controller =
        kubernetesContainerService.getControllerUsingK8sClient(KUBERNETES_CONFIG, "metadata.name=controllerName");
    assertThat(controller).isNotNull();
  }

  @Test
  @Owner(developers = PRATYUSH)
  @Category(UnitTests.class)
  public void testGetControllerWithDeploymentConfigController() throws ApiException {
    String deploymentConfigList = "{\n"
        + "  \"kind\": \"DeploymentConfigList\",\n"
        + "  \"apiVersion\": \"apps.openshift.io/v1\",\n"
        + "  \"metadata\": {},\n"
        + "  \"items\": [\n"
        + "    {\n"
        + "      \"kind\": \"DeploymentConfig\",\n"
        + "      \"apiVersion\": \"apps.openshift.io/v1\",\n"
        + "      \"metadata\": {\n"
        + "        \"name\": \"example-object\",\n"
        + "        \"namespace\": \"mynamespace\"\n"
        + "      },\n"
        + "      \"spec\": {\n"
        + "      }\n"
        + "    }\n"
        + "  ]\n"
        + "}\n";
    Gson gson = new Gson();
    Object deploymentConfigListObject = gson.fromJson(deploymentConfigList, Object.class);
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<V1ReplicationControllerList>() {}).getType()))
        .thenThrow(new ApiException());
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<V1DeploymentList>() {}).getType()))
        .thenThrow(new ApiException());
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<V1ReplicaSetList>() {}).getType()))
        .thenThrow(new ApiException());
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<V1StatefulSetList>() {}).getType()))
        .thenThrow(new ApiException());
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<V1DaemonSetList>() {}).getType()))
        .thenThrow(new ApiException());
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<Object>() {}).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), deploymentConfigListObject));
    when(k8sApiClient.getJSON()).thenReturn(new JSON());
    V1ObjectMeta controller =
        kubernetesContainerService.getControllerUsingK8sClient(KUBERNETES_CONFIG, "metadata.name=controllerName");
    assertThat(controller).isNotNull();
  }

  @Test
  @Owner(developers = PRATYUSH)
  @Category(UnitTests.class)
  public void testGetControllerWithNoController() throws ApiException {
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<V1ReplicationControllerList>() {}).getType()))
        .thenThrow(new ApiException());
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<V1DeploymentList>() {}).getType()))
        .thenThrow(new ApiException());
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<V1ReplicaSetList>() {}).getType()))
        .thenThrow(new ApiException());
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<V1StatefulSetList>() {}).getType()))
        .thenThrow(new ApiException());
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<V1DaemonSetList>() {}).getType()))
        .thenThrow(new ApiException());
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<Object>() {}).getType())).thenThrow(new ApiException());
    V1ObjectMeta controller =
        kubernetesContainerService.getControllerUsingK8sClient(KUBERNETES_CONFIG, "metadata.name=controllerName");
    assertThat(controller).isNull();
  }

  @Test
  @Owner(developers = PRATYUSH)
  @Category(UnitTests.class)
  public void testGetServiceListUsingK8sClientFail() throws ApiException {
    V1ServiceList serviceList = kubernetesContainerService.getServiceListUsingK8sClient(null, "");
    assertThat(serviceList).isNull();

    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<V1ServiceList>() {}).getType()))
        .thenThrow(new ApiException(404, "Failed to fetch resource"));
    serviceList = kubernetesContainerService.getServiceListUsingK8sClient(KUBERNETES_CONFIG, "");
    assertThat(serviceList).isNull();
  }

  @Test
  @Owner(developers = PRATYUSH)
  @Category(UnitTests.class)
  public void testGetServiceListUsingK8sClientInvalidRequestException() throws ApiException {
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<V1ServiceList>() {}).getType())).thenThrow(new ApiException());
    assertThatThrownBy(() -> kubernetesContainerService.getServiceListUsingK8sClient(KUBERNETES_CONFIG, ""))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = PRATYUSH)
  @Category(UnitTests.class)
  public void testGetServiceListUsingK8sClientPass() throws ApiException {
    V1ServiceList v1ServiceList = new V1ServiceList();
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<V1ServiceList>() {}).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), v1ServiceList));
    V1ServiceList serviceList = kubernetesContainerService.getServiceListUsingK8sClient(KUBERNETES_CONFIG, "");
    assertThat(serviceList).isEqualTo(v1ServiceList);
  }

  @Test
  @Owner(developers = PRATYUSH)
  @Category(UnitTests.class)
  public void testGetK8sServiceMetadataUsingK8sClient() throws ApiException {
    String controllerServiceName = "containerServiceName";
    String serviceName = "serviceName";
    Map<String, String> map = new HashMap<>();
    map.put("key", "value");
    map.put(HARNESS_KUBERNETES_REVISION_LABEL_KEY, "value2");
    V1ObjectMeta objectMeta = new V1ObjectMeta();
    objectMeta.setName(controllerServiceName);
    objectMeta.setLabels(map);
    V1ReplicationController v1ReplicationController = new V1ReplicationController();
    v1ReplicationController.setMetadata(objectMeta);
    V1ReplicationControllerList v1ReplicationControllerList = new V1ReplicationControllerList();
    v1ReplicationControllerList.setItems(Collections.singletonList(v1ReplicationController));
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<V1ReplicationControllerList>() {}).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), v1ReplicationControllerList));

    V1ServiceList v1ServiceList = new V1ServiceList();
    V1Service v1Service = new V1Service();
    V1ObjectMeta v1ServiceObjectMeta = new V1ObjectMeta();
    v1ServiceObjectMeta.setName(serviceName);
    v1Service.setMetadata(v1ServiceObjectMeta);
    v1ServiceList.setItems(Collections.singletonList(v1Service));
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<V1ServiceList>() {}).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), v1ServiceList));

    K8sServiceMetadata k8sServiceMetadata = kubernetesContainerService.getK8sServiceMetadataUsingK8sClient(
        KUBERNETES_CONFIG, controllerServiceName, "accId");
    assertThat(k8sServiceMetadata).isEqualTo(K8sServiceMetadata.builder().name(serviceName).labels(map).build());
  }

  @Test
  @Owner(developers = PRATYUSH)
  @Category(UnitTests.class)
  public void testGetK8sServiceMetadataUsingFabric8() {
    String controllerServiceName = "controllerServiceName";
    String serviceName = "serviceName";
    ObjectMeta objectMeta = new ObjectMeta();
    objectMeta.setName(controllerServiceName);
    replicationController.setMetadata(objectMeta);
    Map<String, String> map = ImmutableMap.of("app", "MyApp");
    PodTemplateSpec podTemplateSpec =
        new PodTemplateSpecBuilder().withNewMetadata().withLabels(map).endMetadata().build();

    spec.setTemplate(podTemplateSpec);
    replicationController.setSpec(spec);
    service.setMetadata(new ObjectMeta(
        null, null, null, null, null, null, null, null, null, serviceName, null, null, null, null, null));
    when(serviceList.getItems()).thenReturn(Collections.singletonList(service));

    K8sServiceMetadata k8sServiceMetadata =
        kubernetesContainerService.getK8sServiceMetadataUsingFabric8(KUBERNETES_CONFIG, controllerServiceName, "accId");
    assertThat(k8sServiceMetadata).isEqualTo(K8sServiceMetadata.builder().name(serviceName).labels(map).build());
  }

  @Test
  @Owner(developers = PRATYUSH)
  @Category(UnitTests.class)
  public void testGetVirtualServiceUsingK8sClient() throws ApiException {
    String controllerServiceName = "controllerServiceName";
    String virtualServiceString = "{\n"
        + "  \"kind\": \"VirtualService\",\n"
        + "  \"apiVersion\": \"networking.istio.io/v1alpha3\",\n"
        + "  \"metadata\": {},\n"
        + "  \"items\": []\n"
        + "}\n";
    Gson gson = new Gson();
    Object virtualServiceObject = gson.fromJson(virtualServiceString, Object.class);
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<Object>() {}).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), virtualServiceObject));

    Object virtualService =
        kubernetesContainerService.getVirtualServiceUsingK8sClient(KUBERNETES_CONFIG, controllerServiceName);
    assertThat(virtualService).isNotNull();
  }

  @Test
  @Owner(developers = PRATYUSH)
  @Category(UnitTests.class)
  public void testFailGetVirtualServiceUsingK8sClient() throws ApiException {
    // Null Kubeconfig
    String controllerServiceName = "controllerServiceName";
    Object virtualService = kubernetesContainerService.getVirtualServiceUsingK8sClient(null, controllerServiceName);
    assertThat(virtualService).isNull();

    // ApiException
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<Object>() {}).getType())).thenThrow(new ApiException());
    virtualService =
        kubernetesContainerService.getVirtualServiceUsingK8sClient(KUBERNETES_CONFIG, controllerServiceName);
    assertThat(virtualService).isNull();
  }

  @Test
  @Owner(developers = PRATYUSH)
  @Category(UnitTests.class)
  public void testCreateVirtualServiceUsingK8sClient() throws ApiException {
    String virtualService = "{\n"
        + "  \"kind\": \"VirtualService\",\n"
        + "  \"apiVersion\": \"networking.istio.io/v1alpha3\",\n"
        + "  \"metadata\": {\n"
        + "    \"name\": \"example-object\",\n"
        + "    \"namespace\": \"mynamespace\"\n"
        + "  },\n"
        + "  \"spec\": {\n"
        + "  }\n"
        + "}\n";
    Gson gson = new Gson();
    Object virtualServiceObject = gson.fromJson(virtualService, Object.class);
    KubernetesResource kubernetesResource = KubernetesResource.builder().value(virtualServiceObject).build();
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<Object>() {}).getType()))
        .thenThrow(new ApiException())
        .thenReturn(new ApiResponse<>(200, emptyMap(), virtualServiceObject));

    Object updatedVirtualService =
        kubernetesContainerService.createOrReplaceVirtualServiceUsingK8sClient(KUBERNETES_CONFIG, kubernetesResource);
    assertThat(updatedVirtualService).isNotNull();
  }

  @Test
  @Owner(developers = PRATYUSH)
  @Category(UnitTests.class)
  public void testReplaceVirtualServiceUsingK8sClient() throws ApiException {
    String virtualServiceList = "{\n"
        + "  \"kind\": \"VirtualServiceList\",\n"
        + "  \"apiVersion\": \"networking.istio.io/v1alpha3\",\n"
        + "  \"metadata\": {},\n"
        + "  \"items\": [\n"
        + "    {\n"
        + "      \"kind\": \"VirtualService\",\n"
        + "      \"apiVersion\": \"networking.istio.io/v1alpha3\",\n"
        + "      \"metadata\": {\n"
        + "        \"name\": \"example-object\",\n"
        + "        \"namespace\": \"mynamespace\"\n"
        + "      },\n"
        + "      \"spec\": {\n"
        + "      }\n"
        + "    }\n"
        + "  ]\n"
        + "}\n";
    String virtualService = "{\n"
        + "  \"kind\": \"VirtualService\",\n"
        + "  \"apiVersion\": \"networking.istio.io/v1alpha3\",\n"
        + "  \"metadata\": {\n"
        + "    \"name\": \"example-object\",\n"
        + "    \"namespace\": \"mynamespace\"\n"
        + "  },\n"
        + "  \"spec\": {\n"
        + "  }\n"
        + "}\n";
    Gson gson = new Gson();
    Object virtualServiceListObject = gson.fromJson(virtualServiceList, Object.class);
    Object virtualServiceObject = gson.fromJson(virtualService, Object.class);
    KubernetesResource kubernetesResource = KubernetesResource.builder().value(virtualServiceObject).build();
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<Object>() {}).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), virtualServiceListObject));
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<Object>() {}).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), virtualServiceObject));

    Object updatedVirtualService =
        kubernetesContainerService.createOrReplaceVirtualServiceUsingK8sClient(KUBERNETES_CONFIG, kubernetesResource);
    assertThat(updatedVirtualService).isNotNull();
  }

  @Test
  @Owner(developers = PRATYUSH)
  @Category(UnitTests.class)
  public void testFailCreateVirtualServiceUsingK8sClient() throws ApiException {
    String virtualService = "{\n"
        + "  \"kind\": \"VirtualService\",\n"
        + "  \"apiVersion\": \"networking.istio.io/v1alpha3\",\n"
        + "  \"metadata\": {\n"
        + "    \"name\": \"example-object\",\n"
        + "    \"namespace\": \"mynamespace\"\n"
        + "  },\n"
        + "  \"spec\": {\n"
        + "  }\n"
        + "}\n";
    Gson gson = new Gson();
    Object virtualServiceObject = gson.fromJson(virtualService, Object.class);
    KubernetesResource kubernetesResource = KubernetesResource.builder().value(virtualServiceObject).build();
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<Object>() {}).getType()))
        .thenThrow(new ApiException())
        .thenThrow(new ApiException());

    assertThatThrownBy(()
                           -> kubernetesContainerService.createOrReplaceVirtualServiceUsingK8sClient(
                               KUBERNETES_CONFIG, kubernetesResource))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Unable to create default/VirtualService/example-object Code: 0, message: ");
  }

  @Test
  @Owner(developers = PRATYUSH)
  @Category(UnitTests.class)
  public void testFailReplaceVirtualServiceUsingK8sClient() throws ApiException {
    String virtualServiceList = "{\n"
        + "  \"kind\": \"VirtualServiceList\",\n"
        + "  \"apiVersion\": \"networking.istio.io/v1alpha3\",\n"
        + "  \"metadata\": {},\n"
        + "  \"items\": [\n"
        + "    {\n"
        + "      \"kind\": \"VirtualService\",\n"
        + "      \"apiVersion\": \"networking.istio.io/v1alpha3\",\n"
        + "      \"metadata\": {\n"
        + "        \"name\": \"example-object\",\n"
        + "        \"namespace\": \"mynamespace\"\n"
        + "      },\n"
        + "      \"spec\": {\n"
        + "      }\n"
        + "    }\n"
        + "  ]\n"
        + "}\n";
    String virtualService = "{\n"
        + "  \"kind\": \"VirtualService\",\n"
        + "  \"apiVersion\": \"networking.istio.io/v1alpha3\",\n"
        + "  \"metadata\": {\n"
        + "    \"name\": \"example-object\",\n"
        + "    \"namespace\": \"mynamespace\"\n"
        + "  },\n"
        + "  \"spec\": {\n"
        + "  }\n"
        + "}\n";
    Gson gson = new Gson();
    Object virtualServiceListObject = gson.fromJson(virtualServiceList, Object.class);
    Object virtualServiceObject = gson.fromJson(virtualService, Object.class);
    KubernetesResource kubernetesResource = KubernetesResource.builder().value(virtualServiceObject).build();
    when(k8sApiClient.execute(k8sApiCall, (new TypeToken<Object>() {}).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), virtualServiceListObject))
        .thenThrow(new ApiException());

    assertThatThrownBy(()
                           -> kubernetesContainerService.createOrReplaceVirtualServiceUsingK8sClient(
                               KUBERNETES_CONFIG, kubernetesResource))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Unable to replace default/VirtualService/example-object Code: 0, message: ");
  }

  @Test
  @Owner(developers = PRATYUSH)
  @Category(UnitTests.class)
  public void testFailReplaceOrCreateVirtualServiceUsingK8sClient() {
    String virtualServiceList = "{\n"
        + "  \"kind\": \"VirtualServiceList\",\n"
        + "  \"apiVersion\": \"networking.istio.io\",\n"
        + "  \"metadata\": {},\n"
        + "  \"items\": []\n"
        + "}\n";
    Gson gson = new Gson();
    Object virtualServiceListObject = gson.fromJson(virtualServiceList, Object.class);
    assertThatThrownBy(()
                           -> kubernetesContainerService.createOrReplaceVirtualServiceUsingK8sClient(
                               KUBERNETES_CONFIG, KubernetesResource.builder().value(virtualServiceListObject).build()))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Api version for virtual service is invalid. ApiVersion: networking.istio.io");

    KubernetesResource kubernetesResource = kubernetesContainerService.createOrReplaceVirtualServiceUsingK8sClient(
        null, KubernetesResource.builder().value(virtualServiceListObject).build());
    assertThat(kubernetesResource).isNull();
  }

  private static final String EXPECTED_KUBECONFIG = "apiVersion: v1\n"
      + "clusters:\n"
      + "- cluster:\n"
      + "    server: myMasterUrl\n"
      + "    certificate-authority-data: myCaCert\n"
      + "  name: CLUSTER_NAME\n"
      + "contexts:\n"
      + "- context:\n"
      + "    cluster: CLUSTER_NAME\n"
      + "    user: HARNESS_USER\n"
      + "    namespace: myNamespace\n"
      + "  name: CURRENT_CONTEXT\n"
      + "current-context: CURRENT_CONTEXT\n"
      + "kind: Config\n"
      + "preferences: {}\n"
      + "users:\n"
      + "- name: HARNESS_USER\n"
      + "  user:\n"
      + "    auth-provider:\n"
      + "      name: gcp\n";
}
