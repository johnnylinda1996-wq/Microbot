package net.runelite.client.plugins.microbot.mule;

import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.Global;
import net.runelite.client.plugins.microbot.util.security.Login;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.api.GameState;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import lombok.extern.slf4j.Slf4j;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

@Slf4j
public class MuleScript extends Script {
    public static double version = 1.0;

    private MuleConfig config;
    private HttpClient httpClient;
    private ObjectMapper objectMapper;
    private MuleState currentState = MuleState.WAITING;
    private MuleRequest currentRequest = null;
    private long lastPollTime = 0;
    private long requestStartTime = 0;

    public enum MuleState {
        WAITING,
        LOGGING_IN,
        WALKING,
        TRADING,
        LOGGING_OUT,
        ERROR
    }

    public boolean run(MuleConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();

        log.info("Mule script started - waiting for requests");

        // Use scheduled executor like other Microbot scripts instead of blocking while loop
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                // Skip if not logged in when needed
                if (currentState != MuleState.WAITING && !Microbot.isLoggedIn() &&
                    Microbot.getClient().getGameState() != GameState.LOGGED_IN) {
                    return;
                }

                if (!super.run()) return;

                // Execute state machine
                switch (currentState) {
                    case WAITING:
                        handleWaitingState();
                        break;
                    case LOGGING_IN:
                        handleLoggingInState();
                        break;
                    case WALKING:
                        handleWalkingState();
                        break;
                    case TRADING:
                        handleTradingState();
                        break;
                    case LOGGING_OUT:
                        handleLoggingOutState();
                        break;
                    case ERROR:
                        handleErrorState();
                        break;
                }

            } catch (Exception e) {
                log.error("Error in mule script: ", e);
                currentState = MuleState.ERROR;
                updateRequestStatus("FAILED", "Error: " + e.getMessage());
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);

