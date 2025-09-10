package net.runelite.client.plugins.microbot.util.bank;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.microbot.util.Global;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for selling items at the Grand Exchange
 * Based on common bank seller patterns for RuneLite bots
 */
@Slf4j
public class Rs2BankSeller {

    private static final WorldPoint GRAND_EXCHANGE = new WorldPoint(3164, 3486, 0);
    private static final int GE_CLERK_ID = 2149; // Grand Exchange clerk NPC ID

    // Grand Exchange widget IDs (these may need adjustment)
    private static final int GE_INTERFACE_PARENT = 465;
    private static final int GE_SELL_BUTTON = 1;
    private static final int GE_PRICE_INPUT = 12;
    private static final int GE_QUANTITY_INPUT = 10;
    private static final int GE_ITEM_SLOT = 21;
    private static final int GE_CONFIRM_BUTTON = 27;

    /**
     * Sell specified items at Grand Exchange
     * @param itemIds Array of item IDs to sell
     * @param sellForMarketPrice If true, sells at current market price, otherwise at -5%
     * @return true if selling process completed successfully
     */
    public static boolean sellItems(int[] itemIds, boolean sellForMarketPrice) {
        try {
            log.info("Starting to sell {} item types", itemIds.length);

            // Go to Grand Exchange if not already there
            if (!isAtGrandExchange()) {
                log.info("Walking to Grand Exchange...");
                Rs2Walker.walkTo(GRAND_EXCHANGE);

                if (!Global.sleepUntil(() -> isAtGrandExchange(), 30000)) {
                    log.error("Failed to reach Grand Exchange");
                    return false;
                }
            }

            // Open bank to withdraw items if needed
            log.info("Opening bank to withdraw items to sell");
            if (!Rs2Bank.openBank()) {
                log.warn("Could not open bank at GE. Continuing if items are already in inventory");
            } else {
                for (int itemId : itemIds) {
                    try {
                        if (!Rs2Inventory.hasItem(itemId) && Rs2Bank.hasItem(itemId)) {
                            log.info("Withdrawing all of item ID {} from bank", itemId);
                            Rs2Bank.withdrawAll(itemId);
                            Global.sleep(300, 600);
                        }
                    } catch (Exception e) {
                        log.warn("Error withdrawing item {}: {}", itemId, e.getMessage());
                    }
                }
                // Close bank before opening GE
                Rs2Bank.closeBank();
                Global.sleep(300, 600);
            }

            // Open Grand Exchange
            if (!openGrandExchange()) {
                log.error("Failed to open Grand Exchange");
                return false;
            }

            // Sell each item type
            for (int itemId : itemIds) {
                if (Rs2Inventory.hasItem(itemId)) {
                    int quantity = Rs2Inventory.itemQuantity(itemId);
                    String itemName = Microbot.getClientThread().runOnClientThreadOptional(
                            () -> Microbot.getClient().getItemDefinition(itemId).getName()
                    ).orElse("Item " + itemId);
                    log.info("Selling {} x {} (ID: {})", quantity, itemName, itemId);

                    if (!sellItem(itemId, quantity, sellForMarketPrice)) {
                        log.warn("Failed to sell item ID: {}", itemId);
                    }
                } else {
                    log.debug("Item ID {} not found in inventory", itemId);
                }
            }

            // Collect coins from completed sales
            collectCoins();

            log.info("Item selling process completed");
            return true;

        } catch (Exception e) {
            log.error("Error during item selling: ", e);
            return false;
        }
    }

    /**
     * Sell items by name instead of ID
     */
    public static boolean sellItemsByName(String[] itemNames, boolean sellForMarketPrice) {
        List<Integer> itemIds = Arrays.stream(itemNames)
                .map(Rs2BankSeller::getItemIdByName)
                .filter(id -> id != -1)
                .collect(Collectors.toList());

        return sellItems(itemIds.stream().mapToInt(i -> i).toArray(), sellForMarketPrice);
    }

    /**
     * Check if player is at Grand Exchange area
     */
    private static boolean isAtGrandExchange() {
        WorldPoint playerPos = Rs2Player.getWorldLocation();
        return playerPos != null &&
               playerPos.getX() >= 3140 && playerPos.getX() <= 3190 &&
               playerPos.getY() >= 3460 && playerPos.getY() <= 3510;
    }

