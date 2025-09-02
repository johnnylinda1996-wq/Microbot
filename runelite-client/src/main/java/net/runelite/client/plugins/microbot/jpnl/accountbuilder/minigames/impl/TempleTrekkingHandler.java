package net.runelite.client.plugins.microbot.jpnl.accountbuilder.minigames.impl;

import net.runelite.client.plugins.microbot.jpnl.accountbuilder.enums.MinigameType;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.minigames.MinigameHandler;

public class TempleTrekkingHandler implements MinigameHandler {
    private int ticks;
    @Override public MinigameType getType() { return MinigameType.TEMPLE_TREKKING; }
    @Override public boolean execute() { return ++ticks >= 1; }
}

