/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.search;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.ssca.search.framework.Constants.SBOM_COMPONENT_ENTITY;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.spec.server.ssca.v1.model.ComponentFilter;
import io.harness.spec.server.ssca.v1.model.Operator;
import io.harness.ssca.entities.NormalizedSBOMComponentEntity;
import io.harness.ssca.search.beans.ArtifactFilter;
import io.harness.ssca.search.entities.Component;
import io.harness.ssca.search.entities.SSCAArtifact;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
@OwnedBy(HarnessTeam.SSCA)
public class ArtifactQueryBuilder {
  Map<ComponentFilter.FieldNameEnum, String> componentFilterToFieldNameMap = Map.of(
      ComponentFilter.FieldNameEnum.COMPONENTNAME, NormalizedSBOMComponentEntity.NormalizedSBOMEntityKeys.packageName,
      ComponentFilter.FieldNameEnum.COMPONENTVERSION,
      NormalizedSBOMComponentEntity.NormalizedSBOMEntityKeys.packageVersion);
  public Query getFieldValue(Operator operator, String field, String value) {
    switch (operator) {
      case EQUALS:
        return ElasticSearchQueryBuilder.matchFieldValue(field, value);
      case CONTAINS:
        return ElasticSearchQueryBuilder.containsFieldValue(field, value);
      case STARTSWITH:
        return ElasticSearchQueryBuilder.startsWithFieldValue(field, value);
      default:
        throw new InvalidRequestException(
            String.format("Component version filter does not support %s operator", operator));
    }
  }

  public Query getQuery(
      String accountId, String orgIdentifier, String projectIdentifier, ArtifactFilter artifactFilter) {
    BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
    List<Query> matchQueries = new ArrayList<>();

    List<Query> filterQueries = new ArrayList<>();

    matchQueries.add(ElasticSearchQueryBuilder.matchFieldValue(SSCAArtifact.SSCAArtifactKeys.accountId, accountId));
    matchQueries.add(
        ElasticSearchQueryBuilder.matchFieldValue(SSCAArtifact.SSCAArtifactKeys.orgIdentifier, orgIdentifier));
    matchQueries.add(
        ElasticSearchQueryBuilder.matchFieldValue(SSCAArtifact.SSCAArtifactKeys.projectIdentifier, projectIdentifier));
    matchQueries.add(ElasticSearchQueryBuilder.matchFieldValue(SSCAArtifact.SSCAArtifactKeys.invalid, false));

    if (artifactFilter != null) {
      if (!StringUtils.isEmpty(artifactFilter.getSearchTerm())) {
        matchQueries.add(ElasticSearchQueryBuilder.containsFieldValue(
            SSCAArtifact.SSCAArtifactKeys.name, artifactFilter.getSearchTerm()));
      }

      if (isNotEmpty(artifactFilter.getComponentFilter())) {
        artifactFilter.getComponentFilter().forEach(filter
            -> filterQueries.add(ElasticSearchQueryBuilder.hasChild(SBOM_COMPONENT_ENTITY,
                getFieldValue(filter.getOperator(), componentFilterToFieldNameMap.get(filter.getFieldName()),
                    filter.getValue()))));
      }

      if (Objects.nonNull(artifactFilter.getLicenseFilter())) {
        filterQueries.add(ElasticSearchQueryBuilder.hasChild(SBOM_COMPONENT_ENTITY,
            getFieldValue(artifactFilter.getLicenseFilter().getOperator(), Component.ComponentKeys.packageLicense,
                artifactFilter.getLicenseFilter().getValue())));
      }
    }

    boolQueryBuilder.must(matchQueries);
    if (isNotEmpty(filterQueries)) {
      boolQueryBuilder.filter(filterQueries);
    }
    BoolQuery boolQuery = boolQueryBuilder.build();

    return boolQuery._toQuery();
  }
}
