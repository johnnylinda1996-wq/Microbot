package net.runelite.client.plugins.microbot.jpnl.accountbuilder.minigames.impl.pestcontrol;

import com.google.common.collect.ImmutableSet;
import net.runelite.api.NPCComposition;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.Rs2InventorySetup;
import net.runelite.client.plugins.microbot.util.Global;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.pestcontrol.Portal;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

import static net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer.isQuickPrayerEnabled;
import static net.runelite.client.plugins.microbot.util.walker.Rs2Walker.distanceToRegion;
import static net.runelite.client.plugins.pestcontrol.Portal.*;

public class PestControlScript {
    public static double version = 2.3;
    public static final boolean DEBUG = false;

    private boolean initialise = true;
    private boolean walkToCenter = false;
    private boolean hasAutoTraveled = false;
    private final PestControlConfig config;

    // Pest Control island location (void knight outpost bank)
    private static final WorldPoint PEST_CONTROL_LOCATION = new WorldPoint(2667, 2653, 0);

    public static List<Portal> portals = List.of(PURPLE, BLUE, RED, YELLOW);

    // Replace deprecated NpcID constants with direct integer values
    private static final Set<Integer> SPINNER_IDS = ImmutableSet.of(
            1709, // SPINNER
            1710, // SPINNER_1710
            1711, // SPINNER_1711
            1712, // SPINNER_1712
            1713  // SPINNER_1713
    );

    private static final Set<Integer> BRAWLER_IDS = ImmutableSet.of(
            1734, // BRAWLER
            1736, // BRAWLER_1736
            1738, // BRAWLER_1738
            1737, // BRAWLER_1737
            1735  // BRAWLER_1735
    );

    final int distanceToPortal = 8;

    public PestControlScript(PestControlConfig config) {
        this.config = config;
    }

    private void resetPortals() {
        for (Portal portal : portals) {
            portal.setHasShield(true);
        }
    }

