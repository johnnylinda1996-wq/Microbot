package net.runelite.client.plugins.microbot.aiobot.skills;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

public class RangedSkillHandler implements SkillHandler {

    @Override
    public void execute() {
        if (Rs2Player.isInCombat()) return;

        // Check for arrows/ammo
        if (!Rs2Inventory.hasItem("Bronze arrow") && !Rs2Inventory.hasItem("Iron arrow")) {
            Microbot.status = "Training Ranged - Need arrows";
            return;
        }

        if (Rs2Npc.attack("Chicken")) {
            Microbot.status = "Training Ranged - Fighting Chicken";
        } else if (Rs2Npc.attack("Cow")) {
            Microbot.status = "Training Ranged - Fighting Cow";
        } else if (Rs2Npc.attack("Goblin")) {
            Microbot.status = "Training Ranged - Fighting Goblin";
        } else {
            Microbot.status = "Training Ranged - No targets found";
        }
    }
}