/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.containerStepGroup;

import static io.harness.ci.commonconstants.CIExecutionConstants.PATH_SEPARATOR;
import static io.harness.ci.commonconstants.ContainerExecutionConstants.STEP_MOUNT_PATH;
import static io.harness.filestore.utils.FileStoreNodeUtils.mapFileNodes;

import io.harness.beans.FileReference;
import io.harness.data.structure.EmptyPredicate;
import io.harness.filestore.dto.node.FileNodeDTO;
import io.harness.filestore.dto.node.FileStoreNodeDTO;
import io.harness.filestore.service.FileStoreService;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.filestore.NGFileType;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;

import com.google.inject.Inject;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DownloadHarnessStoreStepHelper {
  @Inject private FileStoreService fileStoreService;

  @Inject private ContainerStepGroupHelper containerStepGroupHelper;

  public Map<String, String> getEnvironmentVariables(
      Ambiance ambiance, DownloadHarnessStoreStepParameters downloadHarnessStoreStepParameters, String stepIdentifier) {
    Map<String, String> files = getFileContentsFromManifest(
        AmbianceUtils.getNgAccess(ambiance), downloadHarnessStoreStepParameters.getFiles().getValue());

    String filesJson = containerStepGroupHelper.convertToJson(files);

    HashMap<String, String> envVarsMap = new HashMap<>();
    envVarsMap.put("PLUGIN_HARNESS_FILE_STORE_MAP", filesJson);

    if (downloadHarnessStoreStepParameters.getDownloadPath() != null
        && EmptyPredicate.isNotEmpty(downloadHarnessStoreStepParameters.getDownloadPath().getValue())) {
      envVarsMap.put("PLUGIN_DOWNLOAD_PATH", downloadHarnessStoreStepParameters.getDownloadPath().getValue());
    } else if (EmptyPredicate.isNotEmpty(stepIdentifier)) {
      envVarsMap.put("PLUGIN_DOWNLOAD_PATH", STEP_MOUNT_PATH + PATH_SEPARATOR + stepIdentifier);
    }

    if (downloadHarnessStoreStepParameters.getOutputFilePathsContent() != null
        && EmptyPredicate.isNotEmpty(downloadHarnessStoreStepParameters.getOutputFilePathsContent().getValue())) {
      envVarsMap.put("PLUGIN_OUTPUT_FILE_PATHS_CONTENT",
          String.join(",", downloadHarnessStoreStepParameters.getOutputFilePathsContent().getValue()));
    }

    return envVarsMap;
  }

  public Map<String, String> getFileContentsFromManifest(NGAccess ngAccess, List<String> scopedFilePathList) {
    Map<String, String> manifestContents = new HashMap<>();

    if (EmptyPredicate.isNotEmpty(scopedFilePathList)) {
      for (String scopedFilePath : scopedFilePathList) {
        FileReference fileReference = FileReference.of(scopedFilePath, ngAccess.getAccountIdentifier(),
            ngAccess.getOrgIdentifier(), ngAccess.getProjectIdentifier());
        Optional<FileStoreNodeDTO> varsFile =
            fileStoreService.getWithChildrenByPath(fileReference.getAccountIdentifier(),
                fileReference.getOrgIdentifier(), fileReference.getProjectIdentifier(), fileReference.getPath(), true);
        if (varsFile.isPresent()) {
          FileStoreNodeDTO fileStoreNodeDTO = varsFile.get();
          if (NGFileType.FILE.equals(fileStoreNodeDTO.getType())) {
            FileNodeDTO file = (FileNodeDTO) fileStoreNodeDTO;
            // remove account from paths in files fetched from harness store
            manifestContents.put(Paths.get(scopedFilePath).getFileName().toString(), file.getContent());
          } else if (NGFileType.FOLDER.equals(fileStoreNodeDTO.getType())) {
            Integer folderPathLength = fileStoreNodeDTO.getPath().length();
            List<List<String>> listOfFiles = mapFileNodes(fileStoreNodeDTO,
                fileNode -> List.of(fileNode.getPath().substring(folderPathLength), fileNode.getContent()));
            listOfFiles.forEach(lst -> manifestContents.put(lst.get(0), lst.get(1)));
          } else {
            throw new UnsupportedOperationException(
                "The File/Folder type is not supported. Please enter the correct file path");
          }
        }
      }
    }
    return manifestContents;
  }
}