    public boolean execute() {
        try {
            if (!Microbot.isLoggedIn()) return false;

            // Auto travel to Pest Control if enabled and not already traveled
            if (!hasAutoTraveled && shouldAutoTravel()) {
                if (performAutoTravel()) {
                    hasAutoTraveled = true;
                } else {
                    return false; // Still traveling
                }
            }

            final boolean isInPestControl = isInPestControl();
            final boolean isInBoat = isInBoat();

            if (initialise && !isInPestControl && !isInBoat) {
                Microbot.log("Initialising Pest Control");
                if (Rs2Player.getWorld() != config.world()) {
                    Microbot.hopToWorld(config.world());
                    Global.sleepUntil(() -> Rs2Player.getWorld() == config.world(), 7000);
                }

                if (Rs2Player.getWorldLocation().getRegionID() == 10537 && Rs2Player.getWorld() == config.world()) {
                    if (!Rs2Bank.isOpen()) {
                        Microbot.log("Opening bank");
                        Rs2Bank.openBank();
                        Global.sleepUntil(Rs2Bank::isOpen, 3000);
                    }

                    if (config.inventorySetup() != null) {
                        var inventorySetup = new Rs2InventorySetup(config.inventorySetup(), null);
                        Microbot.log("Starting Inv Setup");

                        if (!inventorySetup.doesInventoryMatch() || !inventorySetup.doesEquipmentMatch()) {
                            if (!inventorySetup.loadEquipment() || !inventorySetup.loadInventory()) {
                                Microbot.log("Failed to load inventory setup");
                                return true; // Complete with failure
                            }
                        } else {
                            Microbot.log("Inv Setup Finished");
                            Rs2Bank.closeBank();
                            Global.sleepUntil(() -> !Rs2Bank.isOpen(), 2000);
                            initialise = false;
                        }
                    } else {
                        Rs2Bank.closeBank();
                        initialise = false;
                    }
                } else {
                    Microbot.log("Traveling to Pest Island");
                    Rs2Walker.walkTo(PEST_CONTROL_LOCATION);
                }
                return false;
            }

            if (isInPestControl) {
                initialise = false;
                if (!isQuickPrayerEnabled() && Microbot.getClient().getBoostedSkillLevel(Skill.PRAYER) != 0 && config.quickPrayer()) {
                    // Use direct widget ID instead of deprecated ComponentID
                    final Widget prayerOrb = Rs2Widget.getWidget(10485775); // MINIMAP_QUICK_PRAYER_ORB
                    if (prayerOrb != null) {
                        Microbot.getMouse().click(prayerOrb.getCanvasLocation());
                        Global.sleep(1000, 1500);
                    }
                }

                if (!walkToCenter) {
                    WorldPoint worldPoint = WorldPoint.fromRegion(Rs2Player.getWorldLocation().getRegionID(), 32, 17, 0);
                    Rs2Walker.walkTo(worldPoint, 3);
                    if (worldPoint.distanceTo(Rs2Player.getWorldLocation()) > 4) {
                        return false;
                    } else {
                        walkToCenter = true;
                    }
                }

                Rs2Combat.setSpecState(true, config.specialAttackPercentage() * 10);
                Widget activity = Rs2Widget.getWidget(26738700);
                if (activity != null && activity.getChild(0) != null &&
                    activity.getChild(0) != null && activity.getChild(0).getWidth() <= 20 && !Rs2Combat.inCombat()) {
                    Optional<Rs2NpcModel> attackableNpc = Rs2Npc.getAttackableNpcs().findFirst();
                    attackableNpc.ifPresent(rs2NpcModel -> Rs2Npc.interact(rs2NpcModel.getId(), "attack"));
                    return false;
                }

                var brawler = Rs2Npc.getNpc("brawler");
                if (brawler != null && brawler.getWorldLocation().distanceTo(Rs2Player.getWorldLocation()) < 3) {
                    Rs2Npc.interact(brawler, "attack");
                    Global.sleepUntil(() -> !Rs2Combat.inCombat());
                    return false;
                }

                if (Microbot.getClient().getLocalPlayer().isInteracting())
                    return false;

                if (handleAttack(PestControlNpc.BRAWLER, 1)
                        || handleAttack(PestControlNpc.PORTAL, 1)
                        || handleAttack(PestControlNpc.SPINNER, 1)) {
                    return false;
                }

                if (handleAttack(PestControlNpc.BRAWLER, 2)
                        || handleAttack(PestControlNpc.PORTAL, 2)
                        || handleAttack(PestControlNpc.SPINNER, 2)) {
                    return false;
                }

                if (handleAttack(PestControlNpc.BRAWLER, 3)
                        || handleAttack(PestControlNpc.PORTAL, 3)
                        || handleAttack(PestControlNpc.SPINNER, 3)) {
                    return false;
                }

                Rs2NpcModel portal = Arrays.stream(Rs2Npc.getPestControlPortals()).findFirst().orElse(null);
                if (portal != null) {
                    if (Rs2Npc.interact(portal.getId(), "attack")) {
                        Global.sleepUntil(() -> !Microbot.getClient().getLocalPlayer().isInteracting());
                    }
                } else {
                    if (!Microbot.getClient().getLocalPlayer().isInteracting()) {
                        Optional<Rs2NpcModel> attackableNpc = Rs2Npc.getAttackableNpcs().findFirst();
                        attackableNpc.ifPresent(rs2NpcModel -> Rs2Npc.interact(rs2NpcModel.getId(), "attack"));
                    }
                }
                return false;
            } else {
                resetPortals();
                walkToCenter = false;
                Global.sleep(Rs2Random.between(1600, 1800));
                if (!isInBoat && !initialise) {
                    // Use direct object IDs instead of deprecated ObjectID constants
                    if (Microbot.getClient().getLocalPlayer().getCombatLevel() >= 100) {
                        Rs2GameObject.interact(25632); // GANGPLANK_25632
                    } else if (Microbot.getClient().getLocalPlayer().getCombatLevel() >= 70) {
                        Rs2GameObject.interact(25631); // GANGPLANK_25631
                    } else {
                        Rs2GameObject.interact(14315); // GANGPLANK_14315
                    }
                    // Use direct widget ID instead of deprecated WidgetInfo
                    Global.sleepUntil(() -> Microbot.getClient().getWidget(408, 2) != null, 3000); // PEST_CONTROL_BOAT_INFO
                } else {
                    if (config.alchInBoat() && !config.alchItem().equalsIgnoreCase("")) {
                        Rs2Magic.alch(config.alchItem());
                    }
                }
                return false;
            }
        } catch (Exception ex) {
            Microbot.log("Pest Control Error: " + ex.getMessage());
            return false;
        }
    }

    private boolean shouldAutoTravel() {
        // For now, return true to always auto-travel when task starts
        // This will be enhanced to check the AllInOneConfig setting via the adapter
        return true;
    }

    private boolean performAutoTravel() {
        WorldPoint currentLocation = Rs2Player.getWorldLocation();

        // Check if we're already at Pest Control
        if (currentLocation.distanceTo(PEST_CONTROL_LOCATION) <= 10) {
            Microbot.log("Already at Pest Control location");
            return true;
        }

        // Check if we're currently walking by checking if player is moving
        if (Rs2Player.isMoving()) {
            return false; // Still traveling
        }

        Microbot.log("Auto-traveling to Pest Control...");
        Rs2Walker.walkTo(PEST_CONTROL_LOCATION);

        // Wait a bit to see if we've started walking
        Global.sleep(1000);

        // Check if we've arrived
        return currentLocation.distanceTo(PEST_CONTROL_LOCATION) <= 10;
    }

    public boolean isOutside() {
        return Microbot.getClient().getLocalPlayer().getWorldLocation().distanceTo(new WorldPoint(2644, 2644, 0)) < 20;
    }

    public boolean isInBoat() {
        // Use direct widget ID instead of deprecated WidgetInfo
        return Microbot.getClient().getWidget(408, 2) != null; // PEST_CONTROL_BOAT_INFO
    }

