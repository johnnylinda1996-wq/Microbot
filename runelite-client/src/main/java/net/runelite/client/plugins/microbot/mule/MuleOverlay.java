package net.runelite.client.plugins.microbot.mule;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MuleOverlay extends OverlayPanel {

    private final MuleScript muleScript;
    private final MuleConfig config;

    @Inject
    public MuleOverlay(MuleScript muleScript, MuleConfig config) {
        this.muleScript = muleScript;
        this.config = config;
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.enableDebugOverlay()) {
            return null;
        }

        panelComponent.getChildren().clear();

        // Title
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Mule Bot Status")
                .color(Color.CYAN)
                .build());

        // Current state
        MuleScript.MuleState state = muleScript.getCurrentState();
        Color stateColor = getStateColor(state);
        panelComponent.getChildren().add(LineComponent.builder()
                .left("State:")
                .right(state.toString())
                .rightColor(stateColor)
                .build());

        // Current request info
        MuleRequest currentRequest = muleScript.getCurrentRequest();
        if (currentRequest != null) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Request ID:")
                    .right(currentRequest.getId().substring(0, 8) + "...")
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("From:")
                    .right(currentRequest.getRequesterUsername())
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Location:")
                    .right(currentRequest.getLocation())
                    .build());

            if (currentRequest.getItems() != null) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Items:")
                        .right(String.valueOf(currentRequest.getItems().size()))
                        .build());
            }

            // Runtime
            long runtime = System.currentTimeMillis() - muleScript.getRequestStartTime();
            String runtimeStr = formatRuntime(runtime);
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Runtime:")
                    .right(runtimeStr)
                    .build());
        } else {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Status:")
                    .right("Waiting for requests...")
                    .rightColor(Color.YELLOW)
                    .build());
        }

        // Bridge connection info
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Bridge:")
                .right(config.bridgeUrl())
                .build());

        // Last poll time
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Last Poll:")
                .right(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")))
                .build());

        return super.render(graphics);
    }

    private Color getStateColor(MuleScript.MuleState state) {
        switch (state) {
            case WAITING:
                return Color.YELLOW;
            case LOGGING_IN:
            case WALKING:
            case TRADING:
                return Color.GREEN;
            case LOGGING_OUT:
                return Color.CYAN;
            case ERROR:
                return Color.RED;
            default:
                return Color.WHITE;
        }
    }

    private String formatRuntime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;

        if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
}
