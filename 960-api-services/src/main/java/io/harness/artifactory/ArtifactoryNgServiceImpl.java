/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.artifactory;

import static io.harness.artifactory.ArtifactoryClientImpl.getArtifactoryClient;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.util.stream.Collectors.toList;
import static org.jfrog.artifactory.client.model.impl.PackageTypeImpl.docker;
import static org.jfrog.artifactory.client.model.impl.PackageTypeImpl.maven;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.artifacts.comparator.BuildDetailsComparatorDescending;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.ArtifactoryRegistryException;
import io.harness.exception.NestedExceptionUtils;

import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.utils.RepositoryType;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jfrog.artifactory.client.model.impl.PackageTypeImpl;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true,
    components = {HarnessModuleComponent.CDS_ARTIFACTS, HarnessModuleComponent.CDS_AMI_ASG})
@Singleton
@Slf4j
@OwnedBy(HarnessTeam.CDP)
public class ArtifactoryNgServiceImpl implements ArtifactoryNgService {
  private static final String ARTIFACT_PATH_FILTER_SEARCH_WILDCARD_TERM = "*";
  @Inject ArtifactoryClientImpl artifactoryClient;

  @Override
  public List<BuildDetails> getBuildDetails(
      ArtifactoryConfigRequest artifactoryConfig, String repositoryName, String artifactPath, int maxVersions) {
    return artifactoryClient.getBuildDetails(artifactoryConfig, repositoryName, artifactPath, maxVersions);
  }

  @Override
  public List<BuildDetails> getArtifactList(ArtifactoryConfigRequest artifactoryConfig, String repositoryName,
      String artifactPath, int maxVersions, String artifactPathFilter, String artifactDirectory) {
    if (isNotEmpty(artifactPathFilter)) {
      return filterArtifactsOnHarnessSide(
          artifactoryConfig, repositoryName, artifactDirectory, artifactPathFilter, maxVersions, artifactPath);
    }
    return artifactoryClient.getArtifactList(artifactoryConfig, repositoryName, artifactPath, maxVersions);
  }

  private List<BuildDetails> getArtifactListWithArtifactPath(ArtifactoryConfigRequest artifactoryConfig,
      String repositoryName, String artifactDirectory, String artifactPath, int maxVersions) {
    String filePath = Paths.get(artifactDirectory, artifactPath).toString();
    List<BuildDetails> buildDetails =
        artifactoryClient.getArtifactList(artifactoryConfig, repositoryName, filePath, maxVersions);

    List<BuildDetails> buildDetailsFiltered =
        buildDetails.stream().filter(bd -> bd.getArtifactPath().endsWith(artifactPath)).collect(toList());
    if (!buildDetailsFiltered.isEmpty()) {
      return buildDetailsFiltered;
    }
    return buildDetails;
  }

  private Optional<BuildDetails> getLatestArtifactUsingExactPath(ArtifactoryConfigRequest artifactoryConfig,
      String repositoryName, String artifactDirectory, String artifactPath, int maxVersions) {
    // convert A/*/* -> A/
    String[] directoryElements = artifactDirectory.split("/");
    List<String> regexRemovedDirectory = new ArrayList<>();
    for (String directoryElement : directoryElements) {
      if (!directoryElement.contains("*")) {
        regexRemovedDirectory.add(directoryElement);
      }
    }
    String finalDirectory = String.join("/", regexRemovedDirectory);
    String finalFilePath = Paths.get(finalDirectory, artifactPath).toString();

    List<BuildDetails> artifactList =
        artifactoryClient.getArtifactList(artifactoryConfig, repositoryName, finalFilePath, maxVersions);

    return isNotEmpty(artifactList) ? Optional.ofNullable(artifactList.get(0)) : Optional.empty();
  }

