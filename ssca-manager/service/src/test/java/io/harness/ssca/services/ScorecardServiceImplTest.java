/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.services;

import static io.harness.rule.OwnerRule.SHASHWAT_SACHAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.BuilderFactory;
import io.harness.SSCAManagerTestBase;
import io.harness.category.element.UnitTests;
import io.harness.repositories.ScorecardRepo;
import io.harness.rule.Owner;
import io.harness.spec.server.ssca.v1.model.SbomScorecardRequestBody;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;

public class ScorecardServiceImplTest extends SSCAManagerTestBase {
  @Inject ScorecardService scorecardService;

  @Inject ScorecardRepo scorecardRepository;
  private BuilderFactory builderFactory;

  @Before
  public void setup() throws IllegalAccessException {
    MockitoAnnotations.initMocks(this);
    builderFactory = BuilderFactory.getDefault();
  }

  @Test
  @Owner(developers = SHASHWAT_SACHAN)
  @Category(UnitTests.class)
  public void testSave() {
    SbomScorecardRequestBody sbomScorecardRequestBody = builderFactory.getSbomScorecardRequestBody();
    Boolean response = scorecardService.save(sbomScorecardRequestBody);
    assertThat(response).isEqualTo(true);
  }

  @Test
  @Owner(developers = SHASHWAT_SACHAN)
  @Category(UnitTests.class)
  public void testSaveInvalidRequest() {
    SbomScorecardRequestBody sbomScorecardRequestBody = builderFactory.getSbomScorecardRequestBody();
    sbomScorecardRequestBody.setAccountId(null);
    Boolean response = scorecardService.save(sbomScorecardRequestBody);
    assertThat(response).isEqualTo(false);
  }
}
