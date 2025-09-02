package net.runelite.client.plugins.microbot.jpnl.accountbuilder.skills;

import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.settings.SkillRuntimeSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.concurrent.ThreadLocalRandom;

import static net.runelite.api.gameval.ItemID.TINDERBOX;

/**
 * FiremakingSkillHandler - Based on working AutoWoodcuttingScript patterns
 * Integrates with AllInOneConfig for log type selection and proper firemaking mechanics
 */
public class FiremakingSkillHandler implements SkillHandler {

    private boolean enabled = true;
    private String logType = "AUTO"; // From AllInOneConfig.fmMode()
    private static WorldPoint initialPlayerLocation = null;
    private static WorldPoint firemakingSpot = null;
    private long lastAttempt = 0L;
    private static final long ATTEMPT_COOLDOWN = 1200L;

    // Log types matching AllInOneConfig.FiremakingLogType enum
    private enum LogInfo {
        NORMAL("Logs", 1),
        OAK("Oak logs", 15),
        WILLOW("Willow logs", 30),
        TEAK("Teak logs", 35),
        MAPLE("Maple logs", 45),
        MAHOGANY("Mahogany logs", 50),
        YEW("Yew logs", 60),
        MAGIC("Magic logs", 75),
        REDWOOD("Redwood logs", 90);

        private final String itemName;
        private final int levelRequired;

        LogInfo(String itemName, int levelRequired) {
            this.itemName = itemName;
            this.levelRequired = levelRequired;
        }

        public String getItemName() { return itemName; }
        public int getLevelRequired() { return levelRequired; }
    }

    // Firemaking locations - 10 locaties met aanpasbare coordinaten
    private enum FiremakingSpots {
        GRAND_EXCHANGE("Grand Exchange", new WorldPoint(3164, 3500, 0)),
        VARROCK_SMALL_BANK("Varrock Small Bank", new WorldPoint(3185, 3450, 0)),
        EDGEVILLE("Edgeville Bank", new WorldPoint(3095, 3505, 0)),
        DRAYNOR("Draynor Village Bank", new WorldPoint(3095, 3260, 0)),
        ARDOUGNE_EAST("Ardougne East Bank", new WorldPoint(2655, 3290, 0)),
        ARDOUGNE_SOUTH("Ardougne South Bank", new WorldPoint(2655, 3280, 0)),
        YANILLE("Yanille Bank", new WorldPoint(2615, 3108, 0)),
        CAMELOT("Camelot Bank", new WorldPoint(2725, 3505, 0)),
        CATHERBY("Catherby Bank", new WorldPoint(2810, 3455, 0)),
        FALADOR_EAST("Falador East Bank", new WorldPoint(3015, 3355, 0));

        private final String name;
        private final WorldPoint firemakingSpot;

        FiremakingSpots(String name, WorldPoint firemakingSpot) {
            this.name = name;
            this.firemakingSpot = firemakingSpot;
        }

        public String getName() { return name; }
        public WorldPoint getFiremakingSpot() { return firemakingSpot; }
    }

    @Override
    public void applySettings(SkillRuntimeSettings settings) {
        if (settings != null) {
            enabled = settings.isEnabled();
            logType = settings.getMode() != null ? settings.getMode() : "AUTO";
        }
    }

    @Override
    public void execute() {
        if (!enabled) {
            Microbot.status = "Firemaking: disabled";
            return;
        }

        if (!Microbot.isLoggedIn()) return;

        // Initialize player location on first run (like AutoWoodcuttingScript)
        if (initialPlayerLocation == null) {
            initialPlayerLocation = Rs2Player.getWorldLocation();
        }

        // Get current log type to use
        String currentLogName = determineCurrentLogName();
        if (currentLogName == null) {
            Microbot.status = "Firemaking: no suitable logs for level " + Microbot.getClient().getRealSkillLevel(Skill.FIREMAKING);
            return;
        }

        // Check if we have supplies (tinderbox + logs) - if so, go firemake
        if (Rs2Inventory.hasItem(TINDERBOX) && Rs2Inventory.hasItem(currentLogName)) {
            // Close bank if open
            if (Rs2Bank.isOpen()) {
                Rs2Bank.closeBank();
                sleep(500, 1200);
            }

            // Walk to firemaking spot if not there
            if (!isAtFiremakingSpot()) {
                walkToFiremakingSpot();
                return;
            }

            // Do firemaking
            attemptFiremaking(currentLogName);
            return;
        }

        // Need supplies - use AutoWoodcuttingScript firemaking supply pattern
        handleFiremakingSupplies(currentLogName);
    }

