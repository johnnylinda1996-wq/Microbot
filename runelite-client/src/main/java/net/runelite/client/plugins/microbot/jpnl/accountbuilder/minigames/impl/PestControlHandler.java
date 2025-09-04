package net.runelite.client.plugins.microbot.jpnl.accountbuilder.minigames.impl;

import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.AllInOneConfig;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.enums.MinigameType;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.minigames.MinigameHandler;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.minigames.impl.pestcontrol.PestControlScript;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PestControlHandler implements MinigameHandler {

    private PestControlScript script;
    private AllInOneConfig config;
    private boolean initialized = false;

    private final Pattern SHIELD_DROP = Pattern.compile("The ([a-z]+), [^ ]+ portal shield has dropped!", Pattern.CASE_INSENSITIVE);

    @Override
    public MinigameType getType() {
        return MinigameType.PEST_CONTROL;
    }

    @Override
    public boolean execute() {
        if (!initialized) {
            initialize();
        }

        if (script == null) {
            return true; // Complete if we can't initialize
        }

        return script.execute();
    }

    @Override
    public String statusDetail() {
        if (script == null) return "Initializing...";

        if (script.isInPestControl()) {
            return "In game";
        } else if (script.isInBoat()) {
            return "Waiting in boat";
        } else {
            return "Preparing";
        }
    }

    public void setConfig(AllInOneConfig config) {
        this.config = config;
        this.initialized = false; // Force re-initialization with new config
    }

    private void initialize() {
        try {
            if (config == null) {
                Microbot.log("Pest Control Handler: No config provided");
                return;
            }

            // Pass AllInOneConfig directly to the script
            script = new PestControlScript(config);
            initialized = true;
            Microbot.log("Pest Control Handler initialized");
        } catch (Exception e) {
            Microbot.log("Failed to initialize Pest Control Handler: " + e.getMessage());
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage chatMessage) {
        if (script != null && chatMessage.getType() == ChatMessageType.GAMEMESSAGE) {
            Matcher matcher = SHIELD_DROP.matcher(chatMessage.getMessage());
            if (matcher.lookingAt()) {
                script.handleShieldDrop(matcher.group(1));
            }
        }
    }

    public void reset() {
        if (script != null) {
            script.reset();
        }
        initialized = false;
    }

    // Adapter removed; script now uses AllInOneConfig directly
}
