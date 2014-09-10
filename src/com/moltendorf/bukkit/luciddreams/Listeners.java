package com.moltendorf.bukkit.luciddreams;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.*;
import org.bukkit.material.Bed;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

/**
 * Listener register.
 *
 * @author moltendorf
 */
public class Listeners implements Listener {

	final protected Plugin plugin;

	protected BukkitTask clock = null;
	protected World world = null;

	protected Map<UUID, PlayerData> players = new LinkedHashMap<>();

	protected Listeners(final Plugin instance) {
		plugin = instance;
	}

	protected void extendEffects(int duration) {
		for (Entry<UUID, PlayerData> entry : players.entrySet()) {
			PlayerData playerData = entry.getValue();
			Player player = playerData.player;

			if (player != null && playerData.hasEffects) {
				player.removePotionEffect(PotionEffectType.INVISIBILITY);
				player.removePotionEffect(PotionEffectType.NIGHT_VISION);

				player.addPotionEffects(Arrays.asList(
					new PotionEffect(PotionEffectType.INVISIBILITY, duration, 0, true),
					new PotionEffect(PotionEffectType.NIGHT_VISION, duration, 0, true)
				));

				player.sendMessage("You continue dreaming as the rain keeps pouring.");
			}

			if (playerData.taskFlagForEffects != null) {
				playerData.taskFlagForEffects.cancel();
			}
		}
	}

