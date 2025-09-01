package net.runelite.client.plugins.microbot.jpnl.accountbuilder.skills.helpers;

import net.runelite.client.plugins.microbot.Microbot;

/**
 * Placeholder voor special attack functionaliteit.
 * In jouw huidige codebase is geen methode beschikbaar om spec-percentage op te vragen.
 */
public final class SpecUtil {

    private SpecUtil() {}

    /**
     * Retourneert altijd false zolang we geen echte spec-percentage bron hebben.
     * Pas dit aan zodra je een varbit/varp of util gevonden hebt.
     */
    public static boolean canUseSpec(int minPercent) {
        return false;
    }

    /**
     * Doet niets behalve loggen; retourneert false zodat callers niet denken dat spec actief is.
     */
    public static boolean activateSpec() {
        Microbot.log("SpecUtil: activateSpec() placeholder (geen implementatie beschikbaar)");
        return false;
    }
}