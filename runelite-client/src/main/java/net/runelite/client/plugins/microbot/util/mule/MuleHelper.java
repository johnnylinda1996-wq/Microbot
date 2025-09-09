package net.runelite.client.plugins.microbot.util.mule;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Helper utilities for integrating mule services into existing bot scripts
 */
@Slf4j
public class MuleHelper {

    private static final MuleBridgeClient client = MuleBridgeClient.getInstance();

    /**
     * Simple mule request when inventory is full
     * Automatically detects inventory items and requests mule at Grand Exchange
     *
     * @return CompletableFuture<Boolean> indicating success/failure
     */
    public static CompletableFuture<Boolean> requestMuleWhenFull() {
        return requestMuleWhenFull("Grand Exchange");
    }

    /**
     * Simple mule request when inventory is full at specific location
     *
     * @param location Trading location
     * @return CompletableFuture<Boolean> indicating success/failure
     */
    public static CompletableFuture<Boolean> requestMuleWhenFull(String location) {
        if (!Rs2Inventory.isFull()) {
            log.debug("Inventory not full, skipping mule request");
            return CompletableFuture.completedFuture(false);
        }

        List<MuleBridgeClient.MuleTradeItem> items = getInventoryItems();
        if (items.isEmpty()) {
            log.warn("Inventory full but no tradeable items found");
            return CompletableFuture.completedFuture(false);
        }

        return client.requestMule(location, items)
                .thenApply(requestId -> {
                    if (requestId != null) {
                        log.info("Mule requested successfully: {}", requestId);
                        return true;
                    } else {
                        log.error("Failed to request mule");
                        return false;
                    }
                })
                .exceptionally(throwable -> {
                    log.error("Error requesting mule", throwable);
                    return false;
                });
    }

    /**
     * Request mule with automatic inventory detection and status monitoring
     *
     * @param location Trading location
     * @param onStatusUpdate Callback for status updates
     * @param onComplete Callback when trade is completed
     * @return CompletableFuture<Boolean> indicating initial request success
     */
    public static CompletableFuture<Boolean> requestMuleWithMonitoring(
            String location,
            Consumer<MuleBridgeClient.MuleRequestStatus> onStatusUpdate,
            Consumer<Boolean> onComplete) {

        if (!Rs2Inventory.isFull()) {
            log.debug("Inventory not full, skipping mule request");
            return CompletableFuture.completedFuture(false);
        }

        List<MuleBridgeClient.MuleTradeItem> items = getInventoryItems();
        if (items.isEmpty()) {
            log.warn("Inventory full but no tradeable items found");
            return CompletableFuture.completedFuture(false);
        }

        return client.requestMule(location, items)
                .thenApply(requestId -> {
                    if (requestId != null) {
                        log.info("Mule requested successfully: {}", requestId);

                        // Subscribe to status updates
                        client.subscribeToUpdates(requestId, status -> {
                            if (onStatusUpdate != null) {
                                onStatusUpdate.accept(status);
                            }

                            // Check if completed
                            if (status.isCompleted() || status.isFailed()) {
                                client.unsubscribeFromUpdates(requestId);
                                if (onComplete != null) {
                                    onComplete.accept(status.isCompleted());
                                }
                            }
                        });

                        return true;
                    } else {
                        log.error("Failed to request mule");
                        return false;
                    }
                })
                .exceptionally(throwable -> {
                    log.error("Error requesting mule", throwable);
                    return false;
                });
    }

    /**
     * Check if mule bridge is available
     *
     * @return CompletableFuture<Boolean> indicating bridge availability
     */
    public static CompletableFuture<Boolean> isMuleBridgeOnline() {
        return client.isBridgeOnline();
    }