  private List<BuildDetails> filterArtifactsOnHarnessSide(ArtifactoryConfigRequest artifactoryConfig,
      String repositoryName, String artifactDirectory, String artifactPathRegex, int maxVersions,
      String artifactFilter) {
    final Pattern artifactPathRegexPattern;
    try {
      artifactPathRegexPattern = Pattern.compile(artifactPathRegex);
    } catch (PatternSyntaxException e) {
      throw NestedExceptionUtils.hintWithExplanationException(
          "Please check ArtifactPathFilter field in Artifactory artifact configuration.",
          "Regular expression in Artifact Path Filter is invalid", new ArtifactoryRegistryException(e.getMessage()));
    }

    // When user give path Regex as artifactFilter -> filePath = artifactFilter
    // When user give fixed value of directory and artifactFilter -> construct file path
    String filePath = EmptyPredicate.isEmpty(artifactFilter)
        ? Paths.get(artifactDirectory, ARTIFACT_PATH_FILTER_SEARCH_WILDCARD_TERM).toString()
        : artifactFilter;

    List<BuildDetails> buildDetails =
        artifactoryClient.getArtifactList(artifactoryConfig, repositoryName, filePath, maxVersions);

    Pattern finalArtifactPathRegexPattern = artifactPathRegexPattern;

    // artifactDirectory will be empty when user provide artifactFilter
    int directoryPathLength = isNotEmpty(artifactDirectory)
        ? filePath.equalsIgnoreCase("/*") ? 0 : filePath.length() - ARTIFACT_PATH_FILTER_SEARCH_WILDCARD_TERM.length()
        : 0;

    return buildDetails.stream()
        .filter(bd -> {
          // if user provide artifactFilter -> full artifactPath to be considered
          // if user provide directory -> artifactPath following directory to be considered
          String fileName = bd.getArtifactPath().substring(directoryPathLength);
          Matcher matcher = finalArtifactPathRegexPattern.matcher(fileName);
          return matcher.matches();
        })
        .sorted(new BuildDetailsComparatorDescending())
        .collect(Collectors.toList());
  }

  @Override
  public BuildDetails getLatestArtifact(ArtifactoryConfigRequest artifactoryConfig, String repositoryName,
      String artifactDirectory, String artifactPathRegex, String artifactPath, int maxVersions, String artifactFilter) {
    final BuildDetails build;
    if (isEmpty(artifactPath) && isEmpty(artifactPathRegex)) {
      throw NestedExceptionUtils.hintWithExplanationException(
          "Please check ArtifactPath/ArtifactPathFilter field in Artifactory artifact configuration.",
          "Both Artifact Path and Artifact Path Filter cannot be empty",
          new ArtifactoryRegistryException("Could not find an artifact"));
    }

    if (EmptyPredicate.isEmpty(artifactFilter) && EmptyPredicate.isEmpty(artifactPathRegex)) {
      // When user defined fixed directory and fixed artifactPath

      List<BuildDetails> builds = getArtifactListWithArtifactPath(
          artifactoryConfig, repositoryName, artifactDirectory, artifactPath, maxVersions);
      if (isNotEmpty(builds)) {
        build = builds.get(0);
      } else {
        Optional<BuildDetails> buildDetail = getLatestArtifactUsingExactPath(
            artifactoryConfig, repositoryName, artifactDirectory, artifactPath, maxVersions);
        build = buildDetail.orElse(null);
      }
      if (build == null) {
        throw NestedExceptionUtils.hintWithExplanationException(
            "Please check artifactPath or artifactDirectory or repository field in Artifactory artifact configuration.",
            String.format("Could not find any Artifact with artifactPath [%s] for Artifactory repository"
                    + " [%s] for generic artifactDirectory [%s] in registry [%s].",
                artifactPath, repositoryName, artifactDirectory, artifactoryConfig.getArtifactoryUrl()),
            new ArtifactoryRegistryException(
                String.format("Could not find an artifact with artifactPath '%s'", artifactPath)));
      }
      return build;
    }

    if (EmptyPredicate.isEmpty(artifactFilter) && EmptyPredicate.isNotEmpty(artifactPathRegex)) {
      // When user defined fixed directory and regex artifactPath or artifactPathRegex
      // Backward Compatibility for legacy case
      List<BuildDetails> buildDetails = filterArtifactsOnArtifactorySide(
          artifactoryConfig, repositoryName, artifactDirectory, artifactPathRegex, maxVersions);
      if (isNotEmpty(buildDetails)) {
        build = buildDetails.get(0);
      } else {
        List<BuildDetails> builds = filterArtifactsOnHarnessSide(
            artifactoryConfig, repositoryName, artifactDirectory, artifactPathRegex, maxVersions, null);
        build = isNotEmpty(builds) ? builds.get(0) : null;
      }

      if (build == null) {
        throw NestedExceptionUtils.hintWithExplanationException(
            "Please check artifactPathFilter or artifactDirectory or repository field in Artifactory artifact .",
            String.format("Could not find any Artifact that match artifactPathFilter [%s] for Artifactory repository"
                    + " [%s] for generic artifactDirectory [%s] in registry [%s].",
                artifactPathRegex, repositoryName, artifactDirectory, artifactoryConfig.getArtifactoryUrl()),
            new ArtifactoryRegistryException(
                String.format("Could not find an artifact that matches artifactPathFilter '%s'", artifactPathRegex)));
      }
      return build;
    }

    if (EmptyPredicate.isNotEmpty(artifactFilter) && EmptyPredicate.isNotEmpty(artifactPathRegex)) {
      // When user defined artifact filter and regex artifactPath or artifactPathRegex

      List<BuildDetails> builds = filterArtifactsOnHarnessSide(
          artifactoryConfig, repositoryName, null, artifactPathRegex, maxVersions, artifactFilter);
      build = isNotEmpty(builds) ? builds.get(0) : null;

      if (build == null) {
        throw NestedExceptionUtils.hintWithExplanationException(
            "Please check artifactPathFilter or artifactFilter or repository field in Artifactory artifact.",
            String.format(
                "Could not find any Artifact that match artifactFilter [%s] and artifactPathFilter [%s] for Artifactory repository"
                    + " [%s] in registry [%s].",
                artifactFilter, artifactPathRegex, repositoryName, artifactoryConfig.getArtifactoryUrl()),
            new ArtifactoryRegistryException(
                String.format("Could not find an artifact that matches artifactPathFilter '%s'", artifactPathRegex)));
      }
      return build;
    }

    if (EmptyPredicate.isNotEmpty(artifactFilter) && EmptyPredicate.isEmpty(artifactPathRegex)) {
      // When user defined artifact filter and fixed artifactPath
      build = artifactoryClient.getBuildDetailWithFullPath(artifactoryConfig, repositoryName, artifactPath);

      if (build == null) {
        throw NestedExceptionUtils.hintWithExplanationException(
            "Please check artifactPath or artifactFilter or repository field in Artifactory artifact.",
            String.format(
                "Could not find any Artifact that match artifactFilter [%s] and artifactPath [%s] for Artifactory repository"
                    + " [%s] in registry [%s].",
                artifactFilter, artifactPath, repositoryName, artifactoryConfig.getArtifactoryUrl()),
            new ArtifactoryRegistryException(
                String.format("Could not find an artifact that matches artifactPath '%s'", artifactPath)));
      }
      return build;
    }

    return null;
  }

