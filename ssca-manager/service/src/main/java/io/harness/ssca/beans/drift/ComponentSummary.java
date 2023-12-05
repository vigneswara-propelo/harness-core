/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.beans.drift;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.data.mongodb.core.mapping.Field;

@Value
@Builder
@EqualsAndHashCode
@OwnedBy(HarnessTeam.SSCA)
public class ComponentSummary {
  @Field("packagename") String packageName;
  @Field("packageversion") String packageVersion;
  @Field("packagesuppliername") String packageSupplierName;
  @Field("packagemanager") String packageManager;
  String purl;
  @Field("packagelicense") List<String> packageLicense;
}
