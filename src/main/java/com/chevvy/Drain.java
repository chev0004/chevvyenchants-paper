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

public final class Drain {
	public static final String NBT_KEY = "chevvyenchants_drain";
	public static final String LORE_KEY = "enchantment.chevvyenchants.drain";

	private static final int[] CHANCE_PERCENT = {15, 18, 21, 24, 27};
	private static final int[] DRAIN_MIN =      { 1,  1,  1,  2,  2};
	private static final int[] DRAIN_MAX =      { 1,  2,  2,  3,  4};

	private Drain() {}

	public static void register(JavaPlugin pl) {
		Bukkit.getPluginManager().registerEvents(new DrainListener(), pl);
	}

	private static final class DrainListener implements Listener {
		@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
		public void onMeleeHit(EntityDamageByEntityEvent event) {
			if (!(event.getDamager() instanceof Player player)) {
				return;
			}
			if (!(event.getEntity() instanceof Player target)) {
				return;
			}
			if (target.equals(player)) {
				return;
			}
			ItemStack weapon = player.getInventory().getItemInMainHand();
			if (!Tag.ITEMS_SWORDS.isTagged(weapon.getType())) {
				return;
			}
			Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.DRAIN);
			if (en == null) {
				return;
			}
			int level = ChevvyItemUtil.getEnchantLevel(weapon, en);
			if (level <= 0) {
				return;
			}
			int idx = Math.min(level, CHANCE_PERCENT.length) - 1;
			if (ThreadLocalRandom.current().nextInt(100) >= CHANCE_PERCENT[idx]) {
				return;
			}
			int min = DRAIN_MIN[idx];
			int max = DRAIN_MAX[idx];
			int amount = min == max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
			int newFood = Math.max(0, target.getFoodLevel() - amount);
			target.setFoodLevel(newFood);
			float newSat = Math.max(0f, target.getSaturation() - amount);
			target.setSaturation(newSat);
			target.getWorld().spawnParticle(
				Particle.DUST,
				target.getLocation().add(0, target.getHeight() * 0.5, 0),
				15, 0.3, 0.4, 0.3,
				new Particle.DustOptions(Color.fromRGB(90, 120, 30), 1.2f)
			);
			target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.6f, 0.5f);
		}
	}

	public static void applyEnchantToStack(ItemStack stack, World world) {
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.DRAIN);
		ChevvyItemUtil.applyEnchant(stack, en, NBT_KEY, LORE_KEY);
	}

	public static void runAdd(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		if (Enchantment.getByKey(ChevvyEnchantKeys.DRAIN) == null) {
			sender.sendMessage(Component.text("chevvyenchants:drain is not in the registry. Restart the server; check /datapack list for the plugin pack.", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (!Tag.ITEMS_SWORDS.isTagged(stack.getType())) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.drain.need_sword"));
			return;
		}
		applyEnchantToStack(stack, player.getWorld());
		sender.sendMessage(Component.translatable("chevvyenchants.command.drain.added"));
	}

	public static void runClear(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (stack.getType().isAir()) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.drain.need_sword"));
			return;
		}
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.DRAIN);
		ChevvyItemUtil.removeEnchant(stack, en, NBT_KEY, LORE_KEY);
		sender.sendMessage(Component.translatable("chevvyenchants.command.drain.cleared"));
	}
}
