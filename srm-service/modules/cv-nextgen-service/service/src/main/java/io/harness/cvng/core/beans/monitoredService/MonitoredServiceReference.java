/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.beans.monitoredService;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.codehaus.commons.nullanalysis.NotNull;

@Data
@Builder
@AllArgsConstructor
public class MonitoredServiceReference {
  @NotNull String orgIdentifier;
  @NotNull String projectIdentifier;
  @NotNull String accountIdentifier;
  @NotNull String identifier;
  String serviceIdentifier;
  List<String> environmentIdentifiers;
  long lastReconciledTimestamp;
  ReconciliationStatus reconciliationStatus;
}
