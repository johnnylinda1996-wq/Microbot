package net.runelite.client.plugins.microbot.muletest;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.mule.MuleBridgeClient;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.Global;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.Rs2BankSeller;
import net.runelite.client.plugins.microbot.util.botmanager.Rs2BotManager;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.events.PluginPauseEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;

@Slf4j
public class MuleTestScript extends Script {

    public static String version = "1.0.0";
    private MuleTestConfig config;
    private boolean muleRequested = false;
    private List<String> pausedBots = new ArrayList<>();
    private boolean itemsSold = false;
    private boolean coinsCollected = false;
    private long scriptStartTime = 0;
    private long lastMuleTime = 0;
    private boolean hasDroppedThisRequest = false;
    private WorldPoint originalLocation = null;
    private volatile boolean noMoreConfiguredItems = false;

    // Mule process states
    private enum MuleProcessState {
        WAITING,
        PAUSING_BOTS,
        SELLING_ITEMS,
        COLLECTING_COINS,
        REQUESTING_MULE,
        WALKING_TO_MULE,
        DROPPING_ITEMS,
        RETURNING_HOME,
        RESUMING_BOTS,
        COMPLETED
    }

    /**
     * Checks if the bank still contains any of the configured items (by IDs or Names)
     */
    private boolean bankHasConfiguredItems() {
        try {
            // Quick check: if bank not open, open it briefly
            boolean openedHere = false;
            if (!net.runelite.client.plugins.microbot.util.bank.Rs2Bank.isOpen()) {
                if (!net.runelite.client.plugins.microbot.util.bank.Rs2Bank.openBank()) {
                    return false;
                }
                openedHere = true;
                net.runelite.client.plugins.microbot.util.Global.sleep(200, 400);
            }

            int[] ids = getItemIdsToSell();
            for (int id : ids) {
                if (net.runelite.client.plugins.microbot.util.bank.Rs2Bank.hasItem(id)) {
                    if (openedHere) net.runelite.client.plugins.microbot.util.bank.Rs2Bank.closeBank();
                    return true;
                }
            }

            String[] names = getItemNamesToSell();
            for (String name : names) {
                if (name != null && !name.isEmpty() && net.runelite.client.plugins.microbot.util.bank.Rs2Bank.hasItem(name)) {
                    if (openedHere) net.runelite.client.plugins.microbot.util.bank.Rs2Bank.closeBank();
                    return true;
                }
            }

            if (openedHere) net.runelite.client.plugins.microbot.util.bank.Rs2Bank.closeBank();
            return false;
        } catch (Exception e) {
            log.debug("bankHasConfiguredItems check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Fixed Grand Exchange location used for walking and interactions.
     */
    private WorldPoint getGrandExchangeLocation() {
        return new WorldPoint(3164, 3486, 0);
    }

    private MuleProcessState currentState = MuleProcessState.WAITING;

    public boolean run(MuleTestConfig config) {
        this.config = config;
        this.scriptStartTime = System.currentTimeMillis();
        this.lastMuleTime = System.currentTimeMillis();

        log.info("MuleTest script started with TIME INTERVAL: {} hours", config.muleIntervalHours());
        log.info("Sell items first: {}", config.sellItemsFirst());
        log.info("Item IDs to sell: {}", config.sellItemIds());
        log.info("Item Names to sell: {}", config.sellItemNames());

        // Store original location
        if (Rs2Player.getLocalPlayer() != null) {
            originalLocation = Rs2Player.getWorldLocation();
        }

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                // Less frequent login checks to prevent timeout spam - only check every 5 seconds
                if (!isClientReady()) {
                    return;
                }

                if (!super.run()) return;

                // Check if it's time for mule request
                checkTimeForMuleRequest();

            } catch (Exception ex) {
                log.error("Error in mule test script: ", ex);
            }
        }, 0, 5000, TimeUnit.MILLISECONDS); // Changed from 1000ms to 5000ms (5 seconds)
        return true;
    }

    /**
     * Check if it's time to request a mule based on the configured time interval
     */
    private void checkTimeForMuleRequest() {
        try {
            long currentTime = System.currentTimeMillis();
            long timeSinceLastMule = currentTime - lastMuleTime;
            double hoursSinceLastMule = timeSinceLastMule / (1000.0 * 60.0 * 60.0);

            // Log every minute for debugging
            if (timeSinceLastMule % 60000 < 1000) { // Every ~60 seconds
                String hoursFmt = String.format("%.2f", hoursSinceLastMule);
                log.info("Time since last mule: {} hours / {} hours needed | State: {}",
                    hoursFmt, config.muleIntervalHours(), currentState);
            }

            if (hoursSinceLastMule >= config.muleIntervalHours() && currentState == MuleProcessState.WAITING) {
                log.info("⏰ Time interval of {} hours reached! Starting mule process...", config.muleIntervalHours());
                startMuleProcess();
            }

        } catch (Exception e) {
            log.error("Error checking time for mule request: ", e);
        }
    }

