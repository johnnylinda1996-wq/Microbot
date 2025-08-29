package net.runelite.client.plugins.microbot.jpnl.lanterns;

import net.runelite.api.ItemID;
import net.runelite.api.NpcID;
import net.runelite.api.ObjectID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.skillcalculator.skills.MagicAction;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class FillLanternScript extends Script {

    public static String version = "2.1";

    public State currentState = State.BANKING;
    private int profitCount = 0;
    private long startTime;
    private boolean breakHandlerState = false;
    private int failedPathfinderAttempts = 0;
    private static final int MAX_PATHFINDER_ATTEMPTS = 5;
    private boolean justExitedHouse = false; // Flag to prevent immediate re-teleporting

    // Item IDs
    private static final int BULLSEYE_LANTERN_EMPTY_ID = 4546; // Corrected ItemID
    private static final int BULLSEYE_LANTERN_FILLED_ID = ItemID.BULLSEYE_LANTERN_4550;
    private static final int SWAMP_TAR_ID = ItemID.SWAMP_TAR;
    private static final int STAMINA_POTION_ID = ItemID.STAMINA_POTION4;

    // Rune IDs
    private static final int AIR_RUNE_ID = ItemID.AIR_RUNE;
    private static final int EARTH_RUNE_ID = ItemID.EARTH_RUNE;
    private static final int LAW_RUNE_ID = ItemID.LAW_RUNE;
    private static final int WATER_RUNE_ID = ItemID.WATER_RUNE;

    // Staff IDs
    private static final int AIR_STAFF_ID = ItemID.STAFF_OF_AIR;
    private static final int EARTH_STAFF_ID = ItemID.STAFF_OF_EARTH;
    private static final int MYSTIC_AIR_STAFF_ID = ItemID.MYSTIC_AIR_STAFF;
    private static final int MYSTIC_EARTH_STAFF_ID = ItemID.MYSTIC_EARTH_STAFF;
    private static final int[] AIR_STAVES = {AIR_STAFF_ID, MYSTIC_AIR_STAFF_ID, ItemID.AIR_BATTLESTAFF};
    private static final int[] EARTH_STAVES = {EARTH_STAFF_ID, MYSTIC_EARTH_STAFF_ID, ItemID.EARTH_BATTLESTAFF};

    // Object IDs
    private static final Integer[] STILL_OBJECT_IDS = {10005}; // The lamp/still object ID as an array
    private static final Integer[] HOUSE_PORTAL_IDS = {4525}; // House portal to exit

    // Locations
    private static final WorldPoint FALADOR_EAST_BANK = new WorldPoint(3013, 3355, 0);
    private static final WorldPoint FALADOR_WEST_BANK = new WorldPoint(2946, 3368, 0);
    private static final WorldPoint DRAYNOR_BANK = new WorldPoint(3092, 3243, 0);
    private static final WorldPoint CAMELOT_TELEPORT_LOCATION = new WorldPoint(2757, 3479, 0);
    private static final WorldPoint SEERS_BANK = new WorldPoint(2726, 3491, 0);
    private static final WorldPoint RIMMINGTON_STILL = new WorldPoint(2935, 3209, 0); // Location of the lamp/still

    // Player-owned house region (approximate)
    private static final int POH_REGION = 7499;

    public enum State {
        BANKING,
        WALKING_TO_STILL,
        FILLING_LANTERNS,
        RETURNING_TO_BANK,
        EXITING_HOUSE
    }

    public boolean run(FillLanternConfig config, FillLanternPlugin plugin) {
        initialPlayerLocation = null;
        startTime = System.currentTimeMillis();

        // Store the user's break handler preference
        breakHandlerState = config.enableBreakHandler();
        failedPathfinderAttempts = 0;

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;

                if (initialPlayerLocation == null) {
                    initialPlayerLocation = Rs2Player.getWorldLocation();
                }

                Rs2Combat.enableAutoRetialiate();


                // Check if in house and handle it first
                if (isInHouse() && currentState != State.EXITING_HOUSE) {
                    Microbot.log("In house, switching to EXITING_HOUSE state");
                    currentState = State.EXITING_HOUSE;
                    handleState(config);
                    return;
                }

                // Reset the flag if we're no longer in the house
                if (!isInHouse()) {
                    justExitedHouse = false;
                }

                handleState(config);

            } catch (Exception ex) {
                Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
            }
        }, 0, Rs2Random.between(600, 1200), TimeUnit.MILLISECONDS);

        return true;
    }

    private void handleState(FillLanternConfig config) {
        switch (currentState) {
            case BANKING:
                handleBanking(config);
                break;
            case WALKING_TO_STILL:
                handleWalkingToStill(config);
                break;
            case FILLING_LANTERNS:
                handleFillingLanterns(config);
                break;
            case RETURNING_TO_BANK:
                handleReturningToBank(config);
                break;
            case EXITING_HOUSE:
                handleExitingHouse();
                break;
        }
    }

    private void handleExitingHouse() {
        Microbot.log("Attempting to exit house via portal");
        if (Rs2GameObject.findObject(HOUSE_PORTAL_IDS) != null) {
            Rs2GameObject.interact(Arrays.toString(HOUSE_PORTAL_IDS), "Enter");
            sleepUntil(() -> !isInHouse(), 5000);

            if (!isInHouse()) {
                Microbot.log("Successfully exited house");
                justExitedHouse = true;
                currentState = State.RETURNING_TO_BANK;
            } else {
                Microbot.log("Failed to exit house, trying again");
                sleep(Rs2Random.between(1000, 2000));
            }
        } else {
            Microbot.log("Could not find house portal, looking around");
            // Try to find the portal by moving a bit
            WorldPoint currentPos = Rs2Player.getWorldLocation();
            WorldPoint newPos = new WorldPoint(
                    currentPos.getX() + Rs2Random.between(-3, 3),
                    currentPos.getY() + Rs2Random.between(-3, 3),
                    currentPos.getPlane()
            );
            Rs2Walker.walkTo(newPos);
        }
    }

    private void handleBanking(FillLanternConfig config) {
        // Check if we're in a bank area
        if (!isNearAnyBank()) {
            if (isInHouse()) {
                Microbot.log("In house, need to exit for banking");
                currentState = State.EXITING_HOUSE;
                return;
            } else if (Rs2Player.getWorldLocation().distanceTo(CAMELOT_TELEPORT_LOCATION) < 20) {
                // Try walking to Seers' bank since we're near Camelot teleport spot
                Microbot.log("Near Camelot teleport, walking to Seers bank");
                safeWalkTo(SEERS_BANK);
                return;
            } else {
                // If we're not near any known location, try to get to Falador bank
                Microbot.log("Not near a bank, walking to Falador bank");
                safeWalkTo(FALADOR_EAST_BANK);
                return;
            }
        }

        if (!Rs2Bank.isOpen()) {
            Microbot.log("Opening bank...");
            Rs2Bank.openBank();
            sleepUntil(() -> Rs2Bank.isOpen(), 3000);
            return;
        }

        // Deposit filled lanterns for profit tracking
        if (Rs2Inventory.hasItem(BULLSEYE_LANTERN_FILLED_ID)) {
            int filledCount = Rs2Inventory.count(BULLSEYE_LANTERN_FILLED_ID);
            Microbot.log("Depositing " + filledCount + " filled lanterns");
            Rs2Bank.depositAll(BULLSEYE_LANTERN_FILLED_ID);
            profitCount += filledCount;
            sleepUntil(() -> !Rs2Inventory.hasItem(BULLSEYE_LANTERN_FILLED_ID), 2000);
            return;
        }

        // Deposit everything first to start fresh
        Microbot.log("Depositing all items");
        Rs2Bank.depositAll();
        sleepUntil(() -> Rs2Inventory.isEmpty(), 2000);
        sleep(Rs2Random.between(500, 800));

        // Log bank contents for debugging
        Microbot.log("Bank contains: Empty lanterns: " + Rs2Bank.count(BULLSEYE_LANTERN_EMPTY_ID) +
                ", Swamp tar: " + Rs2Bank.count(SWAMP_TAR_ID) +
                ", Law runes: " + Rs2Bank.count(LAW_RUNE_ID));

        // Check if we have the supplies in bank
        boolean hasLanterns = Rs2Bank.count(BULLSEYE_LANTERN_EMPTY_ID) > 0;
        boolean hasSwampTar = Rs2Bank.count(SWAMP_TAR_ID) > 0;

        if (!hasLanterns || !hasSwampTar) {
            Microbot.log("CRITICAL: Missing essential supplies in bank! Empty lanterns: " +
                    hasLanterns + ", Swamp tar: " + hasSwampTar);
            sleep(5000); // Wait a bit before checking again
            return;
        }

        // Check for and equip a single magic staff
        equipPreferredStaff();

        // Withdraw empty lanterns
        int lanternAmount = Rs2Random.between(config.minLanterns(), config.maxLanterns());
        Microbot.log("Withdrawing " + lanternAmount + " empty lanterns");
        Rs2Bank.withdrawX(BULLSEYE_LANTERN_EMPTY_ID, lanternAmount);
        sleepUntil(() -> Rs2Inventory.hasItem(BULLSEYE_LANTERN_EMPTY_ID), 2000);

        // Verify lanterns were withdrawn
        if (!Rs2Inventory.hasItem(BULLSEYE_LANTERN_EMPTY_ID)) {
            Microbot.log("Failed to withdraw lanterns, retrying...");
            return;
        }

        // Withdraw swamp tar - same amount as lanterns
        int lanternCount = Rs2Inventory.count(BULLSEYE_LANTERN_EMPTY_ID);
        Microbot.log("Withdrawing " + lanternCount + " swamp tar");
        Rs2Bank.withdrawX(SWAMP_TAR_ID, lanternCount);
        sleepUntil(() -> Rs2Inventory.hasItem(SWAMP_TAR_ID), 2000);

        // Verify tar was withdrawn
        if (!Rs2Inventory.hasItem(SWAMP_TAR_ID)) {
            Microbot.log("Failed to withdraw swamp tar, retrying...");
            return;
        }

        // Withdraw stamina potion if needed
        if (config.useStamina() && Rs2Player.getRunEnergy() < 30) {
            Microbot.log("Withdrawing stamina potion");
            Rs2Bank.withdrawX(STAMINA_POTION_ID, 1);
            sleep(Rs2Random.between(500, 800));
        }

        // Withdraw teleport runes if using rune teleport method
        if (config.teleportMethod() == FillLanternConfig.TeleportMethod.RUNES_TELEPORT) {
            withdrawMaxRunes();
        }

        Rs2Bank.closeBank();
        sleep(Rs2Random.between(500, 800));

        // Verify we have what we need after banking
        if (Rs2Inventory.hasItem(BULLSEYE_LANTERN_EMPTY_ID) && Rs2Inventory.hasItem(SWAMP_TAR_ID)) {
            Microbot.log("Successfully obtained supplies from bank");
            currentState = State.WALKING_TO_STILL;
        } else {
            Microbot.log("Failed to get all supplies, staying in banking state");
        }
    }

    private void handleWalkingToStill(FillLanternConfig config) {
        // Use stamina potion if needed
        if (config.useStamina() && Rs2Player.getRunEnergy() < 30 && Rs2Inventory.hasItem(STAMINA_POTION_ID)) {
            Microbot.log("Drinking stamina potion");
            Rs2Inventory.interact(STAMINA_POTION_ID, "Drink");
            sleep(Rs2Random.between(1000, 2000));
        }

        // Check if we're already near the still
        if (Rs2Player.getWorldLocation().distanceTo(RIMMINGTON_STILL) < 5) {
            Microbot.log("Reached the still in Rimmington");
            currentState = State.FILLING_LANTERNS;
            return;
        }

        // Try teleporting if option is enabled and we haven't just exited the house
        if (config.teleportMethod() == FillLanternConfig.TeleportMethod.RUNES_TELEPORT && !justExitedHouse) {
            boolean teleported = tryTeleportToRimmington();
            if (teleported) {
                Microbot.log("Successfully teleported near Rimmington");
            } else {
                Microbot.log("Teleport failed or not available, walking instead");
            }
        }

        // Walk to the still regardless of teleport success
        Microbot.log("Walking to the still in Rimmington");
        safeWalkTo(RIMMINGTON_STILL);
    }

    /**
     * Attempt to teleport to Rimmington area
     * @return true if teleport was successful
     */
    private boolean tryTeleportToRimmington() {
        // Check requirements for house teleport
        boolean hasAirStaff = hasStaffEquipped(AIR_STAVES);
        boolean hasEarthStaff = hasStaffEquipped(EARTH_STAVES);
        boolean hasAirRunes = Rs2Inventory.hasItem(AIR_RUNE_ID);
        boolean hasEarthRunes = Rs2Inventory.hasItem(EARTH_RUNE_ID);
        boolean hasLawRunes = Rs2Inventory.hasItem(LAW_RUNE_ID);

        boolean canCastHouseTele = Rs2Magic.canCast(MagicAction.TELEPORT_TO_HOUSE);

        if (!canCastHouseTele) {
            Microbot.log("Cannot cast house teleport - missing requirements");
            return false;
        }

        Microbot.log("Casting House Teleport...");
        Rs2Magic.cast(MagicAction.TELEPORT_TO_HOUSE);

        // Try to select "Outside" option if dialog appears
        sleepUntil(() -> Microbot.getClient().isMenuOpen(), 3000);
        if (Microbot.getClient().isMenuOpen()) {
            Microbot.log("Menu is open, trying to select 'Outside'");
            Microbot.getMouse().click(252, 405); // Attempt to click "Outside" option
        }

        // Wait for teleport to complete
        sleep(Rs2Random.between(2000, 3000));

        // Check if we're now closer to Rimmington
        if (Rs2Player.getWorldLocation().distanceTo(RIMMINGTON_STILL) < 50) {
            return true;
        }

        // If we ended up in the house, we'll handle it in the next tick
        if (isInHouse()) {
            Microbot.log("Teleported inside house, will exit via portal");
            return false;
        }

        return false;
    }

    /**
     * Equips preferred staff (air staff preferred over earth staff)
     * Only equips one staff and banks any others
     */
    private void equipPreferredStaff() {
        // First check if we already have a staff equipped
        boolean hasAirStaffEquipped = hasStaffEquipped(AIR_STAVES);
        boolean hasEarthStaffEquipped = hasStaffEquipped(EARTH_STAVES);

        if (hasAirStaffEquipped || hasEarthStaffEquipped) {
            Microbot.log("Already have a staff equipped: " +
                    (hasAirStaffEquipped ? "Air staff" : "Earth staff"));
            return;
        }

        // Check for air staff first (preferred)
        int staffToEquip = -1;

        for (int staffId : AIR_STAVES) {
            if (Rs2Bank.count(staffId) > 0) {
                staffToEquip = staffId;
                Microbot.log("Found air staff in bank, will equip");
                break;
            }
        }

        // If no air staff, try earth staff
        if (staffToEquip == -1) {
            for (int staffId : EARTH_STAVES) {
                if (Rs2Bank.count(staffId) > 0) {
                    staffToEquip = staffId;
                    Microbot.log("Found earth staff in bank, will equip");
                    break;
                }
            }
        }

        if (staffToEquip != -1) {
            // Withdraw and equip the selected staff
            Rs2Bank.withdrawX(staffToEquip, 1);
            sleep(Rs2Random.between(500, 800));

            if (Rs2Inventory.hasItem(staffToEquip)) {
                Rs2Inventory.interact(staffToEquip, "Wield");
                sleep(Rs2Random.between(800, 1200));
                Microbot.log("Equipped staff: " + staffToEquip);
            }
        } else {
            Microbot.log("No magic staff found in bank");
        }
    }

    /**
     * Checks if one of the staves in the array is equipped
     */
    private boolean hasStaffEquipped(int[] staffIds) {
        for (int staffId : staffIds) {
            if (Rs2Equipment.isWearing(staffId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Withdraws maximum amount of runes needed for teleports
     * @return true if all runes were obtained
     */
    private boolean withdrawMaxRunes() {
        boolean success = true;
        boolean hasAirStaff = hasStaffEquipped(AIR_STAVES);
        boolean hasEarthStaff = hasStaffEquipped(EARTH_STAVES);

        // Air runes (needed for both teleports) - only if no air staff is equipped
        if (!hasAirStaff) {
            Microbot.log("Withdrawing air runes (no air staff equipped)");
            Rs2Bank.withdrawAll(AIR_RUNE_ID);
            sleep(Rs2Random.between(300, 500));
            if (!Rs2Inventory.hasItem(AIR_RUNE_ID)) {
                Microbot.log("Failed to withdraw air runes");
                success = false;
            }
        }

        // Earth runes (for house teleport) - only if no earth staff is equipped
        if (!hasEarthStaff) {
            Microbot.log("Withdrawing earth runes (no earth staff equipped)");
            Rs2Bank.withdrawAll(EARTH_RUNE_ID);
            sleep(Rs2Random.between(300, 500));
            if (!Rs2Inventory.hasItem(EARTH_RUNE_ID)) {
                Microbot.log("Failed to withdraw earth runes");
                success = false;
            }
        }

        // Law runes (needed for both teleports)
        Microbot.log("Withdrawing law runes");
        Rs2Bank.withdrawAll(LAW_RUNE_ID);
        sleep(Rs2Random.between(300, 500));
        if (!Rs2Inventory.hasItem(LAW_RUNE_ID)) {
            Microbot.log("Failed to withdraw law runes");
            success = false;
        }

        // Water runes (for Camelot teleport)
        Microbot.log("Withdrawing water runes");
        Rs2Bank.withdrawAll(WATER_RUNE_ID);
        sleep(Rs2Random.between(300, 500));
        if (!Rs2Inventory.hasItem(WATER_RUNE_ID)) {
            Microbot.log("Failed to withdraw water runes");
            success = false;
        }

        return success;
    }

    private boolean isNearAnyBank() {
        return Rs2Player.getWorldLocation().distanceTo(FALADOR_EAST_BANK) < 15 ||
                Rs2Player.getWorldLocation().distanceTo(FALADOR_WEST_BANK) < 15 ||
                Rs2Player.getWorldLocation().distanceTo(DRAYNOR_BANK) < 15 ||
                Rs2Player.getWorldLocation().distanceTo(SEERS_BANK) < 15;
    }

    private boolean isInHouse() {
        return Rs2Player.getWorldLocation().getRegionID() == POH_REGION;
    }

    private void handleFillingLanterns(FillLanternConfig config) {
        // First check if we still have the items needed
        if (!Rs2Inventory.hasItem(BULLSEYE_LANTERN_EMPTY_ID) || !Rs2Inventory.hasItem(SWAMP_TAR_ID)) {
            Microbot.log("Out of lanterns or tar, returning to bank");
            currentState = State.RETURNING_TO_BANK;
            return;
        }

        // Try to find the still object
        if (Rs2GameObject.findObject(STILL_OBJECT_IDS) != null) {
            // First select the swamp tar
            Microbot.log("Selecting swamp tar");
            Rs2Inventory.interact(SWAMP_TAR_ID, "Use");
            sleep(Rs2Random.between(600, 1000));

            // Then use it on the still
            Microbot.log("Using swamp tar on the still");
            Rs2GameObject.interact(Arrays.toString(STILL_OBJECT_IDS), "Use");

            // Wait for the action to complete
            sleep(Rs2Random.between(1800, 2500));

            // Check if we have filled lanterns now
            if (Rs2Inventory.hasItem(BULLSEYE_LANTERN_FILLED_ID)) {
                Microbot.log("Successfully filled lanterns");
            }

            // If we are out of empty lanterns or swamp tar, go back to bank
            if (!Rs2Inventory.hasItem(BULLSEYE_LANTERN_EMPTY_ID) || !Rs2Inventory.hasItem(SWAMP_TAR_ID)) {
                Microbot.log("Finished filling lanterns, returning to bank");
                currentState = State.RETURNING_TO_BANK;
            }
        } else {
            Microbot.log("Cannot find the still, repositioning");
            // Try to get closer to the still
            safeWalkTo(RIMMINGTON_STILL);
        }
    }

    private void handleReturningToBank(FillLanternConfig config) {
        // Check if we're already at a bank
        if (isNearAnyBank()) {
            Microbot.log("Already at a bank");
            currentState = State.BANKING;
            return;
        }

        if (config.teleportMethod() == FillLanternConfig.TeleportMethod.RUNES_TELEPORT && !justExitedHouse) {
            // Check for staves
            boolean hasAirStaff = hasStaffEquipped(AIR_STAVES);
            boolean hasLawRunes = Rs2Inventory.hasItem(LAW_RUNE_ID);
            boolean hasWaterRunes = Rs2Inventory.hasItem(WATER_RUNE_ID);
            boolean hasAirRunes = Rs2Inventory.hasItem(AIR_RUNE_ID);

            Microbot.log("Camelot teleport requirements: Air staff: " + hasAirStaff +
                    ", Air runes: " + hasAirRunes +
                    ", Water runes: " + hasWaterRunes +
                    ", Law runes: " + hasLawRunes);

            // Check if we can cast Camelot teleport
            boolean canCastCamelotTele = Rs2Magic.canCast(MagicAction.CAMELOT_TELEPORT);

            if (canCastCamelotTele) {
                Microbot.log("Teleporting to Camelot for banking...");
                Rs2Magic.cast(MagicAction.CAMELOT_TELEPORT);

                // Wait for teleport to complete
                sleepUntil(() -> Rs2Player.getWorldLocation().distanceTo(CAMELOT_TELEPORT_LOCATION) < 10, 5000);

                if (Rs2Player.getWorldLocation().distanceTo(CAMELOT_TELEPORT_LOCATION) < 10) {
                    Microbot.log("Successfully teleported to Camelot");
                    safeWalkTo(SEERS_BANK);
                    return;
                } else {
                    Microbot.log("Camelot teleport failed");
                }
            } else {
                Microbot.log("Cannot cast Camelot teleport - missing requirements.");
            }
        }

        // Fallback to walking to the nearest bank
        Microbot.log("Walking to nearest bank");

        // Find the closest bank
        WorldPoint[] bankLocations = {FALADOR_EAST_BANK, FALADOR_WEST_BANK, DRAYNOR_BANK};
        WorldPoint closestBank = FALADOR_EAST_BANK;
        int minDistance = Integer.MAX_VALUE;

        for (WorldPoint bank : bankLocations) {
            int distance = Rs2Player.getWorldLocation().distanceTo(bank);
            if (distance < minDistance) {
                minDistance = distance;
                closestBank = bank;
            }
        }

        Microbot.log("Walking to " + getBankName(closestBank));
        safeWalkTo(closestBank);
    }

    private String getBankName(WorldPoint bankLocation) {
        if (bankLocation.equals(FALADOR_EAST_BANK)) return "Falador East Bank";
        if (bankLocation.equals(FALADOR_WEST_BANK)) return "Falador West Bank";
        if (bankLocation.equals(DRAYNOR_BANK)) return "Draynor Bank";
        if (bankLocation.equals(SEERS_BANK)) return "Seers' Bank";
        return "Unknown Bank";
    }

    /**
     * Safe wrapper around Rs2Walker.walkTo to handle pathfinding errors
     */
    private void safeWalkTo(WorldPoint destination) {
        try {
            // Reset failed attempts counter if destination changed
            if (Rs2Player.getWorldLocation().distanceTo(destination) > 50) {
                failedPathfinderAttempts = 0;
            }

            // If too many failed attempts, try a different bank
            if (failedPathfinderAttempts >= MAX_PATHFINDER_ATTEMPTS) {
                if (destination.equals(FALADOR_EAST_BANK)) {
                    Microbot.log("Too many failed pathfinding attempts to Falador East Bank. Trying Falador West Bank.");
                    destination = FALADOR_WEST_BANK;
                    failedPathfinderAttempts = 0;
                } else if (destination.equals(FALADOR_WEST_BANK)) {
                    Microbot.log("Too many failed pathfinding attempts to Falador West Bank. Trying Draynor Bank.");
                    destination = DRAYNOR_BANK;
                    failedPathfinderAttempts = 0;
                } else if (destination.equals(SEERS_BANK)) {
                    Microbot.log("Too many failed pathfinding attempts to Seers Bank. Trying Falador Bank.");
                    destination = FALADOR_EAST_BANK;
                    failedPathfinderAttempts = 0;
                }
            }

            Rs2Walker.walkTo(destination);

        } catch (Exception e) {
            Microbot.log("Pathfinding error: " + e.getMessage());
            failedPathfinderAttempts++;

            // If pathfinding fails repeatedly, try to move a short distance randomly
            if (failedPathfinderAttempts >= 3) {
                WorldPoint currentPos = Rs2Player.getWorldLocation();
                WorldPoint newPos = new WorldPoint(
                        currentPos.getX() + Rs2Random.between(-5, 5),
                        currentPos.getY() + Rs2Random.between(-5, 5),
                        currentPos.getPlane()
                );
                Microbot.log("Trying to move to a random nearby position to reset pathfinding.");
                try {
                    Rs2Walker.walkTo(newPos);
                } catch (Exception e2) {
                    // If even that fails, just wait a bit
                    Microbot.log("Even random movement failed. Waiting before retrying.");
                    sleep(Rs2Random.between(3000, 5000));
                }
            }
        }
    }

    public int getProfitCount() {
        return profitCount;
    }

    public State getCurrentState() {
        return currentState;
    }

    public long getStartTime() {
        return startTime;
    }

    public boolean isBreakHandlerEnabled() {
        return breakHandlerState;
    }
}