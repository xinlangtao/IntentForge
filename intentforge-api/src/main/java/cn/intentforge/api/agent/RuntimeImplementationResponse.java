package cn.intentforge.api.agent;


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
    id = cn.intentforge.common.util.ValidationSupport.requireText(id, "id");
    capability = cn.intentforge.common.util.ValidationSupport.requireText(capability, "capability");
    displayName = cn.intentforge.common.util.ValidationSupport.requireText(displayName, "displayName");
    version = cn.intentforge.common.util.ValidationSupport.requireText(version, "version");
    implementationClass = cn.intentforge.common.util.ValidationSupport.requireText(implementationClass, "implementationClass");
    metadata = cn.intentforge.common.util.ValidationSupport.immutableStringMap(metadata, "metadata");
  }
}
