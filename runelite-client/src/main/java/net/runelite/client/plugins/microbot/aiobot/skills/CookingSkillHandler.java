package net.runelite.client.plugins.microbot.aiobot.skills;

import net.runelite.api.ObjectID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

public class CookingSkillHandler implements SkillHandler {

    @Override
    public void execute() {
        if (Rs2Inventory.hasItem("Raw shrimps")) {
            if (Rs2GameObject.interact(ObjectID.FIRE, "Cook")) {
                if (Rs2Widget.getWidget(17694734) != null) {
                    Rs2Keyboard.keyPress('1');
                    Microbot.status = "Training Cooking - Cooking shrimps";
                }
            }
        } else {
            Microbot.status = "Training Cooking - Need raw shrimps";
        }
    }
}