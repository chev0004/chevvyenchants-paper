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

public final class Frostbite {
	public static final String NBT_KEY = "chevvyenchants_frostbite";
	public static final String LORE_KEY = "enchantment.chevvyenchants.frostbite";

	private static final Particle.DustOptions FROST_PARTICLES =
		new Particle.DustOptions(Color.fromRGB(150, 200, 255), 1.0f);

	private Frostbite() {}

	public static void register(JavaPlugin pl) {
		Bukkit.getPluginManager().registerEvents(new FrostbiteListener(), pl);
		Bukkit.getScheduler().runTaskTimer(pl, Frostbite::tickFreeze, 0L, 1L);
	}

	private static void tickFreeze() {
		for (World world : Bukkit.getWorlds()) {
			for (LivingEntity entity : world.getLivingEntities()) {
				if (entity.getFreezeTicks() <= 0) {
					continue;
				}
				if (!entity.hasMetadata("chevvy_frostbite")) {
					continue;
				}
				int target = entity.getMetadata("chevvy_frostbite").get(0).asInt();
				if (entity.getFreezeTicks() < target) {
					entity.setFreezeTicks(target);
				}
				if (entity.getTicksLived() % 5 == 0) {
					entity.getWorld().spawnParticle(
						Particle.DUST,
						entity.getLocation().add(0, entity.getHeight() * 0.5, 0),
						6, 0.3, 0.5, 0.3, FROST_PARTICLES
					);
				}
			}
		}
	}

	private static final class FrostbiteListener implements Listener {
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
			Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.FROSTBITE);
			if (en == null) {
				return;
			}
			int level = ChevvyItemUtil.getEnchantLevel(weapon, en);
			if (level <= 0) {
				return;
			}
			int maxFreeze = target.getMaxFreezeTicks();
			int freezeTicks = maxFreeze + (level * 40);
			target.setFreezeTicks(freezeTicks);
			target.setMetadata("chevvy_frostbite",
				new org.bukkit.metadata.FixedMetadataValue(
					Bukkit.getPluginManager().getPlugin(ChevvyEnchants.MOD_ID), freezeTicks));
			int slowTicks = 60 + 40 * level;
			target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowTicks, level - 1, false, false, true));
			Bukkit.getScheduler().runTaskLater(
				Bukkit.getPluginManager().getPlugin(ChevvyEnchants.MOD_ID),
				() -> target.removeMetadata("chevvy_frostbite",
					Bukkit.getPluginManager().getPlugin(ChevvyEnchants.MOD_ID)),
				slowTicks
			);
		}
	}

	public static void applyEnchantToStack(ItemStack stack, World world) {
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.FROSTBITE);
		ChevvyItemUtil.applyEnchant(stack, en, NBT_KEY, LORE_KEY);
	}

	public static void runAdd(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		if (Enchantment.getByKey(ChevvyEnchantKeys.FROSTBITE) == null) {
			sender.sendMessage(Component.text("chevvyenchants:frostbite is not in the registry. Restart the server; check /datapack list for the plugin pack.", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (!Tag.ITEMS_SWORDS.isTagged(stack.getType())) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.frostbite.need_sword"));
			return;
		}
		applyEnchantToStack(stack, player.getWorld());
		sender.sendMessage(Component.translatable("chevvyenchants.command.frostbite.added"));
	}

	public static void runClear(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (stack.getType().isAir()) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.frostbite.need_sword"));
			return;
		}
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.FROSTBITE);
		ChevvyItemUtil.removeEnchant(stack, en, NBT_KEY, LORE_KEY);
		sender.sendMessage(Component.translatable("chevvyenchants.command.frostbite.cleared"));
	}
}