    /**
     * Start the complete mule process
     */
    private void startMuleProcess() {
        try {
            currentState = MuleProcessState.PAUSING_BOTS;

            // Step 1: Pause other bots (without pausing our own script globally)
            pausedBots = Rs2BotManager.pauseAllBots(this);
            log.info("⏸️ Paused {} other bot scripts", pausedBots.size());

            // Step 2: Sell items if enabled
            if (config.sellItemsFirst()) {
                currentState = MuleProcessState.SELLING_ITEMS;
                log.info("🏪 Starting item selling process...");

                if (performBankingAndSelling()) {
                    log.info("✅ Item selling completed successfully");
                    // Coin collection is already handled in performBankingAndSelling()
                } else {
                    log.error("❌ Item selling/banking failed. Stopping mule process and not proceeding to next steps.");
                    // Fail-fast: do not continue to mule step; resume bots and reset state
                    resetMuleProcess();
                    return;
                }
            }

            // Step 3: Request mule
            currentState = MuleProcessState.REQUESTING_MULE;
            requestMuleService();

        } catch (Exception e) {
            log.error("Error starting mule process: ", e);
            resetMuleProcess();
        }
    }

    /**
     * Complete banking and selling process with proper GE logic
     */
    private boolean performBankingAndSelling() {
        try {
            log.info("🏪 Starting comprehensive banking and selling process...");
            // Reset batch-ending flag at the start of a full run
            noMoreConfiguredItems = false;

            // Step 1: Ensure we're on the correct world
            if (!ensureCorrectWorld()) {
                log.error("❌ Failed to switch to correct world");
                return false;
            }

            // Step 2: Go to Grand Exchange (fixed location - only place to sell items)
            WorldPoint grandExchangeLocation = getGrandExchangeLocation();

            if (!Rs2Player.getWorldLocation().equals(grandExchangeLocation)) {
                log.info("🚶 Walking to Grand Exchange: {}...", grandExchangeLocation);
                Rs2Walker.walkTo(grandExchangeLocation);

                if (!Global.sleepUntil(() -> Rs2Player.getWorldLocation().distanceTo(grandExchangeLocation) <= 5, 30000)) {
                    log.error("❌ Failed to reach Grand Exchange");
                    return false;
                }
            }

            // Step 3-5: Batch loop -> withdraw until inv full, sell, repeat until no more items in bank
            int batch = 0;
            while (true) {
                // Safe stop-guard: if we got logged out, abort batch gracefully
                if (!Microbot.isLoggedIn()) {
                    log.info("🛑 Not logged in during batch loop, aborting banking/selling");
                    return false;
                }
                batch++;
                log.info("📦 Preparing batch #{} from bank...", batch);

                if (!handleBankingProcess()) {
                    log.error("❌ Banking process failed");
                    return false;
                }

                // If inventory is empty after banking, nothing to sell -> break
                if (net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory.isEmpty()) {
                    // Small settle delay before probing the bank again
                    net.runelite.client.plugins.microbot.util.Global.sleep(300, 600);
                    if (!noMoreConfiguredItems && bankHasConfiguredItems()) {
                        log.info("🔁 Inventory leeg maar bank heeft nog items; volgende batch voorbereiden...");
                        net.runelite.client.plugins.microbot.util.Global.sleep(300, 600);
                        continue;
                    } else {
                        log.info("✅ Geen items om te verkopen; bank leeg voor geselecteerde items");
                        break;
                    }
                }

                if (!handleGrandExchangeSelling()) {
                    log.error("❌ Grand Exchange selling failed");
                    // Fail fast: niet doorgaan naar coin-collect en mule
                    return false;
                }

                // Als inventory leeg is na sell, kijk of bank nog items heeft; zo ja, volgende batch
                // We bepalen dit door te proberen een mini-withdraw check te doen in de volgende loop-iteratie
                // (handleBankingProcess() zal dan meteen niets vinden en breken)
            }

            // Afronden: coins ophalen van GE naar bank en dan alle coins opnemen
            try {
                if (net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange.openExchange()) {
                    net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange.collectAllToBank();
                    net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange.closeExchange();
                    net.runelite.client.plugins.microbot.util.Global.sleep(400, 800);
                }
            } catch (Exception ignored) {}

            if (net.runelite.client.plugins.microbot.util.bank.Rs2Bank.openBank()) {
                // Deposit restanten behalve coins
                net.runelite.client.plugins.microbot.util.bank.Rs2Bank.depositAllExcept("Coins");
                net.runelite.client.plugins.microbot.util.Global.sleep(400, 800);
                if (net.runelite.client.plugins.microbot.util.bank.Rs2Bank.hasItem("Coins")) {
                    log.info("💰 Withdrawing all coins from bank");
                    net.runelite.client.plugins.microbot.util.bank.Rs2Bank.withdrawAll("Coins");
                    net.runelite.client.plugins.microbot.util.Global.sleep(400, 800);
                }
                net.runelite.client.plugins.microbot.util.bank.Rs2Bank.closeBank();
            }

            log.info("✅ Complete banking and selling process finished");
            return true;

        } catch (Exception e) {
            log.error("Error in banking and selling process: ", e);
            return false;
        }
    }

