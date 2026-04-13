package com.chevvy;

import org.bukkit.command.CommandSender;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public final class Gravedigger {
	public static final String NBT_KEY = "chevvyenchants_gravedigger";
	public static final String LORE_KEY = "enchantment.chevvyenchants.gravedigger";

	private static JavaPlugin plugin;

	private Gravedigger() {}

	public static void register(JavaPlugin pl) {
		plugin = pl;
		Bukkit.getPluginManager().registerEvents(new GravediggerListener(), pl);
	}

	private static final class GravediggerListener implements Listener {
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
			if (!Tag.ITEMS_SHOVELS.isTagged(tool.getType()) || !isOnStack(tool, player.getWorld())) {
				return;
			}
			Block block = event.getBlock();
			Tag<Material> mineable = Bukkit.getTag(Tag.REGISTRY_BLOCKS, NamespacedKey.minecraft("mineable/shovel"), Material.class);
			if (mineable == null || !mineable.isTagged(block.getType())) {
				return;
			}
			Vector look = player.getEyeLocation().getDirection();
			ItemStack toolSnapshot = tool.clone();
			org.bukkit.Location loc = block.getLocation();
			Bukkit.getScheduler().runTask(plugin, () -> {
				if (!player.isOnline()) {
					return;
				}
				Block center = loc.getBlock();
				List<Block> extra = ChevvyMiningPlane.offsetsForPlane(center, look);
				Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_BLOCKS, NamespacedKey.minecraft("mineable/shovel"), Material.class);
				ChevvyMiningPlane.setChainedBreak(true);
				try {
					for (Block target : extra) {
						if (target.getX() == center.getX() && target.getY() == center.getY() && target.getZ() == center.getZ()) {
							continue;
						}
						if (target.getType().isAir() || tag == null || !tag.isTagged(target.getType())) {
							continue;
						}
						target.breakNaturally(toolSnapshot);
					}
				} finally {
					ChevvyMiningPlane.setChainedBreak(false);
				}
			});
		}
	}

	public static void applyEnchantToStack(ItemStack stack, World world) {
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.GRAVEDIGGER);
		ChevvyItemUtil.applyEnchant(stack, en, NBT_KEY, LORE_KEY);
	}

	public static void runAdd(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		if (Enchantment.getByKey(ChevvyEnchantKeys.GRAVEDIGGER) == null) {
			sender.sendMessage(Component.text("chevvyenchants:gravedigger is not in the registry. Restart the server; check /datapack list for the plugin pack.", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (!Tag.ITEMS_SHOVELS.isTagged(stack.getType())) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.gravedigger.need_shovel"));
			return;
		}
		applyEnchantToStack(stack, player.getWorld());
		sender.sendMessage(Component.translatable("chevvyenchants.command.gravedigger.added"));
	}

	public static void runClear(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (stack.getType().isAir()) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.gravedigger.need_shovel"));
			return;
		}
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.GRAVEDIGGER);
		ChevvyItemUtil.removeEnchant(stack, en, NBT_KEY, LORE_KEY);
		sender.sendMessage(Component.translatable("chevvyenchants.command.gravedigger.cleared"));
	}

	public static boolean isOnStack(ItemStack stack, World world) {
		if (stack == null || stack.getType().isAir()) {
			return false;
		}
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.GRAVEDIGGER);
		return ChevvyItemUtil.hasEnchantOrLegacyNbt(stack, en, NBT_KEY);
	}
}
