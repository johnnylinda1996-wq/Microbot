package net.runelite.client.plugins.microbot.jpnl.accountbuilder.enums;

public enum MinigameType {
    BARBARIAN_ASSAULT("Barbarian Assault"),
    WINTERTODT("Wintertodt"),
    PEST_CONTROL("Pest Control"),
    NIGHTMARE_ZONE("Nightmare Zone"),
    SOUL_WARS("Soul Wars"),
    GUARDIANS_OF_THE_RIFT("Guardians of the Rift"),
    TITHE_FARM("Tithe Farm"),
    FISHING_TRAWLER("Fishing Trawler"),
    MAGE_TRAINING_ARENA("Mage Training Arena"),
    TEMPLE_TREKKING("Temple Trekking");

    private final String displayName;
    MinigameType(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }

    public String resourceKey() {
        return name().toLowerCase();
    }
}

