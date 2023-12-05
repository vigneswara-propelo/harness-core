/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.beans.drift;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Comparator;
@OwnedBy(HarnessTeam.SSCA)
public class ComponentDriftComparator implements Comparator<ComponentDrift> {
  @Override
  public int compare(ComponentDrift c1, ComponentDrift c2) {
    // Assuming no element is null the list and either of new or old component is not null.
    String c1PackageName =
        c1.getNewComponent() == null ? c1.getOldComponent().getPackageName() : c1.getNewComponent().getPackageName();
    String c2PackageName =
        c2.getNewComponent() == null ? c2.getOldComponent().getPackageName() : c2.getNewComponent().getPackageName();
    return c1PackageName.compareTo(c2PackageName);
  }
}
