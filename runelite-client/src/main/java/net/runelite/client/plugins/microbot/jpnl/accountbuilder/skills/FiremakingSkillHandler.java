package net.runelite.client.plugins.microbot.jpnl.accountbuilder.skills;

import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.settings.SkillRuntimeSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.*;

public class FiremakingSkillHandler implements SkillHandler {
    private boolean enabled = true;
    private Mode mode = Mode.REGULAR;
    private String configuredLogType = "AUTO"; // AUTO, NORMAL, OAK, WILLOW, MAPLE, YEW, MAGIC, etc.

    private long lastAttempt = 0L;
    private static final long ATTEMPT_COOLDOWN = 1200L;
    private WorldPoint currentFireLocation = null;
    private boolean waitingForFireSuccess = false;

    // Enums
    private enum Mode { REGULAR, CAMPFIRE }

    // Log type data structure
    private static class LogType {
        String name;
        String itemName;
        int minLevel;

        public LogType(String name, String itemName, int minLevel) {
            this.name = name;
            this.itemName = itemName;
            this.minLevel = minLevel;
        }
    }

    // Firemaking location data structure
    private static class FiremakingLocation {
        String name;
        WorldPoint startLocation;
        WorldPoint endLocation;
        BankLocation bankLocation;

        public FiremakingLocation(String name, WorldPoint startLocation, WorldPoint endLocation, BankLocation bankLocation) {
            this.name = name;
            this.startLocation = startLocation;
            this.endLocation = endLocation;
            this.bankLocation = bankLocation;
        }
    }

    // Campfire location data structure
    private static class CampfireLocation {
        String name;
        WorldPoint location;
        BankLocation bankLocation;
        List<Integer> campfireIds;

        public CampfireLocation(String name, WorldPoint location, BankLocation bankLocation, List<Integer> campfireIds) {
            this.name = name;
            this.location = location;
            this.bankLocation = bankLocation;
            this.campfireIds = campfireIds;
        }
    }

    // Log types with levels
    private static final List<LogType> LOG_TYPES = Arrays.asList(
            new LogType("NORMAL", "Logs", 1),
            new LogType("OAK", "Oak logs", 15),
            new LogType("WILLOW", "Willow logs", 30),
            new LogType("TEAK", "Teak logs", 35),
            new LogType("MAPLE", "Maple logs", 45),
            new LogType("MAHOGANY", "Mahogany logs", 50),
            new LogType("YEW", "Yew logs", 60),
            new LogType("MAGIC", "Magic logs", 75),
            new LogType("REDWOOD", "Redwood logs", 90)
    );

    // Firemaking locations - classic burning spots (east-west lines with 27+ free tiles)
    private static final List<FiremakingLocation> FIREMAKING_LOCATIONS = Arrays.asList(
            new FiremakingLocation("Grand Exchange",
                new WorldPoint(3162, 3485, 0),  // Start west of GE bank
                new WorldPoint(3190, 3485, 0),  // End east (>=27 tiles)
                BankLocation.GRAND_EXCHANGE),
            new FiremakingLocation("Varrock West Bank",
                new WorldPoint(3185, 3436, 0),  // Start west of bank
                new WorldPoint(3213, 3436, 0),  // End east (>=27 tiles)
                BankLocation.VARROCK_WEST),
            new FiremakingLocation("Varrock East Bank",
                new WorldPoint(3245, 3421, 0),  // Start west of bank (one tile west of bank entrance line)
                new WorldPoint(3273, 3421, 0),  // End east
                BankLocation.VARROCK_EAST),
            new FiremakingLocation("Edgeville Bank",
                new WorldPoint(3095, 3490, 0),  // Start west of bank
                new WorldPoint(3123, 3490, 0),  // End east
                BankLocation.EDGEVILLE),
            new FiremakingLocation("Falador East Bank",
                new WorldPoint(3015, 3356, 0),  // Start west of bank
                new WorldPoint(3043, 3356, 0),  // End east
                BankLocation.FALADOR_EAST),
            new FiremakingLocation("Draynor Village Bank",
                new WorldPoint(3095, 3244, 0),  // Start west of bank
                new WorldPoint(3123, 3244, 0),  // End east
                BankLocation.DRAYNOR_VILLAGE),
            new FiremakingLocation("Lumbridge Bank",
                new WorldPoint(3208, 3218, 0),  // Ground level start (south-west of castle)
                new WorldPoint(3236, 3218, 0),  // End east
                BankLocation.LUMBRIDGE_TOP),
            new FiremakingLocation("Al Kharid Bank",
                new WorldPoint(3272, 3169, 0),  // Start west of bank
                new WorldPoint(3300, 3169, 0),  // End east
                BankLocation.AL_KHARID),
            new FiremakingLocation("Catherby Bank",
                new WorldPoint(2810, 3441, 0),  // Start west of bank
                new WorldPoint(2838, 3441, 0),  // End east
                BankLocation.CATHERBY),
            new FiremakingLocation("Seers Village Bank",
                new WorldPoint(2725, 3493, 0),  // Start west of bank
                new WorldPoint(2753, 3493, 0),  // End east
                BankLocation.CAMELOT), // FIX: was incorrectly CATHERBY
            new FiremakingLocation("Yanille Bank",
                new WorldPoint(2615, 3094, 0),  // Start west of bank
                new WorldPoint(2643, 3094, 0),  // End east
                BankLocation.YANILLE)
    );

