package net.runelite.client.plugins.microbot.jpnl.accountbuilder;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.minigames.impl.PestControlHandler;

import javax.inject.Inject;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private final Pattern SHIELD_DROP = Pattern.compile("The ([a-z]+), [^ ]+ portal shield has dropped!", Pattern.CASE_INSENSITIVE);

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

    @Subscribe
    public void onChatMessage(ChatMessage chatMessage) {
        if (script == null || chatMessage.getType() != ChatMessageType.GAMEMESSAGE) {
            return;
        }

        // Handle pest control shield drop messages
        Matcher matcher = SHIELD_DROP.matcher(chatMessage.getMessage());
        if (matcher.lookingAt()) {
            // Get the current minigame handler if it's pest control
            try {
                if (script.getCurrentTask() != null &&
                    script.getCurrentTask() instanceof net.runelite.client.plugins.microbot.jpnl.accountbuilder.tasks.AioMinigameTask) {

                    net.runelite.client.plugins.microbot.jpnl.accountbuilder.tasks.AioMinigameTask minigameTask =
                        (net.runelite.client.plugins.microbot.jpnl.accountbuilder.tasks.AioMinigameTask) script.getCurrentTask();

                    if (minigameTask.getMinigameType() == net.runelite.client.plugins.microbot.jpnl.accountbuilder.enums.MinigameType.PEST_CONTROL) {
                        // Forward the shield drop event to the pest control handler
                        PestControlHandler pestControlHandler = (PestControlHandler) script.getMinigameHandlers().get(
                            net.runelite.client.plugins.microbot.jpnl.accountbuilder.enums.MinigameType.PEST_CONTROL);
                        if (pestControlHandler != null) {
                            pestControlHandler.onChatMessage(chatMessage);
                        }
                    }
                }
            } catch (Exception ex) {
                log.debug("[AIO] Error handling pest control chat message: {}", ex.getMessage());
            }
        }
    }
}