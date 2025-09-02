package net.runelite.client.plugins.microbot.jpnl.accountbuilder.skills;

import net.runelite.api.NPCComposition;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.settings.SkillRuntimeSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FishingSkillHandler implements SkillHandler {
    private boolean enabled = true;
    private Mode mode = Mode.DROP;
    private FishingTier tier = FishingTier.NET_BAIT;
    private String configuredFishingMethod = "AUTO"; // New field to store the configured fishing method

    private long lastAttempt = 0L;
    private static final long ATTEMPT_COOLDOWN = 900L;

    private static final int DRAYNOR_MIN_COMBAT = 25;

    private static final WorldPoint LUMBRIDGE_NET = new WorldPoint(3242, 3154, 0);
    private static final WorldPoint DRAYNOR_NET = new WorldPoint(3086, 3232, 0);

    private static final BankLocation LUMBRIDGE_BANK = BankLocation.LUMBRIDGE_FRONT;
    private static final BankLocation DRAYNOR_BANK = BankLocation.DRAYNOR_VILLAGE;

    // Enums
    private enum Mode { DROP, BANK }
    private enum FishingTier { NET_BAIT, LURE, CAGE_HARPOON }

    // Fishing spot data structure
    private static class FishingSpot {
        String name;
        int minLevel;
        List<String> fishTypes;
        List<Integer> spotIds;
        WorldPoint location;
        BankLocation bankLocation;
        List<String> actions;

        public FishingSpot(String name, int minLevel, List<String> fishTypes, List<Integer> spotIds,
                           WorldPoint location, BankLocation bankLocation, List<String> actions) {
            this.name = name;
            this.minLevel = minLevel;
            this.fishTypes = fishTypes;
            this.spotIds = spotIds;
            this.location = location;
            this.bankLocation = bankLocation;
            this.actions = actions;
        }
    }

    private static final List<FishingSpot> FISHING_SPOTS = Arrays.asList(
            new FishingSpot("Lumbridge", 1, Arrays.asList("shrimp", "anchovy"), Arrays.asList(1526, 1527, 1530, 1528), new WorldPoint(3242, 3154, 0), BankLocation.LUMBRIDGE_TOP, Arrays.asList("Net", "Bait")),
            new FishingSpot("Draynor", 1, Arrays.asList("shrimp", "anchovy"), Arrays.asList(1526, 1527, 1530, 1528), new WorldPoint(3086, 3232, 0), BankLocation.DRAYNOR_VILLAGE, Arrays.asList("Net", "Bait")),
            new FishingSpot("Catherby", 1, Arrays.asList("shrimp", "anchovy", "lobster", "tuna", "swordfish", "shark"), Arrays.asList(1526, 1527, 1510, 1511, 1516, 1518, 1520), new WorldPoint(2853, 3431, 0), BankLocation.CATHERBY, Arrays.asList("Net", "Bait", "Cage", "Harpoon")),
            new FishingSpot("Barbarian Village", 20, Arrays.asList("trout", "salmon"), Arrays.asList(1526, 1527, 1528), new WorldPoint(3104, 3433, 0), BankLocation.EDGEVILLE, Arrays.asList("Lure")),
            new FishingSpot("Shilo Village", 30, Arrays.asList("trout", "salmon"), Arrays.asList(1526, 1527, 1528), new WorldPoint(2854, 2952, 0), BankLocation.SHILO_VILLAGE, Arrays.asList("Lure")),
            new FishingSpot("Fishing Guild", 40, Arrays.asList("lobster", "tuna", "swordfish", "shark"), Arrays.asList(1510, 1511, 1516, 1518, 1520), new WorldPoint(2599, 3421, 0), BankLocation.FISHING_GUILD, Arrays.asList("Cage", "Harpoon")),
            new FishingSpot("Piscatoris", 62, Arrays.asList("monkfish"), Arrays.asList(317), new WorldPoint(2334, 3647, 0), BankLocation.PISCATORIS_FISHING_COLONY, Arrays.asList("Net")),
            new FishingSpot("Tai Bwo Wannai", 65, Arrays.asList("karambwan"), Arrays.asList(4712), new WorldPoint(2780, 3178, 0), BankLocation.SHILO_VILLAGE, Arrays.asList("Karambwanji")),
            new FishingSpot("Port Piscarilius", 82, Arrays.asList("anglerfish"), Arrays.asList(6488), new WorldPoint(1799, 3756, 0), BankLocation.PISCARILIUS, Arrays.asList("Bait")),
            new FishingSpot("Living Rock Caverns", 85, Arrays.asList("cavefish", "rocktail"), Arrays.asList(6825), new WorldPoint(3652, 5123, 0), BankLocation.EDGEVILLE, Arrays.asList("Bait"))
    );
    private FishingSpot currentSpot = null;

    @Override
    public void applySettings(SkillRuntimeSettings settings) {
        if (settings != null) {
            enabled = settings.isEnabled();
            if (settings.getMode() != null) {
                String m = settings.getMode().toLowerCase(Locale.ROOT);
                if (m.contains("bank")) {
                    mode = Mode.BANK;
                } else {
                    mode = Mode.DROP;
                }
            }

            // Extract fishing method from flags
            configuredFishingMethod = "AUTO"; // Default
            if (settings.getFlags() != null) {
                for (String flag : settings.getFlags()) {
                    if (flag.startsWith("FISHING_METHOD:")) {
                        configuredFishingMethod = flag.substring("FISHING_METHOD:".length());
                        break;
                    }
                }
            }
        }
    }

    private void updateTier() {
        // Check if user has configured a specific fishing method
        if (!"AUTO".equals(configuredFishingMethod)) {
            // User has chosen a specific method, use that instead of auto-progression
            switch (configuredFishingMethod) {
                case "NET":
                case "BAIT":
                    tier = FishingTier.NET_BAIT;
                    break;
                case "LURE":
                    tier = FishingTier.LURE;
                    break;
                case "CAGE":
                case "HARPOON":
                    tier = FishingTier.CAGE_HARPOON;
                    break;
                default:
                    // Fallback to auto if unknown method
                    updateTierAuto();
                    break;
            }
        } else {
            // Auto mode: use level-based progression as before
            updateTierAuto();
        }
    }

    private void updateTierAuto() {
        // Use CURRENT level only, not target level
        // The bot will automatically upgrade tools when it reaches the required level
        int currentLvl = Microbot.getClient().getRealSkillLevel(Skill.FISHING);

        // Check what tools are actually available and fallback to lower tiers if needed
        if (currentLvl >= 40 && hasToolsForTier(FishingTier.CAGE_HARPOON)) {
            tier = FishingTier.CAGE_HARPOON;
        } else if (currentLvl >= 20 && hasToolsForTier(FishingTier.LURE)) {
            tier = FishingTier.LURE;
        } else if (hasToolsForTier(FishingTier.NET_BAIT)) {
            tier = FishingTier.NET_BAIT;
        } else {
            // Final fallback - use NET_BAIT tier even if tools aren't available
            // The obtainTools method will try to get them
            tier = FishingTier.NET_BAIT;
        }
    }

    private boolean hasToolsForTier(FishingTier checkTier) {
        switch (checkTier) {
            case NET_BAIT:
                return (Rs2Inventory.contains("Small fishing net") || canGetFromBank("Small fishing net"))
                        || (Rs2Inventory.contains("Fishing rod") && Rs2Inventory.contains("Fishing bait"))
                        || (canGetFromBank("Fishing rod") && canGetFromBank("Fishing bait"));
            case LURE:
                return (Rs2Inventory.contains("Fly fishing rod") && Rs2Inventory.contains("Feather"))
                        || (canGetFromBank("Fly fishing rod") && canGetFromBank("Feather"));
            case CAGE_HARPOON:
                return Rs2Inventory.contains("Lobster pot") || Rs2Inventory.contains("Harpoon")
                        || canGetFromBank("Lobster pot") || canGetFromBank("Harpoon");
            default:
                return false;
        }
    }

    private boolean canGetFromBank(String itemName) {
        // Only check bank if we're close to a bank to avoid unnecessary walking
        if (currentSpot != null && currentSpot.bankLocation != null) {
            WorldPoint bankLocation = currentSpot.bankLocation.getWorldPoint();
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

    private void updateCurrentSpot() {
        int lvl = Microbot.getClient().getRealSkillLevel(Skill.FISHING);
        FishingSpot best = null;

        for (FishingSpot spot : FISHING_SPOTS) {
            if (lvl >= spot.minLevel) {
                boolean supportsMethod = false;

                // If user has configured a specific fishing method, check if spot supports it
                if (!"AUTO".equals(configuredFishingMethod)) {
                    for (String action : spot.actions) {
                        switch (configuredFishingMethod) {
                            case "NET":
                                if (action.equalsIgnoreCase("Net")) supportsMethod = true;
                                break;
                            case "BAIT":
                                if (action.equalsIgnoreCase("Bait")) supportsMethod = true;
                                break;
                            case "LURE":
                                if (action.equalsIgnoreCase("Lure")) supportsMethod = true;
                                break;
                            case "CAGE":
                                if (action.equalsIgnoreCase("Cage")) supportsMethod = true;
                                break;
                            case "HARPOON":
                                if (action.equalsIgnoreCase("Harpoon")) supportsMethod = true;
                                break;
                        }
                    }
                    if (!supportsMethod) continue; // Skip spots that don't support the chosen method
                } else {
                    // AUTO mode: check if spot supports current tier's methods
                    for (String action : spot.actions) {
                        switch (tier) {
                            case NET_BAIT:
                                if (action.equalsIgnoreCase("Net") || action.equalsIgnoreCase("Bait")) {
                                    supportsMethod = true;
                                }
                                break;
                            case LURE:
                                if (action.equalsIgnoreCase("Lure")) {
                                    supportsMethod = true;
                                }
                                break;
                            case CAGE_HARPOON:
                                if (action.equalsIgnoreCase("Cage") || action.equalsIgnoreCase("Harpoon")) {
                                    supportsMethod = true;
                                }
                                break;
                        }
                    }
                    if (!supportsMethod) continue; // Skip spots that don't support current tier
                }

                if (best == null || (spot.bankLocation != null && best.bankLocation != null &&
                        Rs2Walker.getDistanceBetween(Microbot.getClient().getLocalPlayer().getWorldLocation(), spot.bankLocation.getWorldPoint()) <
                                Rs2Walker.getDistanceBetween(Microbot.getClient().getLocalPlayer().getWorldLocation(), best.bankLocation.getWorldPoint()))) {
                    best = spot;
                }
            }
        }
        currentSpot = best;
    }

    @Override
    public void execute() {
        if (!enabled) {
            Microbot.status = "Fishing: disabled";
            return;
        }

        if (!Microbot.isLoggedIn()) return;

        // Determine current fishing spot based on level and method
        currentSpot = determineCurrentFishingSpot();
        if (currentSpot == null) {
            Microbot.status = "Fishing: no suitable spot for level " + Microbot.getClient().getRealSkillLevel(Skill.FISHING);
            return;
        }

        // Check if we need to get equipment
        if (!hasRequiredEquipment()) {
            getRequiredEquipment();
            return;
        }

        // Check if inventory is full
        if (Rs2Inventory.isFull()) {
            handleFullInventory();
            return;
        }

        // Check if we're at the fishing spot
        if (!isAtFishingSpot()) {
            walkToFishingSpot();
            return;
        }

        // Do fishing
        attemptFishing();
    }

    private FishingSpot determineCurrentFishingSpot() {
        int fishingLevel = Microbot.getClient().getRealSkillLevel(Skill.FISHING);

        if ("AUTO".equals(configuredFishingMethod)) {
            // Find highest level spot we can fish at
            FishingSpot bestSpot = null;
            for (FishingSpot spot : FISHING_SPOTS) {
                if (fishingLevel >= spot.minLevel) {
                    bestSpot = spot;
                }
            }
            return bestSpot;
        } else {
            // Try to find spot by configured method
            for (FishingSpot spot : FISHING_SPOTS) {
                if (spot.name.equalsIgnoreCase(configuredFishingMethod) && fishingLevel >= spot.minLevel) {
                    return spot;
                }
            }
            // Fallback to auto if configured method not found
            return determineCurrentFishingSpot();
        }
    }

    private boolean hasRequiredEquipment() {
        if (currentSpot == null) return false;

        // Check for required fishing equipment based on spot
        if (currentSpot.actions.contains("Net")) {
            return Rs2Inventory.hasItem("Small fishing net");
        }
        if (currentSpot.actions.contains("Bait")) {
            return Rs2Inventory.hasItem("Fishing rod") && Rs2Inventory.hasItem("Fishing bait");
        }
        if (currentSpot.actions.contains("Lure")) {
            return Rs2Inventory.hasItem("Fly fishing rod") && Rs2Inventory.hasItem("Feather");
        }
        if (currentSpot.actions.contains("Cage")) {
            return Rs2Inventory.hasItem("Lobster pot");
        }
        if (currentSpot.actions.contains("Harpoon")) {
            return Rs2Inventory.hasItem("Harpoon");
        }

        return true; // Default case
    }

    private void getRequiredEquipment() {
        if (!Rs2Bank.openBank()) return;

        Microbot.status = "Fishing: Getting equipment for " + currentSpot.name;

        // Get required equipment based on fishing spot
        if (currentSpot.actions.contains("Net") && !Rs2Inventory.hasItem("Small fishing net")) {
            Rs2Bank.withdrawItem(true, "Small fishing net");
        }
        if (currentSpot.actions.contains("Bait")) {
            if (!Rs2Inventory.hasItem("Fishing rod")) {
                Rs2Bank.withdrawItem(true, "Fishing rod");
            }
            if (!Rs2Inventory.hasItem("Fishing bait")) {
                Rs2Bank.withdrawX(true, "Fishing bait", 1000);
            }
        }
        if (currentSpot.actions.contains("Lure")) {
            if (!Rs2Inventory.hasItem("Fly fishing rod")) {
                Rs2Bank.withdrawItem(true, "Fly fishing rod");
            }
            if (!Rs2Inventory.hasItem("Feather")) {
                Rs2Bank.withdrawX(true, "Feather", 1000);
            }
        }
        if (currentSpot.actions.contains("Cage") && !Rs2Inventory.hasItem("Lobster pot")) {
            Rs2Bank.withdrawItem(true, "Lobster pot");
        }
        if (currentSpot.actions.contains("Harpoon") && !Rs2Inventory.hasItem("Harpoon")) {
            Rs2Bank.withdrawItem(true, "Harpoon");
        }

        Rs2Bank.closeBank();
    }

    private boolean isAtFishingSpot() {
        if (currentSpot == null) return false;
        return Rs2Player.getWorldLocation().distanceTo(currentSpot.location) <= 10;
    }

    private void walkToFishingSpot() {
        if (currentSpot == null) return;

        Microbot.status = "Fishing: Walking to " + currentSpot.name;
        Rs2Walker.walkTo(currentSpot.location);
    }

    private void attemptFishing() {
        long now = System.currentTimeMillis();
        if (now - lastAttempt < ATTEMPT_COOLDOWN) return;
        if (Rs2Player.isAnimating() || Rs2Player.isMoving()) return;

        // Find fishing spot NPC via stream filter (replaces deprecated getNearestNpc)
        Rs2NpcModel fishingSpot = Rs2Npc.getNpcs(npc -> {
            if (npc == null) return false;
            if (currentSpot == null) return false;
            try {
                net.runelite.api.NPCComposition comp = npc.getComposition();
                return comp != null && currentSpot.spotIds.contains(comp.getId());
            } catch (Exception e) {
                return false;
            }
        }).findFirst().orElse(null);

        if (fishingSpot != null) {
            String action = getActionForSpot(0); // action selection based on currentSpot.actions
            if (action != null && Rs2Npc.interact(fishingSpot, action)) {
                Microbot.status = "Fishing: " + action + " at " + currentSpot.name;
                lastAttempt = now;
                Rs2Player.waitForXpDrop(Skill.FISHING, true);
            }
        }
    }

    private String getActionForSpot(int spotId) {
        // Map spot IDs to actions - this would need to be refined based on actual spot IDs
        if (currentSpot.actions.contains("Net")) return "Net";
        if (currentSpot.actions.contains("Bait")) return "Bait";
        if (currentSpot.actions.contains("Lure")) return "Lure";
        if (currentSpot.actions.contains("Cage")) return "Cage";
        if (currentSpot.actions.contains("Harpoon")) return "Harpoon";
        return "Net"; // Default
    }

    private void handleFullInventory() {
        if (mode == Mode.BANK) {
            bankFish();
        } else {
            dropFish();
        }
    }

    private void bankFish() {
        if (currentSpot == null) return;

        List<String> fishTypes = Arrays.asList("Raw shrimp", "Raw anchovy", "Raw trout", "Raw salmon",
                "Raw lobster", "Raw tuna", "Raw swordfish", "Raw shark", "Raw monkfish", "Raw karambwan",
                "Raw anglerfish", "Raw cavefish", "Raw rocktail");

        if (!Rs2Bank.bankItemsAndWalkBackToOriginalPosition(fishTypes, currentSpot.location, 0, 10)) {
            Microbot.status = "Fishing: Banking failed, trying again...";
        }
    }

    private void dropFish() {
        // Drop fish except fishing equipment
        String[] equipmentToKeep = {"Small fishing net", "Fishing rod", "Fly fishing rod", "Big fishing net",
                                   "Harpoon", "Lobster pot", "Feather", "Fishing bait", "Barbarian rod"};
        Rs2Inventory.dropAllExcept(equipmentToKeep);
        Microbot.status = "Fishing: Dropped fish";
    }

    // Getters voor debugging
    public boolean isEnabled() { return enabled; }
    public Mode getMode() { return mode; }
    public FishingSpot getCurrentSpot() { return currentSpot; }
    public String getConfiguredMethod() { return configuredFishingMethod; }
}
