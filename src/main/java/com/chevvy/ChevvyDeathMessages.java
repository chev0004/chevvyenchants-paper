package com.chevvy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;

public final class ChevvyDeathMessages {
	private static final long ATTRIBUTION_TTL_TICKS = 200L;
	private static final int VARIANT_COUNT = 3;

	private record Attribution(UUID attackerId, String enchantKey, Component weaponName, long tick) {}

	private static final Map<UUID, Attribution> ATTRIBUTIONS = new HashMap<>();

	private ChevvyDeathMessages() {}

	public static void register(JavaPlugin pl) {
		Bukkit.getPluginManager().registerEvents(new DeathListener(), pl);
	}

	public static void track(LivingEntity victim, Player attacker, String enchantKey, ItemStack weapon) {
		if (victim == null || attacker == null) {
			return;
		}
		Component weaponName = resolveWeaponName(weapon);
		ATTRIBUTIONS.put(victim.getUniqueId(), new Attribution(
			attacker.getUniqueId(), enchantKey, weaponName, Bukkit.getCurrentTick()
		));
	}

	private static Component resolveWeaponName(ItemStack weapon) {
		if (weapon == null || weapon.getType().isAir()) {
			return Component.translatable("chevvyenchants.death.fallback_weapon");
		}
		if (weapon.hasItemMeta()) {
			ItemMeta meta = weapon.getItemMeta();
			if (meta != null && meta.hasDisplayName()) {
				Component custom = meta.displayName();
				if (custom != null) {
					return custom;
				}
			}
		}
		return Component.translatable(weapon.getType().translationKey());
	}

	private static final class DeathListener implements Listener {
		@EventHandler(priority = EventPriority.HIGH)
		public void onDeath(PlayerDeathEvent event) {
			Player victim = event.getEntity();
			Attribution attr = ATTRIBUTIONS.remove(victim.getUniqueId());
			if (attr == null) {
				return;
			}
			if (Bukkit.getCurrentTick() - attr.tick() > ATTRIBUTION_TTL_TICKS) {
				return;
			}
			Component attackerName;
			Entity attacker = Bukkit.getEntity(attr.attackerId());
			if (attacker instanceof Player p) {
				attackerName = p.displayName();
			} else if (attacker != null) {
				attackerName = attacker.name();
			} else {
				attackerName = Component.translatable("chevvyenchants.death.fallback_attacker");
			}
			int variant = ThreadLocalRandom.current().nextInt(VARIANT_COUNT);
			String key = "chevvyenchants.death." + attr.enchantKey() + "." + variant;
			event.deathMessage(Component.translatable(key, victim.displayName(), attackerName, attr.weaponName()));
		}
	}
}
