package net.runelite.client.plugins.microbot.aiobot.skills;

import net.runelite.api.ObjectID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

public class MiningSkillHandler implements SkillHandler {

    @Override
    public void execute() {
        if (Rs2Player.isAnimating()) return;

        if (Rs2Inventory.isFull()) {
            Rs2Inventory.drop("Copper ore");
            Rs2Inventory.drop("Tin ore");
            Microbot.status = "Training Mining - Dropping ore";
        } else {
            // Use correct ObjectID constants that exist
            if (Rs2GameObject.interact(ObjectID.ROCKS, "Mine")) {
                Microbot.status = "Training Mining - Mining rocks";
            } else {
                Microbot.status = "Training Mining - No rocks found";
            }
        }
    }
}