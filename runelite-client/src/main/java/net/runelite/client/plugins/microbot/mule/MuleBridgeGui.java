package net.runelite.client.plugins.microbot.mule;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class MuleBridgeGui extends JFrame {

    private final MuleConfig config;
    private final MuleScript muleScript;
    private final HttpClient httpClient;

    private JLabel bridgeStatusLabel;
    private JLabel currentStateLabel;
    private JLabel currentRequestLabel;
    private JButton testLocationButton;
    private JButton refreshButton;

    private Timer refreshTimer;

    public MuleBridgeGui(MuleConfig config, MuleScript muleScript) {
        this.config = config;
        this.muleScript = muleScript;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        initializeGui();
        startRefreshTimer();
    }

    private void initializeGui() {
        setTitle("Mule Bridge - Status");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setSize(500, 400);
        setLocationRelativeTo(null);

        // Create main panel
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Status panel
        JPanel statusPanel = new JPanel(new GridBagLayout());
        statusPanel.setBorder(BorderFactory.createTitledBorder("Bridge Status"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Bridge URL
        gbc.gridx = 0; gbc.gridy = 0;
        statusPanel.add(new JLabel("Bridge URL:"), gbc);
        gbc.gridx = 1;
        statusPanel.add(new JLabel(config.bridgeUrl()), gbc);

        // Bridge Status
        gbc.gridx = 0; gbc.gridy = 1;
        statusPanel.add(new JLabel("Status:"), gbc);
        gbc.gridx = 1;
        bridgeStatusLabel = new JLabel("Checking...");
        bridgeStatusLabel.setForeground(Color.ORANGE);
        statusPanel.add(bridgeStatusLabel, gbc);

        // Current State
        gbc.gridx = 0; gbc.gridy = 2;
        statusPanel.add(new JLabel("Mule State:"), gbc);
        gbc.gridx = 1;
        currentStateLabel = new JLabel("WAITING");
        statusPanel.add(currentStateLabel, gbc);

        // Current Request
        gbc.gridx = 0; gbc.gridy = 3;
        statusPanel.add(new JLabel("Current Request:"), gbc);
        gbc.gridx = 1;
        currentRequestLabel = new JLabel("None");
        statusPanel.add(currentRequestLabel, gbc);

        mainPanel.add(statusPanel, BorderLayout.NORTH);

        // Location testing panel with separate X, Y, Z fields
        JPanel locationPanel = new JPanel(new GridBagLayout());
        locationPanel.setBorder(BorderFactory.createTitledBorder("Location Tester"));
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // X coordinate field
        gbc.gridx = 0; gbc.gridy = 0;
        locationPanel.add(new JLabel("X:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextField xField = new JTextField("3164", 6);
        locationPanel.add(xField, gbc);

        // Y coordinate field
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE;
        locationPanel.add(new JLabel("Y:"), gbc);
        gbc.gridx = 3; gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextField yField = new JTextField("3486", 6);
        locationPanel.add(yField, gbc);

        // Z coordinate field
        gbc.gridx = 4; gbc.fill = GridBagConstraints.NONE;
        locationPanel.add(new JLabel("Z:"), gbc);
        gbc.gridx = 5; gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextField zField = new JTextField("0", 4);
        locationPanel.add(zField, gbc);

        // Test button
        gbc.gridx = 6; gbc.fill = GridBagConstraints.NONE;
        testLocationButton = new JButton("Test Location");
        testLocationButton.addActionListener(e -> testCoordinates(xField, yField, zField));
        locationPanel.add(testLocationButton, gbc);

        // Preset buttons row
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 7; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel presetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        JButton geButton = new JButton("GE");
        geButton.setToolTipText("Grand Exchange (3164, 3486, 0)");
        geButton.addActionListener(e -> setCoordinates(xField, yField, zField, 3164, 3486, 0));
        presetPanel.add(geButton);

        JButton lumbridgeButton = new JButton("Lumbridge");
        lumbridgeButton.setToolTipText("Lumbridge (3222, 3218, 0)");
        lumbridgeButton.addActionListener(e -> setCoordinates(xField, yField, zField, 3222, 3218, 0));
        presetPanel.add(lumbridgeButton);

        JButton varrockButton = new JButton("Varrock");
        varrockButton.setToolTipText("Varrock West Bank (3185, 3436, 0)");
        varrockButton.addActionListener(e -> setCoordinates(xField, yField, zField, 3185, 3436, 0));
        presetPanel.add(varrockButton);

        JButton faladorButton = new JButton("Falador");
        faladorButton.setToolTipText("Falador (2965, 3378, 0)");
        faladorButton.addActionListener(e -> setCoordinates(xField, yField, zField, 2965, 3378, 0));
        presetPanel.add(faladorButton);

        JButton edgevilleButton = new JButton("Edgeville");
        edgevilleButton.setToolTipText("Edgeville (3094, 3493, 0)");
        edgevilleButton.addActionListener(e -> setCoordinates(xField, yField, zField, 3094, 3493, 0));
        presetPanel.add(edgevilleButton);

        locationPanel.add(presetPanel, gbc);

        // Current config location display
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 7;
        JLabel configLocationLabel = new JLabel(String.format("Config Default: X=%d, Y=%d, Z=%d",
            config.defaultLocationX(), config.defaultLocationY(), config.defaultLocationZ()));
        configLocationLabel.setFont(configLocationLabel.getFont().deriveFont(Font.ITALIC, 10f));
        configLocationLabel.setForeground(Color.GRAY);
        locationPanel.add(configLocationLabel, gbc);

        mainPanel.add(locationPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        refreshButton = new JButton("Refresh Status");
        refreshButton.addActionListener(e -> updateStatus());
        buttonPanel.add(refreshButton);

        JButton closeButton = new JButton("Hide");
        closeButton.addActionListener(e -> setVisible(false));
        buttonPanel.add(closeButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // Handle window closing
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });
    }

    private void startRefreshTimer() {
        refreshTimer = new Timer(5000, e -> updateStatus()); // Refresh every 5 seconds
        refreshTimer.start();
        updateStatus(); // Initial update
    }

    private void updateStatus() {
        SwingUtilities.invokeLater(() -> {
            // Update mule state
            MuleScript.MuleState state = muleScript.getCurrentState();
            currentStateLabel.setText(state.toString());
            currentStateLabel.setForeground(getStateColor(state));

            // Update current request
            MuleRequest request = muleScript.getCurrentRequest();
            if (request != null) {
                currentRequestLabel.setText(request.getId().substring(0, 8) + "... from " + request.getRequesterUsername());
            } else {
                currentRequestLabel.setText("None");
            }
        });

        // Check bridge status in background
        CompletableFuture.supplyAsync(this::checkBridgeHealth)
                .thenAccept(isOnline -> SwingUtilities.invokeLater(() -> {
                    if (isOnline) {
                        bridgeStatusLabel.setText("Online");
                        bridgeStatusLabel.setForeground(Color.GREEN);
                    } else {
                        bridgeStatusLabel.setText("Offline");
                        bridgeStatusLabel.setForeground(Color.RED);
                    }
                }));
    }

    private boolean checkBridgeHealth() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.bridgeUrl() + "/api/mule/health"))
                    .GET()
                    .timeout(Duration.ofSeconds(3))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;

        } catch (Exception e) {
            return false;
        }
    }

    private Color getStateColor(MuleScript.MuleState state) {
        switch (state) {
            case WAITING:
                return Color.ORANGE;
            case LOGGING_IN:
            case WALKING:
            case TRADING:
                return Color.GREEN;
            case LOGGING_OUT:
                return Color.BLUE;
            case ERROR:
                return Color.RED;
            default:
                return Color.BLACK;
        }
    }

    private void testCoordinates(JTextField xField, JTextField yField, JTextField zField) {
        try {
            int x = Integer.parseInt(xField.getText().trim());
            int y = Integer.parseInt(yField.getText().trim());
            int z = Integer.parseInt(zField.getText().trim());

            // Validate coordinates are reasonable for RuneScape
            if (x < 1000 || x > 4000 || y < 1000 || y > 4000 || z < 0 || z > 3) {
                JOptionPane.showMessageDialog(this,
                    "Warning: Coordinates seem unusual for RuneScape!\n" +
                    "Normal range: X: 1000-4000, Y: 1000-4000, Z: 0-3",
                    "Coordinate Warning", JOptionPane.WARNING_MESSAGE);
            } else {
                String message = String.format("Coordinates are valid:\nX: %d, Y: %d, Z: %d\n\nThis location will be used for mule trades.", x, y, z);
                JOptionPane.showMessageDialog(this, message, "Location Test - Valid", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                "Invalid coordinates! Please enter numbers only.",
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setCoordinates(JTextField xField, JTextField yField, JTextField zField, int x, int y, int z) {
        xField.setText(String.valueOf(x));
        yField.setText(String.valueOf(y));
        zField.setText(String.valueOf(z));
    }

    public void showGui() {
        setVisible(true);
    }

    public void hideGui() {
        setVisible(false);
    }

    @Override
    public void dispose() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
        super.dispose();
    }
}
