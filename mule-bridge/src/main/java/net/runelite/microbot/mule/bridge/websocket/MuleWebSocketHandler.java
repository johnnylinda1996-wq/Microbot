package net.runelite.microbot.mule.bridge.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.runelite.microbot.mule.bridge.model.MuleRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for real-time mule request updates
 */
@Component
public class MuleWebSocketHandler implements WebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(MuleWebSocketHandler.class);
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        logger.info("WebSocket connection established: {}", session.getId());
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        // Handle incoming messages if needed
        logger.debug("Received WebSocket message from {}: {}", session.getId(), message.getPayload());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        logger.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
        sessions.remove(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        sessions.remove(session);
        logger.info("WebSocket connection closed: {} (status: {})", session.getId(), closeStatus);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * Broadcast update to all connected clients
     */
    public void broadcastUpdate(MuleRequest request) {
        if (sessions.isEmpty()) {
            return;
        }

        try {
            String message = objectMapper.writeValueAsString(request);
            TextMessage textMessage = new TextMessage(message);

            // Remove closed sessions and send to active ones
            sessions.removeIf(session -> {
                if (!session.isOpen()) {
                    return true;
                }

                try {
                    session.sendMessage(textMessage);
                    return false;
                } catch (IOException e) {
                    logger.error("Failed to send message to WebSocket session {}: {}",
                                session.getId(), e.getMessage());
                    return true;
                }
            });

            logger.debug("Broadcasted update for request {} to {} clients",
                        request.getId(), sessions.size());

        } catch (Exception e) {
            logger.error("Error broadcasting WebSocket update", e);
        }
    }

    /**
     * Get number of active connections
     */
    public int getActiveConnectionsCount() {
        return sessions.size();
    }
}
