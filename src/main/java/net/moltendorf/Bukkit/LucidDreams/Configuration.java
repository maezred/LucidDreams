package net.moltendorf.Bukkit.LucidDreams;

import org.bukkit.entity.EntityType;

import java.util.*;

/**
 * Configuration class.
 *
 * @author moltendorf
 */
public class Configuration {

	static protected class Global {

		// Final data.
		final protected boolean enabled = true; // Whether or not the plugin is enabled at all; useful for using it as an interface (default is true).

		final protected List<String> worlds = new ArrayList<>(Collections.singletonList("world"));
		final protected Set<UUID> worldIds;

		final protected HashSet<EntityType> disallowed = new HashSet<>(Arrays.asList(
			EntityType.BLAZE,
			EntityType.CAVE_SPIDER,
			EntityType.CREEPER,
			EntityType.ENDERMAN,
			EntityType.ENDERMITE,
			EntityType.ENDER_DRAGON,
			EntityType.GHAST,
			EntityType.GIANT,
			EntityType.GUARDIAN,
			EntityType.IRON_GOLEM,
			EntityType.MAGMA_CUBE,
			EntityType.PIG_ZOMBIE,
			EntityType.PLAYER,
			EntityType.SILVERFISH,
			EntityType.SKELETON,
			EntityType.SLIME,
			EntityType.SPIDER,
			EntityType.WITCH,
			EntityType.WITHER,
			EntityType.WOLF,
			EntityType.ZOMBIE
		));

		final protected HashSet<EntityType> creatures = new HashSet<>(Arrays.asList(
			EntityType.BLAZE,
			EntityType.CAVE_SPIDER,
			EntityType.CREEPER,
			EntityType.ENDERMAN,
			EntityType.ENDERMITE,
			EntityType.GIANT,
			EntityType.GUARDIAN,
			EntityType.IRON_GOLEM,
			EntityType.PIG_ZOMBIE,
			EntityType.SILVERFISH,
			EntityType.SKELETON,
			EntityType.SPIDER,
			EntityType.WITCH,
			EntityType.WITHER,
			EntityType.WOLF,
			EntityType.ZOMBIE
		));

		public Global() {
			worldIds = new LinkedHashSet<>();

			for (final String world : worlds) {
				 worldIds.add(Plugin.instance.getServer().getWorld(world).getUID());
			}
		}
	}

	// Final data.
	final protected Global global = new Global();

	public Configuration() {

		// Placeholder.
	}
}
