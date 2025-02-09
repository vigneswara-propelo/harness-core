/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.servicelevelobjective.entities;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SLIRecordParam {
  private SLIState sliState;
  private Instant timeStamp;
  private Long goodEventCount;
  private Long badEventCount;

  private Long skipEventCount;
}
