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

import java.util.*;

/**
 * Complete Herblore skill handler for AIO bot system.
 * Supports cleaning herbs, making unfinished potions, finished potions, and barbarian herblore.
 */
public class HerbloreSkillHandler implements SkillHandler {

    private boolean enabled = true;
    private String mode = "auto"; // auto, clean, unfinished, potions, barbarian
    private HerbloreItem currentItem;
    private HerbloreMethod currentMethod = HerbloreMethod.CLEAN;
    private int failCount = 0;
    private static final int MAX_FAIL_COUNT = 3;

    // Herblore data structures
    private enum HerbloreMethod {
        CLEAN, UNFINISHED, POTIONS, BARBARIAN
    }

    private static class HerbloreItem {
        String name;
        int levelRequired;
        Map<String, Integer> materials; // material name -> amount needed
        String primaryMaterial;
        String secondaryMaterial;
        HerbloreMethod method;
        int experience;

        public HerbloreItem(String name, int levelRequired, Map<String, Integer> materials,
                          String primaryMaterial, String secondaryMaterial, HerbloreMethod method, int experience) {
            this.name = name;
            this.levelRequired = levelRequired;
            this.materials = materials;
            this.primaryMaterial = primaryMaterial;
            this.secondaryMaterial = secondaryMaterial;
            this.method = method;
            this.experience = experience;
        }
    }

    // Herblore items database
    private static final List<HerbloreItem> HERBLORE_ITEMS = Arrays.asList(
            // Cleaning herbs
            new HerbloreItem("Guam leaf", 1, Map.of("Grimy guam leaf", 1), "Grimy guam leaf", "", HerbloreMethod.CLEAN, 3),
            new HerbloreItem("Marrentill", 5, Map.of("Grimy marrentill", 1), "Grimy marrentill", "", HerbloreMethod.CLEAN, 4),
            new HerbloreItem("Tarromin", 11, Map.of("Grimy tarromin", 1), "Grimy tarromin", "", HerbloreMethod.CLEAN, 5),
            new HerbloreItem("Harralander", 20, Map.of("Grimy harralander", 1), "Grimy harralander", "", HerbloreMethod.CLEAN, 6),
            new HerbloreItem("Ranarr weed", 25, Map.of("Grimy ranarr weed", 1), "Grimy ranarr weed", "", HerbloreMethod.CLEAN, 8),
            new HerbloreItem("Toadflax", 30, Map.of("Grimy toadflax", 1), "Grimy toadflax", "", HerbloreMethod.CLEAN, 8),
            new HerbloreItem("Irit leaf", 40, Map.of("Grimy irit leaf", 1), "Grimy irit leaf", "", HerbloreMethod.CLEAN, 9),
            new HerbloreItem("Avantoe", 48, Map.of("Grimy avantoe", 1), "Grimy avantoe", "", HerbloreMethod.CLEAN, 10),
            new HerbloreItem("Kwuarm", 54, Map.of("Grimy kwuarm", 1), "Grimy kwuarm", "", HerbloreMethod.CLEAN, 11),
            new HerbloreItem("Snapdragon", 59, Map.of("Grimy snapdragon", 1), "Grimy snapdragon", "", HerbloreMethod.CLEAN, 12),
            new HerbloreItem("Cadantine", 65, Map.of("Grimy cadantine", 1), "Grimy cadantine", "", HerbloreMethod.CLEAN, 13),
            new HerbloreItem("Lantadyme", 67, Map.of("Grimy lantadyme", 1), "Grimy lantadyme", "", HerbloreMethod.CLEAN, 14),
            new HerbloreItem("Dwarf weed", 70, Map.of("Grimy dwarf weed", 1), "Grimy dwarf weed", "", HerbloreMethod.CLEAN, 15),
            new HerbloreItem("Torstol", 75, Map.of("Grimy torstol", 1), "Grimy torstol", "", HerbloreMethod.CLEAN, 15),

            // Unfinished potions
            new HerbloreItem("Attack potion (unf)", 3, Map.of("Guam leaf", 1, "Vial of water", 1), "Guam leaf", "Vial of water", HerbloreMethod.UNFINISHED, 25),
            new HerbloreItem("Antipoison (unf)", 5, Map.of("Marrentill", 1, "Vial of water", 1), "Marrentill", "Vial of water", HerbloreMethod.UNFINISHED, 38),
            new HerbloreItem("Strength potion (unf)", 12, Map.of("Tarromin", 1, "Vial of water", 1), "Tarromin", "Vial of water", HerbloreMethod.UNFINISHED, 50),
            new HerbloreItem("Combat potion (unf)", 36, Map.of("Harralander", 1, "Vial of water", 1), "Harralander", "Vial of water", HerbloreMethod.UNFINISHED, 84),
            new HerbloreItem("Prayer potion (unf)", 38, Map.of("Ranarr weed", 1, "Vial of water", 1), "Ranarr weed", "Vial of water", HerbloreMethod.UNFINISHED, 88),
            new HerbloreItem("Super attack (unf)", 45, Map.of("Irit leaf", 1, "Vial of water", 1), "Irit leaf", "Vial of water", HerbloreMethod.UNFINISHED, 100),
            new HerbloreItem("Super strength (unf)", 55, Map.of("Kwuarm", 1, "Vial of water", 1), "Kwuarm", "Vial of water", HerbloreMethod.UNFINISHED, 125),
            new HerbloreItem("Super defence (unf)", 66, Map.of("Cadantine", 1, "Vial of water", 1), "Cadantine", "Vial of water", HerbloreMethod.UNFINISHED, 150),
            new HerbloreItem("Ranging potion (unf)", 72, Map.of("Dwarf weed", 1, "Vial of water", 1), "Dwarf weed", "Vial of water", HerbloreMethod.UNFINISHED, 163),

            // Finished potions
            new HerbloreItem("Attack potion", 3, Map.of("Attack potion (unf)", 1, "Eye of newt", 1), "Attack potion (unf)", "Eye of newt", HerbloreMethod.POTIONS, 25),
            new HerbloreItem("Antipoison", 5, Map.of("Antipoison (unf)", 1, "Unicorn horn dust", 1), "Antipoison (unf)", "Unicorn horn dust", HerbloreMethod.POTIONS, 38),
            new HerbloreItem("Strength potion", 12, Map.of("Strength potion (unf)", 1, "Limpwurt root", 1), "Strength potion (unf)", "Limpwurt root", HerbloreMethod.POTIONS, 50),
            new HerbloreItem("Combat potion", 36, Map.of("Combat potion (unf)", 1, "Goat horn dust", 1), "Combat potion (unf)", "Goat horn dust", HerbloreMethod.POTIONS, 84),
            new HerbloreItem("Prayer potion", 38, Map.of("Prayer potion (unf)", 1, "Snape grass", 1), "Prayer potion (unf)", "Snape grass", HerbloreMethod.POTIONS, 88),
            new HerbloreItem("Super attack", 45, Map.of("Super attack (unf)", 1, "Eye of newt", 1), "Super attack (unf)", "Eye of newt", HerbloreMethod.POTIONS, 100),
            new HerbloreItem("Super strength", 55, Map.of("Super strength (unf)", 1, "Limpwurt root", 1), "Super strength (unf)", "Limpwurt root", HerbloreMethod.POTIONS, 125),
            new HerbloreItem("Super defence", 66, Map.of("Super defence (unf)", 1, "White berries", 1), "Super defence (unf)", "White berries", HerbloreMethod.POTIONS, 150),
            new HerbloreItem("Ranging potion", 72, Map.of("Ranging potion (unf)", 1, "Wine of zamorak", 1), "Ranging potion (unf)", "Wine of zamorak", HerbloreMethod.POTIONS, 163),

            // Barbarian herblore
            new HerbloreItem("Barbarian herblore", 17, Map.of("Attack potion", 2, "Barbarian rod", 1), "Attack potion", "Barbarian rod", HerbloreMethod.BARBARIAN, 18)
    );

