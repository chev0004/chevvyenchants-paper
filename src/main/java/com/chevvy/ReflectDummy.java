package com.chevvy;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class ReflectDummy {
	static final String METADATA_KEY = "chevvyenchants_reflect_dummy";
	private static final NamespacedKey REFLECT_DUMMY_PDC =
		new NamespacedKey(ChevvyEnchants.MOD_ID, "reflect_dummy");

	private static JavaPlugin plugin;

	private static boolean isReflectDummy(LivingEntity entity) {
		return entity.getPersistentDataContainer().has(REFLECT_DUMMY_PDC, PersistentDataType.BYTE)
			|| entity.hasMetadata(METADATA_KEY);
	}

	private ReflectDummy() {}

	public static void register(JavaPlugin pl) {
		plugin = pl;
		Bukkit.getPluginManager().registerEvents(new ReflectListener(), pl);
		pl.registerCommand("chevvyreflect", "Spawn or remove reflect test dummies", new ReflectCommand(pl));
	}

	private static LivingEntity resolveDamagerLiving(Entity damager) {
		if (damager instanceof LivingEntity le) {
			return le;
		}
		if (damager instanceof Projectile proj && proj.getShooter() instanceof LivingEntity le) {
			return le;
		}
		return null;
	}

	private static final class ReflectListener implements Listener {
		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onDamage(EntityDamageByEntityEvent event) {
			if (!(event.getEntity() instanceof LivingEntity victim)) {
				return;
			}
			if (!isReflectDummy(victim)) {
				return;
			}
			LivingEntity attacker = resolveDamagerLiving(event.getDamager());
			if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) {
				return;
			}
			double dealt = event.getFinalDamage();
			if (dealt <= 0) {
				return;
			}
			UUID victimId = victim.getUniqueId();
			UUID attackerId = attacker.getUniqueId();
			Bukkit.getScheduler().runTask(plugin, () -> {
				Entity ve = Bukkit.getEntity(victimId);
				Entity ae = Bukkit.getEntity(attackerId);
				if (!(ve instanceof LivingEntity d) || !(ae instanceof LivingEntity a) || a.isDead()) {
					return;
				}
				healDummy(d);
				int savedTicks = a.getNoDamageTicks();
				a.setNoDamageTicks(0);
				a.damage(dealt);
				a.setNoDamageTicks(savedTicks);
				Bukkit.getScheduler().runTaskLater(plugin, () -> {
					Entity ve2 = Bukkit.getEntity(victimId);
					Entity ae2 = Bukkit.getEntity(attackerId);
					if (ve2 instanceof LivingEntity d2 && ae2 instanceof LivingEntity a2 && !a2.isDead()) {
						mirrorEffectsFromDummy(d2, a2);
					}
				}, 1L);
			});
		}
	}

	private static void healDummy(LivingEntity dummy) {
		if (dummy.getAttribute(Attribute.MAX_HEALTH) != null) {
			dummy.setHealth(dummy.getAttribute(Attribute.MAX_HEALTH).getValue());
		}
	}

	private static void mirrorEffectsFromDummy(LivingEntity dummy, LivingEntity attacker) {
		Bleed.clearForEntity(dummy);
		int freeze = dummy.getFreezeTicks();
		if (dummy.hasMetadata("chevvy_frostbite")) {
			long packed = dummy.getMetadata("chevvy_frostbite").get(0).asLong();
			attacker.setMetadata("chevvy_frostbite", new FixedMetadataValue(plugin, packed));
			dummy.removeMetadata("chevvy_frostbite", plugin);
		}
		for (PotionEffect pe : dummy.getActivePotionEffects()) {
			if (pe.getType().equals(PotionEffectType.REGENERATION) && pe.getDuration() == PotionEffect.INFINITE_DURATION) {
				continue;
			}
			attacker.addPotionEffect(pe);
			dummy.removePotionEffect(pe.getType());
		}
		dummy.setFreezeTicks(0);
		attacker.setFreezeTicks(Math.max(attacker.getFreezeTicks(), freeze));
	}

	private static final class ReflectCommand implements BasicCommand {
		private final JavaPlugin pl;

		private ReflectCommand(JavaPlugin pl) {
			this.pl = pl;
		}

		@Override
		public void execute(CommandSourceStack stack, String[] args) {
			CommandSender sender = stack.getSender();
			if (!sender.hasPermission("chevvyenchants.reflect")) {
				sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
				return;
			}
			if (args.length < 1) {
				sender.sendMessage(Component.text("Usage: /chevvyreflect <spawn|clear>", NamedTextColor.YELLOW));
				return;
			}
			String sub = args[0].toLowerCase(Locale.ROOT);
			if (sub.equals("spawn")) {
				if (!(sender instanceof Player player)) {
					sender.sendMessage(Component.text("Only players can spawn a dummy.", NamedTextColor.RED));
					return;
				}
				Location loc = player.getLocation();
				loc.getWorld().spawn(loc, Cow.class, cow -> {
					cow.setAI(false);
					cow.setRemoveWhenFarAway(false);
					cow.setPersistent(true);
					cow.customName(Component.text("Reflect dummy"));
					cow.setCustomNameVisible(true);
					if (cow.getAttribute(Attribute.MAX_HEALTH) != null) {
						cow.getAttribute(Attribute.MAX_HEALTH).setBaseValue(2048);
					}
					cow.setHealth(cow.getMaxHealth());
					cow.addPotionEffect(new PotionEffect(
						PotionEffectType.REGENERATION, PotionEffect.INFINITE_DURATION, 4, false, false, false
					));
					cow.getPersistentDataContainer().set(REFLECT_DUMMY_PDC, PersistentDataType.BYTE, (byte) 1);
					cow.setMetadata(METADATA_KEY, new FixedMetadataValue(pl, true));
				});
				player.sendMessage(Component.text("Spawned reflect dummy.", NamedTextColor.GREEN));
				return;
			}
			if (sub.equals("clear")) {
				int n = 0;
				for (World world : Bukkit.getWorlds()) {
					for (Entity e : world.getEntities()) {
						if (e instanceof LivingEntity le && isReflectDummy(le)) {
							e.remove();
							n++;
						}
					}
				}
				sender.sendMessage(Component.text("Removed " + n + " reflect dummy(s).", NamedTextColor.GREEN));
				return;
			}
			sender.sendMessage(Component.text("Usage: /chevvyreflect <spawn|clear>", NamedTextColor.RED));
		}

		@Override
		public Collection<String> suggest(CommandSourceStack stack, String[] args) {
			if (!canUse(stack.getSender())) {
				return List.of();
			}
			if (args.length <= 1) {
				String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
				return Stream.of("spawn", "clear")
					.filter(s -> prefix.isEmpty() || s.startsWith(prefix))
					.toList();
			}
			return List.of();
		}

		@Override
		public boolean canUse(CommandSender sender) {
			return sender.hasPermission("chevvyenchants.reflect");
		}
	}
}