	protected void removeEffects() {
		if (clock != null) {
			clock.cancel();
			clock = null;
		}

		for (Entry<UUID, PlayerData> entry : players.entrySet()) {
			PlayerData playerData = entry.getValue();
			Player player = playerData.player;

			if (player != null && playerData.hasEffects) {
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

				if (shooter != null && shooter.getType() == EntityType.PLAYER) {
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

				if (shooter != null && shooter.getType() == EntityType.PLAYER) {
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

		final Entity entity = event.getEntity();
		final UUID id = player.getUniqueId();

		// No cancelling shooting yourself in the foot.
		if (entity.getUniqueId() == id) {
			return;
		}

		final PlayerData playerData = players.get(id);

		if (playerData == null || !playerData.hasEffects) {
			return;
		}

		EntityType targetType = event.getEntityType();

		// Is this entity allowed to be attacked?
		if (!plugin.configuration.global.disallowed.contains(targetType)) {
			return;
		}

		if (plugin.configuration.global.creatures.contains(targetType)) {
			final Creature creature = (Creature) entity;

			final Entity target = creature.getTarget();

			if (target != null && target.getUniqueId() == id) {
				// Negate all damage dealt to the entity.
				event.setCancelled(true);
				creature.damage(0);

				// Clear this creature's target.
				creature.setTarget(null);

				// Regenerate the player to full in case they took any damage.
				player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, (int) ((player.getMaxHealth() - player.getHealth()) * 1.25 * 20.), 1, true));

				player.sendMessage("You've frightened this monster.");

				return;
			}
		}

		long currentWarning = player.getWorld().getFullTime();

		if (currentWarning > playerData.nextWarning) {
			// Negate all damage dealt to the entity.
			event.setCancelled(true);

			player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 4));

			// The warning period lasts for two seconds.
			playerData.nextWarning = currentWarning + 40;

			player.sendMessage("You panic from the thought of the monsters.");
		} else {
			player.removePotionEffect(PotionEffectType.INVISIBILITY);
			player.removePotionEffect(PotionEffectType.NIGHT_VISION);

			players.remove(id);

			player.sendMessage("You jolt awake as soon as you realize you are not in bed.");
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void EntityTargetEventHandler(final EntityTargetEvent event) {

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

			if (world == null) {
				world = player.getWorld();
			}
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

		final Runnable runnable;

		runnable = new Runnable() {

			@Override
			public void run() {
				if (playerData.readyForEffects) {
					// 23458 (the last moment a bed can be used).
					// 23660 (the moment zombies and skeletons begin burning).
					// 24260 (not a valid relative time, but thirty seconds after zombies and skeletons begin burning).
					int duration = 24260 - (int) world.getTime();

					// 24260 - 12541 (the first moment a bed can be used).
					if (duration > 11719) {
						if (world.hasStorm()) {
							duration = world.getWeatherDuration() + 600;
						} else {
							duration = 0;
						}
					}

					// Calculate custom duration for regeneration effect.
					int regenerationDuration = (int) ((player.getMaxHealth() - player.getHealth()) * 1.25 * 20.);

					if (duration > 0) {
						playerData.nextWarning = world.getFullTime() + regenerationDuration;

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

							player.addPotionEffects(Arrays.asList(
								new PotionEffect(PotionEffectType.INVISIBILITY, duration, 0, true),
								new PotionEffect(PotionEffectType.NIGHT_VISION, duration, 0, true),
								new PotionEffect(PotionEffectType.REGENERATION, regenerationDuration, 1, true)
							));

							player.sendMessage("You feel as if you're dreaming.");

							playerData.hasEffects = true;

							if (clock == null) {
								final Runnable runnable;

								runnable = new Runnable() {

									@Override
									public void run() {
										if (world.hasStorm()) {
											int ticks = world.getWeatherDuration() + 600;

											extendEffects(ticks);

											final Runnable runnable;

											runnable = new Runnable() {

												@Override
												public void run() {
													clock = null;

													removeEffects();
												}
											};

											clock = plugin.getServer().getScheduler().runTaskLater(plugin, runnable, ticks);
										} else {
											clock = null;

											removeEffects();
										}
									}
								};

								// Run the task 12 seconds before the effects run out to prevent screen flashing.
								clock = plugin.getServer().getScheduler().runTaskLater(plugin, runnable, duration - 240);
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
		};

		plugin.getServer().getScheduler().runTaskLater(plugin, runnable, 0);
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

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void PlayerQuitEventMonitor(PlayerQuitEvent event) {

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

		playerData.player = null;

		if (!playerData.hasEffects) {
			return;
		}

		// Remove effects.
		player.removePotionEffect(PotionEffectType.INVISIBILITY);
		player.removePotionEffect(PotionEffectType.NIGHT_VISION);
	}

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

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void PlayerJoinEventMonitor(PlayerJoinEvent event) {

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

		playerData.player = player;

		if (!playerData.hasEffects) {
			return;
		}

		int time = (int) world.getTime();

		// We need to fix the durations.
		player.removePotionEffect(PotionEffectType.INVISIBILITY);
		player.removePotionEffect(PotionEffectType.NIGHT_VISION);

		int duration;

		if (time < 260 || time > 12541) {
			duration = 24260 - time;
		} else if (world.hasStorm()) {
			duration = world.getWeatherDuration() + 600;
		} else {
			return;
		}

		player.addPotionEffects(Arrays.asList(
			new PotionEffect(PotionEffectType.INVISIBILITY, duration, 0, true),
			new PotionEffect(PotionEffectType.NIGHT_VISION, duration, 0, true)
		));
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void BlockBreakEventMonitor(BlockBreakEvent event) {

		// Are we enabled at all?
		if (!plugin.configuration.global.enabled) {
			return;
		}

		// We only use the input block to figure out which block is the head and the foot of the bed.
		final Block block = event.getBlock();

		if (block == null || block.getType() != Material.BED_BLOCK) {
			return;
		}

		final Bed blockData = (Bed) block.getState().getData();
		final Location blockLocation = block.getLocation();

		final Block head, foot;

		if (blockData.isHeadOfBed()) {
			head = block;

			switch (blockData.getFacing()) {
				case NORTH:
					foot = blockLocation.add(0, 0, 1).getBlock();
					break;

				case EAST:
					foot = blockLocation.add(-1, 0, 0).getBlock();
					break;

				case SOUTH:
					foot = blockLocation.add(0, 0, -1).getBlock();
					break;

				case WEST:
					foot = blockLocation.add(1, 0, 0).getBlock();
					break;

				default:
					return;
			}
		} else {
			foot = block;

			switch (blockData.getFacing()) {
				case NORTH:
					head = blockLocation.add(0, 0, -1).getBlock();
					break;

				case EAST:
					head = blockLocation.add(1, 0, 0).getBlock();
					break;

				case SOUTH:
					head = blockLocation.add(0, 0, 1).getBlock();
					break;

				case WEST:
					head = blockLocation.add(-1, 0, 0).getBlock();
					break;

				default:
					return;
			}
		}

		// Very unlikely.
		if (head.getType() != Material.BED_BLOCK || foot.getType() != Material.BED_BLOCK) {
			return;
		}

		// No variables defined above are used beyond this point.
		final Location headLocation = head.getLocation();
		final Location footLocation = foot.getLocation();

		final BlockFace[] faces = new BlockFace[]{
			BlockFace.SELF,
			BlockFace.NORTH,
			BlockFace.NORTH_EAST,
			BlockFace.EAST,
			BlockFace.SOUTH_EAST,
			BlockFace.SOUTH,
			BlockFace.SOUTH_WEST,
			BlockFace.WEST,
			BlockFace.NORTH_WEST
		};

		// Iterate all players to see if any of them used this bed to start their dream.
		for (Entry<UUID, PlayerData> entry : players.entrySet()) {
			PlayerData playerData = entry.getValue();
			Player player = playerData.player;

			if (player != null && playerData.hasEffects) {
				final Location spawnLocation = player.getBedSpawnLocation();

				if (spawnLocation != null) {
					final Block spawnBlock = spawnLocation.getBlock();

					// Iterate all sides of the spawn location for a bed.
					for (BlockFace face : faces) {
						final Block checkBlock = spawnBlock.getRelative(face);
						final Location checkLocation = checkBlock.getLocation();

						// Check if this location is the bed that was broken.
						if (footLocation.equals(checkLocation) || headLocation.equals(checkLocation)) {
							player.removePotionEffect(PotionEffectType.INVISIBILITY);
							player.removePotionEffect(PotionEffectType.NIGHT_VISION);

							players.remove(entry.getKey());

							player.sendMessage("You jolt awake as you fall out of bed.");

							break;
						}
					}
				}
			}
		}
	}
}
