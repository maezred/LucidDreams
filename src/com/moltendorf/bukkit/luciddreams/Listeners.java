package com.moltendorf.bukkit.luciddreams;

import java.util.Arrays;
//import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
//import org.bukkit.event.player.PlayerLoginEvent;
//import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

/**
 * Listener register.
 *
 * @author moltendorf
 */
public class Listeners implements Listener {

	final protected Plugin plugin;

	protected BukkitTask clock = null;
	protected Map<UUID, PlayerData> players = new LinkedHashMap<>();

	protected Listeners(final Plugin instance) {
		plugin = instance;
	}

	protected void removeEffects() {
		if (clock != null) {
			clock.cancel();
			clock = null;
		}

		for (Entry<UUID, PlayerData> entry : players.entrySet()) {
			PlayerData playerData = entry.getValue();
			Player player = playerData.player;

			if (playerData.hasEffects) {
				player.removePotionEffect(PotionEffectType.INVISIBILITY);
				player.removePotionEffect(PotionEffectType.NIGHT_VISION);

				player.sendMessage("As you wake, you come to the grave realization that this was not a dream.");
			}

			if (playerData.taskFlagForEffects != null) {
				playerData.taskFlagForEffects.cancel();
			}
		}

		players.clear();
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void EntityDamageByEntityEventHandler(final EntityDamageByEntityEvent event) {

		// Are we enabled at all?
		if (!plugin.configuration.global.enabled) {
			return;
		}

		final Entity damager = event.getDamager();

		if (damager == null) {
			return;
		}

		final Player player;

		final EntityType type = damager.getType();

		final LivingEntity shooter;

		switch (type) {
			case ARROW:
				shooter = ((Arrow) damager).getShooter();

				if (shooter!= null && shooter.getType() == EntityType.PLAYER) {
					player = (Player) shooter;
				} else {
					return;
				}

			break;

			case PLAYER:
				// Convert Entity to Player.
				player = (Player) damager;

			break;

			case SPLASH_POTION:
				shooter = ((ThrownPotion) damager).getShooter();

				if (shooter!= null && shooter.getType() == EntityType.PLAYER) {
					player = (Player) shooter;
				} else {
					return;
				}

			break;

			case PRIMED_TNT:
				final Entity source = ((TNTPrimed) damager).getSource();

				if (source != null && source.getType() == EntityType.PLAYER) {
					player = (Player) source;
				} else {
					return;
				}

			break;

			default:
				return;
		}

		final UUID id = player.getUniqueId();

		// No cancelling shooting yourself in the foot.
		if (event.getEntity().getUniqueId() == id) {
			return;
		}

		final PlayerData playerData = players.get(id);

		if (playerData == null || !playerData.hasEffects) {
			return;
		}

		// Is this entity allowed to be attacked?
		if (!plugin.configuration.global.disallowed.contains(event.getEntityType())) {
			return;
		}

		long currentWarning = player.getWorld().getFullTime();

		if (currentWarning > playerData.nextWarning) {
			// Negate all damage dealt to the entity.
			event.setDamage(0);

			final double health = player.getHealth();

			int regenerationDuration = 0;

			if (health > 1) {
				// Give the player a nice warning.
				player.damage(health / 2.);

				regenerationDuration = (int) ((health - player.getHealth()) * 1.25 * 20.);

				// Regenerate them back up because we're nice.
				player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, regenerationDuration, 1, true));
			} else {
				player.damage(0);
			}

			// The warning period lasts for five seconds or the full duration of the regeneration. Whichever is more.
			if (regenerationDuration > 100) {
				playerData.nextWarning = currentWarning + regenerationDuration;
			} else {
				playerData.nextWarning = currentWarning + 100;
			}

			player.sendMessage("You panic from the thought of the monsters.");
		} else {
			player.removePotionEffect(PotionEffectType.INVISIBILITY);
			player.removePotionEffect(PotionEffectType.NIGHT_VISION);

			players.remove(id);

			player.sendMessage("You jolt awake as soon as you realize you are not in bed.");
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void EntityTargetLivingEntityEventHandler(final EntityTargetLivingEntityEvent event) {

		// Are we enabled at all?
		if (!plugin.configuration.global.enabled) {
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
		if (!plugin.configuration.global.enabled) {
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

			if (playerData.taskFlagForEffects != null) {
				playerData.taskFlagForEffects.cancel();
				playerData.taskFlagForEffects = null;
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
					playerData.taskFlagForEffects = null;
				}
			}
		};

		playerData.taskFlagForEffects = plugin.getServer().getScheduler().runTaskLater(plugin, runnable, 40);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void PlayerBedLeaveEventMonitor(final PlayerBedLeaveEvent event) {

		// Are we enabled at all?
		if (!plugin.configuration.global.enabled) {
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
			// 23660 (the moment zombies and skeletons begin burning).
			// 24260 (not a valid relative time, but thirty seconds after zombies and skeletons begin burning).
			int duration = 24260 - (int) player.getWorld().getTime();

			// Calculate custom duration for regeneration effect.
			int regenerationDuration = (int) ((player.getMaxHealth() - player.getHealth()) * 1.25 * 20.);

			// 24260 - 12541 (the first moment a bed can be used).
			if (duration <= 11719) {
				playerData.nextWarning = player.getWorld().getFullTime() + regenerationDuration;

				if (playerData.hasEffects) {
					player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, regenerationDuration, 1, true));

					player.sendMessage("This dream world suddenly feels very cold.");
				} else {
					Creature creature;
					Entity target;

					for (Entity entity : player.getNearbyEntities(100, 100, 100)) {
						if (plugin.configuration.global.creatures.contains(entity.getType())) {
							creature = (Creature) entity;
							target = creature.getTarget();

							if (target != null && target.getUniqueId() == id) {
								creature.setTarget(null);
							}
						}
					}

					player.addPotionEffects(Arrays.asList(new PotionEffect[]{
						new PotionEffect(PotionEffectType.INVISIBILITY, duration, 0, true),
						new PotionEffect(PotionEffectType.NIGHT_VISION, duration, 0, true),
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

						clock = plugin.getServer().getScheduler().runTaskLater(plugin, runnable, 24260 - player.getWorld().getTime());
					}
				}
			} else {
				player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, regenerationDuration, 1, true));

				removeEffects();
			}

			playerData.readyForEffects = false;
		} else if (playerData.taskFlagForEffects != null) {
			playerData.taskFlagForEffects.cancel();
			playerData.taskFlagForEffects = null;
		}

	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void PlayerItemConsumeEventMonior(final PlayerItemConsumeEvent event) {

		// Are we enabled at all?
		if (!plugin.configuration.global.enabled) {
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

//	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
//	public void PlayerQuitEventMonitor(PlayerQuitEvent event) {
//		if (true) {
//			return;
//		}
//
//		// Are we enabled at all?
//		if (!plugin.configuration.global.enabled) {
//			return;
//		}
//
//		final Player player = event.getPlayer();
//		final UUID id = player.getUniqueId();
//
//		final PlayerData playerData = players.get(id);
//
//		if (playerData == null) {
//			return;
//		}
//
//		Collection<PotionEffect> potions = player.getActivePotionEffects();
//
//		for (PotionEffect effect : potions) {
//
//		}
//	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void PlayerRespawnEventMonitor(PlayerRespawnEvent event) {

		// Are we enabled at all?
		if (!plugin.configuration.global.enabled) {
			return;
		}

		final Player player = event.getPlayer();
		final UUID id = player.getUniqueId();

		final PlayerData playerData = players.get(id);

		if (playerData == null) {
			return;
		}

		players.remove(id);
	}

//	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
//	public void PlayerLoginEventMonitor(PlayerLoginEvent event) {
//		if (true) {
//			return;
//		}
//
//		// Are we enabled at all?
//		if (!plugin.configuration.global.enabled) {
//			return;
//		}
//
//		final Player player = event.getPlayer();
//		final UUID id = player.getUniqueId();
//
//		final PlayerData playerData = players.get(id);
//
//		if (playerData == null) {
//			return;
//		}
//	}
}
