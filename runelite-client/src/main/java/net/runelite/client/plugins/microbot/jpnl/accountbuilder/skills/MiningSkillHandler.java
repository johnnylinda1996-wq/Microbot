package net.runelite.client.plugins.microbot.jpnl.accountbuilder.skills;

import net.runelite.api.GameObject;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.ObjectComposition; // added
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.settings.SkillRuntimeSettings;
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

        // Initialize player location on first run
        if (initialPlayerLocation == null) {
            initialPlayerLocation = Rs2Player.getWorldLocation();
            setupAntiban();
        }

        // Determine current ore type based on mining level
        currentOre = determineCurrentOre();
        if (currentOre == null) {
            Microbot.status = "Mining: no suitable ore for level " + Microbot.getClient().getRealSkillLevel(Skill.MINING);
            return;
        }

        // Handle world hopping if too many players
        if (shouldHopWorld()) {
            hopToRandomWorld();
            return;
        }

        // Check if player is moving or animating
        if (Rs2Player.isMoving() || Rs2Player.isAnimating()) return;

        // Special attack with dragon pickaxe
        if (Rs2Equipment.isWearing("Dragon pickaxe")) {
            Rs2Combat.setSpecState(true, 1000);
        }

        // Mining logic
        if (Rs2Inventory.isFull()) {
            handleFullInventory();
        } else {
            mineOre();
        }
    }

    private void setupAntiban() {
        Rs2Antiban.resetAntibanSettings();
        Rs2Antiban.antibanSetupTemplates.applyMiningSetup();
        Rs2AntibanSettings.actionCooldownChance = 0.1;
    }

    private Rocks determineCurrentOre() {
        int miningLevel = Microbot.getClient().getRealSkillLevel(Skill.MINING);

        if ("auto".equals(mode)) {
            // Find highest level ore we can mine
            Rocks bestOre = null;
            for (Rocks ore : Rocks.values()) {
                if (ore.hasRequiredLevel() && miningLevel >= ore.getMiningLevel()) {
                    bestOre = ore;
                }
            }
            return bestOre;
        } else {
            // Try to use configured ore type
            try {
                Rocks configuredOre = Rocks.valueOf(mode.toUpperCase());
                return configuredOre.hasRequiredLevel() ? configuredOre : null;
            } catch (IllegalArgumentException e) {
                return Rocks.COPPER; // Fallback
            }
        }
    }

    private boolean shouldHopWorld() {
        if (maxPlayersInArea <= 0) return false;

        WorldPoint localLocation = Rs2Player.getWorldLocation();
        long nearbyPlayers = Microbot.getClient().getTopLevelWorldView().players().stream()
                .filter(p -> p != null && p != Microbot.getClient().getLocalPlayer())
                .filter(p -> p.getWorldLocation().distanceTo(localLocation) <= distanceToStray)
                .filter(p -> p.getAnimation() != -1) // Mining animation
                .count();

        return nearbyPlayers >= maxPlayersInArea;
    }

    private void hopToRandomWorld() {
        Microbot.status = "Mining: Too many players nearby. Hopping...";
        Rs2Random.waitEx(3200, 800);

        int world = Login.getRandomWorld(Rs2Player.isMember());
        boolean hopped = Microbot.hopToWorld(world);
        if (hopped) {
            Microbot.status = "Mining: Hopped to world: " + world;
        }
    }

    private void mineOre() {
        // Find rock using modern API
        List<GameObject> rocks = Rs2GameObject.getGameObjects(obj -> {
            ObjectComposition comp = Rs2GameObject.convertToObjectComposition(obj);
            String name = comp != null ? comp.getName() : null;
            return name != null && name.equals(currentOre.name()) &&
                initialPlayerLocation != null && obj.getWorldLocation().distanceTo(initialPlayerLocation) <= distanceToStray;
        });

        GameObject rock = rocks.isEmpty() ? null : rocks.get(0);

        if (rock != null) {
            if (Rs2GameObject.interact(rock)) {
                Microbot.status = "Mining: Mining " + currentOre.name();
                Rs2Player.waitForXpDrop(Skill.MINING, true);
                Rs2Antiban.actionCooldown();
                Rs2Antiban.takeMicroBreakByChance();
            }
        } else {
            // No rock found, try to walk to mining location
            walkToMiningLocation();
        }
    }

    private void walkToMiningLocation() {
        List<WorldPoint> locations = MINING_LOCATIONS.get(currentOre);
        if (locations != null && !locations.isEmpty()) {
            WorldPoint nearestLocation = findNearestLocation(locations);
            if (nearestLocation != null) {
                Microbot.status = "Mining: Walking to " + currentOre.name() + " location";
                Rs2Walker.walkTo(nearestLocation);
                initialPlayerLocation = nearestLocation;
            }
        }
    }

    private WorldPoint findNearestLocation(List<WorldPoint> locations) {
        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        return locations.stream()
                .min(Comparator.comparingInt(loc -> playerLocation.distanceTo(loc)))
                .orElse(null);
    }

    private void handleFullInventory() {
        if (useBank) {
            bankOres();
        } else {
            dropOres();
        }
    }

    private void bankOres() {
        List<String> itemsToBank = Arrays.asList("Copper ore", "Tin ore", "Iron ore", "Coal", "Silver ore",
                "Gold ore", "Mithril ore", "Adamantite ore", "Runite ore", "Uncut gem");

        if (!Rs2Bank.bankItemsAndWalkBackToOriginalPosition(itemsToBank, initialPlayerLocation, 0, distanceToStray)) {
            Microbot.status = "Mining: Banking failed, trying again...";
        }
    }

    private void dropOres() {
        // Drop items except tools and equipment
        String[] itemsToKeep = {"Pickaxe", "Bronze pickaxe", "Iron pickaxe", "Steel pickaxe", "Black pickaxe",
                                "Mithril pickaxe", "Adamant pickaxe", "Rune pickaxe", "Dragon pickaxe"};
        Rs2Inventory.dropAllExcept(itemsToKeep);
        Microbot.status = "Mining: Dropped ores";
    }

    // Getters voor debugging
    public boolean isEnabled() { return enabled; }
    public String getMode() { return mode; }
    public Rocks getCurrentOre() { return currentOre; }
}