package com.moltendorf.bukkit.luciddreams;

import java.util.Arrays;
import java.util.HashSet;
import org.bukkit.entity.EntityType;

/**
 * Configuration class.
 *
 * @author moltendorf
 */
public class Configuration {

	static protected class Global {

		// Final data.
		final protected boolean enabled = true; // Whether or not the plugin is enabled at all; useful for using it as an interface (default is true).

		final protected HashSet disallowed = new HashSet(Arrays.asList(new EntityType[]{
			EntityType.BLAZE,
			EntityType.CAVE_SPIDER,
			EntityType.CREEPER,
			EntityType.ENDERMAN,
			EntityType.ENDER_DRAGON,
			EntityType.GHAST,
			EntityType.GIANT,
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
		}));
	}

	// Final data.
	final protected Global global = new Global();

	public Configuration() {

		// Placeholder.
	}
}
