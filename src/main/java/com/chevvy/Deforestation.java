package com.chevvy;

import org.bukkit.command.CommandSender;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class Deforestation {
	public static final String NBT_KEY = "chevvyenchants_deforestation";
	public static final String LORE_KEY = "enchantment.chevvyenchants.deforestation";
	private static final int MAX_LOGS = 6144;
	private static final BlockFace[] FACES = {
		BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
	};

	private static JavaPlugin plugin;

	private Deforestation() {}

	public static void register(JavaPlugin pl) {
		plugin = pl;
		Bukkit.getPluginManager().registerEvents(new DeforestationListener(), pl);
	}

	private static final class DeforestationListener implements Listener {
		@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
		public void onBlockBreak(BlockBreakEvent event) {
			if (ChevvyMiningPlane.inChainedBreak()) {
				return;
			}
			Player player = event.getPlayer();
			if (player.isSneaking()) {
				return;
			}
			ItemStack tool = player.getInventory().getItemInMainHand();
			if (!Tag.ITEMS_AXES.isTagged(tool.getType()) || !isOnStack(tool, player.getWorld())) {
				return;
			}
			Block start = event.getBlock();
			Tag<Material> logs = Bukkit.getTag(Tag.REGISTRY_BLOCKS, NamespacedKey.minecraft("logs"), Material.class);
			if (logs == null || !logs.isTagged(start.getType())) {
				return;
			}
			List<Block> toBreak = collectConnectedLogs(start, logs);
			if (toBreak.size() <= 1) {
				return;
			}
			ItemStack toolSnapshot = tool.clone();
			World world = start.getWorld();
			Bukkit.getScheduler().runTask(plugin, () -> {
				if (!player.isOnline()) {
					return;
				}
				ChevvyMiningPlane.setChainedBreak(true);
				try {
					for (Block b : toBreak) {
						if (b.getType().isAir() || !logs.isTagged(b.getType())) {
							continue;
						}
						b.breakNaturally(toolSnapshot);
					}
				} finally {
					ChevvyMiningPlane.setChainedBreak(false);
				}
			});
		}
	}

	private static List<Block> collectConnectedLogs(Block start, Tag<Material> logs) {
		ArrayDeque<Block> queue = new ArrayDeque<>();
		Set<Block> seen = new HashSet<>();
		queue.add(start);
		seen.add(start);
		while (!queue.isEmpty() && seen.size() < MAX_LOGS) {
			Block cur = queue.poll();
			for (BlockFace face : FACES) {
				if (seen.size() >= MAX_LOGS) {
					break;
				}
				Block n = cur.getRelative(face);
				if (!logs.isTagged(n.getType())) {
					continue;
				}
				if (seen.add(n)) {
					queue.add(n);
				}
			}
		}
		return new ArrayList<>(seen);
	}

	public static void applyEnchantToStack(ItemStack stack, World world) {
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.DEFORESTATION);
		ChevvyItemUtil.applyEnchant(stack, en, NBT_KEY, LORE_KEY);
	}

	public static void runAdd(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		if (Enchantment.getByKey(ChevvyEnchantKeys.DEFORESTATION) == null) {
			sender.sendMessage(Component.text("chevvyenchants:deforestation is not in the registry. Restart the server; check /datapack list for the plugin pack.", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (!Tag.ITEMS_AXES.isTagged(stack.getType())) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.deforestation.need_axe"));
			return;
		}
		applyEnchantToStack(stack, player.getWorld());
		sender.sendMessage(Component.translatable("chevvyenchants.command.deforestation.added"));
	}

	public static void runClear(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (stack.getType().isAir()) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.deforestation.need_axe"));
			return;
		}
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.DEFORESTATION);
		ChevvyItemUtil.removeEnchant(stack, en, NBT_KEY, LORE_KEY);
		sender.sendMessage(Component.translatable("chevvyenchants.command.deforestation.cleared"));
	}

	public static boolean isOnStack(ItemStack stack, World world) {
		if (stack == null || stack.getType().isAir()) {
			return false;
		}
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.DEFORESTATION);
		return ChevvyItemUtil.hasEnchantOrLegacyNbt(stack, en, NBT_KEY);
	}
}
