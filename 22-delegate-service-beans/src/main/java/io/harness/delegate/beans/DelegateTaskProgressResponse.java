package io.harness.delegate.beans;

import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.persistence.PersistentEntity;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.time.OffsetDateTime;
import java.util.Date;

@Value
@Builder
@Entity(value = "!!!custom_delegateTaskProgressResponses", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "DelegateTaskProgressResponseKeys")
public class DelegateTaskProgressResponse implements PersistentEntity {
  @Id private String uuid;
  private byte[] responseData;
  @FdIndex private long processAfter;

  @FdTtlIndex @Builder.Default private Date validUntil = Date.from(OffsetDateTime.now().plusHours(2).toInstant());
}
