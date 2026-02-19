package net.mervyn.potatoscaling;

import net.fabricmc.api.ModInitializer;
import net.mervyn.potatoscaling.config.ScalingConfig;
import net.tinyconfig.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PotatoScalingMod implements ModInitializer {
    public static final String MOD_ID = "potatoscaling";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static ConfigManager<ScalingConfig> configManager;

    @Override
    public void onInitialize() {
        configManager = new ConfigManager<>(MOD_ID, new ScalingConfig())
                .builder()
                .setDirectory(MOD_ID)
                .sanitize(true)
                .build();
        configManager.refresh();
        configManager.value.cacheAttributes();
        LOGGER.info("Configuration loaded. Potato scaling active.");
    }
}
