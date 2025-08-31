package net.runelite.client.plugins.microbot.aiobot.skills.woodcutting;

import net.runelite.api.coords.WorldPoint;

/**
 * Runtime state tracking for woodcutting operations
 */
public class WoodcuttingRuntime {
    private WoodcuttingState state = WoodcuttingState.IDLE;
    private long lastActionTs = 0L;
    private int startXp = -1;
    private WorldPoint startPoint;
    private long sessionStartTime = System.currentTimeMillis();

    // Getters and setters
    public WoodcuttingState getState() { return state; }
    public void setState(WoodcuttingState state) { this.state = state; }

    public long getLastActionTs() { return lastActionTs; }
    public void setLastActionTs(long lastActionTs) { this.lastActionTs = lastActionTs; }

    public int getStartXp() { return startXp; }
    public void setStartXp(int startXp) { this.startXp = startXp; }

    public WorldPoint getStartPoint() { return startPoint; }
    public void setStartPoint(WorldPoint startPoint) { this.startPoint = startPoint; }

    public long getSessionStartTime() { return sessionStartTime; }

    public long getSessionDuration() {
        return System.currentTimeMillis() - sessionStartTime;
    }

    public void reset() {
        state = WoodcuttingState.IDLE;
        lastActionTs = 0L;
        sessionStartTime = System.currentTimeMillis();
    }
}