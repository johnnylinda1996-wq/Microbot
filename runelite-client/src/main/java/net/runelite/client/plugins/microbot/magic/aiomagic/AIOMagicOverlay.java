package net.runelite.client.plugins.microbot.magic.aiomagic;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.magic.aiomagic.scripts.AlchScript;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ButtonComponent;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.util.ColorUtil;

import javax.inject.Inject;
import java.awt.*;

public class AIOMagicOverlay extends OverlayPanel {

    private static final String BOT_VERSION = "V0.8";
    private static final Color HEADER_COLOR = ColorUtil.fromHex("0077B6");
    private static final Dimension PANEL_SIZE = new Dimension(240, 360);
    private static final Dimension BUTTON_SIZE = new Dimension(80, 18);

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

        pauseBtn = createPauseButton();
        resetBtn = createResetButton();
    }

    private ButtonComponent createPauseButton() {
        ButtonComponent button = new ButtonComponent("Pause");
        button.setParentOverlay(this);
        button.setPreferredSize(BUTTON_SIZE);
        button.setOnClick(() -> {
            if (AlchScript.isPaused()) {
                AlchScript.resume();
            } else {
                AlchScript.pause();
            }
        });
        button.hookMouseListener();
        return button;
    }

    private ButtonComponent createResetButton() {
        ButtonComponent button = new ButtonComponent("Reset");
        button.setParentOverlay(this);
        button.setPreferredSize(BUTTON_SIZE);
        button.setOnClick(() -> {
            AlchScript.resetAll();
            Microbot.log("All stats have been reset!");
        });
        button.hookMouseListener();
        return button;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.getChildren().clear();
            panelComponent.setPreferredSize(PANEL_SIZE);

            // check welke activity actief is
            switch (config.magicActivity()) {
                case ALCHING:
                    renderAlchOverlay();
                    break;
                default:
                    renderSimpleOverlay();
                    break;
            }

        } catch (Exception ex) {
            Microbot.log("Overlay render error: " + ex.getMessage());
        }
        return super.render(graphics);
    }

    private void renderSimpleOverlay() {
        // Header
        panelComponent.getChildren().add(
                TitleComponent.builder()
                        .text("AIO Magic V" + AIOMagicPlugin.version)
                        .color(HEADER_COLOR)
                        .build()
        );
        panelComponent.getChildren().add(LineComponent.builder().build());

        // Status
        panelComponent.getChildren().add(LineComponent.builder()
                .left(Microbot.status)
                .build());
    }

    private void renderAlchOverlay() {
        renderHeader();
        renderStatus();

        if (AlchScript.totalAlched > 0) {
            renderStats();
        }

        renderItemInfo();
        renderAfkInfo();
        renderButtons();
    }

    private void renderHeader() {
        panelComponent.getChildren().add(
               TitleComponent.builder()
                       .text("Alcher v0.8")
                       .color(HEADER_COLOR)
                       .build()
        );
        panelComponent.getChildren().add(LineComponent.builder().build());
    }

    private void renderStatus() {
        String status = getCurrentAlchingStatus();
        panelComponent.getChildren().add(LineComponent.builder()
                .left(status)
                .build());
    }

    private String getCurrentAlchingStatus() {
        if (AlchScript.isPaused()) {
            return "Alcher is Paused.";
        }
        String currentItem = getCurrentAlchItemName();
        if (currentItem != null) {
            return "Alching: " + currentItem;
        }
        return "Waiting for items...";
    }

    private String getCurrentAlchItemName() {
        try {
            if (plugin.getAlchItemNames().isEmpty()) {
                return null;
            }
            return plugin.getAlchItemNames().stream()
                    .filter(Rs2Inventory::hasItem)
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private void renderStats() {
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Runtime:")
                .right(AlchScript.getRuntime())
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Total profit:")
                .right(AlchScript.formatGP(AlchScript.totalProfit))
                .rightColor(Color.GREEN)
                .build());

        renderProfitPerHour();
        renderAlchesPerHour();
    }

    private void renderProfitPerHour() {
        long pph = AlchScript.getProfitPerHour();
        Color pphColor = getProfitPerHourColor(pph);

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Profit/hour:")
                .right(AlchScript.formatGP((int) pph))
                .rightColor(pphColor)
                .build());
    }

    private Color getProfitPerHourColor(long pph) {
        if (pph < 200_000L) return Color.RED;
        if (pph <= 300_000L) return Color.YELLOW;
        return Color.GREEN;
    }

    private void renderAlchesPerHour() {
        int aph = AlchScript.getAlchesPerHour();
        Color aphColor = getAlchesPerHourColor(aph);

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Alches/hour:")
                .right(String.valueOf(aph))
                .rightColor(aphColor)
                .build());
    }

    private Color getAlchesPerHourColor(int aph) {
        if (aph < 1000) return Color.RED;
        if (aph <= 1075) return Color.ORANGE;
        if (aph <= 1125) return Color.YELLOW;
        return Color.GREEN;
    }

    private void renderItemInfo() {
        String currentItem = getCurrentAlchItemName();
        if (currentItem != null) {
            int remainingItems = AlchScript.getRemainingItems();
        } else if (!plugin.getAlchItemNames().isEmpty()) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Status:")
                    .right("No items found")
                    .rightColor(Color.RED)
                    .build());
        }
    }

    private Color getItemCountColor(int count) {
        if (count <= 10) return Color.RED;
        if (count <= 50) return Color.ORANGE;
        if (count <= 100) return Color.YELLOW;
        return Color.WHITE;
    }

    private void renderAfkInfo() {
        if (!config.afkEnabled()) return;

        if (!AlchScript.isAfkScheduled() && !AlchScript.isInAfkBreak()) {
            AlchScript.enableAfkOverlayOnly(config);
        }

        if (AlchScript.isInAfkBreak()) {
            renderCurrentBreakInfo();
        } else if (AlchScript.isAfkScheduled()) {
            renderNextBreakInfo();
        }
    }

    private void renderCurrentBreakInfo() {
        long remaining = AlchScript.getCurrentBreakRemainingMillis();
        long planned = AlchScript.getPlannedAfkDurationMillis();

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Resume in:")
                .right(AlchScript.formatMMSS(remaining))
                .rightColor(Color.ORANGE)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("AFK length:")
                .right(AlchScript.formatMMSS(planned))
                .build());
    }

    private void renderNextBreakInfo() {
        long untilNext = AlchScript.getMillisUntilNextAfk();
        long planned = AlchScript.getPlannedAfkDurationMillis();

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Next AFK/Break in:")
                .right(AlchScript.formatMMSS(untilNext))
                .rightColor(Color.CYAN)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("AFK/Break length:")
                .right(AlchScript.formatMMSS(planned))
                .build());
    }

    private void renderButtons() {
        pauseBtn.setText(AlchScript.isPaused() ? "Resume" : "Pause");
        panelComponent.getChildren().add(pauseBtn);
        panelComponent.getChildren().add(resetBtn);
    }
}
