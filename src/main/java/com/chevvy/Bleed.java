package com.chevvy;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class Bleed {
	public static final String NBT_KEY = "chevvyenchants_bleed";
	public static final String LORE_KEY = "enchantment.chevvyenchants.bleed";

	private static final double DAMAGE_PER_STACK = 1.0;
	private static final Particle.DustOptions BLOOD_PARTICLES =
		new Particle.DustOptions(Color.fromRGB(150, 0, 0), 1.0f);

	private static final Map<UUID, BleedState> BLEED_TARGETS = new HashMap<>();

	private record BleedState(int stacks, int maxStacks, int ticksRemaining) {}

	private Bleed() {}

	public static void register(JavaPlugin pl) {
		Bukkit.getPluginManager().registerEvents(new BleedListener(), pl);
		Bukkit.getScheduler().runTaskTimer(pl, Bleed::tickBleed, 20L, 20L);
	}

	private static int maxStacksForLevel(int level) {
		return switch (level) {
			case 1 -> 3;
			case 2 -> 5;
			default -> 7;
		};
	}

	private static int durationForLevel(int level) {
		return switch (level) {
			case 1 -> 3;
			case 2 -> 4;
			default -> 5;
		};
	}

	private static void tickBleed() {
		Iterator<Map.Entry<UUID, BleedState>> it = BLEED_TARGETS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, BleedState> entry = it.next();
			LivingEntity entity = (LivingEntity) Bukkit.getEntity(entry.getKey());
			if (entity == null || entity.isDead()) {
				it.remove();
				continue;
			}
			BleedState state = entry.getValue();
			int savedTicks = entity.getNoDamageTicks();
			entity.setNoDamageTicks(0);
			entity.damage(DAMAGE_PER_STACK * state.stacks());
			entity.setNoDamageTicks(savedTicks);
			entity.getWorld().spawnParticle(
				Particle.DUST,
				entity.getLocation().add(0, entity.getHeight() * 0.5, 0),
				10, 0.3, 0.5, 0.3, BLOOD_PARTICLES
			);
			int remaining = state.ticksRemaining() - 1;
			if (remaining <= 0) {
				it.remove();
				PotionEffect weakness = entity.getPotionEffect(PotionEffectType.WEAKNESS);
				if (weakness != null && !weakness.hasParticles()) {
					entity.removePotionEffect(PotionEffectType.WEAKNESS);
				}
			} else {
				entry.setValue(new BleedState(state.stacks(), state.maxStacks(), remaining));
			}
		}
	}

	private static final class BleedListener implements Listener {
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
			Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.BLEED);
			if (en == null) {
				return;
			}
			int level = ChevvyItemUtil.getEnchantLevel(weapon, en);
			if (level <= 0) {
				return;
			}
			UUID targetId = target.getUniqueId();
			int maxStacks = maxStacksForLevel(level);
			int duration = durationForLevel(level);
			BleedState current = BLEED_TARGETS.get(targetId);
			int newStacks = current != null ? Math.min(current.stacks() + 1, maxStacks) : 1;
			BLEED_TARGETS.put(targetId, new BleedState(newStacks, maxStacks, duration));
			target.addPotionEffect(new PotionEffect(
				PotionEffectType.WEAKNESS, duration * 20 + 10, 0, false, false, true
			));
		}
	}

	public static void applyEnchantToStack(ItemStack stack, World world) {
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.BLEED);
		ChevvyItemUtil.applyEnchant(stack, en, NBT_KEY, LORE_KEY);
	}

	public static void runAdd(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		if (Enchantment.getByKey(ChevvyEnchantKeys.BLEED) == null) {
			sender.sendMessage(Component.text("chevvyenchants:bleed is not in the registry. Restart the server; check /datapack list for the plugin pack.", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (!Tag.ITEMS_SWORDS.isTagged(stack.getType())) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.bleed.need_sword"));
			return;
		}
		applyEnchantToStack(stack, player.getWorld());
		sender.sendMessage(Component.translatable("chevvyenchants.command.bleed.added"));
	}

	public static void runClear(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (stack.getType().isAir()) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.bleed.need_sword"));
			return;
		}
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.BLEED);
		ChevvyItemUtil.removeEnchant(stack, en, NBT_KEY, LORE_KEY);
		sender.sendMessage(Component.translatable("chevvyenchants.command.bleed.cleared"));
	}
}
