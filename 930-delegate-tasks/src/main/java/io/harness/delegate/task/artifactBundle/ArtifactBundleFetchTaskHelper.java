/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.artifactBundle;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.task.artifactBundle.ArtifactBundledArtifactType.TAR;
import static io.harness.delegate.task.artifactBundle.ArtifactBundledArtifactType.TAR_GZIP;
import static io.harness.delegate.task.artifactBundle.ArtifactBundledArtifactType.ZIP;
import static io.harness.delegate.task.helm.CustomManifestFetchTaskHelper.unzipManifestFiles;
import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;
import static io.harness.filesystem.FileIo.getFilesUnderPath;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.pcf.model.ManifestType.APPLICATION_MANIFEST;
import static io.harness.pcf.model.ManifestType.AUTOSCALAR_MANIFEST;
import static io.harness.pcf.model.ManifestType.VARIABLE_MANIFEST;
import static io.harness.pcf.model.PcfConstants.APPLICATION_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.NAME_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.PCF_AUTOSCALAR_MANIFEST_INSTANCE_LIMITS_ELE;
import static io.harness.pcf.model.PcfConstants.PCF_AUTOSCALAR_MANIFEST_RULES_ELE;
import static io.harness.pcf.model.PcfConstants.REPOSITORY_DIR_PATH;

import static software.wings.beans.LogHelper.color;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.FileData;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.task.cf.CfCommandTaskHelperNG;
import io.harness.delegate.task.cf.TasArtifactDownloadContext;
import io.harness.delegate.task.cf.TasArtifactDownloadResponse;
import io.harness.delegate.task.pcf.artifact.TasPackageArtifactConfig;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.UnexpectedException;
import io.harness.exception.sanitizer.ExceptionMessageSanitizer;
import io.harness.filesystem.FileIo;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import io.harness.pcf.model.ManifestType;

import software.wings.beans.LogColor;
import software.wings.beans.LogWeight;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.inject.Inject;
import com.mchange.v1.db.sql.UnsupportedTypeException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.zip.ZipInputStream;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_PCF})
@Slf4j
@OwnedBy(CDP)
public class ArtifactBundleFetchTaskHelper {
  @Inject protected CfCommandTaskHelperNG cfCommandTaskHelperNG;
  public static final String ARTIFACT_BUNDLE_DOWNLOAD_DIR_PATH = "./repository/artifactBundle";
  public static final String ARTIFACT_BUNDLE_MANIFESTS = "/artifactBundleManifests-";

  public File downloadArtifactFile(
      PackageArtifactConfig packageArtifactConfig, File workingDirectory, LogCallback logCallback) {
    File artifactFile;

    TasArtifactDownloadResponse tasArtifactDownloadResponse = cfCommandTaskHelperNG.downloadPackageArtifact(
        TasArtifactDownloadContext.builder()
            .artifactConfig(toTasPackageArtifactConfig(packageArtifactConfig))
            .workingDirectory(workingDirectory)
            .build(),
        logCallback);
    artifactFile = tasArtifactDownloadResponse.getArtifactFile();

    return artifactFile;
  }

  private TasPackageArtifactConfig toTasPackageArtifactConfig(PackageArtifactConfig packageArtifactConfig) {
    // Convertor this to TasPackageArtifactConfig so that we can use cfCommandTaskHelperNG.downloadPackageArtifact in a
    // Generic way
    if (packageArtifactConfig == null) {
      throw new UnexpectedException("Package Artifact Cannot be null for Artifact Bundle Fetch Task");
    }
    return TasPackageArtifactConfig.builder()
        .connectorConfig(packageArtifactConfig.getConnectorConfig())
        .sourceType(packageArtifactConfig.getSourceType())
        .artifactDetails(packageArtifactConfig.getArtifactDetails())
        .encryptedDataDetails(packageArtifactConfig.getEncryptedDataDetails())
        .build();
  }

  public File generateWorkingDirectoryForDeployment() throws IOException {
    String workingDirecotry = UUIDGenerator.generateUuid();
    createDirectoryIfDoesNotExist(REPOSITORY_DIR_PATH);
    createDirectoryIfDoesNotExist(ARTIFACT_BUNDLE_DOWNLOAD_DIR_PATH);
    String workingDir = ARTIFACT_BUNDLE_DOWNLOAD_DIR_PATH + "/" + workingDirecotry;
    createDirectoryIfDoesNotExist(workingDir);
    return new File(workingDir);
  }

