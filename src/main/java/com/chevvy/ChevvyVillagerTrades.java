package com.chevvy;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.plugin.java.JavaPlugin;

final class ChevvyVillagerTrades {
	private static final int MAX_USES = 8;
	private static final int MERCHANT_XP = 15;
	private static final float PRICE_MULTIPLIER = 0.05f;

	private ChevvyVillagerTrades() {}

	static void register(JavaPlugin plugin) {
		Bukkit.getPluginManager().registerEvents(new Listener() {
			@EventHandler
			public void onEntityAdd(EntityAddToWorldEvent event) {
				Entity entity = event.getEntity();
				if (entity instanceof Villager villager) {
					maybeInject(villager);
				}
			}
		}, plugin);
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			for (World world : Bukkit.getWorlds()) {
				for (Entity entity : world.getEntities()) {
					if (entity instanceof Villager villager) {
						maybeInject(villager);
					}
				}
			}
		}, 20L);
	}

	private static void maybeInject(Villager villager) {
		if (villager.getProfession() == Villager.Profession.TOOLSMITH && villager.getVillagerLevel() >= 4) {
			Enchantment ex = Enchantment.getByKey(ChevvyEnchantKeys.EXCAVATION);
			Enchantment gr = Enchantment.getByKey(ChevvyEnchantKeys.GRAVEDIGGER);
			if (ex != null && !hasEnchantedBookTrade(villager, ex)) {
				addBookTrade(villager, ex, 20, ThreadLocalRandom.current().nextInt(-2, 3));
			}
			if (gr != null && !hasEnchantedBookTrade(villager, gr)) {
				addBookTrade(villager, gr, 18, ThreadLocalRandom.current().nextInt(-2, 3));
			}
			Enchantment df = Enchantment.getByKey(ChevvyEnchantKeys.DEFORESTATION);
			if (df != null && !hasEnchantedBookTrade(villager, df)) {
				addBookTrade(villager, df, 22, ThreadLocalRandom.current().nextInt(-2, 3));
			}
		}
		if (villager.getProfession() == Villager.Profession.ARMORER && villager.getVillagerLevel() >= 4) {
			Enchantment em = Enchantment.getByKey(ChevvyEnchantKeys.EMBER_HEART);
			if (em != null && !hasEnchantedBookTrade(villager, em)) {
				addBookTrade(villager, em, 28, ThreadLocalRandom.current().nextInt(-2, 3));
			}
		}
	}

	private static boolean hasEnchantedBookTrade(Villager villager, Enchantment enchantment) {
		for (MerchantRecipe recipe : villager.getRecipes()) {
			ItemStack result = recipe.getResult();
			if (result.getType() != Material.ENCHANTED_BOOK) {
				continue;
			}
			if (ChevvyItemUtil.getEnchantLevel(result, enchantment) > 0) {
				return true;
			}
		}
		return false;
	}

	private static void addBookTrade(Villager villager, Enchantment enchantment, int basePrice, int priceJitter) {
		ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
		org.bukkit.NamespacedKey k = enchantment.getKey();
		if (k.equals(ChevvyEnchantKeys.EXCAVATION)) {
			Excavation.applyEnchantToStack(book, villager.getWorld());
		} else if (k.equals(ChevvyEnchantKeys.GRAVEDIGGER)) {
			Gravedigger.applyEnchantToStack(book, villager.getWorld());
		} else if (k.equals(ChevvyEnchantKeys.EMBER_HEART)) {
			EmberHeart.applyEnchantToStack(book, villager.getWorld());
		} else if (k.equals(ChevvyEnchantKeys.DEFORESTATION)) {
			Deforestation.applyEnchantToStack(book, villager.getWorld());
		}
		int price = Math.max(1, basePrice + priceJitter);
		MerchantRecipe offer = new MerchantRecipe(book, MAX_USES);
		offer.setIngredients(List.of(new ItemStack(Material.EMERALD, price)));
		offer.setVillagerExperience(MERCHANT_XP);
		offer.setPriceMultiplier(PRICE_MULTIPLIER);
		List<MerchantRecipe> recipes = new ArrayList<>(villager.getRecipes());
		recipes.add(offer);
		villager.setRecipes(recipes);
	}
}
