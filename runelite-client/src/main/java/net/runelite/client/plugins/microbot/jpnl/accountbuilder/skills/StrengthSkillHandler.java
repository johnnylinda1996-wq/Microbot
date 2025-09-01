package net.runelite.client.plugins.microbot.jpnl.accountbuilder.skills;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

public class StrengthSkillHandler implements SkillHandler {

    @Override
    public void execute() {
        if (Rs2Player.isInCombat()) return;

        if (Rs2Npc.attack("Chicken")) {
            Microbot.status = "Training Strength - Fighting Chicken";
        } else if (Rs2Npc.attack("Cow")) {
            Microbot.status = "Training Strength - Fighting Cow";
        } else if (Rs2Npc.attack("Goblin")) {
            Microbot.status = "Training Strength - Fighting Goblin";
        } else {
            Microbot.status = "Training Strength - No targets found";
        }
    }
}