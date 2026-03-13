package cn.intentforge.tool.shell.security;

import static cn.intentforge.common.util.ValidationSupport.normalize;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Default shell command validator.
 */
public final class DefaultShellCommandValidator implements ShellCommandValidator {
  @Override
  public ValidationResult validate(String command, Path workingDirectory, Set<String> allowedExecutables) {
    String normalized = normalize(command);
    if (normalized == null) {
      return ValidationResult.denied("Command must not be blank", "");
    }

    String executable = extractExecutable(normalized);
    if (executable.isEmpty()) {
      return ValidationResult.denied("Cannot parse executable", executable);
    }

    if (containsMultipleCommands(normalized)) {
      return ValidationResult.denied("Multiple command separators are not allowed", executable);
    }

    if (executable.startsWith("./") || executable.startsWith(".\\")) {
      Path candidate = workingDirectory.resolve(executable).normalize();
      if (!candidate.startsWith(workingDirectory)) {
        return ValidationResult.denied("Relative executable escapes working directory", executable);
      }
    }

    if (allowedExecutables != null && !allowedExecutables.isEmpty()) {
      String executableBase = Path.of(executable).getFileName().toString().toLowerCase(Locale.ROOT);
      if (!allowedExecutables.contains(executableBase) && !allowedExecutables.contains(executable.toLowerCase(Locale.ROOT))) {
        return ValidationResult.denied("Executable is not in whitelist: " + executableBase, executable);
      }
    }

    return ValidationResult.allowed(executable);
  }

  private static boolean containsMultipleCommands(String command) {
    boolean inSingleQuote = false;
    boolean inDoubleQuote = false;
    boolean escaped = false;
    for (int index = 0; index < command.length(); index++) {
      char current = command.charAt(index);
      if (escaped) {
        escaped = false;
        continue;
      }
      if (current == '\\') {
        escaped = true;
        continue;
      }
      if (current == '\'' && !inDoubleQuote) {
        inSingleQuote = !inSingleQuote;
        continue;
      }
      if (current == '"' && !inSingleQuote) {
        inDoubleQuote = !inDoubleQuote;
        continue;
      }
      if (inSingleQuote || inDoubleQuote) {
        continue;
      }
      if (current == ';' || current == '|' || current == '&' || current == '\n') {
        return true;
      }
    }
    return false;
  }

  private static String extractExecutable(String command) {
    String trimmed = Objects.requireNonNull(command, "command must not be null").trim();
    if (trimmed.isEmpty()) {
      return "";
    }
    int separatorIndex = findSeparator(trimmed);
    return separatorIndex < 0 ? trimmed : trimmed.substring(0, separatorIndex);
  }

  private static int findSeparator(String value) {
    int spaceIndex = value.indexOf(' ');
    int tabIndex = value.indexOf('\t');
    if (spaceIndex < 0) {
      return tabIndex;
    }
    if (tabIndex < 0) {
      return spaceIndex;
    }
    return Math.min(spaceIndex, tabIndex);
  }
}
