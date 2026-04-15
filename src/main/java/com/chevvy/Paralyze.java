package com.chevvy;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class Paralyze {
	public static final String NBT_KEY = "chevvyenchants_paralyze";
	public static final String LORE_KEY = "enchantment.chevvyenchants.paralyze";

	private static final Particle.DustOptions PARALYZE_PARTICLES =
		new Particle.DustOptions(Color.fromRGB(255, 220, 50), 1.0f);

	private Paralyze() {}

	public static void register(JavaPlugin pl) {
		Bukkit.getPluginManager().registerEvents(new ParalyzeListener(), pl);
	}

	private static final class ParalyzeListener implements Listener {
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
			Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.PARALYZE);
			if (en == null) {
				return;
			}
			int level = ChevvyItemUtil.getEnchantLevel(weapon, en);
			if (level <= 0) {
				return;
			}
			int ticks = 40 + 40 * level;
			int amplifier = level - 1;
			target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks, amplifier + 1, false, false, true));
			target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, ticks, amplifier, false, false, true));
			target.getWorld().spawnParticle(
				Particle.DUST,
				target.getLocation().add(0, target.getHeight() * 0.5, 0),
				15, 0.4, 0.6, 0.4, PARALYZE_PARTICLES
			);
		}
	}

	public static void applyEnchantToStack(ItemStack stack, World world) {
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.PARALYZE);
		ChevvyItemUtil.applyEnchant(stack, en, NBT_KEY, LORE_KEY);
	}

	public static void runAdd(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		if (Enchantment.getByKey(ChevvyEnchantKeys.PARALYZE) == null) {
			sender.sendMessage(Component.text("chevvyenchants:paralyze is not in the registry. Restart the server; check /datapack list for the plugin pack.", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (!Tag.ITEMS_SWORDS.isTagged(stack.getType())) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.paralyze.need_sword"));
			return;
		}
		applyEnchantToStack(stack, player.getWorld());
		sender.sendMessage(Component.translatable("chevvyenchants.command.paralyze.added"));
	}

	public static void runClear(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (stack.getType().isAir()) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.paralyze.need_sword"));
			return;
		}
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.PARALYZE);
		ChevvyItemUtil.removeEnchant(stack, en, NBT_KEY, LORE_KEY);
		sender.sendMessage(Component.translatable("chevvyenchants.command.paralyze.cleared"));
	}
}
