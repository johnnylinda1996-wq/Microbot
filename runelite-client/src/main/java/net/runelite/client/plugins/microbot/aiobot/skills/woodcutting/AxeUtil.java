package net.runelite.client.plugins.microbot.aiobot.skills.woodcutting;

import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;

import java.util.Arrays;
import java.util.List;

/**
 * Bepaalt beste beschikbare axe in inventory (later ook uit bank).
 */
public final class AxeUtil {
    private AxeUtil(){}

    public static final List<Integer> AXE_PRIORITY = Arrays.asList(
            // Beste -> slechtste (IDs uit ItemID)
            net.runelite.api.gameval.ItemID.INFERNAL_AXE,
            net.runelite.api.gameval.ItemID.DRAGON_AXE,
            net.runelite.api.gameval.ItemID.RUNE_AXE,
            net.runelite.api.gameval.ItemID.ADAMANT_AXE,
            net.runelite.api.gameval.ItemID.MITHRIL_AXE,
            net.runelite.api.gameval.ItemID.STEEL_AXE,
            net.runelite.api.gameval.ItemID.BLACK_AXE,
            net.runelite.api.gameval.ItemID.IRON_AXE,
            net.runelite.api.gameval.ItemID.BRONZE_AXE
    );

    public static Integer findBestAxeInInventory() {
        for (int id : AXE_PRIORITY) {
            if (Rs2Inventory.hasItem(id)) return id;
        }
        return null;
    }

    public static boolean meetsLevelReq(int itemId) {
        Client c = Microbot.getClient();
        if (c == null) return false;
        int wc = c.getRealSkillLevel(Skill.WOODCUTTING);
        switch (itemId) {
            case net.runelite.api.gameval.ItemID.DRAGON_AXE:
            case net.runelite.api.gameval.ItemID.INFERNAL_AXE:
                return wc >= 61;
            case net.runelite.api.gameval.ItemID.RUNE_AXE:
                return wc >= 41;
            case net.runelite.api.gameval.ItemID.ADAMANT_AXE:
                return wc >= 31;
            case net.runelite.api.gameval.ItemID.MITHRIL_AXE:
                return wc >= 21;
            case net.runelite.api.gameval.ItemID.BLACK_AXE:
                return wc >= 11;
            case net.runelite.api.gameval.ItemID.STEEL_AXE:
                return wc >= 6;
            default:
                return true;
        }
    }
}