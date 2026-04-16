package com.chevvy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class Purify {
	public static final String NBT_KEY = "chevvyenchants_purify";
	public static final String LORE_KEY = "enchantment.chevvyenchants.purify";

	private static final List<PotionEffectType> NEGATIVE_EFFECTS = List.of(
		PotionEffectType.BLINDNESS,
		PotionEffectType.DARKNESS,
		PotionEffectType.HUNGER,
		PotionEffectType.MINING_FATIGUE,
		PotionEffectType.NAUSEA,
		PotionEffectType.POISON,
		PotionEffectType.SLOWNESS,
		PotionEffectType.WEAKNESS,
		PotionEffectType.WITHER,
		PotionEffectType.BAD_OMEN,
		PotionEffectType.LEVITATION,
		PotionEffectType.UNLUCK
	);

	private Purify() {}

	public static void register(JavaPlugin pl) {
		Bukkit.getPluginManager().registerEvents(new PurifyListener(), pl);
	}

	private static double chanceForLevel(int level) {
		return switch (level) {
			case 1 -> 0.10;
			case 2 -> 0.20;
			default -> 0.35;
		};
	}

	private static final class PurifyListener implements Listener {
		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onDamaged(EntityDamageEvent event) {
			if (!(event.getEntity() instanceof Player player)) {
				return;
			}
			ItemStack chest = player.getInventory().getChestplate();
			if (chest == null || chest.getType().isAir()) {
				return;
			}
			Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.PURIFY);
			if (en == null) {
				return;
			}
			int level = ChevvyItemUtil.getEnchantLevel(chest, en);
			if (level <= 0) {
				if (!ChevvyItemUtil.hasEnchantOrLegacyNbt(chest, en, NBT_KEY)) {
					return;
				}
				level = 1;
			}
			if (ThreadLocalRandom.current().nextDouble() >= chanceForLevel(level)) {
				return;
			}
			for (PotionEffectType type : NEGATIVE_EFFECTS) {
				if (player.hasPotionEffect(type)) {
					player.removePotionEffect(type);
				}
			}
			Frostbite.clearFrostbiteState(player);
		}
	}

	public static void applyEnchantToStack(ItemStack stack, World world) {
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.PURIFY);
		ChevvyItemUtil.applyEnchant(stack, en, NBT_KEY, LORE_KEY);
	}

	public static void runAdd(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		if (Enchantment.getByKey(ChevvyEnchantKeys.PURIFY) == null) {
			sender.sendMessage(Component.text("chevvyenchants:purify is not in the registry. Restart the server; check /datapack list for the plugin pack.", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (!Tag.ITEMS_ENCHANTABLE_CHEST_ARMOR.isTagged(stack.getType())) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.purify.need_chestplate"));
			return;
		}
		applyEnchantToStack(stack, player.getWorld());
		sender.sendMessage(Component.translatable("chevvyenchants.command.purify.added"));
	}

	public static void runClear(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (stack.getType().isAir()) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.purify.need_chestplate"));
			return;
		}
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.PURIFY);
		ChevvyItemUtil.removeEnchant(stack, en, NBT_KEY, LORE_KEY);
		sender.sendMessage(Component.translatable("chevvyenchants.command.purify.cleared"));
	}
}
