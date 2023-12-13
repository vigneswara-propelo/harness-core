/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories.drift;

import io.harness.ssca.entities.drift.DriftEntity;

import java.util.List;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public interface SbomDriftRepositoryCustom {
  boolean exists(Criteria criteria);
  DriftEntity find(Criteria criteria);
  DriftEntity update(Query query, Update update);

  <T> List<T> aggregate(Aggregation aggregation, Class<T> resultClass);

  DriftEntity findOne(Query query);
}
