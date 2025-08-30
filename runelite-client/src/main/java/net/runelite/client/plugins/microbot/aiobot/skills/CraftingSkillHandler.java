package net.runelite.client.plugins.microbot.aiobot.skills;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

public class CraftingSkillHandler implements SkillHandler {

    @Override
    public void execute() {
        if (Rs2Inventory.hasItem("Leather") && Rs2Inventory.hasItem("Needle") && Rs2Inventory.hasItem("Thread")) {
            Rs2Inventory.combine("Needle", "Leather");
            if (Rs2Widget.getWidget(17694734) != null) {
                Rs2Keyboard.keyPress('1');
                Microbot.status = "Training Crafting - Making leather items";
            }
        } else {
            Microbot.status = "Training Crafting - Need leather, needle, and thread";
        }
    }
}