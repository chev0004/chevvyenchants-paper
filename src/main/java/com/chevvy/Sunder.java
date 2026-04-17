package com.chevvy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
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
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class Sunder {
	public static final String NBT_KEY = "chevvyenchants_sunder";
	public static final String LORE_KEY = "enchantment.chevvyenchants.sunder";

	private static final int[] DURABILITY_DAMAGE = {3, 5, 7, 9, 12};

	private Sunder() {}

	public static void register(JavaPlugin pl) {
		Bukkit.getPluginManager().registerEvents(new SunderListener(), pl);
	}

	private static final class SunderListener implements Listener {
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
			Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.SUNDER);
			if (en == null) {
				return;
			}
			int level = ChevvyItemUtil.getEnchantLevel(weapon, en);
			if (level <= 0) {
				return;
			}
			EntityEquipment equipment = target.getEquipment();
			if (equipment == null) {
				return;
			}
			List<ItemStack> armorPieces = new ArrayList<>(4);
			if (isArmor(equipment.getHelmet())) armorPieces.add(equipment.getHelmet());
			if (isArmor(equipment.getChestplate())) armorPieces.add(equipment.getChestplate());
			if (isArmor(equipment.getLeggings())) armorPieces.add(equipment.getLeggings());
			if (isArmor(equipment.getBoots())) armorPieces.add(equipment.getBoots());
			if (armorPieces.isEmpty()) {
				return;
			}
			ItemStack piece = armorPieces.get(ThreadLocalRandom.current().nextInt(armorPieces.size()));
			int idx = Math.min(level, DURABILITY_DAMAGE.length) - 1;
			int dmg = DURABILITY_DAMAGE[idx];
			if (piece.getItemMeta() instanceof Damageable meta) {
				int maxDur = piece.getType().getMaxDurability();
				int newDamage = meta.getDamage() + dmg;
				if (newDamage >= maxDur) {
					piece.setAmount(0);
					target.getWorld().playSound(
						target.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f
					);
				} else {
					meta.setDamage(newDamage);
					piece.setItemMeta(meta);
					target.getWorld().playSound(
						target.getLocation(), Sound.ENTITY_ARMOR_STAND_HIT, 0.8f, 0.6f
					);
				}
			}
		}
	}

	private static boolean isArmor(ItemStack stack) {
		if (stack == null || stack.getType().isAir()) {
			return false;
		}
		return stack.getType().getMaxDurability() > 0
			&& stack.getItemMeta() instanceof Damageable;
	}

	public static void applyEnchantToStack(ItemStack stack, World world) {
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.SUNDER);
		ChevvyItemUtil.applyEnchant(stack, en, NBT_KEY, LORE_KEY);
	}

	public static void runAdd(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		if (Enchantment.getByKey(ChevvyEnchantKeys.SUNDER) == null) {
			sender.sendMessage(Component.text("chevvyenchants:sunder is not in the registry. Restart the server; check /datapack list for the plugin pack.", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (!Tag.ITEMS_SWORDS.isTagged(stack.getType())) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.sunder.need_sword"));
			return;
		}
		applyEnchantToStack(stack, player.getWorld());
		sender.sendMessage(Component.translatable("chevvyenchants.command.sunder.added"));
	}

	public static void runClear(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (stack.getType().isAir()) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.sunder.need_sword"));
			return;
		}
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.SUNDER);
		ChevvyItemUtil.removeEnchant(stack, en, NBT_KEY, LORE_KEY);
		sender.sendMessage(Component.translatable("chevvyenchants.command.sunder.cleared"));
	}
}
