package net.moltendorf.Bukkit.LucidDreams;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.material.Bed;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by moltendorf on 15/04/10.
 *
 * @author moltendorf
 */
public class BedLookup {
	/**
	 *
	 */
	final static private Map<UUID, Map<Vector, Map<Vector, BedHandler>>> worldLookup = new HashMap<>();

	/**
	 * @param block Either the head or the foot block of the bed.
	 * @return An existing or new instance of BedHandler.
	 */
	static public BedHandler get(final Block block) {
		final Bed   blockData = (Bed)block.getState().getData();
		final Block head, foot;

	bed:
		if (blockData.isHeadOfBed()) {
			head = block;

			// Bed.getFacing() is bugged for headReference of bed (always returns east when someone is sleeping in the bed).
			for (BlockFace face : Arrays.asList(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)) {
				final Block test = block.getRelative(face);

				if (test.getType() == Material.BED_BLOCK && !((Bed)test.getState().getData()).isHeadOfBed()) {
					foot = test;

					break bed; // I hope insurance will cover this.
				}
			}

			return null;
		} else {
			head = block.getRelative(blockData.getFacing());
			foot = block;
		}

		final Vector headVector = head.getLocation().toVector();
		final Vector footVector = foot.getLocation().toVector();
		final UUID   worldId    = foot.getWorld().getUID();

		Map<Vector, Map<Vector, BedHandler>> headLookup = worldLookup.get(worldId);
		Map<Vector, BedHandler>              footLookup;
		BedHandler                           bedHandler;

		if (headLookup == null) {
			headLookup = new HashMap<>();
			footLookup = new HashMap<>();
			bedHandler = new BedHandler(new BedSet(head, foot));

			footLookup.put(footVector, bedHandler);
			headLookup.put(headVector, footLookup);
			worldLookup.put(worldId, headLookup);
		} else {
			footLookup = headLookup.get(headVector);

			if (footLookup == null) {
				footLookup = new HashMap<>();
				bedHandler = new BedHandler(new BedSet(head, foot));

				footLookup.put(footVector, bedHandler);
				headLookup.put(headVector, footLookup);
			} else {
				bedHandler = footLookup.get(footVector);

				if (bedHandler == null) {
					bedHandler = new BedHandler(new BedSet(head, foot));

					footLookup.put(footVector, bedHandler);
				}
			}
		}

		return bedHandler;
	}

	/**
	 * @param block Either the head or the foot block of the bed.
	 * @return The BedHandler that was removed.
	 */
	static public BedHandler remove(final Block block) {
		final Bed   blockData = (Bed)block.getState().getData();
		final Block head, foot;

	bed:
		if (blockData.isHeadOfBed()) {
			head = block;

			// Bed.getFacing() is bugged for headReference of bed (always returns east when someone is sleeping in the bed).
			for (BlockFace face : Arrays.asList(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)) {
				final Block test = block.getRelative(face);

				if (test.getType() == Material.BED_BLOCK && !((Bed)test.getState().getData()).isHeadOfBed()) {
					foot = test;

					break bed; // I hope insurance will cover this.
				}
			}

			return null;
		} else {
			head = block.getRelative(blockData.getFacing());
			foot = block;
		}

		final UUID worldId = foot.getWorld().getUID();
		final Map<Vector, Map<Vector, BedHandler>> headLookup = worldLookup.get(worldId);

		if (headLookup == null) {
			return null;
		}

		final Vector headVector = head.getLocation().toVector();
		final Map<Vector, BedHandler> footLookup = headLookup.get(headVector);

		if (footLookup == null) {
			return null;
		}

		final BedHandler bedHandler = footLookup.remove(foot.getLocation().toVector());

		if (bedHandler != null) {
			if (footLookup.size() == 0) {
				headLookup.remove(headVector);

				if (headLookup.size() == 0) {
					worldLookup.remove(worldId);
				}
			}
		}

		return bedHandler;
	}
}
