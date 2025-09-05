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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.plugins.microbot.Microbot;

public class NmzAbsorptionCounter extends JPanel
{
    private static final Color DEFAULT_COLOR = Color.WHITE;

    private final JLabel absorptionLabel = new JLabel();

    public NmzAbsorptionCounter()
    {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(0, 0, 0, 0));

        absorptionLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        absorptionLabel.setForeground(DEFAULT_COLOR);
        absorptionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        absorptionLabel.setVerticalAlignment(SwingConstants.CENTER);

        add(absorptionLabel, BorderLayout.CENTER);

        setPreferredSize(new Dimension(40, 20));
    }

    public void updateAbsorption(Color color, int absorptionThreshold)
    {
        if (Microbot.getClient() == null)
        {
            absorptionLabel.setText("");
            return;
        }

        int absorptionPoints = Microbot.getClient().getVarbitValue(VarbitID.NZONE_ABSORB_POTION_EFFECTS);
        absorptionLabel.setText(Integer.toString(absorptionPoints));
        absorptionLabel.setForeground(absorptionPoints < absorptionThreshold ? color : DEFAULT_COLOR);
    }
}
