package software.wings.sm.states;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.api.HostElement.Builder.aHostElement;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.api.ServiceElement.Builder.aServiceElement;
import static software.wings.api.ServiceTemplateElement.Builder.aServiceTemplateElement;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.common.Constants.BUILD_NO;
import static software.wings.sm.ContextElement.ARTIFACT;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.states.HttpState.Builder.aHttpState;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.DELEGATE_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_INSTANCE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.STATE_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Injector;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.harness.beans.ExecutionStatus;
import io.harness.context.ContextElementType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.HttpStateExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Type;
import software.wings.beans.DelegateTask;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.TaskType;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Class HttpStateTest.
 *
 * @author Rishi
 */
public class HttpStateTest extends WingsBaseTest {
  private static final HttpState.Builder httpStateBuilder =
      aHttpState()
          .withName("healthCheck1")
          .withMethod("GET")
          .withUrl("http://${host.hostName}:8088/health/status")
          .withHeader("Content-Type: application/xml, Accept: */*")
          .withAssertion(
              "(${httpResponseCode}==200 || ${httpResponseCode}==201) && ${xmlFormat()} && ${xpath('//health/status/text()')}.equals('Enabled')");

  private static final Activity activity = Activity.builder()
                                               .applicationName(APP_NAME)
                                               .environmentId(ENV_ID)
                                               .environmentName(ENV_NAME)
                                               .environmentType(EnvironmentType.NON_PROD)
                                               .commandName("healthCheck1")
                                               .type(Type.Verification)
                                               .stateExecutionInstanceId(STATE_EXECUTION_ID)
                                               .stateExecutionInstanceName("healthCheck1")
                                               .commandType(StateType.HTTP.name())
                                               .status(ExecutionStatus.RUNNING)
                                               .commandUnits(Collections.emptyList())
                                               .build();

  static {
    activity.setUuid(ACTIVITY_ID);
    activity.setAppId(APP_ID);
    activity.setValidUntil(null);
  }
  /**
   * The Wire mock rule.
   */
  @Rule public WireMockRule wireMockRule = new WireMockRule(8088);

  @Mock private WorkflowStandardParams workflowStandardParams;
  @Mock private ActivityService activityService;
  @Inject private Injector injector;
  @Mock private DelegateService delegateService;

  private ExecutionResponse asyncExecutionResponse;

  private ExecutionContextImpl context;

  /**
   * Sets context.
   */
  @Before
  public void setupContext() {
    StateExecutionInstance stateExecutionInstance =
        aStateExecutionInstance().withDisplayName("healthCheck1").withUuid(STATE_EXECUTION_ID).build();

    Map<String, StateExecutionData> stateExecutionMap = new HashMap<>();
    stateExecutionMap.put("healthCheck1", HttpStateExecutionData.builder().build());
    stateExecutionInstance.setStateExecutionMap(stateExecutionMap);

    when(workflowStandardParams.getApp()).thenReturn(anApplication().withUuid(APP_ID).withName(APP_NAME).build());
    when(workflowStandardParams.getEnv())
        .thenReturn(
            anEnvironment().withUuid(ENV_ID).withName(ENV_NAME).withEnvironmentType(EnvironmentType.NON_PROD).build());
    when(workflowStandardParams.getAppId()).thenReturn(APP_ID);
    when(workflowStandardParams.getEnvId()).thenReturn(ENV_ID);

    when(workflowStandardParams.getElementType()).thenReturn(ContextElementType.STANDARD);
    context = new ExecutionContextImpl(stateExecutionInstance, null, injector);
    context.pushContextElement(workflowStandardParams);
    context.pushContextElement(aHostElement().withHostName("localhost").build());

    when(activityService.save(any(Activity.class))).thenAnswer(invocation -> {
      Activity activity = invocation.getArgumentAt(0, Activity.class);
      activity.setUuid(ACTIVITY_ID);
      activity.setValidUntil(null);
      return activity;
    });
  }

  @Test
  public void shouldGetPatternsForRequiredContextElementType() {
    List<String> patternsForRequiredContextElementType =
        getHttpState(httpStateBuilder, context).getPatternsForRequiredContextElementType();
    assertThat(patternsForRequiredContextElementType).isNotEmpty();
    assertThat(patternsForRequiredContextElementType)
        .contains("http://${host.hostName}:8088/health/status", "Content-Type: application/xml, Accept: */*",
            "(${httpResponseCode}==200 || ${httpResponseCode}==201) && ${xmlFormat()} && ${xpath('//health/status/text()')}.equals('Enabled')");
  }

