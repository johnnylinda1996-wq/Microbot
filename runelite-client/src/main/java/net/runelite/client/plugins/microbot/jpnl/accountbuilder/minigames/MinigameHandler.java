package net.runelite.client.plugins.microbot.jpnl.accountbuilder.minigames;

import net.runelite.client.plugins.microbot.jpnl.accountbuilder.enums.MinigameType;

public interface MinigameHandler {
    MinigameType getType();
    /**
     * Execute one loop slice. Return true when task complete.
     */
    boolean execute();
    /**
     * Optional status detail appended to base status.
     */
    default String statusDetail() { return ""; }
}
