/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.notification.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.NGEntityName;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.notification.NotificationChannelType;
import io.harness.persistence.PersistentEntity;
import io.harness.spec.server.notification.v1.model.ChannelDTO;

import com.google.common.collect.ImmutableList;
import dev.morphia.annotations.Entity;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "NotificationChannelKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@StoreIn(DbAliases.NOTIFICATION)
@Entity(value = "notificationChannel", noClassnameStored = true)
@Document("notificationChannel")
@TypeAlias("notificationChannel")
@HarnessEntity(exportable = true)
@OwnedBy(HarnessTeam.PL)
@Slf4j
public class NotificationChannel implements PersistentEntity, PersistentRegularIterable, Channel {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("unique_channel_identifier_idx")
                 .unique(true)
                 .field(NotificationChannelKeys.accountIdentifier)
                 .field(NotificationChannelKeys.orgIdentifier)
                 .field(NotificationChannelKeys.projectIdentifier)
                 .field(NotificationChannelKeys.identifier)
                 .build())
        .build();
  }
  @Id @dev.morphia.annotations.Id String uuid;

  @NotEmpty @EntityIdentifier String identifier;
  @NotEmpty @NGEntityName String name;
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;

  NotificationChannelType notificationChannelType;
  Status status;

  Channel channel;

  @CreatedBy EmbeddedUser createdBy;
  @LastModifiedBy EmbeddedUser lastUpdatedBy;
  @CreatedDate Long createdAt;
  @LastModifiedDate Long lastModifiedAt;

  @FdIndex long nextIteration;

  @Override
  public Long obtainNextIteration(String fieldName) {
    return nextIteration;
  }

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
    this.nextIteration = nextIteration;
  }

  @Override
  public String logKeyForId() {
    return PersistentRegularIterable.super.logKeyForId();
  }

  @Override
  public Object toObjectofProtoSchema() {
    // No implementation
    return null;
  }

  @Override
  public NotificationChannelType getChannelType() {
    return notificationChannelType;
  }

  @Override
  public ChannelDTO dto() {
    // No implementation
    return null;
  }

  public enum Status {
    ENABLED,
    DISABLED;
  }
}
