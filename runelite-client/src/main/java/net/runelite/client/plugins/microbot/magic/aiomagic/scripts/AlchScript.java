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
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
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

public class AlchScript extends Script {
    private MagicState state = MagicState.CASTING;
    private final AIOMagicPlugin plugin;
    @Inject private AIOMagicConfig config;

    // ---- Stats ----
    public static int totalProfit = 0;
    public static int totalAlched = 0;

    // ---- Timekeeping (pause-aware) ----
    private static long accumulatedActiveTime = 0L;
    private static long startTimeForThisRun = 0L;
    private static boolean paused = false;
    private static long pauseStartTime = 0L;

    // ---- AFK scheduling (static so overlay can read it) ----
    private static long nextAfkTime = 0L;
    private static long afkEndTime = 0L;
    private static boolean afkPlanned = false;
    private static boolean loginInProgress = false; // NEW: Track login attempts

    @Inject
    public AlchScript(AIOMagicPlugin plugin) {
        this.plugin = plugin;
    }

    /* ---------------- Public static helpers for overlay ---------------- */

    public static boolean isPaused() { return paused; }

    public static String getRuntime() {
        long elapsed = accumulatedActiveTime + (paused
                ? (pauseStartTime - startTimeForThisRun)
                : (System.currentTimeMillis() - startTimeForThisRun));
        return formatHHMMSS(elapsed);
    }

    public static int getProfitPerHour() {
        long activeMs = accumulatedActiveTime + (paused
                ? (pauseStartTime - startTimeForThisRun)
                : (System.currentTimeMillis() - startTimeForThisRun));
        double hours = activeMs / 3_600_000.0;
        return hours > 0 ? (int) (totalProfit / hours) : 0;
    }

    public static int getAlchesPerHour() {
        long activeMs = accumulatedActiveTime + (paused
                ? (pauseStartTime - startTimeForThisRun)
                : (System.currentTimeMillis() - startTimeForThisRun));
        double hours = activeMs / 3_600_000.0;
        return hours > 0 ? (int) (totalAlched / hours) : 0;
    }

    public static String formatGP(int gp) {
        if (gp >= 1_000_000) return String.format("%.1fM", gp / 1_000_000.0);
        if (gp >= 1_000)     return String.format("%.1fK", gp / 1_000.0);
        return String.valueOf(gp);
    }

    // Nieuwe methode om AFK aan te zetten zonder meteen te pauzeren
    public static void enableAfkOverlayOnly(AIOMagicConfig config) {
        if (!config.afkEnabled()) {
            afkPlanned = false;
            nextAfkTime = 0L;
            afkEndTime = 0L;
            return;
        }

        long intervalMs = ThreadLocalRandom.current().nextLong(
                TimeUnit.MINUTES.toMillis(Math.max(0, config.afkIntervalMin())),
                TimeUnit.MINUTES.toMillis(Math.max(config.afkIntervalMin(), config.afkIntervalMax())) + 1
        );
        long durationMs = ThreadLocalRandom.current().nextLong(
                TimeUnit.MINUTES.toMillis(Math.max(0, config.afkDurationMin())),
                TimeUnit.MINUTES.toMillis(Math.max(config.afkDurationMin(), config.afkDurationMax())) + 1
        );

        nextAfkTime = System.currentTimeMillis() + intervalMs;
        afkEndTime = nextAfkTime + durationMs;
        afkPlanned = true;
    }

    public static boolean isAfkScheduled() { return afkPlanned && nextAfkTime > 0; }

    public static boolean isInAfkBreak() {
        return afkPlanned && paused && System.currentTimeMillis() < afkEndTime;
    }

    public static long getMillisUntilNextAfk() {
        long now = System.currentTimeMillis();
        if (!isAfkScheduled()) return 0;
        return Math.max(0, nextAfkTime - now);
    }

    public static long getPlannedAfkDurationMillis() {
        if (!isAfkScheduled()) return 0;
        return Math.max(0, afkEndTime - nextAfkTime);
    }

