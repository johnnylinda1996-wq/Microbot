package net.runelite.client.plugins.microbot.jpnl.accountbuilder.skills;

import lombok.Getter;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.AllInOneConfig;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.settings.SkillRuntimeSettings;
import net.runelite.client.plugins.microbot.nateplugins.skilling.natefishing.enums.Fish;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.camera.Rs2Camera;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.*;

import static net.runelite.client.plugins.microbot.util.npc.Rs2Npc.validateInteractable;

public class FishingSkillHandler implements SkillHandler {
    private boolean enabled = true;
    private AllInOneConfig.GatheringMode mode = AllInOneConfig.GatheringMode.POWER_DROP;
    private AllInOneConfig.FishingMethod fishingMethod = AllInOneConfig.FishingMethod.AUTO;
    private boolean useSpecial = true;
    private AllInOneConfig config;

    // State management
    private enum State { FISHING, RESETTING, GETTING_EQUIPMENT, TRAVELING }
    private State currentState = State.FISHING;
    private String fishAction = "";
    private Fish currentFish = null;
    private WorldPoint initialLocation = null;
    private long lastActionTime = 0;
    private static final long ACTION_COOLDOWN = 600; // ms

    // Location data for fishing spots with banks
    @Getter
    private static class FishingLocation {
        private final String name;
        private final WorldPoint fishingSpot;
        private final BankLocation bankLocation;
        private final int minLevel;
        private final Fish[] availableFish;

        public FishingLocation(String name, WorldPoint fishingSpot, BankLocation bankLocation, int minLevel, Fish... availableFish) {
            this.name = name;
            this.fishingSpot = fishingSpot;
            this.bankLocation = bankLocation;
            this.minLevel = minLevel;
            this.availableFish = availableFish;
        }

        public double distanceFrom(WorldPoint location) {
            return Rs2Walker.getDistanceBetween(location, fishingSpot);
        }
    }

    // Define fishing locations with nearby banks
    private static final List<FishingLocation> FISHING_LOCATIONS = Arrays.asList(
            new FishingLocation("Lumbridge", new WorldPoint(3242, 3154, 0), BankLocation.LUMBRIDGE_TOP, 1, Fish.SHRIMP, Fish.SARDINE),
            new FishingLocation("Draynor", new WorldPoint(3086, 3233, 0), BankLocation.DRAYNOR_VILLAGE, 1, Fish.SHRIMP, Fish.SARDINE),
            new FishingLocation("Barbarian Village", new WorldPoint(3108, 3434, 0), BankLocation.EDGEVILLE, 20, Fish.TROUT, Fish.PIKE),
            new FishingLocation("Catherby", new WorldPoint(2853, 3431, 0), BankLocation.CATHERBY, 1, Fish.SHRIMP, Fish.SARDINE, Fish.LOBSTER, Fish.TUNA, Fish.SHARK),
            new FishingLocation("Fishing Guild", new WorldPoint(2599, 3421, 0), BankLocation.FISHING_GUILD, 40, Fish.LOBSTER, Fish.TUNA, Fish.SHARK),
            new FishingLocation("Piscatoris", new WorldPoint(2334, 3647, 0), BankLocation.PISCATORIS_FISHING_COLONY, 62, Fish.MONKFISH),
            new FishingLocation("Port Piscarilius", new WorldPoint(1799, 3756, 0), BankLocation.PISCARILIUS, 82, Fish.ANGLERFISH)
    );

    @Override
    public void applySettings(SkillRuntimeSettings settings) {
        if (settings != null) {
            enabled = settings.isEnabled();

            // Apply mode from settings or use default
            if (settings.getMode() != null) {
                String modeStr = settings.getMode().toLowerCase();
                if (modeStr.contains("bank")) {
                    mode = AllInOneConfig.GatheringMode.BANK;
                } else {
                    mode = AllInOneConfig.GatheringMode.POWER_DROP;
                }
            }
        }
    }

    public void setConfig(AllInOneConfig config) {
        this.config = config;
        if (config != null) {
            this.mode = config.fishingMode();
            this.fishingMethod = config.fishingMethod();
            this.useSpecial = config.fishingSpecial();
        }
    }

