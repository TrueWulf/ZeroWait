package dev.truewulf.zerowait;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ZeroWait implements ClientModInitializer {
    public static final String MOD_ID = "zerowait";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("ZeroWait initialized");
    }
}
