package net.runelite.client.plugins.microbot.jpnl.accountbuilder.skills;

import net.runelite.api.ItemID;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.settings.SkillRuntimeSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;

import java.util.*;

/**
 * Complete Fletching skill handler for AIO bot system.
 * Supports arrow shafts, shortbows, longbows, crossbows, bolts, and darts.
 */
public class FletchingSkillHandler implements SkillHandler {

    private boolean enabled = true;
    private String mode = "auto"; // auto, arrows, shortbow, longbow, crossbow, etc.
    private FletchingItem currentItem;
    private int failCount = 0;
    private static final int MAX_FAIL_COUNT = 3;

    // Fletching data structures
    private static class FletchingItem {
        String name;
        int levelRequired;
        Map<String, Integer> materials; // material name -> amount needed
        String primaryMaterial;
        String secondaryMaterial;
        int experience;
        String keyPress; // What key to press in interface

        public FletchingItem(String name, int levelRequired, Map<String, Integer> materials,
                           String primaryMaterial, String secondaryMaterial, int experience, String keyPress) {
            this.name = name;
            this.levelRequired = levelRequired;
            this.materials = materials;
            this.primaryMaterial = primaryMaterial;
            this.secondaryMaterial = secondaryMaterial;
            this.experience = experience;
            this.keyPress = keyPress;
        }
    }

