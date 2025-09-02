package net.runelite.client.plugins.microbot.jpnl.accountbuilder.skills;

import net.runelite.api.GameObject;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.ObjectComposition;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.settings.SkillRuntimeSettings;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.woodcutting.enums.WoodcuttingTree;

import java.util.*;

import static net.runelite.api.gameval.ItemID.TINDERBOX;

/**
 * Complete Woodcutting skill handler for AIO bot system.
 * Supports all tree types, banking, dropping, firemaking integration.
 */
public class WoodcuttingSkillHandler implements SkillHandler {

    private boolean enabled = true;
    private String mode = "drop"; // drop, bank, firemaking
    private WoodcuttingTree currentTree = WoodcuttingTree.TREE;
    private WorldPoint initialPlayerLocation;
    private boolean useBank = false;
    private boolean useFiremaking = false;

    // Woodcutting locations per tree type
    private static final Map<WoodcuttingTree, List<WorldPoint>> WOODCUTTING_LOCATIONS = new HashMap<>();

    static {
        WOODCUTTING_LOCATIONS.put(WoodcuttingTree.TREE, Arrays.asList(
                new WorldPoint(3077, 3421, 0), // Barbarian Village
                new WorldPoint(3166, 3412, 0), // Grand Exchange area
                new WorldPoint(3205, 3434, 0), // Varrock East
                new WorldPoint(3278, 3390, 0), // Lumbridge
                new WorldPoint(2721, 3598, 0)  // Seers Village
        ));

        WOODCUTTING_LOCATIONS.put(WoodcuttingTree.OAK, Arrays.asList(
                new WorldPoint(3166, 3412, 0), // Grand Exchange area
                new WorldPoint(2721, 3598, 0), // Seers Village
                new WorldPoint(3278, 3390, 0), // Lumbridge
                new WorldPoint(1308, 3792, 0), // Draynor Village
                new WorldPoint(2595, 3482, 0)  // Catherby
        ));

        WOODCUTTING_LOCATIONS.put(WoodcuttingTree.WILLOW, Arrays.asList(
                new WorldPoint(1308, 3792, 0), // Draynor Village
                new WorldPoint(2721, 3598, 0), // Seers Village
                new WorldPoint(3005, 3114, 0), // Port Sarim
                new WorldPoint(2595, 3482, 0), // Catherby
                new WorldPoint(3088, 3233, 0)  // Lumbridge
        ));

        WOODCUTTING_LOCATIONS.put(WoodcuttingTree.MAPLE, Arrays.asList(
                new WorldPoint(2721, 3598, 0), // Seers Village
                new WorldPoint(2595, 3482, 0), // Catherby
                new WorldPoint(3166, 3412, 0), // Grand Exchange area
                new WorldPoint(1308, 3792, 0)  // Draynor Village
        ));

        WOODCUTTING_LOCATIONS.put(WoodcuttingTree.YEW, Arrays.asList(
                new WorldPoint(2595, 3482, 0), // Catherby
                new WorldPoint(3087, 3468, 0), // Varrock Palace
                new WorldPoint(1308, 3792, 0), // Draynor Village
                new WorldPoint(2756, 3477, 0), // Seers Village
                new WorldPoint(3209, 3221, 0)  // Lumbridge
        ));

        WOODCUTTING_LOCATIONS.put(WoodcuttingTree.MAGIC, Arrays.asList(
                new WorldPoint(2595, 3482, 0), // Catherby
                new WorldPoint(3087, 3468, 0), // Varrock Palace
                new WorldPoint(2756, 3477, 0), // Seers Village
                new WorldPoint(1692, 3508, 0)  // Tree Gnome Stronghold
        ));

        WOODCUTTING_LOCATIONS.put(WoodcuttingTree.REDWOOD, Arrays.asList(
                new WorldPoint(1572, 3486, 0) // Tree Gnome Stronghold - Redwood trees
        ));
    }

    @Override
    public void applySettings(SkillRuntimeSettings settings) {
        if (settings != null) {
            enabled = settings.isEnabled();
            mode = settings.getMode() != null ? settings.getMode().toLowerCase() : "drop";
            useBank = "bank".equals(mode);
            useFiremaking = "firemaking".equals(mode);
        }
    }

    @Override
    public void execute() {
        if (!enabled) {
            Microbot.status = "Woodcutting: disabled";
            return;
        }

        if (!Microbot.isLoggedIn()) return;

        // Initialize player location on first run
        if (initialPlayerLocation == null) {
            initialPlayerLocation = Rs2Player.getWorldLocation();
            setupAntiban();
        }

        // Determine current tree type based on woodcutting level
        currentTree = determineCurrentTree();
        if (currentTree == null) {
            Microbot.status = "Woodcutting: no suitable tree for level " + Microbot.getClient().getRealSkillLevel(Skill.WOODCUTTING);
            return;
        }

        // Check if player is moving or animating
        if (Rs2Player.isMoving() || Rs2Player.isAnimating()) return;

        // Special attack with dragon axe
        if (Rs2Equipment.isWearing("Dragon axe")) {
            Rs2Combat.setSpecState(true, 1000);
        }

        // Woodcutting logic
        if (useFiremaking) {
            handleFiremaking();
        } else if (Rs2Inventory.isFull()) {
            handleFullInventory();
        } else {
            cutTree();
        }
    }

