package net.runelite.client.plugins.microbot.jpnl.accountbuilder.skills;

import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.settings.SkillRuntimeSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class WoodcuttingSkillHandler implements SkillHandler {
    private boolean enabled = true;
    private Mode mode = Mode.POWERDROP;
    private String configuredTreeType = "AUTO"; // AUTO, TREE, OAK, WILLOW, MAPLE, YEW, MAGIC

    private long lastAttempt = 0L;
    private static final long ATTEMPT_COOLDOWN = 900L;

    // Enums
    private enum Mode { POWERDROP, BANK }

    // Tree type data structure
    private static class TreeType {
        String name;
        String logName;
        List<Integer> treeIds;
        int minLevel;

        public TreeType(String name, String logName, List<Integer> treeIds, int minLevel) {
            this.name = name;
            this.logName = logName;
            this.treeIds = treeIds;
            this.minLevel = minLevel;
        }
    }

    // Woodcutting location data structure
    private static class WoodcuttingLocation {
        String name;
        TreeType treeType;
        WorldPoint location;
        BankLocation bankLocation;
        List<Integer> treeIds;

        public WoodcuttingLocation(String name, TreeType treeType, WorldPoint location, BankLocation bankLocation, List<Integer> treeIds) {
            this.name = name;
            this.treeType = treeType;
            this.location = location;
            this.bankLocation = bankLocation;
            this.treeIds = treeIds;
        }
    }

    // Tree types with levels and IDs
    private static final List<TreeType> TREE_TYPES = Arrays.asList(
            new TreeType("TREE", "Logs", Arrays.asList(1276, 1277, 1278, 1279, 1280, 1330, 1331, 1332, 1365, 1383, 1384), 1),
            new TreeType("OAK", "Oak logs", Arrays.asList(10820, 10821, 10822, 10823), 15),
            new TreeType("WILLOW", "Willow logs", Arrays.asList(10819, 10831, 10832, 10833), 30),
            new TreeType("TEAK", "Teak logs", Arrays.asList(15062, 15063, 15064, 15065), 35),
            new TreeType("MAPLE", "Maple logs", Arrays.asList(10832, 10833, 4674), 45),
            new TreeType("MAHOGANY", "Mahogany logs", Arrays.asList(15066, 15067, 15068, 15069), 50),
            new TreeType("YEW", "Yew logs", Arrays.asList(10822, 10823), 60),
            new TreeType("MAGIC", "Magic logs", Collections.singletonList(10834), 75),
            new TreeType("REDWOOD", "Redwood logs", Arrays.asList(29670, 29671), 90)
    );

    // Woodcutting locations with tree types and bank associations
    private static final List<WoodcuttingLocation> WOODCUTTING_LOCATIONS = Arrays.asList(
            // Regular trees
            new WoodcuttingLocation("Draynor Village Trees",
                TREE_TYPES.get(0), new WorldPoint(3086, 3232, 0), BankLocation.DRAYNOR_VILLAGE,
                Arrays.asList(1276, 1277, 1278, 1279, 1280)),
            new WoodcuttingLocation("Lumbridge Trees",
                TREE_TYPES.get(0), new WorldPoint(3238, 3241, 0), BankLocation.LUMBRIDGE_TOP,
                Arrays.asList(1276, 1277, 1278, 1279, 1280)),
            new WoodcuttingLocation("Al Kharid Trees",
                TREE_TYPES.get(0), new WorldPoint(3275, 3144, 0), BankLocation.AL_KHARID,
                Arrays.asList(1276, 1277, 1278, 1279, 1280)),

            // Oak trees
            new WoodcuttingLocation("Barbarian Village Oaks",
                TREE_TYPES.get(1), new WorldPoint(3105, 3431, 0), BankLocation.EDGEVILLE,
                Arrays.asList(10820, 10821, 10822, 10823)),
            new WoodcuttingLocation("Lumbridge Oaks",
                TREE_TYPES.get(1), new WorldPoint(3238, 3251, 0), BankLocation.LUMBRIDGE_TOP,
                Arrays.asList(10820, 10821, 10822, 10823)),
            new WoodcuttingLocation("Varrock West Oaks",
                TREE_TYPES.get(1), new WorldPoint(3158, 3408, 0), BankLocation.VARROCK_WEST,
                Arrays.asList(10820, 10821, 10822, 10823)),

            // Willow trees
            new WoodcuttingLocation("Draynor Village Willows",
                TREE_TYPES.get(2), new WorldPoint(3088, 3234, 0), BankLocation.DRAYNOR_VILLAGE,
                Arrays.asList(10819, 10831, 10832, 10833)),
            new WoodcuttingLocation("Port Sarim Willows",
                TREE_TYPES.get(2), new WorldPoint(3060, 3256, 0), BankLocation.DRAYNOR_VILLAGE,
                Arrays.asList(10819, 10831, 10832, 10833)),
            new WoodcuttingLocation("Catherby Willows",
                TREE_TYPES.get(2), new WorldPoint(2808, 3437, 0), BankLocation.CATHERBY,
                Arrays.asList(10819, 10831, 10832, 10833)),

            // Maple trees
            new WoodcuttingLocation("Seers Village Maples",
                TREE_TYPES.get(4), new WorldPoint(2732, 3503, 0), BankLocation.CATHERBY,
                Arrays.asList(10832, 10833, 4674)),

            // Yew trees
            new WoodcuttingLocation("Edgeville Yews",
                TREE_TYPES.get(6), new WorldPoint(3087, 3481, 0), BankLocation.EDGEVILLE,
                Arrays.asList(10822, 10823)),
            new WoodcuttingLocation("Falador Yews",
                TREE_TYPES.get(6), new WorldPoint(3009, 3312, 0), BankLocation.FALADOR_EAST,
                Arrays.asList(10822, 10823)),
            new WoodcuttingLocation("Catherby Yews",
                TREE_TYPES.get(6), new WorldPoint(2754, 3404, 0), BankLocation.CATHERBY,
                Arrays.asList(10822, 10823)),

            // Magic trees
            new WoodcuttingLocation("Sorcerer's Tower Magic",
                TREE_TYPES.get(7), new WorldPoint(2705, 3397, 0), BankLocation.CATHERBY,
                Collections.singletonList(10834)),
            new WoodcuttingLocation("Tree Gnome Stronghold Magic",
                TREE_TYPES.get(7), new WorldPoint(2692, 3425, 0), BankLocation.TREE_GNOME_STRONGHOLD_NIEVE,
                Collections.singletonList(10834))
    );

    private TreeType currentTreeType = null;
    private WoodcuttingLocation currentLocation = null;

    @Override
    public void applySettings(SkillRuntimeSettings settings) {
        if (settings != null) {
            enabled = settings.isEnabled();

            // Use the mode string directly as the mode type
            String modeString = settings.getMode();
            if (modeString != null) {
                if (modeString.equals("BANK")) {
                    mode = Mode.BANK;
                } else if (modeString.equals("POWERDROP")) {
                    mode = Mode.POWERDROP;
                } else {
                    mode = Mode.POWERDROP; // default
                }
            }

            // Extract tree type from flags
            configuredTreeType = "AUTO"; // Default
            if (settings.getFlags() != null) {
                for (String flag : settings.getFlags()) {
                    if (flag.startsWith("WC_TREE_TYPE:")) {
                        configuredTreeType = flag.substring("WC_TREE_TYPE:".length());
                        break;
                    }
                }
            }
        }
    }

    private void updateCurrentTreeType() {
        int level = Microbot.getClient().getRealSkillLevel(Skill.WOODCUTTING);
        TreeType best = null;

        // If user has configured a specific tree type, use that
        if (!"AUTO".equals(configuredTreeType)) {
            for (TreeType logType : TREE_TYPES) {
                if (level >= logType.minLevel && configuredTreeType.equals(logType.name)) {
                    best = logType;
                    break;
                }
            }
        } else {
            // Auto mode: find the highest level logs that are actually available in bank
            // First try to find the highest level logs we can use
            for (int i = TREE_TYPES.size() - 1; i >= 0; i--) {
                TreeType treeType = TREE_TYPES.get(i);
                if (level >= treeType.minLevel) {
                    // Check if we have these logs in inventory already, or can get them from bank
                    if (Rs2Inventory.contains(treeType.logName) ||
                        (Rs2Bank.isOpen() && Rs2Bank.hasItem(treeType.logName)) ||
                        (!Rs2Bank.isOpen() && canGetFromBank(treeType.logName))) {
                        best = treeType;
                        break;
                    }
                }
            }

            // If no high-level logs found, fallback to any logs we can actually use
            if (best == null) {
                for (TreeType treeType : TREE_TYPES) {
                    if (level >= treeType.minLevel) {
                        if (Rs2Inventory.contains(treeType.logName) ||
                            (Rs2Bank.isOpen() && Rs2Bank.hasItem(treeType.logName)) ||
                            (!Rs2Bank.isOpen() && canGetFromBank(treeType.logName))) {
                            best = treeType;
                            break;
                        }
                    }
                }
            }

            // Final fallback: if still nothing found, just use the highest level we can access
            // This ensures we don't get stuck even if bank check fails
            if (best == null) {
                for (int i = TREE_TYPES.size() - 1; i >= 0; i--) {
                    TreeType treeType = TREE_TYPES.get(i);
                    if (level >= treeType.minLevel) {
                        best = treeType;
                        break;
                    }
                }
            }
        }
        currentTreeType = best;
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
        WoodcuttingLocation best = null;
        WorldPoint playerLocation = Microbot.getClient().getLocalPlayer().getWorldLocation();

        // First try to find a location that matches the current tree type
        for (WoodcuttingLocation location : WOODCUTTING_LOCATIONS) {
            if (currentTreeType != null && location.treeType.name.equals(currentTreeType.name)) {
                if (best == null || (location.bankLocation != null &&
                        Rs2Walker.getDistanceBetween(playerLocation, location.bankLocation.getWorldPoint()) <
                                Rs2Walker.getDistanceBetween(playerLocation, best.bankLocation.getWorldPoint()))) {
                    best = location;
                }
            }
        }

        // If no location found for current tree type, fallback to any location we can access
        if (best == null && currentTreeType != null) {
            int level = Microbot.getClient().getRealSkillLevel(Skill.WOODCUTTING);

            // Try to find any location we can use (starting from our level down)
            for (WoodcuttingLocation location : WOODCUTTING_LOCATIONS) {
                if (level >= location.treeType.minLevel) {
                    if (best == null || (location.bankLocation != null && best.bankLocation != null &&
                            Rs2Walker.getDistanceBetween(playerLocation, location.bankLocation.getWorldPoint()) <
                                    Rs2Walker.getDistanceBetween(playerLocation, best.bankLocation.getWorldPoint()))) {
                        best = location;
                    }
                }
            }

            // If we found a different location, update our tree type to match
            if (best != null && !best.treeType.name.equals(currentTreeType.name)) {
                currentTreeType = best.treeType;
                Microbot.status = "Woodcutting: switching to " + currentTreeType.name + " (location available)";
            }
        }

        currentLocation = best;
    }

    @Override
    public void execute() {
        if (!enabled) {
            Microbot.status = "Woodcutting: disabled";
            return;
        }
        if (!Microbot.isLoggedIn()) return;

        updateCurrentTreeType();
        updateCurrentLocation();

        if (currentTreeType == null) {
            Microbot.status = "Woodcutting: no suitable tree type found for your level";
            return;
        }

        if (currentLocation == null) {
            Microbot.status = "Woodcutting: no woodcutting location available";
            return;
        }

        if (!hasRequiredTools()) {
            Microbot.status = "Woodcutting: missing tools -> bank";
            obtainTools();
            return;
        }

        if (Rs2Inventory.isFull()) {
            if (mode == Mode.BANK) {
                bankLogs();
            } else {
                dropLogs();
            }
            return;
        }

        WorldPoint target = currentLocation.location;
        if (target != null && Rs2Player.getWorldLocation().distanceTo(target) > 15) {
            if (Rs2Walker.walkTo(target, 4)) {
                Microbot.status = "Woodcutting: walking to trees";
            }
            return;
        }

        attemptWoodcut();
    }

    private boolean hasRequiredTools() {
        // Check if player has an axe (either equipped or in inventory)
        // Use more specific checks to avoid false negatives
        return Rs2Equipment.isWearing("axe") ||
               Rs2Equipment.isWearing("hatchet") ||
               Rs2Inventory.hasItem("Bronze axe") ||
               Rs2Inventory.hasItem("Iron axe") ||
               Rs2Inventory.hasItem("Steel axe") ||
               Rs2Inventory.hasItem("Mithril axe") ||
               Rs2Inventory.hasItem("Adamant axe") ||
               Rs2Inventory.hasItem("Rune axe") ||
               Rs2Inventory.hasItem("Dragon axe") ||
               Rs2Inventory.hasItem("Bronze hatchet") ||
               Rs2Inventory.hasItem("Iron hatchet") ||
               Rs2Inventory.hasItem("Steel hatchet") ||
               Rs2Inventory.hasItem("Mithril hatchet") ||
               Rs2Inventory.hasItem("Adamant hatchet") ||
               Rs2Inventory.hasItem("Rune hatchet") ||
               Rs2Inventory.hasItem("Dragon hatchet");
    }

    private void obtainTools() {
        if (!openNearestBank()) return;

        // Try to get the best axe available
        String[] axes = {"Dragon axe", "Rune axe", "Adamant axe", "Mithril axe", "Steel axe", "Iron axe", "Bronze axe",
                         "Dragon hatchet", "Rune hatchet", "Adamant hatchet", "Mithril hatchet", "Steel hatchet", "Iron hatchet", "Bronze hatchet"};
        boolean foundAxe = false;

        // Double check if we already have an axe before withdrawing
        if (hasRequiredTools()) {
            Microbot.status = "Woodcutting: already have axe/hatchet";
            Rs2Bank.closeBank();
            return;
        }

        // Try to withdraw the best available axe
        for (String axe : axes) {
            if (Rs2Bank.hasItem(axe)) {
                Rs2Bank.withdrawOne(axe);
                Microbot.status = "Woodcutting: withdrawing " + axe;
                foundAxe = true;
                break;
            }
        }

        if (!foundAxe) {
            Microbot.status = "Woodcutting: no axe/hatchet found in bank";
        }

        Rs2Bank.closeBank();
    }

    private void attemptWoodcut() {
        long now = System.currentTimeMillis();
        if (now - lastAttempt < ATTEMPT_COOLDOWN) return;
        if (Rs2Player.isAnimating() || Rs2Player.isMoving()) return;

        if (currentLocation == null) return;

        // Try to interact with trees at current location
        for (int treeId : currentLocation.treeIds) {
            if (Rs2GameObject.interact(treeId, "Chop down")) {
                Microbot.status = "Woodcutting: chopping " + currentTreeType.name;
                lastAttempt = now;
                return;
            }
        }

        Microbot.status = "Woodcutting: no trees found (retry)";
        lastAttempt = now;
    }

    private void bankLogs() {
        if (!openNearestBank()) return;

        Set<String> keep = new HashSet<>();
        // Keep axes
        if (Rs2Inventory.contains("axe")) keep.add("axe");
        if (Rs2Inventory.contains("hatchet")) keep.add("hatchet");

        Rs2Bank.depositAllExcept(keep.toArray(new String[0]));
        Rs2Bank.closeBank();
        Microbot.status = "Woodcutting: banked logs";
    }

    private void dropLogs() {
        final AtomicInteger dropped = new AtomicInteger(0);
        Rs2Inventory.dropAll(model -> {
            String n = model.getName();
            if (n == null) return false;
            String ln = n.toLowerCase();
            // Don't drop axes
            if (ln.contains("axe") || ln.contains("hatchet")) return false;
            // Drop logs
            if (isLog(ln)) {
                dropped.incrementAndGet();
                return true;
            }
            return false;
        });
        Microbot.status = "Woodcutting: dropped " + dropped.get() + " logs";
    }

    private boolean isLog(String ln) {
        return ln.contains("logs") || ln.equals("logs") ||
               ln.contains("oak") || ln.contains("willow") || ln.contains("maple") ||
               ln.contains("yew") || ln.contains("magic") || ln.contains("teak") ||
               ln.contains("mahogany") || ln.contains("redwood");
    }

    private boolean openNearestBank() {
        if (currentLocation != null && currentLocation.bankLocation != null) {
            int maxAttempts = 3;
            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                boolean ok = Rs2Bank.walkToBankAndUseBank(currentLocation.bankLocation) && Rs2Bank.isOpen();
                if (ok) {
                    Microbot.status = "Woodcutting: bank opened";
                    return true;
                }
                Microbot.status = "Woodcutting: opening bank (retry " + (attempt + 1) + ")...";
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
            Microbot.status = "Woodcutting: failed to open bank!";
            return false;
        }

        // Fallback to nearest bank
        BankLocation nearestBank = BankLocation.DRAYNOR_VILLAGE; // Default fallback
        int maxAttempts = 3;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            boolean ok = Rs2Bank.walkToBankAndUseBank(nearestBank) && Rs2Bank.isOpen();
            if (ok) {
                Microbot.status = "Woodcutting: bank opened";
                return true;
            }
            Microbot.status = "Woodcutting: opening bank (retry " + (attempt + 1) + ")...";
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        }
        Microbot.status = "Woodcutting: failed to open bank!";
        return false;
    }

    // Debug getters
    public boolean isEnabled() { return enabled; }
    public String getMode() { return mode.name(); }
    public String getConfiguredTreeType() { return configuredTreeType; }
    public String getCurrentTreeType() { return currentTreeType != null ? currentTreeType.name : "None"; }
}