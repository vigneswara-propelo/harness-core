package io.harness.delegate.task.utils;

import static io.harness.delegate.task.utils.PhysicalDataCenterConstants.DEFAULT_SSH_PORT;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Optional;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PhysicalDataCenterUtils {
  public String getPortOrSSHDefault(final String host) {
    return extractPortFromHost(host).map(port -> Integer.toString(port)).orElse(DEFAULT_SSH_PORT);
  }

  public Optional<Integer> extractPortFromHost(final String host) {
    if (isBlank(host)) {
      return Optional.empty();
    }

    String[] hostParts = host.split(":");
    if (hostParts.length == 2) {
      try {
        return Optional.of(Integer.parseInt(hostParts[1]));
      } catch (NumberFormatException nfe) {
        return Optional.empty();
      }
    } else {
      return Optional.empty();
    }
  }

  public Optional<String> extractHostnameFromHost(final String host) {
    if (isBlank(host)) {
      return Optional.empty();
    }

    String[] hostParts = host.split(":");
    if (hostParts.length == 2) {
      return isBlank(hostParts[0]) ? Optional.empty() : Optional.of(hostParts[0]);
    }

    return Optional.of(host);
  }
}
