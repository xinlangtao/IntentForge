package cn.intentforge.tool.connectors.web;

import static cn.intentforge.common.util.ValidationSupport.normalize;

/**
 * One search result entry.
 *
 * @param title result title
 * @param url result URL
 * @param snippet result snippet
 */
public record SearchResultItem(String title, String url, String snippet) {
  /**
   * Creates one search result item.
   *
   * @param title result title
   * @param url result URL
   * @param snippet result snippet
   */
  public SearchResultItem {
    title = normalize(title);
    url = normalize(url);
    snippet = normalize(snippet);
    if (title == null && snippet == null) {
      throw new IllegalArgumentException("title and snippet cannot both be blank");
    }
  }
}
