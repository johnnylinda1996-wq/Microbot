package net.runelite.client.plugins.microbot.aiobot.skills;

import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.aiobot.settings.SkillRuntimeSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.*;

public class FiremakingSkillHandler implements SkillHandler {
    private boolean enabled = true;
    private String configuredLogType = "AUTO"; // AUTO, NORMAL, OAK, WILLOW, MAPLE, YEW, MAGIC, etc.
    private String configuredMethod = "REGULAR"; // REGULAR, CAMPFIRE

    private long lastAttempt = 0L;
    private static final long ATTEMPT_COOLDOWN = 1200L;
    private WorldPoint currentFireLocation = null;
    private boolean waitingForFireSuccess = false;

    // Burning animation IDs
    private static final List<Integer> BURNING_ANIMATION_IDS = Arrays.asList(
            733, // Tinderbox on logs
            6700, // Lighting fire
            6702  // Another burning animation
    );

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

    // Firemaking locations
    private static final List<FiremakingLocation> FIREMAKING_LOCATIONS = Arrays.asList(
            new FiremakingLocation("Varrock West Bank",
                new WorldPoint(3183, 3436, 0),
                new WorldPoint(3183, 3395, 0),
                BankLocation.VARROCK_WEST),
            new FiremakingLocation("Grand Exchange",
                new WorldPoint(3164, 3486, 0),
                new WorldPoint(3164, 3450, 0),
                BankLocation.GRAND_EXCHANGE),
            new FiremakingLocation("Falador East Bank",
                new WorldPoint(3013, 3356, 0),
                new WorldPoint(3013, 3320, 0),
                BankLocation.FALADOR_EAST),
            new FiremakingLocation("Seers Village Bank",
                new WorldPoint(2727, 3493, 0),
                new WorldPoint(2727, 3457, 0),
                BankLocation.CATHERBY),
            new FiremakingLocation("Edgeville Bank",
                new WorldPoint(3094, 3499, 0),
                new WorldPoint(3094, 3463, 0),
                BankLocation.EDGEVILLE),
            new FiremakingLocation("Draynor Village Bank",
                new WorldPoint(3093, 3246, 0),
                new WorldPoint(3093, 3210, 0),
                BankLocation.DRAYNOR_VILLAGE),
            new FiremakingLocation("Lumbridge Top Bank",
                new WorldPoint(3207, 3220, 0),
                new WorldPoint(3207, 3184, 0),
                BankLocation.LUMBRIDGE_TOP),
            new FiremakingLocation("Al Kharid Bank",
                new WorldPoint(3270, 3169, 0),
                new WorldPoint(3270, 3133, 0),
                BankLocation.AL_KHARID),
            new FiremakingLocation("Catherby Bank",
                new WorldPoint(2808, 3441, 0),
                new WorldPoint(2808, 3405, 0),
                BankLocation.CATHERBY),
            new FiremakingLocation("Yanille Bank",
                new WorldPoint(2613, 3094, 0),
                new WorldPoint(2613, 3058, 0),
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
                    configuredMethod = "CAMPFIRE";
                    configuredLogType = "AUTO"; // Campfire uses any available logs
                } else if (modeString.equals("REGULAR")) {
                    // Legacy support: REGULAR is treated as AUTO regular firemaking
                    configuredMethod = "REGULAR";
                    configuredLogType = "AUTO";
                } else {
                    configuredMethod = "REGULAR";
                    configuredLogType = modeString; // Use the selected log type (AUTO, NORMAL, OAK, etc.)
                }
            }

            // Bot always banks for logs automatically - no user configuration needed
            // mode = Mode.BANK;

