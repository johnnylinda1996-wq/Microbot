package net.runelite.client.plugins.microbot.util.botmanager;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Utility for managing other bot scripts - pausing/resuming them
 * when mule operations are in progress
 */
@Slf4j
public class Rs2BotManager {

    private static final ConcurrentMap<String, Script> pausedBots = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Boolean> botStates = new ConcurrentHashMap<>();

    /**
     * Pause all running bot scripts except the specified script
     * @param excludeScript The script to NOT pause (usually the mule script)
     * @return List of paused script names
     */
    public static List<String> pauseAllBots(Script excludeScript) {
        List<String> pausedScriptNames = new ArrayList<>();

        try {
            log.info("Pausing all active bot scripts for mule operation...");

            // Get all active scripts from Microbot's script manager
            // This is a simplified approach - in real implementation you'd need access to the script registry

            // For now, we'll track scripts that register themselves
            synchronized (pausedBots) {
                botStates.forEach((scriptName, isRunning) -> {
                    if (isRunning) {
                        Script script = pausedBots.get(scriptName);
                        if (script != null && script != excludeScript && script.isRunning()) {
                            log.info("Pausing script: {}", scriptName);
                            script.shutdown();
                            pausedScriptNames.add(scriptName);
                            botStates.put(scriptName, false);
                        }
                    }
                });
            }

            log.info("Paused {} bot scripts", pausedScriptNames.size());

        } catch (Exception e) {
            log.error("Error pausing bot scripts: ", e);
        }

        return pausedScriptNames;
    }

    /**
     * Resume previously paused bot scripts
     * @param scriptNames List of script names to resume
     */
    public static void resumeBots(List<String> scriptNames) {
        try {
            log.info("Resuming {} bot scripts after mule operation...", scriptNames.size());

            for (String scriptName : scriptNames) {
                try {
                    Script script = pausedBots.get(scriptName);
                    if (script != null) {
                        log.info("Resuming script: {}", scriptName);
                        // In a real implementation, you'd restart the script here
                        // This depends on how your script system works
                        botStates.put(scriptName, true);
                    }
                } catch (Exception e) {
                    log.error("Failed to resume script {}: ", scriptName, e);
                }
            }

        } catch (Exception e) {
            log.error("Error resuming bot scripts: ", e);
        }
    }

    /**
     * Register a script with the bot manager
     * @param scriptName Name of the script
     * @param script The script instance
     */
    public static void registerScript(String scriptName, Script script) {
        pausedBots.put(scriptName, script);
        botStates.put(scriptName, script.isRunning());
        log.debug("Registered script: {}", scriptName);
    }

    /**
     * Unregister a script from the bot manager
     * @param scriptName Name of the script to unregister
     */
    public static void unregisterScript(String scriptName) {
        pausedBots.remove(scriptName);
        botStates.remove(scriptName);
        log.debug("Unregistered script: {}", scriptName);
    }

    /**
     * Check if any bots are currently paused
     * @return true if any bots are paused
     */
    public static boolean hasAnyBotsPaused() {
        return botStates.values().stream().anyMatch(isRunning -> !isRunning);
    }

    /**
     * Get list of currently paused bot names
     * @return List of paused bot names
     */
    public static List<String> getPausedBotNames() {
        List<String> paused = new ArrayList<>();
        botStates.forEach((name, isRunning) -> {
            if (!isRunning) {
                paused.add(name);
            }
        });
        return paused;
    }

    /**
     * Emergency stop all registered bots
     */
    public static void emergencyStopAllBots() {
        log.warn("Emergency stopping all registered bots!");

        pausedBots.values().forEach(script -> {
            try {
                if (script.isRunning()) {
                    script.shutdown();
                }
            } catch (Exception e) {
                log.error("Error during emergency stop: ", e);
            }
        });

        botStates.replaceAll((name, isRunning) -> false);
    }

    /**
     * Get status of all registered bots
     * @return String representation of bot statuses
     */
    public static String getBotStatusSummary() {
        StringBuilder status = new StringBuilder();
        status.append("Bot Manager Status:\n");

        botStates.forEach((name, isRunning) -> {
            status.append(String.format("  %s: %s\n", name, isRunning ? "RUNNING" : "PAUSED"));
        });

        return status.toString();
    }
}
