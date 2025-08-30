package net.runelite.client.plugins.microbot.aiobot.skills.helpers;

/**
 * Zeer eenvoudige boomprogressie. Breid later uit (eiken, wilgen, maples, yews, magics).
 */
public final class TreeProgressionHelper {

    private TreeProgressionHelper() {}

    public static String bestTreeForLevel(int wcLevel) {
        if (wcLevel >= 75) return "Magic tree";
        if (wcLevel >= 60) return "Yew tree";
        if (wcLevel >= 45) return "Maple tree";
        if (wcLevel >= 30) return "Willow tree";
        if (wcLevel >= 15) return "Oak tree";
        return "Tree"; // gewone
    }
}