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
        description = "Automatically fills bullseye lanterns with swamp tar for profit. Features multiple teleport methods and integrated break handling.",
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

    @Override
    protected void startUp() throws AWTException {
        Microbot.pauseAllScripts.set(false);

        if (overlayManager != null) {
            overlayManager.add(fillLanternOverlay);
        }

        fillLanternScript.run(config, this);

        Microbot.log("Bullseye Lantern Filler started!");
        Microbot.log("Teleport method: " + config.teleportMethod().getName());
        Microbot.log("Anti-ban enabled: " + config.enableAntiban());
        Microbot.log("Break handler enabled: " + config.enableBreakHandler());
    }

    protected void shutDown() {
        fillLanternScript.shutdown();
        overlayManager.remove(fillLanternOverlay);
        Microbot.log("Bullseye Lantern Filler stopped!");
    }
}