  public Map<String, List<FileData>> getManifestFilesFromArtifactBundle(File workingDirectory, File artifactBundleFile,
      ArtifactBundleDelegateConfig artifactBundleDelegateConfig, String activityId, LogCallback logCallback)
      throws Exception {
    try {
      logCallback.saveExecutionLog(
          format("%n Extracting Artifact Bundle : %s ", artifactBundleFile.toPath().getFileName()), INFO);
      File file = extractArtifactBundle(
          artifactBundleDelegateConfig.getArtifactBundleType(), artifactBundleFile, workingDirectory, activityId);
      Map<String, List<FileData>> manifestsFromArtifactBundle = new HashMap<>();

      logCallback.saveExecutionLog(
          format("%n Successfully Extracted Artifact Bundle : %s ", artifactBundleFile.toPath().getFileName()), INFO);

      logCallback.saveExecutionLog(format("%n Starting Reading Manifest From Artifact Bundle."), INFO);
      if (isNotEmpty(artifactBundleDelegateConfig.getFilePaths())) {
        logCallback.saveExecutionLog(" Got the following Manifest: ", INFO);
        List<FileData> filePathList = new ArrayList<>();
        for (String filePath : artifactBundleDelegateConfig.getFilePaths()) {
          List<FileData> fileDatas = readManifestFiles(file.getAbsolutePath(), filePath, logCallback);
          for (FileData fileData : fileDatas) {
            logCallback.saveExecutionLog(format(" -> %s ", fileData.getFilePath()), INFO);
          }
          filePathList.addAll(fileDatas);
        }
        manifestsFromArtifactBundle.put(artifactBundleDelegateConfig.getIdentifier(), filePathList);
      }

      logCallback.saveExecutionLog(" Successfully Read Manifest From Artifact Bundle.", INFO);
      return manifestsFromArtifactBundle;

    } catch (IOException e) {
      IOException sanitizedException = ExceptionMessageSanitizer.sanitizeException(e);
      throw new UnexpectedException("Failed to get Artifact Bundle source manifest files", sanitizedException);
    } catch (Exception e) {
      throw ExceptionMessageSanitizer.sanitizeException(e);
    } finally {
      FileIo.deleteDirectoryAndItsContentIfExists(workingDirectory.getAbsolutePath());
    }
  }
  private File extractZipFile(File artifactBundleFile, File workingDirectory, String activityId) throws IOException {
    InputStream inputStream = new FileInputStream(artifactBundleFile.getAbsolutePath());
    ZipInputStream zipInputStream = new ZipInputStream(inputStream);
    String fileName = ARTIFACT_BUNDLE_MANIFESTS + activityId;
    createDirectoryIfDoesNotExist(workingDirectory + fileName);
    File file = new File(workingDirectory, fileName);
    unzipManifestFiles(file, zipInputStream);
    return file;
  }

  public File extractArtifactBundle(
      String artifactBundleType, File artifactBundleFile, File workingDirectory, String activityId) throws Exception {
    File file;
    if (artifactBundleType.equals(ZIP.toString())) {
      file = extractZipFile(artifactBundleFile, workingDirectory, activityId);
    } else if (artifactBundleType.equals(TAR.toString())) {
      file = extractTarFile(artifactBundleFile, workingDirectory, activityId);
    } else if (artifactBundleType.equals(TAR_GZIP.toString())) {
      file = extractTarGzFile(artifactBundleFile, workingDirectory, activityId);
    } else {
      throw new UnsupportedTypeException("Ony ZIP, TAR and TAR_GZ Type of Artifact Bundle source is Supported");
    }
    if (file == null) {
      throw new UnexpectedException("Failed to get Artifact Bundle source manifest files");
    }
    return file;
  }

  public File extractTarFile(File artifactBundleFile, File workingDirectory, String activityId) throws IOException {
    String fileName = ARTIFACT_BUNDLE_MANIFESTS + activityId;
    createDirectoryIfDoesNotExist(workingDirectory + fileName);
    File extractedDir = new File(workingDirectory, fileName);

    try (FileInputStream fis = new FileInputStream(artifactBundleFile.getAbsolutePath());
         ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream("tar", fis)) {
      extractTarInternal(ais, extractedDir);
    } catch (ArchiveException e) {
      ArchiveException sanitizedException = ExceptionMessageSanitizer.sanitizeException(e);
      FileIo.deleteDirectoryAndItsContentIfExists(workingDirectory.getAbsolutePath());
      throw new RuntimeException(sanitizedException);
    }
    return extractedDir;
  }