    @Override
    public void applySettings(SkillRuntimeSettings settings) {
        if (settings != null) {
            enabled = settings.isEnabled();
            mode = settings.getMode() != null ? settings.getMode().toLowerCase() : "auto";

            // Determine method from mode
            if (mode.contains("clean")) {
                currentMethod = HerbloreMethod.CLEAN;
            } else if (mode.contains("unfinished") || mode.contains("unf")) {
                currentMethod = HerbloreMethod.UNFINISHED;
            } else if (mode.contains("potion")) {
                currentMethod = HerbloreMethod.POTIONS;
            } else if (mode.contains("barbarian")) {
                currentMethod = HerbloreMethod.BARBARIAN;
            } else {
                currentMethod = HerbloreMethod.CLEAN; // Default
            }

            failCount = 0; // Reset fail count when settings change
        }
    }

    @Override
    public void execute() {
        try {
            if (!enabled) {
                Microbot.status = "Herblore: disabled";
                return;
            }

            if (!Microbot.isLoggedIn()) return;

            // Determine current item
            currentItem = determineCurrentItem();
            if (currentItem == null) {
                handleFailure("No suitable herblore item for level " + Microbot.getClient().getRealSkillLevel(Skill.HERBLORE));
                return;
            }

            // Check if we need materials
            if (!hasMaterials()) {
                getMaterials();
                return;
            }

            // Check if inventory is full of made items
            if (Rs2Inventory.isFull() && !hasMaterials()) {
                bankProducts();
                return;
            }

            // Do herblore
            attemptHerblore();

            // Reset fail count on successful execution
            failCount = 0;

        } catch (Exception e) {
            handleFailure("Herblore error: " + e.getMessage());
        }
    }

