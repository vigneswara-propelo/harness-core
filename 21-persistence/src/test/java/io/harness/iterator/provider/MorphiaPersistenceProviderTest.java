package io.harness.iterator.provider;

import static io.harness.rule.OwnerRule.GEORGE;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.PersistenceTest;
import io.harness.category.element.UnitTests;
import io.harness.iterator.TestIterableEntity;
import io.harness.iterator.TestIterableEntity.TestIterableEntityKeys;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mongodb.morphia.query.Query;

public class MorphiaPersistenceProviderTest extends PersistenceTest {
  @Inject private MorphiaPersistenceProvider<TestIterableEntity> persistenceProvider;

  @SuppressWarnings("checkstyle:RepetitiveName")
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testCreateQueryWithNoFilter() {
    final Query<TestIterableEntity> query =
        persistenceProvider.createQuery(TestIterableEntity.class, TestIterableEntityKeys.nextIterations, null);

    assertThat(query.toString().replace(" ", "")).isEqualTo("{query:{}}");
    assertThat(persistenceProvider.createQuery(5, TestIterableEntity.class, TestIterableEntityKeys.nextIterations, null)
                   .toString()
                   .replace(" ", ""))
        .isEqualTo(
            "{query:{\"$or\":[{\"nextIterations\":{\"$lt\":{\"$numberLong\":\"5\"}}},{\"nextIterations\":{\"$exists\":false}}]}}");
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testCreateQueryWithSimpleFilter() {
    MorphiaFilterExpander<TestIterableEntity> filterExpander =
        query -> query.filter(TestIterableEntityKeys.name, "foo");

    final Query<TestIterableEntity> query = persistenceProvider.createQuery(
        TestIterableEntity.class, TestIterableEntityKeys.nextIterations, filterExpander);

    assertThat(query.toString().replace(" ", "")).isEqualTo("{query:{\"name\":\"foo\"}}");
    assertThat(persistenceProvider
                   .createQuery(5, TestIterableEntity.class, TestIterableEntityKeys.nextIterations, filterExpander)
                   .toString()
                   .replace(" ", ""))
        .isEqualTo(
            "{query:{\"name\":\"foo\",\"$or\":[{\"nextIterations\":{\"$lt\":{\"$numberLong\":\"5\"}}},{\"nextIterations\":{\"$exists\":false}}]}}");
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testCreateQueryWithAndFilter() {
    MorphiaFilterExpander<TestIterableEntity> filterExpander = query
        -> query.and(query.criteria(TestIterableEntityKeys.name).exists(),
            query.criteria(TestIterableEntityKeys.name).equal("foo"));

    final Query<TestIterableEntity> query = persistenceProvider.createQuery(
        TestIterableEntity.class, TestIterableEntityKeys.nextIterations, filterExpander);

    assertThat(query.toString().replace(" ", ""))
        .isEqualTo("{query:{\"$and\":[{\"name\":{\"$exists\":true}},{\"name\":\"foo\"}]}}");
    assertThat(persistenceProvider
                   .createQuery(5, TestIterableEntity.class, TestIterableEntityKeys.nextIterations, filterExpander)
                   .toString()
                   .replace(" ", ""))
        .isEqualTo(
            "{query:{\"$and\":[{\"$and\":[{\"name\":{\"$exists\":true}},{\"name\":\"foo\"}]},{\"$or\":[{\"nextIterations\":{\"$lt\":{\"$numberLong\":\"5\"}}},{\"nextIterations\":{\"$exists\":false}}]}]}}");
  }
}