    /**
     * Supply handling - walks to nearest bank when supplies are needed
     */
    private void handleFiremakingSupplies(String logName) {
        // First, walk to nearest bank if we're not at one
        if (!Rs2Bank.isOpen()) {
            walkToNearestBank();
            return;
        }

        if (!Rs2Inventory.hasItem(TINDERBOX)) {
            Rs2Bank.openBank();
            sleepUntil(Rs2Bank::isOpen, 20000);
            if (Rs2Bank.isOpen()) {
                Rs2Bank.withdrawItem(true, "Tinderbox");
                sleep(300, 600);
            }
        }

        if (!Rs2Inventory.hasItem(logName)) {
            Microbot.status = "Firemaking: getting " + logName;
            Rs2Bank.openBank();
            sleepUntil(Rs2Bank::isOpen, 20000);
            if (Rs2Bank.isOpen()) {
                Rs2Bank.withdrawAll(logName);
                Rs2Bank.closeBank();
                sleep(500, 1200);
            }
        }

        // After getting supplies, walk to firemaking spot near the bank
        if (Rs2Inventory.hasItem(TINDERBOX) && Rs2Inventory.hasItem(logName)) {
            if (!isAtFiremakingSpot()) {
                walkToFiremakingSpot();
            }
        }
    }

    /**
     * Walk to the nearest bank based on current player location
     */
    private void walkToNearestBank() {
        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        if (playerLocation == null) {
            Microbot.status = "Firemaking: player location not found";
            return;
        }

        // Find nearest bank location
        BankLocation nearestBank = null;
        int shortestDistance = Integer.MAX_VALUE;

        for (BankLocation bank : BankLocation.values()) {
            if (bank.getWorldPoint() != null) {
                int distance = playerLocation.distanceTo(bank.getWorldPoint());
                if (distance < shortestDistance) {
                    shortestDistance = distance;
                    nearestBank = bank;
                }
            }
        }

        if (nearestBank != null) {
            Microbot.status = "Firemaking: walking to " + nearestBank.name() + " bank (distance: " + shortestDistance + ")";
            Rs2Walker.walkTo(nearestBank.getWorldPoint());
            sleepUntil(Rs2Bank::isOpen, 20000);
        }
    }

    /**
     * Determine current log type based on AllInOneConfig.fmMode() and level
     */
    private String determineCurrentLogName() {
        int firemakingLevel = Microbot.getClient().getRealSkillLevel(Skill.FIREMAKING);

        // If specific log type configured (not AUTO), use that if level allows
        if (!"AUTO".equals(logType)) {
            try {
                // Convert config mode to LogInfo enum (handle different naming)
                String normalizedLogType = normalizeLogType(logType);
                LogInfo configuredLog = LogInfo.valueOf(normalizedLogType);
                if (firemakingLevel >= configuredLog.getLevelRequired()) {
                    return configuredLog.getItemName();
                }
            } catch (IllegalArgumentException e) {
                // Invalid log type, fall back to auto
                Microbot.status = "Firemaking: invalid log type '" + logType + "', using AUTO";
            }
        }

        // AUTO mode - find highest level logs we can use
        LogInfo bestLog = null;
        for (LogInfo log : LogInfo.values()) {
            if (firemakingLevel >= log.getLevelRequired()) {
                bestLog = log;
            }
        }

        return bestLog != null ? bestLog.getItemName() : null;
    }

