/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.backstage.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.backstage.entities.BackstageCatalogApiEntity;
import io.harness.idp.backstage.entities.BackstageCatalogComponentEntity;
import io.harness.idp.backstage.entities.BackstageCatalogDomainEntity;
import io.harness.idp.backstage.entities.BackstageCatalogEntity;
import io.harness.idp.backstage.entities.BackstageCatalogGroupEntity;
import io.harness.idp.backstage.entities.BackstageCatalogLocationEntity;
import io.harness.idp.backstage.entities.BackstageCatalogResourceEntity;
import io.harness.idp.backstage.entities.BackstageCatalogSystemEntity;
import io.harness.idp.backstage.entities.BackstageCatalogTemplateEntity;

@OwnedBy(HarnessTeam.IDP)
public enum BackstageCatalogEntityTypes {
  API("API"),
  COMPONENT("Component"),
  DOMAIN("Domain"),
  GROUP("Group"),
  LOCATION("Location"),
  RESOURCE("Resource"),
  SYSTEM("System"),
  TEMPLATE("Template"),
  USER("User");

  public final String kind;

  BackstageCatalogEntityTypes(String kind) {
    this.kind = kind;
  }

  public static BackstageCatalogEntityTypes fromString(String text) {
    for (BackstageCatalogEntityTypes type : BackstageCatalogEntityTypes.values()) {
      if (type.kind.equalsIgnoreCase(text)) {
        return type;
      }
    }
    throw new IllegalArgumentException(String.format("Could not find type for %s", text));
  }

  public static String getEntityType(BackstageCatalogEntity entity) {
    switch (BackstageCatalogEntityTypes.fromString(entity.getKind())) {
      case API:
        return ((BackstageCatalogApiEntity) entity).getSpec().getType();
      case COMPONENT:
        return ((BackstageCatalogComponentEntity) entity).getSpec().getType();
      case LOCATION:
        return ((BackstageCatalogLocationEntity) entity).getSpec().getType();
      case TEMPLATE:
        return ((BackstageCatalogTemplateEntity) entity).getSpec().getType();
      case RESOURCE:
        return ((BackstageCatalogResourceEntity) entity).getSpec().getType();
      default:
        return null;
    }
  }

  public static String getEntityOwner(BackstageCatalogEntity entity) {
    switch (BackstageCatalogEntityTypes.fromString(entity.getKind())) {
      case API:
        return ((BackstageCatalogApiEntity) entity).getSpec().getOwner();
      case COMPONENT:
        return ((BackstageCatalogComponentEntity) entity).getSpec().getOwner();
      case RESOURCE:
        return ((BackstageCatalogResourceEntity) entity).getSpec().getOwner();
      case DOMAIN:
        return ((BackstageCatalogDomainEntity) entity).getSpec().getOwner();
      case SYSTEM:
        return ((BackstageCatalogSystemEntity) entity).getSpec().getOwner();
      case GROUP:
        return ((BackstageCatalogGroupEntity) entity).getSpec().getOwner();
      case TEMPLATE:
        return ((BackstageCatalogTemplateEntity) entity).getSpec().getOwner();
      default:
        return null;
    }
  }

  public static String getEntityDomain(BackstageCatalogEntity entity) {
    switch (BackstageCatalogEntityTypes.fromString(entity.getKind())) {
      case COMPONENT:
        return ((BackstageCatalogComponentEntity) entity).getSpec().getDomain();
      case SYSTEM:
        return ((BackstageCatalogSystemEntity) entity).getSpec().getDomain();
      default:
        return null;
    }
  }

  public static String getEntitySystem(BackstageCatalogEntity entity) {
    switch (BackstageCatalogEntityTypes.fromString(entity.getKind())) {
      case API:
        return ((BackstageCatalogApiEntity) entity).getSpec().getSystem();
      case COMPONENT:
        return ((BackstageCatalogComponentEntity) entity).getSpec().getSystem();
      case RESOURCE:
        return ((BackstageCatalogResourceEntity) entity).getSpec().getSystem();
      default:
        return null;
    }
  }

  public static String getEntityLifecycle(BackstageCatalogEntity entity) {
    switch (BackstageCatalogEntityTypes.fromString(entity.getKind())) {
      case API:
        return ((BackstageCatalogApiEntity) entity).getSpec().getLifecycle();
      case COMPONENT:
        return ((BackstageCatalogComponentEntity) entity).getSpec().getLifecycle();
      default:
        return null;
    }
  }
}
