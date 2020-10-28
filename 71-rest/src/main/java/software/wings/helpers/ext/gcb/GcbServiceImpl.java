package software.wings.helpers.ext.gcb;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.network.Http.getOkHttpClientBuilder;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.container.ContainerScopes;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.hazelcast.core.RuntimeInterruptedException;
import io.harness.annotations.dev.OwnedBy;
import io.harness.network.Http;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.GcpConfig;
import software.wings.exception.GcbClientException;
import software.wings.helpers.ext.gcb.models.BuildOperationDetails;
import software.wings.helpers.ext.gcb.models.GcbBuildDetails;
import software.wings.helpers.ext.gcb.models.GcbBuildTriggers;
import software.wings.helpers.ext.gcb.models.GcbTrigger;
import software.wings.helpers.ext.gcb.models.RepoSource;
import software.wings.helpers.ext.gcs.GcsRestClient;
import software.wings.service.impl.GcpHelperService;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@OwnedBy(CDC)
@Singleton
@Slf4j
public class GcbServiceImpl implements GcbService {
  private static final String GCP_ERROR_MESSAGE = "Invalid Google Cloud Platform credentials.";
  public static final String GCB_BASE_URL = "https://cloudbuild.googleapis.com/";
  public static final String GCS_BASE_URL = "https://storage.googleapis.com/storage/";
  private final GcpHelperService gcpHelperService;

  @Inject
  public GcbServiceImpl(GcpHelperService gcpHelperService) {
    this.gcpHelperService = gcpHelperService;
  }

  @Override
  public BuildOperationDetails createBuild(
      GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails, GcbBuildDetails buildParams) {
    try {
      Response<BuildOperationDetails> response =
          getRestClient(GcbRestClient.class, GCB_BASE_URL)
              .createBuild(getBasicAuthHeader(gcpConfig, encryptionDetails), getProjectId(gcpConfig), buildParams)
              .execute();
      if (!response.isSuccessful()) {
        throw new GcbClientException(extractErrorMessage(response));
      }
      return response.body();
    } catch (InterruptedIOException e) {
      logger.error("Failed to create GCB build due to: ", e);
      throw new RuntimeInterruptedException();
    } catch (IOException e) {
      throw new GcbClientException(GCP_ERROR_MESSAGE, e);
    }
  }

  @NotNull
  @Override
  public GcbBuildDetails getBuild(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails, String buildId) {
    try {
      Response<GcbBuildDetails> response =
          getRestClient(GcbRestClient.class, GCB_BASE_URL)
              .getBuild(getBasicAuthHeader(gcpConfig, encryptionDetails), getProjectId(gcpConfig), buildId)
              .execute();
      if (!response.isSuccessful()) {
        throw new GcbClientException(extractErrorMessage(response));
      }
      return response.body();
    } catch (InterruptedIOException e) {
      logger.error("Failed to fetch GCB build due to: ", e);
      throw new RuntimeInterruptedException();
    } catch (IOException e) {
      throw new GcbClientException(GCP_ERROR_MESSAGE, e);
    }
  }

  @Override
  public BuildOperationDetails runTrigger(
      GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails, String triggerId, RepoSource repoSource) {
    try {
      Response<BuildOperationDetails> response = getRestClient(GcbRestClient.class, GCB_BASE_URL)
                                                     .runTrigger(getBasicAuthHeader(gcpConfig, encryptionDetails),
                                                         getProjectId(gcpConfig), triggerId, repoSource)
                                                     .execute();
      if (response.isSuccessful()) {
        return response.body();
      }
      throw new GcbClientException(extractErrorMessage(response));
    } catch (InterruptedIOException e) {
      logger.error("Failed to run GCB trigger due to: ", e);
      throw new RuntimeInterruptedException();
    } catch (IOException e) {
      throw new GcbClientException(GCP_ERROR_MESSAGE, e);
    }
  }

  @Override
  public String fetchBuildLogs(
      GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails, String bucketName, String fileName) {
    try {
      final String bucket = bucketName.replace("gs://", "");
      Response<ResponseBody> response =
          getRestClient(GcsRestClient.class, GCS_BASE_URL)
              .fetchLogs(getBasicAuthHeader(gcpConfig, encryptionDetails), bucket, fileName)
              .execute();
      if (!response.isSuccessful()) {
        throw new GcbClientException(extractErrorMessage(response));
      }
      return response.body().string();
    } catch (InterruptedIOException e) {
      logger.error("Failed to fetch GCB build logs due to: ", e);
      throw new RuntimeInterruptedException();
    } catch (IOException e) {
      throw new GcbClientException(GCP_ERROR_MESSAGE, e);
    }
  }

