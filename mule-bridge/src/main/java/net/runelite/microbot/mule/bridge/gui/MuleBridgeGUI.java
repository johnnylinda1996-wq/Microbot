package net.runelite.microbot.mule.bridge.gui;

import com.formdev.flatlaf.FlatDarculaLaf;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * GUI for the Mule Bridge with log console and on/off functionality
 */
public class MuleBridgeGUI extends JFrame {
    private JTextArea logArea;
    private JButton toggleButton;
    private JButton hideButton;
    private JLabel statusLabel;
    private ConfigurableApplicationContext springContext;
    private boolean isRunning = false;
    private SpringApplication springApp;
    private PrintStream originalOut;
    private PrintStream originalErr;
    private TrayIcon trayIcon;
    private SystemTray systemTray;

    public MuleBridgeGUI(SpringApplication springApp) {
        this.springApp = springApp;
        this.originalOut = System.out;
        this.originalErr = System.err;

        initializeGUI();
        setupSystemTray();
        setupSystemOutRedirect();
    }

    private void initializeGUI() {
        // Set look and feel
        try {
            UIManager.setLookAndFeel(new FlatDarculaLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }

        setTitle("Microbot Mule Bridge - Control Panel");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Top panel with controls
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        toggleButton = new JButton("Start Bridge");
        toggleButton.setPreferredSize(new Dimension(120, 35));
        toggleButton.addActionListener(new ToggleActionListener());

        hideButton = new JButton("Hide to Tray");
        hideButton.setPreferredSize(new Dimension(120, 35));
        hideButton.addActionListener(e -> hideToTray());

        statusLabel = new JLabel("Status: Stopped");
        statusLabel.setForeground(Color.RED);
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        topPanel.add(toggleButton);
        topPanel.add(Box.createHorizontalStrut(10));
        topPanel.add(hideButton);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(statusLabel);

        // Log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setBackground(new Color(43, 43, 43));
        logArea.setForeground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Log Console"));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // Bottom panel with info
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        JLabel infoLabel = new JLabel("Bridge URL: http://localhost:8080 | WebSocket: ws://localhost:8080/ws/updates");
        infoLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        bottomPanel.add(infoLabel);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // Window close handler
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int option = JOptionPane.showConfirmDialog(
                        MuleBridgeGUI.this,
                        "Do you want to exit the application or minimize to tray?",
                        "Exit Confirmation",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null
                );

                if (option == JOptionPane.YES_OPTION) {
                    // Exit application
                    if (isRunning && springContext != null) {
                        stopBridge();
                    }
                    cleanupAndExit();
                } else if (option == JOptionPane.NO_OPTION) {
                    // Minimize to tray
                    hideToTray();
                }
                // CANCEL_OPTION does nothing
            }
        });

        logMessage("GUI initialized. Click 'Start Bridge' to begin.");
    }

    private void setupSystemTray() {
        if (!SystemTray.isSupported()) {
            logMessage("System tray is not supported on this system.");
            hideButton.setEnabled(false);
            return;
        }

        systemTray = SystemTray.getSystemTray();

        // Create tray icon image
        Image image = createTrayIcon();

        // Create popup menu for tray icon
        PopupMenu popup = new PopupMenu();

        MenuItem showItem = new MenuItem("Show Bridge");
        showItem.addActionListener(e -> showFromTray());

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(e -> {
            if (isRunning && springContext != null) {
                stopBridge();
            }
            cleanupAndExit();
        });

        popup.add(showItem);
        popup.addSeparator();
        popup.add(exitItem);

        trayIcon = new TrayIcon(image, "Microbot Mule Bridge", popup);
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(e -> showFromTray());

        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showFromTray();
                }
            }
        });
    }

    private Image createTrayIcon() {
        int size = 16;
        Image image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = (Graphics2D) image.getGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw a simple bridge icon
        g2d.setColor(isRunning ? Color.GREEN : Color.RED);
        g2d.fillOval(2, 2, size - 4, size - 4);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 8));
        g2d.drawString("B", 6, 12);

        g2d.dispose();
        return image;
    }

    private void hideToTray() {
        if (systemTray == null) {
            setExtendedState(JFrame.ICONIFIED);
            return;
        }

        try {
            systemTray.add(trayIcon);
            setVisible(false);
            logMessage("Application minimized to system tray. Double-click tray icon to restore.");
        } catch (AWTException e) {
            logMessage("Unable to add to system tray: " + e.getMessage());
            setExtendedState(JFrame.ICONIFIED);
        }
    }

    private void showFromTray() {
        if (systemTray != null && trayIcon != null) {
            systemTray.remove(trayIcon);
        }
        setVisible(true);
        setExtendedState(JFrame.NORMAL);
        toFront();
        requestFocus();
    }

    private void cleanupAndExit() {
        if (systemTray != null && trayIcon != null) {
            systemTray.remove(trayIcon);
        }
        System.exit(0);
    }

    private void setupSystemOutRedirect() {
        // Create custom PrintStream to capture output
        PrintStream customOut = new PrintStream(new ByteArrayOutputStream() {
            @Override
            public void flush() {
                String output = toString();
                if (!output.trim().isEmpty()) {
                    SwingUtilities.invokeLater(() -> {
                        logArea.append(output);
                        logArea.setCaretPosition(logArea.getDocument().getLength());
                    });
                    reset();
                }
                originalOut.print(output);
            }
        });

        PrintStream customErr = new PrintStream(new ByteArrayOutputStream() {
            @Override
            public void flush() {
                String output = toString();
                if (!output.trim().isEmpty()) {
                    SwingUtilities.invokeLater(() -> {
                        logArea.append("[ERROR] " + output);
                        logArea.setCaretPosition(logArea.getDocument().getLength());
                    });
                    reset();
                }
                originalErr.print(output);
            }
        });

        System.setOut(customOut);
        System.setErr(customErr);
    }

    private void logMessage(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + timestamp + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private class ToggleActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (isRunning) {
                stopBridge();
            } else {
                startBridge();
            }
        }
    }

    private void startBridge() {
        toggleButton.setEnabled(false);
        toggleButton.setText("Starting...");

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    logMessage("Starting bridge...");
                    springContext = springApp.run();
                    return null;
                } catch (Exception e) {
                    throw e;
                }
            }

            @Override
            protected void done() {
                try {
                    get(); // Check for exceptions
                    isRunning = true;
                    toggleButton.setText("Stop Bridge");
                    toggleButton.setEnabled(true);
                    statusLabel.setText("Status: Running");
                    statusLabel.setForeground(Color.GREEN);
                    logMessage("Bridge started successfully!");

                    // Update tray icon if available
                    if (trayIcon != null) {
                        trayIcon.setImage(createTrayIcon());
                    }
                } catch (Exception e) {
                    toggleButton.setText("Start Bridge");
                    toggleButton.setEnabled(true);
                    statusLabel.setText("Status: Error");
                    statusLabel.setForeground(Color.RED);
                    logMessage("Error starting bridge: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    private void stopBridge() {
        toggleButton.setEnabled(false);
        toggleButton.setText("Stopping...");

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    logMessage("Stopping bridge...");
                    if (springContext != null) {
                        springContext.close();
                        springContext = null;
                    }
                    return null;
                } catch (Exception e) {
                    throw e;
                }
            }

            @Override
            protected void done() {
                try {
                    get(); // Check for exceptions
                    isRunning = false;
                    toggleButton.setText("Start Bridge");
                    toggleButton.setEnabled(true);
                    statusLabel.setText("Status: Stopped");
                    statusLabel.setForeground(Color.RED);
                    logMessage("Bridge stopped successfully!");

                    // Update tray icon if available
                    if (trayIcon != null) {
                        trayIcon.setImage(createTrayIcon());
                    }
                } catch (Exception e) {
                    toggleButton.setText("Stop Bridge");
                    toggleButton.setEnabled(true);
                    statusLabel.setText("Status: Error");
                    statusLabel.setForeground(Color.RED);
                    logMessage("Error stopping bridge: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }
}
