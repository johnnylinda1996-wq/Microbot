package net.runelite.client.plugins.microbot.jpnl.accountbuilder.skills;

import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.settings.SkillRuntimeSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.*;

/**
 * Complete Thieving skill handler for AIO bot system.
 * Supports pickpocketing NPCs and stall thieving with food management.
 */
public class ThievingSkillHandler implements SkillHandler {

    private boolean enabled = true;
    private String mode = "auto"; // auto, pickpocket, stalls
    private ThievingTarget currentTarget;
    private int failCount = 0;
    private static final int MAX_FAIL_COUNT = 5;
    private long lastThievingAttempt = 0;

    // Thieving data structures
    private enum ThievingMethod {
        PICKPOCKET, STALLS
    }

    private static class ThievingTarget {
        String name;
        int levelRequired;
        ThievingMethod method;
        WorldPoint location;
        String targetName;
        String action;
        int experience;
        int damage; // Damage taken on failure
        String loot;

        public ThievingTarget(String name, int levelRequired, ThievingMethod method, WorldPoint location,
                            String targetName, String action, int experience, int damage, String loot) {
            this.name = name;
            this.levelRequired = levelRequired;
            this.method = method;
            this.location = location;
            this.targetName = targetName;
            this.action = action;
            this.experience = experience;
            this.damage = damage;
            this.loot = loot;
        }
    }

    // Thieving targets database
    private static final List<ThievingTarget> THIEVING_TARGETS = Arrays.asList(
            // Pickpocketing NPCs
            new ThievingTarget("Man", 1, ThievingMethod.PICKPOCKET, new WorldPoint(3208, 3424, 0), "Man", "Pickpocket", 8, 1, "Coins"),
            new ThievingTarget("Woman", 1, ThievingMethod.PICKPOCKET, new WorldPoint(3208, 3424, 0), "Woman", "Pickpocket", 8, 1, "Coins"),
            new ThievingTarget("Farmer", 10, ThievingMethod.PICKPOCKET, new WorldPoint(3078, 3249, 0), "Farmer", "Pickpocket", 15, 1, "Coins, Seeds"),
            new ThievingTarget("Female HAM member", 15, ThievingMethod.PICKPOCKET, new WorldPoint(3166, 9625, 0), "Female HAM member", "Pickpocket", 18, 3, "HAM clothing"),
            new ThievingTarget("Male HAM member", 20, ThievingMethod.PICKPOCKET, new WorldPoint(3166, 9625, 0), "Male HAM member", "Pickpocket", 22, 3, "HAM clothing"),
            new ThievingTarget("Warrior", 25, ThievingMethod.PICKPOCKET, new WorldPoint(2659, 3302, 0), "Warrior", "Pickpocket", 26, 2, "Coins"),
            new ThievingTarget("Rogue", 32, ThievingMethod.PICKPOCKET, new WorldPoint(3282, 3192, 0), "Rogue", "Pickpocket", 36, 2, "Coins, Lockpick"),
            new ThievingTarget("Master Farmer", 38, ThievingMethod.PICKPOCKET, new WorldPoint(3078, 3249, 0), "Master Farmer", "Pickpocket", 43, 3, "Seeds"),
            new ThievingTarget("Guard", 40, ThievingMethod.PICKPOCKET, new WorldPoint(3268, 3430, 0), "Guard", "Pickpocket", 47, 2, "Coins"),
            new ThievingTarget("Fremennik citizen", 45, ThievingMethod.PICKPOCKET, new WorldPoint(2658, 3672, 0), "Fremennik citizen", "Pickpocket", 65, 2, "Coins"),
            new ThievingTarget("Desert bandit", 53, ThievingMethod.PICKPOCKET, new WorldPoint(3176, 2988, 0), "Desert bandit", "Pickpocket", 80, 3, "Coins"),
            new ThievingTarget("Knight of Ardougne", 55, ThievingMethod.PICKPOCKET, new WorldPoint(2654, 3283, 0), "Knight of Ardougne", "Pickpocket", 84, 3, "Coins"),
            new ThievingTarget("Pollnivian bandit", 55, ThievingMethod.PICKPOCKET, new WorldPoint(3359, 2974, 0), "Pollnivian bandit", "Pickpocket", 84, 5, "Coins"),
            new ThievingTarget("Yanille Watchman", 65, ThievingMethod.PICKPOCKET, new WorldPoint(2544, 3096, 0), "Yanille Watchman", "Pickpocket", 138, 5, "Coins"),
            new ThievingTarget("Menaphite Thug", 65, ThievingMethod.PICKPOCKET, new WorldPoint(3359, 2974, 0), "Menaphite Thug", "Pickpocket", 138, 5, "Coins"),
            new ThievingTarget("Paladin", 70, ThievingMethod.PICKPOCKET, new WorldPoint(2654, 3283, 0), "Paladin", "Pickpocket", 152, 7, "Coins, Chaos runes"),
            new ThievingTarget("Hero", 80, ThievingMethod.PICKPOCKET, new WorldPoint(2654, 3283, 0), "Hero", "Pickpocket", 273, 4, "Coins, Gems"),

            // Stalls
            new ThievingTarget("Vegetable stall", 2, ThievingMethod.STALLS, new WorldPoint(3078, 3249, 0), "Vegetable stall", "Steal-from", 10, 0, "Vegetables"),
            new ThievingTarget("Tea stall", 5, ThievingMethod.STALLS, new WorldPoint(3270, 3180, 0), "Tea stall", "Steal-from", 16, 0, "Cup of tea"),
            new ThievingTarget("Fruit stall", 25, ThievingMethod.STALLS, new WorldPoint(2667, 3310, 0), "Fruit stall", "Steal-from", 28, 0, "Fruit"),
            new ThievingTarget("Fish stall", 42, ThievingMethod.STALLS, new WorldPoint(2667, 3310, 0), "Fish stall", "Steal-from", 42, 0, "Fish"),
            new ThievingTarget("Crossbow stall", 49, ThievingMethod.STALLS, new WorldPoint(2667, 3310, 0), "Crossbow stall", "Steal-from", 52, 0, "Crossbow parts"),
            new ThievingTarget("Silver stall", 50, ThievingMethod.STALLS, new WorldPoint(2667, 3310, 0), "Silver stall", "Steal-from", 54, 0, "Silver ore"),
            new ThievingTarget("Spice stall", 65, ThievingMethod.STALLS, new WorldPoint(2667, 3310, 0), "Spice stall", "Steal-from", 81, 0, "Spices"),
            new ThievingTarget("Magic stall", 65, ThievingMethod.STALLS, new WorldPoint(2667, 3310, 0), "Magic stall", "Steal-from", 100, 0, "Runes"),
            new ThievingTarget("Gem stall", 75, ThievingMethod.STALLS, new WorldPoint(2667, 3310, 0), "Gem stall", "Steal-from", 160, 0, "Gems")
    );

