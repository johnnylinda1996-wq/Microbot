package net.runelite.client.plugins.microbot.jpnl.accountbuilder.skills;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.settings.SkillRuntimeSettings;

/**
 * Generieke no-op handler (alleen gebruiken als placeholder).
 */
public class DummySkillHandler implements SkillHandler {
    private boolean enabled;
    private String mode;

    @Override
    public void applySettings(SkillRuntimeSettings settings) {
        if (settings != null) {
            enabled = settings.isEnabled();
            mode = settings.getMode();
        } else {
            enabled = false;
            mode = null;
        }
    }

    @Override
    public void execute() {
        if (!enabled) {
            Microbot.status = "Disabled skill";
            return;
        }
        Microbot.status = "Training (placeholder) " + (mode == null ? "" : "(" + mode + ")");
    }
}