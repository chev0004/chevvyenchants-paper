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

public final class Gills {
	public static final String NBT_KEY = "chevvyenchants_gills";
	public static final String LORE_KEY = "enchantment.chevvyenchants.gills";

	private Gills() {}

	public static void register(JavaPlugin plugin) {
		Bukkit.getScheduler().runTaskTimer(plugin, Gills::tickAllWorlds, 0L, 1L);
	}

	private static void tickAllWorlds() {
		for (var world : Bukkit.getWorlds()) {
			for (Player player : world.getPlayers()) {
				ItemStack helmet = player.getInventory().getHelmet();
				if (isOnStack(helmet, world)) {
					player.addPotionEffect(
						new PotionEffect(
							PotionEffectType.WATER_BREATHING,
							PotionEffect.INFINITE_DURATION,
							0,
							false,
							false,
							false
						)
					);
				} else {
					PotionEffect existing = player.getPotionEffect(PotionEffectType.WATER_BREATHING);
					if (existing != null
						&& existing.getDuration() == PotionEffect.INFINITE_DURATION
						&& existing.getAmplifier() == 0
						&& !existing.isAmbient()
						&& !existing.hasParticles()) {
						player.removePotionEffect(PotionEffectType.WATER_BREATHING);
					}
				}
			}
		}
	}

	public static void applyEnchantToStack(ItemStack stack, World world) {
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.GILLS);
		ChevvyItemUtil.applyEnchant(stack, en, NBT_KEY, LORE_KEY);
	}

	public static void runAdd(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		if (Enchantment.getByKey(ChevvyEnchantKeys.GILLS) == null) {
			sender.sendMessage(Component.text("chevvyenchants:gills is not in the registry. Restart the server; check /datapack list for the plugin pack.", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (!Tag.ITEMS_ENCHANTABLE_HEAD_ARMOR.isTagged(stack.getType())) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.gills.need_helmet"));
			return;
		}
		applyEnchantToStack(stack, player.getWorld());
		sender.sendMessage(Component.translatable("chevvyenchants.command.gills.added"));
	}

	public static void runClear(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (stack.getType().isAir()) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.gills.need_helmet"));
			return;
		}
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.GILLS);
		ChevvyItemUtil.removeEnchant(stack, en, NBT_KEY, LORE_KEY);
		sender.sendMessage(Component.translatable("chevvyenchants.command.gills.cleared"));
	}

	private static boolean isOnStack(ItemStack stack, World world) {
		if (stack == null || stack.getType().isAir()) {
			return false;
		}
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.GILLS);
		return ChevvyItemUtil.hasEnchantOrLegacyNbt(stack, en, NBT_KEY);
	}
}