    @Override
    public void applySettings(SkillRuntimeSettings settings) {
        if (settings != null) {
            enabled = settings.isEnabled();
            mode = settings.getMode() != null ? settings.getMode().toLowerCase() : "auto";
            failCount = 0; // Reset fail count when settings change
        }
    }

    @Override
    public void execute() {
        try {
            if (!enabled) {
                Microbot.status = "Thieving: disabled";
                return;
            }

            if (!Microbot.isLoggedIn()) return;

            // Determine current target
            currentTarget = determineCurrentTarget();
            if (currentTarget == null) {
                handleFailure("No suitable thieving target for level " + Microbot.getClient().getRealSkillLevel(Skill.THIEVING));
                return;
            }

            // Check if we need food for pickpocketing
            if (currentTarget.method == ThievingMethod.PICKPOCKET && needsFood()) {
                getFood();
                return;
            }

            // Check if we need to bank loot
            if (Rs2Inventory.isFull()) {
                bankLoot();
                return;
            }

            // Check if we're at the thieving location
            if (!isAtThievingLocation()) {
                walkToThievingLocation();
                return;
            }

            // Eat food if low HP (for pickpocketing)
            if (currentTarget.method == ThievingMethod.PICKPOCKET && shouldEatFood()) {
                eatFood();
                return;
            }

            // Do thieving
            attemptThieving();

            // Reset fail count on successful execution
            failCount = 0;

        } catch (Exception e) {
            handleFailure("Thieving error: " + e.getMessage());
        }
    }

    private ThievingTarget determineCurrentTarget() {
        int thievingLevel = Microbot.getClient().getRealSkillLevel(Skill.THIEVING);

        if ("auto".equals(mode)) {
            // Find highest level target we can thieve
            ThievingTarget bestTarget = null;
            for (ThievingTarget target : THIEVING_TARGETS) {
                if (thievingLevel >= target.levelRequired) {
                    bestTarget = target;
                }
            }
            return bestTarget;
        } else {
            // Try to find specific target
            for (ThievingTarget target : THIEVING_TARGETS) {
                if (target.name.toLowerCase().contains(mode) && thievingLevel >= target.levelRequired) {
                    return target;
                }
            }
            return null;
        }
    }

    private boolean needsFood() {
        // If we have damage-dealing thieving and low HP, we need food
        if (currentTarget != null && currentTarget.damage > 0) {
            int currentHp = Microbot.getClient().getBoostedSkillLevel(Skill.HITPOINTS);
            int maxHp = Microbot.getClient().getRealSkillLevel(Skill.HITPOINTS);
            return currentHp < maxHp * 0.4 && !hasFood();
        }
        return false;
    }

