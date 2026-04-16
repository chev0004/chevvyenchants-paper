package com.chevvy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class Fart {
	public static final String NBT_KEY = "chevvyenchants_fart";
	public static final String LORE_KEY = "enchantment.chevvyenchants.fart";

	private static final long COOLDOWN_MILLIS = 2500L;
	private static final double RADIUS = 4.5;
	private static final double CONE_DOT = 0.25;
	private static final double DAMAGE = 5.0;
	private static final double KNOCKBACK_STRENGTH = 1.1;
	private static final double KNOCKBACK_LIFT = 0.35;
	private static final int POISON_DURATION_TICKS = 80;
	private static final int POISON_AMPLIFIER = 0;

	private static final Particle.DustOptions FART_DUST =
		new Particle.DustOptions(Color.fromRGB(154, 132, 46), 1.6f);
	private static final Particle.DustOptions FART_DUST_DEEP =
		new Particle.DustOptions(Color.fromRGB(92, 108, 38), 1.8f);

	private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();

	private Fart() {}

	public static void register(JavaPlugin pl) {
		Bukkit.getPluginManager().registerEvents(new FartListener(), pl);
	}

	private static final class FartListener implements Listener {
		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onToggleSneak(PlayerToggleSneakEvent event) {
			if (!event.isSneaking()) {
				return;
			}
			Player player = event.getPlayer();
			ItemStack legs = player.getInventory().getLeggings();
			if (legs == null || legs.getType().isAir()) {
				return;
			}
			Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.FART);
			if (en == null) {
				return;
			}
			if (ChevvyItemUtil.getEnchantLevel(legs, en) <= 0
				&& !ChevvyItemUtil.hasEnchantOrLegacyNbt(legs, en, NBT_KEY)) {
				return;
			}
			long now = System.currentTimeMillis();
			Long last = COOLDOWNS.get(player.getUniqueId());
			if (last != null && now - last < COOLDOWN_MILLIS) {
				return;
			}
			COOLDOWNS.put(player.getUniqueId(), now);
			unleashFart(player);
		}
	}

	private static void unleashFart(Player player) {
		World world = player.getWorld();
		Location origin = player.getLocation();
		Vector facing = origin.getDirection().setY(0).normalize();
		if (facing.lengthSquared() < 1.0E-4) {
			facing = new Vector(0, 0, 1);
		}
		Vector backward = facing.clone().multiply(-1);
		Location burstOrigin = origin.clone().add(0, 1.0, 0).add(backward.clone().multiply(0.6));

		world.spawnParticle(Particle.GUST_EMITTER_LARGE, burstOrigin, 1, 0, 0, 0, 0);
		world.spawnParticle(Particle.GUST, burstOrigin, 24, 0.9, 0.6, 0.9, 0.1);
		world.spawnParticle(Particle.DUST, burstOrigin, 40, 1.2, 0.8, 1.2, 0, FART_DUST);
		world.spawnParticle(Particle.DUST, burstOrigin, 30, 1.6, 0.9, 1.6, 0, FART_DUST_DEEP);
		world.spawnParticle(Particle.CLOUD, burstOrigin, 18, 0.8, 0.4, 0.8, 0.05);
		world.spawnParticle(Particle.SNEEZE, burstOrigin, 12, 0.6, 0.4, 0.6, 0.02);

		world.playSound(origin, Sound.ENTITY_WIND_CHARGE_THROW, SoundCategory.PLAYERS, 1.2f, 0.7f);
		world.playSound(origin, Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 1.4f, 0.6f);

		for (Entity nearby : world.getNearbyEntities(origin, RADIUS, RADIUS, RADIUS)) {
			if (!(nearby instanceof LivingEntity victim)) {
				continue;
			}
			if (victim.equals(player)) {
				continue;
			}
			if (!isInsideEffectiveZone(origin, backward, victim.getLocation())) {
				continue;
			}
			victim.damage(DAMAGE);
			victim.addPotionEffect(new PotionEffect(
				PotionEffectType.POISON, POISON_DURATION_TICKS, POISON_AMPLIFIER, false, true, true
			));
			Vector push = backward.clone().multiply(KNOCKBACK_STRENGTH).setY(KNOCKBACK_LIFT);
			victim.setVelocity(victim.getVelocity().add(push));
		}
		int blockRadius = (int) Math.ceil(RADIUS);
		int baseX = origin.getBlockX();
		int baseY = origin.getBlockY();
		int baseZ = origin.getBlockZ();
		for (int x = baseX - blockRadius; x <= baseX + blockRadius; x++) {
			for (int y = baseY - 1; y <= baseY + 2; y++) {
				for (int z = baseZ - blockRadius; z <= baseZ + blockRadius; z++) {
					Block block = world.getBlockAt(x, y, z);
					if (!isPlantLike(block.getType())) {
						continue;
					}
					if (!isInsideEffectiveZone(origin, backward, block.getLocation().add(0.5, 0.5, 0.5))) {
						continue;
					}
					block.setType(Material.AIR, false);
				}
			}
		}
	}

	private static boolean isInsideEffectiveZone(Location origin, Vector backward, Location target) {
		Vector toTarget = target.toVector().subtract(origin.toVector());
		if (toTarget.lengthSquared() > RADIUS * RADIUS) {
			return false;
		}
		toTarget.setY(0);
		if (toTarget.lengthSquared() < 1.0E-4) {
			return false;
		}
		Vector toTargetDir = toTarget.normalize();
		return toTargetDir.dot(backward) >= CONE_DOT;
	}

	private static boolean isPlantLike(Material type) {
		return Tag.SAPLINGS.isTagged(type)
			|| Tag.SMALL_FLOWERS.isTagged(type)
			|| Tag.FLOWERS.isTagged(type)
			|| Tag.CROPS.isTagged(type)
			|| Tag.LEAVES.isTagged(type)
			|| type == Material.SHORT_GRASS
			|| type == Material.TALL_GRASS
			|| type == Material.FERN
			|| type == Material.LARGE_FERN
			|| type == Material.SWEET_BERRY_BUSH
			|| type == Material.NETHER_WART
			|| type == Material.COCOA
			|| type == Material.SUGAR_CANE
			|| type == Material.BAMBOO
			|| type == Material.CACTUS
			|| type == Material.VINE
			|| type == Material.CAVE_VINES
			|| type == Material.CAVE_VINES_PLANT
			|| type == Material.TWISTING_VINES
			|| type == Material.TWISTING_VINES_PLANT
			|| type == Material.WEEPING_VINES
			|| type == Material.WEEPING_VINES_PLANT
			|| type == Material.KELP
			|| type == Material.KELP_PLANT
			|| type == Material.SEAGRASS
			|| type == Material.TALL_SEAGRASS
			|| type == Material.LILY_PAD
			|| type == Material.TORCHFLOWER_CROP
			|| type == Material.PITCHER_CROP
			|| type == Material.PITCHER_PLANT
			|| type == Material.MANGROVE_PROPAGULE;
	}

	public static void applyEnchantToStack(ItemStack stack, World world) {
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.FART);
		ChevvyItemUtil.applyEnchant(stack, en, NBT_KEY, LORE_KEY);
	}

	public static void runAdd(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		if (Enchantment.getByKey(ChevvyEnchantKeys.FART) == null) {
			sender.sendMessage(Component.text("chevvyenchants:fart is not in the registry. Restart the server; check /datapack list for the plugin pack.", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (!Tag.ITEMS_ENCHANTABLE_LEG_ARMOR.isTagged(stack.getType())) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.fart.need_leggings"));
			return;
		}
		applyEnchantToStack(stack, player.getWorld());
		sender.sendMessage(Component.translatable("chevvyenchants.command.fart.added"));
	}

	public static void runClear(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (stack.getType().isAir()) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.fart.need_leggings"));
			return;
		}
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.FART);
		ChevvyItemUtil.removeEnchant(stack, en, NBT_KEY, LORE_KEY);
		sender.sendMessage(Component.translatable("chevvyenchants.command.fart.cleared"));
	}
}
