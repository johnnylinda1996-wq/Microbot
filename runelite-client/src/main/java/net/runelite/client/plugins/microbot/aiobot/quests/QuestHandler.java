package net.runelite.client.plugins.microbot.aiobot.quests;

/**
 * Simpele interface voor quest handlers.
 * Later kun je uitbreiden met:
 *  - boolean isComplete();
 *  - stappen / state machine
 */
public interface QuestHandler {
    void execute();
}