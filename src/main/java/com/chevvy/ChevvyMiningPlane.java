package com.chevvy;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

final class ChevvyMiningPlane {
	private static final ThreadLocal<Boolean> IN_CHAIN = ThreadLocal.withInitial(() -> false);

	private ChevvyMiningPlane() {}

	static boolean inChainedBreak() {
		return IN_CHAIN.get();
	}

	static void setChainedBreak(boolean value) {
		IN_CHAIN.set(value);
	}

	static List<Block> offsetsForPlane(Block center, Vector look) {
		double ax = Math.abs(look.getX());
		double ay = Math.abs(look.getY());
		double az = Math.abs(look.getZ());
		List<Block> out = new ArrayList<>(9);
		World world = center.getWorld();
		int x = center.getX();
		int y = center.getY();
		int z = center.getZ();
		if (ax >= ay && ax >= az) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					out.add(world.getBlockAt(x, y + dy, z + dz));
				}
			}
		} else if (ay >= ax && ay >= az) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					out.add(world.getBlockAt(x + dx, y, z + dz));
				}
			}
		} else {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					out.add(world.getBlockAt(x + dx, y + dy, z));
				}
			}
		}
		return out;
	}
}
