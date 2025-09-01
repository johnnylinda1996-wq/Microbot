package net.runelite.client.plugins.microbot.jpnl.accountbuilder.skills;

import net.runelite.client.plugins.microbot.Microbot;

public class HitpointsSkillHandler implements SkillHandler {
    @Override
    public void execute() {
        Microbot.status = "Training Hitpoints - Trained through combat";
        // Hitpoints is trained through combat, so redirect to combat training
    }
}