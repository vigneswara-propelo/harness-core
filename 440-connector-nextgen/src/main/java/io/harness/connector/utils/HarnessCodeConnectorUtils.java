/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.utils;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.harness.HarnessApiAccessDTO;
import io.harness.delegate.beans.connector.scm.harness.HarnessApiAccessType;
import io.harness.delegate.beans.connector.scm.harness.HarnessConnectorDTO;
import io.harness.delegate.beans.connector.scm.harness.HarnessConnectorDTO.HarnessConnectorDTOBuilder;
import io.harness.delegate.beans.connector.scm.harness.HarnessJWTTokenSpecDTO;
import io.harness.encryption.SecretRefData;
import io.harness.git.GitClientHelper;
import io.harness.ng.core.NGAccess;
import io.harness.security.JWTTokenServiceUtils;
import io.harness.security.ServiceTokenGenerator;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Singleton
public class HarnessCodeConnectorUtils {
  @Inject ServiceTokenGenerator tokenGenerator;

  public HarnessConnectorDTO getDummyHarnessCodeConnectorWithJwtAuth(String accountId, String orgId, String projectId,
      String repoName, String serviceSecret, String harnessCodeApiBaseUrl) {
    SecretRefData token = SecretRefData.builder().decryptedValue(getToken(serviceSecret).toCharArray()).build();
    HarnessJWTTokenSpecDTO jwtTokenSpecDTO = HarnessJWTTokenSpecDTO.builder().tokenRef(token).build();
    HarnessConnectorDTOBuilder harnessConnectorDTOBuilder =
        HarnessConnectorDTO.builder()
            .connectionType(GitConnectionType.REPO)
            .apiAccess(HarnessApiAccessDTO.builder().spec(jwtTokenSpecDTO).type(HarnessApiAccessType.JWT_TOKEN).build())
            .connectionType(GitConnectionType.REPO)
            .executeOnDelegate(false)
            .apiUrl(harnessCodeApiBaseUrl);
    if (isEmpty(repoName)) {
      harnessConnectorDTOBuilder.accountId(accountId).orgId(orgId).projectId(projectId);
    } else {
      harnessConnectorDTOBuilder.slug(
          GitClientHelper.convertToHarnessRepoName(accountId, orgId, projectId, repoName) + "/+");
    }
    return harnessConnectorDTOBuilder.build();
  }

  private String getToken(String serviceSecret) {
    return tokenGenerator.getServiceTokenWithDuration(serviceSecret, Duration.ofHours(1));
  }

  public String getTokenWithClaims(
      String serviceSecret, NGAccess ngAccess, String repoName, String principal, String principalType, int expiry) {
    String completeRepoName = GitClientHelper.convertToHarnessRepoName(
        ngAccess.getAccountIdentifier(), ngAccess.getOrgIdentifier(), ngAccess.getProjectIdentifier(), repoName);
    String[] allowedResources = {completeRepoName};
    ImmutableMap<String, String> claims = ImmutableMap.of("name", principal, "type", principalType);
    ImmutableMap<String, String[]> arrayClaims = ImmutableMap.of("allowedResources", allowedResources);
    return JWTTokenServiceUtils.generateJWTToken(
        claims, arrayClaims, TimeUnit.MILLISECONDS.convert(expiry, TimeUnit.HOURS), serviceSecret);
  }
}
