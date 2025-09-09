package net.runelite.client.plugins.microbot.example;

import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.mule.MuleHelper;
import net.runelite.client.plugins.microbot.util.mule.MuleBridgeClient;

/**
 * Voorbeeld script dat de Mule Trading System gebruikt
 */
public class ExampleBotWithMule extends Script {

    public boolean run() {
        System.out.println("Bot gestart - met Mule ondersteuning!");

        while (isRunning()) {

            // Je normale bot activiteiten hier...
            doNormalBotActivities();

            // Check als inventory vol is
            if (Rs2Inventory.isFull()) {
                System.out.println("Inventory vol! Mule aanvragen...");
                requestMuleService();
            }

            sleep(1000);
        }

        return true;
    }

    private void doNormalBotActivities() {
        // Bijvoorbeeld: mining, fishing, combat, etc.
        // Je normale bot logica hier
        System.out.println("Bot doet normale activiteiten...");
    }

    private void requestMuleService() {
        // Optie 1: Simpele mule request
        requestMuleSimple();

        // Of Optie 2: Geavanceerde mule request met monitoring
        // requestMuleAdvanced();
    }

    private void requestMuleSimple() {
        System.out.println("=== SIMPELE MULE REQUEST ===");

        // Vraag mule aan op Grand Exchange
        MuleHelper.requestMuleWhenFull("Grand Exchange")
            .thenAccept(success -> {
                if (success) {
                    System.out.println("✅ Mule request succesvol!");
                    System.out.println("Bot kan doorgaan met andere activiteiten...");
                } else {
                    System.err.println("❌ Mule request gefaald!");
                }
            });

        System.out.println("Mule request verstuurd, bot gaat door...");
    }

    private void requestMuleAdvanced() {
        System.out.println("=== GEAVANCEERDE MULE REQUEST ===");

        // Custom locatie met monitoring
        MuleHelper.requestMuleWithMonitoring(
            "3164,3486,0", // Grand Exchange coordinaten
            status -> {
                // Real-time status updates
                System.out.println("📊 Mule Status: " + status.status + " - " + status.currentStep);

                switch (status.status) {
                    case "QUEUED":
                        System.out.println("⏳ Mule request in wachtrij...");
                        break;
                    case "PROCESSING":
                        System.out.println("🏃 Mule is onderweg! (" + status.currentStep + ")");
                        break;
                    case "COMPLETED":
                        System.out.println("✅ Trade voltooid!");
                        break;
                    case "FAILED":
                        System.out.println("❌ Trade gefaald!");
                        break;
                }
            },
            success -> {
                // Final callback when done
                if (success) {
                    System.out.println("🎉 MULE TRADE SUCCESVOL VOLTOOID!");
                    System.out.println("Inventory is nu leeg, bot kan verder...");

                    // Resume normal bot activities
                    resumeNormalActivities();
                } else {
                    System.err.println("💥 MULE TRADE GEFAALD!");
                    System.err.println("Bot moet handmatig inventory legen...");

                    // Handle failed mule trade
                    handleFailedMuleTrade();
                }
            }
        );
    }

    private void resumeNormalActivities() {
        System.out.println("🔄 Bot hervat normale activiteiten...");
        // Ga verder met je bot logica
    }

    private void handleFailedMuleTrade() {
        System.out.println("🛠️ Handling failed mule trade...");
        // Misschien banking, of stop bot, of retry

        // Bijvoorbeeld: ga naar bank
        // Rs2Walker.walkTo(new WorldPoint(3185, 3436, 0)); // Varrock bank
    }

    // Extra utility methods
    private boolean isMuleBridgeOnline() {
        return MuleHelper.isMuleBridgeOnline().join();
    }

    private void checkMuleBridgeStatus() {
        boolean isOnline = isMuleBridgeOnline();
        System.out.println("Bridge Status: " + (isOnline ? "✅ Online" : "❌ Offline"));

        if (!isOnline) {
            System.out.println("⚠️ Mule Bridge offline - start eerst de bridge server!");
            System.out.println("Run: java -jar mule-bridge/target/MuleBridge.jar");
        }
    }

    @Override
    public void shutdown() {
        System.out.println("Bot wordt afgesloten...");
        super.shutdown();
    }
}
