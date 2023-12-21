/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories.exemption;

import io.harness.ssca.entities.exemption.Exemption;
import io.harness.ssca.entities.exemption.Exemption.ExemptionKeys;

import com.google.inject.Inject;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;

public class ExemptionRepositoryCustomImpl implements ExemptionRepositoryCustom {
  @Inject MongoTemplate mongoTemplate;
  @Override
  public Page<Exemption> findExemptions(Criteria criteria, Pageable pageable) {
    Query query = getQueryWithDefaultSorting(criteria).with(pageable);
    List<Exemption> exemptions = mongoTemplate.find(query, Exemption.class);
    return PageableExecutionUtils.getPage(
        exemptions, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Exemption.class));
  }

  @Override
  public List<Exemption> findExemptions(Criteria criteria) {
    return mongoTemplate.find(getQueryWithDefaultSorting(criteria), Exemption.class);
  }

  private static Query getQueryWithDefaultSorting(Criteria criteria) {
    return new Query().addCriteria(criteria).with(Sort.by(ExemptionKeys.createdAt));
  }
}
