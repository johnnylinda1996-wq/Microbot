package net.runelite.client.plugins.microbot.jpnl.accountbuilder.enums;

public enum SkillType {
    ATTACK("Attack"),
    STRENGTH("Strength"),
    DEFENCE("Defence"),
    RANGED("Ranged"),
    PRAYER("Prayer"),
    MAGIC("Magic"),
    RUNECRAFTING("Runecrafting"),
    CONSTRUCTION("Construction"),
    HITPOINTS("Hitpoints"),
    AGILITY("Agility"),
    HERBLORE("Herblore"),
    THIEVING("Thieving"),
    CRAFTING("Crafting"),
    FLETCHING("Fletching"),
    SLAYER("Slayer"),
    HUNTER("Hunter"),
    MINING("Mining"),
    SMITHING("Smithing"),
    FISHING("Fishing"),
    COOKING("Cooking"),
    FIREMAKING("Firemaking"),
    WOODCUTTING("Woodcutting"),
    FARMING("Farming");

    private final String displayName;

    SkillType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}