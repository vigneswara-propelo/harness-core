/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pcf.cfsdk;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ExceptionUtils;
import io.harness.pcf.PivotalClientApiException;
import io.harness.pcf.model.CfRequestConfig;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.cloudfoundry.reactor.tokenprovider.RefreshTokenGrantTokenProvider;

@Singleton
@Slf4j
@OwnedBy(CDP)
public class CloudFoundryClientProvider {
  public CloudFoundryClient getCloudFoundryClient(CfRequestConfig pcfRequestConfig, ConnectionContext connectionContext)
      throws PivotalClientApiException {
    return ReactorCloudFoundryClient.builder()
        .connectionContext(connectionContext)
        .tokenProvider(getTokenProvider(
            pcfRequestConfig.getUserName(), pcfRequestConfig.getPassword(), pcfRequestConfig.getRefreshToken()))
        .build();
  }

  private TokenProvider getTokenProvider(String username, String password, String refreshToken)
      throws PivotalClientApiException {
    try {
      if (StringUtils.isNotBlank(refreshToken)) {
        log.debug("Generating API TAS Token provider using refresh token");
        return RefreshTokenGrantTokenProvider.builder().token(refreshToken).build();
      }
      log.debug("Generating API TAS Token provider using username and password");
      return PasswordGrantTokenProvider.builder().username(username).password(password).build();
    } catch (Exception t) {
      throw new PivotalClientApiException(ExceptionUtils.getMessage(t));
    }
  }
}
