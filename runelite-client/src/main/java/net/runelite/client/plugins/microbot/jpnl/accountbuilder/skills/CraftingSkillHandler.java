package net.runelite.client.plugins.microbot.jpnl.accountbuilder.skills;

import net.runelite.api.GameObject;
import net.runelite.api.ItemID;
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
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.util.*;

/**
 * Complete Crafting skill handler for AIO bot system.
 * Supports pottery, jewelry, leather, glassmaking, spinning, and weaving.
 */
public class CraftingSkillHandler implements SkillHandler {

    private boolean enabled = true;
    private String mode = "auto"; // auto, pottery, jewelry, leather, glass, etc.
    private CraftingMethod currentMethod = CraftingMethod.POTTERY;
    private CraftingLocation currentLocation;
    private CraftingItem currentItem;

    // Crafting data structures
    private enum CraftingMethod {
        POTTERY, JEWELRY, LEATHER, GLASSMAKING, SPINNING, WEAVING
    }

    private static class CraftingItem {
        String name;
        int levelRequired;
        Map<String, Integer> materials; // material name -> amount needed
        String craftingObject;
        CraftingMethod method;
        int experience;

        public CraftingItem(String name, int levelRequired, Map<String, Integer> materials,
                          String craftingObject, CraftingMethod method, int experience) {
            this.name = name;
            this.levelRequired = levelRequired;
            this.materials = materials;
            this.craftingObject = craftingObject;
            this.method = method;
            this.experience = experience;
        }
    }

    private static class CraftingLocation {
        String name;
        WorldPoint location;
        BankLocation bankLocation;
        List<String> objects;
        List<CraftingMethod> supportedMethods;

        public CraftingLocation(String name, WorldPoint location, BankLocation bankLocation,
                              List<String> objects, List<CraftingMethod> supportedMethods) {
            this.name = name;
            this.location = location;
            this.bankLocation = bankLocation;
            this.objects = objects;
            this.supportedMethods = supportedMethods;
        }
    }

