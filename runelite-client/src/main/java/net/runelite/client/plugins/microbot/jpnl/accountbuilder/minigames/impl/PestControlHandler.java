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

            script = new PestControlScript(new ConfigAdapter(config));
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

    // Adapter class to convert AllInOneConfig to PestControlConfig interface
    private static class ConfigAdapter implements net.runelite.client.plugins.microbot.jpnl.accountbuilder.minigames.impl.pestcontrol.PestControlConfig {
        private final AllInOneConfig config;

        public ConfigAdapter(AllInOneConfig config) {
            this.config = config;
        }

        @Override
        public String GUIDE() {
            return "Start near a boat of your combat level";
        }

        @Override
        public net.runelite.client.plugins.microbot.jpnl.accountbuilder.minigames.impl.pestcontrol.PestControlNpc Priority1() {
            return convertNpc(config.pestControlPriority1());
        }

        @Override
        public net.runelite.client.plugins.microbot.jpnl.accountbuilder.minigames.impl.pestcontrol.PestControlNpc Priority2() {
            return convertNpc(config.pestControlPriority2());
        }

        @Override
        public net.runelite.client.plugins.microbot.jpnl.accountbuilder.minigames.impl.pestcontrol.PestControlNpc Priority3() {
            return convertNpc(config.pestControlPriority3());
        }

        @Override
        public boolean alchInBoat() {
            return config.pestControlAlchInBoat();
        }

        @Override
        public String alchItem() {
            return config.pestControlAlchItem();
        }

        @Override
        public boolean quickPrayer() {
            return config.pestControlQuickPrayer();
        }

        @Override
        public int specialAttackPercentage() {
            return config.pestControlSpecAttack();
        }

        @Override
        public net.runelite.client.plugins.microbot.inventorysetups.InventorySetup inventorySetup() {
            return null; // Not implemented in AllInOne system yet
        }

        @Override
        public int world() {
            return config.pestControlWorld();
        }

        private net.runelite.client.plugins.microbot.jpnl.accountbuilder.minigames.impl.pestcontrol.PestControlNpc convertNpc(AllInOneConfig.PestControlNpc npc) {
            switch (npc) {
                case PORTAL:
                    return net.runelite.client.plugins.microbot.jpnl.accountbuilder.minigames.impl.pestcontrol.PestControlNpc.PORTAL;
                case BRAWLER:
                    return net.runelite.client.plugins.microbot.jpnl.accountbuilder.minigames.impl.pestcontrol.PestControlNpc.BRAWLER;
                case SPINNER:
                    return net.runelite.client.plugins.microbot.jpnl.accountbuilder.minigames.impl.pestcontrol.PestControlNpc.SPINNER;
                default:
                    return net.runelite.client.plugins.microbot.jpnl.accountbuilder.minigames.impl.pestcontrol.PestControlNpc.PORTAL;
            }
        }
    }
}
