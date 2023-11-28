/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.artifactbundle;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.delegate.task.artifacts.ArtifactSourceType.ARTIFACTORY_REGISTRY;
import static io.harness.filesystem.FileIo.deleteDirectoryAndItsContentIfExists;
import static io.harness.rule.OwnerRule.NAMAN_TALAYCHA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FileData;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.artifactBundle.ArtifactBundleDelegateConfig;
import io.harness.delegate.task.artifactBundle.ArtifactBundleDetails;
import io.harness.delegate.task.artifactBundle.ArtifactBundleFetchTaskHelper;
import io.harness.delegate.task.artifactBundle.ArtifactBundledArtifactType;
import io.harness.delegate.task.artifactBundle.PackageArtifactConfig;
import io.harness.delegate.task.cf.CfCommandTaskHelperNG;
import io.harness.delegate.task.cf.TasArtifactDownloadResponse;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;

import software.wings.utils.ArtifactType;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

@OwnedBy(CDP)
public class ArtifactBundleFetchTaskHelperTest extends CategoryTest {
  @InjectMocks @Spy CfCommandTaskHelperNG cfCommandTaskHelperNG;
  @InjectMocks ArtifactBundleFetchTaskHelper artifactBundleFetchTaskHelper;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }
  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testDownloadArtifactFile() {
    LogCallback logCallback = mock(LogCallback.class);
    TasArtifactDownloadResponse tasArtifactDownloadResponse = TasArtifactDownloadResponse.builder()
                                                                  .artifactType(ArtifactType.TAR)
                                                                  .artifactFile(new File("artifactFile"))
                                                                  .build();
    doReturn(tasArtifactDownloadResponse).when(cfCommandTaskHelperNG).downloadPackageArtifact(any(), any());
    PackageArtifactConfig packageArtifactConfig =
        PackageArtifactConfig.builder().sourceType(ARTIFACTORY_REGISTRY).build();
    File artifactFile = artifactBundleFetchTaskHelper.downloadArtifactFile(
        packageArtifactConfig, new File("workingDirectory"), logCallback);

    assertThat(artifactFile).isNotNull();
    assertThat(artifactFile.toPath().getFileName().toString()).isEqualTo("artifactFile");
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testGetManifestFilesFromArtifactBundleTypeTAR() throws Exception {
    ClassLoader classLoader = ArtifactBundleFetchTaskHelperTest.class.getClassLoader();
    LogCallback logCallback = mock(LogCallback.class);
    Path temporaryDir = Files.createTempDirectory("testDir");
    Path destinationPath = Paths.get(temporaryDir.toString(), "artifactBundle.tar").normalize();
    Files.copy(classLoader.getResourceAsStream("task/artifactbundle/artifactBundle.tar"), destinationPath,
        StandardCopyOption.REPLACE_EXISTING);

    File workingDir = new File(temporaryDir.toString());

    File artifactBundleFile = new File(destinationPath.toString());
    ArtifactBundleDelegateConfig artifactBundleDelegateConfig =
        ArtifactBundleDelegateConfig.builder()
            .identifier("0")
            .artifactBundleType(ArtifactBundledArtifactType.TAR.toString())
            .filePaths(List.of("artifactBundle/tas/manifest.yaml", "artifactBundle/tas/vars.yaml"))
            .build();

    Map<String, List<FileData>> filesFromArtifactBundle =
        artifactBundleFetchTaskHelper.getManifestFilesFromArtifactBundle(
            workingDir, artifactBundleFile, artifactBundleDelegateConfig, "activityId", logCallback);
    assertThat(filesFromArtifactBundle).isNotNull();
    assertThat(filesFromArtifactBundle.get("0").size()).isEqualTo(2);
    assertThat(filesFromArtifactBundle.get("0").get(0).getFilePath()).isEqualTo("artifactBundle/tas/manifest.yaml");
    assertThat(filesFromArtifactBundle.get("0").get(0).getFileName()).isEqualTo("manifest.yaml");
    assertThat(filesFromArtifactBundle.get("0").get(1).getFilePath()).isEqualTo("artifactBundle/tas/vars.yaml");
    assertThat(filesFromArtifactBundle.get("0").get(1).getFileName()).isEqualTo("vars.yaml");
    deleteDirectoryAndItsContentIfExists(temporaryDir.toString());
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testGetManifestFilesFromArtifactBundleTypeZIP() throws Exception {
    ClassLoader classLoader = ArtifactBundleFetchTaskHelperTest.class.getClassLoader();
    LogCallback logCallback = mock(LogCallback.class);
    Path temporaryDir = Files.createTempDirectory("testDir");
    Path destinationPath = Paths.get(temporaryDir.toString(), "artifactBundleZip.zip").normalize();
    Files.copy(classLoader.getResourceAsStream("task/artifactbundle/artifactBundleZip.zip"), destinationPath,
        StandardCopyOption.REPLACE_EXISTING);

    File workingDir = new File(temporaryDir.toString());

    File artifactBundleFile = new File(destinationPath.toString());
    ArtifactBundleDelegateConfig artifactBundleDelegateConfig =
        ArtifactBundleDelegateConfig.builder()
            .identifier("0")
            .artifactBundleType(ArtifactBundledArtifactType.ZIP.toString())
            .filePaths(List.of("artifactBundle/tas/manifest.yaml", "artifactBundle/tas/vars.yaml"))
            .build();

    Map<String, List<FileData>> filesFromArtifactBundle =
        artifactBundleFetchTaskHelper.getManifestFilesFromArtifactBundle(
            workingDir, artifactBundleFile, artifactBundleDelegateConfig, "activityId", logCallback);
    assertThat(filesFromArtifactBundle).isNotNull();
    assertThat(filesFromArtifactBundle.get("0").size()).isEqualTo(2);
    assertThat(filesFromArtifactBundle.get("0").get(0).getFilePath()).isEqualTo("artifactBundle/tas/manifest.yaml");
    assertThat(filesFromArtifactBundle.get("0").get(0).getFileName()).isEqualTo("manifest.yaml");
    assertThat(filesFromArtifactBundle.get("0").get(1).getFilePath()).isEqualTo("artifactBundle/tas/vars.yaml");
    assertThat(filesFromArtifactBundle.get("0").get(1).getFileName()).isEqualTo("vars.yaml");
    deleteDirectoryAndItsContentIfExists(temporaryDir.toString());
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testGetManifestFilesFromArtifactBundleTypeTarGz() throws Exception {
    ClassLoader classLoader = ArtifactBundleFetchTaskHelperTest.class.getClassLoader();
    LogCallback logCallback = mock(LogCallback.class);
    Path temporaryDir = Files.createTempDirectory("testDir");
    Path destinationPath = Paths.get(temporaryDir.toString(), "artifactBundleGz.tar.gz").normalize();
    Files.copy(classLoader.getResourceAsStream("task/artifactbundle/artifactBundleGz.tar.gz"), destinationPath,
        StandardCopyOption.REPLACE_EXISTING);

    File workingDir = new File(temporaryDir.toString());

    File artifactBundleFile = new File(destinationPath.toString());
    ArtifactBundleDelegateConfig artifactBundleDelegateConfig =
        ArtifactBundleDelegateConfig.builder()
            .identifier("0")
            .artifactBundleType(ArtifactBundledArtifactType.TAR_GZIP.toString())
            .filePaths(List.of("artifactBundle/tas/manifest.yaml", "artifactBundle/tas/vars.yaml"))
            .build();

    Map<String, List<FileData>> filesFromArtifactBundle =
        artifactBundleFetchTaskHelper.getManifestFilesFromArtifactBundle(
            workingDir, artifactBundleFile, artifactBundleDelegateConfig, "activityId", logCallback);
    assertThat(filesFromArtifactBundle).isNotNull();
    assertThat(filesFromArtifactBundle.get("0").size()).isEqualTo(2);
    assertThat(filesFromArtifactBundle.get("0").get(0).getFilePath()).isEqualTo("artifactBundle/tas/manifest.yaml");
    assertThat(filesFromArtifactBundle.get("0").get(0).getFileName()).isEqualTo("manifest.yaml");
    assertThat(filesFromArtifactBundle.get("0").get(1).getFilePath()).isEqualTo("artifactBundle/tas/vars.yaml");
    assertThat(filesFromArtifactBundle.get("0").get(1).getFileName()).isEqualTo("vars.yaml");
    deleteDirectoryAndItsContentIfExists(temporaryDir.toString());
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testGetManifestFilesFromArtifactBundleWithFolderPath() throws Exception {
    ClassLoader classLoader = ArtifactBundleFetchTaskHelperTest.class.getClassLoader();
    LogCallback logCallback = mock(LogCallback.class);
    Path temporaryDir = Files.createTempDirectory("testDir");
    Path destinationPath = Paths.get(temporaryDir.toString(), "artifactBundleGz.tar.gz").normalize();
    Files.copy(classLoader.getResourceAsStream("task/artifactbundle/artifactBundleGz.tar.gz"), destinationPath,
        StandardCopyOption.REPLACE_EXISTING);

    File workingDir = new File(temporaryDir.toString());

    File artifactBundleFile = new File(destinationPath.toString());
    ArtifactBundleDelegateConfig artifactBundleDelegateConfig =
        ArtifactBundleDelegateConfig.builder()
            .identifier("0")
            .artifactBundleType(ArtifactBundledArtifactType.TAR_GZIP.toString())
            .filePaths(List.of("artifactBundle/tas/"))
            .build();

    Map<String, List<FileData>> filesFromArtifactBundle =
        artifactBundleFetchTaskHelper.getManifestFilesFromArtifactBundle(
            workingDir, artifactBundleFile, artifactBundleDelegateConfig, "activityId", logCallback);
    assertThat(filesFromArtifactBundle).isNotNull();
    assertThat(filesFromArtifactBundle.get("0").size()).isEqualTo(2);
    deleteDirectoryAndItsContentIfExists(temporaryDir.toString());
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void getArtifactPathOfArtifactBundle() throws Exception {
    ClassLoader classLoader = ArtifactBundleFetchTaskHelperTest.class.getClassLoader();
    LogCallback logCallback = mock(LogCallback.class);
    Path temporaryDir = Files.createTempDirectory("testDir");
    Path destinationPath = Paths.get(temporaryDir.toString(), "artifactBundleCorrect.tar").normalize();
    Files.copy(classLoader.getResourceAsStream("task/artifactbundle/artifactBundleCorrect.tar"), destinationPath,
        StandardCopyOption.REPLACE_EXISTING);

    File workingDir = new File(temporaryDir.toString());
    ArtifactBundleDetails artifactBundleDetails = ArtifactBundleDetails.builder()
                                                      .deployableUnitPath("artifactBundle/todolist-4.0.war")
                                                      .artifactBundleType(ArtifactBundledArtifactType.TAR.toString())
                                                      .activityId("activityId")
                                                      .build();
    File artifactBundleFile = new File(destinationPath.toString());

    String artifactBundlePath = artifactBundleFetchTaskHelper.getArtifactPathOfArtifactBundle(
        artifactBundleDetails, artifactBundleFile, workingDir, logCallback);
    assertThat(artifactBundlePath).isNotNull();
    assertThat(artifactBundlePath)
        .isEqualTo(
            Paths.get(temporaryDir.toString(), "/artifactBundleManifests-activityId/artifactBundle/todolist-4.0.war")
                .normalize()
                .toString());
    deleteDirectoryAndItsContentIfExists(temporaryDir.toString());
  }
}
