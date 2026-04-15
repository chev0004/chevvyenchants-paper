package com.chevvy;

import org.bukkit.Bukkit;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class Windwalker {
	public static final String NBT_KEY = "chevvyenchants_windwalker";
	public static final String LORE_KEY = "enchantment.chevvyenchants.windwalker";

	private Windwalker() {}

	public static void register(JavaPlugin plugin) {
		Bukkit.getScheduler().runTaskTimer(plugin, Windwalker::tickAllWorlds, 0L, 20L);
	}

	private static void tickAllWorlds() {
		for (World world : Bukkit.getWorlds()) {
			for (Player player : world.getPlayers()) {
				ItemStack boots = player.getInventory().getBoots();
				if (boots == null || boots.getType().isAir()) {
					removeSpeedIfOurs(player);
					continue;
				}
				Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.WINDWALKER);
				if (en == null) {
					continue;
				}
				int level = ChevvyItemUtil.getEnchantLevel(boots, en);
				if (level <= 0) {
					if (ChevvyItemUtil.hasEnchantOrLegacyNbt(boots, en, NBT_KEY)) {
						level = 1;
					} else {
						removeSpeedIfOurs(player);
						continue;
					}
				}
				player.addPotionEffect(
					new PotionEffect(
						PotionEffectType.SPEED,
						40,
						level - 1,
						false,
						false,
						true
					)
				);
			}
		}
	}

	private static void removeSpeedIfOurs(Player player) {
		PotionEffect existing = player.getPotionEffect(PotionEffectType.SPEED);
		if (existing != null && !existing.isAmbient() && !existing.hasParticles() && existing.getDuration() <= 40) {
			player.removePotionEffect(PotionEffectType.SPEED);
		}
	}

	public static void applyEnchantToStack(ItemStack stack, World world) {
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.WINDWALKER);
		ChevvyItemUtil.applyEnchant(stack, en, NBT_KEY, LORE_KEY);
	}

	public static void runAdd(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		if (Enchantment.getByKey(ChevvyEnchantKeys.WINDWALKER) == null) {
			sender.sendMessage(Component.text("chevvyenchants:windwalker is not in the registry. Restart the server; check /datapack list for the plugin pack.", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (!Tag.ITEMS_ENCHANTABLE_FOOT_ARMOR.isTagged(stack.getType())) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.windwalker.need_boots"));
			return;
		}
		applyEnchantToStack(stack, player.getWorld());
		sender.sendMessage(Component.translatable("chevvyenchants.command.windwalker.added"));
	}

	public static void runClear(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (stack.getType().isAir()) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.windwalker.need_boots"));
			return;
		}
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.WINDWALKER);
		ChevvyItemUtil.removeEnchant(stack, en, NBT_KEY, LORE_KEY);
		sender.sendMessage(Component.translatable("chevvyenchants.command.windwalker.cleared"));
	}
}
