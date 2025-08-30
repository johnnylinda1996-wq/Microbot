package net.runelite.client.plugins.microbot.aiobot.skills;

import net.runelite.client.plugins.microbot.aiobot.settings.SkillRuntimeSettings;

/**
 * Basisinterface voor alle skill handlers.
 * execute() wordt elke tick aangeroepen zolang de taak actief is.
 * applySettings() krijgt periodiek een snapshot van de config (kan null zijn).
 */
public interface SkillHandler {

    /**
     * Optioneel instellingen toepassen. Default: niets.
     * @param settings snapshot (kan null zijn)
     */
    default void applySettings(SkillRuntimeSettings settings) {
        // no-op
    }

    /**
     * Eén tick uitvoeren.
     */
    void execute();
}