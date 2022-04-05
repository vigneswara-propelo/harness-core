package io.harness.delegate.beans;

import static io.harness.annotations.dev.HarnessTeam.DEL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.beans.YamlDTO;

import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@OwnedBy(DEL)
public class DelegateDTO implements YamlDTO {
  private String accountId;
  private String delegateId;
  private String delegateName;

  List<String> tags;

  public static DelegateDTO convertToDTO(Delegate delegate) {
    return DelegateDTO.builder()
        .accountId(delegate.getAccountId())
        .delegateId(delegate.getUuid())
        .delegateName(delegate.getDelegateName())
        .tags(isNotEmpty(delegate.getTags()) ? delegate.getTags() : Collections.emptyList())
        .build();
  }
}
