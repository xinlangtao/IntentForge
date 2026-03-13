package cn.intentforge.session.model;

import cn.intentforge.common.util.ValidationSupport;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class SessionModelSupport {
  private SessionModelSupport() {
  }

  static String normalizeKeyword(String value) {
    String normalized = ValidationSupport.normalize(value);
    return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
  }

  static List<SessionMessage> immutableMessages(List<SessionMessage> messages) {
    return ValidationSupport.immutableList(messages, "messages");
  }

  static Map<String, String> immutableMetadata(Map<String, String> metadata) {
    return ValidationSupport.immutableStringMap(metadata, "metadata");
  }
}
