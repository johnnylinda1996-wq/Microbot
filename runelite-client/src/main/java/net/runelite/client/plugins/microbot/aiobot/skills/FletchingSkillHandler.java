package net.runelite.client.plugins.microbot.aiobot.skills;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

public class FletchingSkillHandler implements SkillHandler {

    @Override
    public void execute() {
        if (Rs2Inventory.hasItem("Logs") && Rs2Inventory.hasItem("Knife")) {
            Rs2Inventory.combine("Knife", "Logs");
            if (Rs2Widget.getWidget(17694734) != null) {
                Rs2Keyboard.keyPress('1');
                Microbot.status = "Training Fletching - Making arrow shafts";
            }
        } else {
            Microbot.status = "Training Fletching - Need logs and knife";
        }
    }
}