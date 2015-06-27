package net.moltendorf.Bukkit.LucidDreams;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by moltendorf on 15/04/07.
 *
 * @author moltendorf
 */
public class PlayerLookup {
	static final private   Map<UUID, PlayerHandler>            playerLookup = new HashMap<>();
	static final protected Map<UUID, Map<UUID, PlayerHandler>> worldLookup  = new LinkedHashMap<>();

	static public PlayerHandler get(final Player player) {
		final UUID playerId = player.getUniqueId();

		PlayerHandler handler = playerLookup.get(playerId);

		if (handler == null) {
			handler = new PlayerHandler(player);
			playerLookup.put(playerId, handler);
		}

		return handler;
	}

	static public void put(final UUID worldId, final UUID playerId, final PlayerHandler playerHandler) {
		getPlayerLookupForWorld(worldId).put(playerId, playerHandler);
	}

	static public void remove(final UUID worldId, final UUID playerId) {
		Map<UUID, PlayerHandler> playerLookupForWorld = worldLookup.get(worldId);

		if (playerLookupForWorld == null) {
			return;
		}

		playerLookupForWorld.remove(playerId);

		if (playerLookupForWorld.size() == 0) {
			worldLookup.remove(worldId);
		}
	}

	static public Map<UUID, PlayerHandler> getPlayerLookupForWorld(final UUID worldId) {
		Map<UUID, PlayerHandler> playerLookupForWorld = worldLookup.get(worldId);

		if (playerLookupForWorld == null) {
			playerLookupForWorld = new LinkedHashMap<>();
			worldLookup.put(worldId, playerLookupForWorld);
		}

		return playerLookupForWorld;
	}

	static public void stopEffects(final UUID worldId) {
		final Map<UUID, PlayerHandler> playerLookupForWorld = worldLookup.remove(worldId);

		if (playerLookupForWorld == null) {
			return;
		}

		for (final Map.Entry<UUID, PlayerHandler> entry : playerLookupForWorld.entrySet()) {
			final PlayerHandler playerHandler = entry.getValue();

			playerHandler.activeWorlds.remove(worldId);

			if (worldId.equals(playerHandler.getPlayer().getWorld().getUID())) {
				playerHandler.stopEffects();
			}
		}
	}

	static public void extendEffects(final UUID worldId, final int duration) {
		final Map<UUID, PlayerHandler> playerLookupForWorld = worldLookup.get(worldId);

		if (playerLookupForWorld == null) {
			return;
		}

		for (final Map.Entry<UUID, PlayerHandler> entry : playerLookupForWorld.entrySet()) {
			final PlayerHandler playerHandler = entry.getValue();

			playerHandler.extendEffects(worldId, duration);
		}
	}
}
