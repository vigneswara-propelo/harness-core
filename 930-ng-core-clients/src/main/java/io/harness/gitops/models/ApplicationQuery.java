package io.harness.gitops.models;

import static io.harness.annotations.dev.HarnessTeam.GITOPS;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(GITOPS)
public class ApplicationQuery {
  @JsonProperty("accountIdentifier") String accountId;
  String orgIdentifier;
  String projectIdentifier;
  int pageSize;
  int pageIndex;
  Map<String, Object> filter;
}
