package com.chevvy;

import org.bukkit.command.CommandSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class WitherTouch {
	public static final String NBT_KEY = "chevvyenchants_wither_touch";
	public static final String LORE_KEY = "enchantment.chevvyenchants.wither_touch";

	private WitherTouch() {}

	public static void register(JavaPlugin pl) {
		Bukkit.getPluginManager().registerEvents(new WitherTouchListener(), pl);
	}

	private static final class WitherTouchListener implements Listener {
		@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
		public void onMeleeHit(EntityDamageByEntityEvent event) {
			if (!(event.getDamager() instanceof Player player)) {
				return;
			}
			if (!(event.getEntity() instanceof LivingEntity target)) {
				return;
			}
			if (target.equals(player)) {
				return;
			}
			ItemStack weapon = player.getInventory().getItemInMainHand();
			if (!Tag.ITEMS_SWORDS.isTagged(weapon.getType())) {
				return;
			}
			Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.WITHER_TOUCH);
			if (en == null) {
				return;
			}
			int level = ChevvyItemUtil.getEnchantLevel(weapon, en);
			if (level <= 0) {
				return;
			}
			int ticks = 30 + 10 * level;
			int amplifier = level >= 5 ? 1 : 0;
			target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, ticks, amplifier, false, true, true));
			ChevvyDeathMessages.track(target, player, "wither_touch", weapon);
		}
	}

	public static void applyEnchantToStack(ItemStack stack, World world) {
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.WITHER_TOUCH);
		ChevvyItemUtil.applyEnchant(stack, en, NBT_KEY, LORE_KEY);
	}

	public static void runAdd(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		if (Enchantment.getByKey(ChevvyEnchantKeys.WITHER_TOUCH) == null) {
			sender.sendMessage(Component.text("chevvyenchants:wither_touch is not in the registry. Restart the server; check /datapack list for the plugin pack.", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (!Tag.ITEMS_SWORDS.isTagged(stack.getType())) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.wither_touch.need_sword"));
			return;
		}
		applyEnchantToStack(stack, player.getWorld());
		sender.sendMessage(Component.translatable("chevvyenchants.command.wither_touch.added"));
	}

	public static void runClear(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (stack.getType().isAir()) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.wither_touch.need_sword"));
			return;
		}
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.WITHER_TOUCH);
		ChevvyItemUtil.removeEnchant(stack, en, NBT_KEY, LORE_KEY);
		sender.sendMessage(Component.translatable("chevvyenchants.command.wither_touch.cleared"));
	}
}
