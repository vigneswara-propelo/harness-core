/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.views.utils;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import com.google.common.base.Strings;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@OwnedBy(CE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AwsAccountFieldUtils {
  private static final Pattern ACCOUNT_ID_EXTRACT_PATTERN = Pattern.compile("\\((.*?)\\)");

  public static String mergeAwsAccountIdAndName(final String accountId, final String accountName) {
    String accountDetails = accountId;
    if (!Strings.isNullOrEmpty(accountName)) {
      accountDetails = accountName + " (" + accountId + ")";
    }
    return accountDetails;
  }

  public static String removeAwsAccountNameFromValue(final String value) {
    String accountId = value;
    final Matcher matcher = ACCOUNT_ID_EXTRACT_PATTERN.matcher(value);
    if (matcher.find()) {
      accountId = matcher.group(1);
    }
    return accountId;
  }
}