  private List<BuildDetails> filterArtifactsOnArtifactorySide(ArtifactoryConfigRequest artifactoryConfig,
      String repositoryName, String artifactDirectory, String artifactPathFilter, int maxVersions) {
    String filePath = Paths.get(artifactDirectory, artifactPathFilter).toString();

    List<BuildDetails> buildDetails =
        artifactoryClient.getArtifactList(artifactoryConfig, repositoryName, filePath, maxVersions);

    return buildDetails.stream().sorted(new BuildDetailsComparatorDescending()).collect(Collectors.toList());
  }

  @Override
  public Map<String, String> getRepositories(ArtifactoryConfigRequest artifactoryConfig, String packageType) {
    RepositoryType repositoryType = RepositoryType.valueOf(packageType);

    switch (repositoryType) {
      case docker:
        return artifactoryClient.getRepositories(artifactoryConfig, Arrays.asList(docker));
      case maven:
        return artifactoryClient.getRepositories(artifactoryConfig, Arrays.asList(maven));
      case generic:
      case any:
      default:
        return artifactoryClient.getRepositories(artifactoryConfig,
            Arrays.stream(PackageTypeImpl.values()).filter(type -> docker != type).collect(toList()));
    }
  }

  @Override
  public List<ArtifactoryImagePath> getImagePaths(ArtifactoryConfigRequest artifactoryConfig, String repoKey) {
    List<String> repos = artifactoryClient.listDockerImages(getArtifactoryClient(artifactoryConfig), repoKey);
    if (isEmpty(repos)) {
      return Collections.emptyList();
    }
    return repos.stream().map(repo -> ArtifactoryImagePath.builder().imagePath(repo).build()).collect(toList());
  }

  @Override
  public InputStream downloadArtifacts(ArtifactoryConfigRequest artifactoryConfig, String repoKey,
      Map<String, String> metadata, String artifactPathMetadataKey, String artifactFileNameMetadataKey) {
    return artifactoryClient.downloadArtifacts(
        artifactoryConfig, repoKey, metadata, artifactPathMetadataKey, artifactFileNameMetadataKey);
  }

  @Override
  public Long getFileSize(
      ArtifactoryConfigRequest artifactoryConfig, Map<String, String> metadata, String artifactPathMetadataKey) {
    return artifactoryClient.getFileSize(artifactoryConfig, metadata, artifactPathMetadataKey);
  }
}