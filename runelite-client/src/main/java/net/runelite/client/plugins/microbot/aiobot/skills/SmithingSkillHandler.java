package net.runelite.client.plugins.microbot.aiobot.skills;

import net.runelite.api.ObjectID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

public class SmithingSkillHandler implements SkillHandler {

    @Override
    public void execute() {
        if (Rs2Inventory.hasItem("Bronze bar") && Rs2Inventory.hasItem("Hammer")) {
            if (Rs2GameObject.interact(ObjectID.ANVIL, "Smith")) {
                if (Rs2Widget.getWidget(17694734) != null) {
                    Rs2Keyboard.keyPress('1');
                    Microbot.status = "Training Smithing - Smithing bronze";
                }
            }
        } else {
            Microbot.status = "Training Smithing - Need bronze bars and hammer";
        }
    }
}