            // Legacy support for flags (if any old code still uses them)
            if (settings.getFlags() != null) {
                for (String flag : settings.getFlags()) {
                    if (flag.startsWith("LOG_TYPE:")) {
                        configuredLogType = flag.substring("LOG_TYPE:".length());
                    }
                }
            }
        }
    }

    private void updateCurrentLogType() {
        int level = Microbot.getClient().getRealSkillLevel(Skill.FIREMAKING);
        LogType best = null;

        for (LogType logType : LOG_TYPES) {
            if (level >= logType.minLevel) {
                // If user has configured a specific log type, use that
                if (!"AUTO".equals(configuredLogType)) {
                    if (configuredLogType.equals(logType.name)) {
                        best = logType;
                        break;
                    }
                } else {
                    // Auto mode: use highest level logs available
                    if (best == null || logType.minLevel > best.minLevel) {
                        best = logType;
                    }
                }
            }
        }
        currentLogType = best;
    }

    private void updateCurrentLocation() {
        if ("CAMPFIRE".equals(configuredMethod)) {
            updateCurrentCampfireLocation();
            return;
        }

        FiremakingLocation best = null;
        WorldPoint playerLocation = Microbot.getClient().getLocalPlayer().getWorldLocation();

        for (FiremakingLocation location : FIREMAKING_LOCATIONS) {
            if (best == null || (location.bankLocation != null &&
                    Rs2Walker.getDistanceBetween(playerLocation, location.bankLocation.getWorldPoint()) <
                            Rs2Walker.getDistanceBetween(playerLocation, best.bankLocation.getWorldPoint()))) {
                best = location;
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

        if ("CAMPFIRE".equals(configuredMethod) && currentCampfireLocation == null) {
            Microbot.status = "Firemaking: no campfire location available";
            return;
        } else if (!"CAMPFIRE".equals(configuredMethod) && currentLocation == null) {
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

        if ("CAMPFIRE".equals(configuredMethod)) {
            handleCampfireMethod();
        } else {
            handleRegularMethod();
        }
    }

    private boolean hasRequiredTools() {
        return Rs2Inventory.hasItem("Tinderbox") && Rs2Inventory.hasItem(currentLogType.itemName);
    }

    private void obtainTools() {
        WorldPoint bankLocation = null;
        if ("CAMPFIRE".equals(configuredMethod) && currentCampfireLocation != null) {
            bankLocation = currentCampfireLocation.bankLocation.getWorldPoint();
        } else if (currentLocation != null) {
            bankLocation = currentLocation.bankLocation.getWorldPoint();
        }

        if (bankLocation == null) {
            Microbot.status = "Firemaking: no bank location available";
            return;
        }

        if (Rs2Player.getWorldLocation().distanceTo(bankLocation) > 10) {
            if (Rs2Walker.walkTo(bankLocation, 2)) {
                Microbot.status = "Firemaking: walking to bank";
            }
            return;
        }

        if (!Rs2Bank.isOpen()) {
            if (Rs2Bank.openBank()) {
                Microbot.status = "Firemaking: opening bank";
            }
            return;
        }

        if (!Rs2Inventory.hasItem("Tinderbox")) {
            Rs2Bank.withdrawOne("Tinderbox");
            Microbot.status = "Firemaking: withdrawing tinderbox";
            return;
        }

        if (!Rs2Inventory.hasItem(currentLogType.itemName)) {
            Rs2Bank.withdrawAll(currentLogType.itemName);
            Microbot.status = "Firemaking: withdrawing " + currentLogType.itemName;
            return;
        }

        Rs2Bank.closeBank();
    }

    private void bankItems() {
        WorldPoint bankLocation = null;
        if ("CAMPFIRE".equals(configuredMethod) && currentCampfireLocation != null) {
            bankLocation = currentCampfireLocation.bankLocation.getWorldPoint();
        } else if (currentLocation != null) {
            bankLocation = currentLocation.bankLocation.getWorldPoint();
        }

        if (bankLocation == null) {
            Microbot.status = "Firemaking: no bank location available";
            return;
        }

        if (Rs2Player.getWorldLocation().distanceTo(bankLocation) > 10) {
            if (Rs2Walker.walkTo(bankLocation, 2)) {
                Microbot.status = "Firemaking: walking to bank";
            }
            return;
        }

        if (!Rs2Bank.isOpen()) {
            if (Rs2Bank.openBank()) {
                Microbot.status = "Firemaking: opening bank";
            }
            return;
        }

        Rs2Bank.depositAllExcept("Tinderbox", currentLogType.itemName);

        if (!Rs2Inventory.hasItem(currentLogType.itemName)) {
            Rs2Bank.withdrawAll(currentLogType.itemName);
        }

        Rs2Bank.closeBank();
        Microbot.status = "Firemaking: banked items";
    }

    private void dropItems() {
        Rs2Inventory.dropAllExcept("Tinderbox", currentLogType.itemName);
        Microbot.status = "Firemaking: dropped items";
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

    private void attemptCampfireTraining() {
        long now = System.currentTimeMillis();
        if (now - lastAttempt < ATTEMPT_COOLDOWN) return;

        // Look for a campfire
        for (int campfireId : currentCampfireLocation.campfireIds) {
            var campfire = Rs2GameObject.findObjectById(campfireId);
            if (campfire != null) {
                if (Rs2GameObject.interact(campfire, "Cook")) {
                    Microbot.status = "Firemaking: using campfire";
                    lastAttempt = now;
                    return;
                }
            }
        }

        // If no campfire interaction, try adding logs
        if (Rs2Inventory.hasItem(currentLogType.itemName)) {
            Rs2Inventory.use(currentLogType.itemName);
            // Then click on a nearby campfire or ground
            Microbot.status = "Firemaking: training at GE campfire with " + currentLogType.name.toLowerCase() + " logs";
            lastAttempt = now;
            return;
        }

        Microbot.status = "Firemaking: no logs for campfire training";
        lastAttempt = now;
    }

    private void attemptRegularFiremaking() {
        long now = System.currentTimeMillis();
        if (now - lastAttempt < ATTEMPT_COOLDOWN) return;

        // Check if we're currently in a burning animation
        if (waitingForFireSuccess) {
            if (!Rs2Player.isAnimating() || !BURNING_ANIMATION_IDS.contains(Rs2Player.getAnimation())) {
                // Animation finished, move to next position
                waitingForFireSuccess = false;
                moveToNextFirePosition();
            }
            Microbot.status = "Firemaking: lighting fire with " + currentLogType.name.toLowerCase() + " logs";
            return;
        }

        // Try to light a fire at current position
        if (Rs2Inventory.hasItem(currentLogType.itemName)) {
            if (Rs2Inventory.combine("Tinderbox", currentLogType.itemName)) {
                waitingForFireSuccess = true;
                Microbot.status = "Firemaking: attempting to light " + currentLogType.name.toLowerCase() + " logs";
                lastAttempt = now;
                return;
            }
        }

        Microbot.status = "Firemaking: no logs available";
        lastAttempt = now;
    }

    private void moveToNextFirePosition() {
        if (currentLocation == null) return;

        WorldPoint current = Rs2Player.getWorldLocation();
        WorldPoint start = currentLocation.startLocation;
        WorldPoint end = currentLocation.endLocation;

        // Determine direction (east-west or north-south)
        if (start.getX() != end.getX()) {
            // East-west movement
            int direction = start.getX() < end.getX() ? 1 : -1;
            currentFireLocation = new WorldPoint(current.getX() + direction, current.getY(), current.getPlane());
        } else {
            // North-south movement
            int direction = start.getY() < end.getY() ? 1 : -1;
            currentFireLocation = new WorldPoint(current.getX(), current.getY() + direction, current.getPlane());
        }

        // Check if we've gone too far
        if (currentFireLocation.distanceTo(end) > 27) {
            currentFireLocation = start; // Reset to beginning
        }
    }

    // Debug getters
    public boolean isEnabled() { return enabled; }
    public String getMode() { return "BANK"; } // Always BANK mode
    public String getCurrentLogType() { return currentLogType != null ? currentLogType.name : "None"; }
    public String getCurrentMethod() { return configuredMethod; }
}
