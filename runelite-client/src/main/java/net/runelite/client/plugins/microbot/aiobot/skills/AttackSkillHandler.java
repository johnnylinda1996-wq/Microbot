package net.runelite.client.plugins.microbot.aiobot.skills;

import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.aiobot.settings.SkillRuntimeSettings;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.*;

public class AttackSkillHandler implements SkillHandler {

    private boolean enabled = true;
    private String mode = "progressive";
    private boolean useSpecial = true;
    private boolean useFood = true;
    private int eatHealthPercentage = 50;

    private static final Map<String, List<TrainingLocation>> TRAINING_LOCATIONS = new HashMap<>();

    static {
        TRAINING_LOCATIONS.put("Chicken", Arrays.asList(
                new TrainingLocation("Lumbridge", new WorldPoint(3235, 3295, 0), BankLocation.LUMBRIDGE_FRONT, false),
                new TrainingLocation("Falador", new WorldPoint(2978, 3293, 0), BankLocation.FALADOR_EAST, false)
        ));

        TRAINING_LOCATIONS.put("Cow", Arrays.asList(
                new TrainingLocation("Lumbridge", new WorldPoint(3257, 3266, 0), BankLocation.LUMBRIDGE_FRONT, false)
        ));

        TRAINING_LOCATIONS.put("Goblin", Arrays.asList(
                new TrainingLocation("Lumbridge", new WorldPoint(3247, 3245, 0), BankLocation.LUMBRIDGE_FRONT, false)
        ));
    }

    private static class TrainingLocation {
        private final String name;
        private final WorldPoint location;
        private final BankLocation nearestBank;
        private final boolean membersOnly;

        public TrainingLocation(String name, WorldPoint location, BankLocation nearestBank, boolean membersOnly) {
            this.name = name;
            this.location = location;
            this.nearestBank = nearestBank;
            this.membersOnly = membersOnly;
        }

        public boolean isAccessible() {
            return !membersOnly || Rs2Player.isMember();
        }

        public String getName() { return name; }
        public WorldPoint getLocation() { return location; }
        public BankLocation getNearestBank() { return nearestBank; }
    }

    @Override
    public void applySettings(SkillRuntimeSettings settings) {
        if (settings != null) {
            enabled = settings.isEnabled();
            mode = settings.getMode() != null ? settings.getMode().toLowerCase() : "progressive";
            useSpecial = settings.isUseSpecial();
        }
    }

    @Override
    public void execute() {
        if (!enabled) {
            Microbot.status = "Attack: disabled";
            return;
        }

        if (!Microbot.isLoggedIn()) return;

        if (useFood && needsFood()) {
            eatFood();
            return;
        }

        if (!hasWeapon()) {
            handleMissingWeapon();
            return;
        }

        if (Rs2Player.isInCombat()) {
            // Proper conversion to int - fix for lossy conversion error
            int specEnergy = (int) Math.round(Rs2Combat.getSpecEnergy() / 10.0);
            if (useSpecial && specEnergy >= 25) {
                Rs2Combat.setSpecState(true, 1000);
            }

            Microbot.status = "Attack: in combat";
            return;
        }

        String target = findSuitableTarget();
        TrainingLocation bestLocation = findBestTrainingLocation(target);

        if (bestLocation != null && !isAtTrainingLocation(bestLocation)) {
            walkToTrainingLocation(bestLocation);
            return;
        }

        attackTarget(target);
    }

    private boolean needsFood() {
        // Fix: Cast double to int properly
        int currentHealth = (int) Math.round(Rs2Player.getHealthPercentage());
        return currentHealth < eatHealthPercentage;
    }

    private void eatFood() {
        List<String> foods = Arrays.asList(
                "Salmon", "Trout", "Lobster", "Swordfish", "Shark", "Tuna",
                "Bread", "Cake", "Cooked meat", "Cooked chicken"
        );

        for (String food : foods) {
            if (Rs2Inventory.hasItem(food)) {
                Rs2Inventory.interact(food, "Eat");
                Microbot.status = "Attack: eating " + food;
                return;
            }
        }

        handleNoFood();
    }

