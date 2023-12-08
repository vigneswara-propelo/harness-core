/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;

import java.util.Objects;
import javax.validation.constraints.NotNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Request;
import org.slf4j.MDC;

@OwnedBy(PL)
@Slf4j
@UtilityClass
public class ScopeInfoContext {
  public static final String SCOPE_INFO_ACCOUNT_CONTEXT_PROPERTY = "scopeinfo-account";
  public static final String SCOPE_INFO_ORG_CONTEXT_PROPERTY = "scopeinfo-org";
  public static final String SCOPE_INFO_PROJECT_CONTEXT_PROPERTY = "scopeinfo-project";
  public static final String SCOPE_INFO_SCOPE_TYPE_CONTEXT_PROPERTY = "scopeinfo-scopetype";
  public static final String SCOPE_INFO_UNIQUE_ID_CONTEXT_PROPERTY = "scopeinfo-uniqueid";

  public static ScopeInfo getScopeInfo() {
    String accountIdentifier = MDC.get(SCOPE_INFO_ACCOUNT_CONTEXT_PROPERTY);
    String orgIdentifier = MDC.get(SCOPE_INFO_ORG_CONTEXT_PROPERTY);
    String projectIdentifier = MDC.get(SCOPE_INFO_PROJECT_CONTEXT_PROPERTY);
    String scopeType = MDC.get(SCOPE_INFO_SCOPE_TYPE_CONTEXT_PROPERTY);
    String uniqueId = MDC.get(SCOPE_INFO_UNIQUE_ID_CONTEXT_PROPERTY);
    ScopeInfo.ScopeInfoBuilder scopeInfoBuilder = ScopeInfo.builder();

    if (EmptyPredicate.isNotEmpty(accountIdentifier)) {
      scopeInfoBuilder.accountIdentifier(accountIdentifier);
    }
    if (EmptyPredicate.isNotEmpty(orgIdentifier)) {
      scopeInfoBuilder.orgIdentifier(orgIdentifier);
    }
    if (EmptyPredicate.isNotEmpty(projectIdentifier)) {
      scopeInfoBuilder.projectIdentifier(projectIdentifier);
    }
    if (Objects.nonNull(scopeType)) {
      scopeInfoBuilder.scopeType(ScopeLevel.valueOf(scopeType));
    }
    if (EmptyPredicate.isNotEmpty(uniqueId)) {
      scopeInfoBuilder.uniqueId(uniqueId);
    }
    return scopeInfoBuilder.build();
  }

  public static void setScopeInfo(ScopeInfo scopeInfo) {
    if (EmptyPredicate.isNotEmpty(scopeInfo.getAccountIdentifier())) {
      MDC.put(SCOPE_INFO_ACCOUNT_CONTEXT_PROPERTY, scopeInfo.getAccountIdentifier());
    }
    if (EmptyPredicate.isNotEmpty(scopeInfo.getOrgIdentifier())) {
      MDC.put(SCOPE_INFO_ORG_CONTEXT_PROPERTY, scopeInfo.getOrgIdentifier());
    }
    if (EmptyPredicate.isNotEmpty(scopeInfo.getProjectIdentifier())) {
      MDC.put(SCOPE_INFO_PROJECT_CONTEXT_PROPERTY, scopeInfo.getProjectIdentifier());
    }
    if (EmptyPredicate.isNotEmpty(scopeInfo.getUniqueId())) {
      MDC.put(SCOPE_INFO_UNIQUE_ID_CONTEXT_PROPERTY, scopeInfo.getUniqueId());
    }
    if (Objects.nonNull(scopeInfo.getScopeType())) {
      MDC.put(SCOPE_INFO_SCOPE_TYPE_CONTEXT_PROPERTY, scopeInfo.getScopeType().name());
    }
  }

  public static void clearScopeInfo() {
    MDC.remove(SCOPE_INFO_ACCOUNT_CONTEXT_PROPERTY);
    MDC.remove(SCOPE_INFO_ORG_CONTEXT_PROPERTY);
    MDC.remove(SCOPE_INFO_PROJECT_CONTEXT_PROPERTY);
    MDC.remove(SCOPE_INFO_SCOPE_TYPE_CONTEXT_PROPERTY);
    MDC.remove(SCOPE_INFO_UNIQUE_ID_CONTEXT_PROPERTY);
  }

  @NotNull
  public static Interceptor getScopeInfoInterceptor() {
    return chain -> {
      ScopeInfo scopeInfo = getScopeInfo();
      Request.Builder requestBuilder = chain.request().newBuilder();

      if (EmptyPredicate.isNotEmpty(scopeInfo.getAccountIdentifier())) {
        requestBuilder.addHeader(SCOPE_INFO_ACCOUNT_CONTEXT_PROPERTY, scopeInfo.getAccountIdentifier());
      }
      if (EmptyPredicate.isNotEmpty(scopeInfo.getOrgIdentifier())) {
        requestBuilder.addHeader(SCOPE_INFO_ORG_CONTEXT_PROPERTY, scopeInfo.getOrgIdentifier());
      }
      if (EmptyPredicate.isNotEmpty(scopeInfo.getProjectIdentifier())) {
        requestBuilder.addHeader(SCOPE_INFO_PROJECT_CONTEXT_PROPERTY, scopeInfo.getProjectIdentifier());
      }
      if (Objects.nonNull(scopeInfo.getScopeType())) {
        requestBuilder.addHeader(SCOPE_INFO_SCOPE_TYPE_CONTEXT_PROPERTY, scopeInfo.getScopeType().name());
      }
      if (EmptyPredicate.isNotEmpty(scopeInfo.getUniqueId())) {
        requestBuilder.addHeader(SCOPE_INFO_UNIQUE_ID_CONTEXT_PROPERTY, scopeInfo.getUniqueId());
      }

      return chain.proceed(requestBuilder.build());
    };
  }
}
