package net.runelite.client.plugins.microbot.jpnl.accountbuilder.minigames.impl;

import net.runelite.client.plugins.microbot.jpnl.accountbuilder.enums.MinigameType;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.minigames.MinigameHandler;

public class WintertodtHandler implements MinigameHandler {
    private int ticks;
    @Override public MinigameType getType() { return MinigameType.WINTERTODT; }
    @Override public boolean execute() { return ++ticks >= 1; }
}