    // Campfire locations (for alternative training method)
    private static final List<CampfireLocation> CAMPFIRE_LOCATIONS = Arrays.asList(
            new CampfireLocation("Grand Exchange",
                new WorldPoint(3162, 3478, 0),
                BankLocation.GRAND_EXCHANGE,
                Arrays.asList(26185)) // GE campfire object ID
    );

    private LogType currentLogType = null;
    private FiremakingLocation currentLocation = null;
    private CampfireLocation currentCampfireLocation = null;

    @Override
    public void applySettings(SkillRuntimeSettings settings) {
        if (settings != null) {
            enabled = settings.isEnabled();

            // Use the mode string directly as the log type/method
            String modeString = settings.getMode();
            if (modeString != null) {
                if (modeString.equals("CAMPFIRE")) {
                    mode = Mode.CAMPFIRE;
                    configuredLogType = "AUTO"; // Campfire uses any available logs
                } else if (modeString.equals("REGULAR")) {
                    mode = Mode.REGULAR;
                    configuredLogType = "AUTO";
                } else {
                    mode = Mode.REGULAR;
                    configuredLogType = modeString; // Use the selected log type (AUTO, NORMAL, OAK, etc.)
                }
            }

            // Extract log type from flags if provided
            if (settings.getFlags() != null) {
                for (String flag : settings.getFlags()) {
                    if (flag.startsWith("FM_MODE:")) {
                        String flagMode = flag.substring("FM_MODE:".length());
                        if (flagMode.equals("CAMPFIRE")) {
                            mode = Mode.CAMPFIRE;
                            configuredLogType = "AUTO";
                        } else if (Arrays.asList("AUTO", "NORMAL", "OAK", "WILLOW", "TEAK", "MAPLE", "MAHOGANY", "YEW", "MAGIC", "REDWOOD").contains(flagMode)) {
                            mode = Mode.REGULAR;
                            configuredLogType = flagMode;
                        }
                        break;
                    }
                }
            }
        }
    }

    private void updateCurrentLogType() {
        int level = Microbot.getClient().getRealSkillLevel(Skill.FIREMAKING);
        LogType best = null;

        // If user has configured a specific log type, use that
        if (!"AUTO".equals(configuredLogType)) {
            for (LogType logType : LOG_TYPES) {
                if (level >= logType.minLevel && configuredLogType.equals(logType.name)) {
                    best = logType;
                    break;
                }
            }
        } else {
            // Auto mode: find the highest level logs that are actually available in bank
            // First try to find the highest level logs we can use
            for (int i = LOG_TYPES.size() - 1; i >= 0; i--) {
                LogType logType = LOG_TYPES.get(i);
                if (level >= logType.minLevel) {
                    // Check if we have these logs in inventory already, or can get them from bank
                    if (Rs2Inventory.contains(logType.itemName) ||
                        (Rs2Bank.isOpen() && Rs2Bank.hasItem(logType.itemName)) ||
                        (!Rs2Bank.isOpen() && canGetFromBank(logType.itemName))) {
                        best = logType;
                        break;
                    }
                }
            }

            // If no high-level logs found, fallback to any logs we can actually use
            if (best == null) {
                for (LogType logType : LOG_TYPES) {
                    if (level >= logType.minLevel) {
                        if (Rs2Inventory.contains(logType.itemName) ||
                            (Rs2Bank.isOpen() && Rs2Bank.hasItem(logType.itemName)) ||
                            (!Rs2Bank.isOpen() && canGetFromBank(logType.itemName))) {
                            best = logType;
                            break;
                        }
                    }
                }
            }

            // Final fallback: if still nothing found, just use the highest level we can access
            // This ensures we don't get stuck even if bank check fails
            if (best == null) {
                for (int i = LOG_TYPES.size() - 1; i >= 0; i--) {
                    LogType logType = LOG_TYPES.get(i);
                    if (level >= logType.minLevel) {
                        best = logType;
                        break;
                    }
                }
            }
        }
        currentLogType = best;
    }

