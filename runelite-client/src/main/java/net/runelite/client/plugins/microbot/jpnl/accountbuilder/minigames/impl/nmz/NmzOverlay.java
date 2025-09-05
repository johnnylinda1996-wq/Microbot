/*
 * Copyright (c) 2018, Nickolaj <https://github.com/fire-proof>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.microbot.jpnl.accountbuilder.minigames.impl.nmz;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.AllInOneConfig;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

public class NmzOverlay extends Overlay
{
    private final Client client;
    private final AllInOneConfig config;
    private final PanelComponent panelComponent = new PanelComponent();
    private NmzAbsorptionCounter absorptionCounter;

    @Inject
    private NmzOverlay(Client client, AllInOneConfig config)
    {
        this.client = client;
        this.config = config;
        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.nmzShowOverlay())
        {
            return null;
        }

        panelComponent.getChildren().clear();

        // Points
        if (config.nmzShowPoints())
        {
            int currentPoints = client.getVarbitValue(VarbitID.NZONE_CURRENTPOINTS);
            panelComponent.getChildren().add(LineComponent.builder()
                .left("Points:")
                .right(Integer.toString(currentPoints))
                .build());
        }

        // Absorption
        if (config.nmzShowAbsorption())
        {
            int absorptionPoints = client.getVarbitValue(VarbitID.NZONE_ABSORB_POTION_EFFECTS);
            Color absorptionColor = absorptionPoints < config.nmzAbsorptionThreshold()
                ? config.nmzAbsorptionColorBelowThreshold()
                : config.nmzAbsorptionColorAboveThreshold();

            panelComponent.getChildren().add(LineComponent.builder()
                .left("Absorption:")
                .right(Integer.toString(absorptionPoints))
                .rightColor(absorptionColor)
                .build());
        }

        return panelComponent.render(graphics);
    }

    public void updateConfig()
    {
        if (absorptionCounter != null)
        {
            absorptionCounter.updateAbsorption(
                config.nmzAbsorptionColorBelowThreshold(),
                config.nmzAbsorptionThreshold()
            );
        }
    }

    public void removeAbsorptionCounter()
    {
        absorptionCounter = null;
    }
}
