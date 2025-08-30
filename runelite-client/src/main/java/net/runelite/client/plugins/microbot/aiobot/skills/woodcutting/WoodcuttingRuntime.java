package net.runelite.client.plugins.microbot.aiobot.skills.woodcutting;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.coords.WorldPoint;

@Getter
@Setter
public class WoodcuttingRuntime {
    private WoodcuttingState state = WoodcuttingState.IDLE;
    private long lastActionTs = 0L;
    private long lastXpTs = 0L;
    private int startXp = -1;
    private int logsCut = 0;
    private int lastInventoryCount = 0;
    private WorldPoint startPoint;
    private int stuckTicks = 0;

    public void reset() {
        state = WoodcuttingState.IDLE;
        lastActionTs = System.currentTimeMillis();
        lastXpTs = System.currentTimeMillis();
        startXp = -1;
        logsCut = 0;
        lastInventoryCount = 0;
        stuckTicks = 0;
        startPoint = null;
    }
}