    private void handleNoFood() {
        BankLocation nearestBank = Rs2Bank.getNearestBank();

        if (nearestBank != null && Rs2Bank.walkToBankAndUseBank(nearestBank)) {
            if (Rs2Bank.isOpen()) {
                List<String> foods = Arrays.asList("Salmon", "Trout", "Lobster", "Swordfish", "Bread");

                for (String food : foods) {
                    if (Rs2Bank.hasItem(food)) {
                        Rs2Bank.withdrawX(food, 10);
                        Rs2Bank.closeBank();
                        Microbot.status = "Attack: got food from bank";
                        return;
                    }
                }

                Microbot.status = "Attack: no food in bank";
                Rs2Bank.closeBank();
            }
        }
    }

    private boolean hasWeapon() {
        return Rs2Equipment.isWearing("sword") || Rs2Equipment.isWearing("axe") ||
                Rs2Equipment.isWearing("mace") || Rs2Equipment.isWearing("scimitar") ||
                Rs2Inventory.hasItem("Bronze sword") || Rs2Inventory.hasItem("Iron sword") ||
                Rs2Inventory.hasItem("Steel sword") || Rs2Inventory.hasItem("Mithril sword");
    }

    private void handleMissingWeapon() {
        BankLocation nearestBank = Rs2Bank.getNearestBank();

        if (nearestBank != null && Rs2Bank.walkToBankAndUseBank(nearestBank)) {
            if (Rs2Bank.isOpen()) {
                List<String> weapons = Arrays.asList(
                        "Rune sword", "Adamant sword", "Mithril sword", "Steel sword", "Iron sword", "Bronze sword"
                );

                for (String weapon : weapons) {
                    if (Rs2Bank.hasItem(weapon)) {
                        Rs2Bank.withdrawOne(weapon);
                        Rs2Bank.closeBank();

                        if (Rs2Inventory.hasItem(weapon)) {
                            Rs2Inventory.wield(weapon);
                        }

                        Microbot.status = "Attack: equipped " + weapon;
                        return;
                    }
                }

                Rs2Bank.closeBank();
            }
        }
    }

    private String findSuitableTarget() {
        int combatLevel = Rs2Player.getCombatLevel();

        if ("progressive".equals(mode)) {
            if (combatLevel >= 8) {
                return "Goblin";
            } else if (combatLevel >= 3) {
                return "Cow";
            } else {
                return "Chicken";
            }
        }

        return "Chicken";
    }

    private TrainingLocation findBestTrainingLocation(String target) {
        List<TrainingLocation> locations = TRAINING_LOCATIONS.get(target);
        if (locations == null || locations.isEmpty()) {
            return null;
        }

        WorldPoint playerLoc = Rs2Player.getWorldLocation();

        return locations.stream()
                .filter(TrainingLocation::isAccessible)
                .min(Comparator.comparingInt(loc -> loc.getLocation().distanceTo(playerLoc)))
                .orElse(locations.get(0));
    }

    private boolean isAtTrainingLocation(TrainingLocation location) {
        return Rs2Player.getWorldLocation().distanceTo(location.getLocation()) <= 20;
    }

    private void walkToTrainingLocation(TrainingLocation location) {
        if (Rs2Walker.walkTo(location.getLocation(), 5)) {
            Microbot.status = "Attack: walking to " + location.getName();
        }
    }

    private void attackTarget(String target) {
        if (Rs2Npc.attack(target)) {
            Rs2Antiban.actionCooldown();
            Rs2Antiban.takeMicroBreakByChance();
            Microbot.status = "Attack: fighting " + target;
        } else {
            Microbot.status = "Attack: no " + target + " found";
        }
    }

    public String getCurrentTarget() { return findSuitableTarget(); }
    public boolean isEnabled() { return enabled; }
    public String getMode() { return mode; }
}