package software.wings.sm;

import java.util.Map;

/**
 * Interface for all RepeatElements.
 *
 * @author Rishi
 */
public interface ContextElement {
  /**
   * The constant APP
   */
  static final String APP = "app";
  /**
   * The constant SERVICE
   */
  static final String SERVICE = "service";

  /**
   * The constant APP.
   */
  static final String SERVICE_TEMPLATE = "serviceTemplate";

  /**
   * The constant ENV.
   */
  static final String ENV = "env";
  /**
   * The constant HOST.
   */
  static final String HOST = "host";
  /**
   * The constant INSTANCE.
   */
  static final String INSTANCE = "instance";

  /**
   * The constant TIMESTAMP_ID.
   */
  static final String TIMESTAMP_ID = "timestampId";

  /**
   * Gets element type.
   *
   * @return the element type
   */
  public ContextElementType getElementType();

  /**
   * Gets uuid.
   *
   * @return uuid uuid
   */
  public String getUuid();

  /**
   * Gets name.
   *
   * @return the name
   */
  public String getName();

  /**
   * Param map.
   *
   * @return the map
   */
  public Map<String, Object> paramMap();
}