    // Crafting items database
    private static final List<CraftingItem> CRAFTING_ITEMS = Arrays.asList(
            // Pottery
            new CraftingItem("Pot", 1, Map.of("Soft clay", 1), "Potter's wheel", CraftingMethod.POTTERY, 25),
            new CraftingItem("Pie dish", 7, Map.of("Soft clay", 1), "Potter's wheel", CraftingMethod.POTTERY, 25),
            new CraftingItem("Bowl", 8, Map.of("Soft clay", 1), "Potter's wheel", CraftingMethod.POTTERY, 25),
            new CraftingItem("Plant pot", 19, Map.of("Soft clay", 1), "Potter's wheel", CraftingMethod.POTTERY, 20),
            new CraftingItem("Pot lid", 25, Map.of("Soft clay", 1), "Potter's wheel", CraftingMethod.POTTERY, 20),

            // Jewelry
            new CraftingItem("Gold ring", 5, Map.of("Gold bar", 1), "Furnace", CraftingMethod.JEWELRY, 15),
            new CraftingItem("Sapphire ring", 20, Map.of("Gold bar", 1, "Sapphire", 1), "Furnace", CraftingMethod.JEWELRY, 40),
            new CraftingItem("Emerald ring", 27, Map.of("Gold bar", 1, "Emerald", 1), "Furnace", CraftingMethod.JEWELRY, 55),
            new CraftingItem("Ruby ring", 34, Map.of("Gold bar", 1, "Ruby", 1), "Furnace", CraftingMethod.JEWELRY, 70),
            new CraftingItem("Diamond ring", 43, Map.of("Gold bar", 1, "Diamond", 1), "Furnace", CraftingMethod.JEWELRY, 85),
            new CraftingItem("Gold necklace", 6, Map.of("Gold bar", 1), "Furnace", CraftingMethod.JEWELRY, 20),
            new CraftingItem("Sapphire necklace", 22, Map.of("Gold bar", 1, "Sapphire", 1), "Furnace", CraftingMethod.JEWELRY, 55),
            new CraftingItem("Emerald necklace", 29, Map.of("Gold bar", 1, "Emerald", 1), "Furnace", CraftingMethod.JEWELRY, 60),
            new CraftingItem("Ruby necklace", 40, Map.of("Gold bar", 1, "Ruby", 1), "Furnace", CraftingMethod.JEWELRY, 75),
            new CraftingItem("Diamond necklace", 56, Map.of("Gold bar", 1, "Diamond", 1), "Furnace", CraftingMethod.JEWELRY, 90),

            // Leather
            new CraftingItem("Leather gloves", 1, Map.of("Leather", 1), "Crafting table", CraftingMethod.LEATHER, 14),
            new CraftingItem("Leather boots", 7, Map.of("Leather", 1), "Crafting table", CraftingMethod.LEATHER, 16),
            new CraftingItem("Leather cowl", 9, Map.of("Leather", 1), "Crafting table", CraftingMethod.LEATHER, 18),
            new CraftingItem("Leather vambraces", 11, Map.of("Leather", 1), "Crafting table", CraftingMethod.LEATHER, 22),
            new CraftingItem("Leather body", 14, Map.of("Leather", 1), "Crafting table", CraftingMethod.LEATHER, 25),
            new CraftingItem("Leather chaps", 18, Map.of("Leather", 1), "Crafting table", CraftingMethod.LEATHER, 27),
            new CraftingItem("Hardleather body", 28, Map.of("Hard leather", 1), "Crafting table", CraftingMethod.LEATHER, 35),
            new CraftingItem("Studded body", 20, Map.of("Leather", 1, "Steel studs", 1), "Crafting table", CraftingMethod.LEATHER, 40),
            new CraftingItem("Studded chaps", 44, Map.of("Leather", 1, "Steel studs", 1), "Crafting table", CraftingMethod.LEATHER, 42),

            // Glassmaking
            new CraftingItem("Beer glass", 1, Map.of("Molten glass", 1), "Glassblowing pipe", CraftingMethod.GLASSMAKING, 18),
            new CraftingItem("Candle lantern", 4, Map.of("Molten glass", 1), "Glassblowing pipe", CraftingMethod.GLASSMAKING, 19),
            new CraftingItem("Oil lamp", 12, Map.of("Molten glass", 1), "Glassblowing pipe", CraftingMethod.GLASSMAKING, 25),
            new CraftingItem("Vial", 33, Map.of("Molten glass", 1), "Glassblowing pipe", CraftingMethod.GLASSMAKING, 35),
            new CraftingItem("Fishbowl", 42, Map.of("Molten glass", 1), "Glassblowing pipe", CraftingMethod.GLASSMAKING, 42),
            new CraftingItem("Unpowered orb", 46, Map.of("Molten glass", 1), "Glassblowing pipe", CraftingMethod.GLASSMAKING, 52),
            new CraftingItem("Lantern lens", 49, Map.of("Molten glass", 1), "Glassblowing pipe", CraftingMethod.GLASSMAKING, 55),
            new CraftingItem("Light orb", 87, Map.of("Molten glass", 1), "Glassblowing pipe", CraftingMethod.GLASSMAKING, 70),

            // Spinning
            new CraftingItem("Ball of wool", 1, Map.of("Wool", 1), "Spinning wheel", CraftingMethod.SPINNING, 3),
            new CraftingItem("Bow string", 10, Map.of("Flax", 1), "Spinning wheel", CraftingMethod.SPINNING, 15),
            new CraftingItem("Magic string", 19, Map.of("Magic roots", 1), "Spinning wheel", CraftingMethod.SPINNING, 30)
    );

    // Crafting locations database
    private static final List<CraftingLocation> CRAFTING_LOCATIONS = Arrays.asList(
            new CraftingLocation("Barbarian Village", new WorldPoint(3085, 3410, 0), BankLocation.EDGEVILLE,
                    Arrays.asList("Potter's wheel", "Pottery oven", "Spinning wheel"),
                    Arrays.asList(CraftingMethod.POTTERY, CraftingMethod.SPINNING)),
            new CraftingLocation("Al Kharid", new WorldPoint(3275, 3185, 0), BankLocation.AL_KHARID,
                    Arrays.asList("Furnace", "Crafting table"),
                    Arrays.asList(CraftingMethod.JEWELRY, CraftingMethod.LEATHER)),
            new CraftingLocation("Falador", new WorldPoint(2970, 3369, 0), BankLocation.FALADOR_EAST,
                    Arrays.asList("Furnace", "Spinning wheel"),
                    Arrays.asList(CraftingMethod.JEWELRY, CraftingMethod.SPINNING)),
            new CraftingLocation("Lumbridge", new WorldPoint(3209, 3214, 2), BankLocation.LUMBRIDGE_TOP,
                    Arrays.asList("Spinning wheel"),
                    Arrays.asList(CraftingMethod.SPINNING)),
            new CraftingLocation("Seers Village", new WorldPoint(2721, 3472, 0), BankLocation.CAMELOT, // fixed SEERS_VILLAGE -> CAMELOT
                    Arrays.asList("Spinning wheel", "Flax"),
                    Arrays.asList(CraftingMethod.SPINNING)),
            new CraftingLocation("Yanille", new WorldPoint(2614, 3106, 0), BankLocation.YANILLE,
                    Arrays.asList("Potter's wheel", "Pottery oven"),
                    Arrays.asList(CraftingMethod.POTTERY)),
            new CraftingLocation("Port Khazard", new WorldPoint(2651, 3161, 0), BankLocation.PORT_KHAZARD,
                    Arrays.asList("Furnace"),
                    Arrays.asList(CraftingMethod.GLASSMAKING)),
            new CraftingLocation("Crafting Guild", new WorldPoint(2935, 3283, 0), BankLocation.CRAFTING_GUILD,
                    Arrays.asList("Potter's wheel", "Pottery oven", "Spinning wheel", "Loom"),
                    Arrays.asList(CraftingMethod.POTTERY, CraftingMethod.SPINNING, CraftingMethod.WEAVING))
    );

