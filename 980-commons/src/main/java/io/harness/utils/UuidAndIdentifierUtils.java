/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.utils;

import static org.apache.commons.codec.binary.Base64.decodeBase64;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public final class UuidAndIdentifierUtils {
  private UuidAndIdentifierUtils() {}
  public static String base64StrToUuid(String str) throws BufferUnderflowException, BufferOverflowException {
    byte[] bytes = decodeBase64(str);
    ByteBuffer bb = ByteBuffer.wrap(bytes);
    UUID uuid = new UUID(bb.getLong(), bb.getLong());
    return uuid.toString();
  }

  public static boolean isValidUuidStr(String value) {
    try {
      UUID.fromString(value);
      return true;
    } catch (Exception e) {
      log.info("{} is not a valid UUID", value);
    }
    return false;
  }

  public static String generateHarnessUIFormatIdentifier(String name) {
    name = generateFormattedIdentifier(name);
    if (StringUtils.isBlank(name)) {
      return "";
    }
    name = StringUtils.stripAccents(name);
    return name.trim()
        .replaceAll("^[0-9-$]*", "") // remove starting digits, dashes and $
        .replaceAll("[^0-9a-zA-Z_$ ]", "") // remove special chars except _ and $
        .replaceAll("\\s", "_"); // replace spaces with _
  }

  public static String generateHarnessUIFormatName(String str) {
    if (StringUtils.isEmpty(str)) {
      return str;
    }
    str = StringUtils.stripAccents(str.trim());
    Pattern p = Pattern.compile("[^-./0-9a-zA-Z_\\s]", Pattern.CASE_INSENSITIVE);
    Matcher m = p.matcher(str);
    String generated = m.replaceAll("_");
    return !Character.isLetter(generated.charAt(0)) ? "_" + generated : generated;
  }

  /**
   * Bring back SCIM usergroup name formatting in lieu of https://harness.atlassian.net/browse/PL-43576
   * This method will convert space, dot and hyphen to underscore.
   * @param name
   * @return
   */
  private static String generateFormattedIdentifier(String name) {
    return StringUtils.isBlank(name) ? name : name.trim().replaceAll("\\.", "_").replaceAll("-", "_");
    // replace dot and hyphen with underscore
  }
}