    public static long getCurrentBreakRemainingMillis() {
        long now = System.currentTimeMillis();
        if (!isInAfkBreak()) return 0;
        return Math.max(0, afkEndTime - now);
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

    // Update window title with break info
    private static void updateWindowTitle() {
        try {
            String baseTitle = "Microbot";
            String newTitle = baseTitle;

            // Only show break duration when we're actually logged out during a break
            if (isInAfkBreak() && !Microbot.isLoggedIn()) {
                long remaining = getCurrentBreakRemainingMillis();
                String timeLeft = formatMMSS(remaining);
                newTitle = baseTitle + " - AFK Break: " + timeLeft + " remaining";
            } else {
                // Reset to normal title when logged in or no break
                newTitle = baseTitle;
            }

            // Method 1: Try to update via client canvas
            try {
                // Get canvas via proper Microbot client method
                Canvas canvas = Microbot.getClient().getCanvas();
                Window window = SwingUtilities.getWindowAncestor(canvas);

                if (window instanceof JFrame) {
                    JFrame frame = (JFrame) window;
                    frame.setTitle(newTitle);
                } else if (window instanceof Frame) {
                    Frame frame = (Frame) window;
                    frame.setTitle(newTitle);
                }
            } catch (Exception e1) {
                // Method 2: Try alternative approach via active windows
                try {
                    Frame[] frames = Frame.getFrames();
                    for (Frame frame : frames) {
                        if (frame.getTitle().contains("Microbot") || frame.getTitle().contains("RuneLite") || frame.getTitle().contains("AFK Break")) {
                            frame.setTitle(newTitle);
                            break;
                        }
                    }
                } catch (Exception e2) {
                    // Method 3: System property approach (fallback)
                    try {
                        System.setProperty("runelite.title", newTitle);
                        System.setProperty("microbot.title", newTitle);
                    } catch (Exception e3) {
                        // Silent fail
                    }
                }
            }

        } catch (Exception e) {
            // Silent fail - don't spam logs with title update errors
        }
    }

    /* ---------------- Pause/Resume/Reset ---------------- */

    public static void resetAll() {
        totalProfit = 0;
        totalAlched = 0;
        accumulatedActiveTime = 0L;
        paused = false;
        pauseStartTime = 0L;
        startTimeForThisRun = System.currentTimeMillis();
        afkPlanned = false;
        nextAfkTime = 0L;
        afkEndTime = 0L;
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
        return Rs2Player.getBoostedSkillLevel(Skill.MAGIC) >= 55 ? Rs2Spells.HIGH_LEVEL_ALCHEMY : Rs2Spells.LOW_LEVEL_ALCHEMY;
    }

    private void scheduleNextAfk() {
        if (!config.afkEnabled()) {
            afkPlanned = false;
            nextAfkTime = 0L;
            afkEndTime = 0L;
            return;
        }

        long intervalMs = ThreadLocalRandom.current().nextLong(
                TimeUnit.MINUTES.toMillis(Math.max(0, config.afkIntervalMin())),
                TimeUnit.MINUTES.toMillis(Math.max(config.afkIntervalMin(), config.afkIntervalMax())) + 1
        );
        long durationMs = ThreadLocalRandom.current().nextLong(
                TimeUnit.MINUTES.toMillis(Math.max(0, config.afkDurationMin())),
                TimeUnit.MINUTES.toMillis(Math.max(config.afkDurationMin(), config.afkDurationMax())) + 1
        );
        nextAfkTime = System.currentTimeMillis() + intervalMs;
        afkEndTime = nextAfkTime + durationMs;
        afkPlanned = true;
    }

    @Override
    public boolean run() {
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

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!super.run()) return;

                long now = System.currentTimeMillis();

                // Update window title with break status
                updateWindowTitle();
                //checkt of de speler in instance is
                if (!Rs2Player.IsInInstance() && Rs2Inventory.hasItem(8013) && config.alchinpoh()) {
                    Rs2Inventory.interact(8013, "Inside");
                    Thread.sleep(3000);
                } else {
                    Microbot.showMessage("You need a player owned house and tele to house teletabs.");
                    shutdown();

                }
                // --- AFK handling ---
                if (afkPlanned) {
                    // Start break
                    if (!paused && now >= nextAfkTime && now < afkEndTime) {
                        pause(); // zet script op pauze

                        if (config.logoutEnabled() && Microbot.isLoggedIn()) {
                            Rs2Player.logout();
                            Microbot.log("Break gestart: uitgelogd");
                        } else {
                            Microbot.log("AFK gestart: alleen pauze");
                        }

                        // Timer-loop voor console countdown
                        new Thread(() -> {
                            while (paused && System.currentTimeMillis() < afkEndTime) {
                                long remaining = afkEndTime - System.currentTimeMillis();
                                long sec = remaining / 1000;
                                Microbot.log("Logging back in: " + sec + " seconds.");
                                try {
                                    Thread.sleep(10000); // Log every 10 seconds instead of every second
                                } catch (InterruptedException ignored) {
                                    break;
                                }
                            }
                        }).start();
                    }

                    // FIXED: AFK break is over - handle login properly
                    if (paused && now >= afkEndTime) {
                        Microbot.log("Break voorbij - checking login status");

                        // Check if we need to login
                        if (!Microbot.isLoggedIn() && config.logoutEnabled() && !loginInProgress) {
                            loginInProgress = true;
                            Microbot.log("Attempting to login...");

                            // Use a separate thread for login to avoid blocking
                            new Thread(() -> {
                                try {
                                    Login login = new Login(config.world());
                                    boolean loginSuccess = false;
                                    int attempts = 0;
                                    int maxAttempts = 5;

                                    while (!loginSuccess && attempts < maxAttempts) {
                                        attempts++;
                                        Microbot.log("Login attempt " + attempts + "/" + maxAttempts);

                                        try {
                                            // Wait for game to be ready
                                            Thread.sleep(2000);

                                            // Check if we're already logged in (maybe login screen disappeared)
                                            if (Microbot.isLoggedIn()) {
                                                loginSuccess = true;
                                                Microbot.log("Already logged in!");
                                                break;
                                            }

                                            //login
                                            new Login(config.world());

                                            // Wait a bit and check result
                                            Thread.sleep(3000);
                                            if (Microbot.isLoggedIn()) {
                                                loginSuccess = true;
                                                Microbot.log("Login successful!");
                                            }

                                        } catch (Exception e) {
                                            Microbot.log("Login attempt failed: " + e.getMessage());
                                            Thread.sleep(2000);
                                        }
                                    }

                                    if (!loginSuccess) {
                                        Microbot.log("Failed to login after " + maxAttempts + " attempts");
                                    }

                                    // FIXED: Resume script after login attempt (whether successful or not)
                                    if (loginSuccess || !config.logoutEnabled()) {
                                        // Give a moment for the game to fully load
                                        Thread.sleep(2000);
                                        resume();
                                        scheduleNextAfk();
                                        Microbot.log("Script resumed after login attempt");
                                    }

                                } catch (Exception e) {
                                    Microbot.log("Login error: " + e.getMessage());
                                    // Still resume even on error
                                    resume();
                                    scheduleNextAfk();
                                } finally {
                                    loginInProgress = false;
                                }
                            }).start();
                        } else if (Microbot.isLoggedIn() || !config.logoutEnabled()) {
                            // No login needed, just resume
                            resume();
                            scheduleNextAfk();
                            Microbot.log("Script resumed after AFK break (no login needed)");
                        }
                    }
                }

                // Exit early if paused or not logged in
                if (paused || !Microbot.isLoggedIn()) return;

                // --- Alching logic ---
                if (plugin.getAlchItemNames().isEmpty()) {
                    Microbot.showMessage("Alch Item list is empty");
                    shutdown();
                    return;
                }

                if (state == null) {
                    Microbot.showMessage("Unable to evaluate state");
                    shutdown();
                    return;
                }

                switch (state) {
                    case CASTING:
                        Rs2ItemModel alchItem = plugin.getAlchItemNames().stream()
                                .filter(Rs2Inventory::hasItem)
                                .map(Rs2Inventory::get)
                                .findFirst()
                                .orElse(null);

                        if (alchItem == null) {
                            Microbot.log("Missing alch items...");
                            if (config.outofalch()) {
                                Microbot.log("Logging out in 5 seconds...");
                                shutdown();//zet bot uit voor logout actie word uitgevoerd
                                try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
                                Rs2Player.logout();
                            }
                            return;
                        }

                        if (!Rs2Magic.hasRequiredRunes(plugin.getAlchSpell())) {
                            Microbot.log("Unable to cast alchemy spell");
                            if (config.outofalch()) {
                                Microbot.log("Logging out in 5 seconds...");
                                shutdown();//zet bot uit voor logout actie word uitgevoerd
                                try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
                                Rs2Player.logout();
                            }
                            return;
                        }

                        // --- natural mouse / slot handling ---
                        if (Rs2AntibanSettings.naturalMouse) {
                            int targetSlot = (getAlchSpell() == Rs2Spells.HIGH_LEVEL_ALCHEMY) ? 12 : 4;

                            if (alchItem.getSlot() != targetSlot) {
                                Rs2Inventory.moveItemToSlot(alchItem, targetSlot);

                                // wacht tot item op juiste slot staat (max 1 seconde)
                                long start = System.currentTimeMillis();
                                while (alchItem.getSlot() != targetSlot && System.currentTimeMillis() - start < 1000) {
                                    Thread.sleep(20);
                                    alchItem = Rs2Inventory.get(alchItem.getId()); // refresh reference
                                    if (alchItem == null) break; // Safety check
                                }

                                // pas daarna verder met alchen
                                if (alchItem == null || alchItem.getSlot() != targetSlot) {
                                    return;
                                }
                            }
                        }

                        Rs2Magic.alch(alchItem);
                        Rs2Player.waitForXpDrop(Skill.MAGIC, 10000, false);

                        totalAlched++;

                        int profitPerAlch;
                        try {
                            profitPerAlch = Integer.parseInt(config.alchprofit());
                        } catch (NumberFormatException e) {
                            profitPerAlch = 0;
                        }
                        totalProfit += profitPerAlch;
                        break;
                }

            } catch (Exception ex) {
                System.out.println("Error in main loop: " + ex.getMessage());
                ex.printStackTrace();
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return true;
    }

    @Override
    public void shutdown() {
        Rs2Antiban.resetAntibanSettings();
        resetAll();
        super.shutdown();
    }
}