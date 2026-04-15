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

	private record EnchantOffer(org.bukkit.NamespacedKey key, int basePrice) {}

	private static final int MIN_VILLAGER_LEVEL = 4;

	private static final List<EnchantOffer> TOOLSMITH_POOL = List.of(
		new EnchantOffer(ChevvyEnchantKeys.EXCAVATION, 20),
		new EnchantOffer(ChevvyEnchantKeys.GRAVEDIGGER, 18),
		new EnchantOffer(ChevvyEnchantKeys.DEFORESTATION, 22),
		new EnchantOffer(ChevvyEnchantKeys.MINERS_LANTERN, 20)
	);

	private static final List<EnchantOffer> WEAPONSMITH_POOL = List.of(
		new EnchantOffer(ChevvyEnchantKeys.WITHER_TOUCH, 26),
		new EnchantOffer(ChevvyEnchantKeys.POISON_EDGE, 26),
		new EnchantOffer(ChevvyEnchantKeys.LIFESTEAL, 30)
	);

	private static final List<EnchantOffer> ARMORER_POOL = List.of(
		new EnchantOffer(ChevvyEnchantKeys.EMBER_HEART, 28),
		new EnchantOffer(ChevvyEnchantKeys.LAVA_STRIDE, 24),
		new EnchantOffer(ChevvyEnchantKeys.SUSTENANCE, 18)
	);

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
		if (villager.getVillagerLevel() < MIN_VILLAGER_LEVEL) {
			return;
		}
		Villager.Profession prof = villager.getProfession();
		if (prof == Villager.Profession.NONE || prof == Villager.Profession.NITWIT) {
			return;
		}
		List<EnchantOffer> pool = poolForProfession(prof);
		if (pool.isEmpty()) {
			return;
		}
		int idx = (int) (Math.abs(villager.getUniqueId().getLeastSignificantBits()) % pool.size());
		EnchantOffer chosen = pool.get(idx);
		Enchantment chosenEnchant = Enchantment.getByKey(chosen.key());
		if (chosenEnchant != null && !hasEnchantedBookTrade(villager, chosenEnchant)) {
			removePoolBookTrades(villager, pool);
			addBookTrade(villager, chosenEnchant, chosen.basePrice(), ThreadLocalRandom.current().nextInt(-2, 3));
		}
	}

	private static List<EnchantOffer> poolForProfession(Villager.Profession profession) {
		if (profession == Villager.Profession.TOOLSMITH) {
			return TOOLSMITH_POOL;
		}
		if (profession == Villager.Profession.WEAPONSMITH) {
			return WEAPONSMITH_POOL;
		}
		if (profession == Villager.Profession.ARMORER) {
			return ARMORER_POOL;
		}
		return List.of();
	}

	private static void removePoolBookTrades(Villager villager, List<EnchantOffer> pool) {
		List<MerchantRecipe> recipes = new ArrayList<>(villager.getRecipes());
		boolean changed = false;
		for (EnchantOffer offer : pool) {
			Enchantment enchant = Enchantment.getByKey(offer.key());
			if (enchant == null) continue;
			changed |= recipes.removeIf(r ->
				r.getResult().getType() == Material.ENCHANTED_BOOK
				&& ChevvyItemUtil.getEnchantLevel(r.getResult(), enchant) > 0
			);
		}
		if (changed) villager.setRecipes(recipes);
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
		} else if (k.equals(ChevvyEnchantKeys.WITHER_TOUCH)) {
			WitherTouch.applyEnchantToStack(book, villager.getWorld());
		} else if (k.equals(ChevvyEnchantKeys.POISON_EDGE)) {
			PoisonEdge.applyEnchantToStack(book, villager.getWorld());
		} else if (k.equals(ChevvyEnchantKeys.LAVA_STRIDE)) {
			LavaStride.applyEnchantToStack(book, villager.getWorld());
		} else if (k.equals(ChevvyEnchantKeys.SUSTENANCE)) {
			Sustenance.applyEnchantToStack(book, villager.getWorld());
		} else if (k.equals(ChevvyEnchantKeys.MINERS_LANTERN)) {
			MinersLantern.applyEnchantToStack(book, villager.getWorld());
		} else if (k.equals(ChevvyEnchantKeys.LIFESTEAL)) {
			Lifesteal.applyEnchantToStack(book, villager.getWorld());
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
