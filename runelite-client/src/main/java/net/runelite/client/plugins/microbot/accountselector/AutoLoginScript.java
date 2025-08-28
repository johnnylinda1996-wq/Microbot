package net.runelite.client.plugins.microbot.accountselector;

import net.runelite.api.GameState;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.security.Login;

import java.util.concurrent.TimeUnit;

public class AutoLoginScript extends Script {

    private long loginAttemptTime = -1;
    private boolean waitingToLogin = false;

    public boolean run(AutoLoginConfig autoLoginConfig) {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!super.run()) return;

                if (Microbot.getClient().getGameState() == GameState.LOGIN_SCREEN) {

                    // Check if Wait Before Login is enabled
                    if (autoLoginConfig.WaitBeforeLogin()) {
                        handleDelayedLogin(autoLoginConfig);
                    } else {
                        // Original functionality - direct login
                        performLogin(autoLoginConfig);
                    }
                }

            } catch (Exception ex) {
                Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return true;
    }

    private void handleDelayedLogin(AutoLoginConfig autoLoginConfig) {
        long currentTime = System.currentTimeMillis();

        // If we haven't started waiting yet, set the login time
        if (loginAttemptTime == -1) {
            loginAttemptTime = currentTime + (autoLoginConfig.MinutesbeforeLogin() * 60 * 1000L);
            waitingToLogin = true;
            Microbot.log("Waiting " + autoLoginConfig.MinutesbeforeLogin() + " minutes before logging in...");
        }

        // Check if it's time to login
        if (waitingToLogin && currentTime >= loginAttemptTime) {
            Microbot.log("Wait period completed. Logging in now...");
            performLogin(autoLoginConfig);

            // Reset the wait state
            loginAttemptTime = -1;
            waitingToLogin = false;
        } else if (waitingToLogin) {
            // Still waiting - show remaining time every 30 seconds
            long remainingTime = loginAttemptTime - currentTime;
            long remainingMinutes = remainingTime / (60 * 1000);

            if (remainingTime % 30000 < 1000) { // Every ~30 seconds
                Microbot.log("Login in " + remainingMinutes + " minutes and " +
                        ((remainingTime % (60 * 1000)) / 1000) + " seconds...");
            }
        }
    }

    private void performLogin(AutoLoginConfig autoLoginConfig) {
        if (autoLoginConfig.useRandomWorld()) {
            new Login(Login.getRandomWorld(autoLoginConfig.isMember()));
        } else {
            new Login(autoLoginConfig.world());
        }
        sleep(5000);
    }
}