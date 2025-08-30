package net.runelite.client.plugins.microbot.aiobot.tasks;

import lombok.Getter;
import net.runelite.client.plugins.microbot.aiobot.enums.QuestType;

public class AioQuestTask implements AioTask {
    @Getter
    private final QuestType questType;
    private boolean complete;

    public AioQuestTask(QuestType questType) {
        this.questType = questType;
    }

    @Override
    public String getDisplay() {
        return "Quest: " + questType.name();
    }

    @Override
    public boolean isComplete() {
        return complete;
    }

    @Override
    public void markComplete() {
        complete = true;
    }

    @Override
    public TaskType getType() {
        return TaskType.QUEST;
    }
}