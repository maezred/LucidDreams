package net.moltendorf.Bukkit.LucidDreams;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author moltendorf
 */
public class Plugin extends JavaPlugin {

	// Main instance.
	public static Plugin instance = null;

	// Variable data.
	protected Configuration configuration = null;

	@Override
	public synchronized void onDisable() {
		instance = null;

		// Clear data.
		configuration = null;
	}

	@Override
	public synchronized void onEnable() {
		// Store reference to this.
		instance = this;

		// Construct new configuration.
		configuration = new Configuration();

		// Are we enabled?
		if (!configuration.global.enabled) {
			return;
		}

		// Register our event listeners.
		getServer().getPluginManager().registerEvents(new Listeners(), this);
	}
}
