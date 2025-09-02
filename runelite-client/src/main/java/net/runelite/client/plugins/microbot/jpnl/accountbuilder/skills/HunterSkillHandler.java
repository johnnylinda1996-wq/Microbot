package net.runelite.client.plugins.microbot.jpnl.accountbuilder.skills;

import net.runelite.api.*;
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
 * Complete Hunter skill handler for AIO bot system.
 * Supports birds, box trapping, net hunting, and falconry.
 */
public class HunterSkillHandler implements SkillHandler {

    private boolean enabled = true;
    private String mode = "auto"; // auto, birds, chinchompas, salamanders, falconry
    private HunterMethod currentMethod;
    private WorldPoint hunterLocation;
    private int failCount = 0;
    private static final int MAX_FAIL_COUNT = 5;

    // Hunter data structures
    private enum HunterMethod {
        BIRDS, BOX_TRAPPING, NET_HUNTING, FALCONRY
    }

    private static class HunterTarget {
        String name;
        int levelRequired;
        HunterMethod method;
        WorldPoint location;
        String trapType;
        String bait;
        String tool;
        int experience;
        String loot;

        public HunterTarget(String name, int levelRequired, HunterMethod method, WorldPoint location,
                          String trapType, String bait, String tool, int experience, String loot) {
            this.name = name;
            this.levelRequired = levelRequired;
            this.method = method;
            this.location = location;
            this.trapType = trapType;
            this.bait = bait;
            this.tool = tool;
            this.experience = experience;
            this.loot = loot;
        }
    }

    // Hunter targets database
    private static final List<HunterTarget> HUNTER_TARGETS = Arrays.asList(
            // Birds
            new HunterTarget("Crimson swift", 1, HunterMethod.BIRDS, new WorldPoint(2564, 2915, 0), "Bird snare", "", "Bird snare", 34, "Raw bird meat"),
            new HunterTarget("Golden warbler", 5, HunterMethod.BIRDS, new WorldPoint(2564, 2915, 0), "Bird snare", "", "Bird snare", 48, "Raw bird meat"),
            new HunterTarget("Copper longtail", 9, HunterMethod.BIRDS, new WorldPoint(2564, 2915, 0), "Bird snare", "", "Bird snare", 61, "Raw bird meat"),
            new HunterTarget("Cerulean twitch", 11, HunterMethod.BIRDS, new WorldPoint(2564, 2915, 0), "Bird snare", "", "Bird snare", 65, "Raw bird meat"),
            new HunterTarget("Tropical wagtail", 19, HunterMethod.BIRDS, new WorldPoint(2564, 2915, 0), "Bird snare", "", "Bird snare", 95, "Raw bird meat"),

            // Box trapping
            new HunterTarget("Ferret", 27, HunterMethod.BOX_TRAPPING, new WorldPoint(2564, 2915, 0), "Box trap", "", "Box trap", 115, "Ferret"),
            new HunterTarget("Chinchompa", 53, HunterMethod.BOX_TRAPPING, new WorldPoint(2532, 2911, 0), "Box trap", "", "Box trap", 198, "Chinchompa"),
            new HunterTarget("Red chinchompa", 63, HunterMethod.BOX_TRAPPING, new WorldPoint(2556, 2883, 0), "Box trap", "", "Box trap", 265, "Red chinchompa"),
            new HunterTarget("Black chinchompa", 73, HunterMethod.BOX_TRAPPING, new WorldPoint(3147, 3777, 0), "Box trap", "", "Box trap", 315, "Black chinchompa"),

            // Net hunting
            new HunterTarget("Swamp lizard", 29, HunterMethod.NET_HUNTING, new WorldPoint(3537, 3448, 0), "Small fishing net", "Guam leaf", "Rope", 152, "Swamp lizard"),
            new HunterTarget("Orange salamander", 47, HunterMethod.NET_HUNTING, new WorldPoint(2564, 2915, 0), "Small fishing net", "Marrentill", "Rope", 224, "Orange salamander"),
            new HunterTarget("Red salamander", 59, HunterMethod.NET_HUNTING, new WorldPoint(2705, 3712, 0), "Small fishing net", "Tarromin", "Rope", 272, "Red salamander"),
            new HunterTarget("Black salamander", 67, HunterMethod.NET_HUNTING, new WorldPoint(3362, 3924, 0), "Small fishing net", "Harralander", "Rope", 319, "Black salamander"),

            // Falconry
            new HunterTarget("Spotted kebbit", 43, HunterMethod.FALCONRY, new WorldPoint(2369, 3619, 0), "Gyr falcon", "", "Falconer's glove", 104, "Spotted kebbit fur"),
            new HunterTarget("Dark kebbit", 57, HunterMethod.FALCONRY, new WorldPoint(2369, 3619, 0), "Gyr falcon", "", "Falconer's glove", 132, "Dark kebbit fur"),
            new HunterTarget("Dashing kebbit", 69, HunterMethod.FALCONRY, new WorldPoint(2369, 3619, 0), "Gyr falcon", "", "Falconer's glove", 156, "Dashing kebbit fur")
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
                Microbot.status = "Hunter: disabled";
                return;
            }

