/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.services.impl.sidekickexecutors;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.DHRUVX;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.VerificationApplication;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.beans.sidekick.VerificationTaskCleanupSideKickData;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.MonitoringSourcePerpetualTask;
import io.harness.cvng.core.entities.MonitoringSourcePerpetualTask.MonitoringSourcePerpetualTaskKeys;
import io.harness.cvng.core.entities.SplunkCVConfig;
import io.harness.cvng.core.entities.VerificationTask;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.LogRecordService;
import io.harness.cvng.core.services.api.MonitoringSourcePerpetualTaskService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.core.services.api.monitoredService.MonitoredServiceService;
import io.harness.cvng.models.VerificationType;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelObjectiveDTO;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelObjectiveResponse;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelIndicator;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelIndicator.ServiceLevelIndicatorKeys;
import io.harness.cvng.servicelevelobjective.services.api.ServiceLevelObjectiveService;
import io.harness.persistence.HPersistence;
import io.harness.persistence.PersistentEntity;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.reflections.Reflections;

public class VerificationTaskCleanupSideKickExecutorTest extends CvNextGenTestBase {
  @Inject private HPersistence hPersistence;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private CVConfigService cvConfigService;
  @Inject private LogRecordService logRecordService;
  @Inject private MonitoringSourcePerpetualTaskService monitoringSourcePerpetualTaskService;
  @Inject private VerificationTaskCleanupSideKickExecutor sideKickExecutor;
  @Inject private ServiceLevelObjectiveService serviceLevelObjectiveService;
  @Inject private MonitoredServiceService monitoredServiceService;

  private String connectorId;
  private String cvConfigIdentifier;
  private String projectName;
  private CVConfig cvConfig;
  private ServiceLevelIndicator sli;
  private String verificationTaskIdsForSli;
  private List<String> verificationTaskIdsForCvConfig;

  private BuilderFactory builderFactory;

  @Before
  public void setup() {
    this.builderFactory = BuilderFactory.getDefault();
    this.connectorId = generateUuid();
    this.cvConfigIdentifier = generateUuid();
    this.projectName = generateUuid();
    this.cvConfig = createCVConfig();
    this.sli = createSLI();
    this.verificationTaskIdsForSli =
        verificationTaskService.getSLIVerificationTaskId(builderFactory.getContext().getAccountId(), sli.getUuid());
    this.verificationTaskIdsForCvConfig = verificationTaskService.getServiceGuardVerificationTaskIds(
        builderFactory.getContext().getAccountId(), cvConfig.getUuid());
  }

  @Test
  @Owner(developers = DHRUVX)
  @Category(UnitTests.class)
  public void testExecute_deleteVerificationTaskForMonitoredService() {
    verificationTaskIdsForCvConfig.forEach(id -> {
      VerificationTaskCleanupSideKickData sideKickData =
          VerificationTaskCleanupSideKickData.builder().verificationTaskId(id).cvConfig(cvConfig).build();
      sideKickExecutor.execute(sideKickData);
      assertThat(hPersistence.get(VerificationTask.class, id)).isNull();
    });
  }

  @Test
  @Owner(developers = DHRUVX)
  @Category(UnitTests.class)
  public void testExecute_deleteVerificationTaskForServiceLevelIndicator() {
    VerificationTaskCleanupSideKickData sideKickData =
        VerificationTaskCleanupSideKickData.builder().verificationTaskId(verificationTaskIdsForSli).build();
    sideKickExecutor.execute(sideKickData);
    assertThat(hPersistence.get(VerificationTask.class, verificationTaskIdsForSli)).isNull();
  }

  @Test
  @Owner(developers = DHRUVX)
  @Category(UnitTests.class)
  public void testExecute_deleteMonitoringSourcePerpetualTasks() {
    monitoringSourcePerpetualTaskService.createTask(builderFactory.getContext().getAccountId(),
        cvConfig.getOrgIdentifier(), cvConfig.getProjectIdentifier(), cvConfig.getConnectorIdentifier(),
        cvConfig.getFullyQualifiedIdentifier(), false);
    List<MonitoringSourcePerpetualTask> monitoringSourcePerpetualTasksBeforeDeletion =
        hPersistence.createQuery(MonitoringSourcePerpetualTask.class)
            .filter(
                MonitoringSourcePerpetualTaskKeys.monitoringSourceIdentifier, cvConfig.getFullyQualifiedIdentifier())
            .asList();
    assertThat(monitoringSourcePerpetualTasksBeforeDeletion).hasSize(2);
    hPersistence.delete(CVConfig.class, cvConfig.getUuid());
    verificationTaskIdsForCvConfig.forEach(id
        -> sideKickExecutor.execute(
            VerificationTaskCleanupSideKickData.builder().verificationTaskId(id).cvConfig(cvConfig).build()));
    List<MonitoringSourcePerpetualTask> monitoringSourcePerpetualTasksAfterDeletion =
        hPersistence.createQuery(MonitoringSourcePerpetualTask.class)
            .filter(
                MonitoringSourcePerpetualTaskKeys.monitoringSourceIdentifier, cvConfig.getFullyQualifiedIdentifier())
            .asList();
    assertThat(monitoringSourcePerpetualTasksAfterDeletion).hasSize(0);
  }

