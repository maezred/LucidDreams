package com.moltendorf.bukkit.luciddreams;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

/**
 * Listener register.
 *
 * @author moltendorf
 */
public class Listeners implements Listener {

	protected BukkitTask clock = null;
	protected Map<UUID, PlayerData> players = new LinkedHashMap<>();

	protected void removeEffects() {
		if (clock != null) {
			clock.cancel();
			clock = null;
		}

		for (Entry<UUID, PlayerData> entry : players.entrySet()) {
			PlayerData playerData = entry.getValue();
			Player player = playerData.player;

			if (playerData.hasEffects) {
				player.removePotionEffect(PotionEffectType.HEALTH_BOOST);
				player.removePotionEffect(PotionEffectType.INVISIBILITY);
				player.removePotionEffect(PotionEffectType.NIGHT_VISION);
				player.removePotionEffect(PotionEffectType.WEAKNESS);

				player.removePotionEffect(PotionEffectType.REGENERATION);

				player.sendMessage("As you wake, you come to the grave realization that this was not a dream.");
			}

			if (playerData.taskEnterBed != null) {
				playerData.taskEnterBed.cancel();
			}
		}

		players.clear();
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void EntityTargetEventHandler(final EntityTargetEvent event) {

		// Are we enabled at all?
		if (!Plugin.configuration.global.enabled) {
			return;
		}

		final Entity entity = event.getTarget();

		if (entity == null) {
			return;
		}

		final UUID id = entity.getUniqueId();

		final PlayerData playerData = players.get(id);

		if (playerData == null) {
			return;
		}

		if (playerData.hasEffects) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void PlayerBedEnterEventMonitor(final PlayerBedEnterEvent event) {

		// Are we enabled at all?
		if (!Plugin.configuration.global.enabled) {
			return;
		}

		final Player player = event.getPlayer();
		final UUID id = player.getUniqueId();

		final PlayerData playerData;
		PlayerData fetchedPlayerData = players.get(id);

		if (fetchedPlayerData == null) {
			playerData = new PlayerData(player);
			players.put(id, playerData);
		} else {
			playerData = fetchedPlayerData;

			if (playerData.taskEnterBed != null) {
				playerData.taskEnterBed.cancel();
				playerData.taskEnterBed = null;
			}
		}

		final Runnable runnable;

		runnable = new Runnable() {

			@Override
			public void run() {

				// Just for safety.
				if (player.isSleeping()) {
					if (playerData.hasEffects) {
						player.sendMessage("You feel the warm covers in your dream.");
					} else {
						player.sendMessage("You slowly drift to sleep.");
					}

					playerData.readyForEffects = true;
					playerData.taskEnterBed = null;
				}
			}
		};

		playerData.taskEnterBed = Plugin.instance.getServer().getScheduler().runTaskLater(Plugin.instance, runnable, 40);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void PlayerBedLeaveEventMonitor(final PlayerBedLeaveEvent event) {

		// Are we enabled at all?
		if (!Plugin.configuration.global.enabled) {
			return;
		}

		final Player player = event.getPlayer();
		final UUID id = player.getUniqueId();

		final PlayerData playerData = players.get(id);

		if (playerData == null) {
			// This shouldn't happen.
			return;
		}

		if (playerData.readyForEffects) {
			// 23458 (the last moment a bed can be used).
			int duration = 23458 - (int) player.getWorld().getTime();

			// 23458 - 12541 (the first moment a bed can be used).
			if (duration <= 10917) {
				if (playerData.hasEffects) {
					int regenerationDuration = (int) ((player.getMaxHealth() - player.getHealth()) * 1.25 * 20.);

					if (regenerationDuration > duration) {
						regenerationDuration = duration;
					}

					player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, regenerationDuration, 1, true));

					player.sendMessage("This dream world suddenly feels very cold.");
				} else {
					int regenerationDuration = (int) ((player.getMaxHealth() - player.getHealth()) * 1.25 * 20. + 1.25 * 20. * 20.);

					if (regenerationDuration > duration) {
						regenerationDuration = duration;
					}

					player.addPotionEffects(Arrays.asList(new PotionEffect[]{
						new PotionEffect(PotionEffectType.HEALTH_BOOST, duration, 4, true),
						new PotionEffect(PotionEffectType.INVISIBILITY, duration, 0, true),
						new PotionEffect(PotionEffectType.NIGHT_VISION, duration, 0, true),
						new PotionEffect(PotionEffectType.WEAKNESS, duration, 15, true),
						new PotionEffect(PotionEffectType.REGENERATION, regenerationDuration, 1, true)
					}));

					player.sendMessage("You feel as if you're dreaming.");

					playerData.hasEffects = true;

					if (clock == null) {
						final Runnable runnable;

						runnable = new Runnable() {

							@Override
							public void run() {
								clock = null;

								removeEffects();
							}
						};

						clock = Plugin.instance.getServer().getScheduler().runTaskLater(Plugin.instance, runnable, 23458 - player.getWorld().getTime());
					}
				}
			} else {
				removeEffects();
			}

			playerData.readyForEffects = false;
		} else if (playerData.taskEnterBed != null) {
			playerData.taskEnterBed.cancel();
			playerData.taskEnterBed = null;
		}

	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void PlayerItemConsumeEventMonior(final PlayerItemConsumeEvent event) {

		// Are we enabled at all?
		if (!Plugin.configuration.global.enabled) {
			return;
		}

		if (event.getItem().getType() == Material.MILK_BUCKET) {
			final Player player = event.getPlayer();
			final UUID id = player.getUniqueId();

			final PlayerData playerData = players.get(id);

			if (playerData == null) {
				return;
			}

			players.remove(id);

			player.sendMessage("You feel relieved that the nightmare is over.");
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void PlayerQuitEventMonitor(PlayerQuitEvent event) {
		if (true) {
			return;
		}

		// Are we enabled at all?
		if (!Plugin.configuration.global.enabled) {
			return;
		}

		final Player player = event.getPlayer();
		final UUID id = player.getUniqueId();

		final PlayerData playerData = players.get(id);

		if (playerData == null) {
			return;
		}

		Collection<PotionEffect> potions = player.getActivePotionEffects();

		for (PotionEffect effect : potions) {

		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void PlayerLoginEventMonitor(PlayerLoginEvent event) {
		if (true) {
			return;
		}

		// Are we enabled at all?
		if (!Plugin.configuration.global.enabled) {
			return;
		}

		final Player player = event.getPlayer();
		final UUID id = player.getUniqueId();

		final PlayerData playerData = players.get(id);

		if (playerData == null) {
			return;
		}
	}
}
