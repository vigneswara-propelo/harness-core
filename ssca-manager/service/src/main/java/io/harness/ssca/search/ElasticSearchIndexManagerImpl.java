/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.search;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.GeneralException;
import io.harness.ssca.search.utils.ElasticSearchUtils;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import com.google.inject.Inject;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.SSCA)
@Slf4j
public abstract class ElasticSearchIndexManagerImpl implements ElasticSearchIndexManager {
  @Inject private ElasticsearchClient elasticsearchClient;

  protected abstract String getIndexName(String accountId);
  protected abstract String getIndexMapping();

  @Override
  public String getIndex(String accountId) {
    String index = getIndexName(accountId);
    if (!indexExists(index)) {
      createIndex(index);
    }
    return index;
  }

  @Override
  public boolean indexExists(String index) {
    try {
      return elasticsearchClient.indices().exists(b -> b.index(index)).value();
    } catch (IOException e) {
      log.error("Error while handling index exists request {}", e);
      return false;
    }
  }

  @Override
  public boolean createIndex(String indexName) {
    ElasticsearchIndicesClient elasticsearchIndicesClient = this.elasticsearchClient.indices();

    try {
      if (!indexExists(indexName)) {
        CreateIndexRequest createIndexRequest =
            new CreateIndexRequest.Builder()
                .index(indexName)
                .mappings(ElasticSearchUtils.getTypeMappingFromFile(getIndexMapping()))
                .build();
        CreateIndexResponse createIndexResponse = elasticsearchIndicesClient.create(createIndexRequest);
        return createIndexResponse.acknowledged();

      } else {
        return true;
      }

    } catch (IOException ex) {
      throw new GeneralException("Could not create the index", ex);
    }
  }

  @Override
  public boolean deleteIndex(String accountId) {
    String indexName = getIndex(accountId);
    ElasticsearchIndicesClient elasticsearchIndicesClient = this.elasticsearchClient.indices();

    DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest.Builder().index(indexName).build();

    try {
      DeleteIndexResponse deleteIndexResponse = elasticsearchIndicesClient.delete(deleteIndexRequest);
      return deleteIndexResponse.acknowledged();
    } catch (IOException ex) {
      throw new GeneralException("Could not delete the index", ex);
    }
  }

  @Override
  public boolean deleteIndexByName(String indexName) {
    ElasticsearchIndicesClient elasticsearchIndicesClient = this.elasticsearchClient.indices();
    if (indexExists(indexName)) {
      DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest.Builder().index(indexName).build();

      try {
        DeleteIndexResponse deleteIndexResponse = elasticsearchIndicesClient.delete(deleteIndexRequest);
        return deleteIndexResponse.acknowledged();
      } catch (IOException ex) {
        throw new GeneralException("Could not delete the index", ex);
      }
    }
    return true;
  }
}
