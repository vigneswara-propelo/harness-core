package io.harness.pms.sdk.core.resolver.expressions;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.service.EngineExpressionProtoServiceGrpc.EngineExpressionProtoServiceBlockingStub;
import io.harness.pms.contracts.service.ExpressionEvaluateBlobRequest;
import io.harness.pms.contracts.service.ExpressionEvaluateBlobResponse;
import io.harness.pms.contracts.service.ExpressionRenderBlobRequest;
import io.harness.pms.contracts.service.ExpressionRenderBlobResponse;
import io.harness.pms.sdk.core.AmbianceTestUtils;
import io.harness.pms.sdk.core.PmsSdkCoreTestBase;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(EngineExpressionProtoServiceBlockingStub.class)
public class EngineGrpcExpressionServiceTest extends PmsSdkCoreTestBase {
  EngineExpressionProtoServiceBlockingStub engineExpressionProtoServiceBlockingStub;
  EngineGrpcExpressionService engineGrpcExpressionService;

  @Before
  public void beforeTest() {
    engineExpressionProtoServiceBlockingStub = PowerMockito.mock(EngineExpressionProtoServiceBlockingStub.class);
    engineGrpcExpressionService = new EngineGrpcExpressionService(engineExpressionProtoServiceBlockingStub);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testRenderExpression() {
    Ambiance ambiance = AmbianceTestUtils.buildAmbiance();
    String expression = "test";
    ExpressionRenderBlobResponse expressionRenderBlobResponse =
        ExpressionRenderBlobResponse.newBuilder().setValue("test").build();
    Mockito
        .when(engineExpressionProtoServiceBlockingStub.renderExpression(ExpressionRenderBlobRequest.newBuilder()
                                                                            .setAmbiance(ambiance)
                                                                            .setExpression(expression)
                                                                            .setSkipUnresolvedExpressionsCheck(false)
                                                                            .build()))
        .thenReturn(expressionRenderBlobResponse);
    assertThat(engineGrpcExpressionService.renderExpression(ambiance, expression, false)).isEqualTo("test");
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testEvaluateExpression() {
    Ambiance ambiance = AmbianceTestUtils.buildAmbiance();
    String expression = "{'test':'test'}";
    ExpressionEvaluateBlobResponse expressionRenderBlobResponse = ExpressionEvaluateBlobResponse.newBuilder().build();
    Mockito
        .when(engineExpressionProtoServiceBlockingStub.evaluateExpression(
            ExpressionEvaluateBlobRequest.newBuilder().setAmbiance(ambiance).setExpression(expression).build()))
        .thenReturn(expressionRenderBlobResponse);
    assertThat(engineGrpcExpressionService.evaluateExpression(ambiance, expression))
        .isEqualTo(RecastOrchestrationUtils.fromJson(null, Object.class));
  }
}