package io.harness.ng.core.filestore.utils;

import static io.harness.rule.OwnerRule.VLAD;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.EntityType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.beans.SearchPageParams;
import io.harness.ng.core.entities.NGFile;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.ng.core.entitysetupusage.service.EntitySetupUsageService;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

@OwnedBy(HarnessTeam.CDP)
@RunWith(MockitoJUnitRunner.class)
public class FileReferencedByHelperTest {
  private static final String ACCOUNT_IDENTIFIER = "accountIdentifier";

  @Mock private EntitySetupUsageService entitySetupUsageService;

  @InjectMocks private FileReferencedByHelper fileReferencedByHelper;

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldVerifyFileNotReferencedByOtherEntities() {
    NGFile file = NGFile.builder().identifier("testFile").accountIdentifier(ACCOUNT_IDENTIFIER).build();
    boolean result = fileReferencedByHelper.isFileReferencedByOtherEntities(file);
    assertThat(result).isFalse();
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldVerifyFileIsReferencedByOtherEntities() {
    String identifier = "testFile";
    NGFile file = NGFile.builder().identifier(identifier).accountIdentifier(ACCOUNT_IDENTIFIER).build();
    when(entitySetupUsageService.isEntityReferenced(
             ACCOUNT_IDENTIFIER, ACCOUNT_IDENTIFIER + "/" + identifier, EntityType.FILES))
        .thenReturn(true);
    boolean result = fileReferencedByHelper.isFileReferencedByOtherEntities(file);
    assertThat(result).isTrue();
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldFetchReferencedBy() {
    String identifier = "testFile";
    NGFile file = NGFile.builder().identifier(identifier).accountIdentifier(ACCOUNT_IDENTIFIER).build();
    io.harness.ng.core.beans.SearchPageParams searchPageParams = SearchPageParams.builder().page(1).size(10).build();
    Page<EntitySetupUsageDTO> references = mock(Page.class);
    when(entitySetupUsageService.listAllEntityUsage(searchPageParams.getPage(), searchPageParams.getSize(),
             file.getAccountIdentifier(), ACCOUNT_IDENTIFIER + "/" + identifier, EntityType.FILES, EntityType.PIPELINES,
             searchPageParams.getSearchTerm(),
             Sort.by(Sort.Direction.ASC,
                 io.harness.ng.core.filestore.utils.FileReferencedByHelper.REFFERED_BY_IDENTIFIER_KEY)))
        .thenReturn(references);
    Page<EntitySetupUsageDTO> result =
        fileReferencedByHelper.getReferencedBy(searchPageParams, file, EntityType.PIPELINES);
    assertThat(result).isEqualTo(references);
  }
}
