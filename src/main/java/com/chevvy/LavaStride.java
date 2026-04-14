package com.chevvy;

import org.bukkit.command.CommandSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public final class LavaStride {
	public static final String NBT_KEY = "chevvyenchants_lava_stride";
	public static final String LORE_KEY = "enchantment.chevvyenchants.lava_stride";

	private static final double HORIZONTAL_ACCELERATION = 0.045;
	private static final double MAX_HORIZONTAL_SPEED = 0.42;
	private static final double IDLE_DAMPING_FACTOR = 0.85;
	private static final double UPWARD_THRUST = 0.12;
	private static final double DOWNWARD_THRUST = -0.08;

	private LavaStride() {}

	public static void register(JavaPlugin plugin) {
		Bukkit.getScheduler().runTaskTimer(plugin, LavaStride::tickAllWorlds, 0L, 1L);
	}

	private static void tickAllWorlds() {
		for (World world : Bukkit.getWorlds()) {
			for (Player player : world.getPlayers()) {
			if (!player.isInLava()) {
				continue;
			}
			ItemStack boots = player.getInventory().getBoots();
			if (!isOnStack(boots, world)) {
				continue;
			}
			var input = player.getCurrentInput();
			boolean movingInput = input.isForward() || input.isBackward() || input.isLeft() || input.isRight();
			Vector current = player.getVelocity();
			double nextX = current.getX() * IDLE_DAMPING_FACTOR;
			double nextZ = current.getZ() * IDLE_DAMPING_FACTOR;
			if (movingInput) {
				Vector look = player.getLocation().getDirection();
				Vector forward = new Vector(look.getX(), 0, look.getZ());
				double flen = forward.length();
				if (flen > 1.0e-6) {
					forward.multiply(1.0 / flen);
					Vector right = new Vector(-forward.getZ(), 0, forward.getX());
					double ix = 0.0;
					double iz = 0.0;
					if (input.isForward()) {
						ix += forward.getX();
						iz += forward.getZ();
					}
					if (input.isBackward()) {
						ix -= forward.getX();
						iz -= forward.getZ();
					}
					if (input.isRight()) {
						ix += right.getX();
						iz += right.getZ();
					}
					if (input.isLeft()) {
						ix -= right.getX();
						iz -= right.getZ();
					}
					double horizontal = Math.hypot(ix, iz);
					if (horizontal > 1.0e-6) {
						double nx = ix / horizontal;
						double nz = iz / horizontal;
						nextX = current.getX() + nx * HORIZONTAL_ACCELERATION;
						nextZ = current.getZ() + nz * HORIZONTAL_ACCELERATION;
						double nextHorizontal = Math.hypot(nextX, nextZ);
						if (nextHorizontal > MAX_HORIZONTAL_SPEED) {
							double scale = MAX_HORIZONTAL_SPEED / nextHorizontal;
							nextX *= scale;
							nextZ *= scale;
						}
					}
				}
			}
			double nextY = current.getY();
			if (input.isJump()) {
				nextY = Math.max(nextY, UPWARD_THRUST);
			} else if (player.isSneaking()) {
				nextY = Math.min(nextY, DOWNWARD_THRUST);
			}
			player.setVelocity(new Vector(nextX, nextY, nextZ));
		}
	}
	}

	public static void applyEnchantToStack(ItemStack stack, World world) {
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.LAVA_STRIDE);
		ChevvyItemUtil.applyEnchant(stack, en, NBT_KEY, LORE_KEY);
	}

	public static void runAdd(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		if (Enchantment.getByKey(ChevvyEnchantKeys.LAVA_STRIDE) == null) {
			sender.sendMessage(Component.text("chevvyenchants:lava_stride is not in the registry. Restart the server; check /datapack list for the plugin pack.", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (!Tag.ITEMS_ENCHANTABLE_FOOT_ARMOR.isTagged(stack.getType())) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.lava_stride.need_boots"));
			return;
		}
		applyEnchantToStack(stack, player.getWorld());
		sender.sendMessage(Component.translatable("chevvyenchants.command.lava_stride.added"));
	}

	public static void runClear(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("This command must be run by a player (use it in-game with /).", NamedTextColor.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (stack.getType().isAir()) {
			sender.sendMessage(Component.translatable("chevvyenchants.command.lava_stride.need_boots"));
			return;
		}
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.LAVA_STRIDE);
		ChevvyItemUtil.removeEnchant(stack, en, NBT_KEY, LORE_KEY);
		sender.sendMessage(Component.translatable("chevvyenchants.command.lava_stride.cleared"));
	}

	public static boolean isOnStack(ItemStack stack, World world) {
		if (stack == null || stack.getType().isAir()) {
			return false;
		}
		Enchantment en = Enchantment.getByKey(ChevvyEnchantKeys.LAVA_STRIDE);
		return ChevvyItemUtil.hasEnchantOrLegacyNbt(stack, en, NBT_KEY);
	}
}
