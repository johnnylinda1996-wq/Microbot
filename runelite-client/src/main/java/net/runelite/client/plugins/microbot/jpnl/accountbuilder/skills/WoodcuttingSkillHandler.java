package net.runelite.client.plugins.microbot.jpnl.accountbuilder.skills;

import net.runelite.api.GameObject;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.settings.SkillRuntimeSettings;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import java.util.*;

public class WoodcuttingSkillHandler implements SkillHandler {

    private boolean enabled = true;
    private String mode = "drop";
    private TreeType currentTree = TreeType.TREE;
    private boolean useBank = false;

    private static final Map<TreeType, List<WorldPoint>> TREE_LOCATIONS = new HashMap<>();

    static {
        TREE_LOCATIONS.put(TreeType.TREE, Arrays.asList(
                new WorldPoint(3086, 3232, 0), // Draynor Village
                new WorldPoint(3275, 3144, 0), // Al Kharid
                new WorldPoint(3238, 3241, 0)  // Lumbridge
        ));

        TREE_LOCATIONS.put(TreeType.OAK, Arrays.asList(
                new WorldPoint(3105, 3431, 0), // Barbarian Village
                new WorldPoint(3238, 3251, 0)  // Lumbridge
        ));

        TREE_LOCATIONS.put(TreeType.WILLOW, Arrays.asList(
                new WorldPoint(3086, 3232, 0), // Draynor Village
                new WorldPoint(2925, 3180, 0)  // Karamja
        ));
    }

    private enum TreeType {
        TREE("Tree", 1, "Logs"),
        OAK("Oak", 15, "Oak logs"),
        WILLOW("Willow", 30, "Willow logs"),
        MAPLE("Maple tree", 45, "Maple logs"),
        YEW("Yew", 60, "Yew logs"),
        MAGIC("Magic tree", 75, "Magic logs");

        private final String name;
        private final int levelRequired;
        private final String logName;

        TreeType(String name, int levelRequired, String logName) {
            this.name = name;
            this.levelRequired = levelRequired;
            this.logName = logName;
        }

        public boolean hasRequiredLevel() {
            return Microbot.getClient().getRealSkillLevel(Skill.WOODCUTTING) >= levelRequired;
        }

        public String getName() { return name; }
        public String getLogName() { return logName; }
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
            Microbot.status = "Woodcutting: disabled";
            return;
        }

        if (!Microbot.isLoggedIn()) return;

        updateCurrentTree();

        if (!currentTree.hasRequiredLevel()) {
            Microbot.status = "Woodcutting: insufficient level for " + currentTree.getName();
            return;
        }

        if (!hasAxe()) {
            handleMissingAxe();
            return;
        }

        // Dragon axe special attack
        if (Rs2Equipment.isWearing("Dragon axe") && Rs2Combat.getSpecEnergy() == 1000) {
            Rs2Combat.setSpecState(true, 1000);
            return;
        }

        if (Rs2Player.isMoving() || Rs2Player.isAnimating()) return;

        if (Rs2Inventory.isFull()) {
            handleFullInventory();
            return;
        }

        cutTree();
    }

    private void updateCurrentTree() {
        int woodcuttingLevel = Microbot.getClient().getRealSkillLevel(Skill.WOODCUTTING);

        if (woodcuttingLevel >= 75) {
            currentTree = TreeType.MAGIC;
        } else if (woodcuttingLevel >= 60) {
            currentTree = TreeType.YEW;
        } else if (woodcuttingLevel >= 45) {
            currentTree = TreeType.MAPLE;
        } else if (woodcuttingLevel >= 30) {
            currentTree = TreeType.WILLOW;
        } else if (woodcuttingLevel >= 15) {
            currentTree = TreeType.OAK;
        } else {
            currentTree = TreeType.TREE;
        }
    }

    private boolean hasAxe() {
        return Rs2Equipment.isWearing("axe") || Rs2Inventory.hasItem("axe") ||
                Rs2Equipment.isWearing("Bronze axe") || Rs2Inventory.hasItem("Bronze axe") ||
                Rs2Equipment.isWearing("Iron axe") || Rs2Inventory.hasItem("Iron axe") ||
                Rs2Equipment.isWearing("Steel axe") || Rs2Inventory.hasItem("Steel axe");
    }

    private void handleMissingAxe() {
        BankLocation nearestBank = Rs2Bank.getNearestBank();

        if (nearestBank != null && Rs2Bank.walkToBankAndUseBank(nearestBank)) {
            if (Rs2Bank.isOpen()) {
                List<String> axes = Arrays.asList("Rune axe", "Adamant axe", "Mithril axe", "Steel axe", "Iron axe", "Bronze axe");

                for (String axe : axes) {
                    if (Rs2Bank.hasItem(axe)) {
                        Rs2Bank.withdrawOne(axe);
                        Rs2Bank.closeBank();

                        if (Rs2Inventory.hasItem(axe)) {
                            Rs2Inventory.wield(axe);
                        }

                        Microbot.status = "Woodcutting: equipped " + axe;
                        return;
                    }
                }

                Rs2Bank.closeBank();
            }
        }
    }

    private void cutTree() {
        GameObject tree = Rs2GameObject.getGameObject(currentTree.getName(), true);

        if (tree != null) {
            if (Rs2GameObject.interact(tree, "Chop down")) {
                Rs2Player.waitForXpDrop(Skill.WOODCUTTING, true);
                Rs2Antiban.actionCooldown();
                Rs2Antiban.takeMicroBreakByChance();
                Microbot.status = "Woodcutting: " + currentTree.getName();
            }
        } else {
            Microbot.status = "Woodcutting: no " + currentTree.getName() + " found";
        }
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
                String[] keepItems = {"axe", "Bronze axe", "Iron axe", "Steel axe",
                        "Black axe", "Mithril axe", "Adamant axe",
                        "Rune axe", "Dragon axe", "Crystal axe"};

                Rs2Bank.depositAllExcept(keepItems);
                Rs2Bank.closeBank();
                Microbot.status = "Woodcutting: banked logs";
            }
        }
    }

    private void handleDropping() {
        String[] keepItems = {"axe", "Bronze axe", "Iron axe", "Steel axe",
                "Black axe", "Mithril axe", "Adamant axe",
                "Rune axe", "Dragon axe", "Crystal axe"};

        Rs2Inventory.dropAllExcept(keepItems);
        Microbot.status = "Woodcutting: dropped logs";
    }

    public TreeType getCurrentTree() { return currentTree; }
    public boolean isEnabled() { return enabled; }
    public String getMode() { return mode; }
}