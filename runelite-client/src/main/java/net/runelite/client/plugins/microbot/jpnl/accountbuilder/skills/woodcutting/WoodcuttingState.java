package net.runelite.client.plugins.microbot.jpnl.accountbuilder.skills.woodcutting;

/**
 * States for woodcutting operations
 */
public enum WoodcuttingState {
    IDLE,
    PREPARE,
    WALK_TO_TREE,
    CHOPPING,
    FULL_INVENTORY,
    FIREMAKING,
    BANKING,
    DROPPING,
    ANTIBAN_PAUSE,
    RECOVER,
    STOP
}