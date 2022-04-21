package io.harness.pms.sdk.core.plan.creation.mappers;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.contracts.steps.SkipType;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.PmsSdkCoreTestBase;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class PlanNodeProtoMapperTest extends PmsSdkCoreTestBase {
  @Mock KryoSerializer kryoSerializer;
  @InjectMocks @Inject PlanNodeProtoMapper planNodeProtoMapper;

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testToPlanNodeProtoWithDecoratedFields() {
    PlanNode planNode = PlanNode.builder()
                            .name("name")
                            .stageFqn("fqn")
                            .uuid("uuid")
                            .stepType(StepType.newBuilder().getDefaultInstanceForType())
                            .identifier("identifier")
                            .skipExpressionChain(false)
                            .skipGraphType(SkipType.SKIP_NODE)
                            .skipUnresolvedExpressionsCheck(true)
                            .whenCondition("when")
                            .skipCondition("skip")
                            .group("group")
                            .build();
    PlanNodeProto response = PlanNodeProto.newBuilder()
                                 .setName("name")
                                 .setStageFqn("fqn")
                                 .setUuid("uuid")
                                 .setServiceName(PMS_SDK_CORE_SERVICE_NAME)
                                 .setStepType(StepType.newBuilder().getDefaultInstanceForType())
                                 .setIdentifier("identifier")
                                 .setSkipExpressionChain(false)
                                 .setSkipType(SkipType.SKIP_NODE)
                                 .setSkipUnresolvedExpressionsCheck(true)
                                 .setWhenCondition("when")
                                 .setSkipCondition("skip")
                                 .setGroup("group")
                                 .build();
    assertThat(planNodeProtoMapper.toPlanNodeProtoWithDecoratedFields(planNode)).isEqualTo(response);
  }
}