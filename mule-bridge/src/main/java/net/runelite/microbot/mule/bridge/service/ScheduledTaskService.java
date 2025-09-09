package net.runelite.microbot.mule.bridge.service;

import net.runelite.microbot.mule.bridge.service.MuleRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduled tasks for maintenance operations
 */
@Service
public class ScheduledTaskService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskService.class);

    @Autowired
    private MuleRequestService muleRequestService;

    /**
     * Clean up old requests every 30 minutes
     */
    @Scheduled(fixedRate = 1800000) // 30 minutes
    public void cleanupOldRequests() {
        try {
            logger.debug("Running cleanup of old requests");
            muleRequestService.cleanupOldRequests();
        } catch (Exception e) {
            logger.error("Error during cleanup task", e);
        }
    }

    /**
     * Log system statistics every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void logSystemStats() {
        try {
            long total = muleRequestService.getTotalRequestCount();
            int pending = muleRequestService.getPendingRequestsCount();
            long active = muleRequestService.getActiveRequestsCount();

            logger.info("System Stats - Total: {}, Pending: {}, Active: {}",
                       total, pending, active);
        } catch (Exception e) {
            logger.error("Error logging system stats", e);
        }
    }
}
