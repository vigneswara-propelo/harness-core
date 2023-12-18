/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.scorecard.datasources.providers.scm;

import static io.harness.idp.scorecard.datasourcelocations.constants.DataSourceLocations.REPOSITORY_BRANCH;
import static io.harness.idp.scorecard.datasourcelocations.constants.DataSourceLocations.REPOSITORY_NAME;
import static io.harness.idp.scorecard.datasourcelocations.constants.DataSourceLocations.REPOSITORY_OWNER;
import static io.harness.idp.scorecard.datasourcelocations.constants.DataSourceLocations.REPOSITORY_SUB_FOLDER;
import static io.harness.idp.scorecard.datasourcelocations.constants.DataSourceLocations.REPO_SCM;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.backstage.entities.BackstageCatalogEntity;
import io.harness.idp.scorecard.datapoints.parser.factory.DataPointParserFactory;
import io.harness.idp.scorecard.datapoints.service.DataPointService;
import io.harness.idp.scorecard.datasourcelocations.locations.DataSourceLocationFactory;
import io.harness.idp.scorecard.datasourcelocations.repositories.DataSourceLocationRepository;
import io.harness.idp.scorecard.datasources.providers.HttpDataSourceProvider;
import io.harness.idp.scorecard.datasources.repositories.DataSourceRepository;
import io.harness.idp.scorecard.scores.beans.DataFetchDTO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OwnedBy(HarnessTeam.IDP)
public abstract class ScmBaseProvider extends HttpDataSourceProvider {
  public static final String SOURCE_LOCATION_ANNOTATION = "backstage.io/source-location";
  public static final String GITLAB_REF = "?ref_type=heads";

  protected ScmBaseProvider(String identifier, DataPointService dataPointService,
      DataSourceLocationFactory dataSourceLocationFactory, DataSourceLocationRepository dataSourceLocationRepository,
      DataPointParserFactory dataPointParserFactory, DataSourceRepository dataSourceRepository) {
    super(identifier, dataPointService, dataSourceLocationFactory, dataSourceLocationRepository, dataPointParserFactory,
        dataSourceRepository);
  }

  protected Map<String, Map<String, Object>> scmProcessOut(String accountIdentifier, BackstageCatalogEntity entity,
      List<DataFetchDTO> dataPointsAndInputValues, String configs,
      Map<String, String> possibleReplaceableUrlBodyPairs) {
    Map<String, String> authHeaders = this.getAuthHeaders(accountIdentifier, configs);
    Map<String, String> replaceableHeaders = new HashMap<>(authHeaders);

    Map<String, String> possibleReplaceableRequestBodyPairs = new HashMap<>();

    String catalogLocation = entity.getMetadata().getAnnotations().get(SOURCE_LOCATION_ANNOTATION);
    if (catalogLocation != null) {
      possibleReplaceableRequestBodyPairs = prepareRequestBodyReplaceablePairs(catalogLocation);
    }

    possibleReplaceableUrlBodyPairs.putAll(possibleReplaceableRequestBodyPairs);

    return processOut(accountIdentifier, this.getIdentifier(), entity, replaceableHeaders,
        possibleReplaceableRequestBodyPairs, possibleReplaceableUrlBodyPairs, dataPointsAndInputValues);
  }

  protected Map<String, String> prepareRequestBodyReplaceablePairs(String catalogLocation) {
    Map<String, String> possibleReplaceableRequestBodyPairs = new HashMap<>();

    // Gitlab only
    if (catalogLocation.endsWith(GITLAB_REF)) {
      catalogLocation = catalogLocation.substring(0, catalogLocation.length() - GITLAB_REF.length());
    }

    List<String> catalogLocationParts = new ArrayList<>(Arrays.asList(catalogLocation.split("/")));

    if (catalogLocationParts.size() >= 5) {
      possibleReplaceableRequestBodyPairs.put(REPO_SCM, catalogLocationParts.get(2));
      possibleReplaceableRequestBodyPairs.put(REPOSITORY_OWNER, catalogLocationParts.get(3));
      possibleReplaceableRequestBodyPairs.put(REPOSITORY_NAME, catalogLocationParts.get(4));

      if (catalogLocationParts.size() > 5 && catalogLocationParts.get(5).equals("-")) {
        // Gitlab only
        catalogLocationParts.remove(5);
      }

      if (catalogLocationParts.size() > 6) {
        possibleReplaceableRequestBodyPairs.put(REPOSITORY_BRANCH, catalogLocationParts.get(6));
      }

      StringBuilder subFolder = new StringBuilder();
      if (catalogLocationParts.size() > 7) {
        for (int i = 7; i < catalogLocationParts.size(); i++) {
          subFolder.append(catalogLocationParts.get(i)).append("/");
        }
      }
      possibleReplaceableRequestBodyPairs.put(REPOSITORY_SUB_FOLDER, subFolder.toString());
    }

    return possibleReplaceableRequestBodyPairs;
  }
}
