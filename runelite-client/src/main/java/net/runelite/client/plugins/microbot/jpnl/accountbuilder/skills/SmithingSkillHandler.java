package net.runelite.client.plugins.microbot.jpnl.accountbuilder.skills;

import net.runelite.api.GameObject;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.ObjectComposition; // added
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.settings.SkillRuntimeSettings;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.util.*;

/**
 * Complete Smithing skill handler for AIO bot system.
 * Supports smelting, anvil smithing, and blast furnace.
 */
public class SmithingSkillHandler implements SkillHandler {

    private boolean enabled = true;
    private String mode = "auto"; // auto, bronze, iron, steel, etc.
    private SmithingMethod currentMethod = SmithingMethod.SMELTING;
    private SmithingLocation currentLocation;
    private SmithingBar currentBar = SMITHING_BARS.get(0); // Use first bar as default

    // Smithing data structures
    private enum SmithingMethod {
        SMELTING, ANVIL, BLAST_FURNACE
    }

    private static class SmithingBar {
        String name;
        int levelRequired;
        String barName;
        Map<String, Integer> oreRequirements; // ore name -> amount needed
        boolean needsCoal;
        int coalAmount;

        public SmithingBar(String name, int levelRequired, String barName, Map<String, Integer> oreRequirements, int coalAmount) {
            this.name = name;
            this.levelRequired = levelRequired;
            this.barName = barName;
            this.oreRequirements = oreRequirements;
            this.coalAmount = coalAmount;
            this.needsCoal = coalAmount > 0;
        }
    }

    // Smithing bars
    public static final SmithingBar BRONZE = new SmithingBar("Bronze", 1, "Bronze bar",
        Map.of("Copper ore", 1, "Tin ore", 1), 0);
    public static final SmithingBar IRON = new SmithingBar("Iron", 15, "Iron bar",
        Map.of("Iron ore", 1), 0);
    public static final SmithingBar STEEL = new SmithingBar("Steel", 30, "Steel bar",
        Map.of("Iron ore", 1), 2);
    public static final SmithingBar MITHRIL = new SmithingBar("Mithril", 50, "Mithril bar",
        Map.of("Mithril ore", 1), 4);
    public static final SmithingBar ADAMANTITE = new SmithingBar("Adamantite", 70, "Adamantite bar",
        Map.of("Adamantite ore", 1), 6);
    public static final SmithingBar RUNITE = new SmithingBar("Runite", 85, "Runite bar",
        Map.of("Runite ore", 1), 8);

    private static final List<SmithingBar> SMITHING_BARS = Arrays.asList(
        BRONZE, IRON, STEEL, MITHRIL, ADAMANTITE, RUNITE
    );

    private static class SmithingLocation {
        String name;
        WorldPoint location;
        String furnaceObject;
        String anvilObject;
        boolean hasBank;
        boolean hasBlastFurnace;

        public SmithingLocation(String name, WorldPoint location, String furnaceObject, String anvilObject, boolean hasBank, boolean hasBlastFurnace) {
            this.name = name;
            this.location = location;
            this.furnaceObject = furnaceObject;
            this.anvilObject = anvilObject;
            this.hasBank = hasBank;
            this.hasBlastFurnace = hasBlastFurnace;
        }
    }

    // Smithing locations
    private static final List<SmithingLocation> SMITHING_LOCATIONS = Arrays.asList(
        new SmithingLocation("Edgeville", new WorldPoint(3108, 3500, 0), "Furnace", "Anvil", true, false),
        new SmithingLocation("Varrock", new WorldPoint(3189, 3437, 0), "Furnace", "Anvil", true, false),
        new SmithingLocation("Al Kharid", new WorldPoint(3276, 3185, 0), "Furnace", "Anvil", true, false),
        new SmithingLocation("Blast Furnace", new WorldPoint(1948, 4958, 0), "Blast Furnace", "Anvil", true, true),
        new SmithingLocation("Falador", new WorldPoint(2976, 3370, 0), "Furnace", "Anvil", true, false)
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
            Microbot.status = "Smithing: disabled";
            return;
        }

        if (!Microbot.isLoggedIn()) return;

        // Determine current bar and location
        currentBar = determineCurrentBar();
        currentLocation = determineCurrentLocation();

        if (currentBar == null || currentLocation == null) {
            Microbot.status = "Smithing: no suitable setup for level " + Microbot.getClient().getRealSkillLevel(Skill.SMITHING);
            return;
        }

        // Check if we have required materials
        if (!hasRequiredMaterials()) {
            getMaterials();
            return;
        }

        // Check if we're at the smithing location
        if (!isAtLocation()) {
            walkToLocation();
            return;
        }

