package cn.intentforge.prompt.model;

import static cn.intentforge.common.util.ValidationSupport.normalize;
import static cn.intentforge.common.util.ValidationSupport.requireText;

public record PromptVariable(
    String name,
    boolean required,
    String defaultValue,
    String description
) {
  public PromptVariable {
    name = requireText(name, "name");
    defaultValue = normalize(defaultValue);
    description = normalize(description);
  }
}
