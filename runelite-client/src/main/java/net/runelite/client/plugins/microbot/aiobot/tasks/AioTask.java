package net.runelite.client.plugins.microbot.aiobot.tasks;

public interface AioTask {
    String getDisplay();
    boolean isComplete();          // Script sets & evaluates
    void markComplete();
    TaskType getType();

    enum TaskType {
        SKILL,
        QUEST
    }
}