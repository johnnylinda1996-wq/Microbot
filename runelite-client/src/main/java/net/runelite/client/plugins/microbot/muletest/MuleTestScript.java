package net.runelite.client.plugins.microbot.muletest;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.mule.MuleHelper;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.concurrent.TimeUnit;

@Slf4j
public class MuleTestScript extends Script {

    public static String version = "1.0.0";
    private MuleTestConfig config;
    private boolean muleRequested = false;
    private long lastCheck = 0;

    public boolean run(MuleTestConfig config) {
        this.config = config;
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn() || Microbot.getClient().getGameState() != GameState.LOGGED_IN) {
                    return;
                }

                if (!super.run()) return;

                // Check GP amount periodically
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastCheck >= config.checkInterval() * 1000L) {
                    lastCheck = currentTime;
                    checkGpAndRequestMule();
                }

            } catch (Exception ex) {
                log.error("Error in mule test script: ", ex);
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return true;
    }

    private void checkGpAndRequestMule() {
        try {
            // Get current GP amount - use itemQuantity instead of count to get actual amount
            int currentGp = Rs2Inventory.itemQuantity(995); // 995 is coins item ID
            log.info("Current GP: {} | Threshold: {}", currentGp, config.gpThreshold());

            if (currentGp >= config.gpThreshold() && !muleRequested) {
                log.info("GP threshold reached! Requesting mule...");
                requestMule(currentGp);
            } else if (currentGp < config.gpThreshold() && muleRequested) {
                // Reset mule request if GP drops below threshold (after successful trade)
                muleRequested = false;
                log.info("GP below threshold, reset mule request status");
            }

        } catch (Exception e) {
            log.error("Error checking GP and requesting mule: ", e);
        }
    }

    private void requestMule(int gpAmount) {
        try {
            String location = config.muleLocation().toString();
            log.info("Requesting mule at {} for {} GP", location, gpAmount);

            if (config.autoWalkToMule()) {
                // Walk to mule location first
                walkToMuleLocation();
            }

            // Request mule using the MuleHelper
            MuleHelper.requestMuleWithMonitoring(
                location,
                status -> {
                    log.info("Mule Status Update: {}", status.status);

                    // Handle different status updates
                    switch (status.status) {
                        case "QUEUED":
                            log.info("Mule request queued, waiting for mule bot...");
                            break;
                        case "PROCESSING":
                            log.info("Mule bot is processing request - {} ", status.currentStep);
                            break;
                        case "LOGIN":
                            log.info("Mule bot is logging in...");
                            break;
                        case "WALKING":
                            log.info("Mule bot is walking to location...");
                            break;
                        case "WAITING_FOR_TRADE":
                            log.info("Mule bot is waiting for trade - go trade them!");
                            break;
                    }
                },
                success -> {
                    if (success) {
                        log.info("Mule trade completed successfully!");
                        muleRequested = false;

                        if (config.stopAfterMule()) {
                            log.info("Stopping script as configured");
                            shutdown();
                        }
                    } else {
                        log.error("Mule trade failed!");
                        muleRequested = false;
                    }
                }
            );

            muleRequested = true;
            log.info("Mule request sent successfully");

        } catch (Exception e) {
            log.error("Error requesting mule: ", e);
            muleRequested = false;
        }
    }

    private void walkToMuleLocation() {
        try {
            WorldPoint destination = getMuleLocationWorldPoint();
            if (destination == null) {
                log.error("Unknown mule location: {}", config.muleLocation());
                return;
            }

            log.info("Walking to mule location: {} at {}", config.muleLocation(), destination);

            // Use Rs2Walker to walk to the location
            Rs2Walker.walkTo(destination);

            // Wait until we're close to the destination
            sleep(2000, 5000);

            while (Rs2Player.getWorldLocation().distanceTo(destination) > 5 && isRunning()) {
                sleep(1000);
                log.info("Walking to mule location... Distance: {}", Rs2Player.getWorldLocation().distanceTo(destination));
            }

            log.info("Arrived at mule location!");

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

    @Override
    public void shutdown() {
        super.shutdown();
        log.info("Mule test script stopped");
    }
}
