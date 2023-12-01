/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pms.triggers.v1;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.spec.server.pipeline.v1.model.TriggerSource.TypeEnum.ARTIFACT;
import static io.harness.spec.server.pipeline.v1.model.TriggerSource.TypeEnum.MANIFEST;
import static io.harness.spec.server.pipeline.v1.model.TriggerSource.TypeEnum.SCHEDULED;
import static io.harness.spec.server.pipeline.v1.model.TriggerSource.TypeEnum.WEBHOOK;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.mapper.TagMapper;
import io.harness.ngtriggers.beans.config.NGTriggerConfigV2;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity.NGTriggerEntityBuilder;
import io.harness.ngtriggers.beans.entity.TriggerConfigWrapper;
import io.harness.ngtriggers.beans.source.NGTriggerSourceV2;
import io.harness.ngtriggers.beans.source.NGTriggerSpecV2;
import io.harness.ngtriggers.beans.source.NGTriggerType;
import io.harness.ngtriggers.beans.source.scheduled.CronTriggerSpec;
import io.harness.ngtriggers.beans.source.scheduled.ScheduledTriggerConfig;
import io.harness.ngtriggers.beans.target.TargetType;
import io.harness.pms.yaml.HarnessYamlVersion;
import io.harness.spec.server.pipeline.v1.model.CronScheduledTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.ScheduledTriggerSource;
import io.harness.spec.server.pipeline.v1.model.ScheduledTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.TriggerBody;
import io.harness.spec.server.pipeline.v1.model.TriggerGetResponseBody;
import io.harness.spec.server.pipeline.v1.model.TriggerRequestBody;
import io.harness.spec.server.pipeline.v1.model.TriggerResponseBody;
import io.harness.spec.server.pipeline.v1.model.TriggerSource;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_TRIGGERS})
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PIPELINE)
public class NGTriggerApiUtils {
  io.harness.ngtriggers.mapper.NGTriggerElementMapper ngTriggerElementMapper;

  public TriggerDetails toTriggerDetails(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      TriggerRequestBody body, boolean withServiceV2, String pipeline) {
    TriggerConfigWrapper triggerConfigWrapper = toTriggerConfigWrapper(body);
    NGTriggerEntity ngTriggerEntity = toTriggerEntity(
        accountIdentifier, orgIdentifier, projectIdentifier, body, triggerConfigWrapper, withServiceV2, pipeline);
    return TriggerDetails.builder()
        .ngTriggerConfigV2(toNGTriggerConfigV2(pipeline, orgIdentifier, projectIdentifier, body))
        .ngTriggerEntity(ngTriggerEntity)
        .build();
  }

  public NGTriggerEntity toTriggerEntity(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      TriggerRequestBody body, TriggerConfigWrapper triggerConfigWrapper, boolean withServiceV2, String pipeline) {
    NGTriggerEntityBuilder entityBuilder =
        NGTriggerEntity.builder()
            .name(body.getName())
            .identifier(body.getIdentifier())
            .description(body.getDescription())
            .harnessVersion(HarnessYamlVersion.V1)
            .type(toNGTriggerType(body.getSource().getType()))
            .accountId(accountIdentifier)
            .orgIdentifier(orgIdentifier)
            .projectIdentifier(projectIdentifier)
            .targetIdentifier(pipeline)
            .triggerConfigWrapper(triggerConfigWrapper)
            .targetType(TargetType.PIPELINE)
            .metadata(ngTriggerElementMapper.toMetadata(toNGTriggerSourceV2(body.getSource()), accountIdentifier))
            .enabled(body.isEnabled())
            .pollInterval(body.getSource().getPollInterval() != null ? body.getSource().getPollInterval() : EMPTY)
            .webhookId(body.getSource().getWebhookId())
            .withServiceV2(withServiceV2)
            .tags(TagMapper.convertToList(body.getTags()))
            .encryptedWebhookSecretIdentifier(body.getEncryptedWebhookSecretIdentifier())
            .stagesToExecute(body.getStagesToExecute())
            .tags(TagMapper.convertToList(body.getTags()));

    if (body.getSource().getType() == SCHEDULED) {
      entityBuilder.nextIterations(new ArrayList<>());
    }
    NGTriggerEntity entity = entityBuilder.build();
    if (body.getSource().getType() == SCHEDULED) {
      List<Long> nextIterations = entity.recalculateNextIterations("unused", true, 0);
      if (!nextIterations.isEmpty()) {
        entity.setNextIterations(nextIterations);
      }
    }
    return entity;
  }

  NGTriggerType toNGTriggerType(TriggerSource.TypeEnum typeEnum) {
    switch (typeEnum) {
      case WEBHOOK:
        return NGTriggerType.WEBHOOK;
      case MANIFEST:
        return NGTriggerType.MANIFEST;
      case SCHEDULED:
        return NGTriggerType.SCHEDULED;
      case ARTIFACT:
        return NGTriggerType.ARTIFACT;
      default:
        throw new InvalidRequestException(String.format("NGTrigger not supported for type: %s", typeEnum));
    }
  }