    @Override
    public void applySettings(SkillRuntimeSettings settings) {
        if (settings != null) {
            enabled = settings.isEnabled();
            mode = settings.getMode() != null ? settings.getMode().toLowerCase() : "auto";

            // Determine method from mode
            if (mode.contains("pottery")) {
                currentMethod = CraftingMethod.POTTERY;
            } else if (mode.contains("jewelry")) {
                currentMethod = CraftingMethod.JEWELRY;
            } else if (mode.contains("leather")) {
                currentMethod = CraftingMethod.LEATHER;
            } else if (mode.contains("glass")) {
                currentMethod = CraftingMethod.GLASSMAKING;
            } else if (mode.contains("spinning")) {
                currentMethod = CraftingMethod.SPINNING;
            } else {
                currentMethod = CraftingMethod.POTTERY; // Default
            }
        }
    }

    @Override
    public void execute() {
        if (!enabled) {
            Microbot.status = "Crafting: disabled";
            return;
        }

        if (!Microbot.isLoggedIn()) return;

        // Determine current item and location
        determineCraftingPlan();
        if (currentItem == null || currentLocation == null) {
            Microbot.status = "Crafting: no suitable item/location for level " + Microbot.getClient().getRealSkillLevel(Skill.CRAFTING);
            return;
        }

        // Check if we need materials
        if (!hasMaterials()) {
            getMaterials();
            return;
        }

        // Check if we're at crafting location
        if (!isAtCraftingLocation()) {
            walkToCraftingLocation();
            return;
        }

        // Check if inventory is full of crafted items
        if (Rs2Inventory.isFull() && !hasMaterials()) {
            bankProducts();
            return;
        }

        // Do crafting
        attemptCrafting();
    }

    private void determineCraftingPlan() {
        int craftingLevel = Microbot.getClient().getRealSkillLevel(Skill.CRAFTING);

        // Determine target item based on mode and level
        if ("auto".equals(mode)) {
            // Find highest level item we can craft for current method
            for (CraftingItem item : CRAFTING_ITEMS) {
                if (item.method == currentMethod && craftingLevel >= item.levelRequired) {
                    currentItem = item;
                }
            }
        } else {
            // Try to find specific item
            for (CraftingItem item : CRAFTING_ITEMS) {
                if (item.name.toLowerCase().contains(mode) && craftingLevel >= item.levelRequired) {
                    currentItem = item;
                    currentMethod = item.method; // Update method based on item
                    break;
                }
            }
        }

        // Find nearest location for current method
        currentLocation = findNearestCraftingLocation();
    }

    private CraftingLocation findNearestCraftingLocation() {
        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        return CRAFTING_LOCATIONS.stream()
                .filter(loc -> loc.supportedMethods.contains(currentMethod))
                .min(Comparator.comparingInt(loc -> playerLocation.distanceTo(loc.location)))
                .orElse(CRAFTING_LOCATIONS.get(0)); // Fallback
    }

    private boolean hasMaterials() {
        if (currentItem == null) return false;

        for (Map.Entry<String, Integer> material : currentItem.materials.entrySet()) {
            if (Rs2Inventory.count(material.getKey()) < material.getValue()) {
                return false;
            }
        }

        // Check for special tools
        if (currentMethod == CraftingMethod.GLASSMAKING && !Rs2Inventory.hasItem("Glassblowing pipe")) {
            return false;
        }
        if (currentMethod == CraftingMethod.LEATHER && !Rs2Inventory.hasItem("Needle") && !Rs2Inventory.hasItem("Thread")) {
            return false;
        }

        return true;
    }

    private void getMaterials() {
        if (!Rs2Bank.openBank()) return;

        Microbot.status = "Crafting: Getting materials for " + currentItem.name;

        // Get special tools first
        if (currentMethod == CraftingMethod.GLASSMAKING && !Rs2Inventory.hasItem("Glassblowing pipe")) {
            Rs2Bank.withdrawItem(true, "Glassblowing pipe");
        }
        if (currentMethod == CraftingMethod.LEATHER) {
            if (!Rs2Inventory.hasItem("Needle")) {
                Rs2Bank.withdrawItem(true, "Needle");
            }
            if (!Rs2Inventory.hasItem("Thread")) {
                Rs2Bank.withdrawX(true, "Thread", 100);
            }
        }

        // Get materials
        for (Map.Entry<String, Integer> material : currentItem.materials.entrySet()) {
            int needed = material.getValue() * 14; // Full inventory worth
            Rs2Bank.withdrawX(true, material.getKey(), needed);
        }

        Rs2Bank.closeBank();
    }

