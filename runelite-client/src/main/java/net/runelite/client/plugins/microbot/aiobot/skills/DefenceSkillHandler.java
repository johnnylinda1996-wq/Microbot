package net.runelite.client.plugins.microbot.aiobot.skills;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

public class DefenceSkillHandler implements SkillHandler {

    @Override
    public void execute() {
        if (Rs2Player.isInCombat()) return;

        if (Rs2Npc.attack("Chicken")) {
            Microbot.status = "Training Defence - Fighting Chicken";
        } else if (Rs2Npc.attack("Cow")) {
            Microbot.status = "Training Defence - Fighting Cow";
        } else if (Rs2Npc.attack("Goblin")) {
            Microbot.status = "Training Defence - Fighting Goblin";
        } else {
            Microbot.status = "Training Defence - No targets found";
        }
    }
}