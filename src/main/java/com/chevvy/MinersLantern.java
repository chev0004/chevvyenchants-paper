package com.chevvy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class MinersLantern {
	public static final String NBT_KEY = "chevvyenchants_miners_lantern";
	public static final String LORE_KEY = "enchantment.chevvyenchants.miners_lantern";

	private static final Map<UUID, Integer> MINE_COUNTS = new HashMap<>();

	private MinersLantern() {}

	public static void register(JavaPlugin pl) {
		Bukkit.getPluginManager().registerEvents(new MinersLanternListener(), pl);
	}

	private static int thresholdForLevel(int level) {
		return switch (level) {
			case 1 -> 5;
			case 2 -> 4;
			case 3 -> 3;
			case 4 -> 2;
			default -> 1;
		};
	}

	private static final class MinersLanternListener implements Listener {
		@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
		public void onBlockBreak(BlockBreakEvent event) {
			Player player = event.getPlayer();
			ItemStack tool = player.getInventory().getItemInMainHand();
			if (!Tag.ITEMS_PICKAXES.isTagged(tool.getType())) {
				return;
			}
			Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.MINERS_LANTERN);
			if (en == null) {
				return;
			}
			int level = ChevvyItemUtil.getEnchantLevel(tool, en);
			if (level <= 0) {
				return;
			}
			Tag<Material> mineable = Bukkit.getTag(Tag.REGISTRY_BLOCKS, NamespacedKey.minecraft("mineable/pickaxe"), Material.class);
			if (mineable == null || !mineable.isTagged(event.getBlock().getType())) {
				return;
			}
			UUID id = player.getUniqueId();
			int count = MINE_COUNTS.getOrDefault(id, 0) + 1;
			int threshold = thresholdForLevel(level);
			if (count >= threshold) {
				MINE_COUNTS.put(id, 0);
				Block feet = player.getLocation().getBlock();
				if (feet.getType().isAir()) {
					feet.setType(Material.LIGHT);
				}
			} else {
				MINE_COUNTS.put(id, count);
			}
		}
	}

	public static void applyEnchantToStack(ItemStack stack, World world) {
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.MINERS_LANTERN);
		ChevvyItemUtil.applyEnchant(stack, en, NBT_KEY, LORE_KEY);
	}

	public static void runAdd(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		if (Enchantment.getByKey(ChevvyEnchantKeys.MINERS_LANTERN) == null) {
			sender.sendMessage(Component.text("chevvyenchants:miners_lantern is not in the registry. Restart the server; check /datapack list for the plugin pack.", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (!Tag.ITEMS_PICKAXES.isTagged(stack.getType())) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.miners_lantern.need_pickaxe"));
			return;
		}
		applyEnchantToStack(stack, player.getWorld());
		sender.sendMessage(Component.translatable("chevvyenchants.command.miners_lantern.added"));
	}

	public static void runClear(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (stack.getType().isAir()) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.miners_lantern.need_pickaxe"));
			return;
		}
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.MINERS_LANTERN);
		ChevvyItemUtil.removeEnchant(stack, en, NBT_KEY, LORE_KEY);
		sender.sendMessage(Component.translatable("chevvyenchants.command.miners_lantern.cleared"));
	}
}
