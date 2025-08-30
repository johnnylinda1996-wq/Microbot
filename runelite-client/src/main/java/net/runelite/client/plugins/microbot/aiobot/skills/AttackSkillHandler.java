package net.runelite.client.plugins.microbot.aiobot.skills;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

public class AttackSkillHandler implements SkillHandler {

    @Override
    public void execute() {
        if (Rs2Player.isInCombat()) return;

        // Use Rs2Npc.attack with String name (correct method signature)
        if (Rs2Npc.attack("Chicken")) {
            Microbot.status = "Training Attack - Fighting Chicken";
        } else if (Rs2Npc.attack("Cow")) {
            Microbot.status = "Training Attack - Fighting Cow";
        } else if (Rs2Npc.attack("Goblin")) {
            Microbot.status = "Training Attack - Fighting Goblin";
        } else {
            Microbot.status = "Training Attack - No targets found";
        }
    }
}