    /**
     * Ensure we're on the correct world before starting mule process
     */
    private boolean ensureCorrectWorld() {
        try {
            int currentWorld = Microbot.getClient().getWorld();
            int targetWorld = config.world();

            if (currentWorld != targetWorld) {
                log.info("🌍 Current world: {}, target world: {}. Switching worlds...", currentWorld, targetWorld);

                // Use the Microbot world hopping utility
                if (Microbot.hopToWorld(targetWorld)) {
                    // Wait for world switch to complete
                    if (Global.sleepUntil(() -> Microbot.getClient().getWorld() == targetWorld, 15000)) {
                        log.info("✅ Successfully switched to world {}", targetWorld);
                        // Allow the region, NPCs and interfaces to settle after hop
                        Global.sleep(5000);
                        return true;
                    } else {
                        log.error("❌ World switch timed out");
                        return false;
                    }
                } else {
                    log.error("❌ Failed to initiate world switch");
                    return false;
                }
            } else {
                log.info("✅ Already on correct world: {}", currentWorld);
                return true;
            }

        } catch (Exception e) {
            log.error("Error checking/switching world: ", e);
            return false;
        }
    }

    /**
     * Handle complete banking process: deposit, set noted, withdraw items
     */
    private boolean handleBankingProcess() {
        try {
            // Open bank at Grand Exchange
            log.info("🏦 Opening bank at Grand Exchange...");
            if (!Rs2Bank.openBank()) {
                log.error("❌ Could not open bank at GE");
                return false;
            }
            // Small settle delay for bank interface to fully load
            Global.sleep(1000);

            // Deposit all current inventory
            log.info("📦 Depositing all inventory items...");
            Rs2Bank.depositAll();
            Global.sleep(600, 1200);

            // Set withdraw mode to noted (reliable helper)
            log.info("📋 Setting bank withdraw mode to Note...");
            net.runelite.client.plugins.microbot.util.bank.Rs2Bank.setWithdrawAsNote();
            Global.sleep(300, 600);

            // Withdraw all items that need to be sold
            boolean anyItemsWithdrawn = withdrawAllConfiguredItems();

            if (!anyItemsWithdrawn) {
                log.warn("⚠️ No items were withdrawn from bank (end of batches)");
                Rs2Bank.closeBank();
                // Mark that there are no more configured items so the batch loop can break cleanly
                noMoreConfiguredItems = true;
                return true;
            }

            // Close bank
            Rs2Bank.closeBank();
            Global.sleep(500, 1000);

            log.info("✅ Banking process completed successfully");
            return true;

        } catch (Exception e) {
            log.error("Error in banking process: ", e);
            return false;
        }
    }

    /**
     * Withdraw all configured items from bank (supports multiple inventories)
     */
    private boolean withdrawAllConfiguredItems() {
        boolean anyItemsWithdrawn = false;
        int totalWithdrawn = 0;

        try {
            if (!super.run()) return false;
            // Get item IDs and names to withdraw
            int[] itemIds = getItemIdsToSell();
            String[] itemNames = getItemNamesToSell();

            // Withdraw by item IDs
            for (int itemId : itemIds) {
                if (!super.run()) break;
                if (net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory.isFull()) {
                    log.info("📦 Inventory is full, stop withdrawing by IDs");
                    break;
                }
                if (Rs2Bank.hasItem(itemId)) {
                    int bankQuantity = Rs2Bank.count(itemId);
                    log.info("💰 Found {} x item ID {} in bank - withdrawing all", bankQuantity, itemId);

                    // Withdraw all of this item
                    Rs2Bank.withdrawAll(itemId);
                    Global.sleep(600, 1200);

                    // Check if we need to do multiple withdrawals for large quantities
                    if (bankQuantity > 28) {  // More than one inventory
                        log.info("📦 Large quantity detected - handling multiple inventory loads");
                        while (!net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory.isFull()
                                && Rs2Bank.hasItem(itemId) && Rs2Inventory.hasItem(itemId)) {
                            if (!super.run()) break;
                            // Make space by noting what we have
                            int inventoryCount = Rs2Inventory.count(itemId);
                            totalWithdrawn += inventoryCount;
                            log.info("📊 Withdrew {} so far, continuing...", totalWithdrawn);

                            // Continue withdrawing if more in bank
                            if (Rs2Bank.hasItem(itemId)) {
                                Rs2Bank.withdrawAll(itemId);
                                Global.sleep(600, 1200);
                            }
                        }
                    }

                    anyItemsWithdrawn = true;
                } else {
                    log.debug("Item ID {} not found in bank", itemId);
                }
            }

            // Withdraw by item names (if no IDs specified or as additional)
            for (String itemName : itemNames) {
                if (!super.run()) break;
                if (net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory.isFull()) {
                    log.info("📦 Inventory is full, stop withdrawing by names");
                    break;
                }
                if (!itemName.isEmpty() && Rs2Bank.hasItem(itemName)) {
                    int bankQuantity = Rs2Bank.count(itemName);
                    log.info("💰 Found {} x '{}' in bank - withdrawing all", bankQuantity, itemName);

                    Rs2Bank.withdrawAll(itemName);
                    Global.sleep(600, 1200);

                    // Handle multiple inventories for large quantities
                    if (bankQuantity > 28) {
                        while (!net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory.isFull()
                                && Rs2Bank.hasItem(itemName) && Rs2Inventory.hasItem(itemName)) {
                            if (!super.run()) break;
                            int inventoryCount = Rs2Inventory.count(itemName);
                            totalWithdrawn += inventoryCount;
                            log.info("📊 Withdrew {} '{}' so far, continuing...", inventoryCount, itemName);

                            if (Rs2Bank.hasItem(itemName)) {
                                Rs2Bank.withdrawAll(itemName);
                                Global.sleep(600, 1200);
                            }
                        }
                    }

                    anyItemsWithdrawn = true;
                } else if (!itemName.isEmpty()) {
                    log.debug("Item '{}' not found in bank", itemName);
                }
            }

            if (anyItemsWithdrawn) {
                int finalInventoryCount = Rs2Inventory.count();
                log.info("✅ Withdrawal complete - {} items total in inventory", finalInventoryCount);
            }

        } catch (Exception e) {
            log.error("Error withdrawing items: ", e);
        }

        return anyItemsWithdrawn;
    }

