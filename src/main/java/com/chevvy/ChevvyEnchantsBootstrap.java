package com.chevvy;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Objects;
import org.bukkit.plugin.java.JavaPlugin;

public final class ChevvyEnchantsBootstrap implements PluginBootstrap {

	@Override
	public void bootstrap(BootstrapContext context) {
		context.getLifecycleManager().registerEventHandler(LifecycleEvents.DATAPACK_DISCOVERY, event -> {
			var url = Objects.requireNonNull(
				ChevvyEnchantsBootstrap.class.getResource("/chevvyenchants_datapack"),
				"missing /chevvyenchants_datapack in plugin jar"
			);
			try {
				event.registrar().discoverPack(url.toURI(), "chevvyenchants");
			} catch (URISyntaxException | IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Override
	public JavaPlugin createPlugin(PluginProviderContext context) {
		return new ChevvyEnchants();
	}
}
