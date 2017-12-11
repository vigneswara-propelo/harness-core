package software.wings.service.intfc.yaml;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.ConfigFile;
import software.wings.beans.Environment;
import software.wings.beans.Pipeline;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.ServiceCommand;
import software.wings.yaml.YamlPayload;

/**
 * Yaml Resource Service.
 *
 * @author bsollish
 */
public interface YamlResourceService {
  /**
   * Find by app, service and service command ids.
   *
   * @param appId     the app id
   * @param serviceCommandId the service command id
   * @return the service command
   */
  RestResponse<YamlPayload> getServiceCommand(@NotEmpty String appId, @NotEmpty String serviceCommandId);

  /**
   * Update by app, service and service command ids and yaml payload
   *
   * @param accountId     the account id
   * @param yamlPayload the yaml version of the service command
   * @return the service command
   */
  RestResponse<ServiceCommand> updateServiceCommand(@NotEmpty String accountId, YamlPayload yamlPayload);

  /**
   * Gets the yaml version of a pipeline by pipelineId
   *
   * @param appId     the app id
   * @param pipelineId the pipeline id
   * @return the rest response
   */
  RestResponse<YamlPayload> getPipeline(String appId, String pipelineId);

  /**
   * Update a pipeline that is sent as Yaml (in a JSON "wrapper")
   *
   *
   * @param accountId
   * @param yamlPayload the yaml version of the service command
   * @return the rest response
   */
  RestResponse<Pipeline> updatePipeline(String accountId, YamlPayload yamlPayload);

  /**
   * Gets the yaml version of a trigger by artifactStreamId
   *
   * @param appId     the app id
   * @param artifactStreamId the artifact stream id
   * @return the rest response
   */
  RestResponse<YamlPayload> getTrigger(String appId, String artifactStreamId);

  /**
   * Update a trigger that is sent as Yaml (in a JSON "wrapper")
   *
   * @param appId     the app id
   * @param artifactStreamId the artifact stream id
   * @param yamlPayload the yaml version of the service command
   * @return the rest response
   */
  RestResponse<ArtifactStream> updateTrigger(
      String appId, String artifactStreamId, YamlPayload yamlPayload, boolean deleteEnabled);

  /**
   * Gets the yaml for a workflow
   *
   * @param appId     the app id
   * @param workflowId the workflow id
   * @return the rest response
   */
  RestResponse<YamlPayload> getWorkflow(String appId, String workflowId);

  RestResponse<Workflow> updateWorkflow(String accountId, YamlPayload yamlPayload);

  /**
   * Gets all the setting attributes of a given type by accountId
   *
   * @param accountId   the account id
   * @param type        the SettingVariableTypes
   * @return the rest response
   */
  RestResponse<YamlPayload> getSettingAttributesList(String accountId, String type);

  /**
   * Gets the yaml version of an environment by envId
   *
   * @param appId   the app id
   * @param envId   the environment id
   * @return the rest response
   */
  RestResponse<YamlPayload> getEnvironment(String appId, String envId);

  RestResponse<YamlPayload> getService(String appId, String serviceId);

  RestResponse<YamlPayload> getInfraMapping(String accountId, String appId, String infraMappingId);

  RestResponse<YamlPayload> getContainerTask(String accountId, String appId, String containerTaskId);

  /**
   * Update a environment that is sent as Yaml (in a JSON "wrapper")
   *
   * @param accountId the account id
   * @param yamlPayload the yaml version of environment
   * @return the rest response
   */
  RestResponse<Environment> updateEnvironment(String accountId, YamlPayload yamlPayload);

  /**
   * Update a service that is sent as Yaml (in a JSON "wrapper")
   *
   * @param accountId the account id
   * @param yamlPayload the yaml version of service
   * @return the rest response
   */
  RestResponse<Service> updateService(String accountId, YamlPayload yamlPayload);

  RestResponse<YamlPayload> getLambdaSpec(String accountId, String appId, String lambdaSpecId);

  RestResponse<YamlPayload> getSettingAttribute(String accountId, String uuid);

  RestResponse<ConfigFile> updateConfigFile(
      String appId, String configFileId, YamlPayload yamlPayload, boolean deleteEnabled);

  /**
   * Get config file yaml
   * @param appId
   * @param configFileUuid
   * @return
   */
  RestResponse<YamlPayload> getConfigFileYaml(String accountId, String appId, String configFileUuid);

  RestResponse<YamlPayload> getConfigFileYaml(String accountId, String appId, ConfigFile configFile);
}
