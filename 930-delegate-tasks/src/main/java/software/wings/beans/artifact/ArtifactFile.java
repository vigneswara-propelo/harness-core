/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.ChecksumType;
import io.harness.file.HarnessFile;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import io.harness.validation.Create;
import io.harness.validation.Update;

import software.wings.beans.entityinterface.ApplicationAccess;

import com.github.reinert.jjschema.SchemaIgnore;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Id;

/**
 * The Class ArtifactFile.
 */
@OwnedBy(CDC)
@TargetModule(HarnessModule._957_CG_BEANS)
@Data
public class ArtifactFile
    implements PersistentEntity, HarnessFile, UuidAware, CreatedAtAware, UpdatedAtAware, ApplicationAccess {
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
  @FormDataParam("name") private String name;
  private String fileUuid;
  @NotEmpty(groups = Create.class) private String fileName;
  private String mimeType;
  private long size;
  private ChecksumType checksumType = ChecksumType.MD5;
  @FormDataParam("md5") private String checksum;
  @NotEmpty protected String accountId;
  @FdIndex @NotNull @SchemaIgnore protected String appId;
  @SchemaIgnore @FdIndex private long createdAt;
  @SchemaIgnore @NotNull private long lastUpdatedAt;
  /**
   * The type Builder.
   */
  public static final class Builder {
    private String name;
    private String fileUuid;
    private String fileName;
    private String mimeType;
    private long size;
    private ChecksumType checksumType = ChecksumType.MD5;
    private String checksum;
    private String uuid;
    private String appId;
    private long createdAt;
    private long lastUpdatedAt;

    private Builder() {}

    /**
     * An artifact file builder.
     *
     * @return the builder
     */
    public static Builder anArtifactFile() {
      return new Builder();
    }

    /**
     * With name builder.
     *
     * @param name the name
     * @return the builder
     */
    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * With file uuid builder.
     *
     * @param fileUuid the file uuid
     * @return the builder
     */
    public Builder withFileUuid(String fileUuid) {
      this.fileUuid = fileUuid;
      return this;
    }

    /**
     * With file name builder.
     *
     * @param fileName the file name
     * @return the builder
     */
    public Builder withFileName(String fileName) {
      this.fileName = fileName;
      return this;
    }

    /**
     * With mime type builder.
     *
     * @param mimeType the mime type
     * @return the builder
     */
    public Builder withMimeType(String mimeType) {
      this.mimeType = mimeType;
      return this;
    }

    /**
     * With size builder.
     *
     * @param size the size
     * @return the builder
     */
    public Builder withSize(long size) {
      this.size = size;
      return this;
    }

    /**
     * With checksum type builder.
     *
     * @param checksumType the checksum type
     * @return the builder
     */
    public Builder withChecksumType(ChecksumType checksumType) {
      this.checksumType = checksumType;
      return this;
    }

    /**
     * With checksum builder.
     *
     * @param checksum the checksum
     * @return the builder
     */
    public Builder withChecksum(String checksum) {
      this.checksum = checksum;
      return this;
    }

    /**
     * With uuid builder.
     *
     * @param uuid the uuid
     * @return the builder
     */
    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With app id builder.
     *
     * @param appId the app id
     * @return the builder
     */
    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With created at builder.
     *
     * @param createdAt the created at
     * @return the builder
     */
    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated at builder.
     *
     * @param lastUpdatedAt the last updated at
     * @return the builder
     */
    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anArtifactFile()
          .withName(name)
          .withFileUuid(fileUuid)
          .withFileName(fileName)
          .withMimeType(mimeType)
          .withSize(size)
          .withChecksumType(checksumType)
          .withChecksum(checksum)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedAt(createdAt)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    /**
     * Build artifact file.
     *
     * @return the artifact file
     */
    public ArtifactFile build() {
      ArtifactFile artifactFile = new ArtifactFile();
      artifactFile.setName(name);
      artifactFile.setFileUuid(fileUuid);
      artifactFile.setFileName(fileName);
      artifactFile.setMimeType(mimeType);
      artifactFile.setSize(size);
      artifactFile.setChecksumType(checksumType);
      artifactFile.setChecksum(checksum);
      artifactFile.setUuid(uuid);
      artifactFile.setAppId(appId);
      artifactFile.setCreatedAt(createdAt);
      artifactFile.setLastUpdatedAt(lastUpdatedAt);
      return artifactFile;
    }
  }
}
