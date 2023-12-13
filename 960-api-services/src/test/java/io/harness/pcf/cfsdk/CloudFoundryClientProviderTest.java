/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pcf.cfsdk;

import static io.harness.rule.OwnerRule.ANIL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pcf.PivotalClientApiException;
import io.harness.pcf.model.CfRequestConfig;
import io.harness.rule.Owner;

import java.time.Duration;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.cloudfoundry.reactor.tokenprovider.RefreshTokenGrantTokenProvider;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@OwnedBy(HarnessTeam.CDP)
@RunWith(MockitoJUnitRunner.class)
public class CloudFoundryClientProviderTest extends CategoryTest {
  private final CloudFoundryClientProvider cloudFoundryClientProvider = new CloudFoundryClientProvider();

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testPasswordGrantTokenProvider() throws PivotalClientApiException {
    String userName = "dummyUser";
    String password = "dummyPwd";
    String endPointUrl = "api.system.tas-qa-setup.com";

    CfRequestConfig cfRequestConfig = CfRequestConfig.builder()
                                          .timeOutIntervalInMins(1)
                                          .orgName("devTest")
                                          .applicationName("TestApplication")
                                          .build();
    cfRequestConfig.setUserName(userName);
    cfRequestConfig.setPassword(password);
    cfRequestConfig.setEndpointUrl(endPointUrl);

    DefaultConnectionContext connectionContext = DefaultConnectionContext.builder()
                                                     .apiHost(cfRequestConfig.getEndpointUrl())
                                                     .skipSslValidation(true)
                                                     .connectTimeout(Duration.ofMinutes(10))
                                                     .build();

    CloudFoundryClient cloudFoundryClient =
        cloudFoundryClientProvider.getCloudFoundryClient(cfRequestConfig, connectionContext);

    assertThat(cloudFoundryClient instanceof ReactorCloudFoundryClient).isTrue();

    ReactorCloudFoundryClient reactorCloudFoundryClient = (ReactorCloudFoundryClient) cloudFoundryClient;
    TokenProvider tokenProvider = reactorCloudFoundryClient.getTokenProvider();
    assertThat(tokenProvider instanceof PasswordGrantTokenProvider).isTrue();

    PasswordGrantTokenProvider passwordGrantTokenProvider = (PasswordGrantTokenProvider) tokenProvider;
    assertThat(passwordGrantTokenProvider.getUsername().equals(userName)).isTrue();
    assertThat(passwordGrantTokenProvider.getPassword().equals(password)).isTrue();

    assertThat(reactorCloudFoundryClient.getConnectionContext() instanceof DefaultConnectionContext).isTrue();
    DefaultConnectionContext foundryClientConnectionContext =
        (DefaultConnectionContext) reactorCloudFoundryClient.getConnectionContext();
    assertThat(foundryClientConnectionContext.getApiHost().equals(endPointUrl)).isTrue();
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testRefreshTokenGrantTokenProvider() throws PivotalClientApiException {
    String userName = "dummyUser";
    String password = "dummyPwd";
    String endPointUrl = "api.system.tas-qa-setup.com";
    String refreshToken = "dummyValue";

    CfRequestConfig cfRequestConfig = CfRequestConfig.builder()
                                          .timeOutIntervalInMins(1)
                                          .orgName("devTest")
                                          .applicationName("TestApplication")
                                          .build();
    cfRequestConfig.setUserName(userName);
    cfRequestConfig.setPassword(password);
    cfRequestConfig.setRefreshToken(refreshToken);
    cfRequestConfig.setEndpointUrl(endPointUrl);

    DefaultConnectionContext connectionContext = DefaultConnectionContext.builder()
                                                     .apiHost(cfRequestConfig.getEndpointUrl())
                                                     .skipSslValidation(true)
                                                     .connectTimeout(Duration.ofMinutes(10))
                                                     .build();

    CloudFoundryClient cloudFoundryClient =
        cloudFoundryClientProvider.getCloudFoundryClient(cfRequestConfig, connectionContext);

    assertThat(cloudFoundryClient instanceof ReactorCloudFoundryClient).isTrue();

    ReactorCloudFoundryClient reactorCloudFoundryClient = (ReactorCloudFoundryClient) cloudFoundryClient;
    TokenProvider tokenProvider = reactorCloudFoundryClient.getTokenProvider();
    assertThat(tokenProvider instanceof RefreshTokenGrantTokenProvider).isTrue();

    RefreshTokenGrantTokenProvider refreshTokenGrantTokenProvider = (RefreshTokenGrantTokenProvider) tokenProvider;
    assertThat(refreshTokenGrantTokenProvider.getToken().equals(refreshToken)).isTrue();

    assertThat(reactorCloudFoundryClient.getConnectionContext() instanceof DefaultConnectionContext).isTrue();
    DefaultConnectionContext foundryClientConnectionContext =
        (DefaultConnectionContext) reactorCloudFoundryClient.getConnectionContext();
    assertThat(foundryClientConnectionContext.getApiHost().equals(endPointUrl)).isTrue();
  }
}