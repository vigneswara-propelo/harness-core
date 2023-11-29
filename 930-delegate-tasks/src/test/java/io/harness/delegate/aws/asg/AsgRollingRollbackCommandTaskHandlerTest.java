/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.aws.asg;

import static io.harness.rule.OwnerRule.LOVISH_BANSAL;
import static io.harness.rule.OwnerRule.VITALIE;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.aws.asg.AsgContentParser;
import io.harness.aws.asg.AsgSdkManager;
import io.harness.aws.beans.AwsInternalConfig;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.aws.asg.AsgInfraConfig;
import io.harness.delegate.task.aws.asg.AsgRollingRollbackRequest;
import io.harness.delegate.task.aws.asg.AsgRollingRollbackResponse;
import io.harness.delegate.task.aws.asg.AsgRollingRollbackResult;
import io.harness.delegate.task.aws.asg.AsgTaskHelper;
import io.harness.delegate.task.aws.asg.AutoScalingGroupContainer;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;

import software.wings.service.impl.AwsUtils;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.LaunchTemplateSpecification;
import com.amazonaws.services.autoscaling.model.StartInstanceRefreshResult;
import com.amazonaws.services.ec2.model.LaunchTemplate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class AsgRollingRollbackCommandTaskHandlerTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  static final String ASG_NAME = "testAsg";
  static final String LAUNCH_TEMPLATE_CONTENT = "{\"LaunchTemplateData\": { \"InstanceType\": \"t2.micro\" }}";
  static final String CONFIG_CONTENT = format("{\"autoScalingGroupName\": \"%s\"}", ASG_NAME);

  @Mock AsgTaskHelper asgTaskHelper;
  @Mock AwsUtils awsUtils;

  @Mock ILogStreamingTaskClient iLogStreamingTaskClient;
  @Mock LogCallback rollingRollbackLogCallback;
  @Mock AsgSdkManager asgSdkManager;

  @Spy @InjectMocks private AsgRollingRollbackCommandTaskHandler taskHandler;

  CommandUnitsProgress commandUnitsProgress = CommandUnitsProgress.builder().build();

  AsgInfraConfig asgInfraConfig =
      AsgInfraConfig.builder().region("us-east-1").awsConnectorDTO(AwsConnectorDTO.builder().build()).build();

  @Before
  public void setup() {
    doReturn(rollingRollbackLogCallback).when(asgTaskHelper).getLogCallback(any(), anyString(), anyBoolean(), any());
    doReturn(asgSdkManager).when(asgTaskHelper).getAsgSdkManager(any(), any(), any());
    doReturn(AwsInternalConfig.builder().build()).when(awsUtils).getAwsInternalConfig(any(), anyString());
    doReturn(LAUNCH_TEMPLATE_CONTENT).when(asgTaskHelper).getAsgLaunchTemplateContent(any());
    doReturn(new LaunchTemplate().withLatestVersionNumber(1L))
        .when(asgSdkManager)
        .createLaunchTemplate(anyString(), any());
    doReturn(new AutoScalingGroup().withAutoScalingGroupName(ASG_NAME)).when(asgSdkManager).getASG(anyString());
    doReturn(AutoScalingGroupContainer.builder().autoScalingGroupName(ASG_NAME).build())
        .when(asgTaskHelper)
        .mapToAutoScalingGroupContainer(any());
    doReturn(new StartInstanceRefreshResult().withInstanceRefreshId("id"))
        .when(asgSdkManager)
        .startInstanceRefresh(any(), any(), any(), any());

    CreateAutoScalingGroupRequest createAutoScalingGroupRequest =
        new CreateAutoScalingGroupRequest().withAutoScalingGroupName(ASG_NAME).withLaunchTemplate(
            new LaunchTemplateSpecification().withVersion("version"));
    String createAutoScalingGroupRequestContent = AsgContentParser.toString(createAutoScalingGroupRequest, false);

    doReturn(createAutoScalingGroupRequestContent).when(asgTaskHelper).getAsgConfigurationContent(any());
  }

  @Test
  @Owner(developers = LOVISH_BANSAL)
  @Category(UnitTests.class)
  public void executeTaskInternalTest() {
    AsgRollingRollbackRequest request = createRequest();

    AsgRollingRollbackResponse response = (AsgRollingRollbackResponse) taskHandler.executeTaskInternal(
        request, iLogStreamingTaskClient, commandUnitsProgress);

    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);

    AsgRollingRollbackResult result = response.getAsgRollingRollbackResult();

    assertThat(result.getAutoScalingGroupContainer().getAutoScalingGroupName()).isEqualTo(null);
  }

  @Test
  @Owner(developers = VITALIE)
  @Category(UnitTests.class)
  public void executeRollingRollbackWithInstanceRefresh() {
    Map<String, List<String>> asgManifestsDataForRollback = Map.of("not empty", Collections.EMPTY_LIST);
    AwsInternalConfig awsInternalConfig = AwsInternalConfig.builder().build();

    AutoScalingGroupContainer result = taskHandler.executeRollingRollbackWithInstanceRefresh(
        asgSdkManager, asgManifestsDataForRollback, ASG_NAME, false, false, awsInternalConfig, "region");
    verify(asgSdkManager, times(1))
        .startInstanceRefresh(anyString(), anyBoolean(), nullable(Integer.class), nullable(Integer.class));
  }

  @Test
  @Owner(developers = VITALIE)
  @Category(UnitTests.class)
  public void executeRollingRollbackWithInstanceRefreshInProgress() {
    Map<String, List<String>> asgManifestsDataForRollback = Map.of("not empty", Collections.EMPTY_LIST);
    AwsInternalConfig awsInternalConfig = AwsInternalConfig.builder().build();

    doReturn(true).when(asgSdkManager).checkInstanceRefreshInProgress(anyString());

    taskHandler.executeRollingRollbackWithInstanceRefresh(
        asgSdkManager, asgManifestsDataForRollback, ASG_NAME, false, false, awsInternalConfig, "region");
    verify(asgSdkManager, times(1)).deleteAsg(anyString());
    verify(asgSdkManager, times(0))
        .startInstanceRefresh(anyString(), anyBoolean(), nullable(Integer.class), nullable(Integer.class));
  }

  private AsgRollingRollbackRequest createRequest() {
    return AsgRollingRollbackRequest.builder()
        .commandUnitsProgress(commandUnitsProgress)
        .timeoutIntervalInMin(1)
        .asgManifestsDataForRollback(new HashMap<>())
        .asgInfraConfig(asgInfraConfig)
        .asgName(ASG_NAME)
        .build();
  }
}