    private boolean hasFood() {
        return Rs2Inventory.hasItem("Salmon", "Trout", "Lobster", "Swordfish", "Monkfish", "Shark");
    }

    private void getFood() {
        if (!Rs2Bank.openBank()) return;

        Microbot.status = "Thieving: Getting food";

        // Get food based on what's available
        String[] foodOptions = {"Shark", "Monkfish", "Swordfish", "Lobster", "Salmon", "Trout"};
        for (String food : foodOptions) {
            if (Rs2Bank.hasItem(food)) {
                Rs2Bank.withdrawX(true, food, 10);
                break;
            }
        }

        Rs2Bank.closeBank();
    }

    private boolean shouldEatFood() {
        if (currentTarget != null && currentTarget.damage > 0) {
            int currentHp = Microbot.getClient().getBoostedSkillLevel(Skill.HITPOINTS);
            int maxHp = Microbot.getClient().getRealSkillLevel(Skill.HITPOINTS);
            return currentHp < maxHp * 0.6;
        }
        return false;
    }

    private void eatFood() {
        String[] foodOptions = {"Shark", "Monkfish", "Swordfish", "Lobster", "Salmon", "Trout"};
        for (String food : foodOptions) {
            if (Rs2Inventory.hasItem(food)) {
                Rs2Inventory.interact(food, "Eat");
                Microbot.status = "Thieving: Eating " + food;
                sleep(1000, 2000);
                break;
            }
        }
    }

    private boolean isAtThievingLocation() {
        if (currentTarget == null) return false;
        return Rs2Player.getWorldLocation().distanceTo(currentTarget.location) <= 15;
    }

    private void walkToThievingLocation() {
        if (currentTarget == null) return;

        Microbot.status = "Thieving: Walking to " + currentTarget.name;
        Rs2Walker.walkTo(currentTarget.location);
    }

    private void attemptThieving() {
        if (currentTarget == null) return;

        // Prevent spam clicking
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastThievingAttempt < 2000) {
            return;
        }

        switch (currentTarget.method) {
            case PICKPOCKET:
                attemptPickpocket();
                break;
            case STALLS:
                attemptStallThieving();
                break;
        }

        lastThievingAttempt = currentTime;
    }

    private void attemptPickpocket() {
        Rs2NpcModel target = Rs2Npc.getNpcs(npc -> npc.getName().equals(currentTarget.targetName))
                .findFirst()
                .orElse(null);

        if (target != null) {
            if (Rs2Npc.interact(target, currentTarget.action)) {
                Microbot.status = "Thieving: Pickpocketing " + currentTarget.targetName;
                sleep(1500, 2500);

                // Check if we got stunned and need to wait
                if (Rs2Player.isAnimating()) {
                    Microbot.status = "Thieving: Stunned, waiting...";
                    sleep(3000, 5000);
                }
            }
        } else {
            Microbot.status = "Thieving: No " + currentTarget.targetName + " found nearby";
        }
    }

    private void attemptStallThieving() {
        // For stalls, we need to find the game object using updated API
        GameObject stall = Rs2GameObject.getGameObject(currentTarget.targetName, currentTarget.location);

        if (stall != null) {
            if (Rs2GameObject.interact(stall, currentTarget.action)) {
                Microbot.status = "Thieving: Stealing from " + currentTarget.targetName;
                sleep(2000, 3000);
            }
        } else {
            Microbot.status = "Thieving: No " + currentTarget.targetName + " found nearby";
        }
    }

    private void bankLoot() {
        List<String> itemsToBank = Arrays.asList("Coins", "Lockpick", "HAM hood", "HAM shirt", "HAM robe",
                "HAM logo", "HAM gloves", "HAM boots", "HAM cloak", "Potato seed", "Onion seed", "Cabbage seed",
                "Tomato seed", "Sweetcorn seed", "Strawberry seed", "Watermelon seed", "Raw salmon", "Raw trout",
                "Raw cod", "Raw mackerel", "Sapphire", "Emerald", "Ruby", "Diamond", "Chaos rune", "Death rune",
                "Blood rune", "Nature rune", "Cabbage", "Onion", "Potato", "Cup of tea", "Silk", "Silver ore");

        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        if (!Rs2Bank.bankItemsAndWalkBackToOriginalPosition(itemsToBank, playerLocation, 0, 15)) {
            Microbot.status = "Thieving: Banking failed, trying again...";
        }
    }

    private void handleFailure(String message) {
        failCount++;
        Microbot.status = "Thieving: " + message + " (Fails: " + failCount + "/" + MAX_FAIL_COUNT + ")";

        if (failCount >= MAX_FAIL_COUNT) {
            Microbot.status = "Thieving: Max failures reached, moving to next task";
            enabled = false; // This will cause the queue to move to next task
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
    public ThievingTarget getCurrentTarget() { return currentTarget; }
    public int getFailCount() { return failCount; }
}