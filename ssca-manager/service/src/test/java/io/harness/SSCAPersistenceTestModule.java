/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness;

import io.harness.gitsync.persistance.testing.GitSyncablePersistenceTestConfig;
import io.harness.mongo.MongoConfig;
import io.harness.springdata.HMongoTemplate;
import io.harness.springdata.SpringPersistenceModule;
import io.harness.springdata.SpringPersistenceTestConfig;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.mongodb.ReadPreference;
import org.springframework.data.mongodb.core.MongoTemplate;

public class SSCAPersistenceTestModule extends SpringPersistenceModule {
  @Override
  protected Class<?>[] getConfigClasses() {
    return new Class<?>[] {GitSyncablePersistenceTestConfig.class, SpringPersistenceTestConfig.class};
  }

  @Provides
  @Singleton
  @Named("secondary-mongo")
  protected MongoTemplate getSecondaryMongoTemplate(MongoTemplate mongoTemplate, MongoConfig primaryMongoConfig) {
    HMongoTemplate template =
        new HMongoTemplate(mongoTemplate.getMongoDbFactory(), mongoTemplate.getConverter(), primaryMongoConfig);
    template.setReadPreference(ReadPreference.secondary());
    return template;
  }
}