    private void setupAntiban() {
        Rs2Antiban.resetAntibanSettings();
        Rs2Antiban.antibanSetupTemplates.applyWoodcuttingSetup();
        Rs2AntibanSettings.dynamicActivity = true;
        Rs2AntibanSettings.dynamicIntensity = true;
    }

    private WoodcuttingTree determineCurrentTree() {
        int woodcuttingLevel = Microbot.getClient().getRealSkillLevel(Skill.WOODCUTTING);

        if ("auto".equals(mode)) {
            // Find highest level tree we can cut
            WoodcuttingTree bestTree = null;
            for (WoodcuttingTree tree : WoodcuttingTree.values()) {
                if (tree.hasRequiredLevel() && woodcuttingLevel >= tree.getWoodcuttingLevel()) {
                    bestTree = tree;
                }
            }
            return bestTree;
        } else {
            // Try to use configured tree type
            try {
                WoodcuttingTree configuredTree = WoodcuttingTree.valueOf(mode.toUpperCase());
                return configuredTree.hasRequiredLevel() ? configuredTree : null;
            } catch (IllegalArgumentException e) {
                return WoodcuttingTree.TREE; // Fallback
            }
        }
    }

    private void cutTree() {
        // Find tree using modern API
        List<GameObject> trees = Rs2GameObject.getGameObjects(obj -> {
            ObjectComposition comp = Rs2GameObject.convertToObjectComposition(obj);
            String name = comp != null ? comp.getName() : null;
            return name != null && name.contains(currentTree.name()) &&
                initialPlayerLocation != null && obj.getWorldLocation().distanceTo(initialPlayerLocation) <= 15;
        });

        GameObject tree = trees.isEmpty() ? null : trees.get(0);

        if (tree != null) {
            if (Rs2GameObject.interact(tree, "Chop down")) {
                Microbot.status = "Woodcutting: Cutting " + currentTree.name();
                Rs2Player.waitForXpDrop(Skill.WOODCUTTING, true);
                Rs2Antiban.actionCooldown();
                Rs2Antiban.takeMicroBreakByChance();
            }
        } else {
            // No tree found, try to walk to woodcutting location
            walkToWoodcuttingLocation();
        }
    }

    private void walkToWoodcuttingLocation() {
        List<WorldPoint> locations = WOODCUTTING_LOCATIONS.get(currentTree);
        if (locations != null && !locations.isEmpty()) {
            WorldPoint nearestLocation = findNearestLocation(locations);
            if (nearestLocation != null) {
                Microbot.status = "Woodcutting: Walking to " + currentTree.name() + " location";
                Rs2Walker.walkTo(nearestLocation);
                initialPlayerLocation = nearestLocation;
            }
        }
    }

    private WorldPoint findNearestLocation(List<WorldPoint> locations) {
        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        return locations.stream()
                .min(Comparator.comparingInt(loc -> playerLocation.distanceTo(loc)))
                .orElse(null);
    }

    private void handleFullInventory() {
        if (useBank) {
            bankLogs();
        } else {
            dropLogs();
        }
    }

    private void handleFiremaking() {
        // If no tinderbox, get one from bank
        if (!Rs2Inventory.hasItem(TINDERBOX)) {
            Rs2Bank.openBank();
            if (Rs2Bank.isOpen()) {
                Rs2Bank.withdrawItem(true, "Tinderbox");
                Rs2Bank.closeBank();
            }
            return;
        }

        // If no logs, cut more trees
        if (!Rs2Inventory.hasItem(currentTree.getLog())) {
            cutTree();
            return;
        }

        // Light fires
        if (Rs2Inventory.combine("Tinderbox", currentTree.getLog())) {
            Microbot.status = "Woodcutting: Firemaking " + currentTree.getLog();
            sleep(3000, 5000); // Wait for fire to light
        }
    }

    private void bankLogs() {
        List<String> itemsToBank = Arrays.asList("Logs", "Oak logs", "Willow logs", "Maple logs",
                "Yew logs", "Magic logs", "Redwood logs", "Teak logs", "Mahogany logs");

        if (!Rs2Bank.bankItemsAndWalkBackToOriginalPosition(itemsToBank, initialPlayerLocation, 0, 15)) {
            Microbot.status = "Woodcutting: Banking failed, trying again...";
        }
    }

    private void dropLogs() {
        // Drop logs except axe and equipment
        String[] itemsToKeep = {"Axe", "Bronze axe", "Iron axe", "Steel axe", "Black axe",
                                "Mithril axe", "Adamant axe", "Rune axe", "Dragon axe", "Infernal axe"};
        Rs2Inventory.dropAllExcept(itemsToKeep);
        Microbot.status = "Woodcutting: Dropped logs";
    }

    private void sleep(int min, int max) {
        try {
            Thread.sleep(min + (int) (Math.random() * (max - min)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Getters voor debugging
    public boolean isEnabled() { return enabled; }
    public String getMode() { return mode; }
    public WoodcuttingTree getCurrentTree() { return currentTree; }
}