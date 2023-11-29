/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.cf.apprenaming;

import static software.wings.beans.LogColor.White;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;

import io.harness.delegate.beans.pcf.CfInBuiltVariablesUpdateValues;
import io.harness.delegate.beans.pcf.CfInBuiltVariablesUpdateValues.CfInBuiltVariablesUpdateValuesBuilder;
import io.harness.delegate.beans.pcf.CfRouteUpdateRequestConfigData;
import io.harness.delegate.cf.PcfCommandTaskBaseHelper;
import io.harness.logging.LogCallback;
import io.harness.pcf.CfDeploymentManager;
import io.harness.pcf.PivotalClientApiException;
import io.harness.pcf.model.CfRequestConfig;
import io.harness.pcf.model.PcfConstants;

import java.util.List;
import java.util.SortedMap;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.operations.applications.ApplicationSummary;

/**
 * For BG with only 2 apps NewApp = InactiveApp, because we pushed the NewApp to existing InactiveApp
 *
 * Before renaming the system will have following apps:-
 * OrderService_INACTIVE
 * OrderService
 *
 * After renaming
 * --------------
 * OrderService             -->   OrderService_INACTIVE
 * OrderService_INACTIVE    -->   OrderService
 *
 * The app should be renamed in these order  --> NEW --> ACTIVE
 */

@Slf4j
public class RollbackBGNonVersionToNonVersion implements AppRenamingOperator {
  @Override
  public CfInBuiltVariablesUpdateValues renameApp(CfRouteUpdateRequestConfigData cfRouteUpdateConfigData,
      CfRequestConfig cfRequestConfig, LogCallback executionLogCallback, CfDeploymentManager pcfDeploymentManager,
      PcfCommandTaskBaseHelper pcfCommandTaskBaseHelper) throws PivotalClientApiException {
    AppNamingStrategy existingStrategy = AppNamingStrategy.get(cfRouteUpdateConfigData.getExistingAppNamingStrategy());
    if (AppNamingStrategy.VERSIONING == existingStrategy && !cfRouteUpdateConfigData.isNonVersioning()) {
      // versioning to versioning does not require renaming
      return CfInBuiltVariablesUpdateValues.builder().build();
    }

    executionLogCallback.saveExecutionLog(color("# Reverting app names", White, Bold));

    String cfAppNamePrefix = cfRouteUpdateConfigData.getCfAppNamePrefix();
    List<ApplicationSummary> allReleases = pcfDeploymentManager.getPreviousReleases(cfRequestConfig, cfAppNamePrefix);

    SortedMap<AppType, AppRenamingData> appTypeApplicationSummaryMap =
        getAppsInTheRenamingOrder(cfRouteUpdateConfigData, allReleases);

    ApplicationSummary newApplicationSummary = appTypeApplicationSummaryMap.get(AppType.NEW).getAppSummary();
    String intermediateName = PcfConstants.generateInterimAppName(cfAppNamePrefix);

    renameApp(
        newApplicationSummary, pcfCommandTaskBaseHelper, cfRequestConfig, executionLogCallback, intermediateName, log);

    ApplicationSummary currentActiveApplicationSummary =
        appTypeApplicationSummaryMap.get(AppType.ACTIVE).getAppSummary();
    renameApp(currentActiveApplicationSummary, pcfCommandTaskBaseHelper, cfRequestConfig, executionLogCallback,
        cfAppNamePrefix, log);

    String inActiveName = cfAppNamePrefix + PcfConstants.INACTIVE_APP_NAME_SUFFIX;
    renameApp(newApplicationSummary, pcfCommandTaskBaseHelper, cfRequestConfig, executionLogCallback, inActiveName,
        intermediateName, log);

    CfInBuiltVariablesUpdateValuesBuilder updateValuesBuilder = CfInBuiltVariablesUpdateValues.builder();
    updateValuesBuilder.newAppGuid(currentActiveApplicationSummary.getId());
    updateValuesBuilder.newAppName(cfAppNamePrefix);
    updateValuesBuilder.oldAppGuid(newApplicationSummary.getId());
    updateValuesBuilder.oldAppName(inActiveName);
    return updateValuesBuilder.build();
  }
}
