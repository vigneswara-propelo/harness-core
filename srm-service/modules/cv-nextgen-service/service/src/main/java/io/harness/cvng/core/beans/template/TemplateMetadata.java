/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.beans.template;

import java.util.Optional;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "TemplateMetadataKeys")
public class TemplateMetadata {
  @NotNull String templateIdentifier;
  String versionLabel;
  int templateVersionNumber;
  String templateInputs;
  boolean isTemplateByReference;
  long lastReconciliationTime;

  public static TemplateMetadataBuilder fromTemplateDTO(TemplateDTO templateDTO) {
    return TemplateMetadata.builder()
        .templateIdentifier(templateDTO.getTemplateRef())
        .versionLabel(templateDTO.getVersionLabel())
        .templateVersionNumber(Optional.ofNullable(templateDTO.getTemplateVersionNumber()).orElse(0))
        .templateInputs(templateDTO.getTemplateInputs())
        .isTemplateByReference(templateDTO.getIsTemplateByReference());
  }
}
