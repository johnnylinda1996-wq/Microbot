package net.runelite.client.plugins.microbot.aiobot.skills.helpers;

import java.util.ArrayList;
import java.util.List;

/**
 * Bepaalt welke ore naam geschikt is voor een gegeven mining level.
 * Eenvoudig model; breid later uit met locaties / banking nodes.
 * Java 8 compatible (geen records).
 */
public final class OreProgressionHelper {

    private static final List<OreTier> ORES = new ArrayList<>();

    static {
        // levelRequirement, displayName
        ORES.add(new OreTier(1,  "Copper ore"));
        ORES.add(new OreTier(1,  "Tin ore"));          // alternatief
        ORES.add(new OreTier(15, "Iron ore"));
        ORES.add(new OreTier(30, "Coal"));
        ORES.add(new OreTier(40, "Gold ore"));
        ORES.add(new OreTier(55, "Mithril ore"));
        ORES.add(new OreTier(70, "Adamantite ore"));
        ORES.add(new OreTier(85, "Runite ore"));
    }

    private OreProgressionHelper() {}

    public static String bestOreForLevel(int miningLevel) {
        String best = "Copper ore";
        for (int i = 0; i < ORES.size(); i++) {
            OreTier t = ORES.get(i);
            if (miningLevel >= t.levelReq) {
                best = t.displayName;
            } else {
                break;
            }
        }
        return best;
    }

    public static String nextOreAfter(String current) {
        for (int i = 0; i < ORES.size(); i++) {
            OreTier t = ORES.get(i);
            if (t.displayName.equalsIgnoreCase(current) && i + 1 < ORES.size()) {
                return ORES.get(i + 1).displayName;
            }
        }
        return current;
    }

    // Eenvoudige inner class ipv record
    private static final class OreTier {
        final int levelReq;
        final String displayName;

        private OreTier(int levelReq, String displayName) {
            this.levelReq = levelReq;
            this.displayName = displayName;
        }
    }
}