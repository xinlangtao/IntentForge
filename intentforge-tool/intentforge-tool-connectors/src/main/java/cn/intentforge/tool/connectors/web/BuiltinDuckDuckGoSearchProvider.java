package cn.intentforge.tool.connectors.web;

import static cn.intentforge.common.util.ValidationSupport.normalize;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Built-in search provider backed by DuckDuckGo instant answer API.
 */
public final class BuiltinDuckDuckGoSearchProvider implements SearchProvider {
  /**
   * Default provider id.
   */
  public static final String PROVIDER_ID = "duckduckgo";
  private static final Pattern FIRST_URL_PATTERN = Pattern.compile("\"FirstURL\"\\s*:\\s*\"(.*?)\"");
  private static final Pattern TEXT_PATTERN = Pattern.compile("\"Text\"\\s*:\\s*\"(.*?)\"");

  private final HttpClient httpClient;

  /**
   * Creates provider with default HTTP client.
   */
  public BuiltinDuckDuckGoSearchProvider() {
    this(HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build());
  }

  /**
   * Creates provider.
   *
   * @param httpClient HTTP client
   */
  public BuiltinDuckDuckGoSearchProvider(HttpClient httpClient) {
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
  }

  @Override
  public String id() {
    return PROVIDER_ID;
  }

  @Override
  public List<SearchResultItem> search(String query, int limit, Duration timeout) {
    String normalizedQuery = normalize(query);
    if (normalizedQuery == null) {
      throw new IllegalArgumentException("query must not be blank");
    }
    int normalizedLimit = Math.max(1, limit);
    Duration normalizedTimeout = timeout == null || timeout.isNegative() || timeout.isZero()
        ? Duration.ofSeconds(10)
        : timeout;
    String encoded = URLEncoder.encode(normalizedQuery, StandardCharsets.UTF_8);
    URI uri = URI.create("https://api.duckduckgo.com/?q=" + encoded + "&format=json&no_html=1&skip_disambig=1");
    HttpRequest request = HttpRequest.newBuilder(uri)
        .timeout(normalizedTimeout)
        .header("Accept", "application/json")
        .GET()
        .build();
    try {
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      return parseResults(normalizedQuery, response.body(), normalizedLimit);
    } catch (Exception ex) {
      throw new IllegalStateException("duckduckgo search request failed", ex);
    }
  }

  private static List<SearchResultItem> parseResults(String query, String body, int limit) {
    List<SearchResultItem> results = new ArrayList<>();

    String abstractText = extractField(body, "AbstractText");
    if (abstractText != null) {
      String abstractUrl = extractField(body, "AbstractURL");
      String heading = extractField(body, "Heading");
      results.add(new SearchResultItem(
          heading == null ? "DuckDuckGo Result" : heading,
          abstractUrl,
          abstractText));
    }

    Matcher firstUrlMatcher = FIRST_URL_PATTERN.matcher(body);
    Matcher textMatcher = TEXT_PATTERN.matcher(body);
    while (firstUrlMatcher.find() && textMatcher.find()) {
      String url = jsonUnescape(firstUrlMatcher.group(1));
      String text = jsonUnescape(textMatcher.group(1));
      if (text == null) {
        continue;
      }
      results.add(new SearchResultItem(text, url, text));
      if (results.size() >= limit) {
        break;
      }
    }

    if (results.isEmpty()) {
      results.add(new SearchResultItem(
          "DuckDuckGo Search",
          "https://duckduckgo.com/?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8),
          "No instant result; open the link for full search results."));
    }
    if (results.size() > limit) {
      return List.copyOf(results.subList(0, limit));
    }
    return List.copyOf(results);
  }

  private static String extractField(String body, String fieldName) {
    Pattern pattern = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"(.*?)\"");
    Matcher matcher = pattern.matcher(body == null ? "" : body);
    if (!matcher.find()) {
      return null;
    }
    return jsonUnescape(matcher.group(1));
  }

  private static String jsonUnescape(String value) {
    if (value == null) {
      return null;
    }
    StringBuilder builder = new StringBuilder(value.length());
    for (int index = 0; index < value.length(); index++) {
      char current = value.charAt(index);
      if (current != '\\' || index + 1 >= value.length()) {
        builder.append(current);
        continue;
      }
      char escaped = value.charAt(++index);
      switch (escaped) {
        case '"', '\\', '/' -> builder.append(escaped);
        case 'b' -> builder.append('\b');
        case 'f' -> builder.append('\f');
        case 'n' -> builder.append('\n');
        case 'r' -> builder.append('\r');
        case 't' -> builder.append('\t');
        case 'u' -> {
          if (index + 4 >= value.length()) {
            builder.append("\\u");
            continue;
          }
          String hex = value.substring(index + 1, index + 5);
          try {
            builder.append((char) Integer.parseInt(hex, 16));
            index += 4;
          } catch (NumberFormatException ex) {
            builder.append("\\u").append(hex);
            index += 4;
          }
        }
        default -> builder.append(escaped);
      }
    }
    String normalized = builder.toString().trim();
    return normalized.isEmpty() ? null : normalized;
  }
}
