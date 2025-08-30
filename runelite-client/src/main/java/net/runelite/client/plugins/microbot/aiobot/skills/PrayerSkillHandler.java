package net.runelite.client.plugins.microbot.aiobot.skills;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;

public class PrayerSkillHandler implements SkillHandler {
    @Override
    public void execute() {
        if (Rs2Inventory.hasItem("Bones")) {
            Rs2Inventory.interact("Bones", "Bury");
            Microbot.status = "Training Prayer - Burying bones";
        } else {
            Microbot.status = "Training Prayer - Need bones";
        }
    }
}