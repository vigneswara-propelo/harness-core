/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.search;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.ssca.search.framework.OperatorEnum;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.SSCA)
public class ElasticSearchQueryBuilder {
  Query matchFieldValue(String field, String value) {
    return Query.of(query -> query.match(m -> m.field(field).query(value)));
  }

  Query doesNotMatchFieldValue(String field, String value) {
    return Query.of(query -> query.bool(b -> b.mustNot(q -> q.match(m -> m.field(field).query(value)))));
  }

  Query matchFieldValue(String field, boolean value) {
    return Query.of(query -> query.match(m -> m.field(field).query(value)));
  }

  Query matchFieldValue(String field, int value) {
    return Query.of(query -> query.match(m -> m.field(field).query(value)));
  }

  Query containsFieldValue(String field, String value) {
    return Query.of(query -> query.wildcard(m -> m.field(field).value("*" + value + "*")));
  }

  Query startsWithFieldValue(String field, String value) {
    return Query.of(query -> query.prefix(m -> m.field(field).value(value)));
  }

  Query shouldMatchAtleastOne(List<Query> queryList) {
    return Query.of(query -> query.bool(m -> m.should(queryList)));
  }

  Query mustMatchAll(List<Query> queryList) {
    return Query.of(query -> query.bool(m -> m.must(queryList)));
  }

  Query mustNotMatchAll(List<Query> queryList) {
    return Query.of(query -> query.bool(m -> m.mustNot(queryList)));
  }

  Query greaterThanValue(String field, Object value) {
    return Query.of(query -> query.range(r -> r.field(field).gt(JsonData.of(value))));
  }

  Query greaterThanEquals(String field, Object value) {
    return Query.of(query -> query.range(r -> r.field(field).gte(JsonData.of(value))));
  }

  Query lessThanValue(String field, Object value) {
    return Query.of(query -> query.range(r -> r.field(field).lt(JsonData.of(value))));
  }

  Query lessThanEquals(String field, Object value) {
    return Query.of(query -> query.range(r -> r.field(field).lte(JsonData.of(value))));
  }

  Query hasChild(String childType, Query query) {
    return Query.of(q -> q.hasChild(c -> c.type(childType).query(query)));
  }

  Query getFieldValue(OperatorEnum operator, String field, String value) {
    switch (operator) {
      case EQUALS:
        return ElasticSearchQueryBuilder.matchFieldValue(field, value);
      case CONTAINS:
        return ElasticSearchQueryBuilder.containsFieldValue(field, value);
      case STARTSWITH:
        return ElasticSearchQueryBuilder.startsWithFieldValue(field, value);
      case LESSTHAN:
        return ElasticSearchQueryBuilder.lessThanValue(field, value);
      case LESSTHANEQUALS:
        return ElasticSearchQueryBuilder.lessThanEquals(field, value);
      case GREATERTHAN:
        return ElasticSearchQueryBuilder.greaterThanValue(field, value);
      case GREATERTHANEQUALS:
        return ElasticSearchQueryBuilder.greaterThanEquals(field, value);
      case NOTEQUALS:
        return ElasticSearchQueryBuilder.doesNotMatchFieldValue(field, value);
      default:
        throw new InvalidRequestException(
            String.format("Component version filter does not support %s operator", operator));
    }
  }
}
