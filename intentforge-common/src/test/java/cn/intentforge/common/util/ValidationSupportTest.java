package cn.intentforge.common.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ValidationSupportTest {
  @Test
  void shouldNormalizeOptionalText() {
    Assertions.assertNull(ValidationSupport.normalize(null));
    Assertions.assertNull(ValidationSupport.normalize("   "));
    Assertions.assertEquals("value", ValidationSupport.normalize("  value  "));
    Assertions.assertEquals("value", ValidationSupport.normalizeOptional("  value  "));
  }

  @Test
  void shouldRequireNonBlankText() {
    Assertions.assertEquals("value", ValidationSupport.requireText("  value  ", "field"));
    Assertions.assertThrows(IllegalArgumentException.class, () -> ValidationSupport.requireText("   ", "field"));
  }

  @Test
  void shouldUseNormalizedTextOrDefault() {
    Assertions.assertEquals("value", ValidationSupport.textOrDefault(" value ", "fallback"));
    Assertions.assertEquals("fallback", ValidationSupport.textOrDefault("   ", "fallback"));
  }

  @Test
  void shouldCreateImmutableStringMap() {
    Map<String, String> source = new LinkedHashMap<>();
    source.put(" key ", " value ");

    Map<String, String> normalized = ValidationSupport.immutableStringMap(source, "metadata");

    Assertions.assertEquals(Map.of("key", "value"), normalized);
    Assertions.assertThrows(UnsupportedOperationException.class, () -> normalized.put("next", "value"));
  }

  @Test
  void shouldCreateImmutableObjectMap() {
    Map<String, Object> normalized = ValidationSupport.immutableObjectMap(Map.of(" key ", 1), "metadata");

    Assertions.assertEquals(Map.of("key", 1), normalized);
    Assertions.assertThrows(UnsupportedOperationException.class, () -> normalized.put("next", 2));
  }

  @Test
  void shouldCreateImmutableList() {
    List<String> values = new ArrayList<>();
    values.add("alpha");
    values.add("beta");

    List<String> normalized = ValidationSupport.immutableList(values, "values");

    Assertions.assertEquals(List.of("alpha", "beta"), normalized);
    Assertions.assertThrows(UnsupportedOperationException.class, () -> normalized.add("gamma"));
  }

  @Test
  void shouldRejectNullCollectionEntries() {
    List<String> values = new ArrayList<>();
    values.add("alpha");
    values.add(null);

    Assertions.assertThrows(NullPointerException.class, () -> ValidationSupport.immutableList(values, "values"));
    Assertions.assertThrows(NullPointerException.class, () -> ValidationSupport.immutableStringMap(Map.of("key", null), "metadata"));
    Assertions.assertThrows(NullPointerException.class, () -> ValidationSupport.immutableObjectMap(Map.of("key", null), "metadata"));
  }
}
