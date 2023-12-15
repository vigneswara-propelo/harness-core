/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.helpers;

import io.harness.exception.GeneralException;

import com.google.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.util.CloseableIterator;

@Singleton
@AllArgsConstructor
public class BatchProcessor<T> {
  private static final int BATCH_SIZE = 1000;

  private final MongoTemplate mongoTemplate;
  private final Class<T> entityClass;

  private CloseableIterator<T> runQueryWithBatch(Query query, int batchSize) {
    query.cursorBatchSize(batchSize);
    return mongoTemplate.stream(query, entityClass);
  }

  private void processEntitiesInBatches(CloseableIterator<T> iterator, int batchSize,
      Function<T, String> groupingFunction, BiConsumer<String, List<T>> processBatchFunction) {
    while (iterator.hasNext()) {
      Map<String, List<T>> batch =
          StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false)
              .limit(batchSize)
              .filter(entity -> groupingFunction == null || groupingFunction.apply(entity) != null)
              .collect(Collectors.groupingBy(groupingFunction, Collectors.toList()));

      if (!batch.isEmpty()) {
        for (Map.Entry<String, List<T>> entry : batch.entrySet()) {
          processBatchFunction.accept(entry.getKey(), entry.getValue());
        }
      }
    }
  }

  public void processBatch(
      Query query, Function<T, String> groupingFunction, BiConsumer<String, List<T>> processBatchFunction) {
    try {
      processEntitiesInBatches(
          runQueryWithBatch(query, BATCH_SIZE), BATCH_SIZE, groupingFunction, processBatchFunction);
    } catch (Exception e) {
      throw new GeneralException("Could not process the batch", e);
    }
  }
}
