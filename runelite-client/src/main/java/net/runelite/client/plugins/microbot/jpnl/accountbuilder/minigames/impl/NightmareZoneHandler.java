/*
 * Copyright (c) 2018, Nickolaj <https://github.com/fire-proof>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.microbot.jpnl.accountbuilder.minigames.impl;

import net.runelite.api.ChatMessageType;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.AllInOneConfig;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.enums.MinigameType;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.minigames.MinigameHandler;
import net.runelite.client.util.Text;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

public class NightmareZoneHandler extends Script implements MinigameHandler {

    private static final int[] NMZ_MAP_REGION = {9033};
    private static final Duration OVERLOAD_DURATION = Duration.ofMinutes(5);

    private AllInOneConfig config;

    private boolean absorptionNotificationSend = true;
    private boolean overloadNotificationSend = false;
    private Instant lastOverload;

    private boolean isInitialized = false;

    public NightmareZoneHandler() {
        super();
    }

    public void initialize(AllInOneConfig config) {
        this.config = config;
        this.isInitialized = true;

        // Initialize antiban settings for NMZ
        net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings.actionCooldownActive = true;
        net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings.takeMicroBreaks = true;
        net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings.microBreakDurationLow = 3;
        net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings.microBreakDurationHigh = 8;
    }

    @Override
    public MinigameType getType() {
        return MinigameType.NIGHTMARE_ZONE;
    }

    @Override
    public boolean execute() {
        if (!isInitialized || config == null) {
            return false;
        }

        try {
            if (!isInNightmareZone()) {
                Microbot.log("Not in Nightmare Zone. Navigating...");
                // TODO: Add navigation logic to NMZ if not already there
                return false;
            }

            // Check absorption points
            checkAbsorption();

            // Check overload timer
            if (overloadNotificationSend && config.nmzOverloadEarlyWarningSeconds() > 0) {
                checkOverload();
            }

            // Drink absorption potions if needed
            if (needsAbsorptionPotion()) {
                drinkAbsorptionPotion();
            }

            // Drink overload potion if needed
            if (needsOverloadPotion()) {
                drinkOverloadPotion();
            }

            // Use special attack if enabled and available
            if (config.nmzUseSpecialAttack() && net.runelite.client.plugins.microbot.util.combat.Rs2Combat.getSpecEnergy() >= 500) { // 500 = 50%
                net.runelite.client.plugins.microbot.util.combat.Rs2Combat.setSpecState(true);
            }

            // Handle power-ups if configured
            if (config.nmzUsePowerUps()) {
                handlePowerUps();
            }

            // Use prayer flicking if enabled
            if (config.nmzUseQuickPrayer()) {
                net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer.toggleQuickPrayer(true);
            }

            // Anti-ban actions
            if (net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings.takeMicroBreaks) {
                net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban.actionCooldown();
                net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban.takeMicroBreakByChance();
            }

            sleep(600, 800);
            return true;

        } catch (Exception e) {
            Microbot.log("Error in NMZ handler: " + e.getMessage());
            return false;
        }
    }

    private void checkAbsorption() {
        int absorptionPoints = Microbot.getClient().getVarbitValue(VarbitID.NZONE_ABSORB_POTION_EFFECTS);

        if (!absorptionNotificationSend) {
            if (absorptionPoints < config.nmzAbsorptionThreshold()) {
                Microbot.log("Absorption points below threshold: " + absorptionPoints);
                absorptionNotificationSend = true;
            }
        } else {
            if (absorptionPoints > config.nmzAbsorptionThreshold()) {
                absorptionNotificationSend = false;
            }
        }
    }

    private void checkOverload() {
        if (lastOverload != null &&
            Instant.now().isAfter(lastOverload.plus(OVERLOAD_DURATION).minus(Duration.ofSeconds(config.nmzOverloadEarlyWarningSeconds())))) {
            Microbot.log("Overload potion is about to expire!");
            overloadNotificationSend = false;
        }
    }

    private boolean needsAbsorptionPotion() {
        int absorptionPoints = Microbot.getClient().getVarbitValue(VarbitID.NZONE_ABSORB_POTION_EFFECTS);
        return absorptionPoints < config.nmzAbsorptionThreshold() &&
               net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory.hasItem("Absorption (4)", "Absorption (3)", "Absorption (2)", "Absorption (1)");
    }

    private void drinkAbsorptionPotion() {
        if (net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory.interact("Absorption (4)", "Drink") ||
            net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory.interact("Absorption (3)", "Drink") ||
            net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory.interact("Absorption (2)", "Drink") ||
            net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory.interact("Absorption (1)", "Drink")) {
            Microbot.log("Drinking absorption potion");
            sleep(600, 1200);
        }
    }

    private boolean needsOverloadPotion() {
        // Check if overload effect has worn off or is about to
        return (lastOverload == null ||
                Instant.now().isAfter(lastOverload.plus(OVERLOAD_DURATION).minus(Duration.ofSeconds(30)))) &&
               net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory.hasItem("Overload (4)", "Overload (3)", "Overload (2)", "Overload (1)");
    }

    private void drinkOverloadPotion() {
        if (net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory.interact("Overload (4)", "Drink") ||
            net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory.interact("Overload (3)", "Drink") ||
            net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory.interact("Overload (2)", "Drink") ||
            net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory.interact("Overload (1)", "Drink")) {
            Microbot.log("Drinking overload potion");
            lastOverload = Instant.now();
            overloadNotificationSend = true;
            sleep(600, 1200);
        }
    }

    private void handlePowerUps() {
        // Look for power-ups and pick them up
        // Power surge, Recurrent damage, Zapper, Ultimate force
        // This would require specific widget or game object interaction
        // TODO: Implement power-up detection and interaction
    }

    public boolean isInNightmareZone() {
        if (Microbot.getClient().getLocalPlayer() == null) {
            return false;
        }

        // NMZ and the KBD lair uses the same region ID but NMZ uses planes 1-3 and KBD uses plane 0
        return Microbot.getClient().getLocalPlayer().getWorldLocation().getPlane() > 0 &&
               Microbot.getClient().isInInstancedRegion();
    }

    public void onChatMessage(String message, ChatMessageType type) {
        if (!isInNightmareZone() || (type != ChatMessageType.GAMEMESSAGE && type != ChatMessageType.SPAM)) {
            return;
        }

        String msg = Text.removeTags(message);

        if (msg.contains("The effects of overload have worn off, and you feel normal again.")) {
            overloadNotificationSend = false;
            Microbot.log("Overload has worn off");
        } else if (msg.contains("A power-up has spawned:")) {
            if (msg.contains("Power surge") && config.nmzPowerSurgeNotification()) {
                Microbot.log("Power surge spawned!");
            } else if (msg.contains("Recurrent damage") && config.nmzRecurrentDamageNotification()) {
                Microbot.log("Recurrent damage spawned!");
            } else if (msg.contains("Zapper") && config.nmzZapperNotification()) {
                Microbot.log("Zapper spawned!");
            } else if (msg.contains("Ultimate force") && config.nmzUltimateForceNotification()) {
                Microbot.log("Ultimate force spawned!");
            }
        } else if (msg.contains("You drink some of your overload potion.")) {
            lastOverload = Instant.now();
            overloadNotificationSend = true;
        }
    }

    public void shutdown() {
        super.shutdown();
    }
}
