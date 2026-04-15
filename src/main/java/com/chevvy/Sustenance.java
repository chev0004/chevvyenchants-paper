package com.chevvy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class Sustenance {
	public static final String NBT_KEY = "chevvyenchants_sustenance";
	public static final String LORE_KEY = "enchantment.chevvyenchants.sustenance";

	private static final int POINTS_PER_FOOD = 64;
	private static final int FULL_SET_POINTS_PER_TICK = 8;
	private static final Map<UUID, Integer> FOOD_POINTS = new HashMap<>();

	private Sustenance() {}

	public static void register(JavaPlugin plugin) {
		Bukkit.getScheduler().runTaskTimer(plugin, Sustenance::tickAllWorlds, 20L, 20L);
	}

	private static void tickAllWorlds() {
		for (World world : Bukkit.getWorlds()) {
			for (Player player : world.getPlayers()) {
				int equipped = equippedCount(player);
				UUID id = player.getUniqueId();
				if (equipped <= 0) {
					FOOD_POINTS.remove(id);
					continue;
				}
				int gain = equipped < 4 ? equipped : FULL_SET_POINTS_PER_TICK;
				int points = FOOD_POINTS.getOrDefault(id, 0) + gain;
				while (points >= POINTS_PER_FOOD && player.getFoodLevel() < 20) {
					player.setFoodLevel(player.getFoodLevel() + 1);
					points -= POINTS_PER_FOOD;
				}
				FOOD_POINTS.put(id, points);
			}
		}
	}

	private static int equippedCount(Player player) {
		int count = 0;
		var inv = player.getInventory();
		if (isOnStack(inv.getHelmet(), player.getWorld())) {
			count++;
		}
		if (isOnStack(inv.getChestplate(), player.getWorld())) {
			count++;
		}
		if (isOnStack(inv.getLeggings(), player.getWorld())) {
			count++;
		}
		if (isOnStack(inv.getBoots(), player.getWorld())) {
			count++;
		}
		return count;
	}

	public static void applyEnchantToStack(ItemStack stack, World world) {
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.SUSTENANCE);
		ChevvyItemUtil.applyEnchant(stack, en, NBT_KEY, LORE_KEY);
	}

	public static void runAdd(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		if (Enchantment.getByKey(ChevvyEnchantKeys.SUSTENANCE) == null) {
			sender.sendMessage(Component.text("chevvyenchants:sustenance is not in the registry. Restart the server; check /datapack list for the plugin pack.", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (!isArmor(stack)) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.sustenance.need_armor"));
			return;
		}
		applyEnchantToStack(stack, player.getWorld());
		sender.sendMessage(Component.translatable("chevvyenchants.command.sustenance.added"));
	}

	public static void runClear(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (stack.getType().isAir()) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.sustenance.need_armor"));
			return;
		}
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.SUSTENANCE);
		ChevvyItemUtil.removeEnchant(stack, en, NBT_KEY, LORE_KEY);
		sender.sendMessage(Component.translatable("chevvyenchants.command.sustenance.cleared"));
	}

	public static boolean isOnStack(ItemStack stack, World world) {
		if (stack == null || stack.getType().isAir()) {
			return false;
		}
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.SUSTENANCE);
		return ChevvyItemUtil.hasEnchantOrLegacyNbt(stack, en, NBT_KEY);
	}

	private static boolean isArmor(ItemStack stack) {
		if (stack == null || stack.getType().isAir()) {
			return false;
		}
		return Tag.ITEMS_ENCHANTABLE_HEAD_ARMOR.isTagged(stack.getType())
			|| Tag.ITEMS_ENCHANTABLE_CHEST_ARMOR.isTagged(stack.getType())
			|| Tag.ITEMS_ENCHANTABLE_LEG_ARMOR.isTagged(stack.getType())
			|| Tag.ITEMS_ENCHANTABLE_FOOT_ARMOR.isTagged(stack.getType());
	}
}
