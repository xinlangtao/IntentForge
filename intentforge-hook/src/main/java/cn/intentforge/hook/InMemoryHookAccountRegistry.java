package cn.intentforge.hook;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelType;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory {@link HookAccountRegistry} for local server bootstrap and tests.
 *
 * @since 1.0.0
 */
public final class InMemoryHookAccountRegistry implements HookAccountRegistry {
  private final Map<Key, ChannelAccountProfile> accounts = new ConcurrentHashMap<>();

  /**
   * Registers one account profile in memory.
   *
   * @param accountProfile account profile to register
   */
  @Override
  public void register(ChannelAccountProfile accountProfile) {
    ChannelAccountProfile nonNullAccountProfile = Objects.requireNonNull(accountProfile, "accountProfile must not be null");
    accounts.put(new Key(nonNullAccountProfile.type(), nonNullAccountProfile.id()), nonNullAccountProfile);
  }

  /**
   * Resolves one registered account profile by type and identifier.
   *
   * @param channelType channel type encoded in the hook path
   * @param accountId stable account identifier encoded in the hook path
   * @return matching account profile when registered
   */
  @Override
  public Optional<ChannelAccountProfile> find(ChannelType channelType, String accountId) {
    return Optional.ofNullable(accounts.get(new Key(
        Objects.requireNonNull(channelType, "channelType must not be null"),
        Objects.requireNonNull(accountId, "accountId must not be null"))));
  }

  private record Key(ChannelType channelType, String accountId) {
  }
}