    private HerbloreItem determineCurrentItem() {
        int herbloreLevel = Microbot.getClient().getRealSkillLevel(Skill.HERBLORE);

        if ("auto".equals(mode)) {
            // Find highest level item we can make for current method
            HerbloreItem bestItem = null;
            for (HerbloreItem item : HERBLORE_ITEMS) {
                if (item.method == currentMethod && herbloreLevel >= item.levelRequired) {
                    bestItem = item;
                }
            }
            return bestItem;
        } else {
            // Try to find specific item
            for (HerbloreItem item : HERBLORE_ITEMS) {
                if (item.name.toLowerCase().contains(mode) && herbloreLevel >= item.levelRequired) {
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

        Microbot.status = "Herblore: Getting materials for " + currentItem.name;

        // Get materials in proper proportions
        for (Map.Entry<String, Integer> material : currentItem.materials.entrySet()) {
            String itemName = material.getKey();
            int needed = material.getValue();

            if (!Rs2Inventory.hasItem(itemName)) {
                if (currentMethod == HerbloreMethod.CLEAN) {
                    Rs2Bank.withdrawAll(itemName); // Withdraw all grimy herbs
                } else {
                    // For potions, maintain 1:1 ratio or get lots of secondaries
                    if (itemName.contains("dust") || itemName.contains("Eye of newt") ||
                        itemName.contains("root") || itemName.contains("berries") ||
                        itemName.contains("grass")) {
                        Rs2Bank.withdrawX(true, itemName, 1000); // Get lots of secondaries
                    } else {
                        Rs2Bank.withdrawX(true, itemName, 14); // Get equal amounts for 1:1 ratio
                    }
                }
            }
        }

        Rs2Bank.closeBank();
    }

    private void attemptHerblore() {
        if (!hasMaterials()) return;

        switch (currentMethod) {
            case CLEAN:
                attemptCleaning();
                break;
            case UNFINISHED:
            case POTIONS:
                attemptPotionMaking();
                break;
            case BARBARIAN:
                attemptBarbarianHerblore();
                break;
        }
    }

    private void attemptCleaning() {
        String grimyHerb = currentItem.primaryMaterial;
        if (Rs2Inventory.hasItem(grimyHerb)) {
            if (Rs2Inventory.interact(grimyHerb, "Clean")) {
                Microbot.status = "Herblore: Cleaning " + grimyHerb;
                waitForHerblore();
            }
        }
    }

    private void attemptPotionMaking() {
        // Combine primary and secondary materials
        if (Rs2Inventory.combine(currentItem.primaryMaterial, currentItem.secondaryMaterial)) {
            Microbot.status = "Herblore: Making " + currentItem.name;

            // Wait for herblore interface
            if (waitForHerbloreInterface()) {
                Rs2Widget.clickWidget("Make All");
                waitForHerblore();
            }
        }
    }

    private void attemptBarbarianHerblore() {
        // Barbarian herblore is done at barbarian village
        WorldPoint barbarianVillage = new WorldPoint(3082, 3420, 0);
        if (Rs2Player.getWorldLocation().distanceTo(barbarianVillage) > 10) {
            Rs2Walker.walkTo(barbarianVillage);
            return;
        }

        // Use attack potions on barbarian rod
        if (Rs2Inventory.combine(currentItem.primaryMaterial, currentItem.secondaryMaterial)) {
            Microbot.status = "Herblore: Barbarian herblore";
            waitForHerblore();
        }
    }

    private boolean waitForHerbloreInterface() {
        // Wait for herblore interface to appear
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 5000) {
            if (Rs2Widget.getWidget(270, 14) != null) {
                return true;
            }
            sleep(100, 200);
        }
        return false;
    }

    private void waitForHerblore() {
        // Wait while player is doing herblore
        while (Rs2Player.isAnimating() && hasMaterials()) {
            sleep(1000, 2000);
        }
    }

    private void bankProducts() {
        List<String> itemsToBank = new ArrayList<>();
        for (HerbloreItem item : HERBLORE_ITEMS) {
            itemsToBank.add(item.name);
        }
        // Add clean herbs
        itemsToBank.addAll(Arrays.asList("Guam leaf", "Marrentill", "Tarromin", "Harralander",
                "Ranarr weed", "Toadflax", "Irit leaf", "Avantoe", "Kwuarm", "Snapdragon",
                "Cadantine", "Lantadyme", "Dwarf weed", "Torstol"));

        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        if (!Rs2Bank.bankItemsAndWalkBackToOriginalPosition(itemsToBank, playerLocation, 0, 15)) {
            Microbot.status = "Herblore: Banking failed, trying again...";
        }
    }

    private void handleFailure(String message) {
        failCount++;
        Microbot.status = "Herblore: " + message + " (Fails: " + failCount + "/" + MAX_FAIL_COUNT + ")";

        if (failCount >= MAX_FAIL_COUNT) {
            Microbot.status = "Herblore: Max failures reached, moving to next task";
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
    public HerbloreItem getCurrentItem() { return currentItem; }
    public HerbloreMethod getCurrentMethod() { return currentMethod; }
    public int getFailCount() { return failCount; }
}