package net.runelite.microbot.mule.bridge.service;

import net.runelite.microbot.mule.bridge.model.MuleRequest;
import net.runelite.microbot.mule.bridge.model.RequestStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for managing mule requests with in-memory storage
 */
@Service
public class MuleRequestService {

    private static final Logger logger = LoggerFactory.getLogger(MuleRequestService.class);

    // In-memory storage
    private final ConcurrentHashMap<String, MuleRequest> requestStorage = new ConcurrentHashMap<>();
    private final Queue<String> pendingQueue = new ConcurrentLinkedQueue<>();
    private final AtomicLong requestCount = new AtomicLong(0);

    /**
     * Create a new mule request
     */
    public MuleRequest createRequest(MuleRequest request) {
        logger.info("Creating new mule request: {}", request);

        // Store the request
        requestStorage.put(request.getId(), request);

        // Add to pending queue
        pendingQueue.offer(request.getId());
        requestCount.incrementAndGet();

        logger.info("Mule request created with ID: {}", request.getId());
        return request;
    }

    /**
     * Get the next pending request for a mule to process
     */
    public Optional<MuleRequest> getNextPendingRequest() {
        String requestId = pendingQueue.poll();

        if (requestId == null) {
            return Optional.empty();
        }

        MuleRequest request = requestStorage.get(requestId);
        if (request != null && request.getStatus() == RequestStatus.QUEUED) {
            // Mark as processing
            request.setStatus(RequestStatus.PROCESSING);
            request.setCurrentStep("LOGIN");
            logger.info("Assigned request {} to mule", requestId);
            return Optional.of(request);
        }

        return Optional.empty();
    }

    /**
     * Update request status
     */
    public boolean updateRequestStatus(String requestId, RequestStatus status, String currentStep) {
        MuleRequest request = requestStorage.get(requestId);

        if (request == null) {
            logger.warn("Attempted to update non-existent request: {}", requestId);
            return false;
        }

        request.setStatus(status);
        if (currentStep != null) {
            request.setCurrentStep(currentStep);
        }

        logger.info("Updated request {} status to {} (step: {})", requestId, status, currentStep);
        return true;
    }

    /**
     * Get request by ID
     */
    public Optional<MuleRequest> getRequest(String requestId) {
        return Optional.ofNullable(requestStorage.get(requestId));
    }

    /**
     * Get request status
     */
    public Optional<RequestStatus> getRequestStatus(String requestId) {
        return getRequest(requestId).map(MuleRequest::getStatus);
    }

    /**
     * Clean up old completed/failed requests (older than 1 hour)
     */
    public void cleanupOldRequests() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);

        requestStorage.entrySet().removeIf(entry -> {
            MuleRequest request = entry.getValue();
            boolean shouldRemove = (request.getStatus() == RequestStatus.COMPLETED ||
                                   request.getStatus() == RequestStatus.FAILED) &&
                                   request.getTimestamp().isBefore(cutoff);

            if (shouldRemove) {
                logger.info("Cleaning up old request: {}", entry.getKey());
            }

            return shouldRemove;
        });
    }

    /**
     * Get total request count
     */
    public long getTotalRequestCount() {
        return requestCount.get();
    }

    /**
     * Get pending requests count
     */
    public int getPendingRequestsCount() {
        return pendingQueue.size();
    }

    /**
     * Get active requests count (processing)
     */
    public long getActiveRequestsCount() {
        return requestStorage.values().stream()
                .filter(r -> r.getStatus() == RequestStatus.PROCESSING)
                .count();
    }
}
