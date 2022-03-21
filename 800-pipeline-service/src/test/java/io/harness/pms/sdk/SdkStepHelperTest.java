package io.harness.pms.sdk;

import static io.harness.rule.OwnerRule.SAHIL;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.PipelineServiceTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.pipeline.CommonStepInfo;
import io.harness.rule.Owner;

import java.util.ArrayList;
import java.util.HashMap;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.PIPELINE)
public class SdkStepHelperTest extends PipelineServiceTestBase {
  @Mock PmsSdkInstanceService pmsSdkInstanceService;
  @Mock CommonStepInfo commonStepInfo;
  @InjectMocks SdkStepHelper sdkStepHelper;

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetAllStepsVisibleInUI() {
    when(pmsSdkInstanceService.getSdkSteps()).thenReturn(new HashMap<>());
    when(commonStepInfo.getCommonSteps("")).thenReturn(new ArrayList<>());
    assertEquals(sdkStepHelper.getAllStepVisibleInUI().size(), 0);
    verify(pmsSdkInstanceService).getSdkSteps();
    verify(commonStepInfo).getCommonSteps("");
  }
}