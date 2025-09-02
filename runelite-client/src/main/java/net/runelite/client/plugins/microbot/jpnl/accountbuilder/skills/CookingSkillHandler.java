package net.runelite.client.plugins.microbot.jpnl.accountbuilder.skills;

import lombok.Getter;
import net.runelite.api.GameObject;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.ObjectComposition; // added
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.settings.SkillRuntimeSettings;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.util.*;

/**
 * Complete Cooking skill handler for AIO bot system.
 * Supports cooking fish, meat, and other food items.
 */
@Getter
public class CookingSkillHandler implements SkillHandler {

    // Getters for debugging
    private boolean enabled = true;
    private String mode = "auto"; // auto, shrimp, trout, lobster, etc.
    private CookingLocation currentLocation;
    private CookingFood currentFood;

    // Cooking data structures
    private static class CookingFood {
        String rawName;
        String cookedName;
        int levelRequired;
        int experience;
        int stopBurnLevel;

        public CookingFood(String rawName, String cookedName, int levelRequired, int experience, int stopBurnLevel) {
            this.rawName = rawName;
            this.cookedName = cookedName;
            this.levelRequired = levelRequired;
            this.experience = experience;
            this.stopBurnLevel = stopBurnLevel;
        }
    }

    // Cooking foods
    private static final List<CookingFood> COOKING_FOODS = Arrays.asList(
        new CookingFood("Raw shrimp", "Shrimp", 1, 30, 35),
        new CookingFood("Raw sardine", "Sardine", 1, 40, 38),
        new CookingFood("Raw herring", "Herring", 5, 50, 41),
        new CookingFood("Raw anchovies", "Anchovies", 1, 30, 34),
        new CookingFood("Raw mackerel", "Mackerel", 10, 60, 45),
        new CookingFood("Raw trout", "Trout", 15, 70, 50),
        new CookingFood("Raw cod", "Cod", 18, 75, 52),
        new CookingFood("Raw pike", "Pike", 20, 80, 55),
        new CookingFood("Raw salmon", "Salmon", 25, 90, 58),
        new CookingFood("Raw tuna", "Tuna", 30, 100, 63),
        new CookingFood("Raw lobster", "Lobster", 40, 120, 74),
        new CookingFood("Raw bass", "Bass", 46, 130, 80),
        new CookingFood("Raw swordfish", "Swordfish", 45, 140, 86),
        new CookingFood("Raw monkfish", "Monkfish", 62, 150, 92),
        new CookingFood("Raw shark", "Shark", 80, 210, 99),
        new CookingFood("Raw anglerfish", "Anglerfish", 84, 230, 99)
    );

    private static class CookingLocation {
        String name;
        WorldPoint location;
        String cookingObject;
        boolean hasBank;
        boolean isRange; // true for range, false for fire

        public CookingLocation(String name, WorldPoint location, String cookingObject, boolean hasBank, boolean isRange) {
            this.name = name;
            this.location = location;
            this.cookingObject = cookingObject;
            this.hasBank = hasBank;
            this.isRange = isRange;
        }
    }

    // Cooking locations
    private static final List<CookingLocation> COOKING_LOCATIONS = Arrays.asList(
        new CookingLocation("Lumbridge", new WorldPoint(3209, 3214, 0), "Range", true, true),
        new CookingLocation("Varrock", new WorldPoint(3208, 3370, 0), "Range", true, true),
        new CookingLocation("Edgeville", new WorldPoint(3108, 3500, 0), "Range", true, true),
        new CookingLocation("Al Kharid", new WorldPoint(3273, 3180, 0), "Range", true, true),
        new CookingLocation("Catherby", new WorldPoint(2817, 3441, 0), "Range", true, true),
        new CookingLocation("Rogues Den", new WorldPoint(3043, 4973, 1), "Fire", true, false),
        new CookingLocation("Hosidius House", new WorldPoint(1676, 3621, 0), "Clay oven", true, true),
        new CookingLocation("Cooking Guild", new WorldPoint(1720, 3507, 0), "Range", true, true)
    );

    @Override
    public void applySettings(SkillRuntimeSettings settings) {
        if (settings != null) {
            enabled = settings.isEnabled();
            mode = settings.getMode() != null ? settings.getMode().toLowerCase() : "auto";
        }
    }

