package net.runelite.client.plugins.microbot.jpnl.accountbuilder;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.awt.*;

/**
 * Entry point voor de AIO controller plugin.
 * Verantwoordelijk voor:
 *  - Instantiëren van script
 *  - Laden/schrijven van queue persistentie
 *  - Initialiseren van GUI
 *  - Netjes opruimen bij shutdown
 */
@PluginDescriptor(
        name = PluginDescriptor.JP96NL + "Account Builder",
        description = "Queue-based trainer & quest runner",
        tags = {"aio","queue","skills","quests","microbot"},
        enabledByDefault = false
)
@Slf4j
public class AllInOnePlugin extends Plugin {

    @Inject
    private AllInOneConfig config;

    @Inject
    private ConfigManager configManager;

    private AllInOneScript script;
    private AllInOneBotGUI gui;

    private static final String GROUP = "allInOneAio";
    private static final String QUEUE_KEY = "queuePersistence";

    @Provides
    AllInOneConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(AllInOneConfig.class);
    }

    @Override
    protected void startUp() throws AWTException {
        script = new AllInOneScript(config, configManager, GROUP, QUEUE_KEY);
        // Queue laden vóór starten
        script.loadQueueFromConfig();
        if (config.autoStart()) {
            script.startLoop();
        }
        gui = new AllInOneBotGUI(script);
        log.info("[AIO] Plugin gestart");
    }

    @Override
    protected void shutDown() {
        if (script != null) {
            try {
                script.saveQueueToConfig();
                script.shutdown();
            } catch (Exception ex) {
                log.warn("[AIO] Fout tijdens shutdown script", ex);
            }
        }
        if (gui != null) {
            gui.setVisible(false);
            gui.dispose();
            gui = null;
        }
        log.info("[AIO] Plugin gestopt");
    }
}