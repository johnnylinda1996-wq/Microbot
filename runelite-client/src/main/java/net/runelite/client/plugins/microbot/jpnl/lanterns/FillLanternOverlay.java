package net.runelite.client.plugins.microbot.jpnl.lanterns;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;
import java.text.DecimalFormat;

public class FillLanternOverlay extends OverlayPanel {
    private final FillLanternPlugin plugin;
    private final DecimalFormat formatter = new DecimalFormat("#,###");

    // Estimated prices (update with current market values)
    private static final int BULLSEYE_LANTERN_FILLED_PRICE = 1800;
    private static final int BULLSEYE_LANTERN_EMPTY_PRICE = 1400;
    private static final int SWAMP_TAR_PRICE = 150;
    private static final int PROFIT_PER_LANTERN = BULLSEYE_LANTERN_FILLED_PRICE - BULLSEYE_LANTERN_EMPTY_PRICE - SWAMP_TAR_PRICE;

    @Inject
    FillLanternOverlay(FillLanternPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
        setPosition(OverlayPosition.TOP_LEFT);
        setNaughty();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.setPreferredSize(new Dimension(280, 350));
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("Bullseye Lantern Filler " + FillLanternScript.version)
                    .color(Color.decode("#a4ffff"))
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Status:")
                    .right(getStateDescription(plugin.fillLanternScript.getCurrentState()))
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Lanterns Filled:")
                    .right(formatter.format(plugin.fillLanternScript.getProfitCount()))
                    .build());

            long runTime = System.currentTimeMillis() - plugin.fillLanternScript.getStartTime();
            long hours = runTime / (1000 * 60 * 60);
            long minutes = (runTime % (1000 * 60 * 60)) / (1000 * 60);
            long seconds = (runTime % (1000 * 60)) / 1000;

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Runtime:")
                    .right(String.format("%02d:%02d:%02d", hours, minutes, seconds))
                    .build());

            if (plugin.fillLanternScript.getProfitCount() > 0 && runTime > 0) {
                double lanternsPerHour = (plugin.fillLanternScript.getProfitCount() * 3600000.0) / runTime;
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Lanterns/Hour:")
                        .right(formatter.format((int) lanternsPerHour))
                        .build());

                double profitPerHour = lanternsPerHour * PROFIT_PER_LANTERN;
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Profit/Hour:")
                        .right(formatter.format((int) profitPerHour) + " gp")
                        .build());

                long totalProfit = plugin.fillLanternScript.getProfitCount() * PROFIT_PER_LANTERN;
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Total Profit:")
                        .right(formatter.format(totalProfit) + " gp")
                        .build());
            }

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Profit/Lantern:")
                    .right(formatter.format(PROFIT_PER_LANTERN) + " gp")
                    .build());

        } catch (Exception ex) {
            Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
        }
        return super.render(graphics);
    }

    private String getStateDescription(FillLanternScript.State state) {
        if (state == null) return "Unknown";

        switch (state) {
            case BANKING:
                return "Banking supplies";
            case TELEPORTING_TO_RIMMINGTON:
                return "Teleporting to Rimmington";
            case WALKING_TO_CANDLE_MAKER:
                return "Walking to Candle Maker";
            case FILLING_LANTERNS:
                return "Filling lanterns";
            case RETURNING_TO_BANK:
                return "Returning to bank";
            default:
                return "Unknown";
        }
    }
}