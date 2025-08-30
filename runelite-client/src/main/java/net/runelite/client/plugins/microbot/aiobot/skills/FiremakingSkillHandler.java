package net.runelite.client.plugins.microbot.aiobot.skills;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

public class FiremakingSkillHandler implements SkillHandler {

    @Override
    public void execute() {
        if (Rs2Player.isAnimating()) return;

        if (Rs2Inventory.hasItem("Logs") && Rs2Inventory.hasItem("Tinderbox")) {
            Rs2Inventory.combine("Tinderbox", "Logs");
            Microbot.status = "Training Firemaking - Making fire";
        } else {
            Microbot.status = "Training Firemaking - Need logs and tinderbox";
        }
    }
}