    /**
     * Wait for a mule request to complete
     * Blocks current thread until completion
     *
     * @param requestId Request ID to wait for
     * @param timeoutMinutes Maximum time to wait in minutes
     * @return true if completed successfully, false if failed or timed out
     */
    public static boolean waitForMuleCompletion(String requestId, int timeoutMinutes) {
        long startTime = System.currentTimeMillis();
        long timeoutMs = timeoutMinutes * 60 * 1000;

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                MuleBridgeClient.MuleRequestStatus status = client.checkRequestStatus(requestId).get();

                if (status.isCompleted()) {
                    log.info("Mule request completed successfully: {}", requestId);
                    return true;
                } else if (status.isFailed()) {
                    log.warn("Mule request failed: {}", requestId);
                    return false;
                }

                // Wait before next check
                Thread.sleep(5000); // 5 seconds

            } catch (Exception e) {
                log.error("Error checking mule status", e);
                return false;
            }
        }

        log.warn("Mule request timed out: {}", requestId);
        return false;
    }

    /**
     * Get tradeable items from inventory
     * Filters out untradeable items like quest items, tools, etc.
     */
    private static List<MuleBridgeClient.MuleTradeItem> getInventoryItems() {
        List<MuleBridgeClient.MuleTradeItem> tradeItems = new ArrayList<>();

        try {
            // Iterate over Rs2Inventory items (Rs2ItemModel stream)
            Rs2Inventory.items().forEach(item -> {
                if (item != null && isTradeableItem(item.getId())) {
                    String itemName = item.getName();
                    int quantity = item.getQuantity();

                    tradeItems.add(new MuleBridgeClient.MuleTradeItem(
                            item.getId(), itemName, quantity));
                }
            });

        } catch (Exception e) {
            log.error("Error getting inventory items", e);
        }

        return tradeItems;
    }

    /**
     * Check if an item is tradeable
     * This is a simplified version - in a real implementation,
     * you'd want a comprehensive list or API call
     */
    private static boolean isTradeableItem(int itemId) {
        // Common untradeable items (simplified list)
        int[] untradeableItems = {
                // Tools
                1265, 1267, 1269, 1271, 1273, 1275, 1277, 1279, // Pickaxes
                1349, 1351, 1353, 1355, 1357, 1359, 1361, // Axes
                303, 305, 307, 309, 311, 314, 13433, // Fishing equipment

                // Quest items (examples)
                1856, 1857, 1858, // Anti-dragon shields

                // Other common untradeable
                6570, // Fire cape
                11773, // Barrows gloves
        };

        for (int untradeable : untradeableItems) {
            if (itemId == untradeable) {
                return false;
            }
        }

        return true;
    }

    /**
     * Integration example for existing bot scripts
     * Shows how to use mule services in a typical bot loop
     */
    public static class BotIntegrationExample {

        public static void handleInventoryFull(String currentActivity) {
            log.info("Inventory full during {}, requesting mule...", currentActivity);

            MuleHelper.requestMuleWithMonitoring(
                    "Grand Exchange",
                    status -> {
                        log.info("Mule status update: {} - {}", status.status, status.currentStep);
                    },
                    success -> {
                        if (success) {
                            log.info("Mule trade completed, resuming {}", currentActivity);
                        } else {
                            log.error("Mule trade failed, stopping bot");
                            // Handle failure (maybe stop bot, retry, etc.)
                        }
                    }
            );

            // Bot can continue with other activities while waiting
            // The callbacks will handle the mule completion
        }

        public static boolean waitForInventorySpace(int timeoutMinutes) {
            // Alternative approach: block until mule completes
            if (Rs2Inventory.isFull()) {
                CompletableFuture<String> muleRequest = MuleBridgeClient.getInstance()
                        .requestMule("Grand Exchange", MuleHelper.getInventoryItems());

                try {
                    String requestId = muleRequest.get();
                    if (requestId != null) {
                        return MuleHelper.waitForMuleCompletion(requestId, timeoutMinutes);
                    }
                } catch (Exception e) {
                    log.error("Error waiting for mule", e);
                }
            }

            return false;
        }
    }
}
