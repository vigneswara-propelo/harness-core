package software.wings.graphql.schema.type.manifest;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLObject;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@OwnedBy(CDC)
@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SETTING)
@FieldNameConstants(innerTypeName = "QLManifestKeys")
@TargetModule(HarnessModule._380_CG_GRAPHQL)

public class QLManifest implements QLObject {
  private String id;
  private String name;
  private String description;
  private Long createdAt;
  private String applicationManifestId;
  private String version;
}
