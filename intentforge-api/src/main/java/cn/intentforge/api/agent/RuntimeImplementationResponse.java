package cn.intentforge.api.agent;

import cn.intentforge.api.util.ApiModelSupport;

import java.util.Map;

/**
 * API payload that describes one runtime implementation selected for a run.
 *
 * @param id stable implementation identifier
 * @param capability runtime capability name
 * @param displayName human-readable implementation name
 * @param version implementation version text
 * @param implementationClass concrete implementation class name
 * @param metadata optional implementation metadata
 */
public record RuntimeImplementationResponse(
    String id,
    String capability,
    String displayName,
    String version,
    String implementationClass,
    Map<String, String> metadata
) {
  /**
   * Creates a validated runtime implementation response payload.
   */
  public RuntimeImplementationResponse {
    id = ApiModelSupport.requireText(id, "id");
    capability = ApiModelSupport.requireText(capability, "capability");
    displayName = ApiModelSupport.requireText(displayName, "displayName");
    version = ApiModelSupport.requireText(version, "version");
    implementationClass = ApiModelSupport.requireText(implementationClass, "implementationClass");
    metadata = ApiModelSupport.immutableStringMap(metadata, "metadata");
  }
}
