package net.runelite.client.plugins.microbot.aiobot.skills;

import net.runelite.api.GameObject;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.aiobot.settings.SkillRuntimeSettings;
import net.runelite.client.plugins.microbot.mining.enums.Rocks;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.security.Login;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.*;

/**
 * Complete Mining skill handler voor AIO bot system.
 * Ondersteunt alle ore types, locatie detectie, banking, world hopping.
 */
public class MiningSkillHandler implements SkillHandler {

    private boolean enabled = true;
    private String mode = "drop";
    private Rocks currentOre = Rocks.COPPER;
    private WorldPoint initialPlayerLocation;
    private boolean useBank = false;
    private int maxPlayersInArea = 3;
    private int distanceToStray = 15;

    // Mining locations per ore type
    private static final Map<Rocks, List<WorldPoint>> MINING_LOCATIONS = new HashMap<>();

    static {
        MINING_LOCATIONS.put(Rocks.COPPER, Arrays.asList(
                new WorldPoint(3077, 3421, 0), // Barbarian Village
                new WorldPoint(3298, 3312, 0), // Al Kharid North
                new WorldPoint(3037, 9775, 0), // Dwarven Mines
                new WorldPoint(2580, 3484, 0), // Seers Village
                new WorldPoint(1430, 2882, 0)  // Aldarin
        ));

        MINING_LOCATIONS.put(Rocks.TIN, Arrays.asList(
                new WorldPoint(3077, 3421, 0), // Barbarian Village
                new WorldPoint(3298, 3312, 0), // Al Kharid North
                new WorldPoint(3051, 9820, 0), // Dwarven Mines East Top
                new WorldPoint(2580, 3484, 0)  // Seers Village
        ));

        MINING_LOCATIONS.put(Rocks.IRON, Arrays.asList(
                new WorldPoint(3298, 3312, 0), // Al Kharid North
                new WorldPoint(3032, 9720, 0), // Mining Guild South
                new WorldPoint(3037, 9775, 0), // Dwarven Mines
                new WorldPoint(2724, 3693, 0), // Keldagrim Entrance
                new WorldPoint(1430, 2882, 0)  // Aldarin
        ));

        MINING_LOCATIONS.put(Rocks.COAL, Arrays.asList(
                new WorldPoint(2580, 3484, 0), // Coal Trucks
                new WorldPoint(3040, 9740, 0), // Mining Guild North
                new WorldPoint(3032, 9720, 0), // Mining Guild South
                new WorldPoint(2374, 3850, 0), // Central Fremenik Isles
                new WorldPoint(1430, 2882, 0)  // Aldarin
        ));

        MINING_LOCATIONS.put(Rocks.MITHRIL, Arrays.asList(
                new WorldPoint(3032, 9720, 0), // Mining Guild South
                new WorldPoint(3040, 9740, 0), // Mining Guild North
                new WorldPoint(3037, 9775, 0), // Dwarven Mines
                new WorldPoint(2848, 3033, 0)  // Karamja Jungle
        ));

        MINING_LOCATIONS.put(Rocks.ADAMANTITE, Arrays.asList(
                new WorldPoint(3032, 9720, 0), // Mining Guild South
                new WorldPoint(3040, 9740, 0), // Mining Guild North
                new WorldPoint(2848, 3033, 0), // Karamja Jungle
                new WorldPoint(1936, 9020, 0)  // Myths Guild
        ));

        MINING_LOCATIONS.put(Rocks.RUNITE, Arrays.asList(
                new WorldPoint(3032, 9720, 0), // Mining Guild South
                new WorldPoint(2374, 3850, 0), // Central Fremenik Isles
                new WorldPoint(1936, 9020, 0)  // Myths Guild
        ));
    }

    @Override
    public void applySettings(SkillRuntimeSettings settings) {
        if (settings != null) {
            enabled = settings.isEnabled();
            mode = settings.getMode() != null ? settings.getMode().toLowerCase() : "drop";
            useBank = "bank".equals(mode);
        }
    }

    @Override
    public void execute() {
        if (!enabled) {
            Microbot.status = "Mining: disabled";
            return;
        }

        if (!Microbot.isLoggedIn()) return;
        if (Rs2AntibanSettings.actionCooldownActive) return;

        if (initialPlayerLocation == null) {
            initialPlayerLocation = Rs2Player.getWorldLocation();
        }

        updateCurrentOre();

        if (!currentOre.hasRequiredLevel()) {
            Microbot.status = "Mining: insufficient level for " + currentOre.name();
            return;
        }

        // Dragon pickaxe special attack
        if (Rs2Equipment.isWearing("Dragon pickaxe") && Rs2Combat.getSpecEnergy() == 1000) {
            Rs2Combat.setSpecState(true, 1000);
            return;
        }

        if (Rs2Player.isMoving() || Rs2Player.isAnimating()) return;

        if (handleWorldHopping()) return;

        if (Rs2Inventory.isFull()) {
            handleFullInventory();
            return;
        }

        WorldPoint bestLocation = findBestMiningLocation();
        if (bestLocation != null && !isAtMiningLocation(bestLocation)) {
            walkToMiningLocation(bestLocation);
            return;
        }

        mineRock();
    }

