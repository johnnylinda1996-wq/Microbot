package net.runelite.client.plugins.microbot.aiobot.tasks;

import lombok.Getter;
import net.runelite.client.plugins.microbot.aiobot.enums.QuestType;

@Getter
public class AioQuestTask extends AioTask {

    private final QuestType questType;
    private boolean complete;

    public AioQuestTask(QuestType questType) {
        super(TaskType.QUEST);
        this.questType = questType;
    }

    @Override
    public boolean isComplete() { return complete; }
    public void markComplete() { complete = true; }

    @Override
    public String getDisplay() {
        return "Quest: " + questType.name();
    }

    @Override
    public QuestType getQuestTypeOrNull() { return questType; }
}