  NGTriggerConfigV2 toNGTriggerConfigV2(String pipeline, String org, String project, TriggerRequestBody body) {
    return NGTriggerConfigV2.builder()
        .pipelineIdentifier(pipeline)
        .identifier(body.getIdentifier())
        .projectIdentifier(project)
        .orgIdentifier(org)
        .encryptedWebhookSecretIdentifier(body.getEncryptedWebhookSecretIdentifier())
        .enabled(body.isEnabled())
        .description(body.getDescription())
        .inputYaml(body.getInputs())
        .inputSetRefs(body.getInputSetRefs())
        .name(body.getName())
        .pipelineBranchName(body.getPipelineBranchName())
        .tags(body.getTags())
        .source(toNGTriggerSourceV2(body.getSource()))
        .stagesToExecute(body.getStagesToExecute())
        .build();
  }

  TriggerConfigWrapper toTriggerConfigWrapper(TriggerRequestBody body) {
    return TriggerConfigWrapper.builder()
        .inputYaml(body.getInputs())
        .inputSetRefs(body.getInputSetRefs())
        .source(toNGTriggerSourceV2(body.getSource()))
        .pipelineBranchName(body.getPipelineBranchName())
        .build();
  }

  NGTriggerSourceV2 toNGTriggerSourceV2(TriggerSource source) {
    return NGTriggerSourceV2.builder()
        .pollInterval(source.getPollInterval())
        .webhookId(source.getWebhookId())
        .type(toNGTriggerType(source.getType()))
        .spec(toNGTriggerSpecV2(source))
        .build();
  }

  NGTriggerSpecV2 toNGTriggerSpecV2(TriggerSource source) {
    switch (source.getType()) {
      case SCHEDULED:
        ScheduledTriggerSpec spec = ((ScheduledTriggerSource) source).getSpec();
        return ScheduledTriggerConfig.builder()
            .type(spec.getType().toString())
            .spec(CronTriggerSpec.builder()
                      .type(spec.getSpec().getType())
                      .expression(spec.getSpec().getExpression())
                      .build())
            .build();
      default:
        throw new InvalidRequestException("Type " + source.getType().toString() + " is invalid");
    }
  }

  public TriggerResponseBody toResponseDTO(NGTriggerEntity triggerEntity) {
    TriggerResponseBody responseBody = new TriggerResponseBody();
    responseBody.setIdentifier(triggerEntity.getIdentifier());
    return responseBody;
  }

  public TriggerGetResponseBody toGetResponseDTO(NGTriggerEntity triggerEntity) {
    TriggerGetResponseBody responseBody = new TriggerGetResponseBody();
    responseBody.setIdentifier(triggerEntity.getIdentifier());
    responseBody.setTrigger(toTriggerBody(triggerEntity));
    responseBody.setDescription(triggerEntity.getDescription());
    responseBody.setName(triggerEntity.getName());
    responseBody.setOrg(triggerEntity.getOrgIdentifier());
    responseBody.setPipeline(triggerEntity.getTargetIdentifier());
    responseBody.setProject(triggerEntity.getProjectIdentifier());
    return responseBody;
  }

  public TriggerBody toTriggerBody(NGTriggerEntity triggerEntity) {
    TriggerBody triggerBody = new TriggerBody();
    triggerBody.setEnabled(triggerEntity.getEnabled());
    triggerBody.setEncryptedWebhookSecretIdentifier(triggerEntity.getEncryptedWebhookSecretIdentifier());
    triggerBody.setInputSetRefs(triggerEntity.getTriggerConfigWrapper().getInputSetRefs());
    triggerBody.setInputs(triggerEntity.getTriggerConfigWrapper().getInputYaml());
    triggerBody.setPipelineBranchName(triggerEntity.getTriggerConfigWrapper().getPipelineBranchName());
    triggerBody.setStagesToExecute(triggerEntity.getStagesToExecute());
    triggerBody.setTags(TagMapper.convertToMap(triggerEntity.getTags()));
    triggerBody.setSource(toTriggerSource(triggerEntity.getTriggerConfigWrapper().getSource()));
    return triggerBody;
  }

  public TriggerSource toTriggerSource(NGTriggerSourceV2 triggerSourceV2) {
    switch (triggerSourceV2.getType()) {
      case SCHEDULED:
        ScheduledTriggerSource source = new ScheduledTriggerSource();
        source.setPollInterval(triggerSourceV2.getPollInterval());
        source.setWebhookId(triggerSourceV2.getWebhookId());
        source.setType(toTriggerTypeEnum(triggerSourceV2.getType()));
        ScheduledTriggerSpec spec = new ScheduledTriggerSpec();
        spec.setType(ScheduledTriggerSpec.TypeEnum.CRON);
        CronScheduledTriggerSpec cronScheduledTriggerSpec = new CronScheduledTriggerSpec();
        cronScheduledTriggerSpec.setType(
            ((CronTriggerSpec) ((ScheduledTriggerConfig) triggerSourceV2.getSpec()).getSpec()).getType());
        cronScheduledTriggerSpec.setExpression(
            ((CronTriggerSpec) ((ScheduledTriggerConfig) triggerSourceV2.getSpec()).getSpec()).getExpression());
        spec.setSpec(cronScheduledTriggerSpec);
        source.setSpec(spec);
        return source;
      default:
        throw new InvalidRequestException("Type " + triggerSourceV2.getType().toString() + " is invalid");
    }
  }

  public TriggerSource.TypeEnum toTriggerTypeEnum(NGTriggerType type) {
    switch (type) {
      case WEBHOOK:
        return WEBHOOK;
      case MANIFEST:
        return MANIFEST;
      case SCHEDULED:
        return SCHEDULED;
      case ARTIFACT:
        return ARTIFACT;
      default:
        throw new InvalidRequestException(String.format("NGTrigger not supported for type: %s", type));
    }
  }
}
