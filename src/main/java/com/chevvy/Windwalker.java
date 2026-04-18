package com.chevvy;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class Windwalker {
	public static final String NBT_KEY = "chevvyenchants_windwalker";
	public static final String LORE_KEY = "enchantment.chevvyenchants.windwalker";

	private static final NamespacedKey SPEED_MODIFIER_KEY =
		new NamespacedKey("chevvyenchants", "windwalker_speed");
	private static final double SPEED_PER_LEVEL = 0.20;

	private Windwalker() {}

	public static void register(JavaPlugin plugin) {
		Bukkit.getScheduler().runTaskTimer(plugin, Windwalker::tickAllWorlds, 0L, 1L);
	}

	private static void tickAllWorlds() {
		for (var world : Bukkit.getWorlds()) {
			for (Player player : world.getPlayers()) {
				ItemStack boots = player.getInventory().getBoots();
				int level = 0;
				if (isOnStack(boots, world)) {
					Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.WINDWALKER);
					if (en != null) {
						level = ChevvyItemUtil.getEnchantLevel(boots, en);
						if (level <= 0) {
							level = 1;
						}
					}
				}
				applySpeedModifier(player, level);
			}
		}
	}

	private static void applySpeedModifier(Player player, int level) {
		AttributeInstance attr = player.getAttribute(Attribute.MOVEMENT_SPEED);
		if (attr == null) {
			return;
		}
		AttributeModifier existing = null;
		for (AttributeModifier mod : attr.getModifiers()) {
			if (SPEED_MODIFIER_KEY.equals(mod.getKey())) {
				existing = mod;
				break;
			}
		}
		if (level <= 0) {
			if (existing != null) {
				attr.removeModifier(existing);
			}
			return;
		}
		double desired = SPEED_PER_LEVEL * level;
		if (existing != null) {
			if (Math.abs(existing.getAmount() - desired) < 1.0E-6) {
				return;
			}
			attr.removeModifier(existing);
		}
		attr.addModifier(new AttributeModifier(
			SPEED_MODIFIER_KEY,
			desired,
			AttributeModifier.Operation.ADD_SCALAR,
			EquipmentSlotGroup.ANY
		));
	}

	public static void applyEnchantToStack(ItemStack stack, World world) {
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.WINDWALKER);
		ChevvyItemUtil.applyEnchant(stack, en, NBT_KEY, LORE_KEY);
	}

	public static void runAdd(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		if (Enchantment.getByKey(ChevvyEnchantKeys.WINDWALKER) == null) {
			sender.sendMessage(Component.text("chevvyenchants:windwalker is not in the registry. Restart the server; check /datapack list for the plugin pack.", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (!Tag.ITEMS_ENCHANTABLE_FOOT_ARMOR.isTagged(stack.getType())) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.windwalker.need_boots"));
			return;
		}
		applyEnchantToStack(stack, player.getWorld());
		sender.sendMessage(Component.translatable("chevvyenchants.command.windwalker.added"));
	}

	public static void runClear(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (stack.getType().isAir()) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.windwalker.need_boots"));
			return;
		}
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.WINDWALKER);
		ChevvyItemUtil.removeEnchant(stack, en, NBT_KEY, LORE_KEY);
		sender.sendMessage(Component.translatable("chevvyenchants.command.windwalker.cleared"));
	}

	private static boolean isOnStack(ItemStack stack, World world) {
		if (stack == null || stack.getType().isAir()) {
			return false;
		}
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.WINDWALKER);
		return ChevvyItemUtil.hasEnchantOrLegacyNbt(stack, en, NBT_KEY);
	}
}
