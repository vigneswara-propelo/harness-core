/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.search;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidArgumentsException;
import io.harness.spec.server.ssca.v1.model.ComponentFilter;
import io.harness.ssca.enforcement.executors.mongo.filter.denylist.fields.VersionField;
import io.harness.ssca.search.entities.Component.ComponentKeys;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.SSCA)
public class ComponentVersionQueryBuilder {
  private Query buildEqualsQuery(Integer majorVersion, Integer minorVersion, Integer patchVersion) {
    return ElasticSearchQueryBuilder.mustMatchAll(
        List.of(ElasticSearchQueryBuilder.matchFieldValue(ComponentKeys.majorVersion, majorVersion),
            ElasticSearchQueryBuilder.matchFieldValue(ComponentKeys.minorVersion, minorVersion),
            ElasticSearchQueryBuilder.matchFieldValue(ComponentKeys.patchVersion, patchVersion)));
  }

  private Query buildNotEqualsQuery(Integer majorVersion, Integer minorVersion, Integer patchVersion) {
    return ElasticSearchQueryBuilder.mustNotMatchAll(
        List.of(ElasticSearchQueryBuilder.matchFieldValue(ComponentKeys.majorVersion, majorVersion),
            ElasticSearchQueryBuilder.matchFieldValue(ComponentKeys.minorVersion, minorVersion),
            ElasticSearchQueryBuilder.matchFieldValue(ComponentKeys.patchVersion, patchVersion)));
  }

  private List<Query> buildGreaterThanQuery(Integer majorVersion, Integer minorVersion, Integer patchVersion) {
    return List.of(ElasticSearchQueryBuilder.greaterThanValue(ComponentKeys.majorVersion, majorVersion),
        ElasticSearchQueryBuilder.mustMatchAll(
            List.of(ElasticSearchQueryBuilder.greaterThanEquals(ComponentKeys.majorVersion, majorVersion),
                ElasticSearchQueryBuilder.greaterThanValue(ComponentKeys.minorVersion, minorVersion))),
        ElasticSearchQueryBuilder.mustMatchAll(
            List.of(ElasticSearchQueryBuilder.greaterThanEquals(ComponentKeys.majorVersion, majorVersion),
                ElasticSearchQueryBuilder.greaterThanEquals(ComponentKeys.minorVersion, minorVersion),
                ElasticSearchQueryBuilder.greaterThanValue(ComponentKeys.patchVersion, patchVersion))));
  }

  private List<Query> buildLessThanQuery(Integer majorVersion, Integer minorVersion, Integer patchVersion) {
    return List.of(ElasticSearchQueryBuilder.lessThanValue(ComponentKeys.majorVersion, majorVersion),
        ElasticSearchQueryBuilder.mustMatchAll(
            List.of(ElasticSearchQueryBuilder.matchFieldValue(ComponentKeys.majorVersion, majorVersion),
                ElasticSearchQueryBuilder.lessThanValue(ComponentKeys.minorVersion, minorVersion))),
        ElasticSearchQueryBuilder.mustMatchAll(
            List.of(ElasticSearchQueryBuilder.matchFieldValue(ComponentKeys.majorVersion, majorVersion),
                ElasticSearchQueryBuilder.matchFieldValue(ComponentKeys.minorVersion, minorVersion),
                ElasticSearchQueryBuilder.lessThanValue(ComponentKeys.patchVersion, patchVersion))));
  }

  public Query buildComponentVersionQuery(ComponentFilter filter) {
    List<Integer> versions = VersionField.getVersion(filter.getValue());
    if (versions.size() != 3 || versions.get(0) == -1) {
      throw new InvalidArgumentsException("Unsupported Version Format");
    }
    Integer majorVersion = versions.get(0);
    Integer minorVersion = versions.get(1);
    Integer patchVersion = versions.get(2);

    switch (filter.getOperator()) {
      case EQUALS:
        return buildEqualsQuery(majorVersion, minorVersion, patchVersion);
      case NOTEQUALS:
        return buildNotEqualsQuery(majorVersion, minorVersion, patchVersion);
      case GREATERTHAN:
        return ElasticSearchQueryBuilder.shouldMatchAtleastOne(
            buildGreaterThanQuery(majorVersion, minorVersion, patchVersion));
      case GREATERTHANEQUALS:
        List<Query> gteQueries = new ArrayList<>(buildGreaterThanQuery(majorVersion, minorVersion, patchVersion));
        gteQueries.add(buildEqualsQuery(majorVersion, minorVersion, patchVersion));
        return ElasticSearchQueryBuilder.shouldMatchAtleastOne(gteQueries);
      case LESSTHAN:
        return ElasticSearchQueryBuilder.shouldMatchAtleastOne(
            buildLessThanQuery(majorVersion, minorVersion, patchVersion));
      case LESSTHANEQUALS:
        List<Query> lteQueries = new ArrayList<>(buildLessThanQuery(majorVersion, minorVersion, patchVersion));
        lteQueries.add(buildEqualsQuery(majorVersion, minorVersion, patchVersion));
        return ElasticSearchQueryBuilder.shouldMatchAtleastOne(lteQueries);
      default:
        throw new InvalidArgumentsException("Unsupported Operator for Component Version");
    }
  }
}
