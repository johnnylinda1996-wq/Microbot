package net.runelite.microbot.mule.bridge;

import net.runelite.microbot.mule.bridge.gui.MuleBridgeGUI;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.Banner;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.swing.*;

/**
 * Main Spring Boot application for the Mule Bridge
 */
@SpringBootApplication
@EnableScheduling
public class MuleBridgeApplication {

    public static void main(String[] args) {
        // Set system properties for better GUI experience
        System.setProperty("java.awt.headless", "false");
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        // Configure Spring Boot for GUI mode
        SpringApplication app = new SpringApplication(MuleBridgeApplication.class);
        app.setBannerMode(Banner.Mode.OFF); // Disable banner for cleaner GUI log
        app.setHeadless(false);

        // Start GUI on EDT
        SwingUtilities.invokeLater(() -> {
            try {
                MuleBridgeGUI gui = new MuleBridgeGUI(app);
                gui.setVisible(true);
            } catch (Exception e) {
                System.err.println("========================================");
                System.err.println("  ERROR STARTING MULE BRIDGE GUI!");
                System.err.println("========================================");
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace();
                System.err.println("========================================");

                // Fallback to console mode
                System.out.println("Starting in console mode...");
                try {
                    app.run(args);
                } catch (Exception fallbackError) {
                    System.err.println("Console mode also failed: " + fallbackError.getMessage());
                    System.exit(1);
                }
            }
        });
    }
}
