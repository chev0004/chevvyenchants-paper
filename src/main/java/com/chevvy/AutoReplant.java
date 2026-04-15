package com.chevvy;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
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

public final class AutoReplant {
	public static final String NBT_KEY = "chevvyenchants_auto_replant";
	public static final String LORE_KEY = "enchantment.chevvyenchants.auto_replant";

	private static JavaPlugin plugin;

	private AutoReplant() {}

	public static void register(JavaPlugin pl) {
		plugin = pl;
		Bukkit.getPluginManager().registerEvents(new AutoReplantListener(), pl);
	}

	private static Material seedForCrop(Material crop) {
		return switch (crop) {
			case WHEAT -> Material.WHEAT_SEEDS;
			case CARROTS -> Material.CARROT;
			case POTATOES -> Material.POTATO;
			case BEETROOTS -> Material.BEETROOT_SEEDS;
			case NETHER_WART -> Material.NETHER_WART;
			case TORCHFLOWER_CROP -> Material.TORCHFLOWER_SEEDS;
			case PITCHER_CROP -> Material.PITCHER_POD;
			default -> null;
		};
	}

	private static final class AutoReplantListener implements Listener {
		@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
		public void onBlockBreak(BlockBreakEvent event) {
			Player player = event.getPlayer();
			ItemStack tool = player.getInventory().getItemInMainHand();
			if (!Tag.ITEMS_HOES.isTagged(tool.getType())) {
				return;
			}
			Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.AUTO_REPLANT);
			if (en == null) {
				return;
			}
			int level = ChevvyItemUtil.getEnchantLevel(tool, en);
			if (level <= 0) {
				if (!ChevvyItemUtil.hasEnchantOrLegacyNbt(tool, en, NBT_KEY)) {
					return;
				}
			}
			Block block = event.getBlock();
			if (!(block.getBlockData() instanceof Ageable ageable)) {
				return;
			}
			if (ageable.getAge() < ageable.getMaximumAge()) {
				return;
			}
			Material seed = seedForCrop(block.getType());
			if (seed == null) {
				return;
			}
			Material cropType = block.getType();
			Bukkit.getScheduler().runTask(plugin, () -> {
				block.setType(cropType);
				if (block.getBlockData() instanceof Ageable newAgeable) {
					newAgeable.setAge(0);
					block.setBlockData(newAgeable);
				}
				ItemStack seedStack = new ItemStack(seed, 1);
				var inv = player.getInventory();
				boolean removed = false;
				for (int i = 0; i < inv.getSize(); i++) {
					ItemStack slot = inv.getItem(i);
					if (slot != null && slot.getType() == seed && slot.getAmount() > 0) {
						slot.setAmount(slot.getAmount() - 1);
						removed = true;
						break;
					}
				}
				if (!removed) {
					block.setType(Material.AIR);
				}
			});
		}
	}

	public static void applyEnchantToStack(ItemStack stack, World world) {
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.AUTO_REPLANT);
		ChevvyItemUtil.applyEnchant(stack, en, NBT_KEY, LORE_KEY);
	}

	public static void runAdd(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		if (Enchantment.getByKey(ChevvyEnchantKeys.AUTO_REPLANT) == null) {
			sender.sendMessage(Component.text("chevvyenchants:auto_replant is not in the registry. Restart the server; check /datapack list for the plugin pack.", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (!Tag.ITEMS_HOES.isTagged(stack.getType())) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.auto_replant.need_hoe"));
			return;
		}
		applyEnchantToStack(stack, player.getWorld());
		sender.sendMessage(Component.translatable("chevvyenchants.command.auto_replant.added"));
	}

	public static void runClear(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (stack.getType().isAir()) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.auto_replant.need_hoe"));
			return;
		}
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.AUTO_REPLANT);
		ChevvyItemUtil.removeEnchant(stack, en, NBT_KEY, LORE_KEY);
		sender.sendMessage(Component.translatable("chevvyenchants.command.auto_replant.cleared"));
	}
}
