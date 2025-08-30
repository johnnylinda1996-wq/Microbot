package net.runelite.client.plugins.microbot.aiobot;

import lombok.Getter;

@Getter
public class StatusAccessor {
    private volatile String currentTask = "none";
    private volatile String status = "";
    private volatile int currentLevel;
    private volatile int targetLevel;
    private volatile long taskElapsedMs;

    private volatile int xpGained;
    private volatile double xpPerHour;
    private volatile int xpToTarget;
    private volatile double percentToTarget;

    void update(String currentTask,
                String status,
                int currentLevel,
                int targetLevel,
                long taskElapsedMs,
                int xpGained,
                double xpPerHour,
                int xpToTarget,
                double percentToTarget) {
        this.currentTask = currentTask;
        this.status = status;
        this.currentLevel = currentLevel;
        this.targetLevel = targetLevel;
        this.taskElapsedMs = taskElapsedMs;
        this.xpGained = xpGained;
        this.xpPerHour = xpPerHour;
        this.xpToTarget = xpToTarget;
        this.percentToTarget = percentToTarget;
    }
}