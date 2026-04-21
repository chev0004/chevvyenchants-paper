package com.chevvy;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

final class PlayerHeadDrop {
	private PlayerHeadDrop() {}

	static void register(JavaPlugin pl) {
		Bukkit.getPluginManager().registerEvents(new PlayerHeadDropListener(), pl);
	}

	private static final class PlayerHeadDropListener implements Listener {
		@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
		public void onPlayerDeath(PlayerDeathEvent event) {
			Player victim = event.getEntity();
			ItemStack head = new ItemStack(Material.PLAYER_HEAD);
			SkullMeta meta = (SkullMeta) head.getItemMeta();
			if (meta == null) {
				return;
			}
			meta.setPlayerProfile(victim.getPlayerProfile());
			head.setItemMeta(meta);
			event.getDrops().add(head);
		}
	}
}
