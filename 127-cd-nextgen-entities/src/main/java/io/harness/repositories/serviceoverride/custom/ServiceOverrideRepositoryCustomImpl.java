/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.repositories.serviceoverride.custom;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.ng.core.environment.mappers.EnvironmentFilterHelper;
import io.harness.ng.core.serviceoverride.beans.NGServiceOverridesEntity;
import io.harness.ng.core.serviceoverride.yaml.NGServiceOverrideConfig;
import io.harness.ng.core.serviceoverridev2.beans.ServiceOverrideSpecConfig;
import io.harness.ng.core.serviceoverridev2.beans.ServiceOverridesSpec;
import io.harness.ng.core.serviceoverridev2.mappers.ServiceOverridesMapperV2;
import io.harness.pms.yaml.YamlUtils;
import io.harness.springdata.PersistenceUtils;

import com.google.inject.Inject;
import com.mongodb.client.result.DeleteResult;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.repository.support.PageableExecutionUtils;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true,
    components = {HarnessModuleComponent.CDS_SERVICE_ENVIRONMENT})
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
public class ServiceOverrideRepositoryCustomImpl implements ServiceOverrideRepositoryCustom {
  private final MongoTemplate mongoTemplate;
  private final Duration RETRY_SLEEP_DURATION = Duration.ofSeconds(10);
  private final int MAX_ATTEMPTS = 3;

  @Override
  public Page<NGServiceOverridesEntity> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<NGServiceOverridesEntity> projects = mongoTemplate.find(query, NGServiceOverridesEntity.class);
    return PageableExecutionUtils.getPage(projects, pageable,
        () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), NGServiceOverridesEntity.class));
  }

  @Override
  public NGServiceOverridesEntity upsert(Criteria criteria, NGServiceOverridesEntity serviceOverridesEntity) {
    Query query = new Query(criteria);
    setYamlV2InRequestedEntity(serviceOverridesEntity);
    Update updateOperations = EnvironmentFilterHelper.getUpdateOperationsForServiceOverride(serviceOverridesEntity);

    RetryPolicy<Object> retryPolicy =
        getRetryPolicy("[Retrying]: Failed upserting Service Override Entity; attempt: {}",
            "[Failed]: Failed upserting Service Override Entity; attempt: {}");
    return Failsafe.with(retryPolicy)
        .get(()
                 -> mongoTemplate.findAndModify(query, updateOperations,
                     new FindAndModifyOptions().returnNew(true).upsert(true), NGServiceOverridesEntity.class));
  }

  private void setYamlV2InRequestedEntity(NGServiceOverridesEntity serviceOverridesEntity) {
    try {
      NGServiceOverrideConfig config = YamlUtils.read(serviceOverridesEntity.getYaml(), NGServiceOverrideConfig.class);
      ServiceOverridesSpec serviceOverrideSpec = ServiceOverridesMapperV2.toServiceOverrideSpec(config);
      ServiceOverrideSpecConfig specConfig = ServiceOverrideSpecConfig.builder().spec(serviceOverrideSpec).build();
      String yamlV2 = YamlUtils.writeYamlString(specConfig);
      serviceOverridesEntity.setYamlV2(yamlV2);
    } catch (IOException ex) {
      log.error(String.format("Could not save yamlV2 for override : [%s] of type [%s]",
                    serviceOverridesEntity.getIdentifier(), serviceOverridesEntity.getType()),
          ex);
    }
  }

  @Override
  public DeleteResult delete(Criteria criteria) {
    Query query = new Query(criteria);
    RetryPolicy<Object> retryPolicy = getRetryPolicy("[Retrying]: Failed deleting Service Override; attempt: {}",
        "[Failed]: Failed deleting Service Override; attempt: {}");
    return Failsafe.with(retryPolicy).get(() -> mongoTemplate.remove(query, NGServiceOverridesEntity.class));
  }

  private RetryPolicy<Object> getRetryPolicy(String failedAttemptMessage, String failureMessage) {
    return PersistenceUtils.getRetryPolicy(failedAttemptMessage, failureMessage);
  }
}
