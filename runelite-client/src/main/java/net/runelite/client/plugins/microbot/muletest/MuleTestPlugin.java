package net.runelite.client.plugins.microbot.muletest;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;

@PluginDescriptor(
        name = "Mule Test",
        description = "Test script for mule system - automatically requests mule on a time interval (drop-trade only)",
        tags = {"mule", "trading", "microbot", "test"},
        enabledByDefault = false
)
@Slf4j
public class MuleTestPlugin extends Plugin {

    @Inject
    private MuleTestScript muleTestScript;

    @Inject
    private MuleTestConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private MuleTestOverlay muleTestOverlay;

    @Provides
    MuleTestConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(MuleTestConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        if (overlayManager != null) {
            overlayManager.add(muleTestOverlay);
        }
        muleTestScript.run(config);
        log.info("Mule Test Plugin started - interval: {} hours", config.muleIntervalHours());
    }

    @Override
    protected void shutDown() {
        muleTestScript.shutdown();
        if (overlayManager != null) {
            overlayManager.remove(muleTestOverlay);
        }
        log.info("Mule Test Plugin stopped");
    }

    @Subscribe
    public void onGameTick(GameTick gameTick) {
        // Optional: Add any per-tick logic here if needed
        if (!Microbot.isLoggedIn()) return;

        // Example: Could add additional checks or logging here
        // The main logic runs in the MuleTestScript scheduled task
    }
}
