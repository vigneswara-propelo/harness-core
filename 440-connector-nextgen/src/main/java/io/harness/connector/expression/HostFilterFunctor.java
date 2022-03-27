package io.harness.connector.expression;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.join;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.pdcconnector.HostDTO;
import io.harness.expression.ExpressionFunctor;

import java.util.Arrays;
import java.util.List;
import lombok.NoArgsConstructor;

@OwnedBy(CDP)
@NoArgsConstructor
public class HostFilterFunctor implements ExpressionFunctor {
  private static final String COMMA_AND_NEW_LINE_SEPARATOR_REGEX = "[\n,]";
  private static final String COLON_SEPARATOR = ":";

  public boolean filterByHostName(String filter, HostDTO host) {
    return Arrays.stream(filter.split(COMMA_AND_NEW_LINE_SEPARATOR_REGEX))
        .map(String::trim)
        .collect(toList())
        .contains(host.getHostName());
  }

  public boolean filterByHostAttributes(String filter, HostDTO host) {
    List<String> hostFilters =
        Arrays.stream(filter.split(COMMA_AND_NEW_LINE_SEPARATOR_REGEX)).map(String::trim).collect(toList());

    return host.getHostAttributes()
        .entrySet()
        .stream()
        .map(hostAttr -> join(new String[] {hostAttr.getKey(), hostAttr.getValue()}, COLON_SEPARATOR))
        .anyMatch(hostFilters::contains);
  }
}
