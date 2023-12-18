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
import io.harness.spec.server.ssca.v1.model.ComponentFilter;
import io.harness.spec.server.ssca.v1.model.Operator;
import io.harness.ssca.entities.NormalizedSBOMComponentEntity;
import io.harness.ssca.search.beans.ArtifactFilter;
import io.harness.ssca.search.entities.Component.ComponentKeys;
import io.harness.ssca.search.entities.SSCAArtifact.SSCAArtifactKeys;
import io.harness.ssca.search.framework.OperatorEnum;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
@OwnedBy(HarnessTeam.SSCA)
public class ArtifactQueryBuilder {
  public static OperatorEnum getOperatorEnum(Operator operator) {
    return OperatorEnum.values()[operator.ordinal()];
  }
  public Query getQuery(
      String accountId, String orgIdentifier, String projectIdentifier, ArtifactFilter artifactFilter) {
    BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
    List<Query> matchQueries = new ArrayList<>();

    List<Query> filterQueries = new ArrayList<>();

    matchQueries.add(ElasticSearchQueryBuilder.matchFieldValue(SSCAArtifactKeys.accountId, accountId));
    matchQueries.add(ElasticSearchQueryBuilder.matchFieldValue(SSCAArtifactKeys.orgIdentifier, orgIdentifier));
    matchQueries.add(ElasticSearchQueryBuilder.matchFieldValue(SSCAArtifactKeys.projectIdentifier, projectIdentifier));
    matchQueries.add(ElasticSearchQueryBuilder.matchFieldValue(SSCAArtifactKeys.invalid, false));

    if (artifactFilter != null) {
      if (!StringUtils.isEmpty(artifactFilter.getSearchTerm())) {
        matchQueries.add(
            ElasticSearchQueryBuilder.containsFieldValue(SSCAArtifactKeys.name, artifactFilter.getSearchTerm()));
      }

      if (isNotEmpty(artifactFilter.getComponentFilter())) {
        artifactFilter.getComponentFilter().forEach(filter -> {
          if (ComponentFilter.FieldNameEnum.COMPONENTNAME.equals(filter.getFieldName())) {
            filterQueries.add(ElasticSearchQueryBuilder.getFieldValue(getOperatorEnum(filter.getOperator()),
                NormalizedSBOMComponentEntity.NormalizedSBOMEntityKeys.packageName, filter.getValue()));
          } else if (ComponentFilter.FieldNameEnum.COMPONENTVERSION.equals(filter.getFieldName())) {
            filterQueries.add(ComponentVersionQueryBuilder.buildComponentVersionQuery(filter));
          }
        });
      }

      if (Objects.nonNull(artifactFilter.getLicenseFilter())) {
        filterQueries.add(ElasticSearchQueryBuilder.shouldMatchAtleastOne(List.of(
            ElasticSearchQueryBuilder.getFieldValue(getOperatorEnum(artifactFilter.getLicenseFilter().getOperator()),
                ComponentKeys.packageLicense, artifactFilter.getLicenseFilter().getValue()))));
      }
    }

    boolQueryBuilder.must(matchQueries);
    if (isNotEmpty(filterQueries)) {
      boolQueryBuilder.filter(ElasticSearchQueryBuilder.hasChild(
          SBOM_COMPONENT_ENTITY, ElasticSearchQueryBuilder.mustMatchAll(filterQueries)));
    }
    BoolQuery boolQuery = boolQueryBuilder.build();

    return boolQuery._toQuery();
  }
}
