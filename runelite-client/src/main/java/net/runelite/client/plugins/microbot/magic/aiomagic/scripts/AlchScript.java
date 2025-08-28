package net.runelite.client.plugins.microbot.magic.aiomagic.scripts;

import net.runelite.api.ItemID;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.magic.aiomagic.AIOMagicConfig;
import net.runelite.client.plugins.microbot.magic.aiomagic.AIOMagicPlugin;
import net.runelite.client.plugins.microbot.magic.aiomagic.enums.MagicState;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.antiban.enums.Activity;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.magic.Rs2Spells;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.security.Login;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AlchScript extends Script {
    private static final int POLL_INTERVAL_MS = 1000;
    private static final int LOGIN_WAIT_MS = 2000;
    private static final int ITEM_MOVE_TIMEOUT_MS = 1000;
    private static final int MAX_LOGIN_ATTEMPTS = 10;
    private static final long CONSOLE_LOG_INTERVAL_MS = 10000;

    private final MagicState state = MagicState.CASTING;
    private final AIOMagicPlugin plugin;
    private final AtomicBoolean shutdownRequested = new AtomicBoolean(false);

    @Inject
    private AIOMagicConfig config;

    // Stats
    public static volatile int totalProfit = 0;
    public static volatile int totalAlched = 0;
    private static volatile int remainingItems = 0;

    // Timekeeping (pause-aware)
    private static volatile long accumulatedActiveTime = 0L;
    private static volatile long startTimeForThisRun = 0L;
    private static volatile boolean paused = false;
    private static volatile long pauseStartTime = 0L;

    // AFK scheduling
    private static volatile long nextAfkTime = 0L;
    private static volatile long afkEndTime = 0L;
    private static volatile boolean afkPlanned = false;
    private static volatile boolean loginInProgress = false;

    @Inject
    public AlchScript(AIOMagicPlugin plugin) {
        this.plugin = plugin;
    }

    /* ---------------- Public static helpers for overlay ---------------- */

    public static boolean isPaused() { return paused; }

    public static int getRemainingItems() { return remainingItems; }

    public static String getRuntime() {
        long elapsed = getCurrentActiveTime();
        return formatHHMMSS(elapsed);
    }

    public static int getProfitPerHour() {
        return calculatePerHour(totalProfit);
    }

    public static int getAlchesPerHour() {
        return calculatePerHour(totalAlched);
    }

    private static int calculatePerHour(int value) {
        long activeMs = getCurrentActiveTime();
        double hours = activeMs / 3_600_000.0;
        return hours > 0 ? (int) (value / hours) : 0;
    }

    private static long getCurrentActiveTime() {
        return accumulatedActiveTime + (paused
                ? (pauseStartTime - startTimeForThisRun)
                : (System.currentTimeMillis() - startTimeForThisRun));
    }

    public static String formatGP(int gp) {
        if (gp >= 1_000_000) return String.format("%.1fM", gp / 1_000_000.0);
        if (gp >= 1_000) return String.format("%.1fK", gp / 1_000.0);
        return String.valueOf(gp);
    }

    // Method to get total remaining alch items
    public static int getTotalAlchItemCount(AIOMagicPlugin plugin) {
        if (plugin == null || plugin.getAlchItemNames().isEmpty()) return 0;

        return plugin.getAlchItemNames().stream()
                .mapToInt(itemName -> {
                    try {
                        return Rs2Inventory.count(itemName);
                    } catch (Exception e) {
                        // Fallback if count method doesn't exist
                        Rs2ItemModel item = Rs2Inventory.get(itemName);
                        return item != null ? item.getQuantity() : 0;
                    }
                })
                .sum();
    }

    public static void enableAfkOverlayOnly(AIOMagicConfig config) {
        if (!config.afkEnabled()) {
            resetAfkSettings();
            return;
        }
        scheduleAfk(config);
    }

    private static void scheduleAfk(AIOMagicConfig config) {
        long intervalMs = getRandomTimeInRange(config.afkIntervalMin(), config.afkIntervalMax());
        long durationMs = getRandomTimeInRange(config.afkDurationMin(), config.afkDurationMax());

        nextAfkTime = System.currentTimeMillis() + intervalMs;
        afkEndTime = nextAfkTime + durationMs;
        afkPlanned = true;
    }

    private static long getRandomTimeInRange(int minMinutes, int maxMinutes) {
        return ThreadLocalRandom.current().nextLong(
                TimeUnit.MINUTES.toMillis(Math.max(0, minMinutes)),
                TimeUnit.MINUTES.toMillis(Math.max(minMinutes, maxMinutes)) + 1
        );
    }

    private static void resetAfkSettings() {
        afkPlanned = false;
        nextAfkTime = 0L;
        afkEndTime = 0L;
    }

    // AFK status methods
    public static boolean isAfkScheduled() { return afkPlanned && nextAfkTime > 0; }

    public static boolean isInAfkBreak() {
        return afkPlanned && paused && System.currentTimeMillis() < afkEndTime;
    }

    public static long getMillisUntilNextAfk() {
        return isAfkScheduled() ? Math.max(0, nextAfkTime - System.currentTimeMillis()) : 0;
    }

    public static long getPlannedAfkDurationMillis() {
        return isAfkScheduled() ? Math.max(0, afkEndTime - nextAfkTime) : 0;
    }

    public static long getCurrentBreakRemainingMillis() {
        return isInAfkBreak() ? Math.max(0, afkEndTime - System.currentTimeMillis()) : 0;
    }

    public static String formatMMSS(long millis) {
        if (millis <= 0) return "00:00";
        long m = millis / 60_000;
        long s = (millis % 60_000) / 1000;
        return String.format("%02d:%02d", m, s);
    }

    private static String formatHHMMSS(long millis) {
        if (millis < 0) millis = 0;
        long h = millis / 3_600_000;
        long m = (millis % 3_600_000) / 60_000;
        long s = (millis % 60_000) / 1000;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private static void updateWindowTitle() {
        if (!isInAfkBreak() || Microbot.isLoggedIn()) {
            setWindowTitle("Microbot");
            return;
        }

        long remaining = getCurrentBreakRemainingMillis();
        String timeLeft = formatMMSS(remaining);
        setWindowTitle("Microbot - AFK Break: " + timeLeft + " remaining");
    }

    private static void setWindowTitle(String title) {
        try {
            Canvas canvas = Microbot.getClient().getCanvas();
            Window window = SwingUtilities.getWindowAncestor(canvas);

            if (window instanceof JFrame) {
                ((JFrame) window).setTitle(title);
            } else if (window instanceof Frame) {
                ((Frame) window).setTitle(title);
            }
        } catch (Exception e) {
            // Fallback methods
            try {
                Frame[] frames = Frame.getFrames();
                for (Frame frame : frames) {
                    String frameTitle = frame.getTitle();
                    if (frameTitle.contains("Microbot") || frameTitle.contains("RuneLite") || frameTitle.contains("AFK Break")) {
                        frame.setTitle(title);
                        break;
                    }
                }
            } catch (Exception ignored) {
                // Silent fail
            }
        }
    }

    /* ---------------- Pause/Resume/Reset ---------------- */

    public static void resetAll() {
        totalProfit = 0;
        totalAlched = 0;
        remainingItems = 0;
        accumulatedActiveTime = 0L;
        paused = false;
        pauseStartTime = 0L;
        startTimeForThisRun = System.currentTimeMillis();
        resetAfkSettings();
        loginInProgress = false;
    }

    public static void pause() {
        if (!paused) {
            paused = true;
            pauseStartTime = System.currentTimeMillis();
        }
    }

    public static void resume() {
        if (paused) {
            paused = false;
            accumulatedActiveTime += (pauseStartTime - startTimeForThisRun);
            startTimeForThisRun = System.currentTimeMillis();
            pauseStartTime = 0L;
            loginInProgress = false;
        }
    }

    /* ---------------- Core script ---------------- */

    private Rs2Spells getAlchSpell() {
        return Rs2Player.getBoostedSkillLevel(Skill.MAGIC) >= 55
                ? Rs2Spells.HIGH_LEVEL_ALCHEMY
                : Rs2Spells.LOW_LEVEL_ALCHEMY;
    }

    private void scheduleNextAfk() {
        if (!config.afkEnabled()) {
            resetAfkSettings();
            return;
        }
        scheduleAfk(config);
    }

    private void handlePohEntry() throws InterruptedException {
        if (!Rs2Player.IsInInstance() && Rs2Inventory.hasItem(8013) && config.alchinpoh()) {
            Rs2Inventory.interact(8013, "Inside");
            Thread.sleep(3000);
        }
    }

    private void handleAfkLogic(long now) {
        if (!afkPlanned) return;

        // Start break
        if (!paused && now >= nextAfkTime && now < afkEndTime) {
            startAfkBreak();
        }

        // End break
        if (paused && now >= afkEndTime) {
            endAfkBreak();
        }
    }

    private void startAfkBreak() {
        pause();

        if (config.logoutEnabled() && Microbot.isLoggedIn()) {
            Rs2Player.logout();
            Microbot.log("Break started: logged out");
        } else {
            Microbot.log("AFK started: paused only");
        }

        // Start countdown thread
        startBreakCountdown();
    }

    private void startBreakCountdown() {
        new Thread(() -> {
            while (paused && System.currentTimeMillis() < afkEndTime) {
                long remaining = afkEndTime - System.currentTimeMillis();
                long sec = remaining / 1000;
                Microbot.log("Logging back in: " + sec + " seconds.");
                try {
                    Thread.sleep(CONSOLE_LOG_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "AFK-Countdown").start();
    }

    private void endAfkBreak() {
        Microbot.log("Break finished - checking login status");

        if (shouldAttemptLogin()) {
            attemptLogin();
        } else {
            resumeAfterBreak();
        }
    }

    private boolean shouldAttemptLogin() {
        return !Microbot.isLoggedIn() && config.logoutEnabled() && !loginInProgress;
    }

    private void attemptLogin() {
        loginInProgress = true;
        Microbot.log("Attempting to login...");

        new Thread(() -> {
            try {
                boolean loginSuccess = performLoginAttempts();
                handleLoginResult(loginSuccess);
            } catch (Exception e) {
                Microbot.log("Login error: " + e.getMessage());
                resumeAfterBreak();
            } finally {
                loginInProgress = false;
            }
        }, "Login-Thread").start();
    }

    private boolean performLoginAttempts() throws InterruptedException {
        for (int attempt = 1; attempt <= MAX_LOGIN_ATTEMPTS; attempt++) {
            Microbot.log("Login attempt " + attempt + "/" + MAX_LOGIN_ATTEMPTS);

            Thread.sleep(LOGIN_WAIT_MS);

            if (Microbot.isLoggedIn()) {
                Microbot.log("Already logged in!");
                return true;
            }

            try {
                new Login(config.world());
                Thread.sleep(3000);

                if (Microbot.isLoggedIn()) {
                    Microbot.log("Login successful!");
                    return true;
                }
            } catch (Exception e) {
                Microbot.log("Login attempt failed: " + e.getMessage());
                Thread.sleep(LOGIN_WAIT_MS);
            }
        }

        Microbot.log("Failed to login after " + MAX_LOGIN_ATTEMPTS + " attempts");
        return false;
    }

    private void handleLoginResult(boolean loginSuccess) {
        if (loginSuccess || !config.logoutEnabled()) {
            try {
                Thread.sleep(LOGIN_WAIT_MS); // Give game time to load
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            resumeAfterBreak();
            Microbot.log("Script resumed after login attempt");
        }
    }

    private void resumeAfterBreak() {
        resume();
        scheduleNextAfk();
        Microbot.log("Script resumed after AFK break");
    }

    private boolean hasRequiredItems() {
        if (plugin.getAlchItemNames().isEmpty()) {
            Microbot.showMessage("Alch Item list is empty");
            return false;
        }
        return true;
    }

    private Rs2ItemModel findAlchItem() {
        return plugin.getAlchItemNames().stream()
                .filter(Rs2Inventory::hasItem)
                .map(Rs2Inventory::get)
                .findFirst()
                .orElse(null);
    }

    private boolean handleMissingItems(Rs2ItemModel alchItem) {
        if (alchItem == null) {
            Microbot.log("Missing alch items...");
            if (config.outofalch()) {
                shutdownWithLogout("No more items to alch");
            }
            return true;
        }
        return false;
    }

    private boolean handleMissingRunes() {
        if (!Rs2Magic.hasRequiredRunes(plugin.getAlchSpell())) {
            Microbot.log("Unable to cast alchemy spell - missing runes");
            if (config.outofalch()) {
                shutdownWithLogout("Missing required runes");
            }
            return true;
        }
        return false;
    }

    private void handleNaturalMouseMovement(Rs2ItemModel alchItem) throws InterruptedException {
        if (!Rs2AntibanSettings.naturalMouse) return;

        int targetSlot = (getAlchSpell() == Rs2Spells.HIGH_LEVEL_ALCHEMY) ? 12 : 4;

        if (alchItem.getSlot() == targetSlot) return;

        Rs2Inventory.moveItemToSlot(alchItem, targetSlot);
        waitForItemMove(alchItem, targetSlot);
    }

    private void waitForItemMove(Rs2ItemModel alchItem, int targetSlot) throws InterruptedException {
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < ITEM_MOVE_TIMEOUT_MS) {
            Thread.sleep(20);
            Rs2ItemModel refreshedItem = Rs2Inventory.get(alchItem.getId());

            if (refreshedItem == null || refreshedItem.getSlot() == targetSlot) {
                break;
            }
        }
    }

    private void performAlchemy(Rs2ItemModel alchItem) {
        Rs2Magic.alch(alchItem);
        Rs2Player.waitForXpDrop(Skill.MAGIC, 10000, false);

        totalAlched++;
        updateProfit();
        updateRemainingItemsCount();
    }

    private void updateProfit() {
        try {
            int profitPerAlch = Integer.parseInt(config.alchprofit());
            totalProfit += profitPerAlch;
        } catch (NumberFormatException e) {
            // Invalid profit value, skip update
        }
    }

    private void updateRemainingItemsCount() {
        remainingItems = getTotalAlchItemCount(plugin);
    }

    private void shutdownWithLogout(String reason) {
        Microbot.log(reason + " - Logging out in 5 seconds...");
        shutdownRequested.set(true);

        new Thread(() -> {
            try {
                Thread.sleep(5000);
                Rs2Player.logout();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Logout-Thread").start();
    }

    @Override
    public boolean run() {
        setupScript();

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!super.run() || shutdownRequested.get()) return;

                long now = System.currentTimeMillis();
                updateWindowTitle();

                handlePohEntry();
                handleAfkLogic(now);

                // Exit early if paused or not logged in
                if (paused || !Microbot.isLoggedIn()) return;

                executeAlchingLogic();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Microbot.log("Script interrupted");
            } catch (Exception ex) {
                Microbot.log("Error in main loop: " + ex.getMessage());
                ex.printStackTrace();
            }
        }, 0, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);

        return true;
    }

    private void setupScript() {
        Microbot.enableAutoRunOn = false;
        Rs2Antiban.resetAntibanSettings();
        Rs2Antiban.antibanSetupTemplates.applyGeneralBasicSetup();
        Rs2AntibanSettings.simulateAttentionSpan = true;
        Rs2AntibanSettings.nonLinearIntervals = true;
        Rs2AntibanSettings.contextualVariability = true;
        Rs2AntibanSettings.usePlayStyle = true;
        Rs2Antiban.setActivity(Activity.ALCHING);
        startTimeForThisRun = System.currentTimeMillis();
        scheduleNextAfk();
    }

    private void executeAlchingLogic() throws InterruptedException {
        if (!hasRequiredItems()) {
            shutdown();
            return;
        }

        if (state == null) {
            Microbot.showMessage("Unable to evaluate state");
            shutdown();
            return;
        }

        if (state == MagicState.CASTING) {
            Rs2ItemModel alchItem = findAlchItem();

            if (handleMissingItems(alchItem) || handleMissingRunes()) {
                return;
            }

            handleNaturalMouseMovement(alchItem);
            performAlchemy(alchItem);
        }
    }

    @Override
    public void shutdown() {
        Rs2Antiban.resetAntibanSettings();
        resetAll();
        shutdownRequested.set(false);
        super.shutdown();
    }
}