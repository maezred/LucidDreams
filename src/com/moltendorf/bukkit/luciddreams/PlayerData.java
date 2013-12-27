package com.moltendorf.bukkit.luciddreams;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 *
 * @author moltendorf
 */
public class PlayerData {
	public Player player;

	public double nextWarning = 0;

	public boolean hasEffects = false;
	public boolean readyForEffects = false;

	public BukkitTask taskFlagForEffects = null;

	public PlayerData(Player player) {
		this.player = player;
	}
}
