package net.runelite.microbot.mule.bridge.controller;

import net.runelite.microbot.mule.bridge.model.MuleRequest;
import net.runelite.microbot.mule.bridge.model.RequestStatus;
import net.runelite.microbot.mule.bridge.service.MuleRequestService;
import net.runelite.microbot.mule.bridge.websocket.MuleWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * REST API controller for mule trading requests
 */
@RestController
@RequestMapping("/api/mule")
@Validated
@CrossOrigin(origins = "http://localhost:*", allowCredentials = "true")
public class MuleController {

    private static final Logger logger = LoggerFactory.getLogger(MuleController.class);

    @Autowired
    private MuleRequestService muleRequestService;

    @Autowired
    private MuleWebSocketHandler webSocketHandler;

    /**
     * Create new mule request
     * POST /api/mule/request
     */
    @PostMapping("/request")
    public ResponseEntity<?> createRequest(@Valid @RequestBody MuleRequest request) {
        try {
            logger.info("Received mule request from: {}", request.getRequesterUsername());

            MuleRequest createdRequest = muleRequestService.createRequest(request);

            // Notify WebSocket clients
            webSocketHandler.broadcastUpdate(createdRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("requestId", createdRequest.getId());
            response.put("message", "Mule request created successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error creating mule request", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to create mule request: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Poll for next pending request (for mule clients)
     * GET /api/mule/next-request
     */
    @GetMapping("/next-request")
    public ResponseEntity<?> getNextRequest() {
        try {
            Optional<MuleRequest> nextRequest = muleRequestService.getNextPendingRequest();

            if (nextRequest.isPresent()) {
                MuleRequest request = nextRequest.get();
                logger.info("Serving next request {} to mule", request.getId());

                // Notify WebSocket clients of status change
                webSocketHandler.broadcastUpdate(request);

                return ResponseEntity.ok(request);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "No pending requests");
                return ResponseEntity.ok(response);
            }

        } catch (Exception e) {
            logger.error("Error getting next request", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to get next request: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Update request status
     * PUT /api/mule/request/{id}/status
     */
    @PutMapping("/request/{id}/status")
    public ResponseEntity<?> updateRequestStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> statusUpdate) {

        try {
            String statusStr = statusUpdate.get("status");
            String currentStep = statusUpdate.get("currentStep");

            if (statusStr == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Status is required");
                return ResponseEntity.badRequest().body(error);
            }

            RequestStatus status = RequestStatus.valueOf(statusStr.toUpperCase());
            boolean updated = muleRequestService.updateRequestStatus(id, status, currentStep);

            if (updated) {
                Optional<MuleRequest> request = muleRequestService.getRequest(id);
                if (request.isPresent()) {
                    // Notify WebSocket clients
                    webSocketHandler.broadcastUpdate(request.get());
                }

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Status updated successfully");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Request not found");
                return ResponseEntity.notFound().build();
            }

        } catch (IllegalArgumentException e) {
            logger.error("Invalid status provided: {}", statusUpdate.get("status"));
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Invalid status: " + statusUpdate.get("status"));
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            logger.error("Error updating request status", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to update status: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get request status
     * GET /api/mule/request/{id}/status
     */
    @GetMapping("/request/{id}/status")
    public ResponseEntity<?> getRequestStatus(@PathVariable String id) {
        try {
            Optional<MuleRequest> request = muleRequestService.getRequest(id);

            if (request.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("requestId", id);
                response.put("status", request.get().getStatus());
                response.put("currentStep", request.get().getCurrentStep());
                response.put("timestamp", request.get().getTimestamp());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Request not found");
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("Error getting request status", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to get request status: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Health check endpoint
     * GET /health
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        health.put("totalRequests", muleRequestService.getTotalRequestCount());
        health.put("pendingRequests", muleRequestService.getPendingRequestsCount());
        health.put("activeRequests", muleRequestService.getActiveRequestsCount());

        return ResponseEntity.ok(health);
    }
}