        // Perform smithing based on current method
        switch (currentMethod) {
            case SMELTING:
                performSmelting();
                break;
            case ANVIL:
                performAnvilSmithing();
                break;
            case BLAST_FURNACE:
                performBlastFurnace();
                break;
        }
    }

    private SmithingBar determineCurrentBar() {
        int smithingLevel = Microbot.getClient().getRealSkillLevel(Skill.SMITHING);

        if ("auto".equals(mode)) {
            // Find highest level bar we can make
            SmithingBar bestBar = null;
            for (SmithingBar bar : SMITHING_BARS) {
                if (smithingLevel >= bar.levelRequired) {
                    bestBar = bar;
                }
            }
            return bestBar != null ? bestBar : SMITHING_BARS.get(0);
        } else {
            // Try to find specific bar
            for (SmithingBar bar : SMITHING_BARS) {
                if (bar.name.toLowerCase().contains(mode)) {
                    return smithingLevel >= bar.levelRequired ? bar : null;
                }
            }
            return SMITHING_BARS.get(0); // Fallback to bronze
        }
    }

    private SmithingLocation determineCurrentLocation() {
        // For now, prefer Edgeville for simplicity
        return SMITHING_LOCATIONS.stream()
                .filter(loc -> !loc.hasBlastFurnace) // Avoid blast furnace for now
                .findFirst()
                .orElse(SMITHING_LOCATIONS.get(0));
    }

    private boolean hasRequiredMaterials() {
        if (currentBar == null) return false;

        // Check if we have required ores
        for (Map.Entry<String, Integer> requirement : currentBar.oreRequirements.entrySet()) {
            if (Rs2Inventory.count(requirement.getKey()) < requirement.getValue()) {
                return false;
            }
        }

        // Check coal requirement
        if (currentBar.needsCoal && Rs2Inventory.count("Coal") < currentBar.coalAmount) {
            return false;
        }

        return true;
    }

    private void getMaterials() {
        if (!Rs2Bank.openBank()) return;

        Microbot.status = "Smithing: Getting materials for " + currentBar.name + " bars";

        // Withdraw required ores
        for (Map.Entry<String, Integer> requirement : currentBar.oreRequirements.entrySet()) {
            int needed = requirement.getValue() * 14; // Get enough for 14 bars
            Rs2Bank.withdrawX(true, requirement.getKey(), needed);
        }

        // Withdraw coal if needed
        if (currentBar.needsCoal) {
            int coalNeeded = currentBar.coalAmount * 14; // Get enough for 14 bars
            Rs2Bank.withdrawX(true, "Coal", coalNeeded);
        }

        Rs2Bank.closeBank();
    }

    private boolean isAtLocation() {
        return currentLocation != null &&
               Rs2Player.getWorldLocation().distanceTo(currentLocation.location) <= 15;
    }

    private void walkToLocation() {
        if (currentLocation == null) return;

        Microbot.status = "Smithing: Walking to " + currentLocation.name;
        Rs2Walker.walkTo(currentLocation.location);
    }

    private void performSmelting() {
        // Find furnace using modern API
        List<GameObject> furnaces = Rs2GameObject.getGameObjects(obj -> {
            ObjectComposition comp = Rs2GameObject.convertToObjectComposition(obj);
            String name = comp != null ? comp.getName() : null;
            return name != null && name.contains(currentLocation.furnaceObject) &&
                obj.getWorldLocation().distanceTo(currentLocation.location) <= 10;
        });

        if (!furnaces.isEmpty()) {
            GameObject furnace = furnaces.get(0);
            String primaryOre = currentBar.oreRequirements.keySet().iterator().next();
            if (Rs2Inventory.interact(primaryOre) && Rs2GameObject.interact(furnace)) {
                Microbot.status = "Smithing: Smelting " + currentBar.name + " bars";

                // Wait for interface and select amount
                sleep(2000, 3000);
                if (Rs2Widget.isWidgetVisible(270, 14)) { // Smelting interface Make All button
                    Rs2Widget.clickWidget(270, 14); // Make All
                }

                // Wait for smelting to complete
                Rs2Player.waitForXpDrop(Skill.SMITHING, true);
                Rs2Antiban.actionCooldown();
            }
        }
    }

    private void performAnvilSmithing() {
        // Check if we have bars
        if (!Rs2Inventory.hasItem(currentBar.barName)) {
            Microbot.status = "Smithing: No " + currentBar.barName + " available";
            return;
        }

        // Find anvil using modern API
        List<GameObject> anvils = Rs2GameObject.getGameObjects(obj -> {
            ObjectComposition comp = Rs2GameObject.convertToObjectComposition(obj);
            String name = comp != null ? comp.getName() : null;
            return name != null && name.contains("Anvil") &&
                obj.getWorldLocation().distanceTo(currentLocation.location) <= 10;
        });

        if (!anvils.isEmpty()) {
            GameObject anvil = anvils.get(0);
            if (Rs2Inventory.interact(currentBar.barName) && Rs2GameObject.interact(anvil)) {
                Microbot.status = "Smithing: Smithing with " + currentBar.barName;

                // Wait for interface and select item
                sleep(2000, 3000);
                // TODO: handle specific smithing interface widgets if needed

                Rs2Player.waitForXpDrop(Skill.SMITHING, true);
                Rs2Antiban.actionCooldown();
            }
        }
    }

    private void performBlastFurnace() {
        // Simplified blast furnace implementation
        Microbot.status = "Smithing: Blast furnace not fully implemented";
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
    public SmithingBar getCurrentBar() { return currentBar; }
}