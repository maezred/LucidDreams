package com.moltendorf.bukkit.luciddreams;

import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author moltendorf
 */
public class Plugin extends JavaPlugin {

	// Variable context.
	protected static Configuration configuration = null;
	protected static Plugin instance = null;

	@Override
	public synchronized void onDisable() {

		// Clear context.
		configuration = null;
		instance = null;
	}

	@Override
	public synchronized void onEnable() {

		// For the most part, this shouldn't be needed.
		if (instance != null) {
			instance.onDisable();
		}

		// Prepare context.
		instance = this;

		// Construct new configuration.
		configuration = new Configuration();

		// Are we enabled?
		if (!configuration.global.enabled) {
			return;
		}

		// Get server.
		final Server server = getServer();

		// Get plugin manager.
		final PluginManager manager = server.getPluginManager();

		// Register our event listeners.
		manager.registerEvents(new Listeners(), this);
	}
}