    private boolean canGetFromBank(String itemName) {
        // Only check bank if we're close to a bank to avoid unnecessary walking
        if (currentLocation != null && currentLocation.bankLocation != null) {
            WorldPoint bankLocation = currentLocation.bankLocation.getWorldPoint();
            if (Rs2Player.getWorldLocation().distanceTo(bankLocation) <= 20) {
                // We're close to bank, try to check if item is available
                boolean wasOpen = Rs2Bank.isOpen();
                if (!wasOpen && Rs2Bank.openBank()) {
                    boolean hasItem = Rs2Bank.hasItem(itemName);
                    if (!wasOpen) Rs2Bank.closeBank(); // Close if we opened it
                    return hasItem;
                }
            }
        }
        return false; // Assume not available if we can't easily check
    }

    private void updateCurrentLocation() {
        if (mode == Mode.CAMPFIRE) {
            updateCurrentCampfireLocation();
            return;
        }

        WorldPoint playerLocation = Microbot.getClient().getLocalPlayer().getWorldLocation();
        // Keep current location if still nearby and valid
        if (currentLocation != null && isValidFiremakingLocation(currentLocation)) {
            if (playerLocation.distanceTo(currentLocation.startLocation) < 80) {
                return; // stay on this line
            }
        }

        FiremakingLocation best = null;
        int bestDist = Integer.MAX_VALUE;
        for (FiremakingLocation location : FIREMAKING_LOCATIONS) {
            if (!isValidFiremakingLocation(location)) continue;
            int dist = playerLocation.distanceTo(location.startLocation);
            if (dist < bestDist) {
                best = location;
                bestDist = dist;
            }
        }
        currentLocation = best;
    }

    private void updateCurrentCampfireLocation() {
        // For now, just use the Grand Exchange campfire
        if (!CAMPFIRE_LOCATIONS.isEmpty()) {
            currentCampfireLocation = CAMPFIRE_LOCATIONS.get(0);
        }
    }

    @Override
    public void execute() {
        if (!enabled) {
            Microbot.status = "Firemaking: disabled";
            return;
        }
        if (!Microbot.isLoggedIn()) return;

        updateCurrentLogType();
        updateCurrentLocation();

        if (currentLogType == null) {
            Microbot.status = "Firemaking: no suitable log type found for your level";
            return;
        }

        if (mode == Mode.CAMPFIRE && currentCampfireLocation == null) {
            Microbot.status = "Firemaking: no campfire location available";
            return;
        } else if (mode == Mode.REGULAR && currentLocation == null) {
            Microbot.status = "Firemaking: no firemaking location available";
            return;
        }

        if (!hasRequiredTools()) {
            Microbot.status = "Firemaking: missing tools -> bank";
            obtainTools();
            return;
        }

        if (Rs2Inventory.isFull() && !Rs2Inventory.hasItem(currentLogType.itemName)) {
            bankItems();
            return;
        }

        if (mode == Mode.CAMPFIRE) {
            handleCampfireMethod();
        } else {
            handleRegularMethod();
        }
    }

    private boolean hasRequiredTools() {
        return Rs2Inventory.hasItem("Tinderbox") && Rs2Inventory.hasItem(currentLogType.itemName);
    }

