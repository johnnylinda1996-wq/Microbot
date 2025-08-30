package net.runelite.client.plugins.microbot.aiobot.skills;

import net.runelite.api.gameval.ObjectID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

public class WoodcuttingSkillHandler implements SkillHandler {

    @Override
    public void execute() {
        if (Rs2Player.isAnimating()) return;

        if (Rs2Inventory.isFull()) {
            Rs2Inventory.drop("Logs");
            Microbot.status = "Training Woodcutting - Dropping logs";
        } else {
            if (Rs2GameObject.interact(ObjectID.TREE, "Chop down")) {
                Microbot.status = "Training Woodcutting - Cutting tree";
            } else {
                Microbot.status = "Training Woodcutting - No trees found";
            }
        }
    }
}