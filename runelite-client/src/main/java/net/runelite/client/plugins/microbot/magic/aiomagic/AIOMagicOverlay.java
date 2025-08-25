package net.runelite.client.plugins.microbot.magic.aiomagic;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.magic.aiomagic.scripts.AlchScript;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ButtonComponent;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.util.ColorUtil;

import javax.inject.Inject;
import java.awt.*;

public class AIOMagicOverlay extends OverlayPanel {

    private final AIOMagicPlugin plugin;
    @Inject private AIOMagicConfig config;

    private final ButtonComponent pauseBtn;
    private final ButtonComponent resetBtn;

    @Inject
    AIOMagicOverlay(AIOMagicPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
        setPosition(OverlayPosition.TOP_LEFT);
        setNaughty();

        // Buttons aanmaken (blijven persistent)
        pauseBtn = new ButtonComponent(AlchScript.isPaused() ? "Resume" : "Pause");
        pauseBtn.setParentOverlay(this);
        pauseBtn.setPreferredSize(new Dimension(80, 18));
        pauseBtn.setOnClick(() -> {
            if (AlchScript.isPaused()) AlchScript.resume();
            else AlchScript.pause();
        });
        pauseBtn.hookMouseListener();

        resetBtn = new ButtonComponent("Reset");
        resetBtn.setParentOverlay(this);
        resetBtn.setPreferredSize(new Dimension(80, 18));
        resetBtn.setOnClick(() -> {
            AlchScript.resetAll();
            Microbot.log("All stats have been reset!");
        });
        resetBtn.hookMouseListener();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.getChildren().clear();
            panelComponent.setPreferredSize(new Dimension(240, 360));

            // Header
            panelComponent.getChildren().add(
                    TitleComponent.builder()
                            .text("AIO Magic V" + AIOMagicPlugin.version)
                            .color(ColorUtil.fromHex("0077B6"))
                            .build()
            );
            panelComponent.getChildren().add(LineComponent.builder().build());

            // Status
            panelComponent.getChildren().add(LineComponent.builder()
                    .left(Microbot.status)
                    .build());

            // Stats (alleen als er al alchs zijn gedaan)
            if (AlchScript.totalAlched > 0) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Runtime:")
                        .right(AlchScript.getRuntime())
                        .build());

                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Total profit:")
                        .right(AlchScript.formatGP(AlchScript.totalProfit))
                        .rightColor(Color.GREEN)
                        .build());

                long pph = AlchScript.getProfitPerHour();
                Color pphColor = (pph < 200_000L) ? Color.RED : (pph <= 300_000L) ? Color.YELLOW : Color.GREEN;
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Profit/hour:")
                        .right(AlchScript.formatGP((int) pph))
                        .rightColor(pphColor)
                        .build());

                int aph = AlchScript.getAlchesPerHour();
                Color aphColor = (aph < 1000) ? Color.RED
                               : (aph <= 1075) ? Color.ORANGE
                               : (aph <= 1125) ? Color.YELLOW
                               : Color.GREEN;
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Alches/hour:")
                        .right(String.valueOf(aph))
                        .rightColor(aphColor)
                        .build());
            }

            // AFK info//new Login(config.world());
            if (config.afkEnabled()) {
                // Plan een AFK als het nog niet gepland is maar AFK aan staat
                if (!AlchScript.isAfkScheduled() && !AlchScript.isInAfkBreak()) {
                    AlchScript.enableAfkOverlayOnly(config);

                }

                if (AlchScript.isInAfkBreak()) {
                    long remaining = AlchScript.getCurrentBreakRemainingMillis();
                    long planned = AlchScript.getPlannedAfkDurationMillis();
                    panelComponent.getChildren().add(LineComponent.builder()
                            .left("Resume in:")
                            .right(AlchScript.formatMMSS(remaining))
                            .rightColor(Color.ORANGE)
                            .build());
                    panelComponent.getChildren().add(LineComponent.builder()
                            .left("AFK/Break length:")
                            .right(AlchScript.formatMMSS(planned))
                            .build());
                } else if (AlchScript.isAfkScheduled()) {
                    long untilNext = AlchScript.getMillisUntilNextAfk();
                    long planned = AlchScript.getPlannedAfkDurationMillis();
                    panelComponent.getChildren().add(LineComponent.builder()
                            .left("Next AFK/Break in:")
                            .right(AlchScript.formatMMSS(untilNext))
                            .rightColor(Color.CYAN)
                            .build());
                    panelComponent.getChildren().add(LineComponent.builder()
                            .left("AFK length:")
                            .right(AlchScript.formatMMSS(planned))
                            .build());
                }
            }

            // Update pause button text
            pauseBtn.setText(AlchScript.isPaused() ? "Resume" : "Pause");

            // Voeg buttons toe
            panelComponent.getChildren().add(pauseBtn);
            panelComponent.getChildren().add(resetBtn);

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return super.render(graphics);
    }
}