            if (!Microbot.isLoggedIn()) return;

            // Determine current hunter target
            HunterTarget currentTarget = determineCurrentTarget();
            if (currentTarget == null) {
                handleFailure("No suitable hunter target for level " + Microbot.getClient().getRealSkillLevel(Skill.HUNTER));
                return;
            }

            currentMethod = currentTarget.method;
            hunterLocation = currentTarget.location;

            // Check if we need equipment
            if (!hasRequiredEquipment(currentTarget)) {
                getRequiredEquipment(currentTarget);
                return;
            }

            // Check if we need to bank loot
            if (shouldBank()) {
                bankLoot();
                return;
            }

            // Check if we're at the hunting location
            if (!isAtHuntingLocation()) {
                walkToHuntingLocation();
                return;
            }

            // Execute hunting method
            switch (currentMethod) {
                case BIRDS:
                    huntBirds();
                    break;
                case BOX_TRAPPING:
                    huntBoxTrapping();
                    break;
                case NET_HUNTING:
                    huntNetHunting(currentTarget);
                    break;
                case FALCONRY:
                    huntFalconry(currentTarget);
                    break;
            }

            // Reset fail count on successful execution
            failCount = 0;

        } catch (Exception e) {
            handleFailure("Hunter error: " + e.getMessage());
        }
    }

    private HunterTarget determineCurrentTarget() {
        int hunterLevel = Microbot.getClient().getRealSkillLevel(Skill.HUNTER);

        if ("auto".equals(mode)) {
            // Find highest level target we can hunt
            HunterTarget bestTarget = null;
            for (HunterTarget target : HUNTER_TARGETS) {
                if (hunterLevel >= target.levelRequired) {
                    bestTarget = target;
                }
            }
            return bestTarget;
        } else {
            // Try to find specific target
            for (HunterTarget target : HUNTER_TARGETS) {
                if (target.name.toLowerCase().contains(mode) && hunterLevel >= target.levelRequired) {
                    return target;
                }
            }
            return null;
        }
    }

    private boolean hasRequiredEquipment(HunterTarget target) {
        switch (target.method) {
            case BIRDS:
                return Rs2Inventory.hasItem("Bird snare");
            case BOX_TRAPPING:
                return Rs2Inventory.hasItem("Box trap");
            case NET_HUNTING:
                return Rs2Inventory.hasItem("Small fishing net") &&
                       Rs2Inventory.hasItem("Rope") &&
                       Rs2Inventory.hasItem(target.bait);
            case FALCONRY:
                return Rs2Inventory.hasItem("Falconer's glove");
            default:
                return false;
        }
    }

    private void getRequiredEquipment(HunterTarget target) {
        if (!Rs2Bank.openBank()) return;

        Microbot.status = "Hunter: Getting equipment for " + target.name;

        switch (target.method) {
            case BIRDS:
                if (!Rs2Inventory.hasItem("Bird snare")) {
                    Rs2Bank.withdrawX(true, "Bird snare", 5);
                }
                break;
            case BOX_TRAPPING:
                if (!Rs2Inventory.hasItem("Box trap")) {
                    Rs2Bank.withdrawX(true, "Box trap", 5);
                }
                break;
            case NET_HUNTING:
                if (!Rs2Inventory.hasItem("Small fishing net")) {
                    Rs2Bank.withdrawItem(true, "Small fishing net");
                }
                if (!Rs2Inventory.hasItem("Rope")) {
                    Rs2Bank.withdrawX(true, "Rope", 5);
                }
                if (!Rs2Inventory.hasItem(target.bait)) {
                    Rs2Bank.withdrawX(true, target.bait, 20);
                }
                break;
            case FALCONRY:
                if (!Rs2Inventory.hasItem("Falconer's glove")) {
                    Rs2Bank.withdrawItem(true, "Falconer's glove");
                }
                break;
        }

        Rs2Bank.closeBank();
    }

    private boolean shouldBank() {
        // Bank when inventory is nearly full
        return Rs2Inventory.emptySlotCount() <= 2;
    }

    private void bankLoot() {
        List<String> itemsToBank = Arrays.asList("Raw bird meat", "Bones", "Feather", "Ferret",
                "Chinchompa", "Red chinchompa", "Black chinchompa", "Swamp lizard", "Orange salamander",
                "Red salamander", "Black salamander", "Spotted kebbit fur", "Dark kebbit fur", "Dashing kebbit fur");

        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        if (!Rs2Bank.bankItemsAndWalkBackToOriginalPosition(itemsToBank, playerLocation, 0, 15)) {
            Microbot.status = "Hunter: Banking failed, trying again...";
        }
    }

    private boolean isAtHuntingLocation() {
        return hunterLocation != null && Rs2Player.getWorldLocation().distanceTo(hunterLocation) <= 20;
    }

    private void walkToHuntingLocation() {
        if (hunterLocation == null) return;

        Microbot.status = "Hunter: Walking to hunting location";
        Rs2Walker.walkTo(hunterLocation);
    }

    private void huntBirds() {
        // Check for trapped birds first
        List<GameObject> traps = Rs2GameObject.getGameObjects(obj -> {
            ObjectComposition composition = Rs2GameObject.convertToObjectComposition(obj);
            String objName = composition != null ? composition.getName() : null;
            return objName != null && objName.equals("Bird snare") &&
                   hunterLocation != null && obj.getWorldLocation().distanceTo(hunterLocation) <= 15;
        });

        if (!traps.isEmpty()) {
            GameObject trap = traps.get(0);
            if (Rs2GameObject.interact(trap, "Check")) {
                Microbot.status = "Hunter: Checking bird snare";
                sleep(2000, 3000);
                return;
            }
        }

        // Lay new snares if we have them
        if (Rs2Inventory.hasItem("Bird snare")) {
            // Find a good spot to lay snare
            WorldPoint snareLocation = findNearbyTile(hunterLocation, 10);
            if (snareLocation != null) {
                Rs2Walker.walkTo(snareLocation);
                sleep(1000, 2000);

                if (Rs2Inventory.interact("Bird snare", "Lay")) {
                    Microbot.status = "Hunter: Laying bird snare";
                    sleep(2000, 3000);
                }
            }
        }
    }

    private void huntBoxTrapping() {
        // Check for caught creatures in existing traps
        List<GameObject> shakingTraps = Rs2GameObject.getGameObjects(obj -> {
            ObjectComposition composition = Rs2GameObject.convertToObjectComposition(obj);
            String objName = composition != null ? composition.getName() : null;
            return objName != null && objName.equals("Shaking box") &&
                   hunterLocation != null && obj.getWorldLocation().distanceTo(hunterLocation) <= 15;
        });

        if (!shakingTraps.isEmpty()) {
            GameObject shakingTrap = shakingTraps.get(0);
            if (Rs2GameObject.interact(shakingTrap, "Check")) {
                Microbot.status = "Hunter: Checking shaking box trap";
                sleep(2000, 3000);
                return;
            }
        }

        // Check for failed traps
        List<GameObject> failedTraps = Rs2GameObject.getGameObjects(obj -> {
            ObjectComposition composition = Rs2GameObject.convertToObjectComposition(obj);
            String objName = composition != null ? composition.getName() : null;
            return objName != null && objName.equals("Box trap") &&
                   hunterLocation != null && obj.getWorldLocation().distanceTo(hunterLocation) <= 15;
        });

        if (!failedTraps.isEmpty()) {
            GameObject failedTrap = failedTraps.get(0);
            if (Rs2GameObject.interact(failedTrap, "Reset")) {
                Microbot.status = "Hunter: Resetting box trap";
                sleep(2000, 3000);
                return;
            }
        }

        // Lay new box traps if we have them and don't have max deployed
        if (Rs2Inventory.hasItem("Box trap") && countDeployedTraps() < getMaxTraps()) {
            WorldPoint trapLocation = findNearbyTile(hunterLocation, 15);
            if (trapLocation != null) {
                Rs2Walker.walkTo(trapLocation);
                sleep(1000, 2000);

                if (Rs2Inventory.interact("Box trap", "Lay")) {
                    Microbot.status = "Hunter: Laying box trap";
                    sleep(2000, 3000);
                }
            }
        }
    }

    private void huntNetHunting(HunterTarget target) {
        // Look for salamanders/lizards to catch
        Rs2NpcModel creature = Rs2Npc.getNpc(target.name);
        if (creature != null) {
            if (Rs2Npc.interact(creature, "Net")) {
                Microbot.status = "Hunter: Netting " + target.name;
                sleep(3000, 5000);
            }
        }
    }

    private void huntFalconry(HunterTarget target) {
        // Look for kebbits to catch with falcon
        Rs2NpcModel kebbit = Rs2Npc.getNpc(target.name);
        if (kebbit != null) {
            if (Rs2Npc.interact(kebbit, "Catch")) {
                Microbot.status = "Hunter: Catching " + target.name + " with falcon";
                sleep(3000, 5000);
            }
        }
    }

    private int countDeployedTraps() {
        // Count visible traps near hunting location
        int count = 0;
        if (hunterLocation != null) {
            List<GameObject> allTraps = Rs2GameObject.getGameObjects(obj -> {
                ObjectComposition composition = Rs2GameObject.convertToObjectComposition(obj);
                String objName = composition != null ? composition.getName() : null;
                return objName != null &&
                       (objName.equals("Box trap") || objName.equals("Shaking box") || objName.equals("Bird snare")) &&
                       obj.getWorldLocation().distanceTo(hunterLocation) <= 20;
            });
            count = allTraps.size();
        }
        return count;
    }

    private int getMaxTraps() {
        int hunterLevel = Microbot.getClient().getRealSkillLevel(Skill.HUNTER);
        // Base 1 trap + 1 per 20 levels
        return 1 + (hunterLevel / 20);
    }

    private WorldPoint findNearbyTile(WorldPoint center, int radius) {
        // Find a walkable tile near the center for placing traps
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                WorldPoint candidate = new WorldPoint(center.getX() + x, center.getY() + y, center.getPlane());
                if (Rs2Walker.canReach(candidate)) {
                    return candidate;
                }
            }
        }
        return center; // Fallback to center
    }

    private void handleFailure(String message) {
        failCount++;
        Microbot.status = "Hunter: " + message + " (Fails: " + failCount + "/" + MAX_FAIL_COUNT + ")";

        if (failCount >= MAX_FAIL_COUNT) {
            Microbot.status = "Hunter: Max failures reached, moving to next task";
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

    // Getters for debugging
    public boolean isEnabled() { return enabled; }
    public String getMode() { return mode; }
}