  @Test
  public void shouldGetTemplatedHttpPatternsForRequiredContextElementType() {
    HttpState.Builder jsonHttpStateBuilder =
        aHttpState()
            .withName("healthCheck1")
            .withMethod("GET")
            .withUrl("http://${url}")
            .withHeader("Content-Type: ${contentType}, Accept: */*")
            .withAssertion("${httpResponseCode}==200 && ${jsonpath(\"data.version\")}==${buildNo}")
            .withTemplateVariables(asList(aVariable().withName("url").withValue("localhost:8088/health/status").build(),
                aVariable().withName("buildNo").withValue("2.31.0-MASTER-SNAPSHOT").build(),
                aVariable().withName("contentType").withValue("application/json").build()));
    List<String> patternsForRequiredContextElementType =
        getHttpState(httpStateBuilder, context).getPatternsForRequiredContextElementType();
    assertThat(patternsForRequiredContextElementType).isNotEmpty();
    assertThat(patternsForRequiredContextElementType)
        .contains("http://${host.hostName}:8088/health/status", "Content-Type: application/xml, Accept: */*",
            "(${httpResponseCode}==200 || ${httpResponseCode}==201) && ${xmlFormat()} && ${xpath('//health/status/text()')}.equals('Enabled')");
  }

  /**
   * Should execute and evaluate JSON response.
   */
  @Test
  public void shouldExecuteAndEvaluateJsonResponse() {
    wireMockRule.stubFor(
        get(urlEqualTo("/health/status"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Accept", equalTo("*/*"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/json")
                    .withBody(
                        "{\"status\":{\"code\":\"SUCCESS\"},\"data\":{\"title\":\"Some server\",\"version\":\"2.31.0-MASTER-SNAPSHOT\",\"buildTimestamp\":1506086747259}}")));

    Map<String, Object> map = ImmutableMap.of(
        ARTIFACT, anArtifact().withMetadata(ImmutableMap.of(BUILD_NO, "2.31.0-MASTER-SNAPSHOT")).build());
    when(workflowStandardParams.paramMap(context)).thenReturn(map);

    HttpState.Builder jsonHttpStateBuilder =
        aHttpState()
            .withName("healthCheck1")
            .withMethod("GET")
            .withUrl("http://${host.hostName}:8088/health/status")
            .withHeader("Content-Type: application/json, Accept: */*")
            .withAssertion("${httpResponseCode}==200 && ${jsonpath(\"data.version\")}==${artifact.buildNo}");

    ExecutionResponse response = getHttpState(jsonHttpStateBuilder.but(), context).execute(context);

    assertThat(response).isNotNull().extracting(ExecutionResponse::isAsync).containsExactly(true);

    response = asyncExecutionResponse;

    assertThat(response.getStateExecutionData())
        .isNotNull()
        .isInstanceOf(HttpStateExecutionData.class)
        .isEqualToComparingOnlyGivenFields(
            HttpStateExecutionData.builder()
                .httpUrl("http://localhost:8088/health/status")
                .assertionStatus("SUCCESS")
                .httpResponseCode(200)
                .httpResponseBody(
                    "{\"status\":{\"code\":\"SUCCESS\"},\"data\":{\"title\":\"Some server\",\"version\":\"2.31.0-MASTER-SNAPSHOT\",\"buildTimestamp\":1506086747259}}")
                .build(),
            "httpUrl", "assertionStatus", "httpResponseCode", "httpResponseBody");

    verify(activityService).save(activity);
    verify(activityService).updateStatus(ACTIVITY_ID, APP_ID, ExecutionStatus.SUCCESS);
  }

  @Test
  public void shouldExecuteAndEvaluateJsonResponseWithVariables() {
    wireMockRule.stubFor(
        get(urlEqualTo("/health/status"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Accept", equalTo("*/*"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/json")
                    .withBody(
                        "{\"status\":{\"code\":\"SUCCESS\"},\"data\":{\"title\":\"Some server\",\"version\":\"2.31.0-MASTER-SNAPSHOT\",\"buildTimestamp\":1506086747259}}")));

    Map<String, Object> map = ImmutableMap.of(
        ARTIFACT, anArtifact().withMetadata(ImmutableMap.of(BUILD_NO, "2.31.0-MASTER-SNAPSHOT")).build());
    when(workflowStandardParams.paramMap(context)).thenReturn(map);

    HttpState.Builder jsonHttpStateBuilder =
        aHttpState()
            .withName("healthCheck1")
            .withMethod("GET")
            .withUrl("http://${url}")
            .withHeader("Content-Type: ${contentType}, Accept: */*")
            .withAssertion("${httpResponseCode}==200 && ${jsonpath(\"data.version\")}==${buildNo}")
            .withTemplateVariables(asList(aVariable().withName("url").withValue("localhost:8088/health/status").build(),
                aVariable().withName("buildNo").withValue("2.31.0-MASTER-SNAPSHOT").build(),
                aVariable().withName("contentType").withValue("application/json").build()));

    ExecutionResponse response = getHttpState(jsonHttpStateBuilder.but(), context).execute(context);

    assertThat(response).isNotNull().extracting(ExecutionResponse::isAsync).containsExactly(true);

    response = asyncExecutionResponse;

    assertThat(response.getStateExecutionData())
        .isNotNull()
        .isInstanceOf(HttpStateExecutionData.class)
        .isEqualToComparingOnlyGivenFields(
            HttpStateExecutionData.builder()
                .httpUrl("http://localhost:8088/health/status")
                .assertionStatus("SUCCESS")
                .httpResponseCode(200)
                .httpResponseBody(
                    "{\"status\":{\"code\":\"SUCCESS\"},\"data\":{\"title\":\"Some server\",\"version\":\"2.31.0-MASTER-SNAPSHOT\",\"buildTimestamp\":1506086747259}}")
                .build(),
            "httpUrl", "assertionStatus", "httpResponseCode", "httpResponseBody");

    verify(activityService).save(activity);
    verify(activityService).updateStatus(ACTIVITY_ID, APP_ID, ExecutionStatus.SUCCESS);
  }
  /**
   * Should execute and evaluate response.
   */
  @Test
  public void shouldExecuteAndEvaluateResponse() {
    wireMockRule.stubFor(get(urlEqualTo("/health/status"))
                             .withHeader("Content-Type", equalTo("application/xml"))
                             .withHeader("Accept", equalTo("*/*"))
                             .willReturn(aResponse()
                                             .withStatus(200)
                                             .withBody("<health><status>Enabled</status></health>")
                                             .withHeader("Content-Type", "text/xml")));

    ExecutionResponse response = getHttpState(httpStateBuilder.but(), context).execute(context);

    assertThat(response).isNotNull().extracting(ExecutionResponse::isAsync).containsExactly(true);

    response = asyncExecutionResponse;

    assertThat(response.getStateExecutionData())
        .isNotNull()
        .isInstanceOf(HttpStateExecutionData.class)
        .isEqualToComparingOnlyGivenFields(HttpStateExecutionData.builder()
                                               .httpUrl("http://localhost:8088/health/status")
                                               .assertionStatus("SUCCESS")
                                               .httpResponseCode(200)
                                               .httpResponseBody("<health><status>Enabled</status></health>")
                                               .build(),
            "httpUrl", "assertionStatus", "httpResponseCode", "httpResponseBody");

    verify(activityService).save(activity);
    verify(activityService).updateStatus(ACTIVITY_ID, APP_ID, ExecutionStatus.SUCCESS);
  }

  /**
   * Should execute and evaluate response.
   */
  @Test
  public void shouldExecuteAndEvaluateResponseWithVariables() {
    wireMockRule.stubFor(get(urlEqualTo("/health/status"))
                             .withHeader("Content-Type", equalTo("application/xml"))
                             .withHeader("Accept", equalTo("*/*"))
                             .willReturn(aResponse()
                                             .withStatus(200)
                                             .withBody("<health><status>Enabled</status></health>")
                                             .withHeader("Content-Type", "text/xml")));

    httpStateBuilder.withTemplateVariables(asList(aVariable().withName("status").withValue("Enabled").build()));
    ExecutionResponse response = getHttpState(httpStateBuilder.but(), context).execute(context);

    assertThat(response).isNotNull().extracting(ExecutionResponse::isAsync).containsExactly(true);

    response = asyncExecutionResponse;

    assertThat(response.getStateExecutionData())
        .isNotNull()
        .isInstanceOf(HttpStateExecutionData.class)
        .isEqualToComparingOnlyGivenFields(HttpStateExecutionData.builder()
                                               .httpUrl("http://localhost:8088/health/status")
                                               .assertionStatus("SUCCESS")
                                               .httpResponseCode(200)
                                               .httpResponseBody("<health><status>Enabled</status></health>")
                                               .build(),
            "httpUrl", "assertionStatus", "httpResponseCode", "httpResponseBody");

    verify(activityService).save(activity);
    verify(activityService).updateStatus(ACTIVITY_ID, APP_ID, ExecutionStatus.SUCCESS);
  }

  /**
   * Should execute and evaluate response.
   */
  @Test
  public void shouldExecuteAndEvaluateResponseWithInstance() {
    wireMockRule.stubFor(get(urlEqualTo("/health/status"))
                             .withHeader("Content-Type", equalTo("application/xml"))
                             .withHeader("Accept", equalTo("*/*"))
                             .willReturn(aResponse()
                                             .withStatus(200)
                                             .withBody("<health><status>Enabled</status></health>")
                                             .withHeader("Content-Type", "text/xml")));

    context.pushContextElement(
        anInstanceElement()
            .withHost(aHostElement().withHostName("localhost").build())
            .withServiceTemplateElement(
                aServiceTemplateElement()
                    .withName(TEMPLATE_NAME)
                    .withUuid(TEMPLATE_ID)
                    .withServiceElement(aServiceElement().withName(SERVICE_NAME).withUuid(SERVICE_ID).build())
                    .build())
            .withUuid(SERVICE_INSTANCE_ID)
            .build());

    ExecutionResponse response = getHttpState(httpStateBuilder.but(), context).execute(context);

    assertThat(response).isNotNull().extracting(ExecutionResponse::isAsync).containsExactly(true);

    response = asyncExecutionResponse;
    assertThat(response).isNotNull().extracting(ExecutionResponse::isAsync).containsExactly(false);
    assertThat(response.getStateExecutionData())
        .isNotNull()
        .isInstanceOf(HttpStateExecutionData.class)
        .isEqualToComparingOnlyGivenFields(HttpStateExecutionData.builder()
                                               .httpUrl("http://localhost:8088/health/status")
                                               .assertionStatus("SUCCESS")
                                               .httpResponseCode(200)
                                               .httpResponseBody("<health><status>Enabled</status></health>")
                                               .build(),
            "httpUrl", "assertionStatus", "httpResponseCode", "httpResponseBody");

    Activity act = Activity.builder()
                       .applicationName(APP_NAME)
                       .environmentId(ENV_ID)
                       .environmentName(ENV_NAME)
                       .environmentType(EnvironmentType.NON_PROD)
                       .commandName("healthCheck1")
                       .type(Type.Verification)
                       .stateExecutionInstanceId(STATE_EXECUTION_ID)
                       .stateExecutionInstanceName("healthCheck1")
                       .commandType(StateType.HTTP.name())
                       .hostName("localhost")
                       .serviceId(SERVICE_ID)
                       .serviceName(SERVICE_NAME)
                       .serviceInstanceId(SERVICE_INSTANCE_ID)
                       .serviceTemplateId(TEMPLATE_ID)
                       .serviceTemplateName(TEMPLATE_NAME)
                       .status(ExecutionStatus.RUNNING)
                       .commandUnits(Collections.emptyList())
                       .build();

    act.setUuid(ACTIVITY_ID);
    act.setAppId(APP_ID);
    act.setValidUntil(null);

    verify(activityService).save(act);
    verify(activityService).updateStatus(ACTIVITY_ID, APP_ID, ExecutionStatus.SUCCESS);
  }

  /**
   * Should execute and get summary/details.
   */
  @Test
  public void shouldGetExecutionDataSummaryDetails() {
    wireMockRule.stubFor(get(urlEqualTo("/health/status"))
                             .withHeader("Content-Type", equalTo("application/xml"))
                             .withHeader("Accept", equalTo("*/*"))
                             .willReturn(aResponse()
                                             .withStatus(200)
                                             .withBody("<health><status>Enabled</status></health>")
                                             .withHeader("Content-Type", "text/xml")));

    ExecutionResponse response = getHttpState(httpStateBuilder.but(), context).execute(context);

    assertThat(response).isNotNull().extracting(ExecutionResponse::isAsync).containsExactly(true);

    response = asyncExecutionResponse;
    assertThat(response.getStateExecutionData()).isNotNull().isInstanceOf(HttpStateExecutionData.class);
    response.getStateExecutionData().setStatus(ExecutionStatus.SUCCESS);
    assertThat(response.getStateExecutionData().getExecutionSummary()).isNotNull();
    assertThat(response.getStateExecutionData().getExecutionDetails()).isNotNull();

    verify(activityService).save(activity);
    verify(activityService).updateStatus(ACTIVITY_ID, APP_ID, ExecutionStatus.SUCCESS);
  }

  /**
   * Should fail on socket timeout.
   */
  @Test
  public void shouldFailOnSocketTimeout() {
    wireMockRule.stubFor(get(urlEqualTo("/health/status"))
                             .withHeader("Content-Type", equalTo("application/xml"))
                             .withHeader("Accept", equalTo("*/*"))
                             .willReturn(aResponse()
                                             .withStatus(200)
                                             .withBody("<health><status>Enabled</status></health>")
                                             .withHeader("Content-Type", "text/xml")
                                             .withFixedDelay(2000)));

    ExecutionResponse response =
        getHttpState(httpStateBuilder.but().withSocketTimeoutMillis(1000), context).execute(context);
    assertThat(response).isNotNull().extracting(ExecutionResponse::isAsync).containsExactly(true);

    response = asyncExecutionResponse;
    assertThat(response.getStateExecutionData())
        .isNotNull()
        .isInstanceOf(HttpStateExecutionData.class)
        .isEqualToComparingOnlyGivenFields(HttpStateExecutionData.builder()
                                               .httpUrl("http://localhost:8088/health/status")
                                               .assertionStatus("FAILED")
                                               .httpResponseCode(500)
                                               .httpResponseBody("SocketTimeoutException: Read timed out")
                                               .build(),
            "httpUrl", "assertionStatus", "httpResponseCode", "httpResponseBody");

    verify(activityService).save(activity);
    verify(activityService).updateStatus(ACTIVITY_ID, APP_ID, ExecutionStatus.FAILED);
  }

  /**
   * Should fail on empty response.
   */
  @Test
  public void shouldFailOnEmptyResponse() {
    wireMockRule.stubFor(get(urlEqualTo("/health/status"))
                             .withHeader("Content-Type", equalTo("application/xml"))
                             .withHeader("Accept", equalTo("*/*"))
                             .willReturn(aResponse().withStatus(200).withFault(Fault.EMPTY_RESPONSE)));

    ExecutionResponse response = getHttpState(httpStateBuilder.but(), context).execute(context);
    assertThat(response).isNotNull().extracting(ExecutionResponse::isAsync).containsExactly(true);

    response = asyncExecutionResponse;
    assertThat(response.getStateExecutionData())
        .isNotNull()
        .isInstanceOf(HttpStateExecutionData.class)
        .isEqualToComparingOnlyGivenFields(
            HttpStateExecutionData.builder()
                .httpUrl("http://localhost:8088/health/status")
                .assertionStatus("FAILED")
                .httpResponseCode(500)
                .httpResponseBody("NoHttpResponseException: localhost:8088 failed to respond")
                .build(),
            "httpUrl", "assertionStatus", "httpResponseCode", "httpResponseBody");
    verify(activityService).save(activity);
    verify(activityService).updateStatus(ACTIVITY_ID, APP_ID, ExecutionStatus.FAILED);
  }

  /**
   * Should fail on malformed response.
   */
  @Test
  public void shouldFailOnMalformedResponse() {
    wireMockRule.stubFor(get(urlEqualTo("/health/status"))
                             .withHeader("Content-Type", equalTo("application/xml"))
                             .withHeader("Accept", equalTo("*/*"))
                             .willReturn(aResponse().withStatus(200).withFault(Fault.MALFORMED_RESPONSE_CHUNK)));

    ExecutionResponse response = getHttpState(httpStateBuilder.but(), context).execute(context);
    assertThat(response).isNotNull().extracting(ExecutionResponse::isAsync).containsExactly(true);

    response = asyncExecutionResponse;
    assertThat(response.getStateExecutionData())
        .isNotNull()
        .isInstanceOf(HttpStateExecutionData.class)
        .isEqualToComparingOnlyGivenFields(HttpStateExecutionData.builder()
                                               .httpUrl("http://localhost:8088/health/status")
                                               .assertionStatus("FAILED")
                                               .httpResponseCode(500)
                                               .httpResponseBody("MalformedChunkCodingException: Bad chunk header")
                                               .build(),
            "httpUrl", "assertionStatus", "httpResponseCode", "httpResponseBody");
    verify(activityService).save(activity);
    verify(activityService).updateStatus(ACTIVITY_ID, APP_ID, ExecutionStatus.FAILED);
  }

  /**
   * Should fail on random data.
   */
  @Test
  public void shouldFailOnRandomData() {
    wireMockRule.stubFor(get(urlEqualTo("/health/status"))
                             .withHeader("Content-Type", equalTo("application/xml"))
                             .withHeader("Accept", equalTo("*/*"))
                             .willReturn(aResponse().withStatus(200).withFault(Fault.RANDOM_DATA_THEN_CLOSE)));

    ExecutionResponse response = getHttpState(httpStateBuilder.but(), context).execute(context);
    assertThat(response).isNotNull().extracting(ExecutionResponse::isAsync).containsExactly(true);

    response = asyncExecutionResponse;
    assertThat(response.getStateExecutionData())
        .isNotNull()
        .isInstanceOf(HttpStateExecutionData.class)
        .isEqualToComparingOnlyGivenFields(HttpStateExecutionData.builder()
                                               .httpUrl("http://localhost:8088/health/status")
                                               .assertionStatus("FAILED")
                                               .httpResponseCode(500)
                                               .httpResponseBody("ClientProtocolException: ")
                                               .build(),
            "httpUrl", "assertionStatus", "httpResponseCode", "httpResponseBody");
    verify(activityService).save(activity);
    verify(activityService).updateStatus(ACTIVITY_ID, APP_ID, ExecutionStatus.FAILED);
  }

  /**
   * Should fail on connect timeout.
   */
  @Test
  public void shouldFailOnConnectTimeout() {
    context.pushContextElement(aHostElement().withHostName("www.google.com").build());

    ExecutionResponse response =
        getHttpState(httpStateBuilder.but().withUrl("http://${host.hostName}:81/health/status"), context)
            .execute(context);
    assertThat(response).isNotNull().extracting(ExecutionResponse::isAsync).containsExactly(true);

    response = asyncExecutionResponse;
    assertThat(response.getStateExecutionData())
        .isNotNull()
        .isInstanceOf(HttpStateExecutionData.class)
        .isEqualToComparingOnlyGivenFields(HttpStateExecutionData.builder()
                                               .httpUrl("http://www.google.com:81/health/status")
                                               .assertionStatus("FAILED")
                                               .httpResponseCode(500)
                                               .build(),
            "httpUrl", "assertionStatus", "httpResponseCode");
    assertThat(((HttpStateExecutionData) response.getStateExecutionData()).getHttpResponseBody())
        .contains("Connect to www.google.com:81 ");
    verify(activityService).save(activity);
    verify(activityService).updateStatus(ACTIVITY_ID, APP_ID, ExecutionStatus.FAILED);
  }

  private HttpState getHttpState(HttpState.Builder builder, ExecutionContext context) {
    HttpState httpState = builder.build();
    on(httpState).set("activityService", activityService);
    on(httpState).set("delegateService", delegateService);

    doAnswer(invocation -> {
      DelegateTask task = invocation.getArgumentAt(0, DelegateTask.class);
      TaskType.valueOf(task.getTaskType())
          .getDelegateRunnableTask(DELEGATE_ID, task,
              o
              -> asyncExecutionResponse =
                     httpState.handleAsyncResponse(context, ImmutableMap.of(task.getWaitId(), o.getResponse())),
              () -> true)
          .run();
      return null;
    })
        .when(delegateService)
        .queueTask(any(DelegateTask.class));

    return httpState;
  }
}
