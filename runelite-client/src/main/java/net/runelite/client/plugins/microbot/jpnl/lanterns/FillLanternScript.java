package net.runelite.client.plugins.microbot.jpnl.lanterns;

import net.runelite.api.ItemID;
import net.runelite.api.NpcID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.skillcalculator.skills.MagicAction;

import java.util.concurrent.TimeUnit;

public class FillLanternScript extends Script {

    public static String version = "1.2";

    public State currentState = State.BANKING;
    private int profitCount = 0;
    private long startTime;

    // Item IDs
    private static final int BULLSEYE_LANTERN_EMPTY_ID = ItemID.BULLSEYE_LANTERN;
    private static final int BULLSEYE_LANTERN_FILLED_ID = ItemID.BULLSEYE_LANTERN_4550;
    private static final int SWAMP_TAR_ID = ItemID.SWAMP_TAR;
    private static final int STAMINA_POTION_ID = ItemID.STAMINA_POTION4;

    // NPC ID
    private static final int CANDLE_MAKER_NPC_ID = NpcID.CANDLE_MAKER;

    // Locations
    private static final WorldPoint RIMMINGTON_CANDLE_MAKER = new WorldPoint(2917, 3212, 0);
    private static final WorldPoint FALADOR_EAST_BANK = new WorldPoint(3013, 3355, 0);
    private static final WorldPoint RIMMINGTON_HOUSE_PORTAL = new WorldPoint(2953, 3224, 0);

    // Player-owned house region (approximate)
    private static final int POH_REGION = 7499;

    public enum State {
        BANKING,
        TELEPORTING_TO_RIMMINGTON,
        WALKING_TO_CANDLE_MAKER,
        FILLING_LANTERNS,
        RETURNING_TO_BANK
    }

