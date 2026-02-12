package net.mervyn.potatoscaling;

import net.fabricmc.api.ModInitializer;
import net.mervyn.potatoscaling.config.ScalingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PotatoScalingMod implements ModInitializer {
    public static final String MOD_ID = "potatoscaling";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Potato Scaling Mod initialized. Loading configuration...");
        ScalingConfig.load();
        LOGGER.info("Configuration loaded. Potato scaling active.");
    }
}