    /**
     * Normalize log type from config to match LogInfo enum
     */
    private String normalizeLogType(String configLogType) {
        if (configLogType == null) return "AUTO";

        // Handle different naming conventions between config and LogInfo
        switch (configLogType.toUpperCase()) {
            case "LOGS":
            case "REGULAR":
            case "NORMAL":
                return "NORMAL";
            case "OAK":
                return "OAK";
            case "WILLOW":
                return "WILLOW";
            case "TEAK":
                return "TEAK";
            case "MAPLE":
                return "MAPLE";
            case "MAHOGANY":
                return "MAHOGANY";
            case "YEW":
                return "YEW";
            case "MAGIC":
                return "MAGIC";
            case "REDWOOD":
                return "REDWOOD";
            default:
                return "AUTO";
        }
    }

    /**
     * Check if we're at a good firemaking spot
     */
    private boolean isAtFiremakingSpot() {
        if (firemakingSpot == null) {
            return false;
        }
        return Rs2Player.getWorldLocation().distanceTo(firemakingSpot) <= 8;
    }

    /**
     * Walk to nearest firemaking spot based on player location
     */
    private void walkToFiremakingSpot() {
        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        if (playerLocation == null) {
            Microbot.status = "Firemaking: player location not found";
            return;
        }

        // Find nearest firemaking spot from our predefined locations
        FiremakingSpots nearestSpot = null;
        int shortestDistance = Integer.MAX_VALUE;

        for (FiremakingSpots spot : FiremakingSpots.values()) {
            int distance = playerLocation.distanceTo(spot.getFiremakingSpot());
            if (distance < shortestDistance) {
                shortestDistance = distance;
                nearestSpot = spot;
            }
        }

        if (nearestSpot == null) {
            Microbot.status = "Firemaking: no suitable firemaking spot found";
            return;
        }

        // Set the firemaking spot with slight randomization to avoid exact same tile
        firemakingSpot = new WorldPoint(
            nearestSpot.getFiremakingSpot().getX() + ThreadLocalRandom.current().nextInt(-1, 2),
            nearestSpot.getFiremakingSpot().getY() + ThreadLocalRandom.current().nextInt(-1, 2),
            nearestSpot.getFiremakingSpot().getPlane()
        );

        // Walk to the chosen firemaking spot
        if (Rs2Walker.walkTo(firemakingSpot)) {
            Microbot.status = "Firemaking: walking to " + nearestSpot.getName() + " (distance: " + shortestDistance + ")";
        }

        // Wait until we arrive at the firemaking spot
        sleepUntil(() -> Rs2Player.getWorldLocation().distanceTo(firemakingSpot) <= 8, 15000);
    }

    /**
     * Attempt to light fires (based on AutoWoodcuttingScript patterns)
     */
    private void attemptFiremaking(String logName) {
        long now = System.currentTimeMillis();
        if (now - lastAttempt < ATTEMPT_COOLDOWN) return;
        if (Rs2Player.isAnimating() || Rs2Player.isMoving()) return;

        if (!Rs2Inventory.hasItem(TINDERBOX) || !Rs2Inventory.hasItem(logName)) {
            Microbot.status = "Firemaking: missing tinderbox or logs - returning to bank";
            firemakingSpot = null; // Reset spot so we recalculate nearest location
            return;
        }

        // Use tinderbox on logs (Rs2Inventory.combine pattern from microbot)
        if (Rs2Inventory.combine("Tinderbox", logName)) {
            Microbot.status = "Firemaking: lighting " + logName + " (" + Rs2Inventory.count(logName) + " remaining)";
            lastAttempt = now;

            // Wait for firemaking animation to complete
            // Note: The game automatically moves the player east after lighting a fire
            sleepUntil(() -> !Rs2Player.isAnimating(), 5000);
        }
    }

    /**
     * Sleep utility methods from microbot patterns
     */
    private void sleep(int min, int max) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(min, max));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void sleepUntil(java.util.function.BooleanSupplier condition, long timeout) {
        long start = System.currentTimeMillis();
        while (!condition.getAsBoolean() && System.currentTimeMillis() - start < timeout) {
            sleep(50, 100);
        }
    }

    // Debug getters
    public boolean isEnabled() { return enabled; }
    public String getLogType() { return logType; }
    public String getCurrentLogName() { return determineCurrentLogName(); }
}
