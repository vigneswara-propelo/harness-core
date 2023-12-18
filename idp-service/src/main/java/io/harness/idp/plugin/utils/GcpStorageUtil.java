/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.plugin.utils;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.UnexpectedException;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.IDP)
@Slf4j
public class GcpStorageUtil {
  public static final String GCS_BASE_URL = "https://storage.cloud.google.com/";
  private static final String PATH_SEPARATOR = "/";
  private final Storage storage;

  public GcpStorageUtil() {
    this.storage = StorageOptions.getDefaultInstance().getService();
  }

  public String uploadFileToGcs(String bucketName, String filePath, String fileName, InputStream fileContent) {
    try {
      BlobId blobId = BlobId.of(bucketName, filePath + "/" + fileName);
      Blob blob = storage.create(BlobInfo.newBuilder(blobId).build(), fileContent.readAllBytes());
      log.info("File uploaded to GCS: {}", blob.getName());
      return GCS_BASE_URL + bucketName + PATH_SEPARATOR + blob.getName();
    } catch (IOException e) {
      String errorMessage =
          "Could not upload file to GCS: " + bucketName + PATH_SEPARATOR + filePath + PATH_SEPARATOR + fileName;
      log.error(errorMessage);
      throw new UnexpectedException(errorMessage, e);
    } finally {
      if (fileContent != null) {
        try {
          fileContent.close();
        } catch (IOException e) {
          throw new UnexpectedException("Could not close file stream", e);
        }
      }
    }
  }

  public void deleteFileFromGcs(String gcsUrl) {
    try {
      URI uri = new URI(gcsUrl);
      String path = uri.getPath().substring(1);
      String[] pathComponents = path.split(PATH_SEPARATOR, 2);

      if (pathComponents.length == 2) {
        String bucketName = pathComponents[0];
        String objectName = pathComponents[1];
        BlobId blobId = BlobId.of(bucketName, objectName);
        boolean deleted = storage.delete(blobId);
        if (deleted) {
          log.info("File deleted from GCS: {}", blobId.getName());
        } else {
          log.warn("File not found or unable to delete from GCS: {}", blobId.getName());
        }
      } else {
        log.warn("File not found or unable to delete from GCS: {}", gcsUrl);
      }
    } catch (URISyntaxException e) {
      throw new UnexpectedException("Invalid GCS URL: " + gcsUrl, e);
    }
  }
}
