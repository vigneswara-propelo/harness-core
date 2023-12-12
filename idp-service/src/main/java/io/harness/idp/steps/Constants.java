/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.idp.steps;

import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;

public interface Constants {
  String COOKIECUTTER = "CookieCutter";
  String CREATE_REPO = "CreateRepo";
  String DIRECT_PUSH = "DirectPush";
  String CREATE_CATALOG = "CreateCatalog";
  String SLACK_NOTIFY = "SlackNotify";
  String COOKIECUTTER_STEP_NODE = "CookieCutterStepNode";
  String CREATE_REPO_STEP_NODE = "CreateRepoStepNode";
  String REGISTER_CATALOG_STEP_NODE = "RegisterCatalogStepNode";
  String DIRECT_PUSH_STEP_NODE = "DirectPushStepNode";
  String CREATE_CATALOG_STEP_NODE = "CreateCatalogStepNode";
  String SLACK_NOTIFY_STEP_NODE = "SlackNotifyStepNode";

  String REGISTER_CATALOG = "RegisterCatalog";
  StepType COOKIECUTTER_STEP_TYPE =
      StepType.newBuilder().setType(COOKIECUTTER).setStepCategory(StepCategory.STEP).build();
  StepType CREATE_REPO_STEP_TYPE =
      StepType.newBuilder().setType(CREATE_REPO).setStepCategory(StepCategory.STEP).build();
  StepType DIRECT_PUSH_STEP_TYPE =
      StepType.newBuilder().setType(DIRECT_PUSH).setStepCategory(StepCategory.STEP).build();

  StepType REGISTER_CATALOG_STEP_TYPE =
      StepType.newBuilder().setType(REGISTER_CATALOG).setStepCategory(StepCategory.STEP).build();

  StepType CREATE_CATALOG_STEP_TYPE =
      StepType.newBuilder().setType(CREATE_CATALOG).setStepCategory(StepCategory.STEP).build();
  StepType SLACK_NOTIFY_STEP_TYPE =
      StepType.newBuilder().setType(SLACK_NOTIFY).setStepCategory(StepCategory.STEP).build();
}