    public boolean run(FillLanternConfig config, FillLanternPlugin plugin) {
        initialPlayerLocation = null;
        startTime = System.currentTimeMillis();

        // Setup antiban if enabled
        if (config.enableAntiban()) {
            setupAntiban();
        }

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;

                if (initialPlayerLocation == null) {
                    initialPlayerLocation = Rs2Player.getWorldLocation();
                }

                // Handle antiban
                if (config.enableAntiban()) {
                    Rs2Antiban.actionCooldown();
                    if (!Rs2Bank.isOpen()) {
                        Rs2Antiban.moveMouseOffScreen();
                    }
                }

                Rs2Combat.enableAutoRetialiate();
                handleState(config);

            } catch (Exception ex) {
                Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
            }
        }, 0, Rs2Random.between(600, 1200), TimeUnit.MILLISECONDS);

        return true;
    }

    private void setupAntiban() {
        Rs2AntibanSettings.naturalMouse = true;
        Rs2AntibanSettings.moveMouseOffScreen = true;
        Rs2AntibanSettings.actionCooldownActive = true;
        Rs2AntibanSettings.takeMicroBreaks = true;
        Rs2AntibanSettings.microBreakDurationLow = 1;
        Rs2AntibanSettings.microBreakDurationHigh = 4;
    }

    private void handleState(FillLanternConfig config) {
        switch (currentState) {
            case BANKING:
                handleBanking(config);
                break;
            case TELEPORTING_TO_RIMMINGTON:
                handleTeleporting(config);
                break;
            case WALKING_TO_CANDLE_MAKER:
                handleWalkingToCandleMaker();
                break;
            case FILLING_LANTERNS:
                handleFillingLanterns(config);
                break;
            case RETURNING_TO_BANK:
                handleReturningToBank(config);
                break;
        }
    }

    private void handleBanking(FillLanternConfig config) {
        if (!isNearBank()) {
            if (config.useHouseTeleport() && Rs2Player.getWorldLocation().distanceTo(RIMMINGTON_HOUSE_PORTAL) < 10) {
                if (Rs2Magic.canCast(MagicAction.TELEPORT_TO_HOUSE)) {
                    Rs2Magic.cast(MagicAction.TELEPORT_TO_HOUSE);
                    sleepUntil(() -> isInHouse(), 5000);
                }
                return;
            } else {
                Rs2Walker.walkTo(FALADOR_EAST_BANK);
                return;
            }
        }

        if (!Rs2Bank.isOpen()) {
            Rs2Bank.openBank();
            sleepUntil(() -> Rs2Bank.isOpen(), 3000);
            return;
        }

        // Deposit filled lanterns for profit tracking
        if (Rs2Inventory.hasItem(BULLSEYE_LANTERN_FILLED_ID)) {
            int filledCount = Rs2Inventory.count(BULLSEYE_LANTERN_FILLED_ID);
            Rs2Bank.depositAll(BULLSEYE_LANTERN_FILLED_ID);
            profitCount += filledCount;
            sleepUntil(() -> !Rs2Inventory.hasItem(BULLSEYE_LANTERN_FILLED_ID), 2000);
            return;
        }

        // Check if we need supplies
        if (!hasRequiredSupplies(config)) {
            Microbot.log("Missing required supplies in bank!");
            return;
        }

        // Withdraw empty lanterns
        if (!Rs2Inventory.hasItem(BULLSEYE_LANTERN_EMPTY_ID)) {
            int amount = Rs2Random.between(config.minLanterns(), config.maxLanterns());
            Rs2Bank.withdrawX(BULLSEYE_LANTERN_EMPTY_ID, amount);
            sleepUntil(() -> Rs2Inventory.hasItem(BULLSEYE_LANTERN_EMPTY_ID), 2000);
            return;
        }

        // Withdraw swamp tar
        if (!Rs2Inventory.hasItem(SWAMP_TAR_ID)) {
            int lanternCount = Rs2Inventory.count(BULLSEYE_LANTERN_EMPTY_ID);
            Rs2Bank.withdrawX(SWAMP_TAR_ID, lanternCount);
            sleepUntil(() -> Rs2Inventory.hasItem(SWAMP_TAR_ID), 2000);
            return;
        }

        // Withdraw stamina potion if needed
        if (config.useStamina() && Rs2Player.getRunEnergy() < 30 && !Rs2Inventory.hasItem(STAMINA_POTION_ID)) {
            Rs2Bank.withdrawX(STAMINA_POTION_ID, 1);
            sleepUntil(() -> Rs2Inventory.hasItem(STAMINA_POTION_ID), 2000);
            return;
        }

        Rs2Bank.closeBank();
        currentState = State.TELEPORTING_TO_RIMMINGTON;
    }

    private boolean isNearBank() {
        return Rs2Player.getWorldLocation().distanceTo(FALADOR_EAST_BANK) < 10;
    }

    private boolean isInHouse() {
        return Rs2Player.getWorldLocation().getRegionID() == POH_REGION;
    }

    private boolean hasRequiredSupplies(FillLanternConfig config) {
        int requiredLanterns = config.maxLanterns();
        int requiredTar = config.maxLanterns();

        return Rs2Bank.count(BULLSEYE_LANTERN_EMPTY_ID) >= requiredLanterns &&
                Rs2Bank.count(SWAMP_TAR_ID) >= requiredTar;
    }

    private void handleTeleporting(FillLanternConfig config) {
        // Use stamina potion if needed
        if (config.useStamina() && Rs2Player.getRunEnergy() < 30 && Rs2Inventory.hasItem(STAMINA_POTION_ID)) {
            Rs2Inventory.interact(STAMINA_POTION_ID, "Drink");
            sleep(Rs2Random.between(1000, 2000));
        }

        switch (config.teleportMethod()) {
            case FALADOR_TELEPORT:
                if (Rs2Magic.canCast(MagicAction.FALADOR_TELEPORT)) {
                    Rs2Magic.cast(MagicAction.FALADOR_TELEPORT);
                    sleepUntil(() -> Rs2Player.getWorldLocation().distanceTo(new WorldPoint(2964, 3378, 0)) < 10, 5000);
                }
                break;
            case RIMMINGTON_TELEPORT:
                // Note: Rimmington teleport might not be available as a standard MagicAction
                // You may need to use a different teleport method or tablet
                Rs2Walker.walkTo(RIMMINGTON_CANDLE_MAKER);
                break;
            case HOUSE_TELEPORT:
                if (Rs2Magic.canCast(MagicAction.TELEPORT_TO_HOUSE)) {
                    Rs2Magic.cast(MagicAction.TELEPORT_TO_HOUSE);
                    sleepUntil(() -> isInHouse(), 5000);
                    Rs2Walker.walkTo(RIMMINGTON_CANDLE_MAKER);
                }
                break;
            case WALKING:
                Rs2Walker.walkTo(RIMMINGTON_CANDLE_MAKER);
                break;
        }

        currentState = State.WALKING_TO_CANDLE_MAKER;
    }

    private void handleWalkingToCandleMaker() {
        if (Rs2Player.getWorldLocation().distanceTo(RIMMINGTON_CANDLE_MAKER) > 3) {
            Rs2Walker.walkTo(RIMMINGTON_CANDLE_MAKER);
            return;
        }
        currentState = State.FILLING_LANTERNS;
    }

    private void handleFillingLanterns(FillLanternConfig config) {
        if (!Rs2Inventory.hasItem(BULLSEYE_LANTERN_EMPTY_ID) || !Rs2Inventory.hasItem(SWAMP_TAR_ID)) {
            currentState = State.RETURNING_TO_BANK;
            return;
        }

        if (Rs2Npc.getNpc(CANDLE_MAKER_NPC_ID) != null) {
            if (Rs2Npc.interact(CANDLE_MAKER_NPC_ID, "Talk-to")) {
                sleepUntil(() -> Rs2Dialogue.isInDialogue(), 5000);

                if (Rs2Dialogue.isInDialogue()) {
                    handleCandleMakerDialogue();
                }
            }
        }

        // Add small delay for natural behavior
        sleep(Rs2Random.between(800, 1500));
    }

    private void handleCandleMakerDialogue() {
        if (Rs2Dialogue.hasSelectAnOption()) {
            if (Rs2Dialogue.clickOption("I'd like to buy some candles.")) {
                sleepUntil(() -> Rs2Dialogue.hasSelectAnOption(), 3000);
                Rs2Dialogue.clickOption("I have my own lantern that I'd like to fill.");
                sleepUntil(() -> !Rs2Dialogue.isInDialogue() ||
                        (!Rs2Inventory.hasItem(BULLSEYE_LANTERN_EMPTY_ID) || !Rs2Inventory.hasItem(SWAMP_TAR_ID)), 10000);
            }
        } else {
            Rs2Dialogue.clickContinue();
        }
    }

    private void handleReturningToBank(FillLanternConfig config) {
        WorldPoint targetBank = config.useHouseTeleport() ? RIMMINGTON_HOUSE_PORTAL : FALADOR_EAST_BANK;

        if (Rs2Player.getWorldLocation().distanceTo(targetBank) > 10) {
            Rs2Walker.walkTo(targetBank);
            return;
        }

        currentState = State.BANKING;
    }

    public int getProfitCount() {
        return profitCount;
    }

    public State getCurrentState() {
        return currentState;
    }

    public long getStartTime() {
        return startTime;
    }
}