    private void updateCurrentOre() {
        int miningLevel = Microbot.getClient().getRealSkillLevel(Skill.MINING);

        if (miningLevel >= 85) {
            currentOre = Rocks.RUNITE;
        } else if (miningLevel >= 70) {
            currentOre = Rocks.ADAMANTITE;
        } else if (miningLevel >= 55) {
            currentOre = Rocks.MITHRIL;
        } else if (miningLevel >= 30) {
            currentOre = Rocks.COAL;
        } else if (miningLevel >= 15) {
            currentOre = Rocks.IRON;
        } else if (miningLevel >= 1) {
            currentOre = Rocks.TIN;
        } else {
            currentOre = Rocks.COPPER;
        }
    }

    private WorldPoint findBestMiningLocation() {
        List<WorldPoint> locations = MINING_LOCATIONS.get(currentOre);
        if (locations == null || locations.isEmpty()) {
            return null;
        }

        WorldPoint playerLoc = Rs2Player.getWorldLocation();

        return locations.stream()
                .filter(this::isLocationAccessible)
                .min(Comparator.comparingInt(loc -> loc.distanceTo(playerLoc)))
                .orElse(locations.get(0));
    }

    private boolean isLocationAccessible(WorldPoint location) {
        return !requiresMembers(location) || Rs2Player.isMember();
    }

    private boolean requiresMembers(WorldPoint location) {
        int x = location.getX();
        int y = location.getY();

        // F2P mining locations
        if ((x >= 3070 && x <= 3100 && y >= 3410 && y <= 3440) || // Barbarian Village
                (x >= 3280 && x <= 3320 && y >= 3300 && y <= 3330) || // Al Kharid
                (x >= 3020 && x <= 3060 && y >= 9700 && y <= 9850)) { // Dwarven mines
            return false;
        }

        return true;
    }

    private boolean isAtMiningLocation(WorldPoint location) {
        return Rs2Player.getWorldLocation().distanceTo(location) <= distanceToStray;
    }

    private void walkToMiningLocation(WorldPoint location) {
        if (Rs2Walker.walkTo(location, 3)) {
            Microbot.status = "Mining: walking to " + currentOre.name() + " location";
        }
    }

    private void mineRock() {
        // Gebruik de correcte API signature
        GameObject rock = Rs2GameObject.findReachableObject(currentOre.getName(), true, distanceToStray, initialPlayerLocation);

        if (rock != null) {
            if (Rs2GameObject.interact(rock)) {
                Rs2Player.waitForXpDrop(Skill.MINING, true);
                Rs2Antiban.actionCooldown();
                Rs2Antiban.takeMicroBreakByChance();
                Microbot.status = "Mining: " + currentOre.name();
            }
        } else {
            Microbot.status = "Mining: no " + currentOre.name() + " found";
        }
    }

    private boolean handleWorldHopping() {
        if (maxPlayersInArea <= 0) return false;

        WorldPoint localLocation = Rs2Player.getWorldLocation();

        // Gebruik de correcte API voor players
        long nearbyPlayers = Microbot.getClient().getPlayers().stream()
                .filter(p -> p != null && p != Microbot.getClient().getLocalPlayer())
                .filter(p -> p.getWorldLocation().distanceTo(localLocation) <= distanceToStray)
                .filter(p -> p.getAnimation() != -1)
                .count();

        if (nearbyPlayers >= maxPlayersInArea) {
            Microbot.status = "Mining: too many players, hopping...";
            Rs2Random.waitEx(3200, 800);

            int world = Login.getRandomWorld(Rs2Player.isMember());
            boolean hopped = Microbot.hopToWorld(world);
            if (hopped) {
                Microbot.status = "Mining: hopped to world " + world;
                return true;
            }
        }

        return false;
    }

    private void handleFullInventory() {
        if (useBank) {
            handleBanking();
        } else {
            handleDropping();
        }
    }

    private void handleBanking() {
        BankLocation nearestBank = Rs2Bank.getNearestBank();

        if (nearestBank != null && Rs2Bank.walkToBankAndUseBank(nearestBank)) {
            if (Rs2Bank.isOpen()) {
                String[] keepItems = {"pickaxe", "Bronze pickaxe", "Iron pickaxe", "Steel pickaxe",
                        "Black pickaxe", "Mithril pickaxe", "Adamant pickaxe",
                        "Rune pickaxe", "Dragon pickaxe", "Crystal pickaxe"};

                Rs2Bank.depositAllExcept(keepItems);
                Rs2Bank.closeBank();

                WorldPoint miningLoc = findBestMiningLocation();
                if (miningLoc != null) {
                    Rs2Walker.walkTo(miningLoc, 3);
                }

                Microbot.status = "Mining: banked successfully";
            }
        }
    }

    private void handleDropping() {
        String[] keepItems = {"pickaxe", "Bronze pickaxe", "Iron pickaxe", "Steel pickaxe",
                "Black pickaxe", "Mithril pickaxe", "Adamant pickaxe",
                "Rune pickaxe", "Dragon pickaxe", "Crystal pickaxe"};

        Rs2Inventory.dropAllExcept(keepItems);
        Microbot.status = "Mining: dropped ores";
    }

    public Rocks getCurrentOre() { return currentOre; }
    public boolean isEnabled() { return enabled; }
    public String getMode() { return mode; }
    public WorldPoint getInitialLocation() { return initialPlayerLocation; }
}