package com.chevvy;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

final class ChevvyEnchantCommands implements BasicCommand {
	private static final ChevvyEnchantCommands INSTANCE = new ChevvyEnchantCommands();

	private ChevvyEnchantCommands() {}

	static void register(JavaPlugin plugin) {
		plugin.registerCommand(
			"chevvyenchants",
			"Apply or clear ChevvyEnchants custom enchantments",
			INSTANCE
		);
	}

	@Override
	public void execute(CommandSourceStack stack, String[] args) {
		CommandSender sender = stack.getSender();
		if (!ChevvyEnchantPermissions.mayUse(sender)) {
			sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
			return;
		}
		if (args.length < 2) {
			sender.sendMessage(Component.text("Usage: /chevvyenchants <excavation|gravedigger|emberheart|deforestation> <add|clear>", NamedTextColor.YELLOW));
			sender.sendMessage(Component.text("Run as a player in-game with the correct item in your main hand.", NamedTextColor.GRAY));
			return;
		}
		String sub = args[0].toLowerCase(Locale.ROOT);
		String act = args[1].toLowerCase(Locale.ROOT);
		if (!act.equals("add") && !act.equals("clear")) {
			sender.sendMessage(Component.text("Second argument must be add or clear.", NamedTextColor.RED));
			return;
		}
		boolean add = act.equals("add");
		switch (sub) {
			case "excavation":
				if (add) {
					Excavation.runAdd(sender);
				} else {
					Excavation.runClear(sender);
				}
				break;
			case "gravedigger":
				if (add) {
					Gravedigger.runAdd(sender);
				} else {
					Gravedigger.runClear(sender);
				}
				break;
			case "emberheart":
				if (add) {
					EmberHeart.runAdd(sender);
				} else {
					EmberHeart.runClear(sender);
				}
				break;
			case "deforestation":
				if (add) {
					Deforestation.runAdd(sender);
				} else {
					Deforestation.runClear(sender);
				}
				break;
			default:
				sender.sendMessage(Component.text("Unknown subcommand. Use excavation, gravedigger, emberheart, or deforestation.", NamedTextColor.RED));
		}
	}

	@Override
	public Collection<String> suggest(CommandSourceStack stack, String[] args) {
		if (!canUse(stack.getSender())) {
			return List.of();
		}
		if (args.length <= 1) {
			String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
			return Stream.of("excavation", "gravedigger", "emberheart", "deforestation")
				.filter(s -> prefix.isEmpty() || s.startsWith(prefix))
				.toList();
		}
		if (args.length == 2) {
			String prefix = args[1].toLowerCase(Locale.ROOT);
			return Stream.of("add", "clear")
				.filter(s -> prefix.isEmpty() || s.startsWith(prefix))
				.toList();
		}
		return List.of();
	}

	@Override
	public boolean canUse(CommandSender sender) {
		return ChevvyEnchantPermissions.mayUse(sender);
	}
}
