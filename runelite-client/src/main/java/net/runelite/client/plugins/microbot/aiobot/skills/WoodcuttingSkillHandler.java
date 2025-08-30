package net.runelite.client.plugins.microbot.aiobot.skills;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.TileObject;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.aiobot.settings.SkillRuntimeSettings;
import net.runelite.client.plugins.microbot.aiobot.skills.woodcutting.AxeUtil;
import net.runelite.client.plugins.microbot.aiobot.skills.woodcutting.WoodcuttingRuntime;
import net.runelite.client.plugins.microbot.aiobot.skills.woodcutting.WoodcuttingState;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.List;

/**
 * Basis woodcutting handler voor AIO.
 * Minimalistische eerste versie die compileert tegen jouw bestaande utilities.
 *
 * TODO volgende iteraties:
 *  - Boomkeuze obv level
 *  - Logs/hr & xp tracking koppelen aan status
 *  - Firemaking / banking strategieën configgedreven
 *  - Stuck detection via anim/xp
 *  - Forestry events
 */
public class WoodcuttingSkillHandler implements SkillHandler {

    private final WoodcuttingRuntime runtime = new WoodcuttingRuntime();

    // Voor nu: alleen normale tree ObjectID.TREE (er zijn meerdere varianten; uitbreiden later)
    private int[] treeObjectIds = new int[] {
            net.runelite.api.gameval.ObjectID.TREE,
            net.runelite.api.gameval.ObjectID.TREE_1278,
            net.runelite.api.gameval.ObjectID.TREE_1276
    };

    private boolean dropLogs = true;
    private boolean enableFiremaking = false;

    private Integer currentAxeId = null;

    @Override
    public void applySettings(SkillRuntimeSettings settings) {
        // Nog niet in gebruik; later mappen we hier tree type & strategie
    }

    @Override
    public void execute() {
        Client client = Microbot.getClient();
        if (client == null) {
            Microbot.status = "WC: client null";
            return;
        }

        if (runtime.getStartXp() < 0) {
            runtime.setStartXp(client.getSkillExperience(Skill.WOODCUTTING));
            runtime.setStartPoint(Rs2Player.getWorldLocation());
        }

        updateAxeCache();

        switch (runtime.getState()) {
            case IDLE:
                enterPrepare();
                break;
            case PREPARE:
                handlePrepare();
                break;
            case WALK_TO_TREE:
                handleWalkToTree();
                break;
            case CHOPPING:
                handleChopping();
                break;
            case FULL_INVENTORY:
                handleFullInventory();
                break;
            case FIREMAKING:
                handleFiremaking();
                break;
            case BANKING:
                handleBanking();
                break;
            case DROPPING:
                handleDropping();
                break;
            case ANTIBAN_PAUSE:
                handleAntibanPause();
                break;
            case RECOVER:
                handleRecover();
                break;
            case STOP:
                Microbot.status = "WC: stop";
                break;
            default:
                runtime.setState(WoodcuttingState.WALK_TO_TREE);
                break;
        }
    }

    private void updateAxeCache() {
        Integer axe = AxeUtil.findBestAxeInInventory();
        if (axe != null && AxeUtil.meetsLevelReq(axe)) {
            currentAxeId = axe;
        }
    }

    private void enterPrepare() {
        runtime.setState(WoodcuttingState.PREPARE);
        runtime.setLastActionTs(System.currentTimeMillis());
        Microbot.status = "WC: prepare";
    }

    private void handlePrepare() {
        if (currentAxeId == null) {
            Microbot.status = "WC: geen axe -> bank";
            runtime.setState(WoodcuttingState.BANKING);
            return;
        }
        runtime.setState(WoodcuttingState.WALK_TO_TREE);
        Microbot.status = "WC: zoek boom";
    }

    private TileObject getTreeObject(int maxDistance) {
        // Probeer eerst directe match binnen afstand
        for (int id : treeObjectIds) {
            TileObject t = Rs2GameObject.findObjectByIdAndDistance(id, maxDistance);
            if (t != null) return t;
        }
        // Als niets, neem eerste zichtbare variant (ruwe fallback)
        for (int id : treeObjectIds) {
            TileObject t = Rs2GameObject.findObjectById(id);
            if (t != null) return t;
        }
        return null;
    }

