package cn.intentforge.hook;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelType;
import java.util.List;
import java.util.Optional;

/**
 * Stores hook-visible channel accounts that can be resolved from external webhook paths.
 *
 * @since 1.0.0
 */
public interface HookAccountRegistry {
  /**
   * Registers one channel account for hook endpoint resolution.
   *
   * @param accountProfile account profile to register
   */
  void register(ChannelAccountProfile accountProfile);

  /**
   * Finds one channel account by channel type and account identifier.
   *
   * @param channelType channel type encoded in the hook path
   * @param accountId stable account identifier encoded in the hook path
   * @return matching account profile when registered
   */
  Optional<ChannelAccountProfile> find(ChannelType channelType, String accountId);

  /**
   * Lists all registered hook-visible channel accounts.
   *
   * @return immutable account profile list
   */
  List<ChannelAccountProfile> list();
}
