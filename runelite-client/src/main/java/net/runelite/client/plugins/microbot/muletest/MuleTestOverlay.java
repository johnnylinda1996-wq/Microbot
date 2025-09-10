package net.runelite.client.plugins.microbot.muletest;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

@Slf4j
public class MuleTestOverlay extends OverlayPanel {

    private final MuleTestConfig config;
    private final MuleTestScript script;

    @Inject
    MuleTestOverlay(MuleTestConfig config, MuleTestScript script) {
        super();
        this.config = config;
        this.script = script;
        setPosition(OverlayPosition.TOP_LEFT);
        setNaughty();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.getChildren().clear();

            // Title
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("Mule Test")
                    .color(Color.GREEN)
                    .build());

            // Current GP
            int currentGp = Rs2Inventory.count(995);

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Current GP:")
                    .right(String.format("%,d", currentGp))
                    .build());

            // Interval (hours)
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Interval:")
                    .right(config.muleIntervalHours() + " h")
                    .build());

            // Drop Location (replaces removed mule location)
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Drop Location:")
                    .right(String.format("X=%d, Y=%d, Z=%d",
                        config.dropLocationX(), config.dropLocationY(), config.dropLocationZ()))
                    .build());

            // Mule Account
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Mule Account:")
                    .right(config.muleAccount())
                    .build());

            // Status
            String status;
            Color statusColor;

            if (!script.isRunning()) {
                status = "Stopped";
                statusColor = Color.RED;
            } else {
                status = "Running (time-interval)";
                statusColor = Color.GREEN;
            }

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Status:")
                    .right(status)
                    .rightColor(statusColor)
                    .build());

            // Bridge connection status
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Bridge:")
                    .right(config.bridgeUrl())
                    .rightColor(Color.CYAN)
                    .build());

        } catch (Exception ex) {
            log.error("Error in MuleTestOverlay render: ", ex);
        }

        return super.render(graphics);
    }
}