    private void handleWalkToTree() {
        TileObject tree = getTreeObject(12);
        if (tree == null) {
            Microbot.status = "WC: geen boom";
            // Klein random idle / camera adjust kan later
            return;
        }
        WorldPoint player = Rs2Player.getWorldLocation();
        if (player == null) return;

        WorldPoint treeWp = tree.getWorldLocation();
        if (player.distanceTo(treeWp) > 1) {
            Rs2Walker.walkTo(treeWp);
            Microbot.status = "WC: lopen naar boom";
            return;
        }

        // Interact (kies of we direct via object of via ID willen)
        boolean clicked = false;
        if (tree instanceof GameObject) {
            clicked = Rs2GameObject.interact(((GameObject) tree).getId(), "Chop down", 2);
        } else {
            // fallback: probeer eerste ID binnen array
            clicked = Rs2GameObject.interact(tree.getId(), "Chop down", 2);
        }

        if (clicked) {
            runtime.setState(WoodcuttingState.CHOPPING);
            runtime.setLastActionTs(System.currentTimeMillis());
            Microbot.status = "WC: chop...";
        } else {
            Microbot.status = "WC: interact fail";
        }
    }

    private void handleChopping() {
        if (Rs2Inventory.isFull()) {
            runtime.setState(WoodcuttingState.FULL_INVENTORY);
            Microbot.status = "WC: inventory vol";
            return;
        }

        long now = System.currentTimeMillis();
        if (now - runtime.getLastActionTs() > 9000) {
            // Boom mogelijk weg / misclick
            runtime.setState(WoodcuttingState.WALK_TO_TREE);
            Microbot.status = "WC: herselecteer boom";
            return;
        }

        // Kleine kans op pauze
        if (Math.random() < 0.004d) {
            runtime.setState(WoodcuttingState.ANTIBAN_PAUSE);
            Microbot.status = "WC: pauze";
        }
    }

    private void handleFullInventory() {
        if (enableFiremaking) {
            runtime.setState(WoodcuttingState.FIREMAKING);
            return;
        }
        if (dropLogs) {
            runtime.setState(WoodcuttingState.DROPPING);
        } else {
            runtime.setState(WoodcuttingState.BANKING);
        }
    }

    private void handleFiremaking() {
        // TODO: echte firemaking integratie
        Microbot.status = "WC: firemaking TODO";
        runtime.setState(WoodcuttingState.DROPPING);
    }

    private void handleBanking() {
        if (!Rs2Bank.isOpen()) {
            if (!Rs2Bank.openBank()) {
                Microbot.status = "WC: bank openen...";
                return;
            }
        }
        if (currentAxeId != null) {
            Rs2Bank.depositAllExcept(Integer.valueOf(currentAxeId));
        } else {
            Rs2Bank.depositAll();
        }
        Microbot.status = "WC: bank done";
        runtime.setState(WoodcuttingState.WALK_TO_TREE);
        runtime.setLastActionTs(System.currentTimeMillis());
    }

    private void handleDropping() {
        boolean droppedAny = false;
        List<net.runelite.client.plugins.microbot.models.Rs2ItemModel> items = Rs2Inventory.all();
        if (items != null) {
            for (net.runelite.client.plugins.microbot.models.Rs2ItemModel item : items) {
                if (item == null) continue;
                if (currentAxeId != null && item.getId() == currentAxeId.intValue()) continue;
                String nm = item.getName();
                if (nm == null) continue;
                String low = nm.toLowerCase();
                if (low.endsWith("logs") || low.equals("log")) {
                    Rs2Inventory.drop(item.getId());
                    droppedAny = true;
                    safeSleep(80, 160);
                }
            }
        }
        if (!droppedAny) {
            runtime.setState(WoodcuttingState.WALK_TO_TREE);
            Microbot.status = "WC: terug naar boom";
        } else {
            Microbot.status = "WC: droppen";
        }
    }

    private void handleAntibanPause() {
        safeSleep(600, 1400);
        runtime.setState(WoodcuttingState.CHOPPING);
        runtime.setLastActionTs(System.currentTimeMillis());
    }

    private void handleRecover() {
        runtime.setState(WoodcuttingState.WALK_TO_TREE);
    }

    private void safeSleep(int min, int max) {
        try {
            int dur = min + (int)(Math.random() * Math.max(1, (max - min)));
            Thread.sleep(dur);
        } catch (InterruptedException ignored) { }
    }
}