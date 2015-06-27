package net.moltendorf.Bukkit.LucidDreams;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.material.Bed;
import org.bukkit.util.Vector;

import java.lang.ref.SoftReference;
import java.util.UUID;

/**
 * Created by moltendorf on 15/04/07.
 *
 * @author moltendorf
 */
public class BedSet {
	public SoftReference<Block> headReference;
	public SoftReference<Block> footReference;
	public SoftReference<World> worldReference;

	// Lookups in case we lose the reference.
	final public Vector headVector;
	final public Vector footVector;
	final public UUID   worldId;

	public BedSet(final Block head, final Block foot) {
		headReference = new SoftReference<>(head);
		footReference = new SoftReference<>(foot);

		// Everybody loves the foot.
		final World world = foot.getWorld();

		worldReference = new SoftReference<>(world);

		headVector = head.getLocation().toVector();
		footVector = foot.getLocation().toVector();
		worldId = world.getUID();
	}

	public Block getHeadBlock() {
		final Block block = headReference.get();

		if (block == null) {
			return getWorld().getBlockAt(headVector.getBlockX(), headVector.getBlockY(), headVector.getBlockZ());
		}

		return block;
	}

	public Block getFootBlock() {
		final Block block = footReference.get();

		if (block == null) {
			return getWorld().getBlockAt(footVector.getBlockX(), footVector.getBlockY(), footVector.getBlockZ());
		}

		return block;
	}

	public World getWorld() {
		final World world = worldReference.get();

		if (world == null) {
			return Plugin.instance.getServer().getWorld(worldId);
		}

		return world;
	}

	public boolean isValid() {
		final Block      foot      = getFootBlock();
		final BlockState footState = foot.getState();
		final Material   type      = footState.getType();

		if (type != Material.BED_BLOCK) {
			return false;
		}

		final Bed data = ((Bed)footState.getData());

		return !data.isHeadOfBed() && foot.getRelative(data.getFacing()).getLocation().toVector().equals(headVector);
	}
}