  public File extractTarGzFile(File artifactBundleFile, File workingDirectory, String activityId) throws IOException {
    String fileName = ARTIFACT_BUNDLE_MANIFESTS + activityId;
    createDirectoryIfDoesNotExist(workingDirectory + fileName);
    File extractedDir = new File(workingDirectory, fileName);

    try (FileInputStream fis = new FileInputStream(artifactBundleFile.getAbsolutePath());
         GzipCompressorInputStream gzis = new GzipCompressorInputStream(fis);
         ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream("tar", gzis)) {
      extractTarInternal(ais, extractedDir);
    } catch (ArchiveException e) {
      ArchiveException sanitizedException = ExceptionMessageSanitizer.sanitizeException(e);
      FileIo.deleteDirectoryAndItsContentIfExists(workingDirectory.getAbsolutePath());
      throw new RuntimeException(sanitizedException);
    }

    return extractedDir;
  }

  private void extractTarInternal(ArchiveInputStream ais, File extractedDir) throws IOException {
    ArchiveEntry entry;
    while ((entry = ais.getNextEntry()) != null) {
      File outFile = new File(extractedDir, entry.getName());

      if (entry.isDirectory()) {
        if (!outFile.exists() && !outFile.mkdirs()) {
          throw new IOException("Failed to create directory: " + outFile.getAbsolutePath());
        }
      } else {
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
          byte[] buffer = new byte[8192];
          int read;
          while ((read = ais.read(buffer)) != -1) {
            fos.write(buffer, 0, read);
          }
        }
      }
    }
  }

  public List<FileData> readManifestFiles(String workingDirPath, String filePath, LogCallback logCallback) {
    File file = new File(workingDirPath, filePath);
    if (file.isDirectory()) {
      return readManifestFilesFromDirectory(workingDirPath, filePath, logCallback);
    } else {
      return readManifestFilesAsFilePath(workingDirPath, filePath, logCallback);
    }
  }

  public List<FileData> readManifestFilesAsFilePath(String workingDirPath, String filePath, LogCallback logCallback) {
    List<FileData> fileDataList;
    Path absoluteFilePath = new File(workingDirPath, filePath).toPath();
    try {
      fileDataList = getFilesUnderPath(absoluteFilePath.toString());
    } catch (Exception ex) {
      Exception sanitizedException = ExceptionMessageSanitizer.sanitizeException(ex);
      log.error(ExceptionUtils.getMessage(sanitizedException));
      throw new UnexpectedException("Failed to get files. Error: " + ExceptionUtils.getMessage(sanitizedException));
    }

    List<FileData> manifestFiles = new ArrayList<>();
    for (FileData fileData : fileDataList) {
      try {
        String fileContent = new String(fileData.getFileBytes(), UTF_8);
        if (isValidManifest(fileContent, logCallback)) {
          manifestFiles.add(FileData.builder()
                                .fileName(absoluteFilePath.getFileName().toString())
                                .filePath(filePath)
                                .fileContent(fileContent)
                                .build());
        }
      } catch (Exception ex) {
        Exception sanitizedException = ExceptionMessageSanitizer.sanitizeException(ex);
        throw new UnexpectedException(String.format("Failed to read content of file %s. Error: %s",
            new File(filePath).getName(), ExceptionUtils.getMessage(sanitizedException)));
      }
    }

    return manifestFiles;
  }

  public List<FileData> readManifestFilesFromDirectory(
      String workingDirPath, String folderPath, LogCallback logCallback) {
    List<FileData> fileDataList;
    Path directory = new File(workingDirPath, folderPath).toPath();

    try {
      fileDataList = getFilesUnderPath(directory.toString());
    } catch (Exception ex) {
      Exception sanitizedException = ExceptionMessageSanitizer.sanitizeException(ex);
      log.error(ExceptionUtils.getMessage(sanitizedException));
      throw new UnexpectedException("Failed to get files. Error: " + ExceptionUtils.getMessage(sanitizedException));
    }

    List<FileData> manifestFiles = new ArrayList<>();
    for (FileData fileData : fileDataList) {
      String filePath = fileData.getFilePath();
      try {
        String fileContent = new String(fileData.getFileBytes(), UTF_8);
        if (isValidManifest(fileContent, logCallback)) {
          manifestFiles.add(
              FileData.builder().fileName(fileData.getFileName()).filePath(filePath).fileContent(fileContent).build());
        }
      } catch (Exception ex) {
        Exception sanitizedException = ExceptionMessageSanitizer.sanitizeException(ex);
        throw new UnexpectedException(String.format("Failed to read content of file %s. Error: %s",
            new File(filePath).getName(), ExceptionUtils.getMessage(sanitizedException)));
      }
    }

    return manifestFiles;
  }

  public boolean isValidManifest(String fileContent, LogCallback logCallback) {
    ManifestType manifestType = getManifestType(fileContent, null, logCallback);
    return null != manifestType;
  }

  public ManifestType getManifestType(String content, @Nullable String fileName, LogCallback logCallback) {
    // Currently checking for TAS manifest Type only but this can be expandable for other types if required in future
    Map<String, Object> map;
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    try {
      map = mapper.readValue(content, Map.class);
    } catch (Exception e) {
      Exception sanitizedException = ExceptionMessageSanitizer.sanitizeException(e);
      log.warn(getParseErrorMessage(fileName, sanitizedException.getMessage()));
      logCallback.saveExecutionLog(getParseErrorMessage(fileName), LogLevel.WARN);
      logCallback.saveExecutionLog("Error: " + sanitizedException.getMessage(), LogLevel.WARN);
      return null;
    }

    if (isApplicationManifest(map)) {
      return APPLICATION_MANIFEST;
    }

    if (isVariableManifest(map)) {
      return VARIABLE_MANIFEST;
    }

    if (isAutoscalarManifest(map)) {
      return AUTOSCALAR_MANIFEST;
    }

    log.warn(getParseErrorMessage(fileName));
    logCallback.saveExecutionLog(getParseErrorMessage(fileName), LogLevel.WARN);
    return null;
  }

  private String getParseErrorMessage(String fileName) {
    return "Failed to parse file" + (isNotEmpty(fileName) ? " " + fileName : "") + ".";
  }

  private String getParseErrorMessage(String fileName, String errorMessage) {
    return String.format("Failed to parse file [%s]. Error - [%s]", isEmpty(fileName) ? "" : fileName, errorMessage);
  }

  private boolean isAutoscalarManifest(Map<String, Object> map) {
    return map.containsKey(PCF_AUTOSCALAR_MANIFEST_INSTANCE_LIMITS_ELE)
        && map.containsKey(PCF_AUTOSCALAR_MANIFEST_RULES_ELE);
  }

  private boolean isVariableManifest(Map<String, Object> map) {
    Optional entryOptional = map.entrySet().stream().filter(entry -> isInvalidValue(entry.getValue())).findFirst();

    return !entryOptional.isPresent();
  }

  private boolean isInvalidValue(Object value) {
    return value instanceof Map;
  }

  private boolean isApplicationManifest(Map<String, Object> map) {
    if (map.containsKey(APPLICATION_YML_ELEMENT)) {
      List<Map> applicationMaps = (List<Map>) map.get(APPLICATION_YML_ELEMENT);
      if (isEmpty(applicationMaps)) {
        return false;
      }

      Map application = applicationMaps.get(0);
      Map<String, Object> treeMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
      treeMap.putAll(application);
      return treeMap.containsKey(NAME_MANIFEST_YML_ELEMENT);
    }
    return false;
  }

  public String getArtifactPathOfArtifactBundle(ArtifactBundleDetails artifactBundleDetails, File artifactFile,
      File workingDirectory, LogCallback logCallback) throws Exception {
    if (artifactFile == null) {
      throw new IOException("Failed to download Artifact Bundle from the Artifact source");
    }
    File artifactBundleFile = extractArtifactBundle(artifactBundleDetails.getArtifactBundleType(), artifactFile,
        workingDirectory, artifactBundleDetails.getActivityId());

    File updatedArtifactFile =
        new File(Paths.get(artifactBundleFile.getAbsolutePath(), artifactBundleDetails.getDeployableUnitPath())
                     .normalize()
                     .toString());

    if (!updatedArtifactFile.exists()) {
      throw new IOException(format(
          "Failed to get Artifact from the Artifact Bundle '%s' . Please check if Deployable Unit Path '%s' is correct.",
          artifactFile.getName(), artifactBundleDetails.getDeployableUnitPath()));
    }

    logCallback.saveExecutionLog(
        color(format("Using Artifact '%s' from the Artifact Bundle '%s' ",
                  artifactBundleDetails.getDeployableUnitPath(), artifactBundleFile.toPath().getFileName()),
            LogColor.White, LogWeight.Bold));

    return updatedArtifactFile.toString();
  }
}
