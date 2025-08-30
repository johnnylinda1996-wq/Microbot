package net.runelite.client.plugins.microbot.aiobot.skills;

import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.aiobot.settings.SkillRuntimeSettings;
import net.runelite.client.plugins.microbot.aiobot.skills.helpers.OreProgressionHelper;
import net.runelite.client.plugins.microbot.aiobot.skills.helpers.SimpleBankWalker;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import java.util.Set;

import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

/**
 * Mining handler met modes:
 *  - POWERDROP: alle bekende ores droppen
 *  - BANK: bank all except pickaxe (placeholder)
 *  - AUTO: kies ore obv level; droppen of banken? (hier: powerdrop stijl tot Runite, pas aan naar wens)
 *
 *  customList (settings.customList) = whitelist van ore namen; zo niet leeg override voor AUTO selectie.
 */
public class MiningSkillHandler implements SkillHandler {

    private boolean enabled;
    private int targetLevel;
    private String mode;
    private Set<String> customRocks = Set.of();
    private boolean hopIfNoResource;

    // State
    private String activeOre = "Copper ore";
    private long lastNoRockTs = 0;

    @Override
    public void applySettings(SkillRuntimeSettings s) {
        if (s == null) {
            enabled = false;
            return;
        }
        enabled = s.isEnabled();
        targetLevel = s.getTargetLevel();
        mode = s.getMode();
        customRocks = s.getCustomList();
        hopIfNoResource = s.isHopIfNoResource();
    }

    @Override
    public void execute() {
        if (!enabled) {
            Microbot.status = "Mining disabled";
            return;
        }

        if (determineMode().equals("BANK")) {
            handleBankMode();
        } else { // POWERDROP of AUTO
            handleDropOrAuto();
        }
    }

    /* ===================== Mode Handlers ===================== */

    private void handleDropOrAuto() {
        // Inventory full -> drop
        if (Rs2Inventory.isFull()) {
            dropOres();
            Microbot.status = "Mining (" + determineMode() + "): Dropping";
            return;
        }

        if (Rs2Player.isAnimating()) {
            Microbot.status = "Mining (" + activeOre + ")";
            return;
        }

        selectActiveOreIfNeeded();

        // Probeer interactie
        if (Rs2GameObject.interact(activeOre, "Mine")
                || Rs2GameObject.interact("Rocks", "Mine")) {
            Microbot.status = "Mining: " + activeOre;
            sleepUntil(Rs2Player::isAnimating, 3000);
        } else {
            Microbot.status = "Mining: No " + activeOre;
            lastNoRockTs = System.currentTimeMillis();
            maybeHopWorld();
        }
    }

    private void handleBankMode() {
        // Vol? -> bank
        if (Rs2Inventory.isFull()) {
            Microbot.status = "Mining (BANK): Banking...";
            if (SimpleBankWalker.bankAllExcept(pickaxeKeywords())) {
                Microbot.status = "Mining (BANK): Banked";
            }
            return;
        }

        if (Rs2Player.isAnimating()) {
            Microbot.status = "Mining (BANK): Animating";
            return;
        }

        selectActiveOreIfNeeded();

        if (Rs2GameObject.interact(activeOre, "Mine")
                || Rs2GameObject.interact("Rocks", "Mine")) {
            Microbot.status = "Mining (BANK): " + activeOre;
            sleepUntil(Rs2Player::isAnimating, 3000);
        } else {
            Microbot.status = "Mining (BANK): No " + activeOre;
            lastNoRockTs = System.currentTimeMillis();
            maybeHopWorld();
        }
    }

    /* ===================== Helpers ===================== */

    private void selectActiveOreIfNeeded() {
        if (isAutoMode()) {
            int lvl = Microbot.getClient().getRealSkillLevel(Skill.MINING);
            if (!customRocks.isEmpty()) {
                // kies eerste uit whitelist dat <= level requirement (simpel: we checken niet; nemen eerste)
                activeOre = customRocks.iterator().next();
            } else {
                activeOre = OreProgressionHelper.bestOreForLevel(lvl);
            }
        } else if (activeOre == null) {
            activeOre = "Copper ore";
        }
    }

    private void dropOres() {
        String[] ores = {
                "Copper ore","Tin ore","Iron ore","Clay","Coal","Silver ore",
                "Gold ore","Mithril ore","Adamantite ore","Runite ore"
        };
        for (String ore : ores) {
            while (Rs2Inventory.hasItem(ore)) {
                Rs2Inventory.drop(ore);
            }
        }
    }

    private void maybeHopWorld() {
        if (!hopIfNoResource) return;
        if (System.currentTimeMillis() - lastNoRockTs > 6000) {
            // TODO: world hop implementeren
            Microbot.log("Mining: (placeholder) would hop world now.");
            lastNoRockTs = System.currentTimeMillis();
        }
    }

    private String determineMode() {
        if (mode == null) return "POWERDROP";
        String m = mode.toUpperCase();
        if (m.startsWith("BANK")) return "BANK";
        if (m.startsWith("AUTO")) return "AUTO";
        if (m.contains("DROP") || m.contains("POWER")) return "POWERDROP";
        return m;
    }

    private boolean isAutoMode() {
        return "AUTO".equals(determineMode());
    }

    private String[] pickaxeKeywords() {
        return new String[]{
                "Bronze pickaxe","Iron pickaxe","Steel pickaxe","Black pickaxe","Mithril pickaxe",
                "Adamant pickaxe","Rune pickaxe","Dragon pickaxe","Infernal pickaxe","Crystal pickaxe"
        };
    }
}