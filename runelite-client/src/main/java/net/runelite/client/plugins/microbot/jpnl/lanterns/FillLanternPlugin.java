package net.runelite.client.plugins.microbot.jpnl.lanterns;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginDescriptor.JP96NL + "Bullseye Lantern Filler",
        description = "Automatically fills bullseye lanterns with swamp tar for profit. Features teleportation with runes and integrated break handling.",
        tags = {"Skilling", "microbot", "profit", "lanterns", "rimmington", "money making"},
        enabledByDefault = false
)
@Slf4j
public class FillLanternPlugin extends Plugin {

    @Inject
    private FillLanternConfig config;

    @Provides
    FillLanternConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(FillLanternConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private FillLanternOverlay fillLanternOverlay;

    @Inject
    public FillLanternScript fillLanternScript;

    private boolean originalBreakHandlerState = false;

    @Override
    protected void startUp() throws AWTException {
        Microbot.pauseAllScripts.set(false);

        // Store the original break handler state
        originalBreakHandlerState = config.enableBreakHandler();

        if (overlayManager != null) {
            overlayManager.add(fillLanternOverlay);
        }

        // Start the script with the current configuration
        fillLanternScript.run(config, this);

        Microbot.log("Bullseye Lantern Filler v" + FillLanternScript.version + " started!");
        Microbot.log("Teleport method: " + config.teleportMethod().getName());
        Microbot.log("Anti-ban enabled: " + config.enableAntiban());

        // Only enable break handler if user has it enabled
        if (config.enableBreakHandler()) {
            Microbot.log("Break handler enabled: " + config.enableBreakHandler());
        } else {
            Microbot.log("Break handler disabled by user");
        }
    }

    /**
     * Get the current running script instance
     * @return The FillLanternScript instance
     */
    private FillLanternScript script = null;
    public FillLanternScript getScript() {
        return this.script;
    }

    protected void shutDown() {
        try {
            fillLanternScript.shutdown();

            if (overlayManager != null && fillLanternOverlay != null) {
                overlayManager.remove(fillLanternOverlay);
            }

            Microbot.log("Bullseye Lantern Filler stopped!");
        } catch (Exception e) {
            Microbot.log("Error during shutdown: " + e.getMessage());
        }
    }

    // Method to check if break handler was enabled by user
    public boolean wasBreakHandlerEnabledByUser() {
        return originalBreakHandlerState;
    }
}