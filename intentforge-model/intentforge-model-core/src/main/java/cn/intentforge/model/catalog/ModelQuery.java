package cn.intentforge.model.catalog;

import static cn.intentforge.common.util.ValidationSupport.normalize;

public record ModelQuery(
    String providerId,
    ModelType type,
    ModelCapability capability,
    Boolean streaming
) {
  public ModelQuery {
    providerId = normalize(providerId);
  }

  public static ModelQuery byProvider(String providerId) {
    return new ModelQuery(providerId, null, null, null);
  }

  public static ModelQuery byType(ModelType type) {
    return new ModelQuery(null, type, null, null);
  }

  public static ModelQuery byCapability(ModelCapability capability) {
    return new ModelQuery(null, null, capability, null);
  }
}
