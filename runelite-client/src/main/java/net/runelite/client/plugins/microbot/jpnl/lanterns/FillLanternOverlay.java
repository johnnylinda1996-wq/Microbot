package net.runelite.client.plugins.microbot.jpnl.lanterns;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.mouse.VirtualMouse;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.util.ColorUtil;

import javax.inject.Inject;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;

@Slf4j
public class FillLanternOverlay extends OverlayPanel {
    private final FillLanternPlugin plugin;
    private final FillLanternConfig config;

    @Inject
    private VirtualMouse mouse;

    @Inject
    public FillLanternOverlay(FillLanternPlugin plugin, FillLanternConfig config) {
        super(plugin);
        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        FillLanternScript script = plugin.getScript();

        if (script == null) {
            return super.render(graphics);
        }

        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Bullseye Lantern Filler v" + FillLanternScript.version)
                .color(Color.GREEN)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("State:")
                .leftColor(Color.WHITE)
                .right(getScriptState(script))
                .rightColor(Color.YELLOW)
                .build());

        Duration duration = Duration.between(Instant.ofEpochMilli(script.getStartTime()), Instant.now());
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Runtime:")
                .leftColor(Color.WHITE)
                .right(formatDuration(duration))
                .rightColor(Color.YELLOW)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Filled Lanterns:")
                .leftColor(Color.WHITE)
                .right(String.valueOf(script.getProfitCount()))
                .rightColor(Color.YELLOW)
                .build());

        if (duration.getSeconds() > 0) {
            double lanternsPerHour = (double) script.getProfitCount() / ((double) duration.getSeconds() / 3600.0);
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Lanterns/hr:")
                    .leftColor(Color.WHITE)
                    .right(String.format("%.1f", lanternsPerHour))
                    .rightColor(Color.YELLOW)
                    .build());
        }

        if (config.enableBreakHandler() && script.isBreakHandlerEnabled()) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Break Handler:")
                    .right("Enabled")
                    .rightColor(Color.GREEN)
                    .build());
        }

        return super.render(graphics);
    }

    private String getScriptState(FillLanternScript script) {
        switch (script.getCurrentState()) {
            case BANKING:
                return "Banking";
            case WALKING_TO_STILL:
                return "Walking to Still";
            case FILLING_LANTERNS:
                return "Filling Lanterns";
            case RETURNING_TO_BANK:
                return "Returning to Bank";
            default:
                return "Unknown";
        }
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}