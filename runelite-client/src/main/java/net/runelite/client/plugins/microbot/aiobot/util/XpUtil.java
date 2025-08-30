package net.runelite.client.plugins.microbot.aiobot.util;

import net.runelite.api.Skill;

public final class XpUtil {
    private XpUtil(){}

    // OSRS XP tabel (index = level, value = totaal xp voor dat level)
    // Level 0 ongebruikt om index = level te houden.
    private static final int[] XP_TABLE = {
            0,
            0,83,174,276,388,512,650,801,969,1154,
            1358,1584,1833,2107,2411,2746,3115,3523,3973,4470,
            5018,5624,6291,7028,7842,8740,9730,10824,12031,13363,
            14833,16456,18247,20224,22406,24815,27473,30408,33648,37224,
            41171,45529,50339,55649,61512,67983,75127,83014,91721,101333,
            111945,123660,136594,150872,166636,184040,203254,224466,247886,273742,
            302288,333804,368599,407015,449428,496254,547953,605032,668051,737627,
            814445,899257,992895,1096278,1210421,1336443,1475581,1629200,1798808,1986068,
            2192818,2421087,2673114,2951373,3258594,3597792,3972294,4385776,4842295,5346332,
            5902831,6517253,7195629,7944614,8771558,9684577,10692629,11805606,13034431
    };

    public static int getXpForLevel(int level) {
        if (level < 1) return 0;
        if (level >= XP_TABLE.length) return XP_TABLE[XP_TABLE.length - 1];
        return XP_TABLE[level];
    }

    public static double percentToTarget(int currentXp, int targetLevel) {
        int targetXp = getXpForLevel(targetLevel);
        int level = levelForXp(currentXp);
        int baseXp = getXpForLevel(level);
        int nextXp = targetXp;
        if (targetXp <= currentXp) return 100.0;
        // Procent van huidige naar target
        double gainedTowardsTarget = currentXp - baseXp;
        double totalNeeded = targetXp - baseXp;
        if (totalNeeded <= 0) return 100.0;
        double p = (gainedTowardsTarget / totalNeeded) * 100.0;
        if (p < 0) p = 0;
        if (p > 100) p = 100;
        return p;
    }

    public static int levelForXp(int xp) {
        for (int lvl = XP_TABLE.length - 1; lvl >= 1; lvl--) {
            if (xp >= XP_TABLE[lvl]) return lvl;
        }
        return 1;
    }

    public static int xpForTarget(int targetLevel) {
        return getXpForLevel(targetLevel);
    }
}