    /**
     * Handle Grand Exchange selling with proper interface interaction
     */
    private boolean handleGrandExchangeSelling() {
        try {
            log.info("🛒 Starting Grand Exchange selling process...");

            // Verkoop alleen verhandelbare items die in de configuratie zijn opgegeven (IDs of namen)
            int[] allowedIds = getItemIdsToSell();
            java.util.Set<Integer> allowedIdSet = java.util.Arrays.stream(allowedIds).boxed().collect(java.util.stream.Collectors.toSet());
            java.util.Set<String> allowedNameSet = new java.util.HashSet<>();
            for (String n : getItemNamesToSell()) { if (n != null && !n.isEmpty()) allowedNameSet.add(n.toLowerCase()); }

            if (allowedIdSet.isEmpty() && allowedNameSet.isEmpty()) {
                log.warn("⚠️ Geen item IDs of namen geconfigureerd om te verkopen. Sla GE-verkoop over.");
                return false;
            }

            // Verzamel de items VOORDAT we de GE openen (zoals in BankSellerScript),
            // gebruik items() zonder predicate en filter per item in de loop om edgecases te voorkomen
            final java.util.List<net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel> items =
                    net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory.items()
                            .collect(java.util.stream.Collectors.toList());
            if (items.isEmpty()) {
                log.warn("⚠️ Inventory is leeg vlak voor GE-open; sla GE-verkoop over");
                return false;
            }

            // Debug: laat zien welke items we gaan verkopen
            try {
                String dbg = items.stream().map(net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel::getName)
                        .distinct().limit(10).reduce((a,b) -> a + ", " + b).orElse("<none>");
                log.info("🧾 Items to sell (sample): {} (total {} stacks)", dbg, items.size());
            } catch (Exception ignored) {}

            // Extra debug: huidige inventory stack namen
            try {
                String invDbg = net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory.all()
                        .stream().map(net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel::getName)
                        .distinct().limit(12).reduce((a,b) -> a + ", " + b).orElse("<empty>");
                log.info("🎒 Inventory now: {}", invDbg);
            } catch (Exception ignored) {}

            // Open Grand Exchange via utility (handles clerk/booth and pin)
            if (!net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange.openExchange()) {
                log.error("❌ Failed to open Grand Exchange");
                return false;
            }
            net.runelite.client.plugins.microbot.util.Global.sleepUntil(
                    net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange::isOpen);
            // Small settle delay for GE interface
            net.runelite.client.plugins.microbot.util.Global.sleep(300, 600);

            int offersCreated = 0;
            for (var item : items) {
                // Sla coins en niet-geconfigureerde items over
                String nm = item.getName() != null ? item.getName().toLowerCase() : "";
                if (nm.equals("coins")) continue;
                boolean allowed = allowedIdSet.contains(item.getId()) || allowedNameSet.stream().anyMatch(n -> !n.isEmpty() && nm.contains(n));
                if (!allowed) continue;
                if (!super.run()) {
                    log.info("🛑 Script stop detected during GE selling");
                    break;
                }

                // Wachten op vrije slots; indien verkopen klaar, eerst collecten naar bank
                while (net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange.getAvailableSlotsCount() == 0) {
                    if (!super.run()) break;
                    if (net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange.hasSoldOffer()) {
                        if (net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange.openExchange()) {
                            net.runelite.client.plugins.microbot.util.Global.sleepUntil(
                                    net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange::isOpen);
                            net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange.collectAllToBank();
                            net.runelite.client.plugins.microbot.util.Global.sleepUntil(() ->
                                    !net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange.hasSoldOffer());
                        }
                    }
                    net.runelite.client.plugins.microbot.util.Global.sleepUntil(() ->
                            net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange.getAvailableSlotsCount() > 0
                                    || net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange.hasSoldOffer());
                }
                if (net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange.getAvailableSlotsCount() == 0) {
                    break;
                }

                // Align with BankSellerScript, but fallback when price lookup is unreliable
                int basePrice = net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange.getPrice(item.getId());
                int percent = 0;
                try {
                    switch (config.sellPriceStrategy()) {
                        case MINUS_5: percent = -5; break;
                        case MINUS_10: percent = -10; break;
                        case MINUS_20: percent = -20; break;
                        case MARKET:
                        default: percent = 0; break;
                    }
                } catch (Exception ignored) { percent = 0; }

                if (basePrice <= 1) {
                    // Manual fallback: open offer via inventory and apply -5% clicks
                    if (!net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory.interact(item.getName(), "Offer", true)) {
                        continue;
                    }
                    net.runelite.client.plugins.microbot.util.Global.sleepUntil(
                            net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange::isOfferScreenOpen);
                    net.runelite.client.plugins.microbot.util.widget.Rs2Widget.clickWidget("All");
                    net.runelite.client.plugins.microbot.util.Global.sleep(200, 350);
                    int clicks = 0;
                    switch (config.sellPriceStrategy()) {
                        case MINUS_5: clicks = 1; break;
                        case MINUS_10: clicks = 2; break;
                        case MINUS_20: clicks = 4; break;
                        case MARKET:
                        default: clicks = 0; break;
                    }
                    for (int i = 0; i < clicks; i++) {
                        if (!net.runelite.client.plugins.microbot.util.widget.Rs2Widget.clickWidget("-5%")) break;
                        net.runelite.client.plugins.microbot.util.Global.sleep(150, 300);
                    }
                    net.runelite.client.plugins.microbot.util.widget.Rs2Widget.clickWidget("Confirm");
                    net.runelite.client.plugins.microbot.util.Global.sleepUntil(
                            () -> !net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange.isOfferScreenOpen());
                    net.runelite.client.plugins.microbot.util.Global.sleepUntil(
                            () -> !net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory.hasItem(item.getName(), true));
                    offersCreated++;
                } else {
                    // Normal path: processOffer with base price and percent
                    var request = net.runelite.client.plugins.microbot.util.grandexchange.GrandExchangeRequest.builder()
                            .action(net.runelite.client.plugins.microbot.util.grandexchange.GrandExchangeAction.SELL)
                            .itemName(item.getName())
                            .quantity(item.getQuantity())
                            .price(basePrice)
                            .percent(percent)
                            .closeAfterCompletion(false)
                            .build();

                    net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange.processOffer(request);
                    net.runelite.client.plugins.microbot.util.Global.sleepUntil(
                            net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange::isOpen);
                    net.runelite.client.plugins.microbot.util.Global.sleepUntil(
                            () -> !net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory.hasItem(item.getName(), true));
                    offersCreated++;
                }
            }

            // Sluit GE na afronden
            net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange.closeExchange();
            net.runelite.client.plugins.microbot.util.Global.sleepUntil(() ->
                    !net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange.isOpen());

            log.info("✅ Successfully created {} sell offers at Grand Exchange", offersCreated);
            return offersCreated > 0;

        } catch (Exception e) {
            log.error("Error in Grand Exchange selling: ", e);
            return false;
        }
    }

