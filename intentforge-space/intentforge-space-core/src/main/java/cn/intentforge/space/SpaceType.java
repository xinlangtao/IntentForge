package cn.intentforge.space;

import java.util.Optional;

/**
 * Enumerates the supported hierarchical space levels.
 */
public enum SpaceType {
  /**
   * Company level root space.
   */
  COMPANY(null),

  /**
   * Project space nested under a company.
   */
  PROJECT(COMPANY),

  /**
   * Product space nested under a project.
   */
  PRODUCT(PROJECT),

  /**
   * Application space nested under a product.
   */
  APPLICATION(PRODUCT);

  private final SpaceType expectedParentType;

  SpaceType(SpaceType expectedParentType) {
    this.expectedParentType = expectedParentType;
  }

  /**
   * Returns the expected direct parent type of the current space.
   *
   * @return expected parent type, or empty for root company spaces
   */
  public Optional<SpaceType> expectedParentType() {
    return Optional.ofNullable(expectedParentType);
  }
}