    private void obtainTools() {
        if (!openNearestBank()) return;

        boolean needsWithdraw = false;

        if (!Rs2Inventory.hasItem("Tinderbox")) {
            if (Rs2Bank.hasItem("Tinderbox")) {
                Rs2Bank.withdrawOne("Tinderbox");
                Microbot.status = "Firemaking: withdrawing tinderbox";
                needsWithdraw = true;
            } else {
                Microbot.status = "Firemaking: no tinderbox found in bank";
                Rs2Bank.closeBank();
                return;
            }
        }

        // Try to get the current log type first
        if (!Rs2Inventory.hasItem(currentLogType.itemName)) {
            if (Rs2Bank.hasItem(currentLogType.itemName)) {
                Rs2Bank.withdrawAll(currentLogType.itemName);
                Microbot.status = "Firemaking: withdrawing " + currentLogType.itemName;
                needsWithdraw = true;
            } else {
                // Current log type not available, try to find any available logs we can use
                LogType fallbackLogType = findAvailableLogType();
                if (fallbackLogType != null && !fallbackLogType.itemName.equals(currentLogType.itemName)) {
                    currentLogType = fallbackLogType; // Update to use available logs
                    Rs2Bank.withdrawAll(currentLogType.itemName);
                    Microbot.status = "Firemaking: falling back to " + currentLogType.itemName;
                    needsWithdraw = true;
                } else {
                    Microbot.status = "Firemaking: no logs found in bank";
                }
            }
        }

        Rs2Bank.closeBank();

        if (!needsWithdraw) {
            Microbot.status = "Firemaking: already have required tools";
        }
    }

    private LogType findAvailableLogType() {
        int level = Microbot.getClient().getRealSkillLevel(Skill.FIREMAKING);

        // Try from highest to lowest level logs that we can actually use
        for (int i = LOG_TYPES.size() - 1; i >= 0; i--) {
            LogType logType = LOG_TYPES.get(i);
            if (level >= logType.minLevel && Rs2Bank.hasItem(logType.itemName)) {
                return logType;
            }
        }

        // If nothing found, try any logs we have regardless of level (emergency fallback)
        for (LogType logType : LOG_TYPES) {
            if (Rs2Bank.hasItem(logType.itemName)) {
                return logType;
            }
        }

        return null; // No logs found at all
    }

    private void bankItems() {
        if (!openNearestBank()) return;

        Set<String> keep = new HashSet<>();
        keep.add("Tinderbox");
        if (Rs2Inventory.hasItem(currentLogType.itemName)) {
            keep.add(currentLogType.itemName);
        }

        Rs2Bank.depositAllExcept(keep.toArray(new String[0]));

        if (!Rs2Inventory.hasItem(currentLogType.itemName) && Rs2Bank.hasItem(currentLogType.itemName)) {
            Rs2Bank.withdrawAll(currentLogType.itemName);
        }

        Rs2Bank.closeBank();
        Microbot.status = "Firemaking: banked items";
    }

    private void handleCampfireMethod() {
        WorldPoint campfireLocation = currentCampfireLocation.location;

        if (Rs2Player.getWorldLocation().distanceTo(campfireLocation) > 10) {
            if (Rs2Walker.walkTo(campfireLocation, 3)) {
                Microbot.status = "Firemaking: walking to campfire";
            }
            return;
        }

        attemptCampfireTraining();
    }

    private void handleRegularMethod() {
        WorldPoint targetLocation = getNextFireLocation();

        if (Rs2Player.getWorldLocation().distanceTo(targetLocation) > 15) {
            if (Rs2Walker.walkTo(targetLocation, 1)) {
                Microbot.status = "Firemaking: walking to fire location";
            }
            return;
        }

        attemptRegularFiremaking();
    }

    private WorldPoint getNextFireLocation() {
        if (currentFireLocation == null) {
            // Start at the beginning of the line
            currentFireLocation = currentLocation.startLocation;
            return currentFireLocation;
        }

        // Check if we've reached the end of the line
        if (currentFireLocation.distanceTo(currentLocation.endLocation) <= 1) {
            // Reset to beginning
            currentFireLocation = currentLocation.startLocation;
            return currentFireLocation;
        }

        // Continue along the line
        return currentFireLocation;
    }

