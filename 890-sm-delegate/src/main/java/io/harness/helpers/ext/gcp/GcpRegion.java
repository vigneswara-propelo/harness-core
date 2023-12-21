/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.helpers.ext.gcp;

public enum GcpRegion {
  US_EAST_1("us-east1"),
  US_EAST_5("us-east5"),
  US_EAST_4("us-east4"),
  US_WEST_1("us-west1"),
  US_WEST_2("us-west2"),
  US_WEST_3("us-west3"),
  US_WEST_4("us-west4"),
  US_CENTRAL_1("us-central1"),
  US_SOUTH_1("us-south1"),

  EU_WEST_1("europe-west1"),
  EU_WEST_2("europe-west2"),
  EU_WEST_3("europe-west3"),
  EU_WEST_4("europe-west4"),
  EU_WEST_6("europe-west6"),
  EU_WEST_8("europe-west8"),
  EU_WEST_9("europe-west9"),
  EU_WEST_10("europe-west10"),
  EU_WEST_12("europe-west12"),
  EU_NORTH_1("europe-north1"),
  EU_CENTRAL_2("europe-central2"),

  ASIA_EAST_1("asia-east1"),
  ASIA_EAST_2("asia-east2"),

  ASIA_NORTHEAST_1("asia-northeast1"),
  ASIA_NORTHEAST_2("asia-northeast2"),
  ASIA_NORTHEAST_3("asia-northeast3"),

  ASIA_SOUTHEAST_1("asia-southeast1"),
  ASIA_SOUTHEAST_2("asia-southeast2"),

  ASIA_SOUTH_1("asia-south1"),
  ASIA_SOUTH_2("asia-south2"),

  AUSTRALIA_SOUTHEAST_1("australia-southeast1"),
  AUSTRALIA_SOUTHEAST_2("australia-southeast2"),

  NA_NORTHEAST_1("northamerica-northeast1"),
  NA_NORTHEAST_2("northamerica-northeast2"),
  SA_EAST_1("southamerica-east1"),
  SA_WEST_1("southamerica-west1"),
  ME_CENTRAL_2("me-central2"),
  ME_CENTRAL_1("me-central1"),
  ME_WEST_1("me-west1");

  public static final GcpRegion DEFAULT_REGION = US_WEST_2;
  private final String name;

  GcpRegion(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }

  public static GcpRegion fromName(String regionName) {
    GcpRegion[] values = values();
    for (GcpRegion region : values) {
      if (region.getName().equals(regionName)) {
        return region;
      }
    }
    throw new IllegalArgumentException("Cannot create enum from " + regionName + " value!");
  }
}
