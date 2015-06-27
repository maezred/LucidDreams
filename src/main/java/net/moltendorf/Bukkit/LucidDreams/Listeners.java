package net.moltendorf.Bukkit.LucidDreams;

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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.*;
import org.bukkit.material.Bed;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.Map.Entry;

/**
 * Listener register.
 *
 * @author moltendorf
 */
public class Listeners implements Listener {
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void EntityDamageByEntityEventHandler(final EntityDamageByEntityEvent event) {

		// Are we enabled at all?
		if (!Plugin.instance.configuration.global.enabled) {
			return;
		}

		final Entity damager = event.getDamager();

		if (damager == null) {
			return;
		}

		final Player player;

		final EntityType type = damager.getType();

		final ProjectileSource shooter;

		switch (type) {
			case ARROW:
				shooter = ((Arrow) damager).getShooter();

				if (shooter != null && shooter instanceof Player) {
					player = (Player) shooter;
				} else {
					return;
				}

				break;

			case PLAYER:
				if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
					// Convert Entity to Player.
					player = (Player) damager;
				} else {
					return;
				}

				break;

			case SPLASH_POTION:
				shooter = ((ThrownPotion) damager).getShooter();

				if (shooter != null && shooter instanceof Player) {
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

		// No cancelling shooting yourself in the footReference.
		if (entity.getUniqueId() == id) {
			return;
		}

		final PlayerHandler playerHandler = players.get(id);

		if (playerHandler == null || !playerHandler.effects) {
			return;
		}

		EntityType targetType = event.getEntityType();

		// Is this entity allowed to be attacked?
		if (!Plugin.instance.configuration.global.disallowed.contains(targetType)) {
			return;
		}

		if (Plugin.instance.configuration.global.creatures.contains(targetType)) {
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

		if (currentWarning > playerHandler.nextWarning) {
			// Negate all damage dealt to the entity.
			event.setCancelled(true);

			player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 4));

			// The warning period lasts for two seconds.
			playerHandler.nextWarning = currentWarning + 40;

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

		final PlayerHandler playerHandler = players.get(id);

		if (playerHandler == null) {
			return;
		}

		if (playerHandler.effects) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void PlayerBedEnterEventMonitor(final PlayerBedEnterEvent event) {

		// Are we enabled at all?
		if (!plugin.configuration.global.enabled) {
			return;
		}

		System.out.println(((Bed) event.getBed().getState().getData()).isHeadOfBed());

		final Player player = event.getPlayer();
		final UUID id = player.getUniqueId();

		final PlayerHandler playerHandler;
		PlayerHandler fetchedPlayerHandler = players.get(id);

		if (fetchedPlayerHandler == null) {
			playerHandler = new PlayerHandler(player);
			players.put(id, playerHandler);
		} else {
			playerHandler = fetchedPlayerHandler;

			if (playerHandler.taskFlagForEffects != null) {
				playerHandler.taskFlagForEffects.cancel();
				playerHandler.taskFlagForEffects = null;
			}
		}

		playerHandler.taskFlagForEffects = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
			// Just for safety.
			if (player.isSleeping()) {
				if (playerHandler.effects) {
					player.sendMessage("You feel the warm covers in your dream.");
				} else {
					player.sendMessage("You slowly drift to sleep.");
				}

				playerHandler.readyForEffects = true;
				playerHandler.taskFlagForEffects = null;
			}
		}, 40);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void PlayerBedLeaveEventMonitor(final PlayerBedLeaveEvent event) {

		// Are we enabled at all?
		if (!plugin.configuration.global.enabled) {
			return;
		}

		final Player player = event.getPlayer();
		final UUID id = player.getUniqueId();

		final PlayerHandler playerHandler = players.get(id);

		if (playerHandler == null) {
			// This shouldn't happen.
			return;
		}

		plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
			if (playerHandler.readyForEffects) {
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
					playerHandler.nextWarning = world.getFullTime() + regenerationDuration;

					if (playerHandler.effects) {
						player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, regenerationDuration, 1, true));

						player.sendMessage("This dream worldReference suddenly feels very cold.");
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

						playerHandler.effects = true;

						if (clock == null) {
							// Run the task 12 seconds before the effects run out to prevent screen flashing.
							clock = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
								if (world.hasStorm()) {
									int ticks = world.getWeatherDuration() + 600;

									PlayerLookup.extendEffects(world.getUID(), ticks);

									clock = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
										clock = null;

										PlayerLookup.stopEffects(world.getUID());
									}, ticks);
								} else {
									clock = null;

									PlayerLookup.stopEffects(world.getUID());
								}
							}, duration - 240);
						}
					}
				} else {
					player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, regenerationDuration, 1, true));

					PlayerLookup.stopEffects(world.getUID());
				}

				playerHandler.readyForEffects = false;
			} else if (playerHandler.taskFlagForEffects != null) {
				playerHandler.taskFlagForEffects.cancel();
				playerHandler.taskFlagForEffects = null;
			}
		}, 0);
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

			final PlayerHandler playerHandler = players.get(id);

			if (playerHandler == null) {
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

		final PlayerHandler playerHandler = players.get(id);

		if (playerHandler == null) {
			return;
		}

		playerHandler.player = null;

		if (!playerHandler.effects) {
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

		final PlayerHandler playerHandler = players.get(id);

		if (playerHandler == null) {
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

		final PlayerHandler playerHandler = players.get(id);

		if (playerHandler == null) {
			return;
		}

		playerHandler.player = player;

		if (!playerHandler.effects) {
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

		// We only use the input block to figure out which block is the headReference and the footReference of the bed.
		final Block block = event.getBlock();

		if (set == null) {
			set = BedSet.getBedAt(block);
		}

		if (block == null || block.getType() != Material.BED_BLOCK) {
			return;
		}

		final Bed blockData = (Bed) block.getState().getData();
		final Block head, foot;

		bed:
		if (blockData.isHeadOfBed()) {
			head = block;

			// Bed.getFacing() is bugged for headReference of bed (always returns east when someone is sleeping in the bed).
			for (BlockFace face : Arrays.asList(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)) {
				Block test = block.getRelative(face);

				if (test.getType() == Material.BED_BLOCK && !((Bed) test.getState().getData()).isHeadOfBed()) {
					foot = test;

					break bed;
				}
			}

			return;
		} else {
			head = block.getRelative(blockData.getFacing());
			foot = block;
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
		for (final Iterator<Entry<UUID, PlayerHandler>> iterator = players.entrySet().iterator(); iterator.hasNext(); ) {
			final Entry<UUID, PlayerHandler> entry = iterator.next();

			final PlayerHandler playerHandler = entry.getValue();
			final Player player = playerHandler.player;

			if (player != null && playerHandler.effects) {
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

							iterator.remove();

							player.sendMessage("You jolt awake as you fall out of bed.");

							break;
						}
					}
				}
			}
		}
	}
}