    @Override
    public void execute() {
        if (!enabled) {
            Microbot.status = "Cooking: disabled";
            return;
        }

        if (!Microbot.isLoggedIn()) return;

        // Determine current food and location
        currentFood = determineCurrentFood();
        currentLocation = determineCurrentLocation();

        if (currentFood == null || currentLocation == null) {
            Microbot.status = "Cooking: no suitable setup for level " + Microbot.getClient().getRealSkillLevel(Skill.COOKING);
            return;
        }

        // Check if we have raw food to cook
        if (!hasRawFood()) {
            getRawFood();
            return;
        }

        // Check if we're at the cooking location
        if (!isAtLocation()) {
            walkToLocation();
            return;
        }

        // Perform cooking
        performCooking();
    }

    private CookingFood determineCurrentFood() {
        int cookingLevel = Microbot.getClient().getRealSkillLevel(Skill.COOKING);

        if ("auto".equals(mode)) {
            // Find highest level food we can cook
            CookingFood bestFood = null;
            for (CookingFood food : COOKING_FOODS) {
                if (cookingLevel >= food.levelRequired) {
                    bestFood = food;
                }
            }
            return bestFood;
        } else {
            // Try to find specific food
            for (CookingFood food : COOKING_FOODS) {
                if (food.rawName.toLowerCase().contains(mode) || food.cookedName.toLowerCase().contains(mode)) {
                    return cookingLevel >= food.levelRequired ? food : null;
                }
            }
            return COOKING_FOODS.get(0); // Fallback to shrimp
        }
    }

    private CookingLocation determineCurrentLocation() {
        // Prefer locations with ranges and banks
        return COOKING_LOCATIONS.stream()
                .filter(loc -> loc.hasBank && loc.isRange)
                .findFirst()
                .orElse(COOKING_LOCATIONS.get(0));
    }

    private boolean hasRawFood() {
        return currentFood != null && Rs2Inventory.hasItem(currentFood.rawName);
    }

    private void getRawFood() {
        if (!Rs2Bank.openBank()) return;

        Microbot.status = "Cooking: Getting " + currentFood.rawName;

        // Bank any cooked food first
        for (CookingFood food : COOKING_FOODS) {
            if (Rs2Inventory.hasItem(food.cookedName)) { // inventory check ok
                Rs2Bank.depositAll(food.cookedName); // replaced deprecated/non-existent depositItem
            }
        }

        // Withdraw raw food
        Rs2Bank.withdrawX(true, currentFood.rawName, 28);
        Rs2Bank.closeBank();
    }

    private boolean isAtLocation() {
        return currentLocation != null &&
               Rs2Player.getWorldLocation().distanceTo(currentLocation.location) <= 10;
    }

    private void walkToLocation() {
        if (currentLocation == null) return;

        Microbot.status = "Cooking: Walking to " + currentLocation.name;
        Rs2Walker.walkTo(currentLocation.location);
    }

    private void performCooking() {
        if (!Rs2Inventory.hasItem(currentFood.rawName)) {
            Microbot.status = "Cooking: No " + currentFood.rawName + " to cook";
            return;
        }

        // Find cooking object using modern API with ObjectComposition
        List<GameObject> cookingObjects = Rs2GameObject.getGameObjects(obj -> {
            ObjectComposition comp = Rs2GameObject.convertToObjectComposition(obj);
            String name = comp != null ? comp.getName() : null;
            return name != null && name.equals(currentLocation.cookingObject) &&
                    obj.getWorldLocation().distanceTo(currentLocation.location) <= 10;
        });

        if (!cookingObjects.isEmpty()) {
            GameObject cookingObject = cookingObjects.get(0);
            // Manually perform use item on object (previous helper missing)
            if (Rs2Inventory.interact(currentFood.rawName)) { // select item (Use)
                if (Rs2GameObject.interact(cookingObject)) {
                    Microbot.status = "Cooking: Cooking " + currentFood.rawName;

                    // Wait for interface and select amount
                    sleep(2000, 3000);
                    if (Rs2Widget.isWidgetVisible(270, 14)) { // Cooking interface Make/Cook All button
                        Rs2Widget.clickWidget(270, 14);
                    }

                    // Wait for cooking to complete
                    Rs2Player.waitForXpDrop(Skill.COOKING, true);
                    Rs2Antiban.actionCooldown();
                }
            }
        }
    }

    private void sleep(int min, int max) {
        try {
            Thread.sleep(min + (int) (Math.random() * (max - min)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
