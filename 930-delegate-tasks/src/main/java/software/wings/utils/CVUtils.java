package software.wings.utils;

public class CVUtils {
  public static String appendPathToBaseUrl(String baseUrl, String path) {
    if (baseUrl.charAt(baseUrl.length() - 1) != '/') {
      baseUrl += '/';
    }
    if (path.length() > 0 && path.charAt(0) == '/') {
      path = path.substring(1);
    }
    return baseUrl + path;
  }
}
