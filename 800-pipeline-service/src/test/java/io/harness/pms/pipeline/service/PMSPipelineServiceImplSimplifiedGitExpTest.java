package io.harness.pms.pipeline.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.HintException;
import io.harness.gitsync.persistance.GitSyncSdkService;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.repositories.pipeline.PMSPipelineRepository;
import io.harness.rule.Owner;

import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(PIPELINE)
public class PMSPipelineServiceImplSimplifiedGitExpTest extends CategoryTest {
  PMSPipelineServiceImpl pipelineService;
  @Mock private PMSPipelineServiceHelper pipelineServiceHelper;
  @Mock private GitSyncSdkService gitSyncSdkService;
  @Mock private PMSPipelineRepository pipelineRepository;

  String accountIdentifier = "acc";
  String orgIdentifier = "org";
  String projectIdentifier = "proj";
  String pipelineId = "pipeline";
  String pipelineYaml = "pipeline: yaml";

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    pipelineService = new PMSPipelineServiceImpl(
        pipelineRepository, null, pipelineServiceHelper, null, gitSyncSdkService, null, null, null);
    doReturn(false).when(gitSyncSdkService).isGitSyncEnabled(accountIdentifier, orgIdentifier, projectIdentifier);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testCreatePipeline() throws IOException {
    PipelineEntity pipelineToSave = PipelineEntity.builder()
                                        .accountId(accountIdentifier)
                                        .orgIdentifier(orgIdentifier)
                                        .projectIdentifier(projectIdentifier)
                                        .identifier(pipelineId)
                                        .yaml(pipelineYaml)
                                        .build();
    PipelineEntity pipelineToSaveWithUpdatedInfo = pipelineToSave.withStageCount(0);
    PipelineEntity pipelineEntitySaved = pipelineToSaveWithUpdatedInfo.withVersion(0L);
    doReturn(pipelineToSaveWithUpdatedInfo).when(pipelineServiceHelper).updatePipelineInfo(pipelineToSave);
    doReturn(pipelineEntitySaved).when(pipelineRepository).save(pipelineToSaveWithUpdatedInfo);

    PipelineEntity pipelineEntity = pipelineService.create(pipelineToSave);
    assertThat(pipelineEntity).isEqualTo(pipelineEntitySaved);
    verify(pipelineServiceHelper, times(1))
        .sendPipelineSaveTelemetryEvent(pipelineEntitySaved, "creating new pipeline");
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testCreatePipelineWithHintException() throws IOException {
    PipelineEntity pipelineToSave = PipelineEntity.builder()
                                        .accountId(accountIdentifier)
                                        .orgIdentifier(orgIdentifier)
                                        .projectIdentifier(projectIdentifier)
                                        .identifier(pipelineId)
                                        .yaml(pipelineYaml)
                                        .build();
    PipelineEntity pipelineToSaveWithUpdatedInfo = pipelineToSave.withStageCount(0);
    doReturn(pipelineToSaveWithUpdatedInfo).when(pipelineServiceHelper).updatePipelineInfo(pipelineToSave);
    doThrow(new HintException("this is a hint")).when(pipelineRepository).save(pipelineToSaveWithUpdatedInfo);

    assertThatThrownBy(() -> pipelineService.create(pipelineToSave))
        .isInstanceOf(HintException.class)
        .hasMessage("this is a hint");
    verify(pipelineServiceHelper, times(0)).sendPipelineSaveTelemetryEvent(any(), any());
  }
}