    public boolean isInPestControl() {
        // Use direct widget ID instead of deprecated WidgetInfo
        return Microbot.getClient().getWidget(408, 3) != null; // PEST_CONTROL_BLUE_SHIELD
    }

    public void exitBoat() {
        // Use direct object IDs instead of deprecated ObjectID constants
        if (Microbot.getClient().getLocalPlayer().getCombatLevel() >= 100) {
            Rs2GameObject.interact(25630); // LADDER_25630
        } else if (Microbot.getClient().getLocalPlayer().getCombatLevel() >= 70) {
            Rs2GameObject.interact(25629); // LADDER_25629
        } else {
            Rs2GameObject.interact(14314); // LADDER_14314
        }
        // Use direct widget ID instead of deprecated WidgetInfo
        Global.sleepUntil(() -> Microbot.getClient().getWidget(408, 2) == null, 3000); // PEST_CONTROL_BOAT_INFO
    }

    private boolean handleAttack(PestControlNpc npcType, int priority) {
        if (priority == 1) {
            if (config.Priority1() == npcType) {
                return performAttack(npcType);
            }
        } else if (priority == 2) {
            if (config.Priority2() == npcType) {
                return performAttack(npcType);
            }
        } else {
            if (config.Priority3() == npcType) {
                return performAttack(npcType);
            }
        }
        return false;
    }

    private boolean performAttack(PestControlNpc npcType) {
        if (npcType == PestControlNpc.BRAWLER) {
            return attackBrawler();
        } else if (npcType == PestControlNpc.PORTAL) {
            return attackPortals();
        } else if (npcType == PestControlNpc.SPINNER) {
            return attackSpinner();
        }
        return false;
    }

    public Portal getClosestAttackablePortal() {
        List<Pair<Portal, Integer>> distancesToPortal = new ArrayList<>();
        for (Portal portal : portals) {
            if (!portal.isHasShield() && !portal.getHitPoints().getText().trim().equals("0")) {
                distancesToPortal.add(Pair.of(portal, distanceToRegion(portal.getRegionX(), portal.getRegionY())));
            }
        }

        Pair<Portal, Integer> closestPortal = distancesToPortal.stream().min(Map.Entry.comparingByValue()).orElse(null);

        if (closestPortal == null) return null;

        return closestPortal.getKey();
    }

    private boolean attackPortal() {
        if (!Microbot.getClient().getLocalPlayer().isInteracting()) {
            Rs2NpcModel npcPortal = Rs2Npc.getNpc("portal");
            if (npcPortal == null) return false;
            NPCComposition npc = Microbot.getClientThread().runOnClientThreadOptional(() ->
                    Microbot.getClient().getNpcDefinition(npcPortal.getId())).orElse(null);
            if (npc == null) return false;

            if (Arrays.stream(npc.getActions()).anyMatch(x -> x != null && x.equalsIgnoreCase("attack"))) {
                return Rs2Npc.interact(npcPortal, "attack");
            } else {
                return false;
            }
        }
        return false;
    }

    private boolean attackPortals() {
        Portal closestAttackablePortal = getClosestAttackablePortal();
        if (closestAttackablePortal == null) return false;
        for (Portal portal : portals) {
            if (!portal.isHasShield() && !portal.getHitPoints().getText().trim().equals("0") && closestAttackablePortal == portal) {
                if (!Rs2Walker.isCloseToRegion(distanceToPortal, portal.getRegionX(), portal.getRegionY())) {
                    Rs2Walker.walkTo(WorldPoint.fromRegion(Rs2Player.getWorldLocation().getRegionID(), portal.getRegionX(), portal.getRegionY(), 0), 5);
                    attackPortal();
                } else {
                    attackPortal();
                }
                return true;
            }
        }
        return false;
    }

    private boolean attackSpinner() {
        for (int spinner : SPINNER_IDS) {
            if (Rs2Npc.interact(spinner, "attack")) {
                Global.sleepUntil(() -> !Microbot.getClient().getLocalPlayer().isInteracting());
                return true;
            }
        }
        return false;
    }

    private boolean attackBrawler() {
        for (int brawler : BRAWLER_IDS) {
            if (Rs2Npc.interact(brawler, "attack")) {
                Global.sleepUntil(() -> !Microbot.getClient().getLocalPlayer().isInteracting());
                return true;
            }
        }
        return false;
    }

    public void reset() {
        initialise = true;
        walkToCenter = false;
        hasAutoTraveled = false;
        resetPortals();
    }

    public void handleShieldDrop(String color) {
        switch (color) {
            case "purple":
                portals.stream().filter(x -> x == Portal.PURPLE).findFirst().ifPresent(p -> p.setHasShield(false));
                break;
            case "blue":
                portals.stream().filter(x -> x == Portal.BLUE).findFirst().ifPresent(p -> p.setHasShield(false));
                break;
            case "red":
                portals.stream().filter(x -> x == Portal.RED).findFirst().ifPresent(p -> p.setHasShield(false));
                break;
            case "yellow":
                portals.stream().filter(x -> x == Portal.YELLOW).findFirst().ifPresent(p -> p.setHasShield(false));
                break;
        }
    }
}
