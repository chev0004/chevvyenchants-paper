package com.chevvy;

import org.bukkit.command.CommandSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;

public final class EmberHeart {
	public static final String NBT_KEY = "chevvyenchants_ember_heart";
	public static final String LORE_KEY = "enchantment.chevvyenchants.ember_heart";

	private EmberHeart() {}

	public static void register(JavaPlugin plugin) {
		Bukkit.getScheduler().runTaskTimer(plugin, EmberHeart::tickAllWorlds, 0L, 1L);
	}

	private static void tickAllWorlds() {
		for (var world : Bukkit.getWorlds()) {
			for (Player player : world.getPlayers()) {
				ItemStack chest = player.getInventory().getChestplate();
				if (isOnStack(chest, world)) {
					player.addPotionEffect(
						new PotionEffect(
							PotionEffectType.FIRE_RESISTANCE,
							PotionEffect.INFINITE_DURATION,
							1,
							false,
							false,
							false
						)
					);
					emberHeartSuppressFireOverlay(player);
				} else {
					PotionEffect existing = player.getPotionEffect(PotionEffectType.FIRE_RESISTANCE);
					if (existing != null
						&& existing.getDuration() == PotionEffect.INFINITE_DURATION
						&& existing.getAmplifier() == 1
						&& !existing.isAmbient()
						&& !existing.hasParticles()) {
						player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
					}
				}
			}
		}
	}

	private static void emberHeartSuppressFireOverlay(Player player) {
		if (player.getFireTicks() != 0) {
			player.setFireTicks(0);
		}
	}

	public static void applyEnchantToStack(ItemStack stack, World world) {
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.EMBER_HEART);
		ChevvyItemUtil.applyEnchant(stack, en, NBT_KEY, LORE_KEY);
	}

	public static void runAdd(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		if (Enchantment.getByKey(ChevvyEnchantKeys.EMBER_HEART) == null) {
			sender.sendMessage(Component.text("chevvyenchants:ember_heart is not in the registry. Restart the server; check /datapack list for the plugin pack.", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (!Tag.ITEMS_ENCHANTABLE_CHEST_ARMOR.isTagged(stack.getType())) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.ember_heart.need_chestplate"));
			return;
		}
		applyEnchantToStack(stack, player.getWorld());
		sender.sendMessage(Component.translatable("chevvyenchants.command.ember_heart.added"));
	}

	public static void runClear(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (stack.getType().isAir()) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.ember_heart.need_chestplate"));
			return;
		}
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.EMBER_HEART);
		ChevvyItemUtil.removeEnchant(stack, en, NBT_KEY, LORE_KEY);
		sender.sendMessage(Component.translatable("chevvyenchants.command.ember_heart.cleared"));
	}

	public static boolean isOnStack(ItemStack stack, World world) {
		if (stack == null || stack.getType().isAir()) {
			return false;
		}
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.EMBER_HEART);
		return ChevvyItemUtil.hasEnchantOrLegacyNbt(stack, en, NBT_KEY);
	}
}
