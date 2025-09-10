package net.runelite.client.plugins.microbot.util.mule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * HTTP client for communicating with the Mule Bridge
 * Provides simple API for bot scripts to request mule services
 */
@Slf4j
public class MuleBridgeClient {

    private static final String DEFAULT_BRIDGE_URL = "http://localhost:8080";
    // Maintain instances per bridge URL to avoid stale singleton URL issues
    private static final java.util.concurrent.ConcurrentHashMap<String, MuleBridgeClient> INSTANCES = new java.util.concurrent.ConcurrentHashMap<>();

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String bridgeUrl;
    private final ConcurrentHashMap<String, Consumer<MuleRequestStatus>> statusCallbacks;
    private WebSocket webSocket;

    private MuleBridgeClient(String bridgeUrl) {
        this.bridgeUrl = bridgeUrl != null ? bridgeUrl : DEFAULT_BRIDGE_URL;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
        this.statusCallbacks = new ConcurrentHashMap<>();

        // Initialize WebSocket connection for status updates
        initializeWebSocket();
    }

    /**
     * Get singleton instance
     */
    public static MuleBridgeClient getInstance() {
        return getInstance(DEFAULT_BRIDGE_URL);
    }

    /**
     * Get singleton instance with custom bridge URL
     */
    public static MuleBridgeClient getInstance(String bridgeUrl) {
        final String key = bridgeUrl == null ? DEFAULT_BRIDGE_URL : bridgeUrl;
        return INSTANCES.computeIfAbsent(key, MuleBridgeClient::new);
    }

    /**
     * Request mule services - simple API for bot scripts
     *
     * @param location Trading location (e.g., "Grand Exchange", "3164,3486,0")
     * @param items List of items to trade
     * @return CompletableFuture with request ID
     */
    public CompletableFuture<String> requestMule(String location, List<MuleTradeItem> items) {
        return requestMule("default_mule", location, items);
    }

    /**
     * Request mule services with specific mule account
     *
     * @param muleAccount Specific mule account to use
     * @param location Trading location
     * @param items List of items to trade
     * @return CompletableFuture with request ID
     */
    public CompletableFuture<String> requestMule(String muleAccount, String location, List<MuleTradeItem> items) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get current player name (requester)
                String requesterUsername = getCurrentPlayerName();

                // Convert MuleTradeItem objects to TradeItem objects for the bridge
                List<TradeItem> tradeItems = items.stream()
                        .map(MuleTradeItem::toTradeItem)
                        .collect(java.util.stream.Collectors.toList());

                // Build request JSON
                MuleRequestData requestData = new MuleRequestData();
                requestData.requesterUsername = requesterUsername;
                requestData.muleAccount = muleAccount;
                requestData.location = location;
                requestData.items = tradeItems; // Now using converted TradeItem objects

                String requestBody = objectMapper.writeValueAsString(requestData);

                log.info("Sending mule request: requester={}, muleAccount={}, location={}, items={}",
                        requesterUsername, muleAccount, location, tradeItems.size());