    @Override
    public void execute() {
        if (!enabled) {
            Microbot.status = "Fishing: disabled";
            return;
        }

        if (!Microbot.isLoggedIn()) return;
        if (Rs2AntibanSettings.actionCooldownActive) return;

        // Initialize starting location
        if (initialLocation == null) {
            initialLocation = Rs2Player.getWorldLocation();
        }

        // Determine best fish and location based on level and method
        updateCurrentFishAndLocation();

        if (currentFish == null) {
            Microbot.status = "Fishing: No suitable fish found for level " + Microbot.getClient().getRealSkillLevel(Skill.FISHING);
            return;
        }

        if (Rs2Player.isMoving() || Rs2Antiban.getCategory().isBusy()) return;

        // Execute current state
        switch (currentState) {
            case FISHING:
                handleFishing();
                break;
            case RESETTING:
                handleResetting();
                break;
            case GETTING_EQUIPMENT:
                handleGettingEquipment();
                break;
            case TRAVELING:
                handleTraveling();
                break;
        }
    }

    private void updateCurrentFishAndLocation() {
        int fishingLevel = Microbot.getClient().getRealSkillLevel(Skill.FISHING);

        if (fishingMethod == AllInOneConfig.FishingMethod.AUTO) {
            // Auto mode: select best fish based on level
            if (fishingLevel >= 82) {
                currentFish = Fish.ANGLERFISH;
            } else if (fishingLevel >= 76) {
                currentFish = Fish.SHARK;
            } else if (fishingLevel >= 62) {
                currentFish = Fish.MONKFISH;
            } else if (fishingLevel >= 50) {
                currentFish = Fish.TUNA;
            } else if (fishingLevel >= 40) {
                currentFish = Fish.LOBSTER;
            } else if (fishingLevel >= 30) {
                currentFish = Fish.PIKE;
            } else if (fishingLevel >= 20) {
                currentFish = Fish.TROUT;
            } else if (fishingLevel >= 5) {
                currentFish = Fish.SARDINE;
            } else {
                currentFish = Fish.SHRIMP;
            }
        } else {
            // Specific method selected
            switch (fishingMethod) {
                case NET:
                    if (fishingLevel >= 62) {
                        currentFish = Fish.MONKFISH;
                    } else {
                        currentFish = Fish.SHRIMP;
                    }
                    break;
                case BAIT:
                    if (fishingLevel >= 82) {
                        currentFish = Fish.ANGLERFISH;
                    } else if (fishingLevel >= 30) {
                        currentFish = Fish.PIKE;
                    } else {
                        currentFish = Fish.SARDINE;
                    }
                    break;
                case LURE:
                    currentFish = Fish.TROUT;
                    break;
                case CAGE:
                    currentFish = Fish.LOBSTER;
                    break;
                case HARPOON:
                    if (fishingLevel >= 76) {
                        currentFish = Fish.SHARK;
                    } else {
                        currentFish = Fish.TUNA;
                    }
                    break;
                default:
                    currentFish = Fish.SHRIMP;
                    break;
            }
        }
    }