    /**
     * Open Grand Exchange interface
     */
    private static boolean openGrandExchange() {
        try {
            // Try to interact with Grand Exchange clerk or booth
            if (Rs2GameObject.interact("Grand Exchange booth", "Exchange")) {
                return Global.sleepUntil(() -> isGrandExchangeOpen(), 5000);
            }

            // Alternative: try NPC
            if (net.runelite.client.plugins.microbot.util.npc.Rs2Npc.interact(GE_CLERK_ID, "Exchange")) {
                return Global.sleepUntil(() -> isGrandExchangeOpen(), 5000);
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
    private static boolean isGrandExchangeOpen() {
        Widget geInterface = Rs2Widget.getWidget(GE_INTERFACE_PARENT, 0);
        return geInterface != null && !geInterface.isHidden();
    }

    /**
     * Sell a specific item
     */
    private static boolean sellItem(int itemId, int quantity, boolean sellForMarketPrice) {
        try {
            // Find available GE slot
            int slot = findAvailableGESlot();
            if (slot == -1) {
                log.warn("No available GE slots");
                return false;
            }

            // Click on sell offer slot
            Widget sellSlot = Rs2Widget.getWidget(GE_INTERFACE_PARENT, slot);
            if (sellSlot != null) {
                Rs2Widget.clickWidget(sellSlot);
                Global.sleep(600);
            }

            // Click item in inventory to select it
            if (!Rs2Inventory.interact(itemId, "Sell")) {
                log.warn("Failed to select item for selling: {}", itemId);
                return false;
            }

            Global.sleep(1000);

            // Set quantity (default is usually fine for "All")
            setGEQuantity(quantity);

            // Set price (market price or 5% below)
            if (sellForMarketPrice) {
                // Click guide price to use market price
                clickGEGuidePrice();
            } else {
                // Set price to 95% of guide price
                setPriceBelow5Percent();
            }

            // Confirm the offer
            confirmGEOffer();

            Global.sleep(1500);
            log.info("Successfully created sell offer for {} x {}", quantity, itemId);
            return true;

        } catch (Exception e) {
            log.error("Error selling item {}: ", itemId, e);
            return false;
        }
    }

    private static int findAvailableGESlot() {
        // Check each of the 8 GE slots (typically widgets 7-14)
        for (int i = 7; i <= 14; i++) {
            Widget slot = Rs2Widget.getWidget(GE_INTERFACE_PARENT, i);
            if (slot != null && slot.getText() == null || slot.getText().isEmpty()) {
                return i;
            }
        }
        return -1; // No available slots
    }

    private static void setGEQuantity(int quantity) {
        try {
            Widget quantityWidget = Rs2Widget.getWidget(GE_INTERFACE_PARENT, GE_QUANTITY_INPUT);
            if (quantityWidget != null) {
                // For selling all items of a type, usually clicking "All" button or max quantity
                Widget allButton = Rs2Widget.getWidget(GE_INTERFACE_PARENT, 31); // "All" button
                if (allButton != null) {
                    Rs2Widget.clickWidget(allButton);
                }
            }
        } catch (Exception e) {
            log.warn("Error setting GE quantity: ", e);
        }
    }

    private static void clickGEGuidePrice() {
        try {
            // Click the guide price to use market price
            Widget guidePriceWidget = Rs2Widget.getWidget(GE_INTERFACE_PARENT, 25);
            if (guidePriceWidget != null) {
                Rs2Widget.clickWidget(guidePriceWidget);
            }
        } catch (Exception e) {
            log.warn("Error clicking guide price: ", e);
        }
    }

    private static void setPriceBelow5Percent() {
        try {
            // Click -5% button or similar
            Widget minusFivePercent = Rs2Widget.getWidget(GE_INTERFACE_PARENT, 23);
            if (minusFivePercent != null) {
                Rs2Widget.clickWidget(minusFivePercent);
            }
        } catch (Exception e) {
            log.warn("Error setting price below 5%: ", e);
        }
    }

    private static void confirmGEOffer() {
        try {
            Widget confirmButton = Rs2Widget.getWidget(GE_INTERFACE_PARENT, GE_CONFIRM_BUTTON);
            if (confirmButton != null) {
                Rs2Widget.clickWidget(confirmButton);
            }
        } catch (Exception e) {
            log.warn("Error confirming GE offer: ", e);
        }
    }

    /**
     * Collect coins from completed GE sales
     */
    private static void collectCoins() {
        try {
            log.info("Collecting coins from completed sales...");

            // Check each GE slot for completed sales
            for (int i = 7; i <= 14; i++) {
                Widget slot = Rs2Widget.getWidget(GE_INTERFACE_PARENT, i);
                if (slot != null && slot.getText() != null && slot.getText().contains("Completed")) {
                    // Click on the slot to collect
                    Rs2Widget.clickWidget(slot);
                    Global.sleep(300);

                    // Click collect button
                    Widget collectButton = Rs2Widget.getWidget(GE_INTERFACE_PARENT, 30);
                    if (collectButton != null) {
                        Rs2Widget.clickWidget(collectButton);
                        Global.sleep(600);
                    }
                }
            }

        } catch (Exception e) {
            log.warn("Error collecting coins: ", e);
        }
    }

    /**
     * Get item ID by name (simplified implementation)
     */
    private static int getItemIdByName(String itemName) {
        // This would need to be implemented with proper item database lookup
        // For now, return -1 as placeholder
        log.warn("Item name lookup not implemented yet: {}", itemName);
        return -1;
    }

    /**
     * Parse item IDs from comma-separated string
     */
    public static int[] parseItemIds(String itemIdsString) {
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

    /**
     * Parse item names from comma-separated string
     */
    public static String[] parseItemNames(String itemNamesString) {
        if (itemNamesString == null || itemNamesString.trim().isEmpty()) {
            return new String[0];
        }

        return Arrays.stream(itemNamesString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }
}
