package com.chevvy;

import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class Deflect {
	public static final String NBT_KEY = "chevvyenchants_deflect";
	public static final String LORE_KEY = "enchantment.chevvyenchants.deflect";

	private static final double CHANCE_PER_PIECE = 0.05;

	private static final Particle.DustOptions DEFLECT_PARTICLES =
		new Particle.DustOptions(Color.fromRGB(200, 220, 255), 1.2f);

	private Deflect() {}

	public static void register(JavaPlugin pl) {
		Bukkit.getPluginManager().registerEvents(new DeflectListener(), pl);
	}

	private static final class DeflectListener implements Listener {
		@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
		public void onDamaged(EntityDamageByEntityEvent event) {
			if (!(event.getEntity() instanceof Player player)) {
				return;
			}
			Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.DEFLECT);
			if (en == null) {
				return;
			}
			int pieces = equippedCount(player, en);
			if (pieces <= 0) {
				return;
			}
			double chance = CHANCE_PER_PIECE * pieces;
			if (ThreadLocalRandom.current().nextDouble() >= chance) {
				return;
			}
			event.setCancelled(true);
			player.getWorld().spawnParticle(
				Particle.DUST,
				player.getLocation().add(0, player.getHeight() * 0.5, 0),
				20, 0.4, 0.6, 0.4, DEFLECT_PARTICLES
			);
			player.getWorld().playSound(
				player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.4f
			);
		}
	}

	private static int equippedCount(Player player, Enchantment en) {
		int count = 0;
		var inv = player.getInventory();
		if (hasDeflect(inv.getHelmet(), en)) count++;
		if (hasDeflect(inv.getChestplate(), en)) count++;
		if (hasDeflect(inv.getLeggings(), en)) count++;
		if (hasDeflect(inv.getBoots(), en)) count++;
		return count;
	}

	private static boolean hasDeflect(ItemStack stack, Enchantment en) {
		if (stack == null || stack.getType().isAir()) {
			return false;
		}
		return ChevvyItemUtil.getEnchantLevel(stack, en) > 0;
	}

	public static void applyEnchantToStack(ItemStack stack, World world) {
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.DEFLECT);
		ChevvyItemUtil.applyEnchant(stack, en, NBT_KEY, LORE_KEY);
	}

	public static void runAdd(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		if (Enchantment.getByKey(ChevvyEnchantKeys.DEFLECT) == null) {
			sender.sendMessage(Component.text("chevvyenchants:deflect is not in the registry. Restart the server; check /datapack list for the plugin pack.", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (!isArmor(stack)) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.deflect.need_armor"));
			return;
		}
		applyEnchantToStack(stack, player.getWorld());
		sender.sendMessage(Component.translatable("chevvyenchants.command.deflect.added"));
	}

	public static void runClear(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (stack.getType().isAir()) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.deflect.need_armor"));
			return;
		}
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.DEFLECT);
		ChevvyItemUtil.removeEnchant(stack, en, NBT_KEY, LORE_KEY);
		sender.sendMessage(Component.translatable("chevvyenchants.command.deflect.cleared"));
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
