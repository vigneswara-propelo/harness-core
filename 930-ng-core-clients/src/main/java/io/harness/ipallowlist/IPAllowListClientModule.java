/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ipallowlist;

import io.harness.remote.client.ClientMode;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.*;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class IPAllowListClientModule extends AbstractModule {
  private final ServiceHttpClientConfig serviceHttpClientConfig;
  private final String serviceSecret;
  private final String clientId;

  public IPAllowListClientModule(
      ServiceHttpClientConfig serviceHttpClientConfig, String serviceSecret, String clientId) {
    this.serviceHttpClientConfig = serviceHttpClientConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
  }
  @Provides
  @Named("PRIVILEGED")
  @Singleton
  public IPAllowListHttpClientFactory privilegedIPAllowListHttpClientFactory(
      KryoConverterFactory kryoConverterFactory) {
    return new IPAllowListHttpClientFactory(serviceHttpClientConfig, serviceSecret, new ServiceTokenGenerator(),
        kryoConverterFactory, clientId, ClientMode.PRIVILEGED);
  }
  @Provides
  @Singleton
  public IPAllowListHttpClientFactory nonPrivilegedIPAllowListClientFactory(KryoConverterFactory kryoConverterFactory) {
    return new IPAllowListHttpClientFactory(serviceHttpClientConfig, serviceSecret, new ServiceTokenGenerator(),
        kryoConverterFactory, clientId, ClientMode.NON_PRIVILEGED);
  }

  @Override
  protected void configure() {
    bind(IPAllowListClient.class).toProvider(IPAllowListHttpClientFactory.class).in(Scopes.SINGLETON);
    bind(IPAllowListClient.class)
        .annotatedWith(Names.named(ClientMode.PRIVILEGED.name()))
        .toProvider(Key.get(IPAllowListHttpClientFactory.class, Names.named(ClientMode.PRIVILEGED.name())))
        .in(Scopes.SINGLETON);
  }
}
