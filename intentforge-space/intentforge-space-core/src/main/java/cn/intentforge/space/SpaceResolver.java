package cn.intentforge.space;

/**
 * Resolves the effective profile for a target space.
 */
public interface SpaceResolver {
  /**
   * Resolves the effective profile for the provided space identifier.
   *
   * @param spaceId target space identifier
   * @return resolved effective profile
   * @throws SpaceResolutionException when the hierarchy is invalid or incomplete
   */
  ResolvedSpaceProfile resolve(String spaceId);
}