    // Fletching items database
    private static final List<FletchingItem> FLETCHING_ITEMS = Arrays.asList(
            // Arrow shafts and arrows
            new FletchingItem("Arrow shaft", 1, Map.of("Logs", 1, "Knife", 1), "Logs", "Knife", 5, "1"),
            new FletchingItem("Headless arrow", 1, Map.of("Arrow shaft", 1, "Feather", 1), "Arrow shaft", "Feather", 1, "1"),
            new FletchingItem("Bronze arrow", 1, Map.of("Headless arrow", 1, "Bronze arrowheads", 1), "Headless arrow", "Bronze arrowheads", 2, "1"),
            new FletchingItem("Iron arrow", 15, Map.of("Headless arrow", 1, "Iron arrowheads", 1), "Headless arrow", "Iron arrowheads", 4, "1"),
            new FletchingItem("Steel arrow", 30, Map.of("Headless arrow", 1, "Steel arrowheads", 1), "Headless arrow", "Steel arrowheads", 6, "1"),
            new FletchingItem("Mithril arrow", 45, Map.of("Headless arrow", 1, "Mithril arrowheads", 1), "Headless arrow", "Mithril arrowheads", 8, "1"),
            new FletchingItem("Adamant arrow", 60, Map.of("Headless arrow", 1, "Adamant arrowheads", 1), "Headless arrow", "Adamant arrowheads", 10, "1"),
            new FletchingItem("Rune arrow", 75, Map.of("Headless arrow", 1, "Rune arrowheads", 1), "Headless arrow", "Rune arrowheads", 12, "1"),

            // Shortbows
            new FletchingItem("Shortbow (u)", 5, Map.of("Logs", 1, "Knife", 1), "Logs", "Knife", 5, "2"),
            new FletchingItem("Shortbow", 5, Map.of("Shortbow (u)", 1, "Bow string", 1), "Shortbow (u)", "Bow string", 5, "1"),
            new FletchingItem("Oak shortbow (u)", 20, Map.of("Oak logs", 1, "Knife", 1), "Oak logs", "Knife", 17, "2"),
            new FletchingItem("Oak shortbow", 20, Map.of("Oak shortbow (u)", 1, "Bow string", 1), "Oak shortbow (u)", "Bow string", 17, "1"),
            new FletchingItem("Willow shortbow (u)", 35, Map.of("Willow logs", 1, "Knife", 1), "Willow logs", "Knife", 33, "2"),
            new FletchingItem("Willow shortbow", 35, Map.of("Willow shortbow (u)", 1, "Bow string", 1), "Willow shortbow (u)", "Bow string", 33, "1"),
            new FletchingItem("Maple shortbow (u)", 50, Map.of("Maple logs", 1, "Knife", 1), "Maple logs", "Knife", 50, "2"),
            new FletchingItem("Maple shortbow", 50, Map.of("Maple shortbow (u)", 1, "Bow string", 1), "Maple shortbow (u)", "Bow string", 50, "1"),
            new FletchingItem("Yew shortbow (u)", 65, Map.of("Yew logs", 1, "Knife", 1), "Yew logs", "Knife", 67, "2"),
            new FletchingItem("Yew shortbow", 65, Map.of("Yew shortbow (u)", 1, "Bow string", 1), "Yew shortbow (u)", "Bow string", 67, "1"),
            new FletchingItem("Magic shortbow (u)", 80, Map.of("Magic logs", 1, "Knife", 1), "Magic logs", "Knife", 83, "2"),
            new FletchingItem("Magic shortbow", 80, Map.of("Magic shortbow (u)", 1, "Bow string", 1), "Magic shortbow (u)", "Bow string", 83, "1"),

            // Longbows
            new FletchingItem("Longbow (u)", 10, Map.of("Logs", 1, "Knife", 1), "Logs", "Knife", 10, "3"),
            new FletchingItem("Longbow", 10, Map.of("Longbow (u)", 1, "Bow string", 1), "Longbow (u)", "Bow string", 10, "1"),
            new FletchingItem("Oak longbow (u)", 25, Map.of("Oak logs", 1, "Knife", 1), "Oak logs", "Knife", 25, "3"),
            new FletchingItem("Oak longbow", 25, Map.of("Oak longbow (u)", 1, "Bow string", 1), "Oak longbow (u)", "Bow string", 25, "1"),
            new FletchingItem("Willow longbow (u)", 40, Map.of("Willow logs", 1, "Knife", 1), "Willow logs", "Knife", 42, "3"),
            new FletchingItem("Willow longbow", 40, Map.of("Willow longbow (u)", 1, "Bow string", 1), "Willow longbow (u)", "Bow string", 42, "1"),
            new FletchingItem("Maple longbow (u)", 55, Map.of("Maple logs", 1, "Knife", 1), "Maple logs", "Knife", 58, "3"),
            new FletchingItem("Maple longbow", 55, Map.of("Maple longbow (u)", 1, "Bow string", 1), "Maple longbow (u)", "Bow string", 58, "1"),
            new FletchingItem("Yew longbow (u)", 70, Map.of("Yew logs", 1, "Knife", 1), "Yew logs", "Knife", 75, "3"),
            new FletchingItem("Yew longbow", 70, Map.of("Yew longbow (u)", 1, "Bow string", 1), "Yew longbow (u)", "Bow string", 75, "1"),
            new FletchingItem("Magic longbow (u)", 85, Map.of("Magic logs", 1, "Knife", 1), "Magic logs", "Knife", 92, "3"),
            new FletchingItem("Magic longbow", 85, Map.of("Magic longbow (u)", 1, "Bow string", 1), "Magic longbow (u)", "Bow string", 92, "1"),

            // Bolts and darts
            new FletchingItem("Bronze dart", 10, Map.of("Bronze dart tip", 1, "Feather", 1), "Bronze dart tip", "Feather", 2, "1"),
            new FletchingItem("Iron dart", 22, Map.of("Iron dart tip", 1, "Feather", 1), "Iron dart tip", "Feather", 4, "1"),
            new FletchingItem("Steel dart", 37, Map.of("Steel dart tip", 1, "Feather", 1), "Steel dart tip", "Feather", 8, "1"),
            new FletchingItem("Bronze bolts", 9, Map.of("Bronze bolts (unf)", 1, "Feather", 1), "Bronze bolts (unf)", "Feather", 1, "1"),
            new FletchingItem("Iron bolts", 39, Map.of("Iron bolts (unf)", 1, "Feather", 1), "Iron bolts (unf)", "Feather", 2, "1"),
            new FletchingItem("Steel bolts", 46, Map.of("Steel bolts (unf)", 1, "Feather", 1), "Steel bolts (unf)", "Feather", 4, "1")
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
                Microbot.status = "Fletching: disabled";
                return;
            }

            if (!Microbot.isLoggedIn()) return;

