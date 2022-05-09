package io.harness.ng.core.mapper;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.ng.core.dto.EmbeddedUserDetailsDTO;

@OwnedBy(CDP)
public class EmbeddedUserDTOMapper {
  public static EmbeddedUserDetailsDTO fromEmbeddedUser(EmbeddedUser user) {
    if (user == null) {
      return null;
    }
    return EmbeddedUserDetailsDTO.builder().name(user.getName()).email(user.getEmail()).build();
  }
}
