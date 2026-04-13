package com.chevvy;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemEnchantments;
import io.papermc.paper.datacomponent.item.ItemLore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class ChevvyItemUtil {
	private ChevvyItemUtil() {}

	public static int getEnchantLevel(ItemStack stack, Enchantment en) {
		if (stack == null || en == null) {
			return 0;
		}
		ItemEnchantments onItem = stack.getData(DataComponentTypes.ENCHANTMENTS);
		if (onItem != null) {
			Integer level = onItem.enchantments().get(en);
			if (level != null && level > 0) {
				return level;
			}
		}
		ItemEnchantments stored = stack.getData(DataComponentTypes.STORED_ENCHANTMENTS);
		if (stored != null) {
			Integer level = stored.enchantments().get(en);
			if (level != null && level > 0) {
				return level;
			}
		}
		return 0;
	}

	public static boolean hasEnchantOrLegacyNbt(ItemStack stack, Enchantment en, String legacyNbtKey) {
		if (stack == null || stack.getType().isAir()) {
			return false;
		}
		if (getEnchantLevel(stack, en) > 0) {
			return true;
		}
		ItemMeta meta = stack.getItemMeta();
		if (meta == null) {
			return false;
		}
		PersistentDataContainer pdc = meta.getPersistentDataContainer();
		NamespacedKey key = new NamespacedKey(ChevvyEnchants.MOD_ID, legacyNbtKey);
		Integer v = pdc.get(key, PersistentDataType.INTEGER);
		return v != null && v >= 1;
	}

	public static void applyEnchant(ItemStack stack, Enchantment en, String legacyNbtKey, String loreTranslationKey) {
		if (en == null) {
			return;
		}
		if (stack.getType() == Material.ENCHANTED_BOOK) {
			ItemEnchantments current = stack.getData(DataComponentTypes.STORED_ENCHANTMENTS);
			Map<Enchantment, Integer> map = new HashMap<>(current != null ? current.enchantments() : Map.of());
			map.put(en, 1);
			stack.setData(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantments.itemEnchantments(map));
		} else {
			ItemEnchantments current = stack.getData(DataComponentTypes.ENCHANTMENTS);
			Map<Enchantment, Integer> map = new HashMap<>(current != null ? current.enchantments() : Map.of());
			map.put(en, 1);
			stack.setData(DataComponentTypes.ENCHANTMENTS, ItemEnchantments.itemEnchantments(map));
		}
		clearLegacyNbt(stack, legacyNbtKey);
		stripModLore(stack, loreTranslationKey);
	}

	public static void removeEnchant(ItemStack stack, Enchantment en, String legacyNbtKey, String loreTranslationKey) {
		if (en == null) {
			return;
		}
		removeFromComponent(stack, DataComponentTypes.ENCHANTMENTS, en);
		removeFromComponent(stack, DataComponentTypes.STORED_ENCHANTMENTS, en);
		clearLegacyNbt(stack, legacyNbtKey);
		stripModLore(stack, loreTranslationKey);
	}

	private static void removeFromComponent(
		ItemStack stack,
		DataComponentType.Valued<ItemEnchantments> type,
		Enchantment en
	) {
		ItemEnchantments current = stack.getData(type);
		if (current == null) {
			return;
		}
		Map<Enchantment, Integer> map = new HashMap<>(current.enchantments());
		map.remove(en);
		if (map.isEmpty()) {
			stack.unsetData(type);
		} else {
			stack.setData(type, ItemEnchantments.itemEnchantments(map));
		}
	}

	private static void clearLegacyNbt(ItemStack stack, String legacyNbtKey) {
		ItemMeta meta = stack.getItemMeta();
		if (meta == null) {
			return;
		}
		PersistentDataContainer pdc = meta.getPersistentDataContainer();
		pdc.remove(new NamespacedKey(ChevvyEnchants.MOD_ID, legacyNbtKey));
		stack.setItemMeta(meta);
	}

	public static void stripModLore(ItemStack stack, String translationKey) {
		ItemLore lore = stack.getData(DataComponentTypes.LORE);
		if (lore == null) {
			return;
		}
		List<Component> kept = new ArrayList<>();
		for (Component line : lore.lines()) {
			if (line instanceof TranslatableComponent t && translationKey.equals(t.key())) {
				continue;
			}
			kept.add(line);
		}
		if (kept.isEmpty()) {
			stack.unsetData(DataComponentTypes.LORE);
		} else {
			stack.setData(DataComponentTypes.LORE, ItemLore.lore(kept));
		}
	}
}