  @Test
  @Owner(developers = DHRUVX)
  @Category(UnitTests.class)
  public void testExecute_deleteMonitoringSourcePerpetualTasksFails() {
    monitoringSourcePerpetualTaskService.createTask(builderFactory.getContext().getAccountId(),
        cvConfig.getOrgIdentifier(), cvConfig.getProjectIdentifier(), cvConfig.getConnectorIdentifier(),
        cvConfig.getFullyQualifiedIdentifier(), false);
    List<MonitoringSourcePerpetualTask> monitoringSourcePerpetualTasksBeforeDeletion =
        hPersistence.createQuery(MonitoringSourcePerpetualTask.class)
            .filter(
                MonitoringSourcePerpetualTaskKeys.monitoringSourceIdentifier, cvConfig.getFullyQualifiedIdentifier())
            .asList();
    assertThat(monitoringSourcePerpetualTasksBeforeDeletion).hasSize(2);
    verificationTaskIdsForCvConfig.forEach(id
        -> sideKickExecutor.execute(
            VerificationTaskCleanupSideKickData.builder().verificationTaskId(id).cvConfig(cvConfig).build()));
    List<MonitoringSourcePerpetualTask> monitoringSourcePerpetualTasksAfterDeletion =
        hPersistence.createQuery(MonitoringSourcePerpetualTask.class)
            .filter(
                MonitoringSourcePerpetualTaskKeys.monitoringSourceIdentifier, cvConfig.getFullyQualifiedIdentifier())
            .asList();
    assertThat(monitoringSourcePerpetualTasksAfterDeletion).hasSize(2);
  }

  @Test
  @Owner(developers = DHRUVX)
  @Category(UnitTests.class)
  public void testTriggerCleanup_entitiesList() {
    Set<Class<? extends PersistentEntity>> entitiesWithVerificationTaskId = new HashSet<>();
    entitiesWithVerificationTaskId.addAll(
        VerificationTaskCleanupSideKickExecutor.ENTITIES_TO_DELETE_BY_VERIFICATION_ID);
    entitiesWithVerificationTaskId.addAll(
        VerificationTaskCleanupSideKickExecutor.ENTITIES_DELETE_BLACKLIST_BY_VERIFICATION_ID);
    Reflections reflections = new Reflections(VerificationApplication.class.getPackage().getName());
    Set<Class<? extends PersistentEntity>> withVerificationTaskId = new HashSet<>();
    reflections.getSubTypesOf(PersistentEntity.class).forEach(entity -> {
      if (doesClassContainField(entity, VerificationTask.VERIFICATION_TASK_ID_KEY)) {
        withVerificationTaskId.add(entity);
      }
    });
    assertThat(entitiesWithVerificationTaskId)
        .isEqualTo(withVerificationTaskId)
        .withFailMessage(
            "Entities with verificationTaskId found which is not added to ENTITIES_TO_DELETE_BY_VERIFICATION_ID or ENTITIES_DELETE_BLACKLIST_BY_VERIFICATION_ID");
  }

  private boolean doesClassContainField(Class<?> clazz, String fieldName) {
    return Arrays.stream(clazz.getDeclaredFields()).anyMatch(f -> f.getName().equals(fieldName));
  }

  private CVConfig createCVConfig() {
    SplunkCVConfig splunkCVConfig = new SplunkCVConfig();
    fillCommon(splunkCVConfig);
    splunkCVConfig.setQuery("exception");
    splunkCVConfig.setServiceInstanceIdentifier(builderFactory.getContext().getServiceIdentifier());
    cvConfigService.save(splunkCVConfig);
    return splunkCVConfig;
  }

  private void fillCommon(CVConfig cvConfig) {
    cvConfig.setVerificationType(VerificationType.LOG);
    cvConfig.setAccountId(builderFactory.getContext().getAccountId());
    cvConfig.setConnectorIdentifier(connectorId);
    cvConfig.setServiceIdentifier(builderFactory.getContext().getServiceIdentifier());
    cvConfig.setOrgIdentifier(builderFactory.getContext().getOrgIdentifier());
    cvConfig.setEnvIdentifier(builderFactory.getContext().getEnvIdentifier());
    cvConfig.setProjectIdentifier(builderFactory.getContext().getProjectIdentifier());
    cvConfig.setIdentifier(cvConfigIdentifier);
    cvConfig.setMonitoringSourceName(generateUuid());
    cvConfig.setCategory(CVMonitoringCategory.PERFORMANCE);
    cvConfig.setProductName(projectName);
  }

  private ServiceLevelIndicator createSLI() {
    ServiceLevelObjectiveDTO sloDTO = builderFactory.getServiceLevelObjectiveDTOBuilder().build();
    createMonitoredService();
    ProjectParams projectParams = ProjectParams.builder()
                                      .accountIdentifier(builderFactory.getContext().getAccountId())
                                      .projectIdentifier(builderFactory.getContext().getProjectIdentifier())
                                      .orgIdentifier(builderFactory.getContext().getOrgIdentifier())
                                      .build();
    ServiceLevelObjectiveResponse serviceLevelObjectiveResponse =
        serviceLevelObjectiveService.create(projectParams, sloDTO);
    String identifier =
        serviceLevelObjectiveResponse.getServiceLevelObjectiveDTO().getServiceLevelIndicators().get(0).getIdentifier();
    return hPersistence.createQuery(ServiceLevelIndicator.class)
        .filter(ServiceLevelIndicatorKeys.accountId, projectParams.getAccountIdentifier())
        .filter(ServiceLevelIndicatorKeys.orgIdentifier, projectParams.getOrgIdentifier())
        .filter(ServiceLevelIndicatorKeys.projectIdentifier, projectParams.getProjectIdentifier())
        .filter(ServiceLevelIndicatorKeys.identifier, identifier)
        .get();
  }

  private void createMonitoredService() {
    MonitoredServiceDTO monitoredServiceDTO = builderFactory.monitoredServiceDTOBuilder().build();
    monitoredServiceDTO.setSources(MonitoredServiceDTO.Sources.builder().build());
    monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO);
  }
}
