package net.runelite.client.plugins.microbot.jpnl.accountbuilder.tasks;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.travel.TravelLocation;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

/**
 * Task that walks the player to either a predefined TravelLocation or a custom WorldPoint using Rs2Walker.
 */
public class AioTravelTask extends AioTask {
    @Getter
    private final TravelLocation location; // null for custom
    private final String customName;      // null for predefined
    private final WorldPoint customPoint; // null for predefined
    private boolean complete;
    private static final int COMPLETE_DISTANCE = 5; // tiles
    private long lastWalkAttempt = 0L;
    private static final long WALK_RETRY_MS = 4_000L;

    /**
     * Predefined location constructor
     */
    public AioTravelTask(TravelLocation location) {
        super(TaskType.TRAVEL);
        this.location = location;
        this.customName = null;
        this.customPoint = null;
    }

    /**
     * Custom location constructor
     */
    public AioTravelTask(String name, WorldPoint point) {
        super(TaskType.TRAVEL);
        this.location = null;
        this.customName = name == null ? "Custom" : name.trim();
        this.customPoint = point;
    }

    public boolean isCustom() { return location == null; }
    public String getDisplayName() { return isCustom() ? customName : location.getDisplayName(); }
    public WorldPoint getTargetPoint() { return isCustom() ? customPoint : location.getPoint(); }
    public String getCustomName() { return customName; }
    public WorldPoint getCustomPoint() { return customPoint; }

    @Override
    public boolean isComplete() {
        if (complete) return true;
        if (!Microbot.isLoggedIn()) return false;
        WorldPoint player = Rs2Player.getWorldLocation();
        WorldPoint target = getTargetPoint();
        if (player != null && target != null && player.distanceTo(target) <= COMPLETE_DISTANCE) {
            complete = true;
            return true;
        }
        return false;
    }

    public void tickTravel() {
        if (isComplete()) return;
        long now = System.currentTimeMillis();
        if (now - lastWalkAttempt >= WALK_RETRY_MS) {
            WorldPoint target = getTargetPoint();
            if (target != null) Rs2Walker.walkTo(target);
            lastWalkAttempt = now;
        }
    }

    @Override
    public String getDisplay() {
        return "Travel to " + getDisplayName();
    }

    @Override
    public net.runelite.client.plugins.microbot.jpnl.accountbuilder.travel.TravelLocation getTravelLocationOrNull() {
        return location; // may be null for custom
    }

    public void forceComplete() { this.complete = true; }
}