    private void handleFishing() {
        // Check if we have required equipment
        if (!hasRequiredItems(currentFish)) {
            currentState = State.GETTING_EQUIPMENT;
            return;
        }

        // Check if inventory is full
        if (Rs2Inventory.isFull()) {
            currentState = State.RESETTING;
            return;
        }

        // Check if we're at the right location
        FishingLocation bestLocation = getBestLocationForFish(currentFish);
        if (bestLocation != null && Rs2Player.getWorldLocation().distanceTo(bestLocation.getFishingSpot()) > 10) {
            currentState = State.TRAVELING;
            return;
        }

        // Find fishing spot
        Rs2NpcModel fishingSpot = getFishingSpot(currentFish);
        if (fishingSpot == null) {
            Microbot.status = "Fishing: Looking for fishing spot...";
            return;
        }

        // Determine fishing action
        if (fishAction.isEmpty()) {
            fishAction = Rs2Npc.getAvailableAction(fishingSpot, currentFish.getActions());
            if (fishAction.isEmpty()) {
                Microbot.status = "Fishing: Unable to find action for fishing spot";
                return;
            }
        }

        // Ensure fishing spot is visible
        if (!Rs2Camera.isTileOnScreen(fishingSpot.getLocalLocation())) {
            validateInteractable(fishingSpot);
        }

        // Special handling for karambwan
        if (currentFish.equals(Fish.KARAMBWAN) && Rs2Inventory.hasItem(ItemID.TBWT_RAW_KARAMBWANJI)) {
            if (Rs2Inventory.hasItem(ItemID.TBWT_KARAMBWAN_VESSEL)) {
                Rs2Inventory.waitForInventoryChanges(() -> Rs2Inventory.combineClosest(ItemID.TBWT_RAW_KARAMBWANJI, ItemID.TBWT_KARAMBWAN_VESSEL), 600, 5000);
            }
        }

        // Perform fishing action
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastActionTime >= ACTION_COOLDOWN) {
            if (Rs2Npc.interact(fishingSpot, fishAction)) {
                Microbot.status = "Fishing: " + fishAction + " " + currentFish.getName();
                lastActionTime = currentTime;
                Rs2Antiban.actionCooldown();
                Rs2Antiban.takeMicroBreakByChance();
            }
        }
    }

    private void handleResetting() {
        if (mode == AllInOneConfig.GatheringMode.BANK) {
            // Banking mode
            FishingLocation currentLocation = getBestLocationForFish(currentFish);
            BankLocation nearestBank = currentLocation != null ? currentLocation.getBankLocation() : Rs2Bank.getNearestBank();

            boolean isBankOpen = Rs2Bank.isNearBank(nearestBank, 8) ? Rs2Bank.openBank() : Rs2Bank.walkToBankAndUseBank(nearestBank);
            if (!isBankOpen || !Rs2Bank.isOpen()) {
                Microbot.status = "Fishing: Opening bank...";
                return;
            }

            // First, deposit all fish
            Rs2Bank.depositAll(i -> currentFish.getRawNames().stream().anyMatch(fishName -> i.getName().equalsIgnoreCase(fishName)));
            Rs2Inventory.waitForInventoryChanges(1800);

            // Then deposit ALL items except fishing equipment
            Rs2Bank.depositAllExcept(item -> {
                String itemName = item.getName().toLowerCase();
                // Keep fishing equipment based on current fish type
                switch (currentFish) {
                    case MONKFISH:
                    case KARAMBWANJI:
                    case SHRIMP:
                        return itemName.contains("small fishing net");
                    case SARDINE:
                    case PIKE:
                        return itemName.contains("fishing rod") || itemName.contains("fishing bait");
                    case MACKEREL:
                        return itemName.contains("big fishing net");
                    case TROUT:
                        return itemName.contains("fly fishing rod") || itemName.contains("feather");
                    case TUNA:
                    case SHARK:
                        return itemName.contains("harpoon");
                    case LOBSTER:
                        return itemName.contains("lobster pot");
                    case LAVA_EEL:
                        return itemName.contains("oily fishing rod") || itemName.contains("fishing bait");
                    case CAVE_EEL:
                        return itemName.contains("fishing rod") || itemName.contains("fishing bait");
                    case ANGLERFISH:
                        return itemName.contains("fishing rod") || itemName.contains("sandworms");
                    case KARAMBWAN:
                        return itemName.contains("karambwan vessel") || itemName.contains("karambwanji");
                    case BARB_FISH:
                        return itemName.contains("barbarian rod") || itemName.contains("fishing bait") ||
                               itemName.contains("feather") || itemName.contains("fish cuts");
                    default:
                        return false;
                }
            });
            Rs2Inventory.waitForInventoryChanges(1800);

            // Close bank and return to fishing spot
            Rs2Bank.closeBank();
            Rs2Player.waitForAnimation();

            if (initialLocation != null) {
                Rs2Walker.walkTo(initialLocation);
            }

            currentState = State.FISHING;
        } else {
            // Power fishing mode - drop fish
            Rs2Inventory.dropAll(i -> currentFish.getRawNames().stream().anyMatch(fishName -> fishName.equalsIgnoreCase(i.getName())));
            Microbot.status = "Fishing: Dropped fish";
            currentState = State.FISHING;
        }
    }

    private void handleGettingEquipment() {
        FishingLocation currentLocation = getBestLocationForFish(currentFish);
        BankLocation bankLocation = currentLocation != null ? currentLocation.getBankLocation() : Rs2Bank.getNearestBank();

        boolean isBankOpen = Rs2Bank.isNearBank(bankLocation, 8) ? Rs2Bank.openBank() : Rs2Bank.walkToBankAndUseBank(bankLocation);
        if (!isBankOpen || !Rs2Bank.isOpen()) {
            Microbot.status = "Fishing: Walking to bank for equipment...";
            return;
        }

        Microbot.status = "Fishing: Getting required equipment";

        // Get required items based on fish type
        boolean gotAllItems = getRequiredItemsFromBank(currentFish);

        Rs2Bank.closeBank();
        Rs2Player.waitForAnimation();

        if (gotAllItems) {
            currentState = State.TRAVELING;
        } else {
            Microbot.status = "Fishing: Missing required equipment in bank";
        }
    }

    private void handleTraveling() {
        FishingLocation bestLocation = getBestLocationForFish(currentFish);
        if (bestLocation == null) {
            Microbot.status = "Fishing: No suitable location found";
            return;
        }

        Microbot.status = "Fishing: Traveling to " + bestLocation.getName();
        Rs2Walker.walkTo(bestLocation.getFishingSpot());

        // Update initial location once we reach the fishing spot
        if (Rs2Player.getWorldLocation().distanceTo(bestLocation.getFishingSpot()) <= 5) {
            initialLocation = Rs2Player.getWorldLocation();
            currentState = State.FISHING;
        }
    }

    private boolean hasRequiredItems(Fish fish) {
        switch (fish) {
            case MONKFISH:
            case KARAMBWANJI:
            case SHRIMP:
                return Rs2Inventory.hasItem("small fishing net");
            case SARDINE:
            case PIKE:
                return Rs2Inventory.hasItem("fishing rod") && Rs2Inventory.hasItem("fishing bait");
            case MACKEREL:
                return Rs2Inventory.hasItem("big fishing net");
            case TROUT:
                return Rs2Inventory.hasItem("fly fishing rod") && Rs2Inventory.hasItem("feather");
            case TUNA:
            case SHARK:
                return Rs2Inventory.hasItem("harpoon") || Rs2Equipment.isWearing("harpoon");
            case LOBSTER:
                return Rs2Inventory.hasItem("lobster pot");
            case LAVA_EEL:
                return Rs2Inventory.hasItem("oily fishing rod") && Rs2Inventory.hasItem("fishing bait");
            case CAVE_EEL:
                return Rs2Inventory.hasItem("fishing rod") && Rs2Inventory.hasItem("fishing bait");
            case ANGLERFISH:
                return Rs2Inventory.hasItem("fishing rod") && Rs2Inventory.hasItem("sandworms");
            case KARAMBWAN:
                return (Rs2Inventory.hasItem(ItemID.TBWT_KARAMBWAN_VESSEL) || Rs2Inventory.hasItem(ItemID.TBWT_KARAMBWAN_VESSEL_LOADED_WITH_KARAMBWANJI)) && Rs2Inventory.hasItem(ItemID.TBWT_RAW_KARAMBWANJI);
            case BARB_FISH:
                return Rs2Inventory.hasItem(ItemID.BRUT_FISHING_ROD) && (Rs2Inventory.hasItem(ItemID.FISHING_BAIT) || Rs2Inventory.hasItem(ItemID.FEATHER) || Rs2Inventory.hasItem(ItemID.BRUT_FISH_CUTS));
            default:
                return false;
        }
    }

    private boolean getRequiredItemsFromBank(Fish fish) {
        switch (fish) {
            case MONKFISH:
            case KARAMBWANJI:
            case SHRIMP:
                return Rs2Bank.withdrawItem(true, "small fishing net");
            case SARDINE:
            case PIKE:
                boolean hasRod = Rs2Bank.withdrawItem(true, "fishing rod");
                boolean hasBait = Rs2Bank.withdrawX(true, "fishing bait", 1000);
                return hasRod && hasBait;
            case MACKEREL:
                return Rs2Bank.withdrawItem(true, "big fishing net");
            case TROUT:
                boolean hasFlyRod = Rs2Bank.withdrawItem(true, "fly fishing rod");
                boolean hasFeathers = Rs2Bank.withdrawX(true, "feather", 1000);
                return hasFlyRod && hasFeathers;
            case TUNA:
            case SHARK:
                return Rs2Bank.withdrawItem(true, "harpoon");
            case LOBSTER:
                return Rs2Bank.withdrawItem(true, "lobster pot");
            case ANGLERFISH:
                boolean hasRodAng = Rs2Bank.withdrawItem(true, "fishing rod");
                boolean hasSandworms = Rs2Bank.withdrawX(true, "sandworms", 1000);
                return hasRodAng && hasSandworms;
            default:
                return false;
        }
    }

    private FishingLocation getBestLocationForFish(Fish fish) {
        WorldPoint playerLocation = Rs2Player.getWorldLocation();

        return FISHING_LOCATIONS.stream()
                .filter(location -> Arrays.asList(location.getAvailableFish()).contains(fish))
                .filter(location -> Microbot.getClient().getRealSkillLevel(Skill.FISHING) >= location.getMinLevel())
                .min(Comparator.comparing(location -> location.distanceFrom(playerLocation)))
                .orElse(null);
    }

    private Rs2NpcModel getFishingSpot(Fish fish) {
        return Arrays.stream(fish.getFishingSpot())
                .mapToObj(Rs2Npc::getNpc)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    // Getters for debugging/status
    public boolean isEnabled() { return enabled; }
    public AllInOneConfig.GatheringMode getMode() { return mode; }
    public AllInOneConfig.FishingMethod getFishingMethod() { return fishingMethod; }
    public Fish getCurrentFish() { return currentFish; }
    public State getCurrentState() { return currentState; }
}
