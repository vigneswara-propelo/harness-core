package io.harness.ng.core.dto.filestore;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

@Data
@OwnedBy(CDP)
@NoArgsConstructor
@AllArgsConstructor
public class CreatedBy {
  @Id private String createdBy;
}
