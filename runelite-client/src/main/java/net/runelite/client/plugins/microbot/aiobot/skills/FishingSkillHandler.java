package net.runelite.client.plugins.microbot.aiobot.skills;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.aiobot.settings.SkillRuntimeSettings;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import java.util.Set;

import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

/**
 * Eenvoudige fishing handler die settings leest.
 * (Nog steeds placeholder logic; breid later uit met method progression / tiles etc.)
 */
public class FishingSkillHandler implements SkillHandler {

    private boolean enabled;
    private int targetLevel;
    private String mode;
    private boolean useSpec;
    private Set<String> dropList = Set.of();

    @Override
    public void applySettings(SkillRuntimeSettings s) {
        if (s == null) {
            enabled = false;
            return;
        }
        enabled = s.isEnabled();
        targetLevel = s.getTargetLevel();
        mode = s.getMode();
        useSpec = s.isUseSpecial();
        dropList = s.getCustomList();
    }

    @Override
    public void execute() {
        if (!enabled) {
            Microbot.status = "Fishing disabled";
            return;
        }

        // Heel simpele placeholder: als inventaris vol -> drop configuratie
        if (Rs2Inventory.isFull()) {
            boolean any = false;
            for (String item : dropList) {
                while (Rs2Inventory.hasItem(item)) {
                    Rs2Inventory.drop(item);
                    any = true;
                }
            }
            Microbot.status = any ? "Fishing: Dropping fish (" + mode + ")" : "Fishing: Full (no droppables)";
            return;
        }

        if (Rs2Player.isAnimating()) {
            Microbot.status = "Fishing: Animating (" + mode + ")";
            return;
        }

        var spot = Rs2Npc.getNpc("Fishing spot");
        if (spot != null) {
            if (Rs2Npc.interact(spot, deriveAction(mode))) {
                Microbot.status = "Fishing: Interacting (" + mode + ")";
                sleepUntil(Rs2Player::isAnimating, 4000);
            } else {
                Microbot.status = "Fishing: Failed interact";
            }
        } else {
            Microbot.status = "Fishing: No spot";
        }
    }

    private String deriveAction(String m) {
        if (m == null) return "Net";
        switch (m) {
            case "POWERFISH": // treat as standard low-level fish
            case "BANK":
                return "Net";
            default:
                return "Net";
        }
    }
}