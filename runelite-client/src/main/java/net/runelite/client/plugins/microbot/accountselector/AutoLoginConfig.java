package net.runelite.client.plugins.microbot.accountselector;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("AutoLoginConfig")
public interface AutoLoginConfig extends Config {
    @ConfigSection(
            name = "General",
            description = "General",
            position = 0,
            closedByDefault = false
    )
    String generalSection = "general";

    @ConfigItem(
            keyName = "Is Member",
            name = "Is Member",
            description = "use Member worlds",
            position = 0,
            section = generalSection
    )
    default boolean isMember() { return false; }

    @ConfigItem(
            keyName = "World",
            name = "World",
            description = "World",
            position = 1,
            section = generalSection
    )
    default int world() { return 360; }

    @ConfigItem(
            keyName = "RandomWorld",
            name = "RandomWorld",
            description = "use random worlds",
            position = 2,
            section = generalSection
    )
    default boolean useRandomWorld() { return true; }

    @ConfigItem(
            keyName = "WaitBeforeLogin",
            name = "Wait Before logging in?",
            description = "Waits the given minutes before logging in.",
            position = 3,
            section = generalSection
    )
    default boolean WaitBeforeLogin() { return false; }

    @ConfigItem(
            keyName = "MinutesbeforeLogin",
            name = "Minutes before logging in",
            description = "Enter how much minutes to wait before logging back in.",
            position = 4,
            section = generalSection
    )
    default int MinutesbeforeLogin() { return 20; }

}