        return true;
    }

    private void handleWaitingState() {
        // Poll for new requests every configured interval (reduced for faster response)
        long currentTime = System.currentTimeMillis();
        long pollIntervalMs = Math.min((long) config.pollInterval() * 1000, 1000); // Cap at 1 second max
        if (currentTime - lastPollTime >= pollIntervalMs) {
            log.info("Poll interval reached, last poll was {} ms ago", (currentTime - lastPollTime));
            pollForNewRequest();
            lastPollTime = currentTime;
        }
    }

    private void handleLoggingInState() {
        if (Microbot.isLoggedIn()) {
            System.out.println("Client already logged in, proceeding to walking");
            currentState = MuleState.WALKING;
            updateRequestStatus("PROCESSING", "WALKING");
            return;
        }

        System.out.println("Client not logged in, attempting automatic login...");

        try {
            // Use the robust login logic from AutoLoginScript
            performRobustLogin();

            // Wait for login to complete with timeout
            boolean loginSuccessful = Global.sleepUntil(Microbot::isLoggedIn, 30000);

            if (loginSuccessful && Microbot.isLoggedIn()) {
                System.out.println("Successfully logged in automatically");
                currentState = MuleState.WALKING;
                updateRequestStatus("PROCESSING", "WALKING");
            } else {
                System.err.println("Auto-login failed after timeout");
                currentState = MuleState.ERROR;
                updateRequestStatus("FAILED", "Auto-login timeout");
            }

        } catch (Exception e) {
            System.err.println("Error during auto-login: " + e.getMessage());
            currentState = MuleState.ERROR;
            updateRequestStatus("FAILED", "Login error: " + e.getMessage());
        }
    }

    private void performRobustLogin() {
        try {
            // Check current login screen state like AutoLoginScript does
            if (Microbot.getClient() != null && Microbot.getClient().getGameState() == net.runelite.api.GameState.LOGIN_SCREEN) {

                int currentLoginIndex = Microbot.getClient().getLoginIndex();
                System.out.println("Login screen detected, login index: " + currentLoginIndex);

                // Handle different login screen states
                if (currentLoginIndex == 3 || currentLoginIndex == 24) {
                    System.out.println("Detected disconnection screen, handling...");
                    // Will be handled by Login constructor
                } else if (currentLoginIndex == 4 || currentLoginIndex == 3) {
                    System.err.println("Authentication failed - check credentials in RuneLite");
                    throw new RuntimeException("Authentication failed");
                } else if (currentLoginIndex == 34) {
                    System.err.println("Account is not a member, cannot login to members world");
                    throw new RuntimeException("Non-member account on members world");
                }

                // Perform login using existing Login utility (like AutoLoginScript does)
                System.out.println("Attempting login with saved credentials...");
                new Login(); // Use default login with saved RuneLite credentials

            } else {
                System.err.println("Not on login screen, current game state: " +
                    (Microbot.getClient() != null ? Microbot.getClient().getGameState() : "null"));
                throw new RuntimeException("Not on login screen");
            }

        } catch (Exception e) {
            System.err.println("Error in performRobustLogin: " + e.getMessage());
            throw e;
        }
    }

    private void handleWalkingState() {
        if (currentRequest == null) {
            currentState = MuleState.WAITING;
            return;
        }

        try {
            // Parse location string to WorldPoint
            WorldPoint targetLocation = parseLocation(currentRequest.getLocation());

            if (targetLocation == null) {
                System.err.println("Invalid location: " + currentRequest.getLocation());
                currentState = MuleState.ERROR;
                updateRequestStatus("FAILED", "Invalid location");
                return;
            }

            System.out.println("Walking to location: " + targetLocation);

            // Walk to the target location
            Rs2Walker.walkTo(targetLocation);

            // Wait for arrival
            Global.sleepUntil(() -> {
                WorldPoint playerPos = Rs2Player.getWorldLocation();
                return playerPos != null && playerPos.distanceTo(targetLocation) <= 3;
            }, 60000); // 1 minute timeout

            WorldPoint playerPos = Rs2Player.getWorldLocation();
            if (playerPos != null && playerPos.distanceTo(targetLocation) <= 3) {
                System.out.println("Arrived at destination");
                currentState = MuleState.TRADING;
                updateRequestStatus("PROCESSING", "TRADING");
            } else {
                System.err.println("Failed to reach destination");
                currentState = MuleState.ERROR;
                updateRequestStatus("FAILED", "Could not reach destination");
            }

        } catch (Exception e) {
            System.err.println("Error during walking: " + e.getMessage());
            currentState = MuleState.ERROR;
            updateRequestStatus("FAILED", "Walking error: " + e.getMessage());
        }
    }

    private void handleTradingState() {
        if (currentRequest == null) {
            currentState = MuleState.WAITING;
            return;
        }

        try {
            String requesterUsername = currentRequest.getRequesterUsername();
            System.out.println("Waiting for trade from: " + requesterUsername);

            long tradeWaitStart = System.currentTimeMillis();
            long maxWaitTime = (long) config.maxTradeWaitTime() * 60 * 1000; // Convert to milliseconds

            // Wait for incoming trade request using widget detection
            Global.sleepUntil(() -> hasIncomingTradeRequest() ||
                       (System.currentTimeMillis() - tradeWaitStart) > maxWaitTime, (int) maxWaitTime);

            if (!hasIncomingTradeRequest()) {
                System.err.println("No trade request received within timeout");
                currentState = MuleState.ERROR;
                updateRequestStatus("FAILED", "Trade timeout");
                return;
            }

            // Accept the trade if it's from the correct player
            String tradePartner = getTradePartnerName();
            if (tradePartner != null && tradePartner.equalsIgnoreCase(requesterUsername)) {
                System.out.println("Accepting trade from: " + tradePartner);
                acceptTrade();

                // Wait for trade completion
                Global.sleepUntil(() -> !isTradeWindowOpen(), 30000);

                if (!isTradeWindowOpen()) {
                    System.out.println("Trade completed successfully");
                    currentState = config.logoutAfterTrade() ? MuleState.LOGGING_OUT : MuleState.WAITING;
                    updateRequestStatus("COMPLETED", "Trade completed");
                    currentRequest = null;
                } else {
                    System.err.println("Trade did not complete properly");
                    currentState = MuleState.ERROR;
                    updateRequestStatus("FAILED", "Trade incomplete");
                }
            } else {
                System.err.println("Trade request from wrong player: " + tradePartner + " (expected: " + requesterUsername + ")");
                declineTrade();
                // Continue waiting for correct trade partner
            }

        } catch (Exception e) {
            System.err.println("Error during trading: " + e.getMessage());
            currentState = MuleState.ERROR;
            updateRequestStatus("FAILED", "Trading error: " + e.getMessage());
        }
    }

    private void handleLoggingOutState() {
        try {
            System.out.println("Logging out after completed trade");
            Rs2Player.logout();

            Global.sleepUntil(() -> !Microbot.isLoggedIn(), 10000);

            if (!Microbot.isLoggedIn()) {
                System.out.println("Successfully logged out");
                currentState = MuleState.WAITING;
            } else {
                System.err.println("Logout failed, continuing anyway");
                currentState = MuleState.WAITING;
            }

        } catch (Exception e) {
            System.err.println("Error during logout: " + e.getMessage());
            currentState = MuleState.WAITING; // Continue anyway
        }
    }

    private void handleErrorState() {
        System.out.println("In error state, waiting before retry");
        sleep(10000); // Wait 10 seconds before retry

        // Reset state and try again
        currentRequest = null;
        currentState = MuleState.WAITING;
    }

    private void pollForNewRequest() {
        try {
            log.info("Polling for new request from: {}", config.bridgeUrl() + "/api/mule/next-request");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.bridgeUrl() + "/api/mule/next-request"))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();

            CompletableFuture<HttpResponse<String>> future = httpClient.sendAsync(request,
                    HttpResponse.BodyHandlers.ofString());

            HttpResponse<String> response = future.get(10, TimeUnit.SECONDS);

            log.info("Polling response: status={}, body={}", response.statusCode(), response.body());

            if (response.statusCode() == 200) {
                String responseBody = response.body();

                // Simple check - if response contains "id" field, we have a request
                if (responseBody.contains("\"id\":")) {
                    try {
                        // Parse the JSON manually to avoid deserialization issues
                        currentRequest = parseRequestManually(responseBody);
                        if (currentRequest != null) {
                            requestStartTime = System.currentTimeMillis();
                            log.info("Successfully parsed mule request: {}", currentRequest.getId());

                            if (Microbot.isLoggedIn()) {
                                currentState = MuleState.WALKING;
                                updateRequestStatus("PROCESSING", "WALKING");
                            } else {
                                currentState = MuleState.LOGGING_IN;
                                updateRequestStatus("PROCESSING", "LOGIN");
                            }
                        } else {
                            log.error("Failed to parse request from response: {}", responseBody);
                        }
                    } catch (Exception e) {
                        log.error("Error parsing request manually: ", e);
                        // Fallback to Jackson parsing
                        try {
                            JsonNode jsonNode = objectMapper.readTree(responseBody);
                            currentRequest = objectMapper.treeToValue(jsonNode, MuleRequest.class);
                            requestStartTime = System.currentTimeMillis();
                            log.info("Fallback parsing successful: {}", currentRequest.getId());

                            if (Microbot.isLoggedIn()) {
                                currentState = MuleState.WALKING;
                                updateRequestStatus("PROCESSING", "WALKING");
                            } else {
                                currentState = MuleState.LOGGING_IN;
                                updateRequestStatus("PROCESSING", "LOGIN");
                            }
                        } catch (Exception fallbackError) {
                            log.error("Both manual and Jackson parsing failed: ", fallbackError);
                        }
                    }
                } else if (responseBody.contains("No pending requests")) {
                    log.info("No pending requests found in response");
                } else {
                    log.warn("Unexpected response format: {}", responseBody);
                }
            } else {
                log.warn("Polling failed with status: {}", response.statusCode());
            }

        } catch (Exception e) {
            log.error("Error during polling: ", e);
        }
    }

    // Manual JSON parsing to bypass deserialization issues
    private MuleRequest parseRequestManually(String json) {
        try {
            MuleRequest request = new MuleRequest();

            // Extract ID
            String id = extractJsonValue(json, "\"id\":");
            if (id != null) request.setId(id.replace("\"", ""));

            // Extract requesterUsername
            String requester = extractJsonValue(json, "\"requesterUsername\":");
            if (requester != null) request.setRequesterUsername(requester.replace("\"", ""));

            // Extract location
            String location = extractJsonValue(json, "\"location\":");
            if (location != null) request.setLocation(location.replace("\"", ""));

            // Extract status (handle both string and enum)
            String status = extractJsonValue(json, "\"status\":");
            if (status != null) {
                status = status.replace("\"", "").trim();
                request.setStatus(status);
            }

            // Extract currentStep
            String currentStep = extractJsonValue(json, "\"currentStep\":");
            if (currentStep != null) request.setCurrentStep(currentStep.replace("\"", ""));

            log.info("Manual parsing result: id={}, requester={}, location={}, status={}",
                    request.getId(), request.getRequesterUsername(), request.getLocation(), request.getStatus());

            return request;
        } catch (Exception e) {
            log.error("Manual parsing failed: ", e);
            return null;
        }
    }

    // Helper method to extract JSON values
    private String extractJsonValue(String json, String key) {
        try {
            int keyIndex = json.indexOf(key);
            if (keyIndex == -1) return null;

            int valueStart = keyIndex + key.length();
            char firstChar = json.charAt(valueStart);
            while (firstChar == ' ' || firstChar == '\t') {
                valueStart++;
                firstChar = json.charAt(valueStart);
            }

            if (firstChar == '"') {
                // String value
                int valueEnd = json.indexOf('"', valueStart + 1);
                return json.substring(valueStart, valueEnd + 1);
            } else {
                // Non-string value (number, boolean, etc.)
                int valueEnd = valueStart;
                char c = json.charAt(valueEnd);
                while (c != ',' && c != '}' && c != '\n' && c != '\r') {
                    valueEnd++;
                    if (valueEnd >= json.length()) break;
                    c = json.charAt(valueEnd);
                }
                return json.substring(valueStart, valueEnd).trim();
            }
        } catch (Exception e) {
            log.error("Error extracting JSON value for key {}: ", key, e);
            return null;
        }
    }

    private WorldPoint parseLocation(String location) {
        try {
            // Expected format: "x,y,plane" or location name
            if (location.contains(",")) {
                String[] parts = location.split(",");
                if (parts.length >= 2) {
                    int x = Integer.parseInt(parts[0].trim());
                    int y = Integer.parseInt(parts[1].trim());
                    int plane = parts.length > 2 ? Integer.parseInt(parts[2].trim()) : 0;
                    return new WorldPoint(x, y, plane);
                }
            }

            // Handle common location names
            return getKnownLocation(location);

        } catch (Exception e) {
            System.err.println("Error parsing location: " + location + " - " + e.getMessage());
            return null;
        }
    }

    private WorldPoint getKnownLocation(String locationName) {
        // Common trading locations
        switch (locationName.toLowerCase().trim()) {
            case "grand exchange":
            case "ge":
                return new WorldPoint(3164, 3486, 0);
            case "varrock west bank":
                return new WorldPoint(3185, 3436, 0);
            case "lumbridge":
                return new WorldPoint(3222, 3218, 0);
            case "falador":
                return new WorldPoint(2965, 3378, 0);
            default:
                System.err.println("Unknown location name: " + locationName);
                return null;
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        if (currentRequest != null) {
            updateRequestStatus("FAILED", "Script shutdown");
        }
        System.out.println("Mule script shutdown");
    }

    // Getters for overlay
    public MuleState getCurrentState() {
        return currentState;
    }

    public MuleRequest getCurrentRequest() {
        return currentRequest;
    }

    public long getRequestStartTime() {
        return requestStartTime;
    }

    // Trade-related helper methods using widget detection
    private boolean hasIncomingTradeRequest() {
        // Check for trade request interface
        Widget tradeRequestWidget = Rs2Widget.getWidget(219, 0); // Trade request interface
        return tradeRequestWidget != null && !tradeRequestWidget.isHidden();
    }

    private String getTradePartnerName() {
        try {
            Widget tradeRequestWidget = Rs2Widget.getWidget(219, 1); // Trade request text widget
            if (tradeRequestWidget != null && tradeRequestWidget.getText() != null) {
                String text = tradeRequestWidget.getText();
                // Extract username from "wishes to trade with you" message
                if (text.contains("wishes to trade with you")) {
                    return text.split(" ")[0]; // Get first word (username)
                }
            }
        } catch (Exception e) {
            // Silent catch
        }
        return null;
    }

    private void acceptTrade() {
        try {
            Widget acceptButton = Rs2Widget.getWidget(219, 3); // Accept button
            if (acceptButton != null) {
                Rs2Widget.clickWidget(acceptButton);
            }
        } catch (Exception e) {
            System.err.println("Error accepting trade: " + e.getMessage());
        }
    }

    private void declineTrade() {
        try {
            Widget declineButton = Rs2Widget.getWidget(219, 4); // Decline button
            if (declineButton != null) {
                Rs2Widget.clickWidget(declineButton);
            }
        } catch (Exception e) {
            System.err.println("Error declining trade: " + e.getMessage());
        }
    }

    private boolean isTradeWindowOpen() {
        // Check if trade window is open
        Widget tradeWindow = Rs2Widget.getWidget(335, 0); // Trade interface
        return tradeWindow != null && !tradeWindow.isHidden();
    }

    private void updateRequestStatus(String status, String currentStep) {
        if (currentRequest == null) return;

        try {
            String requestBody = String.format(
                "{\"status\":\"%s\",\"currentStep\":\"%s\"}",
                status, currentStep
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.bridgeUrl() + "/api/mule/request/" + currentRequest.getId() + "/status"))
                    .PUT(HttpRequest.BodyPublishers.ofString(requestBody))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(5))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        } catch (Exception e) {
            // Silent debug
            log.debug("Failed to update request status: ", e);
        }
    }
}
