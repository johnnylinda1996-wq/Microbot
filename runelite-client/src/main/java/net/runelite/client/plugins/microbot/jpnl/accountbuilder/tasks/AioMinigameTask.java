package net.runelite.client.plugins.microbot.jpnl.accountbuilder.tasks;

import lombok.Getter;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.enums.MinigameType;

@Getter
public class AioMinigameTask extends AioTask {
    private final MinigameType minigameType;
    private final int durationMinutes; // New field for duration
    private final long startTimeMs; // Track when the task started
    private boolean complete;

    // Constructor for minigames with duration
    public AioMinigameTask(MinigameType type, int durationMinutes) {
        super(TaskType.MINIGAME);
        this.minigameType = type;
        this.durationMinutes = durationMinutes;
        this.startTimeMs = System.currentTimeMillis();
    }

    // Legacy constructor for backward compatibility
    public AioMinigameTask(MinigameType type) {
        this(type, 10); // Default 10 minutes
    }

    @Override
    public boolean isComplete() {
        // Check if manually marked complete or duration has elapsed
        if (complete) return true;

        long elapsedMs = System.currentTimeMillis() - startTimeMs;
        long elapsedMinutes = elapsedMs / (60 * 1000);
        return elapsedMinutes >= durationMinutes;
    }

    public void markComplete() { complete = true; }

    @Override
    public String getDisplay() {
        return "Minigame: " + minigameType.name() + " (" + durationMinutes + "m)";
    }

    @Override
    public MinigameType getMinigameTypeOrNull() { return minigameType; }

    // Helper method to get elapsed time in seconds
    public long elapsedMs() {
        return System.currentTimeMillis() - startTimeMs;
    }
}