                // Send HTTP request
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(bridgeUrl + "/api/mule/request"))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonNode jsonResponse = objectMapper.readTree(response.body());
                    if (jsonResponse.get("success").asBoolean()) {
                        String requestId = jsonResponse.get("requestId").asText();
                        log.info("Mule request created successfully: {}", requestId);
                        return requestId;
                    } else {
                        String message = jsonResponse.get("message").asText();
                        throw new RuntimeException("Request failed: " + message);
                    }
                } else {
                    throw new RuntimeException("HTTP error: " + response.statusCode());
                }

            } catch (Exception e) {
                log.error("Error requesting mule services", e);
                throw new RuntimeException("Failed to request mule: " + e.getMessage());
            }
        });
    }

    /**
     * Check status of a mule request
     *
     * @param requestId Request ID to check
     * @return CompletableFuture with current status
     */
    public CompletableFuture<MuleRequestStatus> checkRequestStatus(String requestId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(bridgeUrl + "/api/mule/request/" + requestId + "/status"))
                        .GET()
                        .timeout(Duration.ofSeconds(10))
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonNode jsonResponse = objectMapper.readTree(response.body());
                    if (jsonResponse.get("success").asBoolean()) {
                        MuleRequestStatus status = new MuleRequestStatus();
                        status.requestId = jsonResponse.get("requestId").asText();
                        status.status = jsonResponse.get("status").asText();
                        status.currentStep = jsonResponse.get("currentStep").asText();
                        status.timestamp = jsonResponse.get("timestamp").asText();
                        return status;
                    } else {
                        throw new RuntimeException("Status check failed: " +
                                jsonResponse.get("message").asText());
                    }
                } else if (response.statusCode() == 404) {
                    throw new RuntimeException("Request not found");
                } else {
                    throw new RuntimeException("HTTP error: " + response.statusCode());
                }

            } catch (Exception e) {
                log.error("Error checking request status", e);
                throw new RuntimeException("Failed to check status: " + e.getMessage());
            }
        });
    }

    /**
     * Subscribe to real-time status updates for a request
     *
     * @param requestId Request ID to monitor
     * @param callback Callback function for status updates
     */
    public void subscribeToUpdates(String requestId, Consumer<MuleRequestStatus> callback) {
        statusCallbacks.put(requestId, callback);
        log.info("Subscribed to updates for request: {}", requestId);
    }

    /**
     * Unsubscribe from status updates
     *
     * @param requestId Request ID to stop monitoring
     */
    public void unsubscribeFromUpdates(String requestId) {
        statusCallbacks.remove(requestId);
        log.info("Unsubscribed from updates for request: {}", requestId);
    }

    /**
     * Check if bridge is online
     *
     * @return CompletableFuture<Boolean> indicating bridge availability
     */
    public CompletableFuture<Boolean> isBridgeOnline() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(bridgeUrl + "/api/mule/health"))
                        .GET()
                        .timeout(Duration.ofSeconds(5))
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                return response.statusCode() == 200;

            } catch (Exception e) {
                log.debug("Bridge health check failed: {}", e.getMessage());
                return false;
            }
        });
    }

    /**
     * Convenience method: Request mule when inventory is full
     *
     * @param location Trading location
     * @return CompletableFuture with request ID, or null if inventory not full
     */
    public CompletableFuture<String> requestMuleForFullInventory(String location) {
        // Check if inventory is full
        if (!isInventoryFull()) {
            return CompletableFuture.completedFuture(null);
        }

        // Get all tradeable items from inventory
        List<MuleTradeItem> items = getTradeableInventoryItems();

        if (items.isEmpty()) {
            log.warn("Inventory full but no tradeable items found");
            return CompletableFuture.completedFuture(null);
        }

        log.info("Requesting mule for full inventory ({} items)", items.size());
        return requestMule(location, items);
    }

    private void initializeWebSocket() {
        try {
            String wsUrl = bridgeUrl.replace("http://", "ws://") + "/ws/updates";

            webSocket = HttpClient.newHttpClient()
                    .newWebSocketBuilder()
                    .buildAsync(URI.create(wsUrl), new WebSocket.Listener() {
                        @Override
                        public void onOpen(WebSocket webSocket) {
                            log.info("WebSocket connected to bridge");
                            WebSocket.Listener.super.onOpen(webSocket);
                        }

                        @Override
                        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                            handleWebSocketMessage(data.toString());
                            return WebSocket.Listener.super.onText(webSocket, data, last);
                        }

                        @Override
                        public void onError(WebSocket webSocket, Throwable error) {
                            log.warn("WebSocket error: {}", error.getMessage());
                            WebSocket.Listener.super.onError(webSocket, error);
                        }
                    })
                    .get();

        } catch (Exception e) {
            log.warn("Failed to initialize WebSocket connection: {}", e.getMessage());
        }
    }

    private void handleWebSocketMessage(String message) {
        try {
            JsonNode update = objectMapper.readTree(message);
            String requestId = update.get("id").asText();

            Consumer<MuleRequestStatus> callback = statusCallbacks.get(requestId);
            if (callback != null) {
                MuleRequestStatus status = new MuleRequestStatus();
                status.requestId = requestId;
                status.status = update.get("status").asText();
                status.currentStep = update.get("currentStep").asText();

                callback.accept(status);
            }

        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
        }
    }

    private String getCurrentPlayerName() {
        // This would integrate with Rs2Player utility
        try {
            var local = Rs2Player.getLocalPlayer();
            if (local != null) {
                String name = local.getName();
                if (name != null && !name.isBlank()) {
                    return name;
                }
            }
            log.warn("Local player not available or name is null/blank, using default");
            return "unknown_player";
        } catch (Exception e) {
            log.warn("Could not get current player name, using default");
            return "unknown_player";
        }
    }

    private boolean isInventoryFull() {
        // This would integrate with Rs2Inventory utility
        try {
            return net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory.isFull();
        } catch (Exception e) {
            log.warn("Could not check inventory status");
            return false;
        }
    }

    private List<MuleTradeItem> getTradeableInventoryItems() {
        // This would integrate with Rs2Inventory to get actual items
        // For now, return empty list as placeholder
        return java.util.Collections.emptyList();
    }

    // Data classes for requests - Updated to match bridge server format
    private static class MuleRequestData {
        public String requesterUsername;
        public String muleAccount;
        public String location;
        public List<TradeItem> items;  // Changed from MuleTradeItem to TradeItem to match bridge
    }

    public static class TradeItem {  // Renamed and updated to match bridge model
        public int itemId;
        public String itemName;
        public int quantity;

        public TradeItem() {}

        public TradeItem(int itemId, String itemName, int quantity) {
            this.itemId = itemId;
            this.itemName = itemName;
            this.quantity = quantity;
        }
    }

    public static class MuleTradeItem {
        public int itemId;
        public String itemName;
        public int quantity;

        public MuleTradeItem() {}

        public MuleTradeItem(int itemId, String itemName, int quantity) {
            this.itemId = itemId;
            this.itemName = itemName;
            this.quantity = quantity;
        }

        // Convert to bridge format
        public TradeItem toTradeItem() {
            return new TradeItem(itemId, itemName, quantity);
        }
    }

    public static class MuleRequestStatus {
        public String requestId;
        public String status;
        public String currentStep;
        public String timestamp;

        public boolean isCompleted() {
            return "COMPLETED".equals(status);
        }

        public boolean isFailed() {
            return "FAILED".equals(status);
        }

        public boolean isProcessing() {
            return "PROCESSING".equals(status);
        }

        public boolean isQueued() {
            return "QUEUED".equals(status);
        }
    }
}
