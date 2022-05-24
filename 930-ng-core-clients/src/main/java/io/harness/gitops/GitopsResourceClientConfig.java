package io.harness.gitops;

import static io.harness.annotations.dev.HarnessTeam.GITOPS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.secret.ConfigSecret;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(GITOPS)
public class GitopsResourceClientConfig {
  @JsonProperty("config") ServiceHttpClientConfig clientConfig;
  @ConfigSecret @JsonProperty("secret") String serviceSecret;
}
