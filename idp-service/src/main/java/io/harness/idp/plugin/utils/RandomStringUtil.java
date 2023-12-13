/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.plugin.utils;

import java.security.SecureRandom;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RandomStringUtil {
  private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

  public static String generate(int length) {
    if (length <= 0) {
      throw new IllegalArgumentException("Length must be greater than 0");
    }

    StringBuilder randomString = new StringBuilder(length);
    SecureRandom random = new SecureRandom();

    for (int i = 0; i < length; i++) {
      int randomIndex = random.nextInt(CHARACTERS.length());
      randomString.append(CHARACTERS.charAt(randomIndex));
    }

    return randomString.toString();
  }
}
