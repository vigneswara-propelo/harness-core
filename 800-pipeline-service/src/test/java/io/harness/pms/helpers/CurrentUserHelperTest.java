package io.harness.pms.helpers;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.security.dto.Principal;
import io.harness.security.dto.UserPrincipal;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class CurrentUserHelperTest extends CategoryTest {
  @InjectMocks CurrentUserHelper currentUserHelper;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetPrincipalFromSecurityContextNull() {
    assertThatThrownBy(() -> currentUserHelper.getPrincipalFromSecurityContext())
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Unable to fetch current user");
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetPrincipalFromSecurityContext() {
    Principal principal = new UserPrincipal("1", "", "", "acc");

    SourcePrincipalContextBuilder.setSourcePrincipal(principal);
    assertThat(currentUserHelper.getPrincipalFromSecurityContext()).isNotNull();
    assertThat(currentUserHelper.getPrincipalFromSecurityContext()).isEqualTo(principal);
  }
}