    /**
     * Open Grand Exchange interface
     */
    private boolean openGrandExchange() {
        try {
            // Try Grand Exchange booth first
            if (Rs2GameObject.interact("Grand Exchange booth", "Exchange")) {
                if (Global.sleepUntil(() -> isGrandExchangeOpen(), 5000)) {
                    return true;
                }
            }

            // Try Grand Exchange clerk as backup
            if (net.runelite.client.plugins.microbot.util.npc.Rs2Npc.interact(2149, "Exchange")) {
                if (Global.sleepUntil(() -> isGrandExchangeOpen(), 5000)) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            log.error("Error opening Grand Exchange: ", e);
            return false;
        }
    }

    /**
     * Check if Grand Exchange interface is open
     */
    private boolean isGrandExchangeOpen() {
        try {
            // Check for GE interface (widget ID 465 is common for GE)
            return Rs2Widget.getWidget(465, 0) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Sell specific item at Grand Exchange
     */
    private boolean sellItemAtGE(int itemId) {
        try {
            String itemName = getItemName(itemId);
            int quantity = Rs2Inventory.count(itemId);

            log.info("🏷️ Selling {} x {} (ID: {})", quantity, itemName, itemId);

            // Find empty sell slot (there are usually 8 slots, widgets 465,7 through 465,14)
            for (int slot = 7; slot <= 14; slot++) {
                if (isGESlotEmpty(slot)) {
                    log.debug("Using GE slot {}", slot);

                    // Click the empty sell slot
                    if (Rs2Widget.clickWidget(465, slot)) {
                        Global.sleep(300, 600);

                        // Click the item in inventory to select it
                        if (Rs2Inventory.interact(itemId, "Offer")) {
                            Global.sleep(600, 1200);

                            // Set quantity (click "All" or enter specific amount)
                            if (Rs2Widget.clickWidget("All")) {
                                Global.sleep(300, 600);
                            }

                            // Set price per configured strategy: MARKET, -5%, -10%, -20%
                            try {
                                int clicks = 0;
                                switch (config.sellPriceStrategy()) {
                                    case MINUS_5: clicks = 1; break;
                                    case MINUS_10: clicks = 2; break;
                                    case MINUS_20: clicks = 4; break;
                                    case MARKET:
                                    default: clicks = 0; break;
                                }
                                for (int i = 0; i < clicks; i++) {
                                    if (!Rs2Widget.clickWidget("-5%")) {
                                        adjustPriceDown();
                                    }
                                    Global.sleep(250, 450);
                                }
                            } catch (Exception ignored) { }

                            // Confirm the sale
                            if (Rs2Widget.clickWidget("Confirm")) {
                                Global.sleep(600, 1200);
                                log.info("✅ Successfully listed {} x {} for sale", quantity, itemName);
                                return true;
                            }
                        }
                    }
                    break;
                }
            }

            log.warn("⚠️ Failed to sell {} x {}", quantity, itemName);
            return false;

        } catch (Exception e) {
            log.error("Error selling item ID {}: ", itemId, e);
            return false;
        }
    }

    /**
     * Check if a GE slot is empty
     */
    private boolean isGESlotEmpty(int slot) {
        try {
            // Check if the slot widget is empty/available
            return Rs2Widget.getWidget(465, slot) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Adjust price down by 5% (manual price setting)
     */
    private void adjustPriceDown() {
        try {
            // This would involve more complex widget interaction to manually lower price
            // For now, just log the attempt
            log.debug("Attempting to adjust price down by 5%");
        } catch (Exception e) {
            log.warn("Could not adjust price down: ", e);
        }
    }

    /**
     * Wait for sales to complete and collect all coins
     */
    private boolean waitForSalesAndCollectCoins() {
        try {
            log.info("⏳ Waiting for sales to complete and collecting coins...");

            // Wait for some sales to complete (30-60 seconds)
            int waitTime = 45000 + (int)(Math.random() * 15000); // 45-60 seconds
            log.info("Waiting {} seconds for initial sales...", waitTime / 1000);
            Global.sleep(waitTime);

            // Collect coins from completed sales
            collectCoinsFromGE();

            // Open bank to deposit any remaining items and get all coins
            if (Rs2Bank.openBank()) {
                // Deposit any unsold items
                Rs2Bank.depositAllExcept("Coins");
                Global.sleep(600, 1200);

                // Withdraw all coins
                if (Rs2Bank.hasItem("Coins")) {
                    log.info("💰 Withdrawing all coins from bank");
                    Rs2Bank.withdrawAll("Coins");
                    Global.sleep(600, 1200);
                }

                Rs2Bank.closeBank();
            }

            int totalCoins = Rs2Inventory.count("Coins");
            log.info("✅ Collected {} total coins", totalCoins);
            return true;

        } catch (Exception e) {
            log.error("Error collecting coins: ", e);
            return false;
        }
    }

    /**
     * Collect coins from completed Grand Exchange sales
     */
    private void collectCoinsFromGE() {
        try {
            log.info("💰 Collecting coins from Grand Exchange...");

            // Open GE if not already open
            if (!isGrandExchangeOpen()) {
                if (!openGrandExchange()) {
                    log.error("❌ Could not open Grand Exchange for coin collection");
                    return;
                }
            }

            // Wait for interface to fully load
            Global.sleep(1000, 2000);

            boolean coinsCollected = false;

            // Check each slot for completed sales and collect coins
            for (int slot = 7; slot <= 14; slot++) {
                try {
                    log.debug("Checking GE slot {} for completed trades", slot);

                    // Click the slot to select it
                    if (Rs2Widget.clickWidget(465, slot)) {
                        Global.sleep(600, 1200);

                        // Try different collect button variations
                        if (Rs2Widget.clickWidget("Collect")) {
                            log.info("✅ Collected coins from slot {}", slot);
                            coinsCollected = true;
                            Global.sleep(600, 1200);
                        } else if (Rs2Widget.clickWidget("Collect to inventory")) {
                            log.info("✅ Collected to inventory from slot {}", slot);
                            coinsCollected = true;
                            Global.sleep(600, 1200);
                        } else if (Rs2Widget.clickWidget("Collect to bank")) {
                            log.info("✅ Collected to bank from slot {}", slot);
                            coinsCollected = true;
                            Global.sleep(600, 1200);
                        }
                    }
                } catch (Exception e) {
                    // Continue with other slots if one fails
                    log.debug("Could not collect from slot {}: {}", slot, e.getMessage());
                }
            }

            // Try the "Collect All" button if available
            try {
                if (Rs2Widget.clickWidget("Collect All")) {
                    log.info("✅ Used 'Collect All' button");
                    coinsCollected = true;
                    Global.sleep(1000, 2000);
                }
            } catch (Exception e) {
                log.debug("Collect All button not available: {}", e.getMessage());
            }

            if (!coinsCollected) {
                log.warn("⚠️ No coins were collected from Grand Exchange - may need manual intervention");
            }

            // Close GE interface
            if (Rs2Widget.clickWidget("Close")) {
                Global.sleep(500, 1000);
            }

            log.info("✅ Finished collecting from Grand Exchange");

        } catch (Exception e) {
            log.error("Error collecting coins from GE: ", e);
        }
    }

    /**
     * Get item name by ID
     */
    private String getItemName(int itemId) {
        try {
            return Microbot.getClientThread().runOnClientThreadOptional(
                () -> Microbot.getClient().getItemDefinition(itemId).getName()
            ).orElse("Item " + itemId);
        } catch (Exception e) {
            return "Item " + itemId;
        }
    }

    /**
     * Get item ID by name (simplified version)
     */
    private int getItemIdByName(String itemName) {
        // This would need a proper item database lookup
        // For now, return -1 for unknown items
        return -1;
    }

    /**
     * Request mule service and handle the drop trade
     */
    private void requestMuleService() {
        try {
            if (!Rs2Inventory.hasItem("Coins")) {
                log.warn("⚠️ No coins found in inventory for mule request");
            }

            // Create items list for mule request (mainly coins)
            List<MuleBridgeClient.MuleTradeItem> items = new ArrayList<>();
            if (Rs2Inventory.hasItem("Coins")) {
                int coinAmount = Rs2Inventory.itemQuantity("Coins");
                items.add(new MuleBridgeClient.MuleTradeItem(995, "Coins", coinAmount));
                log.info("💰 Preparing to mule {} coins", coinAmount);
            }

            // Add any other items in inventory
            Rs2Inventory.items().forEach(item -> {
                if (item != null && item.getId() != 995) { // Skip coins, already added
                    items.add(new MuleBridgeClient.MuleTradeItem(
                            item.getId(), item.getName(), item.getQuantity()));
                }
            });

            if (items.isEmpty()) {
                log.warn("⚠️ No gold to mule, skipping mule request");
                resetMuleProcess();
                return;
            }

            log.info("📤 Requesting mule for {} item types...", items.size());

            // Request mule WITH drop location (bridge requires non-blank location)
            MuleBridgeClient client = MuleBridgeClient.getInstance(config.bridgeUrl());
            // Bouw een geldige location string (NotBlank vereist door de bridge)
            String location;
            try {
                int lx = config.dropLocationX();
                int ly = config.dropLocationY();
                int lz = config.dropLocationZ();
                location = lx + "," + ly + "," + lz;
            } catch (Exception ex) {
                // Fallback naar GE coördinaten als parsing faalt
                location = "3164,3486,0";
            }
            if (location == null || location.isBlank()) {
                location = "3164,3486,0";
            }
            log.info("📍 Using drop location for mule request: {}", location);

            CompletableFuture<String> muleRequest = client.requestMule(config.muleAccount(), location, items);

            muleRequest.thenAccept(requestId -> {
                if (requestId != null) {
                    log.info("✅ Mule request created: {}", requestId);
                    currentState = MuleProcessState.WALKING_TO_MULE;

                    // Walk to mule location and perform drop trade
                    performDropTrade(requestId);
                } else {
                    log.error("❌ Failed to create mule request");
                    resetMuleProcess();
                }
            }).exceptionally(throwable -> {
                log.error("❌ Error requesting mule: ", throwable);
                resetMuleProcess();
                return null;
            });

        } catch (Exception e) {
            log.error("Error requesting mule service: ", e);
            resetMuleProcess();
        }
    }

    /**
     * Perform the drop trade at drop location
     */
    private void performDropTrade(String requestId) {
        try {
            WorldPoint dropLocation = config.getDropLocationWorldPoint();

            // Walk to drop location (using the configured X, Y, Z coordinates)
            log.info("🚶 Walking to drop location: X={}, Y={}, Z={}",
                config.dropLocationX(), config.dropLocationY(), config.dropLocationZ());
            Rs2Walker.walkTo(dropLocation);

            if (!Global.sleepUntil(() -> Rs2Player.getWorldLocation().distanceTo(dropLocation) <= 3, 60000)) {
                log.error("❌ Failed to reach drop location");
                resetMuleProcess();
                return;
            }

            currentState = MuleProcessState.DROPPING_ITEMS;
            log.info("📍 Arrived at drop location (X={}, Y={}, Z={}), preparing for drop trade",
                config.dropLocationX(), config.dropLocationY(), config.dropLocationZ());

            // Wait a bit for mule to arrive
            Global.sleep(5000, 10000);

            // Drop all items
            log.info("📦 Starting drop trade - dropping all inventory items");

            List<Integer> droppedItems = new ArrayList<>();

            Rs2Inventory.items().forEach(item -> {
                if (item != null) {
                    try {
                        log.info("📦 Dropping: {} x{}", item.getName(), item.getQuantity());
                        Rs2Inventory.interact(item.getId(), "Drop");
                        droppedItems.add(item.getId());
                        Global.sleep(600, 1200);
                    } catch (Exception e) {
                        log.error("Error dropping item {}: ", item.getName(), e);
                    }
                }
            });

            // Wait for drops to appear on ground and verify
            Global.sleep(2000, 3000);

            log.info("✅ Drop trade completed - dropped {} item types at location X={}, Y={}, Z={}",
                droppedItems.size(), config.dropLocationX(), config.dropLocationY(), config.dropLocationZ());
            hasDroppedThisRequest = true;

            // Notify bridge that drop is completed
            notifyMuleCompleted(requestId);

            // Return to original location
            currentState = MuleProcessState.RETURNING_HOME;
            returnToOriginalLocation();

        } catch (Exception e) {
            log.error("Error during drop trade: ", e);
            resetMuleProcess();
        }
    }

    /**
     * Notify the bridge that the mule process is completed
     */
    private void notifyMuleCompleted(String requestId) {
        try {
            // This would update the bridge about completion
            // For now, just log the completion
            log.info("✅ Mule process completed for request: {}", requestId);
        } catch (Exception e) {
            log.error("Error notifying mule completion: ", e);
        }
    }

    /**
     * Return to original location after mule process
     */
    private void returnToOriginalLocation() {
        try {
            WorldPoint returnLocation = config.getReturnWorldPoint();

            log.info("🏠 Returning to location: {}", returnLocation);
            Rs2Walker.walkTo(returnLocation);

            Global.sleepUntil(() -> Rs2Player.getWorldLocation().distanceTo(returnLocation) <= 5, 60000);

            currentState = MuleProcessState.RESUMING_BOTS;
            resumeBotsAndComplete();

        } catch (Exception e) {
            log.error("Error returning to original location: ", e);
            resumeBotsAndComplete();
        }
    }

    /**
     * Resume paused bots and complete the mule process
     */
    private void resumeBotsAndComplete() {
        try {
            // Resume paused bots
            if (!pausedBots.isEmpty()) {
                log.info("▶️ Resuming {} paused bot scripts", pausedBots.size());
                Rs2BotManager.resumeBots(pausedBots);
                pausedBots.clear();
            }

            // Release global pause flags similar to BreakHandler
            try {
                PluginPauseEvent.setPaused(false);
                Microbot.pauseAllScripts.compareAndSet(true, false);
            } catch (Exception ignored) {}

            // Reset for next mule cycle
            resetMuleProcess();
            lastMuleTime = System.currentTimeMillis();

            currentState = MuleProcessState.COMPLETED;
            log.info("🎉 Mule process completed successfully! Next mule in {} hours", config.muleIntervalHours());

            // Reset to waiting state after brief delay
            Global.sleep(2000);
            currentState = MuleProcessState.WAITING;

            // Stop script if configured
            if (config.stopAfterMule()) {
                log.info("🛑 Stopping script as configured");
                shutdown();
            }

        } catch (Exception e) {
            log.error("Error resuming bots: ", e);
        }
    }

    /**
     * Reset mule process state
     */
    private void resetMuleProcess() {
        muleRequested = false;
        itemsSold = false;
        coinsCollected = false;
        hasDroppedThisRequest = false;
        currentState = MuleProcessState.WAITING;

        // Ensure bots are resumed if something went wrong
        if (!pausedBots.isEmpty()) {
            log.info("🔄 Emergency resume of {} paused bots", pausedBots.size());
            Rs2BotManager.resumeBots(pausedBots);
            pausedBots.clear();
        }

        // Also release global pause in emergency reset
        try {
            PluginPauseEvent.setPaused(false);
            Microbot.pauseAllScripts.compareAndSet(true, false);
        } catch (Exception ignored) {}
    }

    /**
     * Helper methods
     */
    private boolean isAtGrandExchange() {
        WorldPoint playerPos = Rs2Player.getWorldLocation();
        return playerPos != null &&
               playerPos.getX() >= 3140 && playerPos.getX() <= 3190 &&
               playerPos.getY() >= 3460 && playerPos.getY() <= 3510;
    }

    private int[] getItemIdsToSell() {
        String itemIdsString = config.sellItemIds();
        if (itemIdsString == null || itemIdsString.trim().isEmpty()) {
            return new int[0];
        }

        try {
            return Arrays.stream(itemIdsString.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .mapToInt(Integer::parseInt)
                    .toArray();
        } catch (NumberFormatException e) {
            log.error("Error parsing item IDs: {}", itemIdsString, e);
            return new int[0];
        }
    }

    private String[] getItemNamesToSell() {
        String itemNamesString = config.sellItemNames();
        if (itemNamesString == null || itemNamesString.trim().isEmpty()) {
            return new String[0];
        }

        return Arrays.stream(itemNamesString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }

    public MuleProcessState getCurrentState() {
        return currentState;
    }

    /**
     * Check if client is ready without causing TimeoutExceptions
     * Uses less aggressive checking to prevent client thread overload
     */
    private boolean isClientReady() {
        try {
            // First check basic client state without widget checking
            if (Microbot.getClient() == null) {
                return false;
            }

            // Check game state directly without widgets
            net.runelite.api.GameState gameState = Microbot.getClient().getGameState();
            if (gameState != net.runelite.api.GameState.LOGGED_IN) {
                return false;
            }

            // Only do occasional widget checks to reduce client thread pressure
            // Check widgets only every 10 script cycles (50 seconds with 5-second intervals)
            if (System.currentTimeMillis() % 50000 < 5000) {
                try {
                    // Use timeout-safe widget check
                    return safeWidgetCheck();
                } catch (Exception e) {
                    // If widget check fails, assume we're still logged in based on game state
                    log.debug("Widget check failed, using game state only: {}", e.getMessage());
                    return true;
                }
            }

            // For most cycles, just rely on game state
            return true;

        } catch (Exception e) {
            log.debug("Client readiness check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Safe widget check that won't cause TimeoutExceptions
     */
    private boolean safeWidgetCheck() {
        try {
            // Use CompletableFuture with timeout to prevent hanging
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return Microbot.isLoggedIn();
                } catch (Exception e) {
                    return false;
                }
            }).completeOnTimeout(true, 2, TimeUnit.SECONDS).get();
        } catch (Exception e) {
            // If anything fails, assume logged in based on game state
            return true;
        }
    }
}
