package com.chevvy;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

final class ChevvyEnchantPermissions {
	private ChevvyEnchantPermissions() {}

	static boolean mayUse(CommandSender sender) {
		if (sender instanceof ConsoleCommandSender) {
			return true;
		}
		if (sender instanceof Player player) {
			return player.hasPermission("chevvyenchants.commands");
		}
		return false;
	}
}
