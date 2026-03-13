package cn.intentforge.tool.core.annotation;

import static cn.intentforge.common.util.ValidationSupport.normalize;

import cn.intentforge.tool.core.model.ToolCallRequest;
import cn.intentforge.tool.core.model.ToolCallResult;
import cn.intentforge.tool.core.model.ToolDefinition;
import cn.intentforge.tool.core.model.ToolExecutionContext;
import cn.intentforge.tool.core.registry.ToolRegistration;
import cn.intentforge.tool.core.registry.ToolRegistry;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Registers methods annotated with {@link IntentTool} into a {@link ToolRegistry}.
 */
public final class AnnotationToolRegistrar {
  private final ToolRegistry toolRegistry;

  /**
   * Creates registrar.
   *
   * @param toolRegistry target registry
   */
  public AnnotationToolRegistrar(ToolRegistry toolRegistry) {
    this.toolRegistry = Objects.requireNonNull(toolRegistry, "toolRegistry must not be null");
  }

  /**
   * Registers all annotated methods from one object.
   *
   * @param toolObject tool object
   */
  public void register(Object toolObject) {
    Objects.requireNonNull(toolObject, "toolObject must not be null");
    for (Method method : toolObject.getClass().getDeclaredMethods()) {
      IntentTool toolAnnotation = method.getAnnotation(IntentTool.class);
      if (toolAnnotation == null) {
        continue;
      }
      registerMethod(toolObject, method, toolAnnotation);
    }
  }

  private void registerMethod(Object target, Method method, IntentTool toolAnnotation) {
    String toolId = normalize(toolAnnotation.id());
    if (toolId == null) {
      toolId = method.getName();
    }
    String description = normalize(toolAnnotation.description());
    if (description == null) {
      description = "Tool: " + toolId;
    }

    Map<String, Object> schema = buildSchema(method);
    ToolDefinition definition = new ToolDefinition(toolId, description, schema, toolAnnotation.sensitive());

    method.setAccessible(true);
    toolRegistry.register(new ToolRegistration(definition, request -> invoke(target, method, request)));
  }

  private ToolCallResult invoke(Object target, Method method, ToolCallRequest request) {
    try {
      Object[] args = buildInvocationArgs(method, request);
      Object result = method.invoke(target, args);
      return convertResult(result);
    } catch (InvocationTargetException ex) {
      Throwable cause = ex.getCause() == null ? ex : ex.getCause();
      return ToolCallResult.error("ANNOTATION_TOOL_INVOCATION_ERROR", cause.getMessage());
    } catch (Exception ex) {
      return ToolCallResult.error("ANNOTATION_TOOL_INVOCATION_ERROR", ex.getMessage());
    }
  }

  private Object[] buildInvocationArgs(Method method, ToolCallRequest request) {
    Parameter[] parameters = method.getParameters();
    Object[] args = new Object[parameters.length];
    for (int index = 0; index < parameters.length; index++) {
      Parameter parameter = parameters[index];
      if (parameter.getType() == ToolExecutionContext.class) {
        args[index] = request.context();
        continue;
      }
      IntentToolParam paramAnnotation = parameter.getAnnotation(IntentToolParam.class);
      if (paramAnnotation == null) {
        throw new IllegalArgumentException(
            "Missing @IntentToolParam on parameter " + index + " of " + method.getName());
      }
      String paramName = normalize(paramAnnotation.name());
      Object value = request.parameters().get(paramName);
      args[index] = convertValue(value, parameter.getType());
    }
    return args;
  }

  private Map<String, Object> buildSchema(Method method) {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("type", "object");

    Map<String, Object> properties = new LinkedHashMap<>();
    List<String> required = new ArrayList<>();

    Parameter[] parameters = method.getParameters();
    for (Parameter parameter : parameters) {
      if (parameter.getType() == ToolExecutionContext.class) {
        continue;
      }
      IntentToolParam paramAnnotation = parameter.getAnnotation(IntentToolParam.class);
      if (paramAnnotation == null) {
        throw new IllegalArgumentException(
            "Missing @IntentToolParam for parameter " + parameter.getName() + " in method " + method.getName());
      }
      String paramName = normalize(paramAnnotation.name());
      if (paramName == null) {
        throw new IllegalArgumentException("@IntentToolParam.name must not be blank");
      }
      Map<String, Object> paramSchema = new LinkedHashMap<>();
      String explicitType = normalize(paramAnnotation.type());
      paramSchema.put("type", explicitType == null ? typeFor(parameter.getType()) : explicitType);
      String description = normalize(paramAnnotation.description());
      if (description != null) {
        paramSchema.put("description", description);
      }
      properties.put(paramName, paramSchema);
      if (paramAnnotation.required()) {
        required.add(paramName);
      }
    }

    schema.put("properties", properties);
    if (!required.isEmpty()) {
      schema.put("required", required);
    }
    return Map.copyOf(schema);
  }

  private static String typeFor(Class<?> type) {
    if (type == String.class || type == Character.class || type == char.class || type.isEnum()) {
      return "string";
    }
    if (type == Integer.class || type == int.class || type == Long.class || type == long.class) {
      return "integer";
    }
    if (type == Float.class || type == float.class || type == Double.class || type == double.class) {
      return "number";
    }
    if (type == Boolean.class || type == boolean.class) {
      return "boolean";
    }
    if (List.class.isAssignableFrom(type)) {
      return "array";
    }
    if (Map.class.isAssignableFrom(type)) {
      return "object";
    }
    return "string";
  }

  private static ToolCallResult convertResult(Object result) {
    if (result == null) {
      return ToolCallResult.success("OK");
    }
    if (result instanceof ToolCallResult toolCallResult) {
      return toolCallResult;
    }
    if (result instanceof String text) {
      return ToolCallResult.success(text);
    }
    if (result instanceof Map<?, ?> map) {
      return ToolCallResult.success("", map, Map.of());
    }
    return ToolCallResult.success(String.valueOf(result), result, Map.of());
  }

  private static Object convertValue(Object value, Class<?> targetType) {
    if (value == null) {
      if (targetType.isPrimitive()) {
        return switch (targetType.getName()) {
          case "boolean" -> false;
          case "byte", "short", "int", "long" -> 0;
          case "float", "double" -> 0.0;
          case "char" -> '\0';
          default -> null;
        };
      }
      return null;
    }
    if (targetType.isInstance(value)) {
      return value;
    }
    if (targetType == String.class) {
      return String.valueOf(value);
    }
    if (targetType == Integer.class || targetType == int.class) {
      return Integer.parseInt(String.valueOf(value));
    }
    if (targetType == Long.class || targetType == long.class) {
      return Long.parseLong(String.valueOf(value));
    }
    if (targetType == Double.class || targetType == double.class) {
      return Double.parseDouble(String.valueOf(value));
    }
    if (targetType == Float.class || targetType == float.class) {
      return Float.parseFloat(String.valueOf(value));
    }
    if (targetType == Boolean.class || targetType == boolean.class) {
      return Boolean.parseBoolean(String.valueOf(value));
    }
    if (targetType.isEnum()) {
      @SuppressWarnings({"rawtypes", "unchecked"})
      Enum<?> enumValue = Enum.valueOf((Class<? extends Enum>) targetType, String.valueOf(value).toUpperCase(Locale.ROOT));
      return enumValue;
    }
    return value;
  }
}
