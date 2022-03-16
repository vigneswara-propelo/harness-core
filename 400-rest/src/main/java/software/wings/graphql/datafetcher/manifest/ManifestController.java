package software.wings.graphql.datafetcher.manifest;

import software.wings.beans.appmanifest.HelmChart;
import software.wings.graphql.schema.type.manifest.QLManifest.QLManifestBuilder;

public class ManifestController {
  public static void populateManifest(HelmChart helmChart, QLManifestBuilder qlManifestBuilder) {
    qlManifestBuilder.id(helmChart.getUuid())
        .name(helmChart.getName())
        .description(helmChart.getDescription())
        .createdAt(helmChart.getCreatedAt())
        .version(helmChart.getVersion())
        .applicationManifestId(helmChart.getApplicationManifestId());
  }
}
