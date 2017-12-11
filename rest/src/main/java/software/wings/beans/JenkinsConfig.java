package software.wings.beans;

import static software.wings.yaml.YamlHelper.ENCRYPTED_VALUE_STR;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.Encryptable;
import software.wings.annotation.Encrypted;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.yaml.setting.ArtifactServerYaml;
import software.wings.yaml.setting.VerificationProviderYaml;

/**
 * Created by peeyushaggarwal on 5/26/16.
 */
@JsonTypeName("JENKINS")
@Data
@Builder
@ToString(exclude = "password")
public class JenkinsConfig extends SettingValue implements Encryptable {
  @Attributes(title = "Jenkins URL", required = true) @NotEmpty private String jenkinsUrl;
  @Attributes(title = "Username", required = true) @NotEmpty private String username;
  @JsonView(JsonViews.Internal.class)
  @Attributes(title = "Password", required = true)
  @Encrypted
  private char[] password;
  @SchemaIgnore @NotEmpty private String accountId;

  @SchemaIgnore private String encryptedPassword;

  /**
   * Instantiates a new jenkins config.
   */
  public JenkinsConfig() {
    super(SettingVariableTypes.JENKINS.name());
  }

  public JenkinsConfig(
      String jenkinsUrl, String username, char[] password, String accountId, String encryptedPassword) {
    super(SettingVariableTypes.JENKINS.name());
    this.jenkinsUrl = jenkinsUrl;
    this.username = username;
    this.password = password;
    this.accountId = accountId;
    this.encryptedPassword = encryptedPassword;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends ArtifactServerYaml {
    @Builder
    public Yaml(String type, String url, String username, String password) {
      super(type, url, username, password);
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class VerificationYaml extends VerificationProviderYaml {
    private String url;
    private String username;
    private String password = ENCRYPTED_VALUE_STR;

    @Builder
    public VerificationYaml(String type, String url, String username, String password) {
      super(type);
      this.url = url;
      this.username = username;
      this.password = password;
    }
  }
}
