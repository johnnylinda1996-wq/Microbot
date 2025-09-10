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
import net.runelite.client.plugins.microbot.util.bank.Rs2BankSeller;
import net.runelite.client.plugins.microbot.util.botmanager.Rs2BotManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MuleTestScript extends Script {

    public static String version = "1.0.0";
    private MuleTestConfig config;
    private boolean muleRequested = false;
    private List<String> pausedBots = new ArrayList<>();
    private boolean itemsSold = false;
    private long scriptStartTime = 0;
    private long lastMuleTime = 0;
    private boolean hasDroppedThisRequest = false;

    public boolean run(MuleTestConfig config) {
        this.config = config;
        this.scriptStartTime = System.currentTimeMillis();
        this.lastMuleTime = System.currentTimeMillis();

        // Pure time-interval mode
        log.info("MuleTest script started (DROP-TRADE ONLY) with TIME INTERVAL: {} hours",
                config.muleIntervalHours());

        // Register this script with the bot manager
        Rs2BotManager.registerScript("MuleTest", this);

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn() || Microbot.getClient().getGameState() != net.runelite.api.GameState.LOGGED_IN) {
                    return;
                }

                if (!super.run()) return;

                // Always time-based mule request
                checkTimeForMuleRequest();

            } catch (Exception ex) {
                log.error("Error in mule test script: ", ex);
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return true;
    }

    /**
     * Check if it's time to request a mule based on the configured time interval
     */
    private void checkTimeForMuleRequest() {
        try {
            long currentTime = System.currentTimeMillis();
            long timeSinceLastMule = currentTime - lastMuleTime;

            // Convert to hours (with decimals)
            double hoursSinceLastMule = timeSinceLastMule / (1000.0 * 60.0 * 60.0);

            // Log every minute for debugging
            if (timeSinceLastMule % 60000 < 1000) { // Every ~60 seconds
                String hoursFmt = String.format("%.2f", hoursSinceLastMule);
                log.info("Time since last mule: {} hours / {} hours needed", hoursFmt, config.muleIntervalHours());
            }

            if (hoursSinceLastMule >= config.muleIntervalHours() && !muleRequested) {
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
            // Step 1: Always pause other bots during mule process
            pausedBots = Rs2BotManager.pauseAllBots(this);
            log.info("⏸️ Paused {} other bot scripts", pausedBots.size());

            // Step 2: Sell items if enabled
            if (config.sellItemsFirst() && !itemsSold) {
                if (sellConfiguredItems()) {
                    itemsSold = true;
                    log.info("✅ Items sold successfully, proceeding with mule request...");
                } else {
                    log.warn("⚠️ Item selling failed, proceeding with current GP amount");
                }
            }

            // Step 3: Request mule with updated GP amount
            int currentGp = Rs2Inventory.itemQuantity(995);
            if (currentGp <= 0) {
                log.warn("No GP to mule. Skipping mule request and resetting timer");
                lastMuleTime = System.currentTimeMillis();
                resetMuleProcess();
                return;
            }

            requestMule(currentGp);
            lastMuleTime = System.currentTimeMillis(); // Update last mule time

        } catch (Exception e) {
            log.error("Error starting mule process: ", e);
            resetMuleProcess(); // Resume paused bots on error
        }
    }

    /**
     * Sell configured items at Grand Exchange
     */
    private boolean sellConfiguredItems() {
        try {
            log.info("🏪 Starting item selling process...");

            // Parse item IDs and names from config
            int[] itemIds = Rs2BankSeller.parseItemIds(config.sellItemIds());
            String[] itemNames = Rs2BankSeller.parseItemNames(config.sellItemNames());

            // Check if we have any items to sell
            if (itemIds.length == 0 && itemNames.length == 0) {
                log.warn("No items configured for selling");
                return true; // Not a failure, just nothing to sell
            }

            // Sell items by ID first
            boolean success = true;
            if (itemIds.length > 0) {
                log.info("Selling {} item types by ID: {}", itemIds.length, java.util.Arrays.toString(itemIds));
                success = Rs2BankSeller.sellItems(itemIds, true); // Use market price
            }

            // Sell items by name
            if (itemNames.length > 0 && success) {
                log.info("Selling {} item types by name: {}", itemNames.length, java.util.Arrays.toString(itemNames));
                success = Rs2BankSeller.sellItemsByName(itemNames, true);
            }

            if (success) {
                log.info("Item selling completed successfully");
                // Wait a bit for sales to complete and coins to be collected
                sleep(5000);
            } else {
                log.error("Item selling failed");
            }

            return success;

        } catch (Exception e) {
            log.error("Error during item selling: ", e);
            return false;
        }
    }

    private void requestMule(int gpAmount) {
        try {
            // Force DROP TRADE by appending marker to location
            String location = config.muleLocation().toString() + "_DROP_TRADE";
            log.info("📦 Using DROP TRADE mode at {} for {} GP", location, gpAmount);

            if (config.autoWalkToMule()) {
                // Walk to mule location first
                walkToMuleLocation();
            }

            // Create mule request with ONLY GP - no other items
            List<MuleBridgeClient.MuleTradeItem> items = new ArrayList<>();

            // Add ONLY GP (coins) as trade item
            items.add(new MuleBridgeClient.MuleTradeItem(995, "Coins", gpAmount));
            log.info("Adding {} GP to mule request", gpAmount);

            log.info("Requesting mule with {} items (GP only): {}", items.size(),
                    items.stream().map(item -> item.itemName + "(" + item.quantity + ")").toArray());

            // Get the bridge client with the correct URL from config
            MuleBridgeClient bridgeClient = MuleBridgeClient.getInstance(config.bridgeUrl());

            // Make the request directly using the bridge client
            CompletableFuture<String> requestFuture = bridgeClient.requestMule(
                config.muleAccount(), location, items);

            requestFuture.thenAccept(requestId -> {
                if (requestId != null) {
                    log.info("Mule request created successfully: {}", requestId);

                    // Subscribe to status updates
                    bridgeClient.subscribeToUpdates(requestId, status -> {
                        log.info("Mule Status Update: {} - {}", status.status, status.currentStep);

                        // Handle different status updates
                        switch (status.status) {
                            case "QUEUED":
                                log.info("Mule request queued, waiting for mule bot...");
                                break;
                            case "PROCESSING":
                                log.info("Mule bot is processing request - {} ", status.currentStep);
                                if ("WAITING_FOR_DROP".equals(status.currentStep) && !hasDroppedThisRequest) {
                                    log.info("Mule is waiting for drop trade - dropping items now!");
                                    performDropTrade();
                                }
                                break;
                            case "LOGIN":
                                log.info("Mule bot is logging in...");
                                break;
                            case "WALKING":
                                log.info("Mule bot is walking to location...");
                                break;
                            case "WAITING_FOR_TRADE":
                                // Always handle as drop-trade
                                if (!hasDroppedThisRequest) {
                                    log.info("Mule bot is ready for drop trade - dropping items!");
                                    performDropTrade();
                                }
                                break;
                        }

                        // Check if completed - ONLY resume bots after actual completion
                        if (status.isCompleted() || status.isFailed()) {
                            bridgeClient.unsubscribeFromUpdates(requestId);

                            boolean success = status.isCompleted();
                            if (success) {
                                log.info("✅ Mule trade COMPLETED successfully! GP has been transferred to mule.");
                                log.info("🔄 Now resuming paused bot scripts...");
                                resetMuleProcess(); // Resume paused bots ONLY after successful completion

                                if (config.stopAfterMule()) {
                                    log.info("Stopping script as configured");
                                    shutdown();
                                }
                            } else {
                                log.error("❌ Mule trade FAILED!");
                                log.info("🔄 Resuming paused bot scripts due to failure...");
                                resetMuleProcess(); // Resume paused bots on failure too
                            }
                        }
                    });

                } else {
                    log.error("Failed to create mule request - resuming paused bots");
                    resetMuleProcess(); // Resume paused bots on request failure
                }
            }).exceptionally(throwable -> {
                log.error("Error creating mule request - resuming paused bots", throwable);
                resetMuleProcess(); // Resume paused bots on error
                return null;
            });

            muleRequested = true;
            log.info("🚀 Mule request sent successfully - bots remain PAUSED until trade completion");

        } catch (Exception e) {
            log.error("Error requesting mule - resuming paused bots: ", e);
            resetMuleProcess(); // Resume paused bots on error
        }
    }

    /**
     * Perform drop trade by dropping GP at the current location
     */
    private void performDropTrade() {
        try {
            log.info("Performing drop trade - dropping GP at location");

            // Get current GP amount
            int gpAmount = Rs2Inventory.itemQuantity(995);
            if (gpAmount <= 0) {
                log.warn("No GP to drop!");
                return;
            }

            // Drop the GP
            if (Rs2Inventory.hasItem(995)) {
                log.info("Dropping {} GP at location", gpAmount);
                Rs2Inventory.interact(995, "Drop");
                sleep(1000);
                log.info("GP dropped successfully - mule should pick it up");
                hasDroppedThisRequest = true;
            } else {
                log.warn("Could not find GP in inventory to drop");
            }

        } catch (Exception e) {
            log.error("Error during drop trade: ", e);
        }
    }

    private void walkToMuleLocation() {
        try {
            WorldPoint destination = getMuleLocationWorldPoint();
            if (destination == null) {
                log.error("Unknown mule location: {}", config.muleLocation());
                return;
            }

            log.info("🚶 Walking to mule location: {} at {}", config.muleLocation(), destination);

            // Use Rs2Walker to walk to the location
            Rs2Walker.walkTo(destination);

            // Wait until we're close to the destination
            sleep(2000, 5000);

            while (Rs2Player.getWorldLocation().distanceTo(destination) > 5 && isRunning()) {
                sleep(1000);
                log.info("Walking to mule location... Distance: {}", Rs2Player.getWorldLocation().distanceTo(destination));
            }

            log.info("✅ Arrived at mule location!");

        } catch (Exception e) {
            log.error("Error walking to mule location: ", e);
        }
    }

    private WorldPoint getMuleLocationWorldPoint() {
        switch (config.muleLocation()) {
            case GRAND_EXCHANGE:
                return new WorldPoint(3164, 3486, 0);
            case VARROCK_WEST_BANK:
                return new WorldPoint(3185, 3436, 0);
            case LUMBRIDGE:
                return new WorldPoint(3222, 3218, 0);
            case FALADOR:
                return new WorldPoint(2964, 3378, 0);
            default:
                return null;
        }
    }

    /**
     * Reset the mule process and resume paused bots
     */
    private void resetMuleProcess() {
        muleRequested = false;
        itemsSold = false;
        hasDroppedThisRequest = false;

        // Resume paused bots
        if (!pausedBots.isEmpty()) {
            Rs2BotManager.resumeBots(pausedBots);
            log.info("▶️ Resumed {} bot scripts after mule completion", pausedBots.size());
            pausedBots.clear();
        }

        log.info("Mule process completed, bots resumed");
    }

    @Override
    public void shutdown() {
        // Unregister from bot manager
        Rs2BotManager.unregisterScript("MuleTest");

        // Resume any paused bots before shutting down
        if (!pausedBots.isEmpty()) {
            Rs2BotManager.resumeBots(pausedBots);
            log.info("Resumed {} bot scripts during shutdown", pausedBots.size());
        }

        super.shutdown();
        log.info("Mule test script stopped");
    }
}
