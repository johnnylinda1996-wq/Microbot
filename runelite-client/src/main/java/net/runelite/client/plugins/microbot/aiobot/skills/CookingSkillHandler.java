package net.runelite.client.plugins.microbot.aiobot.skills;

import net.runelite.api.GameObject;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.aiobot.settings.SkillRuntimeSettings;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.*;

public class CookingSkillHandler implements SkillHandler {

    private boolean enabled = true;
    private String mode = "range";
    private String currentFood = "Raw shrimps";

    private static final List<CookingLocation> COOKING_LOCATIONS = Arrays.asList(
            new CookingLocation("Al Kharid", new WorldPoint(3273, 3181, 0), BankLocation.AL_KHARID, "Range", false),
            new CookingLocation("Lumbridge", new WorldPoint(3209, 3214, 0), BankLocation.LUMBRIDGE_FRONT, "Range", false),
            new CookingLocation("Varrock East", new WorldPoint(3253, 3421, 0), BankLocation.VARROCK_EAST, "Range", false),
            new CookingLocation("Edgeville", new WorldPoint(3077, 3492, 0), BankLocation.EDGEVILLE, "Range", false)
    );

    private static final Map<Integer, List<String>> FOOD_PROGRESSION = new HashMap<>();

    static {
        FOOD_PROGRESSION.put(1, Arrays.asList("Raw shrimps", "Raw sardine", "Raw anchovies"));
        FOOD_PROGRESSION.put(15, Arrays.asList("Raw trout", "Raw salmon"));
        FOOD_PROGRESSION.put(30, Arrays.asList("Raw tuna", "Raw lobster"));
        FOOD_PROGRESSION.put(40, Arrays.asList("Raw swordfish", "Raw monkfish"));
    }

    private static class CookingLocation {
        private final String name;
        private final WorldPoint location;
        private final BankLocation nearestBank;
        private final String cookingType;
        private final boolean membersOnly;

        public CookingLocation(String name, WorldPoint location, BankLocation nearestBank,
                               String cookingType, boolean membersOnly) {
            this.name = name;
            this.location = location;
            this.nearestBank = nearestBank;
            this.cookingType = cookingType;
            this.membersOnly = membersOnly;
        }

        public boolean isAccessible() {
            return !membersOnly || Rs2Player.isMember();
        }

        public boolean matchesCookingType(String type) {
            return cookingType.equalsIgnoreCase(type);
        }

        public String getName() { return name; }
        public WorldPoint getLocation() { return location; }
        public BankLocation getNearestBank() { return nearestBank; }
    }

    @Override
    public void applySettings(SkillRuntimeSettings settings) {
        if (settings != null) {
            enabled = settings.isEnabled();
            mode = settings.getMode() != null ? settings.getMode().toLowerCase() : "range";
        }
    }

    @Override
    public void execute() {
        if (!enabled) {
            Microbot.status = "Cooking: disabled";
            return;
        }

        if (!Microbot.isLoggedIn()) return;

        updateCurrentFood();

        if (currentFood == null) {
            handleNoFood();
            return;
        }

        if (!Rs2Inventory.contains(currentFood)) {
            handleMissingFood();
            return;
        }

        CookingLocation bestLocation = findBestCookingLocation();
        if (bestLocation != null && !isAtCookingLocation(bestLocation)) {
            walkToCookingLocation(bestLocation);
            return;
        }

        startCooking();
    }

    private void updateCurrentFood() {
        int cookingLevel = Microbot.getClient().getRealSkillLevel(Skill.COOKING);
        currentFood = null;

        for (Map.Entry<Integer, List<String>> entry : FOOD_PROGRESSION.entrySet()) {
            if (cookingLevel >= entry.getKey()) {
                for (String food : entry.getValue()) {
                    if (Rs2Inventory.contains(food)) {
                        currentFood = food;
                        break;
                    }
                }
                if (currentFood != null) break;
            }
        }

        if (currentFood == null) {
            List<String> allFoods = Arrays.asList(
                    "Raw shrimps", "Raw sardine", "Raw anchovies", "Raw trout", "Raw salmon"
            );

            for (String food : allFoods) {
                if (Rs2Inventory.contains(food)) {
                    currentFood = food;
                    break;
                }
            }
        }
    }

    private void handleNoFood() {
        Microbot.status = "Cooking: no raw food available";
    }

    private void handleMissingFood() {
        BankLocation nearestBank = Rs2Bank.getNearestBank();

        if (nearestBank != null && Rs2Bank.walkToBankAndUseBank(nearestBank)) {
            if (Rs2Bank.isOpen()) {
                if (Rs2Bank.hasItem(currentFood)) {
                    Rs2Bank.withdrawAll(currentFood);
                    Rs2Bank.closeBank();
                    Microbot.status = "Cooking: got " + currentFood + " from bank";
                }
            }
        }
    }

    private CookingLocation findBestCookingLocation() {
        WorldPoint playerLoc = Rs2Player.getWorldLocation();

        return COOKING_LOCATIONS.stream()
                .filter(CookingLocation::isAccessible)
                .filter(loc -> loc.matchesCookingType(mode))
                .min(Comparator.comparingInt(loc -> loc.getLocation().distanceTo(playerLoc)))
                .orElse(COOKING_LOCATIONS.get(0));
    }

    private boolean isAtCookingLocation(CookingLocation location) {
        return Rs2Player.getWorldLocation().distanceTo(location.getLocation()) <= 5;
    }

    private void walkToCookingLocation(CookingLocation location) {
        if (Rs2Walker.walkTo(location.getLocation(), 3)) {
            Microbot.status = "Cooking: walking to " + location.getName();
        }
    }

    private void startCooking() {
        GameObject cookingSource = findCookingSource();

        if (cookingSource == null) {
            Microbot.status = "Cooking: no " + mode + " found";
            return;
        }

        // Fix: Gebruik de juiste API signature met item name (String) en object ID (int)
        if (Rs2Inventory.useItemOnObject(Integer.parseInt(currentFood), cookingSource.getId())) {
            Rs2Player.waitForXpDrop(Skill.COOKING, true);
            Rs2Antiban.actionCooldown();
            Rs2Antiban.takeMicroBreakByChance();
            Microbot.status = "Cooking: " + currentFood.replace("Raw ", "");
        }
    }

    private GameObject findCookingSource() {
        // Gebruik de niet-deprecated API
        if ("range".equalsIgnoreCase(mode)) {
            return Rs2GameObject.getGameObject("Range", true);
        } else {
            GameObject fire = Rs2GameObject.getGameObject("Fire", true);
            if (fire == null) {
                return Rs2GameObject.getGameObject("Range", true);
            }
            return fire;
        }
    }

    public String getCurrentFood() { return currentFood; }
    public boolean isEnabled() { return enabled; }
    public String getMode() { return mode; }
}