package io.harness.gitsync.common.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserRepoResponse {
  String namespace;
  String name;
}