            // Determine current item
            currentItem = determineCurrentItem();
            if (currentItem == null) {
                handleFailure("No suitable fletching item for level " + Microbot.getClient().getRealSkillLevel(Skill.FLETCHING));
                return;
            }

            // Check if we need materials
            if (!hasMaterials()) {
                getMaterials();
                return;
            }

            // Check if inventory is full of fletched items
            if (Rs2Inventory.isFull() && !hasMaterials()) {
                bankProducts();
                return;
            }

            // Do fletching
            attemptFletching();

            // Reset fail count on successful execution
            failCount = 0;

        } catch (Exception e) {
            handleFailure("Fletching error: " + e.getMessage());
        }
    }

    private FletchingItem determineCurrentItem() {
        int fletchingLevel = Microbot.getClient().getRealSkillLevel(Skill.FLETCHING);

        if ("auto".equals(mode)) {
            // Find highest level item we can fletch
            FletchingItem bestItem = null;
            for (FletchingItem item : FLETCHING_ITEMS) {
                if (fletchingLevel >= item.levelRequired) {
                    bestItem = item;
                }
            }
            return bestItem;
        } else {
            // Try to find specific item
            for (FletchingItem item : FLETCHING_ITEMS) {
                if (item.name.toLowerCase().contains(mode) && fletchingLevel >= item.levelRequired) {
                    return item;
                }
            }
            return null;
        }
    }

    private boolean hasMaterials() {
        if (currentItem == null) return false;

        for (Map.Entry<String, Integer> material : currentItem.materials.entrySet()) {
            if (Rs2Inventory.count(material.getKey()) < material.getValue()) {
                return false;
            }
        }
        return true;
    }

    private void getMaterials() {
        if (!Rs2Bank.openBank()) return;

        Microbot.status = "Fletching: Getting materials for " + currentItem.name;

        // Get materials
        for (Map.Entry<String, Integer> material : currentItem.materials.entrySet()) {
            if (!Rs2Inventory.hasItem(material.getKey())) {
                if (material.getKey().equals("Knife") || material.getKey().equals("Feather") ||
                    material.getKey().contains("string") || material.getKey().contains("tip")) {
                    Rs2Bank.withdrawX(true, material.getKey(), 1000); // Get lots of consumables
                } else {
                    Rs2Bank.withdrawAll(material.getKey()); // Get all logs/materials
                }
            }
        }

        Rs2Bank.closeBank();
    }

    private void attemptFletching() {
        if (!hasMaterials()) return;

        // Combine materials
        if (Rs2Inventory.combine(currentItem.primaryMaterial, currentItem.secondaryMaterial)) {
            Microbot.status = "Fletching: Making " + currentItem.name;

            // Wait for fletching interface and press appropriate key
            if (waitForFletchingInterface()) {
                Rs2Keyboard.keyPress(currentItem.keyPress.charAt(0));
                waitForFletching();
            }
        }
    }

    private boolean waitForFletchingInterface() {
        // Wait for fletching interface to appear
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 5000) {
            if (Rs2Widget.getWidget(17694734) != null || Rs2Widget.getWidget(270, 14) != null) {
                return true;
            }
            sleep(100, 200);
        }
        return false;
    }

    private void waitForFletching() {
        // Wait while player is fletching
        while (Rs2Player.isAnimating() && hasMaterials()) {
            sleep(1000, 2000);
        }
    }

    private void bankProducts() {
        List<String> itemsToBank = new ArrayList<>();
        for (FletchingItem item : FLETCHING_ITEMS) {
            itemsToBank.add(item.name);
        }

        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        if (!Rs2Bank.bankItemsAndWalkBackToOriginalPosition(itemsToBank, playerLocation, 0, 15)) {
            Microbot.status = "Fletching: Banking failed, trying again...";
        }
    }

    private void handleFailure(String message) {
        failCount++;
        Microbot.status = "Fletching: " + message + " (Fails: " + failCount + "/" + MAX_FAIL_COUNT + ")";

        if (failCount >= MAX_FAIL_COUNT) {
            Microbot.status = "Fletching: Max failures reached, moving to next task";
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
    public FletchingItem getCurrentItem() { return currentItem; }
    public int getFailCount() { return failCount; }
}