  @Override
  public List<GcbTrigger> getAllTriggers(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails) {
    try {
      Response<GcbBuildTriggers> response =
          getRestClient(GcbRestClient.class, GCB_BASE_URL)
              .getAllTriggers(getBasicAuthHeader(gcpConfig, encryptionDetails), getProjectId(gcpConfig))
              .execute();
      if (!response.isSuccessful()) {
        throw new GcbClientException(extractErrorMessage(response));
      }
      return response.body().getTriggers();
    } catch (IOException e) {
      throw new GcbClientException(GCP_ERROR_MESSAGE, e);
    }
  }

  @Override
  public GcbBuildDetails cancelBuild(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails, String buildId) {
    try {
      Response<GcbBuildDetails> response =
          getRestClient(GcbRestClient.class, GCB_BASE_URL)
              .cancelBuild(getBasicAuthHeader(gcpConfig, encryptionDetails), getProjectId(gcpConfig), buildId)
              .execute();
      if (!response.isSuccessful()) {
        throw new GcbClientException(response.errorBody().string());
      }
      return response.body();
    } catch (IOException e) {
      throw new GcbClientException(GCP_ERROR_MESSAGE, e);
    }
  }

  @VisibleForTesting
  <T> T getRestClient(final Class<T> client, String baseUrl) {
    OkHttpClient okHttpClient = getOkHttpClientBuilder()
                                    .connectTimeout(5, TimeUnit.SECONDS)
                                    .proxy(Http.checkAndGetNonProxyIfApplicable(baseUrl))
                                    .build();
    Retrofit retrofit = new Retrofit.Builder()
                            .client(okHttpClient)
                            .baseUrl(baseUrl)
                            .addConverterFactory(JacksonConverterFactory.create())
                            .build();
    return retrofit.create(client);
  }

  @VisibleForTesting
  String getBasicAuthHeader(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails) throws IOException {
    if (gcpConfig.isUseDelegate()) {
      return getAccessTokenUsingK8sWorkloadIdentity();
    }
    GoogleCredential gc = gcpHelperService.getGoogleCredential(gcpConfig, encryptionDetails);

    if (gc.refreshToken()) {
      return String.join(" ", "Bearer", gc.getAccessToken());
    } else {
      String msg = "Could not refresh token for google cloud provider";
      logger.warn(msg);
      throw new GcbClientException(msg);
    }
  }

  private String getAccessTokenUsingK8sWorkloadIdentity() {
    try {
      GoogleCredentials gc = GoogleCredentials.getApplicationDefault();
      if (gc.createScopedRequired()) {
        gc = gc.createScoped(ContainerScopes.CLOUD_PLATFORM);
      }
      return String.join(" ", "Bearer", gc.refreshAccessToken().getTokenValue());
    } catch (IOException e) {
      logger.error("Failed to get default credentials", e);
      throw new GcbClientException("Failed to retrieve GCP access token");
    }
  }

  private String getProjectId(GcpConfig gcpConfig) {
    if (gcpConfig.isUseDelegate()) {
      return getClusterProjectId();
    } else {
      return (String) (JsonUtils.asObject(new String(gcpConfig.getServiceAccountKeyFileContent()), HashMap.class))
          .get("project_id");
    }
  }

  private String getClusterProjectId() {
    OkHttpClient client = new OkHttpClient();
    Request request = new Request.Builder()
                          .header("Metadata-Flavor", "Google")
                          .url("http://metadata.google.internal/computeMetadata/v1/project/project-id")
                          .build();
    try {
      okhttp3.Response response = client.newCall(request).execute();
      return response.body().string();
    } catch (IOException | NullPointerException e) {
      logger.error("Failed to get projectId due to: ", e);
      throw new GcbClientException("Can not retrieve project-id from from cluster meta");
    }
  }

  private String extractErrorMessage(Response<?> response) {
    try {
      return (String) ((Map) JsonUtils.asObject(response.errorBody().string(), HashMap.class).get("error"))
          .get("message");
    } catch (IOException e) {
      throw new GcbClientException("Exception has occurred while reading error message", e);
    }
  }
}
