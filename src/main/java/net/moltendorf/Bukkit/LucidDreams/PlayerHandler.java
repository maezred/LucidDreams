package net.moltendorf.Bukkit.LucidDreams;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

/**
 * @author moltendorf
 */
public class PlayerHandler {
	final UUID playerId;

	public SoftReference<Player> playerReference;

	public boolean effectsActive  = false;
	public boolean effectsEnabled = false;

	final public Collection<UUID>         activeWorlds  = new HashSet<>();
	final public Collection<PotionEffect> pausedPotions = new ArrayList<>();

	public boolean readyForEffects = false;

	public BukkitTask taskFlagForEffects = null;

	public PlayerHandler(final Player player) {
		playerReference = new SoftReference<>(player);
		playerId = player.getUniqueId();
	}

	public Player getPlayer() {
		final Player player = playerReference.get();

		if (player == null) {
			return Plugin.instance.getServer().getPlayer(playerId);
		}

		return player;
	}

	public void startEffectsInWorld(final UUID worldId, final int duration) {
		// Check if the effects are already enabled.
		if (activeWorlds.add(worldId)) {
			PlayerLookup.put(worldId, playerId, this);

			// Enable the effects if the player is in the same world we're starting effects on.
			if (worldId.equals(getPlayer().getWorld().getUID())) {
				startEffects(duration);
			}
		}
	}

	public void stopEffectsInWorld(final UUID worldId) {
		// Check if the effects are enabled.
		if (activeWorlds.remove(worldId)) {
			PlayerLookup.remove(worldId, playerId);

			if (worldId.equals(getPlayer().getWorld().getUID())) {
				stopEffects();
			}
		}
	}

	public void extendEffects(final UUID worldId, final int duration) {
		if (worldId.equals(getPlayer().getWorld().getUID()) && activeWorlds.contains(worldId)) {
			startEffects(duration);
		}
	}

	public void enableEffects(final int duration) {
		if (!effectsEnabled) {
			effectsEnabled = true;

			if (activeWorlds.contains(getPlayer().getWorld().getUID())) {
				startEffects(duration);
			}
		}
	}

	public void disableEffects(final int duration) {
		if (effectsEnabled) {
			stopEffects();

			effectsEnabled = false;
		}
	}

	public void startEffects(final int duration) {
		if (effectsEnabled) {
			final Player player = getPlayer();

			if (!effectsActive) {
				effectsActive = true;

				// Backup any effects we replace on the player.
				for (final PotionEffect effect : player.getActivePotionEffects()) {
					if (effect.getType() == PotionEffectType.NIGHT_VISION) {
						pausedPotions.add(effect);

						break;
					}
				}
			}

			// Replace the player's effects with ours.
			player.removePotionEffect(PotionEffectType.NIGHT_VISION);
			player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, duration, 0, true));
		}
	}

	public void stopEffects() {
		if (effectsEnabled && effectsActive) {
			effectsActive = false;

			final Player player = getPlayer();

			// Restore the player's effects we replaced.
			player.removePotionEffect(PotionEffectType.NIGHT_VISION);
			player.addPotionEffects(pausedPotions);

			pausedPotions.clear();
		}
	}
}
