package com.chevvy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class Soulbound {
	public static final String NBT_KEY = "chevvyenchants_soulbound";
	public static final String LORE_KEY = "enchantment.chevvyenchants.soulbound";

	private Soulbound() {}

	public static void register(JavaPlugin pl) {
		Bukkit.getPluginManager().registerEvents(new SoulboundListener(), pl);
	}

	private static boolean isSoulbound(ItemStack stack) {
		if (stack == null || stack.getType().isAir()) {
			return false;
		}
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.SOULBOUND);
		return ChevvyItemUtil.hasEnchantOrLegacyNbt(stack, en, NBT_KEY);
	}

	private static final class SoulboundListener implements Listener {
		@EventHandler(priority = EventPriority.HIGH)
		public void onDeath(PlayerDeathEvent event) {
			if (event.getKeepInventory()) {
				return;
			}
			List<ItemStack> drops = event.getDrops();
			List<ItemStack> kept = new ArrayList<>();
			Iterator<ItemStack> it = drops.iterator();
			while (it.hasNext()) {
				ItemStack drop = it.next();
				if (isSoulbound(drop)) {
					kept.add(drop.clone());
					it.remove();
				}
			}
			if (!kept.isEmpty()) {
				event.getEntity().setMetadata("chevvy_soulbound",
					new org.bukkit.metadata.FixedMetadataValue(
						Bukkit.getPluginManager().getPlugin(ChevvyEnchants.MOD_ID), kept));
			}
		}

		@SuppressWarnings("unchecked")
		@EventHandler(priority = EventPriority.HIGH)
		public void onRespawn(PlayerRespawnEvent event) {
			Player player = event.getPlayer();
			if (!player.hasMetadata("chevvy_soulbound")) {
				return;
			}
			List<ItemStack> kept = (List<ItemStack>) player.getMetadata("chevvy_soulbound").get(0).value();
			player.removeMetadata("chevvy_soulbound",
				Bukkit.getPluginManager().getPlugin(ChevvyEnchants.MOD_ID));
			if (kept == null) {
				return;
			}
			for (ItemStack item : kept) {
				player.getInventory().addItem(item);
			}
		}
	}

	public static void applyEnchantToStack(ItemStack stack, World world) {
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.SOULBOUND);
		ChevvyItemUtil.applyEnchant(stack, en, NBT_KEY, LORE_KEY);
	}

	public static void runAdd(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		if (Enchantment.getByKey(ChevvyEnchantKeys.SOULBOUND) == null) {
			sender.sendMessage(Component.text("chevvyenchants:soulbound is not in the registry. Restart the server; check /datapack list for the plugin pack.", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (stack.getType().isAir()) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.soulbound.need_item"));
			return;
		}
		applyEnchantToStack(stack, player.getWorld());
		sender.sendMessage(Component.translatable("chevvyenchants.command.soulbound.added"));
	}

	public static void runClear(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (stack.getType().isAir()) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.soulbound.need_item"));
			return;
		}
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.SOULBOUND);
		ChevvyItemUtil.removeEnchant(stack, en, NBT_KEY, LORE_KEY);
		sender.sendMessage(Component.translatable("chevvyenchants.command.soulbound.cleared"));
	}
}
