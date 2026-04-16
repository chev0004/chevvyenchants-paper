package com.chevvy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class SonicShot {
	public static final String NBT_KEY = "chevvyenchants_sonic_shot";
	public static final String LORE_KEY = "enchantment.chevvyenchants.sonic_shot";

	private static final long COOLDOWN_MILLIS = 1500L;
	private static final double BEAM_RANGE = 15.0;
	private static final double BEAM_RADIUS = 1.25;
	private static final double DAMAGE = 10.0;
	private static final double KNOCKBACK_STRENGTH = 0.8;
	private static final double KNOCKBACK_LIFT = 0.4;
	private static final int BEAM_STEPS = 16;

	private static final Particle.DustOptions WHITE_DUST =
		new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.6f);

	private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();

	private SonicShot() {}

	public static void register(JavaPlugin pl) {
		Bukkit.getPluginManager().registerEvents(new SonicShotListener(), pl);
	}

	private static final class SonicShotListener implements Listener {
		@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
		public void onShoot(EntityShootBowEvent event) {
			if (!(event.getEntity() instanceof Player player)) {
				return;
			}
			ItemStack bow = event.getBow();
			if (bow == null || bow.getType().isAir()) {
				return;
			}
			Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.SONIC_SHOT);
			if (en == null) {
				return;
			}
			if (ChevvyItemUtil.getEnchantLevel(bow, en) <= 0
				&& !ChevvyItemUtil.hasEnchantOrLegacyNbt(bow, en, NBT_KEY)) {
				return;
			}
			long now = System.currentTimeMillis();
			Long last = COOLDOWNS.get(player.getUniqueId());
			if (last != null && now - last < COOLDOWN_MILLIS) {
				return;
			}
			COOLDOWNS.put(player.getUniqueId(), now);
			event.setCancelled(true);
			fireBeam(player, bow);
		}
	}

	private static void fireBeam(Player player, ItemStack bow) {
		World world = player.getWorld();
		Location origin = player.getEyeLocation();
		Vector direction = origin.getDirection().normalize();

		world.playSound(origin, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 1.4f, 1.0f);
		world.playSound(origin, Sound.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.PLAYERS, 1.0f, 1.2f);

		for (int i = 1; i <= BEAM_STEPS; i++) {
			double t = (double) i * (BEAM_RANGE / BEAM_STEPS);
			Location point = origin.clone().add(direction.clone().multiply(t));
			world.spawnParticle(Particle.SONIC_BOOM, point, 1, 0, 0, 0, 0);
			world.spawnParticle(Particle.DUST, point, 6, 0.25, 0.25, 0.25, 0, WHITE_DUST);
			world.spawnParticle(Particle.END_ROD, point, 2, 0.1, 0.1, 0.1, 0.01);
			world.spawnParticle(Particle.FIREWORK, point, 3, 0.15, 0.15, 0.15, 0.01);
		}

		for (Entity nearby : world.getNearbyEntities(
			origin.clone().add(direction.clone().multiply(BEAM_RANGE / 2)),
			BEAM_RANGE, BEAM_RANGE, BEAM_RANGE
		)) {
			if (!(nearby instanceof LivingEntity victim)) {
				continue;
			}
			if (victim.equals(player)) {
				continue;
			}
			Vector toVictim = victim.getLocation().toVector()
				.add(new Vector(0, victim.getHeight() * 0.5, 0))
				.subtract(origin.toVector());
			double projection = toVictim.dot(direction);
			if (projection <= 0 || projection > BEAM_RANGE) {
				continue;
			}
			Vector closest = direction.clone().multiply(projection);
			double perpendicular = toVictim.clone().subtract(closest).length();
			if (perpendicular > BEAM_RADIUS) {
				continue;
			}
			DamageSource source = DamageSource.builder(DamageType.SONIC_BOOM)
				.withCausingEntity(player)
				.withDirectEntity(player)
				.build();
			ChevvyDeathMessages.track(victim, player, "sonic_shot", bow);
			victim.damage(DAMAGE, source);
			Vector push = direction.clone().multiply(KNOCKBACK_STRENGTH).setY(KNOCKBACK_LIFT);
			victim.setVelocity(victim.getVelocity().add(push));
		}
	}

	public static void applyEnchantToStack(ItemStack stack, World world) {
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.SONIC_SHOT);
		ChevvyItemUtil.applyEnchant(stack, en, NBT_KEY, LORE_KEY);
	}

	public static void runAdd(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		if (Enchantment.getByKey(ChevvyEnchantKeys.SONIC_SHOT) == null) {
			sender.sendMessage(Component.text("chevvyenchants:sonic_shot is not in the registry. Restart the server; check /datapack list for the plugin pack.", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (!Tag.ITEMS_ENCHANTABLE_BOW.isTagged(stack.getType())) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.sonic_shot.need_bow"));
			return;
		}
		applyEnchantToStack(stack, player.getWorld());
		sender.sendMessage(Component.translatable("chevvyenchants.command.sonic_shot.added"));
	}

	public static void runClear(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (stack.getType().isAir()) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.sonic_shot.need_bow"));
			return;
		}
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.SONIC_SHOT);
		ChevvyItemUtil.removeEnchant(stack, en, NBT_KEY, LORE_KEY);
		sender.sendMessage(Component.translatable("chevvyenchants.command.sonic_shot.cleared"));
	}
}
