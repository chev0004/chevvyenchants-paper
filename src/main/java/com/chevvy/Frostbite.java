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
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
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
		Bukkit.getPluginManager().registerEvents(new FrostbiteListener(pl), pl);
		Bukkit.getScheduler().runTaskTimer(pl, Frostbite::tickFreeze, 0L, 1L);
	}

	public static void clearFrostbiteState(LivingEntity entity) {
		var plug = Bukkit.getPluginManager().getPlugin(ChevvyEnchants.MOD_ID);
		if (plug != null && entity.hasMetadata("chevvy_frostbite")) {
			entity.removeMetadata("chevvy_frostbite", plug);
		}
		entity.setFreezeTicks(0);
	}

	private static void tickFreeze() {
		int now = Bukkit.getCurrentTick();
		for (World world : Bukkit.getWorlds()) {
			for (LivingEntity entity : world.getLivingEntities()) {
				if (!entity.hasMetadata("chevvy_frostbite")) {
					continue;
				}
				var meta = entity.getMetadata("chevvy_frostbite").get(0);
				long packed = meta.asLong();
				int freezeTarget = (int) (packed >> 32);
				int expiresAt = (int) packed;
				if (now >= expiresAt) {
					entity.removeMetadata("chevvy_frostbite",
						Bukkit.getPluginManager().getPlugin(ChevvyEnchants.MOD_ID));
					continue;
				}
				if (entity.getFreezeTicks() < freezeTarget) {
					entity.setFreezeTicks(freezeTarget);
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
		private final JavaPlugin plugin;

		private FrostbiteListener(JavaPlugin plugin) {
			this.plugin = plugin;
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onPlayerDeath(PlayerDeathEvent event) {
			clearFrostbiteState(event.getEntity());
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onPlayerRespawn(PlayerRespawnEvent event) {
			clearFrostbiteState(event.getPlayer());
		}

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
			int freezeTicks = maxFreeze + (level * 20);
			int slowTicks = 30 + 20 * level;
			int expiresAt = Bukkit.getCurrentTick() + slowTicks;
			long packed = ((long) freezeTicks << 32) | (expiresAt & 0xFFFFFFFFL);
			target.setFreezeTicks(freezeTicks);
			target.setMetadata("chevvy_frostbite",
				new org.bukkit.metadata.FixedMetadataValue(plugin, packed));
			target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowTicks, level - 1, false, false, true));
			ChevvyDeathMessages.track(target, player, "frostbite", weapon);
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
