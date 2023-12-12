/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.search;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.SSCA)
public class ElasticSearchQueryBuilder {
  Query matchFieldValue(String field, String value) {
    return Query.of(query -> query.match(m -> m.field(field).query(value)));
  }

  Query matchFieldValue(String field, boolean value) {
    return Query.of(query -> query.match(m -> m.field(field).query(value)));
  }

  Query containsFieldValue(String field, String value) {
    return Query.of(query -> query.wildcard(m -> m.field(field).value(value)));
  }

  Query startsWithFieldValue(String field, String value) {
    return Query.of(query -> query.prefix(m -> m.field(field).value(value)));
  }

  Query hasChild(String childType, Query query) {
    return Query.of(q -> q.hasChild(c -> c.type(childType).query(query)));
  }
}
