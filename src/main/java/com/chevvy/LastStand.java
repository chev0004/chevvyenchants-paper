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

public final class LastStand {
	public static final String NBT_KEY = "chevvyenchants_last_stand";
	public static final String LORE_KEY = "enchantment.chevvyenchants.last_stand";

	private static final double HP_THRESHOLD = 0.2;

	private LastStand() {}

	public static void register(JavaPlugin plugin) {
		Bukkit.getScheduler().runTaskTimer(plugin, LastStand::tickAllWorlds, 0L, 1L);
	}

	private static void tickAllWorlds() {
		for (World world : Bukkit.getWorlds()) {
			for (Player player : world.getPlayers()) {
				ItemStack chest = player.getInventory().getChestplate();
				if (isOnStack(chest, world)
					&& player.getHealth() / player.getMaxHealth() <= HP_THRESHOLD) {
					player.addPotionEffect(
						new PotionEffect(
							PotionEffectType.RESISTANCE,
							PotionEffect.INFINITE_DURATION,
							0,
							false,
							false,
							false
						)
					);
					player.addPotionEffect(
						new PotionEffect(
							PotionEffectType.STRENGTH,
							PotionEffect.INFINITE_DURATION,
							0,
							false,
							false,
							false
						)
					);
				} else {
					removeIfOurs(player, PotionEffectType.RESISTANCE);
					removeIfOurs(player, PotionEffectType.STRENGTH);
				}
			}
		}
	}

	private static void removeIfOurs(Player player, PotionEffectType type) {
		PotionEffect existing = player.getPotionEffect(type);
		if (existing != null
			&& existing.getDuration() == PotionEffect.INFINITE_DURATION
			&& existing.getAmplifier() == 0
			&& !existing.isAmbient()
			&& !existing.hasParticles()) {
			player.removePotionEffect(type);
		}
	}

	public static void applyEnchantToStack(ItemStack stack, World world) {
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.LAST_STAND);
		ChevvyItemUtil.applyEnchant(stack, en, NBT_KEY, LORE_KEY);
	}

	public static void runAdd(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		if (Enchantment.getByKey(ChevvyEnchantKeys.LAST_STAND) == null) {
			sender.sendMessage(Component.text("chevvyenchants:last_stand is not in the registry. Restart the server; check /datapack list for the plugin pack.", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (!Tag.ITEMS_ENCHANTABLE_CHEST_ARMOR.isTagged(stack.getType())) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.last_stand.need_chestplate"));
			return;
		}
		applyEnchantToStack(stack, player.getWorld());
		sender.sendMessage(Component.translatable("chevvyenchants.command.last_stand.added"));
	}

	public static void runClear(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (stack.getType().isAir()) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.last_stand.need_chestplate"));
			return;
		}
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.LAST_STAND);
		ChevvyItemUtil.removeEnchant(stack, en, NBT_KEY, LORE_KEY);
		sender.sendMessage(Component.translatable("chevvyenchants.command.last_stand.cleared"));
	}

	private static boolean isOnStack(ItemStack stack, World world) {
		if (stack == null || stack.getType().isAir()) {
			return false;
		}
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.LAST_STAND);
		return ChevvyItemUtil.hasEnchantOrLegacyNbt(stack, en, NBT_KEY);
	}
}
