package com.chevvy;

import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChevvyEnchants extends JavaPlugin {
	public static final String MOD_ID = "chevvyenchants";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onEnable() {
		ChevvyEnchantCommands.register(this);
		ReflectDummy.register(this);
		ChevvyCreativeItems.register();
		ChevvyVillagerTrades.register(this);
		Excavation.register(this);
		Gravedigger.register(this);
		EmberHeart.register(this);
		Deforestation.register(this);
		WitherTouch.register(this);
		PoisonEdge.register(this);
		LavaStride.register(this);
		Sustenance.register(this);
		MinersLantern.register(this);
		Lifesteal.register(this);
		Executioner.register(this);
		Bleed.register(this);
		Paralyze.register(this);
		Blind.register(this);
		Frostbite.register(this);
		Windwalker.register(this);
		LastStand.register(this);
		AutoReplant.register(this);
		Soulbound.register(this);
		Purify.register(this);
		Fart.register(this);
		SonicShot.register(this);
		LOGGER.info("ChevvyEnchants initialized");
	}
}