    private boolean isAtCraftingLocation() {
        if (currentLocation == null) return false;
        return Rs2Player.getWorldLocation().distanceTo(currentLocation.location) <= 15;
    }

    private void walkToCraftingLocation() {
        if (currentLocation == null) return;

        Microbot.status = "Crafting: Walking to " + currentLocation.name;
        Rs2Walker.walkTo(currentLocation.location);
    }

    private void attemptCrafting() {
        if (!hasMaterials()) return;

        // Find crafting object (updated API usage)
        GameObject craftingObject = Rs2GameObject.getGameObject(currentItem.craftingObject, currentLocation.location);

        if (craftingObject != null) {
            switch (currentMethod) {
                case POTTERY:
                    attemptPottery(craftingObject);
                    break;
                case JEWELRY:
                    attemptJewelry(craftingObject);
                    break;
                case LEATHER:
                    attemptLeather();
                    break;
                case GLASSMAKING:
                    attemptGlassmaking();
                    break;
                case SPINNING:
                    attemptSpinning(craftingObject);
                    break;
                default:
                    break;
            }
        } else {
            Microbot.status = "Crafting: No " + currentItem.craftingObject + " found at " + currentLocation.name;
        }
    }

    private void attemptPottery(GameObject wheel) {
        String material = currentItem.materials.keySet().iterator().next();
        if (Rs2Inventory.interact(material) && Rs2GameObject.interact(wheel)) {
            Microbot.status = "Crafting: Making " + currentItem.name;

            // Wait for pottery interface and select item
            if (waitForCraftingInterface()) {
                Rs2Widget.clickWidget(currentItem.name);
                waitForCrafting();
            }
        }
    }

    private void attemptJewelry(GameObject furnace) {
        String goldBar = "Gold bar";
        if (Rs2Inventory.interact(goldBar) && Rs2GameObject.interact(furnace)) {
            Microbot.status = "Crafting: Making " + currentItem.name;

            // Wait for jewelry interface and select item
            if (waitForCraftingInterface()) {
                Rs2Widget.clickWidget(currentItem.name);
                waitForCrafting();
            }
        }
    }

    private void attemptLeather() {
        // Use needle on leather for leather crafting
        String leather = currentItem.materials.keySet().iterator().next();
        if (Rs2Inventory.combine("Needle", leather)) {
            Microbot.status = "Crafting: Making " + currentItem.name;

            // Wait for crafting interface and select item
            if (waitForCraftingInterface()) {
                Rs2Widget.clickWidget(currentItem.name);
                waitForCrafting();
            }
        }
    }

    private void attemptGlassmaking() {
        // Use glassblowing pipe on molten glass
        if (Rs2Inventory.combine("Glassblowing pipe", "Molten glass")) {
            Microbot.status = "Crafting: Making " + currentItem.name;

            // Wait for glassmaking interface and select item
            if (waitForCraftingInterface()) {
                Rs2Widget.clickWidget(currentItem.name);
                waitForCrafting();
            }
        }
    }

    private void attemptSpinning(GameObject wheel) {
        String material = currentItem.materials.keySet().iterator().next();
        if (Rs2Inventory.interact(material) && Rs2GameObject.interact(wheel)) {
            Microbot.status = "Crafting: Making " + currentItem.name;
            waitForCrafting();
        }
    }

    private boolean waitForCraftingInterface() {
        return Rs2Widget.isWidgetVisible(270, 14); // replaced incorrect hasWidget usage
    }

    private void waitForCrafting() {
        // Wait while player is crafting
        while (Rs2Player.isAnimating() && hasMaterials()) {
            sleep(1000, 2000);
        }
    }

    private void bankProducts() {
        List<String> itemsToBank = new ArrayList<>();
        for (CraftingItem item : CRAFTING_ITEMS) {
            itemsToBank.add(item.name);
        }

        if (!Rs2Bank.bankItemsAndWalkBackToOriginalPosition(itemsToBank, currentLocation.location, 0, 15)) {
            Microbot.status = "Crafting: Banking failed, trying again...";
        }
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
    public CraftingItem getCurrentItem() { return currentItem; }
    public CraftingLocation getCurrentLocation() { return currentLocation; }
    public CraftingMethod getCurrentMethod() { return currentMethod; }
}