    private void attemptRegularFiremaking() {
        long now = System.currentTimeMillis();
        if (now - lastAttempt < ATTEMPT_COOLDOWN) return;
        if (Rs2Player.isAnimating() || Rs2Player.isMoving()) return;

        if (!Rs2Inventory.hasItem("Tinderbox") || !Rs2Inventory.hasItem(currentLogType.itemName)) {
            Microbot.status = "Firemaking: missing required items";
            return;
        }

        // Use tinderbox on logs
        if (Rs2Inventory.combine("Tinderbox", currentLogType.itemName)) {
            Microbot.status = "Firemaking: lighting " + currentLogType.itemName;
            lastAttempt = now;
            waitingForFireSuccess = true;

            // Move to next location after lighting (east-west movement)
            if (currentLocation != null) {
                if (currentFireLocation.getX() < currentLocation.endLocation.getX()) {
                    // Move one tile east
                    currentFireLocation = new WorldPoint(currentFireLocation.getX() + 1, currentFireLocation.getY(), currentFireLocation.getPlane());
                } else {
                    // Reached end, reset to start
                    currentFireLocation = currentLocation.startLocation;
                }
            }
        }
    }

    private void attemptCampfireTraining() {
        long now = System.currentTimeMillis();
        if (now - lastAttempt < ATTEMPT_COOLDOWN) return;
        if (Rs2Player.isAnimating() || Rs2Player.isMoving()) return;

        if (!Rs2Inventory.hasItem(currentLogType.itemName)) {
            Microbot.status = "Firemaking: no logs to add to campfire";
            return;
        }

        // Find and use the campfire
        for (int campfireId : currentCampfireLocation.campfireIds) {
            if (Rs2GameObject.interact(campfireId, "Add-logs")) {
                Microbot.status = "Firemaking: adding " + currentLogType.itemName + " to campfire";
                lastAttempt = now;
                return;
            }
        }

        Microbot.status = "Firemaking: campfire not found";
    }

    private boolean openNearestBank() {
        BankLocation bankLocation = null;
        if (mode == Mode.CAMPFIRE && currentCampfireLocation != null) {
            bankLocation = currentCampfireLocation.bankLocation;
        } else if (currentLocation != null) {
            bankLocation = currentLocation.bankLocation;
        }

        if (bankLocation == null) {
            Microbot.status = "Firemaking: no bank location available";
            return false;
        }

        int maxAttempts = 3;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            boolean ok = Rs2Bank.walkToBankAndUseBank(bankLocation) && Rs2Bank.isOpen();
            if (ok) {
                Microbot.status = "Firemaking: bank opened";
                return true;
            }
            Microbot.status = "Firemaking: opening bank (retry " + (attempt + 1) + ")...";
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        }
        Microbot.status = "Firemaking: failed to open bank!";
        return false;
    }

    private boolean isValidFiremakingLocation(FiremakingLocation loc) {
        if (loc == null) return false;
        // Ensure east-west straight line same plane
        if (loc.startLocation.getPlane() != loc.endLocation.getPlane()) return false;
        if (loc.startLocation.getY() != loc.endLocation.getY()) return false;
        int length = Math.abs(loc.endLocation.getX() - loc.startLocation.getX());
        if (length < 27) return false; // need at least 27 steps east for full inventory burn (>=28 tiles inclusive)
        // Bank requirements
        if (loc.bankLocation != null && !loc.bankLocation.hasRequirements()) return false;
        // Only perform heavy walkable check if nearby to avoid loading issues when far
        WorldPoint playerLoc = Rs2Player.getWorldLocation();
        if (playerLoc != null && playerLoc.distanceTo(loc.startLocation) < 32) {
            int walkable = 0;
            int total = length + 1;
            int minX = Math.min(loc.startLocation.getX(), loc.endLocation.getX());
            int maxX = Math.max(loc.startLocation.getX(), loc.endLocation.getX());
            for (int x = minX; x <= maxX; x++) {
                WorldPoint p = new WorldPoint(x, loc.startLocation.getY(), loc.startLocation.getPlane());
                if (net.runelite.client.plugins.microbot.util.tile.Rs2Tile.isWalkable(p)) walkable++;
            }
            if (walkable < (int)(total * 0.8)) return false; // allow a few blocked tiles
        }
        return true;
    }

    // Debug getters
    public boolean isEnabled() { return enabled; }
    public String getMode() { return mode.name(); }
    public String getConfiguredLogType() { return configuredLogType; }
    public String getCurrentLogType() { return currentLogType != null ? currentLogType.name : "None"; }
}
