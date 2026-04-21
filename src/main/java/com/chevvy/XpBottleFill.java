package com.chevvy;

import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;

final class XpBottleFill {
	private XpBottleFill() {}

	static void register(JavaPlugin pl) {
		Bukkit.getPluginManager().registerEvents(new XpBottleFillListener(), pl);
	}

	private static int rollVanillaBottleXp() {
		return 3 + ThreadLocalRandom.current().nextInt(6) + ThreadLocalRandom.current().nextInt(6);
	}

	private static final class XpBottleFillListener implements Listener {
		@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
		public void onInteract(PlayerInteractEvent event) {
			Action action = event.getAction();
			if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
				return;
			}
			Player player = event.getPlayer();
			if (!player.isSneaking()) {
				return;
			}
			GameMode mode = player.getGameMode();
			if (mode != GameMode.SURVIVAL && mode != GameMode.ADVENTURE && mode != GameMode.CREATIVE) {
				return;
			}
			EquipmentSlot hand = event.getHand();
			PlayerInventory inv = player.getInventory();
			ItemStack bottle;
			if (hand == EquipmentSlot.OFF_HAND) {
				bottle = inv.getItemInOffHand();
			} else {
				bottle = inv.getItemInMainHand();
			}
			if (bottle.getType() != Material.GLASS_BOTTLE) {
				return;
			}
			event.setCancelled(true);
			int cost = rollVanillaBottleXp();
			if (mode != GameMode.CREATIVE) {
				if (player.getTotalExperience() < cost) {
					player.sendMessage(Component.translatable("chevvyenchants.xp_bottle_fill.not_enough_xp"));
					return;
				}
				player.giveExp(-cost);
			}
			if (bottle.getAmount() <= 1) {
				bottle.setType(Material.EXPERIENCE_BOTTLE);
			} else {
				bottle.setAmount(bottle.getAmount() - 1);
				HashMap<Integer, ItemStack> leftover = inv.addItem(new ItemStack(Material.EXPERIENCE_BOTTLE));
				for (ItemStack drop : leftover.values()) {
					player.getWorld().dropItem(player.getLocation(), drop);
				}
			}
			player.playSound(
				player.getLocation(),
				Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
				SoundCategory.PLAYERS,
				0.35f,
				0.85f + ThreadLocalRandom.current().nextFloat() * 0.2f
			);
		}
	}
}
