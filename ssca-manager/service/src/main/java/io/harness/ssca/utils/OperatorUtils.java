/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.utils;

import io.harness.exception.InvalidArgumentsException;
import io.harness.spec.server.ssca.v1.model.Operator;

import org.springframework.data.mongodb.core.query.Criteria;

public class OperatorUtils {
  public static Criteria getCriteria(Operator operator, String key, int value) {
    switch (operator) {
      case NOTEQUALS:
        return new Criteria().and(key).ne(value);
      case GREATERTHAN:
        return new Criteria().and(key).gt(value);
      case GREATERTHANEQUALS:
        return new Criteria().and(key).gte(value);
      case LESSTHAN:
        return new Criteria().and(key).lt(value);
      case LESSTHANEQUALS:
        return new Criteria().and(key).lte(value);
      case EQUALS:
        return new Criteria().and(key).is(value);
      default:
        throw new InvalidArgumentsException(String.format("Unsupported Operator: %s", operator));
    }
  }

  public static Operator getComponentFilterOperatorMapping(Operator operator) {
    if (operator == Operator.GREATERTHANEQUALS) {
      return Operator.GREATERTHAN;
    } else if (operator == Operator.LESSTHANEQUALS) {
      return Operator.LESSTHAN;
    } else {
      return operator;
    }
  }
}
