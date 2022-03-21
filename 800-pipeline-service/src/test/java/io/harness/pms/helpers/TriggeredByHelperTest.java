package io.harness.pms.helpers;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.plan.TriggeredBy;
import io.harness.rule.Owner;
import io.harness.security.dto.Principal;
import io.harness.security.dto.UserPrincipal;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TriggeredByHelperTest extends CategoryTest {
  @Mock CurrentUserHelper currentUserHelper;
  @InjectMocks TriggeredByHelper triggeredByHelper;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetFromSecurityContext() {
    when(currentUserHelper.getPrincipalFromSecurityContext()).thenReturn(null);
    assertThat(triggeredByHelper.getFromSecurityContext()).isNotNull();
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetPrincipalFromSecurityContext() {
    Principal principal = new UserPrincipal("1", "e", "u", "acc");
    when(currentUserHelper.getPrincipalFromSecurityContext()).thenReturn(principal);
    assertThat(triggeredByHelper.getFromSecurityContext()).isNotNull();
    TriggeredBy triggeredBy = triggeredByHelper.getFromSecurityContext();
    assertThat(triggeredBy.getIdentifier()).isEqualTo("u");
    assertThat(triggeredBy.getExtraInfoCount()).isEqualTo(1);
    assertThat(triggeredBy.getExtraInfoMap().get("email")).isEqualTo("e");
  }
}
