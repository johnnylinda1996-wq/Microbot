package net.runelite.client.plugins.microbot.mule;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;

@PluginDescriptor(
        name = PluginDescriptor.Mocrosoft + "Mule Bot",
        description = "Automated mule trading system that waits for requests from bridge server",
        tags = {"mule", "trading", "microbot", "automation"},
        enabledByDefault = false
)
@Slf4j
public class MulePlugin extends Plugin {

    @Inject
    private MuleScript muleScript;

    @Inject
    private MuleConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private MuleOverlay muleOverlay;

    private MuleBridgeGui bridgeGui;

    @Provides
    MuleConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(MuleConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        if (overlayManager != null) {
            overlayManager.add(muleOverlay);
        }

        // Create and show bridge GUI if enabled
        if (config.showBridgeGui()) {
            bridgeGui = new MuleBridgeGui(config, muleScript);
            bridgeGui.showGui();
        }

        log.info("Mule plugin started");

        // Start the mule script
        muleScript.run(config);
    }

    @Override
    protected void shutDown() throws Exception {
        muleScript.shutdown();

        if (overlayManager != null) {
            overlayManager.remove(muleOverlay);
        }

        // Hide and dispose GUI
        if (bridgeGui != null) {
            bridgeGui.dispose();
            bridgeGui = null;
        }

        log.info("Mule plugin stopped");
    }
}
