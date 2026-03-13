package cn.intentforge.agent.core;

import java.util.Map;

/**
 * Immutable artifact emitted by one agent stage.
 *
 * @param name artifact file-like name
 * @param mediaType artifact media type
 * @param content artifact textual content
 * @param metadata artifact metadata
 */
public record Artifact(
    String name,
    String mediaType,
    String content,
    Map<String, String> metadata
) {
  /**
   * Creates a validated artifact.
   */
  public Artifact {
    name = cn.intentforge.common.util.ValidationSupport.requireText(name, "name");
    mediaType = cn.intentforge.common.util.ValidationSupport.requireText(mediaType, "mediaType");
    content = cn.intentforge.common.util.ValidationSupport.requireText(content, "content");
    metadata = AgentModelSupport.immutableMetadata(metadata);
  }
}
