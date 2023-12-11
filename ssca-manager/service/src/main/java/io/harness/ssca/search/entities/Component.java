/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.search.entities;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ssca.search.beans.RelationshipType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(HarnessTeam.SSCA)
public class Component {
  String uuid;
  String orchestrationId;
  String sbomVersion;

  String artifactUrl;
  String artifactId;
  String artifactName;
  List<String> tags;
  Long createdOn;

  String toolVersion;
  String toolName;
  String toolVendor;

  String packageId;
  String packageName;
  String packageDescription;
  List<String> packageLicense;
  String packageSourceInfo;
  String packageVersion;

  String packageOriginatorName;
  String originatorType;
  String packageType;
  String packageCpe;
  String packageProperties;
  String purl;
  String packageManager;
  String packageNamespace;

  Integer majorVersion;
  Integer minorVersion;
  Integer patchVersion;

  String pipelineIdentifier;
  String projectIdentifier;
  String orgIdentifier;

  String sequenceId;
  String accountId;

  RelationshipType relation_type;
}
