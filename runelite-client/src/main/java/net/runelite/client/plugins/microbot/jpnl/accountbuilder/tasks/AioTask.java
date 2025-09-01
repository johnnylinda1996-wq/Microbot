package net.runelite.client.plugins.microbot.jpnl.accountbuilder.tasks;

import lombok.Getter;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.enums.SkillType;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.enums.QuestType;

public abstract class AioTask {
    public enum TaskType { SKILL, QUEST }

    @Getter
    private final TaskType type;
    @Getter
    private long startTimestamp = -1L;

    protected AioTask(TaskType type) {
        this.type = type;
    }

    public void markStarted() {
        if (startTimestamp <= 0) startTimestamp = System.currentTimeMillis();
    }

    public abstract boolean isComplete();
    public abstract String getDisplay();

    public SkillType getSkillTypeOrNull() { return null; }
    public QuestType getQuestTypeOrNull() { return null; }
}