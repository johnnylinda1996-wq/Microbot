package net.runelite.client.plugins.microbot.magic.aiomagic;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.plugins.microbot.magic.aiomagic.enums.MagicActivity;
import net.runelite.client.plugins.microbot.magic.aiomagic.enums.StunSpell;
import net.runelite.client.plugins.microbot.magic.aiomagic.enums.SuperHeatItem;
import net.runelite.client.plugins.microbot.magic.aiomagic.enums.TeleportSpell;
import net.runelite.client.plugins.microbot.util.magic.Rs2CombatSpells;

@ConfigGroup(AIOMagicConfig.configGroup)
public interface AIOMagicConfig extends Config {
    String configGroup = "aio-magic";
    String activity = "magicActivity";
    String combatSpell = "magicCombatSpell";
    String alchItems = "alchItems";
    String alchprofit = "alchprofit";
    String superHeatItem = "superHeatItem";
    String npcName = "npcName";
    String staff = "staff";
    String teleportSpell = "teleportSpell";
    String stunSpell = "stunSpell";
    String stunNpcName = "stunNpcName";

    @ConfigSection(
            name = "General Settings",
            description = "Configure general plugin configuration & preferences",
            position = 0
    )
    String generalSection = "general";

    @ConfigSection(
            name = "Alch Settings",
            description = "Configure Alching settings",
            position = 1
    )
    String alchSection = "alch";

    @ConfigSection(
            name = "Stun Settings",
            description = "Configure splashing settings",
            position = 1
    )
    String stunSection = "stun";

    @ConfigSection(
            name = "Splashing Settings",
            description = "Configure Stun settings",
            position = 2
    )
    String splashSection = "splash";

    @ConfigSection(
            name = "SuperHeat Settings",
            description = "Configure SuperHeat settings",
            position = 2
    )
    String superHeatSection = "superHeat";

    @ConfigSection(
            name = "Teleport Settings",
            description = "Configure teleport settings",
            position = 3
    )
    String teleportSection = "teleport";

    @ConfigItem(
            keyName = activity,
            name = "Activity",
            description = "Select the activity you would like to perform",
            position = 0,
            section = generalSection
    )
    default MagicActivity magicActivity() {
        return MagicActivity.SPLASHING;
    }

    @ConfigItem(
            keyName = stunNpcName,
            name = "Stun npc name",
            description = "Name of the npc to stun",
            position = 0,
            section = stunSection
    )
    default String stunNpcName() {
        return "";
    }

    @ConfigItem(
            keyName = stunSpell,
            name = "Stun spell",
            description = "Name of the stun spell",
            position = 1,
            section = stunSection
    )
    default StunSpell stunSpell() {
        return StunSpell.STUN;
    }

    @ConfigItem(
            keyName = npcName,
            name = "NPC Name",
            description = "Name of the NPC you would like to splash",
            position = 0,
            section = splashSection
    )
    default String npcName() {
        return "";
    }

    @ConfigItem(
            keyName = combatSpell,
            name = "Combat Spell",
            description = "Select the spell you would like to splash with",
            position = 1,
            section = splashSection
    )
    default Rs2CombatSpells combatSpell() {
        return Rs2CombatSpells.WIND_STRIKE;
    }

    @ConfigItem(
            keyName = alchItems,
            name = "Item name to alch:",
            description = "List of items you would like to alch",
            position = 0,
            section = alchSection
    )
    default String alchItems() {
        return "";
    }

    @ConfigItem(
            keyName = alchprofit,
            name = "Profit per alch:",
            description = "Enter the profit per alch here",
            position = 1,
            section = alchSection
    )
    default String alchprofit() {
        return "";
    }

    @ConfigItem(
            keyName = "outofalch",
            name = "Logout when out of alchables?:",
            description = "Logs the character off when out of alchables.",
            position = 2,
            section = alchSection
    )
    default boolean outofalch() {
        return false;
    }

    @ConfigItem(
            keyName = "alchinpoh",
            name = "Alch in POH?:",
            description = "Teleports to your house before starting with alch. Requires teletabs in inventory.",
            position = 3,
            section = alchSection
    )
    default boolean alchinpoh() {
        return false;
    }

    // --- AFK instellingen ---
    @ConfigItem(
            keyName = "afkEnabled",
            name = "Enable AFK mode:",
            description = "Bot will take AFK breaks at random intervals",
            position = 4,
            section = alchSection
    )
    default boolean afkEnabled() {
        return false;
    }
    @ConfigItem(
            keyName = "logoutEnabled",
            name = "Logout when AFK:",
            description = "This will log out and take breaks instead of Afking.",
            position = 5,
            section = alchSection
    )
    default boolean logoutEnabled() {
        return false;
    }

    @ConfigItem(
            keyName = "World",
            name = "World:",
            description = "Logs into this world after breaking.",
            position = 6,
            section = alchSection
    )
    default int world() { return 360; }

    @ConfigItem(
            keyName = "afkIntervalMin",
            name = "Min time before AFK:",
            description = "Minimum time before taking an AFK break",
            position = 7,
            section = alchSection
    )
    default int afkIntervalMin() {
        return 60;
    }

    @ConfigItem(
            keyName = "afkIntervalMax",
            name = "Max time before AFK:",
            description = "Maximum time before taking an AFK break",
            position = 8,
            section = alchSection
    )
    default int afkIntervalMax() {
        return 100;
    }

    @ConfigItem(
            keyName = "afkDurationMin",
            name = "AFK duration min:",
            description = "Minimum duration of AFK break",
            position = 9,
            section = alchSection
    )
    default int afkDurationMin() {
        return 10;
    }

    @ConfigItem(
            keyName = "afkDurationMax",
            name = "AFK duration max:",
            description = "Maximum duration of AFK break",
            position = 10,
            section = alchSection
    )
    default int afkDurationMax() {
        return 15;
    }


    @ConfigItem(
            keyName = superHeatItem,
            name = "SuperHeat Items",
            description = "List of items you would like to superheat",
            position = 0,
            section = superHeatSection
    )
    default SuperHeatItem superHeatItem() {
        return SuperHeatItem.IRON;
    }

    @ConfigItem(
            keyName = teleportSpell,
            name = "Teleport Spell",
            description = "Select the teleport spell you would like to use",
            position = 0,
            section = teleportSection
    )
    default TeleportSpell teleportSpell() {
        return TeleportSpell.VARROCK_TELEPORT;
    }

}
