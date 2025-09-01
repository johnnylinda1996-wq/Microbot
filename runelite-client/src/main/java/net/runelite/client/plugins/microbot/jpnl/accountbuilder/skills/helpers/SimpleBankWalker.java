package net.runelite.client.plugins.microbot.jpnl.accountbuilder.skills.helpers;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;

/**
 * Eenvoudige banking helper zonder walkToBank() (die bestaat niet in jouw codebase).
 * Probeert enkel een bank te openen; uitgebreid padlopen kan later met Rs2Walker.walkTo(WorldPoint).
 */
public final class SimpleBankWalker {

    private SimpleBankWalker() {}

    /**
     * Probeert bank te openen en alle items te deponeren behalve de meegegeven keep-namen.
     * @param keep items die je wilt behouden (bijv. pickaxe/axe).
     * @return true als er succesvol is gedeeld/gedeponeerd, false anders.
     */
    public static boolean bankAllExcept(String... keep) {
        try {
            if (!Rs2Bank.isOpen()) {
                if (!Rs2Bank.openBank()) {
                    Microbot.status = "Bank: kan bank niet openen (geen walkToBank methode beschikbaar)";
                    return false;
                }
            }
            Rs2Bank.depositAllExcept(keep);
            return true;
        } catch (Exception ex) {
            Microbot.log("Bank error: " + ex.getMessage());
            return false;
        }
    }
}