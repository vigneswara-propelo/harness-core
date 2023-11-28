/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.utils;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import java.net.URL;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class ProxyUtils {
  public String getProxyHost(String urlString) {
    if (isEmpty(urlString)) {
      return null;
    }
    try {
      URL url = new URL(urlString);
      return url.getHost();
    } catch (Exception e) {
      log.error("Unable to parse proxy url to get host", e);
    }
    return null;
  }

  public Integer getProxyPort(String urlString) {
    if (isEmpty(urlString)) {
      return null;
    }
    try {
      URL url = new URL(urlString);
      return url.getPort();
    } catch (Exception e) {
      log.error("Unable to parse proxy url to get port", e);
    }
    return null;
  }
}
