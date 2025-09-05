package net.runelite.client.plugins.microbot.jpnl.accountbuilder.minigames.impl.pestcontrol;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.pestcontrol.Portal;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

import static net.runelite.client.plugins.microbot.jpnl.accountbuilder.minigames.impl.pestcontrol.PestControlScript.portals;

public class PestControlOverlay extends OverlayPanel {
    private final PestControlScript script;

    @Inject
    public PestControlOverlay(PestControlScript script) {
        this.script = script;
        setPosition(OverlayPosition.TOP_LEFT);
        setNaughty();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.setPreferredSize(new Dimension(200, 300));
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("Pest Control " + PestControlScript.version)
                    .color(Color.GREEN)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder().build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left(Microbot.status)
                    .build());

            if (PestControlScript.DEBUG) {
                for(Portal portal: portals) {
                    if (portal.getHitPoints() == null) continue;
                    panelComponent.getChildren().add(LineComponent.builder()
                            .left(portal.toString() + " - H" + portal.getHitPoints().getText() + " - A " + portal.isAttackAble())
                            .build());
                }
            }
        } catch(Exception ex) {
            System.out.println(ex.getMessage());
        }
        return super.render(graphics);
    }
}
