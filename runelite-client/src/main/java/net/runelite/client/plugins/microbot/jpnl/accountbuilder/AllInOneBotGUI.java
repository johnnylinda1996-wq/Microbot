package net.runelite.client.plugins.microbot.jpnl.accountbuilder;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.enums.QuestType;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.enums.SkillType;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.enums.MinigameType;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.tasks.AioTask;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.tasks.AioSkillTask;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.tasks.AioQuestTask;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.tasks.AioMinigameTask;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.tasks.AioTravelTask; // NEW
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.travel.TravelLocation; // NEW
import net.runelite.api.coords.WorldPoint; // NEW
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.AllInOneConfig; // ADDED IMPORT

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Comparator;
import java.util.prefs.Preferences;

public class AllInOneBotGUI extends JFrame {

    private final AllInOneScript script;
    private final AllInOneConfig config; // NEW: Direct config access for method updates

    // Theme Management (removed LIGHT theme)
    private enum Theme {
        DARK("🌙 Dark Theme", new Color(32, 36, 40), new Color(45, 50, 56), Color.WHITE, new Color(220, 50, 50)),
        BLUE("💙 Blue Theme", new Color(25, 35, 45), new Color(35, 45, 60), Color.WHITE, new Color(100, 150, 255)),
        GREEN("💚 Green Theme", new Color(20, 40, 30), new Color(30, 50, 40), Color.WHITE, new Color(80, 200, 120)),
        RED("❤️ Red Theme", new Color(40, 20, 20), new Color(60, 30, 30), Color.WHITE, new Color(255, 100, 100));

        final String displayName;
        final Color background;
        final Color panelBackground;
        final Color foreground;
        final Color accent;
        final Color hoverColor;

        Theme(String displayName, Color bg, Color panelBg, Color fg, Color accent) {
            this.displayName = displayName;
            this.background = bg;
            this.panelBackground = panelBg;
            this.foreground = fg;
            this.accent = accent;
            this.hoverColor = accent.brighter();
        }
    }

    private Theme currentTheme = Theme.DARK;
    private Preferences prefs = Preferences.userNodeForPackage(AllInOneBotGUI.class);

    // Layout visibility options
    private boolean showSkillPanel = true;
    private boolean showQuestPanel = true;
    private boolean showMinigamePanel = true;
    private boolean showStatusPanel = true;
    private boolean showControlPanel = true;
    private boolean compactMode = true;

    // Position preservation variables
    private boolean preserveLocation = true;
    private Point originalLocation;

    private JComboBox<SkillType> skillCombo;
    private JSpinner targetLevelSpinner;
    private JComboBox<Object> trainingMethodCombo; // NEW: Dynamic training method selector
    private JButton addSkillButton;

    private JComboBox<QuestType> questCombo;
    private JButton addQuestButton;
    private JComboBox<MinigameType> minigameCombo;
    private JSpinner minigameDurationSpinner; // NEW: Duration for minigames
    private JButton addMinigameButton;
    // --- Travel task components (NEW) ---
    private JComboBox<Object> travelCombo; // TravelLocation values + group labels + "Custom..." (NEW refactored)
    private JButton addTravelButton;

    private JButton removeSelectedButton;
    private JButton startButton;
    private JButton pauseButton;
    private JButton clearButton;
    private JButton refreshButton;
    private JLabel currentTaskLabel;
    private JLabel statusLabel;
    private JLabel levelLabel;
    private JLabel xpGainedLabel;
    private JLabel xpPerHourLabel;
    private JLabel xpRemainingLabel;
    private JProgressBar progressBar;
    private JLabel elapsedLabel;
    // Added task counters
    private JLabel tasksSummaryLabel;
    // Filter
    // private JTextField filterField;

    private final Timer uiTimer;
    private final Timer queueListLiteTimer; // NEW: lightweight list-only refresh timer

    private JRadioButton levelModeRadio;
    private JRadioButton timeModeRadio;
    private JSpinner minutesSpinner;
    private JButton editSelectedButton;
    private JButton moveUpButton;
    private JButton moveDownButton;
    private JButton skipButton;
    private JButton hideButton;
    private SystemTray tray;
    private TrayIcon trayIcon;

    // New queue utility buttons
    private JButton saveButton;
    private JButton loadButton;
    private JButton shuffleButton;
    //    private JButton duplicateButton;
    //    private JButton topButton;
    //    private JButton bottomButton;

    // New model typed entries
    private DefaultListModel<TaskListEntry> taskModel;
    private JList<TaskListEntry> taskList;

    // Quest icon
    private static Icon QUEST_ICON;
    private static final Map<MinigameType, Icon> MINIGAME_ICONS = new EnumMap<>(MinigameType.class);
    private static Icon MINIGAME_FALLBACK_ICON;

    // Icon cache for skills
    private static final Map<SkillType, Icon> SKILL_ICONS = new EnumMap<>(SkillType.class);
    private static final int SKILL_ICON_SIZE = 16;
    private static final int QUEST_ICON_SIZE = 18;
    private static final int MINIGAME_ICON_SIZE = 18;

    private JLabel typeCountsLabel;

    // Icon cache for control buttons
    private Icon playIcon, pauseIcon, skipIcon, hideIcon, saveIconSmall, loadIconSmall, shuffleIcon;

    // Travel icons
    private static final Map<TravelLocation, Icon> TRAVEL_ICONS = new EnumMap<>(TravelLocation.class); // NEW
    private static Icon TRAVEL_CUSTOM_ICON; // NEW

    // Keep panel references for language/theme updates
    private JPanel skillPanelRef, questPanelRef, minigamePanelRef, queuePanelRef, controlPanelRef, statusPanelRef;
    private JPanel travelPanelRef;
    // Inline skill options container and component registry
    private JPanel skillOptionsPanelInline;
    private Map<String, JComponent> skillOptionsComponentsInline = new HashMap<>();

    // Menu item references for language switching
    private JMenu appearanceMenuRef; // to add language submenu here instead of Help
    private JMenu languageMenuRef;

    private JMenuItem resetLayoutItemRef; // For translation if desired later

    // Store position to prevent unwanted repositioning
    private Point savedLocation;
    private boolean locationInitialized = false;

    // ================== BEGIN MODIFICATIONS ==================
    // Track last known window location to preserve position on any UI refresh/layout changes
    private Point lastKnownLocation = null;
    // ================== END MODIFICATIONS ==================

    public AllInOneBotGUI(AllInOneScript script) {
        super("🚀 Account Builder v0.1");
        this.script = script;
        this.config = script.getConfig(); // Direct config access

        // Must be called before the frame becomes displayable (before pack()/setVisible())
        setUndecorated(true);

        // Set custom icon for the window
        setCustomWindowIcon();

        loadPreferences();
        initComponents();
        createMenuBar();
        layoutComponents();
        attachListeners();
        applyThemeTweaks();
        refreshQueue();

        updateWindowSize();
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE); // Changed to hide when X is clicked

        // Manually center on screen (more reliable for undecorated frames)
        centerOnScreen();

        // Store location after everything is set up
        SwingUtilities.invokeLater(() -> {
            savedLocation = getLocation();
            locationInitialized = true;
        });

        setAlwaysOnTop(false);
        setVisible(true);

        uiTimer = new Timer(1000, e -> refreshStatus());
        uiTimer.start();

        // NEW: lightweight list-only refresh every ~7 seconds (only updates JList model)
        queueListLiteTimer = new Timer(7000, e -> refreshQueueListOnly());
        queueListLiteTimer.start();

        // Add listener to keep last known location updated
        this.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentMoved(java.awt.event.ComponentEvent e) { lastKnownLocation = getLocation(); }
        });
    }

    private void setCustomWindowIcon() {
        try {
            // Try to load a custom icon first
            java.net.URL iconUrl = getClass().getResource("/icons/account_builder_icon.png");
            if (iconUrl != null) {
                ImageIcon icon = new ImageIcon(iconUrl);
                setIconImage(icon.getImage());
            } else {
                // Create a custom icon with "AB" text
                BufferedImage iconImg = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = iconImg.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Create gradient background
                GradientPaint gradient = new GradientPaint(0, 0, new Color(70, 130, 180), 32, 32, new Color(100, 149, 237));
                g2d.setPaint(gradient);
                g2d.fillRoundRect(2, 2, 28, 28, 8, 8);

                // Add border
                g2d.setColor(new Color(47, 79, 79));
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRoundRect(2, 2, 28, 28, 8, 8);

                // Add "AB" text
                g2d.setFont(new Font("Arial", Font.BOLD, 14));
                g2d.setColor(Color.WHITE);
                FontMetrics fm = g2d.getFontMetrics();
                String text = "AB";
                int textWidth = fm.stringWidth(text);
                int textHeight = fm.getAscent();
                g2d.drawString(text, (32 - textWidth) / 2, (32 + textHeight) / 2 - 2);

                g2d.dispose();
                setIconImage(iconImg);
            }
        } catch (Exception e) {
            // Fallback to default icon if custom icon fails
            System.out.println("Could not load custom icon, using default");
        }
    }

    private void loadPreferences() {
        String themeName = prefs.get("theme", Theme.DARK.name());
        try {
            currentTheme = Theme.valueOf(themeName);
        } catch (IllegalArgumentException e) {
            currentTheme = Theme.DARK;
        }

        showSkillPanel = prefs.getBoolean("showSkillPanel", true);
        showQuestPanel = prefs.getBoolean("showQuestPanel", true);
        showMinigamePanel = prefs.getBoolean("showMinigamePanel", true);
        showStatusPanel = prefs.getBoolean("showStatusPanel", true);
        showControlPanel = prefs.getBoolean("showControlPanel", true);
        // Default to compact mode and prefer it over any old saved value
        compactMode = prefs.getBoolean("compactMode", true);

        // Language support removed
    }

    private void savePreferences() {
        prefs.put("theme", currentTheme.name());
        prefs.putBoolean("showSkillPanel", showSkillPanel);
        prefs.putBoolean("showQuestPanel", showQuestPanel);
        prefs.putBoolean("showMinigamePanel", showMinigamePanel);
        prefs.putBoolean("showStatusPanel", showStatusPanel);
        prefs.putBoolean("showControlPanel", showControlPanel);
        prefs.putBoolean("compactMode", compactMode);
        // Language support removed
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Appearance Menu
        JMenu appearanceMenu = new JMenu("🎨 Appearance");
        appearanceMenuRef = appearanceMenu;

        // Theme submenu
        JMenu themeMenu = new JMenu("Themes");
        ButtonGroup themeGroup = new ButtonGroup();
        for (Theme theme : Theme.values()) {
            JRadioButtonMenuItem themeItem = new JRadioButtonMenuItem(theme.displayName);
            themeItem.setSelected(theme == currentTheme);
            themeItem.addActionListener(e -> {
                currentTheme = theme;
                applyThemeTweaks();
                updatePanelVisibility(); // Force a complete UI refresh
                savePreferences();
                SwingUtilities.invokeLater(() -> repaint()); // Ensure repaint happens
            });
            themeGroup.add(themeItem);
            themeMenu.add(themeItem);
        }
        appearanceMenu.add(themeMenu);

        // Layout submenu (compact mode is now the only mode)
        JMenu layoutMenu = new JMenu("Layout Options");

        layoutMenu.addSeparator();

        // Panel visibility options
        JCheckBoxMenuItem skillPanelItem = new JCheckBoxMenuItem("Show Skill Panel");
        skillPanelItem.setSelected(showSkillPanel);
        skillPanelItem.addActionListener(e -> {
            showSkillPanel = skillPanelItem.isSelected();
            updatePanelVisibility();
            savePreferences();
        });
        layoutMenu.add(skillPanelItem);

        JCheckBoxMenuItem questPanelItem = new JCheckBoxMenuItem("Show Quest Panel");
        questPanelItem.setSelected(showQuestPanel);
        questPanelItem.addActionListener(e -> {
            showQuestPanel = questPanelItem.isSelected();
            updatePanelVisibility();
            savePreferences();
        });
        layoutMenu.add(questPanelItem);

        JCheckBoxMenuItem minigamePanelItem = new JCheckBoxMenuItem("Show Minigame Panel");
        minigamePanelItem.setSelected(showMinigamePanel);
        minigamePanelItem.addActionListener(e -> {
            showMinigamePanel = minigamePanelItem.isSelected();
            updatePanelVisibility();
            savePreferences();
        });
        layoutMenu.add(minigamePanelItem);

        JCheckBoxMenuItem statusPanelItem = new JCheckBoxMenuItem("Show Status Panel");
        statusPanelItem.setSelected(showStatusPanel);
        statusPanelItem.addActionListener(e -> {
            showStatusPanel = statusPanelItem.isSelected();
            updatePanelVisibility();
            savePreferences();
        });
        layoutMenu.add(statusPanelItem);

        JCheckBoxMenuItem controlPanelItem = new JCheckBoxMenuItem("Show Control Panel");
        controlPanelItem.setSelected(showControlPanel);
        controlPanelItem.addActionListener(e -> {
            showControlPanel = controlPanelItem.isSelected();
            updatePanelVisibility();
            savePreferences();
        });
        layoutMenu.add(controlPanelItem);

        appearanceMenu.add(layoutMenu);

        // Tools Menu
        JMenu toolsMenu = new JMenu("🔧 Tools");

        JMenuItem resetLayoutItem = new JMenuItem("Reset Layout");
        resetLayoutItemRef = resetLayoutItem;
        resetLayoutItem.addActionListener(e -> resetLayout());
        toolsMenu.add(resetLayoutItem);

        JMenuItem exportQueueItem = new JMenuItem("Export Queue...");
        exportQueueItem.addActionListener(e -> exportQueue());
        toolsMenu.add(exportQueueItem);

        JMenuItem importQueueItem = new JMenuItem("Import Queue...");
        importQueueItem.addActionListener(e -> importQueue());
        toolsMenu.add(importQueueItem);

        // Help Menu
        JMenu helpMenu = new JMenu("❓ Help");

        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAbout());
        helpMenu.add(aboutItem);

        JMenuItem shortcutsItem = new JMenuItem("Keyboard Shortcuts");
        shortcutsItem.addActionListener(e -> showShortcuts());
        helpMenu.add(shortcutsItem);

        // Add working menus directly to the JMenuBar (required for proper menu behavior)
        menuBar.add(appearanceMenu);
        menuBar.add(toolsMenu);
        menuBar.add(helpMenu);

        // Centered title in the menu bar using symmetric fillers that mirror left menu widths
        // Sequence: [Appearance][Tools][Help][leftFiller][Title][rightFiller]
        Component leftFiller = Box.createRigidArea(new Dimension(0, 0));
        menuBar.add(leftFiller);
        JLabel titleLabel;
        if (getIconImage() != null) {
            titleLabel = new JLabel("  Account Builder", new ImageIcon(getIconImage()), JLabel.CENTER);
        } else {
            titleLabel = new JLabel("  Account Builder");
        }
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        titleLabel.setForeground(currentTheme.foreground);
        menuBar.add(titleLabel);

        Component rightFiller = Box.createRigidArea(new Dimension(0, 0));
        menuBar.add(rightFiller);

        Runnable syncCenter = () -> {
            int totalLeftWidth = 0;
            // Sum preferred widths of the first three menus (Appearance, Tools, Help)
            for (int i = 0; i < Math.min(3, menuBar.getMenuCount()); i++) {
                JMenu m = menuBar.getMenu(i);
                if (m != null) {
                    totalLeftWidth += m.getPreferredSize().width;
                }
            }
            // Mirror left menu width on both sides of the title
            Dimension d = new Dimension(totalLeftWidth, 0);
            leftFiller.setPreferredSize(d);
            leftFiller.setMaximumSize(new Dimension(totalLeftWidth, Integer.MAX_VALUE));
            rightFiller.setPreferredSize(d);
            rightFiller.setMaximumSize(new Dimension(totalLeftWidth, Integer.MAX_VALUE));
            menuBar.revalidate();
            menuBar.repaint();
        };
        SwingUtilities.invokeLater(syncCenter);
        menuBar.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) { syncCenter.run(); }
        });

        // Allow dragging the undecorated window by dragging the menu bar
        final Point[] dragOffset = {null};
        java.awt.event.MouseAdapter dragger = new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) { dragOffset[0] = e.getPoint(); }
            @Override public void mouseDragged(java.awt.event.MouseEvent e) {
                if (dragOffset[0] != null) {
                    Point p = e.getLocationOnScreen();
                    setLocation(p.x - dragOffset[0].x, p.y - dragOffset[0].y);
                }
            }
        };
        menuBar.addMouseListener(dragger);
        menuBar.addMouseMotionListener(dragger);
        titleLabel.addMouseListener(dragger);
        titleLabel.addMouseMotionListener(dragger);

        setJMenuBar(menuBar);
    }

    private void updatePanelVisibility() {
        Point currentPos = getLocation();
        layoutComponents();
        applyThemeTweaks();
        updateWindowSize();
        revalidate();
        repaint();
        SwingUtilities.invokeLater(() -> setLocation(currentPos));
    }

    private JComponent wrapLeft(JComponent comp) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        p.setOpaque(false);
        p.add(comp);
        return p;
    }

    private void resetLayout() {
        showSkillPanel = true;
        showQuestPanel = true;
        showMinigamePanel = true;
        showStatusPanel = true;
        showControlPanel = true;
        compactMode = true;
        currentTheme = Theme.DARK;

        taskList.setFixedCellHeight(compactMode ? 20 : 28);
        updatePanelVisibility();
        savePreferences();

        JOptionPane.showMessageDialog(this, "Layout has been reset to default!", "Reset Complete", JOptionPane.INFORMATION_MESSAGE);
    }

    private void exportQueue() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogTitle("Export Queue to TXT file");
        chooser.setSelectedFile(new java.io.File("queue_export.txt"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                java.io.File file = chooser.getSelectedFile();
                if (!file.getName().toLowerCase().endsWith(".txt")) {
                    file = new java.io.File(file.getAbsolutePath() + ".txt");
                }

                // Build export content
                StringBuilder content = new StringBuilder();
                content.append("# Account Builder Pro v2.0 - Queue Export\n");
                content.append("# Created: ").append(new java.util.Date()).append("\n");
                content.append("# Total Tasks: ").append(script.getQueueSnapshotRaw().size()).append("\n\n");

                java.util.List<AioTask> tasks = script.getQueueSnapshotRaw();
                for (int i = 0; i < tasks.size(); i++) {
                    AioTask task = tasks.get(i);
                    content.append(String.format("%d. %s\n", i + 1, formatTaskForExport(task)));
                }

                // Write to file
                try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
                    writer.write(content.toString());
                }

                JOptionPane.showMessageDialog(this,
                        "Queue exported successfully to:\n" + file.getAbsolutePath(),
                        "Export Complete", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Failed to export queue: " + e.getMessage(),
                        "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void importQueue() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogTitle("Import Queue from TXT file");
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(java.io.File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".txt");
            }
            @Override
            public String getDescription() {
                return "Text Files (*.txt)";
            }
        });

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                java.io.File file = chooser.getSelectedFile();

                // Read file content
                StringBuilder content = new StringBuilder();
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                }

                // Parse and import tasks (basic implementation)
                String[] lines = content.toString().split("\n");
                int imported = 0;

                for (String line : lines) {
                    line = line.trim();
                    if (line.startsWith("#") || line.isEmpty()) continue;

                    // Remove line numbers like "1. "
                    if (line.matches("^\\d+\\.\\s+.*")) {
                        line = line.replaceFirst("^\\d+\\.\\s+", "");
                    }

                    // Try to parse different task types
                    if (parseAndAddTask(line)) {
                        imported++;
                    }
                }

                refreshQueue();
                JOptionPane.showMessageDialog(this,
                        String.format("Successfully imported %d tasks from:\n%s", imported, file.getAbsolutePath()),
                        "Import Complete", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Failed to import queue: " + e.getMessage(),
                        "Import Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String formatTaskForExport(AioTask task) {
        if (task instanceof AioSkillTask) {
            AioSkillTask s = (AioSkillTask) task;
            if (s.isTimeMode()) {
                return String.format("SKILL:%s:TIME:%d", s.getSkillType().name(), s.getDurationMinutes());
            } else {
                return String.format("SKILL:%s:LEVEL:%d", s.getSkillType().name(), s.getTargetLevel());
            }
        } else if (task instanceof AioQuestTask) {
            AioQuestTask q = (AioQuestTask) task;
            return String.format("QUEST:%s", q.getQuestType().name());
        } else if (task instanceof AioMinigameTask) {
            AioMinigameTask m = (AioMinigameTask) task;
            return String.format("MINIGAME:%s", m.getMinigameType().name());
        } else if (task instanceof AioTravelTask) { // NEW travel export
            AioTravelTask tr = (AioTravelTask) task;
            if (tr.getTravelLocationOrNull() != null) {
                return "TRAVEL:" + tr.getTravelLocationOrNull().name();
            } else if (tr.getCustomPoint() != null) {
                WorldPoint p = tr.getCustomPoint();
                return "TRAVEL_CUSTOM:" + tr.getCustomName().replace(":","_") + ":" + p.getX() + ":" + p.getY() + ":" + p.getPlane();
            }
        }
        return task.getDisplay();
    }

    private boolean parseAndAddTask(String line) {
        try {
            String[] parts = line.split(":");
            if (parts.length < 2) return false;
            String type = parts[0].toUpperCase();
            if ("SKILL".equals(type) && parts.length >= 4) {
                SkillType skillType = SkillType.valueOf(parts[1]);
                String mode = parts[2];
                int value = Integer.parseInt(parts[3]);

                if ("TIME".equals(mode)) {
                    script.addSkillTaskTime(skillType, value);
                } else if ("LEVEL".equals(mode)) {
                    script.addSkillTask(skillType, value);
                }
                return true;

            } else if ("QUEST".equals(type) && parts.length >= 2) {
                QuestType questType = QuestType.valueOf(parts[1]);
                script.addQuestTask(questType);
                return true;

            } else if ("MINIGAME".equals(type) && parts.length >= 2) {
                MinigameType minigameType = MinigameType.valueOf(parts[1]);
                script.addMinigameTask(minigameType);
                return true;
            } else if ("TRAVEL_CUSTOM".equals(type) && parts.length >= 5) { // NEW parse custom travel
                try {
                    String name = parts[1];
                    int x = Integer.parseInt(parts[2]);
                    int y = Integer.parseInt(parts[3]);
                    int z = Integer.parseInt(parts[4]);
                    script.addTravelTaskCustom(name, new WorldPoint(x,y,z));
                    return true;
                } catch (Exception ignored) {}
            } else if ("TRAVEL".equals(type) && parts.length >= 2) { // NEW parse preset travel
                try {
                    TravelLocation tl = TravelLocation.valueOf(parts[1]);
                    script.addTravelTask(tl);
                    return true;
                } catch (Exception ignored) {}
            }
        } catch (Exception e) { }
        return false;
    }

    private void showAbout() {
        String aboutText = "<html><body style='width: 400px; padding: 15px; font-family: Arial, sans-serif;'>" +
                "<h2 style='color: #ff6b35;'>🔥 Account Builder Pro v2.0</h2>" +
                "<p><b>🚀 Ultimate RuneScape Automation Script</b></p>" +
                "<hr>" +
                "<p><b>✨ Premium Features:</b></p>" +
                "<ul style='margin-left: 10px;'>" +
                "<li>🎯 Advanced skill training with level/time targets</li>" +
                "<li>📜 Quest automation with progress tracking</li>" +
                "<li>🎮 Minigame support with duration control</li>" +
                "<li>📊 Real-time progress monitoring & XP tracking</li>" +
                "<li>💾 Queue save/load functionality</li>" +
                "<li>🔄 Advanced queue management</li>" +
                "<li>⚡ Lightning-fast performance optimization</li>" +
                "<li>🛡️ Anti-ban protection features</li>" +
                "</ul>" +
                "<hr>" +
                "<p><b>👨‍💻 Created by:</b> <span style='color: #4CAF50; font-weight: bold;'>JP96NL & AI</span></p>" +
                "<hr>" +
                "<p style='font-size: 11px; color: #888;'>" +
                "🔥 Built with passion for the OSRS community<br>" +
                "💪 Powered by advanced AI technology<br>" +
                "🎮 For educational purposes only" +
                "</p>" +
                "</body></html>";

        JOptionPane.showMessageDialog(this, aboutText, "About Account Builder Pro", JOptionPane.INFORMATION_MESSAGE);
    }

    // --- Visual/theme helpers ---
    private void applyThemeTweaks() {
        // Apply theme colors immediately
        Color bgPanel = currentTheme.background;
        Color bgAlt = currentTheme.panelBackground;
        Color accent = currentTheme.accent;
        Color fgColor = currentTheme.foreground;
        Font base = getFont() != null ? getFont() : new Font("SansSerif", Font.PLAIN, 12);
        Font bold = base.deriveFont(Font.BOLD, base.getSize2D());

        // Apply theme to main components immediately
        getContentPane().setBackground(bgPanel);

        // Update MenuBar
        JMenuBar menuBar = getJMenuBar();
        if (menuBar != null) {
            menuBar.setBackground(bgAlt);
            menuBar.setForeground(fgColor);
        }

        // Apply to task list immediately
        if (taskList != null) {
            taskList.setBackground(bgAlt);
            taskList.setForeground(fgColor);
            taskList.repaint();
        }

        // Update progress bar
        if (progressBar != null) {
            progressBar.setForeground(accent);
            progressBar.setBackground(bgAlt);
        }

        // Apply button styling
        JButton[] buttons = {addSkillButton, addQuestButton, addMinigameButton, removeSelectedButton,
                startButton, pauseButton, clearButton, refreshButton, editSelectedButton,
                moveUpButton, moveDownButton, skipButton, hideButton, saveButton,
                loadButton, shuffleButton, addTravelButton};

        for (JButton b : buttons) {
            if (b != null) {
                b.setFocusPainted(false);
                b.setBackground(bgAlt.darker());
                b.setForeground(fgColor);
                b.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(bgAlt.brighter(), 1, true),
                        BorderFactory.createEmptyBorder(3,6,3,6)));
                b.setMargin(new Insets(2,6,2,6));
                b.setRolloverEnabled(true);
                b.setOpaque(true);
                installButtonHover(b, bgAlt.darker(), bgAlt, bgAlt.darker().darker());
            }
        }

        // Apply label styling
        JLabel[] labels = {currentTaskLabel, statusLabel, levelLabel, xpGainedLabel,
                xpPerHourLabel, xpRemainingLabel, elapsedLabel, tasksSummaryLabel};

        for (JLabel lab : labels) {
            if (lab != null) {
                lab.setForeground(new Color(220,220,220));
            }
        }

        // Apply combo box styling
        JComboBox<?>[] combos = {skillCombo, questCombo, minigameCombo, trainingMethodCombo, travelCombo};
        for (JComboBox<?> combo : combos) {
            if (combo != null) {
                combo.setBackground(bgAlt);
                combo.setForeground(fgColor);
                combo.setBorder(BorderFactory.createLineBorder(bgAlt.brighter(), 1, true));
            }
        }

        // Apply spinner styling
        JSpinner[] spinners = {targetLevelSpinner, minutesSpinner, minigameDurationSpinner};
        for (JSpinner spinner : spinners) {
            if (spinner != null) {
                spinner.getEditor().getComponent(0).setBackground(bgAlt);
                spinner.getEditor().getComponent(0).setForeground(fgColor);
                spinner.setBorder(BorderFactory.createLineBorder(bgAlt.brighter(), 1, true));
                try {
                    ((JComponent) spinner.getEditor().getComponent(0)).setBorder(BorderFactory.createEmptyBorder(2,4,2,4));
                } catch (Exception ignored) {}
            }
        }

        // Apply to radio buttons
        if (levelModeRadio != null) {
            levelModeRadio.setBackground(bgPanel);
            levelModeRadio.setForeground(fgColor);
        }
        if (timeModeRadio != null) {
            timeModeRadio.setBackground(bgPanel);
            timeModeRadio.setForeground(fgColor);
        }

        // Force immediate visual update
        SwingUtilities.invokeLater(() -> {
            Container root = getContentPane();
            if (root != null) {
                colorize(root, bgPanel, bgAlt, bold);
                revalidate();
                repaint();
            }
        });
    }

    private void colorize(Component c, Color bgPanel, Color bgAlt, Font bold) {
        if (c instanceof JPanel) {
            c.setBackground(bgPanel);
        } else if (c instanceof JScrollPane) {
            c.setBackground(bgPanel);
            ((JScrollPane) c).getViewport().setBackground(bgAlt);
        }
        if (c instanceof JProgressBar) {
            ((JProgressBar) c).setBackground(bgAlt);
        }
        // Ensure any dynamically added controls also receive styling
        if (c instanceof JButton) {
            JButton b = (JButton) c;
            // Skip styling for arrow buttons inside combo/spinner to avoid icon glitches
            boolean insideCombo = SwingUtilities.getAncestorOfClass(JComboBox.class, b) != null;
            boolean insideSpinner = SwingUtilities.getAncestorOfClass(JSpinner.class, b) != null;
            boolean isArrow = b instanceof javax.swing.plaf.basic.BasicArrowButton;
            if (!insideCombo && !insideSpinner && !isArrow) {
                b.setFocusPainted(false);
                b.setBackground(currentTheme.panelBackground.darker());
                b.setForeground(currentTheme.foreground);
                b.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(currentTheme.panelBackground.brighter(), 1),
                        BorderFactory.createEmptyBorder(3,6,3,6)));
                b.setMargin(new Insets(2,6,2,6));
                b.setRolloverEnabled(true);
                b.setOpaque(true);
                installButtonHover(b, currentTheme.panelBackground.darker(), currentTheme.panelBackground, currentTheme.panelBackground.darker().darker());
            }
        } else if (c instanceof JComboBox) {
            JComboBox<?> combo = (JComboBox<?>) c;
            combo.setBackground(currentTheme.panelBackground);
            combo.setForeground(currentTheme.foreground);
        } else if (c instanceof JSpinner) {
            JSpinner sp = (JSpinner) c;
            try {
                Component ed = sp.getEditor().getComponent(0);
                ed.setBackground(currentTheme.panelBackground);
                ed.setForeground(currentTheme.foreground);
            } catch (Exception ignored) {}
        }
        if (c instanceof JComponent) {
            TitledBorder tb = null;
            if ((tb = getTitledBorder((JComponent)c)) != null) {
                tb.setTitleColor(Color.WHITE);
                tb.setTitleFont(bold);
                // Compact interior padding for titled panels (avoid repeated wrapping)
                javax.swing.border.Border current = ((JComponent) c).getBorder();
                if (!(current instanceof javax.swing.border.CompoundBorder)) {
                    ((JComponent) c).setBorder(BorderFactory.createCompoundBorder(
                            current,
                            BorderFactory.createEmptyBorder(2,2,2,2)));
                }
            }
        }
        if (c instanceof Container) {
            for (Component ch : ((Container)c).getComponents()) {
                colorize(ch, bgPanel, bgAlt, bold);
            }
        }
    }

    private TitledBorder getTitledBorder(JComponent comp) {
        if (comp.getBorder() instanceof TitledBorder) return (TitledBorder) comp.getBorder();
        return null;
    }

    // Dialog to choose a TravelLocation for editing a preset travel task
    private TravelLocation showTravelLocationChooser(TravelLocation current) {
        // Build a compact panel with a combo box of TravelLocation values
        JComboBox<TravelLocation> combo = new JComboBox<>(TravelLocation.values());
        combo.setSelectedItem(current);
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = baseGbc();
        panel.add(new JLabel("Location:"), gbc);
        gbc.gridx = 1;
        panel.add(combo, gbc);

        int result = JOptionPane.showConfirmDialog(this, panel, "Edit Travel Task", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            return (TravelLocation) combo.getSelectedItem();
        }
        return null;
    }

    // Hover/pressed visual feedback for buttons (no imports needed due to FQCN)
    private void installButtonHover(final JButton b, final Color normal, final Color hover, final Color pressed) {
        // Avoid duplicate installation using a client property flag
        Object flag = b.getClientProperty("hoverInstalled");
        if (flag instanceof Boolean && (Boolean) flag) {
            return;
        }
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setBackground(hover);
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                b.setBackground(normal);
            }
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                b.setBackground(pressed);
            }
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                // If released inside, show hover; otherwise normal
                java.awt.Point p = e.getPoint();
                if (p.x >= 0 && p.y >= 0 && p.x < b.getWidth() && p.y < b.getHeight()) {
                    b.setBackground(hover);
                } else {
                    b.setBackground(normal);
                }
            }
        });
        b.putClientProperty("hoverInstalled", true);
    }

    private void showShortcuts() {
        String shortcutsText = "<html><body style='width: 350px; padding: 10px;'>" +
                "<h2>⌨️ Keyboard Shortcuts</h2>" +
                "<table>" +
                "<tr><td><b>F1</b></td><td>Start/Resume bot</td></tr>" +
                "<tr><td><b>F2</b></td><td>Pause bot</td></tr>" +
                "<tr><td><b>F3</b></td><td>Skip current task</td></tr>" +
                "<tr><td><b>F5</b></td><td>Refresh queue</td></tr>" +
                "<tr><td><b>Ctrl+S</b></td><td>Save queue</td></tr>" +
                "<tr><td><b>Ctrl+L</b></td><td>Load queue</td></tr>" +
                "<tr><td><b>Delete</b></td><td>Remove selected task</td></tr>" +
                "<tr><td><b>Double Click</b></td><td>Edit task</td></tr>" +
                "<tr><td><b>Right Click</b></td><td>Context menu</td></tr>" +
                "</table>" +
                "</body></html>";

        JOptionPane.showMessageDialog(this, shortcutsText, "Keyboard Shortcuts", JOptionPane.INFORMATION_MESSAGE);
    }

    // NEW: Apply theme to dialog windows
    private void applyDialogTheme(JDialog dialog) {
        if (dialog == null) return;

        Color bgPanel = currentTheme.background;
        Color bgAlt = currentTheme.panelBackground;
        Color fgColor = currentTheme.foreground;

        // Apply theme to dialog
        dialog.getContentPane().setBackground(bgPanel);

        // Apply theme to all components in the dialog
        applyThemeToContainer(dialog.getContentPane(), bgPanel, bgAlt, fgColor);

        dialog.revalidate();
        dialog.repaint();
    }

    // Helper method to recursively apply theme to container components
    private void applyThemeToContainer(Container container, Color bgPanel, Color bgAlt, Color fgColor) {
        for (Component component : container.getComponents()) {
            if (component instanceof JPanel) {
                component.setBackground(bgPanel);
                component.setForeground(fgColor);
            } else if (component instanceof JButton) {
                component.setBackground(bgAlt.darker());
                component.setForeground(fgColor);
            } else if (component instanceof JLabel) {
                component.setForeground(fgColor);
            } else if (component instanceof JTextField) {
                component.setBackground(bgAlt);
                component.setForeground(fgColor);
            } else if (component instanceof JComboBox) {
                component.setBackground(bgAlt);
                component.setForeground(fgColor);
            } else if (component instanceof JSpinner) {
                component.setBackground(bgAlt);
                component.setForeground(fgColor);
            }

            // Recursively apply to nested containers
            if (component instanceof Container) {
                applyThemeToContainer((Container) component, bgPanel, bgAlt, fgColor);
            }
        }
    }

    private void initComponents() {
        // Load icons early
        loadSkillIcons();
        loadQuestIcon();
        loadMinigameIcons();
        loadTravelIcons(); // NEW

        skillCombo = new JComboBox<>(Arrays.stream(SkillType.values())
                .sorted(Comparator.comparing(SkillType::name))
                .toArray(SkillType[]::new));
        skillCombo.setRenderer(new SkillIconRenderer());
        skillCombo.setMaximumRowCount(20);

        // NEW: Training method combo that updates when skill changes
        trainingMethodCombo = new JComboBox<>();
        trainingMethodCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null) {
                    setText(value.toString());
                }
                return this;
            }
        });

        targetLevelSpinner = new JSpinner(new SpinnerNumberModel(10, 2, 99, 1)); // Max level 99
        addSkillButton = new JButton("Add Skill Task");

        // Add listener to update training methods when skill changes
        skillCombo.addActionListener(e -> updateTrainingMethods());

        // Add listener to update config when method changes
        trainingMethodCombo.addActionListener(e -> updateSkillMethodConfig());

        // Initialize with first skill's methods
        updateTrainingMethods();

        questCombo = new JComboBox<>(Arrays.stream(QuestType.values())
                .sorted(Comparator.comparing(QuestType::name))
                .toArray(QuestType[]::new));
        questCombo.setRenderer(new QuestIconRenderer());
        addQuestButton = new JButton("Add Quest Task");

        minigameCombo = new JComboBox<>(Arrays.stream(MinigameType.values())
                .sorted(Comparator.comparing(MinigameType::getDisplayName))
                .filter(mt -> !mt.name().equals("BARBARIAN_ASSAULT")) // NEW exclude Barbarian Assault
                .toArray(MinigameType[]::new));
        minigameCombo.setRenderer(new MinigameIconRenderer());
        addMinigameButton = new JButton("Add Minigame Task");

        // --- Travel components (NEW) ---
        travelCombo = new JComboBox<>();
        populateTravelCombo(); // NEW
        addTravelButton = new JButton("Add Travel Task");
        setTravelCustomEnabled(false);
        travelCombo.addActionListener(e -> setTravelCustomEnabled(isTravelCustomSelected()));

        taskModel = new DefaultListModel<>();
        taskList = new JList<>(taskModel);
        taskList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        taskList.setCellRenderer(new EnhancedQueueCellRenderer());

        removeSelectedButton = new JButton("Remove Selected");
        startButton = new JButton("Start / Resume");
        pauseButton = new JButton("Pause");
        clearButton = new JButton("Clear Queue");
        refreshButton = new JButton("Refresh");
        currentTaskLabel = new JLabel("Current: none");
        statusLabel = new JLabel("Status: ");
        levelLabel = new JLabel("Level: -");
        xpGainedLabel = new JLabel("XP Gained: 0");
        xpPerHourLabel = new JLabel("XP/h: 0");
        xpRemainingLabel = new JLabel("Remaining: -");
        elapsedLabel = new JLabel("Elapsed: 0s");
        progressBar = new JProgressBar(0,100);
        progressBar.setStringPainted(true);

        tasksSummaryLabel = new JLabel("Tasks: 0");
        typeCountsLabel = new JLabel();
        typeCountsLabel.setForeground(new Color(200,200,200));
        typeCountsLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        typeCountsLabel.setFont(typeCountsLabel.getFont().deriveFont(Font.PLAIN, 11f));

        levelModeRadio = new JRadioButton("Target Level", true);
        timeModeRadio = new JRadioButton("Duration (min)");
        ButtonGroup bg = new ButtonGroup();
        bg.add(levelModeRadio); bg.add(timeModeRadio);
        minutesSpinner = new JSpinner(new SpinnerNumberModel(10,1,10000,1));
        editSelectedButton = new JButton("Edit");
        moveUpButton = new JButton("↑");
        moveDownButton = new JButton("↓");
        skipButton = new JButton("Skip");
        hideButton = new JButton("Hide");
        saveButton = new JButton("Save");
        loadButton = new JButton("Load");
        shuffleButton = new JButton("Shuffle");

        generateControlIcons();
        applyControlIcons();
        enhancePrimaryControlButtons(); // use enhanced sizing instead of shrinking
        applyLanguage(); // initial language application
    }

    private void loadSkillIcons() {
        for (SkillType st : SkillType.values()) {
            String resName = toResourceName(st);
            String path = "/skill_icons_small/" + resName + ".png";
            try {
                java.net.URL url = getClass().getResource(path);
                if (url != null) {
                    ImageIcon raw = new ImageIcon(url);
                    Image scaled = raw.getImage().getScaledInstance(SKILL_ICON_SIZE, SKILL_ICON_SIZE, Image.SCALE_SMOOTH);
                    SKILL_ICONS.put(st, new ImageIcon(scaled));
                }
            } catch (Exception ignored) {}
        }
    }

    private String toResourceName(SkillType st) {
        switch (st) {
            case RUNECRAFTING: return "runecraft";
            case HITPOINTS: return "hitpoints";
            default: return st.name().toLowerCase();
        }
    }

    private void loadQuestIcon() {
        try {
            java.net.URL url = getClass().getResource("/quest_icons/quest.png");
            if (url != null) {
                ImageIcon raw = new ImageIcon(url);
                Image scaled = raw.getImage().getScaledInstance(QUEST_ICON_SIZE, QUEST_ICON_SIZE, Image.SCALE_SMOOTH);
                QUEST_ICON = new ImageIcon(scaled);
                return;
            }
        } catch (Exception ignored) {}
        // Create blue quest icon with Q
        BufferedImage img = new BufferedImage(QUEST_ICON_SIZE, QUEST_ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // Blue gradient for quest icon
        GradientPaint gp = new GradientPaint(0,0,new Color(100,150,255),0,QUEST_ICON_SIZE,new Color(60,110,200));
        g.setPaint(gp);
        g.fillRoundRect(1,1,QUEST_ICON_SIZE-2,QUEST_ICON_SIZE-2,5,5);
        g.setColor(new Color(30,60,120));
        g.drawRoundRect(1,1,QUEST_ICON_SIZE-2,QUEST_ICON_SIZE-2,5,5);
        g.setFont(new Font("Serif", Font.BOLD, 11));
        g.setColor(Color.WHITE);
        g.drawString("Q", QUEST_ICON_SIZE/2-4, QUEST_ICON_SIZE/2+5);
        g.dispose();
        QUEST_ICON = new ImageIcon(img);
    }

    private void loadMinigameIcons() {
        for (MinigameType mt : MinigameType.values()) {
            String path = "/minigame_icons/" + mt.resourceKey() + ".png";
            try {
                java.net.URL url = getClass().getResource(path);
                if (url != null) {
                    ImageIcon raw = new ImageIcon(url);
                    Image scaled = raw.getImage().getScaledInstance(MINIGAME_ICON_SIZE, MINIGAME_ICON_SIZE, Image.SCALE_SMOOTH);
                    MINIGAME_ICONS.put(mt, new ImageIcon(scaled));
                } else {
                    MINIGAME_ICONS.put(mt, generateMinigameIcon(mt));
                }
            } catch (Exception ex) {
                MINIGAME_ICONS.put(mt, generateMinigameIcon(mt));
            }
        }
        if (MINIGAME_FALLBACK_ICON == null) {
            MINIGAME_FALLBACK_ICON = generateMinigameIcon(null);
        }
    }

    private Icon generateMinigameIcon(MinigameType mt) {
        BufferedImage img = new BufferedImage(MINIGAME_ICON_SIZE, MINIGAME_ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int hash = (mt == null ? 12345 : mt.name().hashCode());
        int r = 60 + (hash & 0x7F);
        int gCol = 60 + ((hash >> 7) & 0x7F);
        int b = 60 + ((hash >> 14) & 0x7F);
        Color base = new Color(r%256,gCol%256,b%256);
        GradientPaint gp = new GradientPaint(0,0,base.brighter(),MINIGAME_ICON_SIZE,MINIGAME_ICON_SIZE,base.darker());
        g.setPaint(gp);
        g.fillOval(1,1,MINIGAME_ICON_SIZE-2,MINIGAME_ICON_SIZE-2);
        g.setColor(new Color(255,255,255,180));
        g.setStroke(new BasicStroke(1.2f));
        g.drawOval(1,1,MINIGAME_ICON_SIZE-2,MINIGAME_ICON_SIZE-2);
        g.setFont(new Font("SansSerif", Font.BOLD, 9));
        String letter = mt == null ? "M" : mt.name().substring(0,1);
        FontMetrics fm = g.getFontMetrics();
        int tx = (MINIGAME_ICON_SIZE - fm.stringWidth(letter))/2;
        int ty = (MINIGAME_ICON_SIZE - fm.getHeight())/2 + fm.getAscent();
        g.drawString(letter, tx, ty);
        g.dispose();
        return new ImageIcon(img);
    }

    private void generateControlIcons() {
        playIcon = generateLetterIcon('▶', new Color(70,150,70));
        pauseIcon = generateLetterIcon('Ⅱ', new Color(180,140,40));
        skipIcon = generateLetterIcon('⤼', new Color(160,70,70));
        hideIcon = generateLetterIcon('—', new Color(90,90,160));
        saveIconSmall = generateLetterIcon('S', new Color(60,120,160));
        loadIconSmall = generateLetterIcon('L', new Color(120,80,150));
        shuffleIcon = generateLetterIcon('↺', new Color(100,100,100));
    }

    private Icon generateLetterIcon(char c, Color base) {
        int sz = 16;
        BufferedImage img = new BufferedImage(sz, sz, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        GradientPaint gp = new GradientPaint(0,0, base.brighter(), sz, sz, base.darker());
        g.setPaint(gp); g.fillRoundRect(0,0,sz-1,sz-1,4,4);
        g.setColor(Color.BLACK); g.drawRoundRect(0,0,sz-1,sz-1,4,4);
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        FontMetrics fm = g.getFontMetrics();
        int tx = (sz - fm.charWidth(c))/2;
        int ty = (sz - fm.getHeight())/2 + fm.getAscent();
        g.setColor(Color.WHITE);
        g.drawString(String.valueOf(c), tx, ty);
        g.dispose();
        return new ImageIcon(img);
    }

    // NEW: load travel icons (simple generated icons, special color for banks/f2p/p2p)
    private void loadTravelIcons() {
        for (TravelLocation tl : TravelLocation.values()) {
            TRAVEL_ICONS.put(tl, generateTravelIcon(tl));
        }
        if (TRAVEL_CUSTOM_ICON == null) TRAVEL_CUSTOM_ICON = generateCustomTravelIcon();
    }

    private Icon generateTravelIcon(TravelLocation tl) {
        int sz = 18;
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(sz, sz, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        String name = tl.getDisplayName().toLowerCase();
        boolean bank = name.contains("bank");
        Color base;
        if (bank) base = new Color(190,160,40);
        else if (tl.isF2p()) base = new Color(60,120,190);
        else base = new Color(120,70,150);
        GradientPaint gp = new GradientPaint(0,0, base.brighter(), sz, sz, base.darker());
        g.setPaint(gp);
        g.fillRoundRect(1,1,sz-2,sz-2,5,5);
        g.setColor(new Color(20,20,20,180));
        g.drawRoundRect(1,1,sz-2,sz-2,5,5);
        g.setFont(new Font("SansSerif", Font.BOLD, bank ? 9 : 10));
        String letter;
        if (bank) letter = "B";
        else {
            letter = tl.name().substring(0,1);
            if (!Character.isLetter(letter.charAt(0))) letter = "T";
        }
        FontMetrics fm = g.getFontMetrics();
        int tx = (sz - fm.stringWidth(letter))/2;
        int ty = (sz - fm.getHeight())/2 + fm.getAscent();
        g.setColor(Color.WHITE);
        g.drawString(letter, tx, ty);
        g.dispose();
        return new ImageIcon(img);
    }

    private Icon generateCustomTravelIcon() {
        int sz = 18;
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(sz, sz, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        GradientPaint gp = new GradientPaint(0,0,new Color(90,90,90), sz, sz, new Color(60,60,60));
        g.setPaint(gp);
        g.fillOval(1,1,sz-2,sz-2);
        g.setColor(Color.BLACK);
        g.drawOval(1,1,sz-2,sz-2);
        g.setFont(new Font("SansSerif", Font.BOLD, 9));
        String letter = "C";
        FontMetrics fm = g.getFontMetrics();
        int tx = (sz - fm.stringWidth(letter))/2;
        int ty = (sz - fm.getHeight())/2 + fm.getAscent();
        g.setColor(Color.WHITE);
        g.drawString(letter, tx, ty);
        g.dispose();
        return new ImageIcon(img);
    }

    // --- ADDED HELPERS (travel + control icons) ---
    private void applyControlIcons() {
        if (startButton != null) { startButton.setIcon(playIcon); startButton.setHorizontalTextPosition(SwingConstants.RIGHT); }
        if (pauseButton != null) { pauseButton.setIcon(pauseIcon); pauseButton.setHorizontalTextPosition(SwingConstants.RIGHT); }
        if (skipButton != null) { skipButton.setIcon(skipIcon); skipButton.setHorizontalTextPosition(SwingConstants.RIGHT); }
        if (hideButton != null) { hideButton.setIcon(hideIcon); hideButton.setHorizontalTextPosition(SwingConstants.RIGHT); }
        if (saveButton != null) { saveButton.setIcon(saveIconSmall); saveButton.setHorizontalTextPosition(SwingConstants.RIGHT); }
        if (loadButton != null) { loadButton.setIcon(loadIconSmall); loadButton.setHorizontalTextPosition(SwingConstants.RIGHT); }
        if (shuffleButton != null) { shuffleButton.setIcon(shuffleIcon); shuffleButton.setHorizontalTextPosition(SwingConstants.RIGHT); }
    }

    private void populateTravelCombo() {
        if (travelCombo == null) return;
        DefaultComboBoxModel<Object> model = new DefaultComboBoxModel<>();
        model.addElement("--- Locations ---");
        for (TravelLocation tl : TravelLocation.values()) if (tl.isF2p()) model.addElement(tl);
        model.addElement("--- P2P Locations ---");
        for (TravelLocation tl : TravelLocation.values()) if (!tl.isF2p()) model.addElement(tl);
        model.addElement("Custom Location...");
        travelCombo.setModel(model);
        travelCombo.setRenderer(new TravelLocationRenderer());
    }

    private static class TravelLocationRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof TravelLocation) {
                TravelLocation tl = (TravelLocation) value;
                lbl.setText(tl.getDisplayName());
                Icon ic = TRAVEL_ICONS.get(tl);
                if (ic != null) lbl.setIcon(ic);
                lbl.setIconTextGap(6);
            } else if (value instanceof String) {
                String s = (String) value;
                if (s.startsWith("---")) {
                    lbl.setText(s.replace("-"," ").trim());
                    lbl.setFont(lbl.getFont().deriveFont(Font.ITALIC));
                    lbl.setForeground(new Color(150,150,150));
                    lbl.setIcon(null);
                } else if (s.startsWith("Custom")) {
                    lbl.setIcon(TRAVEL_CUSTOM_ICON);
                }
            }
            return lbl;
        }
    }

    private TravelCustomResult showCustomTravelDialog() {
        JDialog dialog = new JDialog(this, "Custom Travel", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1; JTextField nameField = new JTextField("Custom", 12); panel.add(nameField, gbc);
        gbc.gridx = 0; gbc.gridy++; panel.add(new JLabel("X:"), gbc);
        gbc.gridx = 1; JSpinner xSpin = new JSpinner(new SpinnerNumberModel(3222,0,4000,1)); panel.add(xSpin, gbc);
        gbc.gridx = 0; gbc.gridy++; panel.add(new JLabel("Y:"), gbc);
        gbc.gridx = 1; JSpinner ySpin = new JSpinner(new SpinnerNumberModel(3218,0,4000,1)); panel.add(ySpin, gbc);
        gbc.gridx = 0; gbc.gridy++; panel.add(new JLabel("Z:"), gbc);
        gbc.gridx = 1; JSpinner zSpin = new JSpinner(new SpinnerNumberModel(0,0,3,1)); panel.add(zSpin, gbc);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = new JButton("OK"); JButton cancel = new JButton("Cancel");
        final TravelCustomResult[] res = {null};
        ok.addActionListener(ev -> { res[0] = new TravelCustomResult(nameField.getText().isBlank()?"Custom":nameField.getText().trim(), (Integer)xSpin.getValue(), (Integer)ySpin.getValue(), (Integer)zSpin.getValue()); dialog.dispose(); });
        cancel.addActionListener(ev -> dialog.dispose());
        buttons.add(ok); buttons.add(cancel);
        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2; panel.add(buttons, gbc);
        dialog.add(panel);
        dialog.pack(); dialog.setLocationRelativeTo(this); applyDialogTheme(dialog); dialog.setVisible(true);
        return res[0];
    }

    private static class TravelCustomResult { final String name; final int x,y,z; TravelCustomResult(String n,int x,int y,int z){this.name=n;this.x=x;this.y=y;this.z=z;} }
    // --- END ADDED HELPERS ---

    // Renderer classes
    private class SkillIconRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof SkillType) {
                SkillType st = (SkillType) value;
                lbl.setText(st.getDisplayName());
                Icon ic = SKILL_ICONS.get(st);
                lbl.setIcon(ic);
                lbl.setIconTextGap(8);
                lbl.setToolTipText(st.getDisplayName());
            }
            return lbl;
        }
    }

    private class QuestIconRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof QuestType) {
                QuestType qt = (QuestType) value;
                lbl.setText(qt.getDisplayName());
                lbl.setIcon(QUEST_ICON); // Show the blue Q icon
                lbl.setIconTextGap(8);
                lbl.setToolTipText(qt.getDisplayName());
            }
            return lbl;
        }
    }

    private class MinigameIconRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof MinigameType) {
                MinigameType mt = (MinigameType) value;
                lbl.setText(mt.getDisplayName());
                // Show the generated minigame icon with first letter
                Icon ic = MINIGAME_ICONS.get(mt);
                lbl.setIcon(ic != null ? ic : MINIGAME_FALLBACK_ICON);
                lbl.setIconTextGap(8);
                lbl.setToolTipText(mt.getDisplayName());
            }
            return lbl;
        }
    }

    private class EnhancedQueueCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            // Simplified color scheme (single dark-oriented variant)
            Color even = new Color(50,55,60);
            Color odd = new Color(44,48,52);
            Color sel = new Color(80,120,160);
            Color currentColor = new Color(60,100,60);
            Color baseFg = Color.WHITE;
            if (value instanceof TaskListEntry) {
                TaskListEntry e = (TaskListEntry) value;
                String base = e.displayText;
                if (e.isCurrent) base = "▶ " + base; else base = e.number + ". " + base;
                lbl.setText(base);
                if (e.type == AioTask.TaskType.SKILL && e.skillType != null) lbl.setIcon(SKILL_ICONS.get(e.skillType));
                else if (e.type == AioTask.TaskType.QUEST) lbl.setIcon(QUEST_ICON);
                else if (e.type == AioTask.TaskType.MINIGAME) {
                    Icon ic = MINIGAME_ICONS.get(e.minigameType); lbl.setIcon(ic != null ? ic : MINIGAME_FALLBACK_ICON);
                } else if (e.type == AioTask.TaskType.TRAVEL) {
                    Icon tIcon = null;
                    for (Map.Entry<TravelLocation, Icon> en : TRAVEL_ICONS.entrySet()) {
                        if (e.displayText.toLowerCase().contains(en.getKey().getDisplayName().toLowerCase())) { tIcon = en.getValue(); break; }
                    }
                    if (tIcon == null) tIcon = TRAVEL_CUSTOM_ICON;
                    lbl.setIcon(tIcon);
                }
                lbl.setIconTextGap(10);
                lbl.setOpaque(true);
                if (e.isCurrent) {
                    lbl.setBackground(currentColor);
                    lbl.setForeground(baseFg.darker());
                } else if (!isSelected) {
                    lbl.setBackground(index % 2 == 0 ? even : odd);
                    lbl.setForeground(baseFg);
                } else {
                    lbl.setBackground(sel);
                    lbl.setForeground(Color.BLACK);
                }
                lbl.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0,0,1,0,new Color(30,35,40)),
                        BorderFactory.createEmptyBorder(2,6,2,4)));
            }
            return lbl;
        }
    }

    // Build panels now store refs for language/theme update
    private JPanel buildSkillPanel() {
        skillPanelRef = new JPanel(new GridBagLayout());
        skillPanelRef.setBorder(new TitledBorder(translate("Skill Task")));
        GridBagConstraints gbc = baseGbc();

        // Skill selection
        skillPanelRef.add(new JLabel(translate("Skill:")), gbc);
        gbc.gridx = 1; skillPanelRef.add(skillCombo, gbc);


        // Target level/time selection
        gbc.gridx = 0; gbc.gridy++; skillPanelRef.add(levelModeRadio, gbc);
        gbc.gridx = 1; skillPanelRef.add(targetLevelSpinner, gbc);
        gbc.gridx = 0; gbc.gridy++; skillPanelRef.add(timeModeRadio, gbc);
        gbc.gridx = 1; skillPanelRef.add(minutesSpinner, gbc);
        // Inline options under Duration (min) - include full skill-specific options, not just Method
        gbc.gridx = 0; gbc.gridy++; skillPanelRef.add(new JLabel(translate("Method:")), gbc);
        // Ensure method choices are populated for current skill before showing
        SkillType curSkillForMethods = (SkillType) skillCombo.getSelectedItem();
        if (curSkillForMethods != null) {
            populateMethodCombo(trainingMethodCombo, curSkillForMethods);
        }
        gbc.gridx = 1; skillPanelRef.add(trainingMethodCombo, gbc);
        // Container for additional skill-specific options
        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2;
        if (skillOptionsPanelInline == null) skillOptionsPanelInline = new JPanel(new GridBagLayout());
        skillOptionsPanelInline.setOpaque(false);
        skillPanelRef.add(skillOptionsPanelInline, gbc);
        // Build options for current selection
        rebuildInlineSkillOptions();

        // Add button
        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2; skillPanelRef.add(addSkillButton, gbc);

        return skillPanelRef;
    }

    // Rebuilds the inline skill-specific options panel under the Method combo
    private void rebuildInlineSkillOptions() {
        if (skillOptionsPanelInline == null) return;
        // Clear existing components and registry
        skillOptionsPanelInline.removeAll();
        if (skillOptionsComponentsInline != null) {
            skillOptionsComponentsInline.clear();
        }
        // Determine current skill
        SkillType st = (SkillType) skillCombo.getSelectedItem();
        if (st != null) {
            GridBagConstraints gbc = baseGbc();
            gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
            // Populate skill-specific controls into the inline panel
            addSkillSpecificOptions(st, skillOptionsPanelInline, skillOptionsComponentsInline, gbc);
        }
        skillOptionsPanelInline.revalidate();
        skillOptionsPanelInline.repaint();
    }

    private JPanel buildQuestPanel() {
        questPanelRef = new JPanel(new GridBagLayout());
        questPanelRef.setBorder(new TitledBorder(translate("Quest Task")));
        GridBagConstraints gbc = baseGbc();

        questPanelRef.add(new JLabel(translate("Quest:")), gbc);
        gbc.gridx = 1; questPanelRef.add(questCombo, gbc);

        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2;
        questPanelRef.add(addQuestButton, gbc);

        return questPanelRef;
    }

    private JPanel buildMinigamePanel() {
        minigamePanelRef = new JPanel(new GridBagLayout());
        minigamePanelRef.setBorder(new TitledBorder(translate("Minigame Task")));
        GridBagConstraints gbc = baseGbc();
        minigamePanelRef.add(new JLabel(translate("Minigame:")), gbc);
        gbc.gridx = 1; minigamePanelRef.add(minigameCombo, gbc);
        gbc.gridx = 0; gbc.gridy++; minigamePanelRef.add(new JLabel(translate("Duration (min):")), gbc);
        gbc.gridx = 1; minigamePanelRef.add(minigameDurationSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 1440, 1)), gbc);
        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2; minigamePanelRef.add(addMinigameButton, gbc);
        return minigamePanelRef;
    }

    // --- Travel panel (NEW) ---
    private JPanel buildTravelPanel() {
        travelPanelRef = new JPanel(new GridBagLayout());
        travelPanelRef.setBorder(new TitledBorder(translate("Travel Task")));
        GridBagConstraints gbc = baseGbc();
        travelPanelRef.add(new JLabel(translate("Location:")), gbc);
        gbc.gridx = 1; travelPanelRef.add(travelCombo, gbc);
        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2; travelPanelRef.add(addTravelButton, gbc);
        return travelPanelRef;
    }

    // Helpers for custom travel enable (NEW)
    private boolean isTravelCustomSelected() {
        Object sel = travelCombo.getSelectedItem();
        return sel instanceof String && ((String) sel).startsWith("Custom");
    }
    private void setTravelCustomEnabled(boolean en) {
        if (travelCombo != null) travelCombo.setEnabled(true);
    }

    private JPanel buildQueuePanel() {
        queuePanelRef = new JPanel(new BorderLayout());
        queuePanelRef.setBorder(new TitledBorder(translate("Queue")));
        // Removed filter UI entirely per request
        // Rework top header: first row = top buttons, second row = counts (single row, left-aligned)
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        JPanel countsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        countsRow.setOpaque(false);
        // Ensure labels exist; align next to each other: Tasks, Skills, Quests, Minigames, Travel
        if (tasksSummaryLabel != null) countsRow.add(tasksSummaryLabel);
        if (typeCountsLabel != null) countsRow.add(typeCountsLabel);
        queuePanelRef.add(top, BorderLayout.NORTH);
        taskList.setFixedCellHeight(compactMode ? 18 : 28);
        JScrollPane sp = new JScrollPane(taskList);
        queuePanelRef.add(sp, BorderLayout.CENTER);
        // ...existing code for buttons (unchanged)...
        JPanel buttonArea = new JPanel(new BorderLayout());
        JPanel topButtons = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 2));
        // Ensure clear button labeled
        if (clearButton != null) {
            clearButton.setText("Clear Queue");
            clearButton.setToolTipText("Clear all tasks from the queue");
        }
        topButtons.add(loadButton);
        topButtons.add(saveButton);
        topButtons.add(refreshButton);
        topButtons.add(clearButton);
        // First row: top buttons
        top.add(topButtons);
        // Second row: counts row
        top.add(countsRow);
        JPanel bottomButtons = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 2));
        bottomButtons.add(removeSelectedButton);
        bottomButtons.add(editSelectedButton);
        bottomButtons.add(moveUpButton);
        bottomButtons.add(moveDownButton);
        bottomButtons.add(shuffleButton);
        // Ensure critical buttons are visible
        if (clearButton != null) clearButton.setVisible(true);
        if (shuffleButton != null) shuffleButton.setVisible(true);
        buttonArea.add(bottomButtons, BorderLayout.SOUTH);
        queuePanelRef.add(buttonArea, BorderLayout.SOUTH);
        // Do not force a preferred size; let layout and content dictate height so bottom buttons are visible in compact mode
        return queuePanelRef;
    }

    private JPanel buildControlPanel() {
        // Reworked: always horizontal compact layout for the four primary buttons
        controlPanelRef = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        controlPanelRef.setBorder(new TitledBorder(translate("Control")));
        // Shortened labels for compactness
        startButton.setText(script.isPaused()?"Start":"Start");
        pauseButton.setText(script.isPaused()?"Resume":"Pause");
        skipButton.setText("Skip");
        hideButton.setText("Hide");
        controlPanelRef.add(startButton);
        controlPanelRef.add(pauseButton);
        controlPanelRef.add(skipButton);
        controlPanelRef.add(hideButton);
        enhancePrimaryControlButtons(); // apply enhanced sizing
        return controlPanelRef;
    }

    private JPanel buildStatusPanel() {
        statusPanelRef = new JPanel(new GridLayout(compactMode ? 0 : 0,1,2,2));
        statusPanelRef.setBorder(new TitledBorder(translate("Status")));
        if (compactMode) {
            // Strict vertical order as requested
            statusPanelRef.setLayout(new BoxLayout(statusPanelRef, BoxLayout.Y_AXIS));
            statusPanelRef.add(wrapLeft(currentTaskLabel));
            statusPanelRef.add(wrapLeft(statusLabel));
            statusPanelRef.add(wrapLeft(levelLabel));
            statusPanelRef.add(wrapLeft(xpGainedLabel));
            statusPanelRef.add(wrapLeft(xpPerHourLabel));
            statusPanelRef.add(wrapLeft(elapsedLabel));
            statusPanelRef.add(wrapLeft(xpRemainingLabel));
            statusPanelRef.add(progressBar);
        } else {
            statusPanelRef.add(currentTaskLabel);
            statusPanelRef.add(statusLabel);
            statusPanelRef.add(levelLabel);
            statusPanelRef.add(xpGainedLabel);
            statusPanelRef.add(xpPerHourLabel);
            statusPanelRef.add(xpRemainingLabel);
            statusPanelRef.add(elapsedLabel);
            statusPanelRef.add(progressBar);
        }

        // Make status panel wider (25% more horizontal space)
        if (!compactMode) {
            statusPanelRef.setPreferredSize(new Dimension(250, statusPanelRef.getPreferredSize().height));
        } else {
            statusPanelRef.setPreferredSize(new Dimension(200, statusPanelRef.getPreferredSize().height));
        }

        return statusPanelRef;
    }

    private void layoutComponents() {
        if (compactMode) {
            JPanel root = new JPanel(new BorderLayout(4,4));
            root.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
            // Left side becomes tabs to save space
            root.add(buildCompactTabs(), BorderLayout.WEST);
            root.add(buildQueuePanel(), BorderLayout.CENTER);
            JPanel east = new JPanel();
            east.setLayout(new BoxLayout(east, BoxLayout.Y_AXIS));
            east.add(buildControlPanel());
            east.add(Box.createVerticalStrut(4));
            east.add(buildStatusPanel());
            root.add(east, BorderLayout.EAST);
            setContentPane(root);
        } else {
            JPanel left = new JPanel();
            left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
            if (showSkillPanel) left.add(buildSkillPanel());
            left.add(Box.createVerticalStrut(6));
            if (showQuestPanel) left.add(buildQuestPanel());
            left.add(Box.createVerticalStrut(6));
            if (showMinigamePanel) left.add(buildMinigamePanel());
            left.add(Box.createVerticalStrut(6));
            left.add(buildTravelPanel()); // NEW travel panel always shown

            JPanel middle = buildQueuePanel();

            JPanel right = new JPanel();
            right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
            if (showControlPanel) right.add(buildControlPanel());
            right.add(Box.createVerticalStrut(6));
            if (showStatusPanel) right.add(buildStatusPanel());

            JPanel root = new JPanel(new BorderLayout(8,8));
            root.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
            root.add(left, BorderLayout.WEST);
            root.add(middle, BorderLayout.CENTER);
            root.add(right, BorderLayout.EAST);
            setContentPane(root);
        }
    }

    private JPanel buildCompactTabs() {
        JTabbedPane tp = new JTabbedPane();
        tp.addTab(translate("Skill"), buildSkillPanel());
        tp.addTab(translate("Quest"), buildQuestPanel());
        tp.addTab(translate("Minigame"), buildMinigamePanel());
        tp.addTab(translate("Travel"), buildTravelPanel()); // NEW tab
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.add(tp, BorderLayout.CENTER);
        return wrap;
    }

    // ================== BEGIN MODIFICATIONS ==================
    // Preserve current position explicitly and avoid centering again
    private void updateWindowSize() {
        if (compactMode) {
            setMinimumSize(new Dimension(560, 420));
        } else {
            setMinimumSize(new Dimension(820, 600));
        }
        pack();
        if (lastKnownLocation != null) setLocation(lastKnownLocation); // restore
    }
    // ================== END MODIFICATIONS ==================

    // Reliable centering for undecorated frames
    private void centerOnScreen() {
        try {
            // Ensure we have a valid size
            if (getWidth() == 0 || getHeight() == 0) {
                pack();
            }
            Point center = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
            int x = center.x - getWidth() / 2;
            int y = center.y - getHeight() / 2;
            setLocation(Math.max(0, x), Math.max(0, y));
        } catch (Exception ignored) {}
    }

    private void attachListeners() {
        addSkillButton.addActionListener(e -> {
            // Store current location before adding task
            if (preserveLocation) {
                originalLocation = getLocation();
            }

            SkillType st = (SkillType) skillCombo.getSelectedItem();
            if (st == null) return; // null guard

            // Use inline options (no popup)
            if (timeModeRadio.isSelected()) {
                int mins = (int) minutesSpinner.getValue();
                script.addSkillTaskTime(st, mins);
            } else {
                int target = (int) targetLevelSpinner.getValue();
                script.addSkillTask(st, target);
            }
            // Persist selected method and inline options to config so the task can use them
            try {
                ConfigManager cm = script.getConfigManager();
                String group = script.getConfigGroup();
                Object selMethod = trainingMethodCombo.getSelectedItem();
                String methodKey = getConfigKeyForMethod(st);
                if (selMethod != null && methodKey != null) {
                    String value = selMethod instanceof Enum ? ((Enum<?>) selMethod).name() : selMethod.toString();
                    cm.setConfiguration(group, methodKey, value);
                }
                if (skillOptionsComponentsInline != null) {
                    updateSkillSpecificConfig(st, skillOptionsComponentsInline, cm, group);
                }
            } catch (Exception ignored) {}
            refreshQueue();

            // Restore location after adding task
            if (preserveLocation && originalLocation != null) {
                SwingUtilities.invokeLater(() -> setLocation(originalLocation));
            }
        });

        // Update methods and options when skill changes
        skillCombo.addActionListener(e -> {
            SkillType st = (SkillType) skillCombo.getSelectedItem();
            if (st != null) {
                populateMethodCombo(trainingMethodCombo, st);
                rebuildInlineSkillOptions();
            }
        });
        // Update options when method changes
        trainingMethodCombo.addActionListener(e -> rebuildInlineSkillOptions());

        addQuestButton.addActionListener(e -> {
            // Store current location before adding task
            if (preserveLocation) {
                originalLocation = getLocation();
            }

            QuestType qt = (QuestType) questCombo.getSelectedItem();
            script.addQuestTask(qt);
            refreshQueue();

            // Restore location after adding task
            if (preserveLocation && originalLocation != null) {
                SwingUtilities.invokeLater(() -> setLocation(originalLocation));
            }
        });

        addMinigameButton.addActionListener(e -> {
            // Store current location before adding task
            if (preserveLocation) {
                originalLocation = getLocation();
            }

            MinigameType mt = (MinigameType) minigameCombo.getSelectedItem();
            int duration = (int) minigameDurationSpinner.getValue();
            script.addMinigameTask(mt, duration); // Use the new method with duration
            refreshQueue();

            // Restore location after adding task
            if (preserveLocation && originalLocation != null) {
                SwingUtilities.invokeLater(() -> setLocation(originalLocation));
            }
        });

        addTravelButton.addActionListener(e -> { // NEW travel listener modified: keep combo enabled after custom
            if (preserveLocation) originalLocation = getLocation();
            Object sel = travelCombo.getSelectedItem();
            boolean wasCustom = false;
            if (sel instanceof TravelLocation) {
                script.addTravelTask((TravelLocation) sel);
            } else if (sel instanceof String && ((String) sel).startsWith("Custom")) {
                wasCustom = true;
                TravelCustomResult r = showCustomTravelDialog();
                if (r != null) script.addTravelTaskCustom(r.name, new WorldPoint(r.x, r.y, r.z));
            }
            refreshQueue();
            // Re-enable and reset selection if custom was chosen so user can pick normal locations again
            travelCombo.setEnabled(true);
            if (wasCustom && travelCombo.getItemCount() > 1) {
                travelCombo.setSelectedIndex(1); // first actual entry after F2P header
            }
            if (preserveLocation && originalLocation != null) SwingUtilities.invokeLater(() -> setLocation(originalLocation));
        });

        removeSelectedButton.addActionListener(e -> {
            int idx = taskList.getSelectedIndex();
            if (idx >= 0) {
                int modelIndex = toUnderlyingQueueIndex(idx);
                if (modelIndex >= 0) {
                    script.removeTask(modelIndex);
                    refreshQueue();
                }
            }
        });

        clearButton.addActionListener(e -> {
            script.clearQueue();
            refreshQueue();
        });

        startButton.addActionListener(e -> {
            if (!script.isRunning()) {
                script.startLoop();
            } else {
                script.resumeLoop();
            }
        });

        pauseButton.addActionListener(e -> {
            if (script.isPaused()) {
                script.resumeLoop();
            } else {
                script.pauseLoop();
            }
            updatePauseLabel();
        });

        refreshButton.addActionListener(e -> {
            refreshQueue();
            refreshStatus();
        });

        skipButton.addActionListener(e -> script.skipCurrentTask());
        hideButton.addActionListener(e -> toggleTray());

        editSelectedButton.addActionListener(e -> editSelected());
        moveUpButton.addActionListener(e -> {
            int idx = taskList.getSelectedIndex();
            int qIdx = toUnderlyingQueueIndex(idx);
            if (qIdx >= 0 && script.moveTaskUp(qIdx)) {
                refreshQueue();
                selectByQueueIndex(qIdx-1);
            }
        });
        moveDownButton.addActionListener(e -> {
            int idx = taskList.getSelectedIndex();
            int qIdx = toUnderlyingQueueIndex(idx);
            if (qIdx >= 0 && script.moveTaskDown(qIdx)) {
                refreshQueue();
                selectByQueueIndex(qIdx+1);
            }
        });

        shuffleButton.addActionListener(e -> {
            script.shuffleQueue();
            refreshQueue();
        });

        saveButton.addActionListener(e -> script.saveQueueToConfig());
        loadButton.addActionListener(e -> {
            script.loadQueueFromConfig();
            refreshQueue();
        });

        // Double click edit
        taskList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) editSelected();
            }
        });

        // Context menu
        JPopupMenu menu = new JPopupMenu();
        JMenuItem miEdit = new JMenuItem("Edit");
        miEdit.addActionListener(e -> editSelected());
        menu.add(miEdit);
        JMenuItem miRemove = new JMenuItem("Remove");
        miRemove.addActionListener(e -> removeSelectedButton.doClick());
        menu.add(miRemove);
        JMenuItem miUp = new JMenuItem("Move Up");
        miUp.addActionListener(e -> moveUpButton.doClick());
        menu.add(miUp);
        JMenuItem miDown = new JMenuItem("Move Down");
        miDown.addActionListener(e -> moveDownButton.doClick());
        menu.add(miDown);
        taskList.setComponentPopupMenu(menu);
    }

    private void refreshQueue() {
        Point keep = getLocation(); // preserve position
        taskModel.clear();
        AioTask cur = script.getCurrentTask();
        if (cur != null && !cur.isComplete()) taskModel.addElement(buildEntryCurrent(cur));
        java.util.List<AioTask> raw = script.getQueueSnapshotRaw();
        int number = 1;
        int skillCount=0, questCount=0, miniCount=0, travelCount=0;
        for (AioTask t : raw) {
            if (t.getType() == AioTask.TaskType.SKILL) skillCount++; else if (t.getType()==AioTask.TaskType.QUEST) questCount++; else if (t.getType()==AioTask.TaskType.MINIGAME) miniCount++; else if (t.getType()==AioTask.TaskType.TRAVEL) travelCount++;
            TaskListEntry e = buildEntry(t, number);
            taskModel.addElement(e);
            number++;
        }
        tasksSummaryLabel.setText("Tasks: " + raw.size());
        typeCountsLabel.setText("<html><span style='color:#6fa8dc'>Skills: " + skillCount + "</span> | <span style='color:#f1c232'>Quests: " + questCount + "</span> | <span style='color:#b4a7d6'>Minigames: " + miniCount + "</span> | <span style='color:#93c47d'>Travel: " + travelCount + "</span></html>");
        taskList.repaint();
        // Do NOT resize or reposition window on periodic refresh before the frame is shown/initialized
        if (isShowing() && locationInitialized) {
            if (lastKnownLocation != null) {
                SwingUtilities.invokeLater(() -> setLocation(lastKnownLocation));
            } else {
                SwingUtilities.invokeLater(() -> setLocation(keep));
            }
        }
    }

    // NEW: lightweight refresh only updating the list model (no labels, counts or relocation)
    private void refreshQueueListOnly() {
        if (taskModel == null || script == null) return;
        // Capture selection to restore
        int selectedIndex = taskList.getSelectedIndex();
        AioTask cur = script.getCurrentTask();
        java.util.List<AioTask> raw = script.getQueueSnapshotRaw();
        // Quick diff: if counts and first/last types match, rebuild silently; else full refresh for safety
        int existingSize = taskModel.getSize();
        int expectedSize = raw.size() + (cur != null && !cur.isComplete() ? 1 : 0);
        if (existingSize != expectedSize) { refreshQueue(); return; }
        // Rebuild model (fast path)
        taskModel.clear();
        if (cur != null && !cur.isComplete()) taskModel.addElement(buildEntryCurrent(cur));
        int number = 1;
        for (AioTask t : raw) {
            taskModel.addElement(buildEntry(t, number));
            number++;
        }
        if (selectedIndex >= 0 && selectedIndex < taskModel.size()) taskList.setSelectedIndex(selectedIndex);
        taskList.repaint();
    }

    private void refreshStatus() {
        StatusAccessor sa = script.getStatusAccessor();
        currentTaskLabel.setText("Current: " + sa.getCurrentTask());
        statusLabel.setText("Status: " + sa.getStatus());
        if (sa.getTargetLevel() > 0) {
            levelLabel.setText("Level: " + sa.getCurrentLevel() + "/" + sa.getTargetLevel());
        } else {
            levelLabel.setText("Level: " + sa.getCurrentLevel());
        }
        xpGainedLabel.setText("XP Gained: " + sa.getXpGained());
        xpPerHourLabel.setText("XP/h: " + (long)sa.getXpPerHour());
        xpRemainingLabel.setText("Remaining: " + (sa.getXpToTarget() < 0 ? "-" : sa.getXpToTarget()));
        elapsedLabel.setText("Elapsed: " + (sa.getTaskElapsedMs()/1000) + "s");

        double pct = sa.getPercentToTarget();
        if (pct < 0) pct = 0;
        if (pct > 100) pct = 100;
        progressBar.setValue((int) Math.round(pct));
        progressBar.setString(String.format("%.1f%%", pct));

        if (compactMode) {
            // In compact mode show condensed remaining + elapsed in tooltip
            progressBar.setToolTipText("Remaining: " + xpRemainingLabel.getText() + " | Elapsed: " + elapsedLabel.getText());
        }

        updatePauseLabel();
        taskList.repaint();
    }

    private void updatePauseLabel() {
        pauseButton.setText(translate(script.isPaused() ? "Resume" : "Pause"));
    }

    private GridBagConstraints baseGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2,2,2,2);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        return gbc;
    }

    private void editSelected() {
        int idx = taskList.getSelectedIndex();
        if (idx < 0) return;
        TaskListEntry entry = taskModel.get(idx);
        if (entry.isCurrent) return;
        int qIdx = toUnderlyingQueueIndex(idx);
        if (qIdx < 0) return;

        if (entry.type == AioTask.TaskType.SKILL) {
            AioTask t = script.getQueueSnapshotRaw().get(qIdx);
            if (t instanceof AioSkillTask) {
                AioSkillTask s = (AioSkillTask) t;
                // Show comprehensive edit dialog for skill tasks
                if (showEditSkillDialog(s, qIdx)) {
                    refreshQueue();
                    selectByQueueIndex(qIdx);
                }
            }
        } else if (entry.type == AioTask.TaskType.QUEST) {
            // Quest tasks don't have configurable options currently
            JOptionPane.showMessageDialog(this, "Quest tasks cannot be edited.", "Edit Task", JOptionPane.INFORMATION_MESSAGE);
        } else if (entry.type == AioTask.TaskType.MINIGAME) {
            AioTask t = script.getQueueSnapshotRaw().get(qIdx);
            if (t instanceof AioMinigameTask) {
                AioMinigameTask m = (AioMinigameTask) t;
                // Simple duration edit for minigames
                String input = JOptionPane.showInputDialog(this, "Duration (minutes):", "Edit Minigame Duration", JOptionPane.PLAIN_MESSAGE);
                if (input != null) {
                    try {
                        int duration = Integer.parseInt(input);
                        if (duration > 0) {
                            // Update minigame duration (would need method in script)
                            refreshQueue();
                            selectByQueueIndex(qIdx);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        } else if (entry.type == AioTask.TaskType.TRAVEL) {
            AioTask t = script.getQueueSnapshotRaw().get(qIdx);
            if (t instanceof AioTravelTask) {
                AioTravelTask tr = (AioTravelTask) t;
                // Edit travel task (preset or custom)
                if (tr.getTravelLocationOrNull() != null) {
                    // Preset travel task -> show selection dialog
                    TravelLocation selected = showTravelLocationChooser(tr.getTravelLocationOrNull());
                    if (selected != null) {
                        script.editTravelTask(qIdx, selected);
                        refreshQueue();
                        selectByQueueIndex(qIdx);
                    }
                } else {
                    // Custom travel task
                    JDialog dialog = new JDialog(this, "Edit Travel Task", true);
                    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

                    JPanel panel = new JPanel(new GridBagLayout());
                    GridBagConstraints gbc = new GridBagConstraints();
                    gbc.insets = new Insets(10, 10, 10, 10);
                    gbc.gridx = 0;
                    gbc.gridy = 0;
                    gbc.anchor = GridBagConstraints.WEST;

                    panel.add(new JLabel("Task Name:"), gbc);
                    JTextField nameField = new JTextField(tr.getCustomName(), 15);
                    gbc.gridx = 1;
                    panel.add(nameField, gbc);

                    panel.add(new JLabel("X Coordinate:"), gbc);
                    JSpinner xSpinner = new JSpinner(new SpinnerNumberModel(tr.getCustomPoint().getX(), 0, 4000, 1));
                    gbc.gridx = 1;
                    panel.add(xSpinner, gbc);

                    panel.add(new JLabel("Y Coordinate:"), gbc);
                    JSpinner ySpinner = new JSpinner(new SpinnerNumberModel(tr.getCustomPoint().getY(), 0, 4000, 1));
                    gbc.gridx = 1;
                    panel.add(ySpinner, gbc);

                    panel.add(new JLabel("Z Coordinate:"), gbc);
                    JSpinner zSpinner = new JSpinner(new SpinnerNumberModel(tr.getCustomPoint().getPlane(), 0, 3, 1));
                    gbc.gridx = 1;
                    panel.add(zSpinner, gbc);

                    JButton saveButton = new JButton("Save");
                    saveButton.addActionListener(e -> {
                        String newName = nameField.getText().trim();
                        int x = (Integer) xSpinner.getValue();
                        int y = (Integer) ySpinner.getValue();
                        int z = (Integer) zSpinner.getValue();
                        script.editTravelTaskCustom(qIdx, newName, new WorldPoint(x, y, z));
                        refreshQueue();
                        dialog.dispose();
                    });
                    gbc.gridx = 0;
                    gbc.gridy++;
                    gbc.gridwidth = 2;
                    panel.add(saveButton, gbc);

                    dialog.add(panel);
                    dialog.pack();
                    dialog.setLocationRelativeTo(this);
                    dialog.setVisible(true);
                }
            }
        }
    }

    /**
     * Shows a comprehensive edit dialog for skill tasks including all configuration options
     */
    private boolean showEditSkillDialog(AioSkillTask skillTask, int queueIndex) {
        SkillType skillType = skillTask.getSkillType();

        JDialog dialog = new JDialog(this, "Edit " + skillType.getDisplayName() + " Task", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Title with skill icon
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        Icon skillIcon = SKILL_ICONS.get(skillType);
        if (skillIcon != null) {
            titlePanel.add(new JLabel(skillIcon));
        }
        JLabel titleLabel = new JLabel("Edit " + skillType.getDisplayName() + " Task");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        titlePanel.add(titleLabel);
        mainPanel.add(titlePanel, BorderLayout.NORTH);

        // Configuration panel
        JPanel configPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Target Level vs Time Mode
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JRadioButton levelModeEdit = new JRadioButton("Target Level", !skillTask.isTimeMode());
        JRadioButton timeModeEdit = new JRadioButton("Duration (minutes)", skillTask.isTimeMode());
        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(levelModeEdit);
        modeGroup.add(timeModeEdit);

        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        modePanel.add(levelModeEdit);
        modePanel.add(timeModeEdit);
        configPanel.add(modePanel, gbc);

        // Target level spinner
        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 1;
        configPanel.add(new JLabel("Target Level:"), gbc);
        // Fix: Ensure target level is within valid range (1-99)
        int currentTargetLevel = skillTask.getTargetLevel();
        if (currentTargetLevel < 1) currentTargetLevel = 1;
        if (currentTargetLevel > 99) currentTargetLevel = 99;
        JSpinner levelSpinner = new JSpinner(new SpinnerNumberModel(currentTargetLevel, 1, 99, 1));
        levelSpinner.setEnabled(!skillTask.isTimeMode());
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        configPanel.add(levelSpinner, gbc);

        // Duration spinner
        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
        configPanel.add(new JLabel("Duration (min):"), gbc);
        // Fix: Ensure duration is within valid range (1-10000)
        int currentDuration = skillTask.getDurationMinutes();
        if (currentDuration < 1) currentDuration = 1;
        if (currentDuration > 10000) currentDuration = 10000;
        JSpinner durationSpinner = new JSpinner(new SpinnerNumberModel(currentDuration, 1, 10000, 1));
        durationSpinner.setEnabled(skillTask.isTimeMode());
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        configPanel.add(durationSpinner, gbc);

        // Add listeners to enable/disable spinners
        levelModeEdit.addActionListener(e -> {
            levelSpinner.setEnabled(true);
            durationSpinner.setEnabled(false);
        });
        timeModeEdit.addActionListener(e -> {
            levelSpinner.setEnabled(false);
            durationSpinner.setEnabled(true);
        });

        // Training method selection
        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
        configPanel.add(new JLabel("Training Method:"), gbc);
        JComboBox<Object> methodCombo = new JComboBox<>();
        populateMethodCombo(methodCombo, skillType);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        configPanel.add(methodCombo, gbc);

        // Additional skill-specific options
        Map<String, JComponent> additionalComponents = new HashMap<>();
        addSkillSpecificOptions(skillType, configPanel, additionalComponents, gbc);

        mainPanel.add(configPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton okButton = new JButton("Save Changes");
        JButton cancelButton = new JButton("Cancel");

        final boolean[] result = {false};

        okButton.addActionListener(e -> {
            try {
                // Update the task with new values
                boolean isTimeMode = timeModeEdit.isSelected();

                if (isTimeMode) {
                    int minutes = (Integer) durationSpinner.getValue();
                    script.editSkillTask(queueIndex, null, minutes, true);
                } else {
                    int targetLevel = (Integer) levelSpinner.getValue();
                    script.editSkillTask(queueIndex, targetLevel, null, false);
                }

                // Update config with selected values
                updateConfigFromDialog(skillType, methodCombo, additionalComponents);

                result[0] = true;
                dialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error saving changes: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> {
            result[0] = false;
            dialog.dispose();
        });

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(mainPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);

        // Apply theme
        applyDialogTheme(dialog);

        // Center window on show
        dialog.setVisible(true);
        return result[0];
    }

    private void toggleTray() {
        if (!SystemTray.isSupported()) {
            setState(Frame.ICONIFIED);
            return;
        }
        try {
            // Hide when no tray icon exists; restore when it does
            if (trayIcon == null) {
                tray = SystemTray.getSystemTray();
                Image img = getIconImage();
                if (img == null) img = new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);
                PopupMenu pm = new PopupMenu();
                MenuItem restore = new MenuItem("Restore");
                restore.addActionListener(e -> restoreFromTray());
                pm.add(restore);
                trayIcon = new TrayIcon(img, "Account Builder", pm);
                trayIcon.addActionListener(e -> restoreFromTray());
                tray.add(trayIcon);
                setVisible(false);
            } else {
                restoreFromTray();
            }
        } catch (Exception ex) {
            setState(Frame.ICONIFIED);
        }
    }

    private void restoreFromTray() {
        if (trayIcon != null && tray != null) {
            tray.remove(trayIcon);
            trayIcon = null;
        }
        setVisible(true);
        setState(Frame.NORMAL);
        toFront();
    }

    // ================== BEGIN MODIFICATIONS ==================
    // Fix syntax error in toUnderlyingQueueIndex
    private int toUnderlyingQueueIndex(int listIndex) {
        if (listIndex < 0) return -1;
        TaskListEntry e = taskModel.get(listIndex);
        if (e.isCurrent) return -1;
        return e.number - 1;
    }
    // ================== END MODIFICATIONS ==================

    private void selectByQueueIndex(int qIndex) {
        for (int i=0;i<taskModel.size();i++) {
            TaskListEntry e = taskModel.get(i);
            if (!e.isCurrent && e.number -1 == qIndex) {
                taskList.setSelectedIndex(i);
                taskList.ensureIndexIsVisible(i);
                break;
            }
        }
    }

    private TaskListEntry buildEntry(AioTask t, int number) {
        TaskListEntry e = new TaskListEntry();
        e.number = number;
        e.type = t.getType();
        if (t instanceof AioSkillTask) {
            AioSkillTask s = (AioSkillTask) t;
            e.skillType = s.getSkillType();
            if (s.isTimeMode()) {
                String timeDisplay = formatMinutesToReadable(s.getDurationMinutes());
                e.displayText = s.getSkillType().getDisplayName() + " for " + timeDisplay;
                e.tooltip = "Time-based skill task";
            } else {
                int tgt = s.getTargetLevel();
                e.displayText = s.getSkillType().getDisplayName() + " to " + (tgt == 0 ? "(cfg)" : tgt);
                e.tooltip = "Train to target level";
            }
        } else if (t instanceof AioQuestTask) {
            AioQuestTask q = (AioQuestTask) t;
            e.questType = q.getQuestType();
            e.displayText = q.getQuestType().getDisplayName();
            e.tooltip = "Quest task";
        } else if (t instanceof AioMinigameTask) {
            AioMinigameTask m = (AioMinigameTask) t;
            e.minigameType = m.getMinigameType();
            String baseDisplay = m.getMinigameType().getDisplayName();
            try {
                java.lang.reflect.Method getDurationMethod = m.getClass().getMethod("getDurationMinutes");
                int duration = (Integer) getDurationMethod.invoke(m);
                if (duration > 0) {
                    String timeDisplay = formatMinutesToReadable(duration);
                    baseDisplay += " for " + timeDisplay;
                }
            } catch (Exception ignored) {
            }
            e.displayText = baseDisplay;
            e.tooltip = "Minigame task";
        } else if (t instanceof AioTravelTask) { // NEW travel entry
            AioTravelTask tr = (AioTravelTask) t;
            e.displayText = tr.getDisplay();
            e.tooltip = "Travel task";
        } else {
            e.displayText = t.getDisplay();
            e.tooltip = t.getDisplay();
        }
        return e;
    }

    private TaskListEntry buildEntryCurrent(AioTask t) {
        TaskListEntry e = new TaskListEntry();
        e.isCurrent = true;
        e.type = t.getType();
        if (t instanceof AioSkillTask) {
            AioSkillTask s = (AioSkillTask) t;
            if (s.isTimeMode()) {
                long elapsed = s.elapsedMs()/1000;
                e.displayText = s.getSkillType().getDisplayName() + " (" + elapsed + "s)";
            } else {
                e.displayText = s.getSkillType().getDisplayName();
            }
            e.skillType = s.getSkillType();
            e.tooltip = "Current skill task";
        } else if (t instanceof AioQuestTask) {
            AioQuestTask q = (AioQuestTask) t;
            e.questType = q.getQuestType();
            e.displayText = q.getQuestType().getDisplayName();
            e.tooltip = "Current quest task";
        } else if (t instanceof AioMinigameTask) {
            AioMinigameTask m = (AioMinigameTask) t;
            e.minigameType = m.getMinigameType();
            e.displayText = m.getMinigameType().getDisplayName();
            e.tooltip = "Current minigame task";
        } else if (t instanceof AioTravelTask) { // NEW current travel
            AioTravelTask tr = (AioTravelTask) t;
            e.displayText = tr.getDisplay();
            e.tooltip = "Current travel task";
        } else {
            e.displayText = t.getDisplay();
            e.tooltip = t.getDisplay();
        }
        return e;
    }

    private static class TaskListEntry {
        int number; // 1-based for queued tasks
        String displayText;
        AioTask.TaskType type;
        SkillType skillType;
        QuestType questType;
        MinigameType minigameType;
        String tooltip;
        boolean isCurrent;
    }

    // ================== BEGIN MODIFICATIONS ==================
    // Removed language system: ensure translate() is identity and drop language state
    private String translate(String key) { return key; }
    // ================== END MODIFICATIONS ==================

    private void applyLanguage() {
        if (addSkillButton != null) addSkillButton.setText("Add Skill Task");
        if (addQuestButton != null) addQuestButton.setText("Add Quest Task");
        if (addMinigameButton != null) addMinigameButton.setText("Add Minigame Task");
        if (addTravelButton != null) addTravelButton.setText("Add Travel Task");
        if (removeSelectedButton != null) removeSelectedButton.setText("Remove Selected");
        if (startButton != null) startButton.setText("Start"); // shortened
        if (pauseButton != null) pauseButton.setText(script.isPaused()?"Resume":"Pause");
        if (clearButton != null) clearButton.setText("Clear Queue");
        if (refreshButton != null) refreshButton.setText("Refresh");
        if (skipButton != null) skipButton.setText("Skip"); // shortened
        if (hideButton != null) hideButton.setText("Hide");
        if (saveButton != null) saveButton.setText("Save");
        if (loadButton != null) loadButton.setText("Load");
        if (shuffleButton != null) shuffleButton.setText("Shuffle");
        if (editSelectedButton != null) editSelectedButton.setText("Edit");
        if (moveUpButton != null) moveUpButton.setToolTipText("Move Up");
        if (moveDownButton != null) moveDownButton.setToolTipText("Move Down");
        if (levelModeRadio != null) levelModeRadio.setText("Target Level");
        if (timeModeRadio != null) timeModeRadio.setText("Duration (min)");
        updatePanelBorder(skillPanelRef, "Skill Task");
        updatePanelBorder(questPanelRef, "Quest Task");
        updatePanelBorder(minigamePanelRef, "Minigame Task");
        updatePanelBorder(queuePanelRef, "Queue");
        updatePanelBorder(controlPanelRef, "Control");
        updatePanelBorder(statusPanelRef, "Status");
        updatePanelBorder(travelPanelRef, "Travel Task");
    }

    // NEW: enhanced control buttons sizing for better visibility and usability
    private void enhancePrimaryControlButtons() {
        JButton[] primaryButtons = { startButton, pauseButton, skipButton, hideButton };
        for (JButton b : primaryButtons) {
            if (b == null) continue;

            // Make buttons significantly larger and more prominent
            int width = 85;  // Increased from previous shrunk size
            int height = 32; // Increased from previous shrunk size

            b.setPreferredSize(new Dimension(width, height));
            b.setMinimumSize(new Dimension(width, height));
            b.setMaximumSize(new Dimension(width, height));

            // Better margins for improved appearance
            b.setMargin(new Insets(6, 8, 6, 8));

            // Larger, more readable font
            b.setFont(b.getFont().deriveFont(Font.BOLD, 12f));

            // Enhanced visual styling
            b.setFocusPainted(false);
            b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createRaisedBevelBorder(),
                    BorderFactory.createEmptyBorder(2, 4, 2, 4)
            ));
        }

        if (controlPanelRef != null) controlPanelRef.revalidate();
    }

    private void updatePanelBorder(JComponent comp, String key) {
        if (comp == null) return;
        if (comp.getBorder() instanceof TitledBorder) {
            ((TitledBorder)comp.getBorder()).setTitle(translate(key));
        }
    }

    private void updateConfigFromDialog(SkillType skillType, JComboBox<Object> methodCombo,
                                        Map<String, JComponent> components) {
        try {
            ConfigManager configManager = script.getConfigManager();
            String configGroup = script.getConfigGroup();

            Object selectedMethod = methodCombo.getSelectedItem();
            if (selectedMethod != null) {
                String methodKey = getConfigKeyForMethod(skillType);
                if (methodKey != null) {
                    String value = selectedMethod instanceof Enum ?
                            ((Enum<?>) selectedMethod).name() : selectedMethod.toString();
                    configManager.setConfiguration(configGroup, methodKey, value);
                }
            }

            updateSkillSpecificConfig(skillType, components, configManager, configGroup);

        } catch (Exception e) {
            System.out.println("Failed to update config for " + skillType + ": " + e.getMessage());
        }
    }

    private String getConfigKeyForMethod(SkillType skillType) {
        switch (skillType) {
            case ATTACK: return "attackStyle";
            case STRENGTH: return "strengthStyle";
            case DEFENCE: return "defenceStyle";
            case RANGED: return "rangedAmmo";
            case MAGIC: return "magicMethod";
            case PRAYER: return "prayerMethod";
            case MINING: return "miningMode";
            case SMITHING: return "smithingMethod";
            case FISHING: return "fishingMethod";
            case COOKING: return "cookingLocation";
            case FIREMAKING: return "firemakingLogs";
            case WOODCUTTING: return "woodcuttingTrees";
            case CRAFTING: return "craftingMethod";
            case FLETCHING: return "fletchingMethod";
            case HERBLORE: return "herbloreMethod";
            case RUNECRAFTING: return "runecraftingMethod";
            case CONSTRUCTION: return "constructionMethod";
            case AGILITY: return "agilityCourse";
            case THIEVING: return "thievingMethod";
            case SLAYER: return "slayerStrategy";
            case HUNTER: return "hunterMethod";
            case FARMING: return "farmingRunType";
            case HITPOINTS: return "hitpointsMethod";
            default: return null;
        }
    }

    private void updateSkillSpecificConfig(SkillType skillType, Map<String, JComponent> components,
                                           ConfigManager configManager, String configGroup) {
        try {
            switch (skillType) {
                case ATTACK:
                    if (components.containsKey("foodHP")) {
                        JSpinner spinner = (JSpinner) components.get("foodHP");
                        configManager.setConfiguration(configGroup, "attackFoodHP", spinner.getValue().toString());
                    }
                    if (components.containsKey("useSpec")) {
                        JCheckBox checkBox = (JCheckBox) components.get("useSpec");
                        configManager.setConfiguration(configGroup, "attackUseSpec", String.valueOf(checkBox.isSelected()));
                    }
                    break;

                case STRENGTH:
                    if (components.containsKey("foodHP")) {
                        JSpinner spinner = (JSpinner) components.get("foodHP");
                        configManager.setConfiguration(configGroup, "strengthFoodHP", spinner.getValue().toString());
                    }
                    if (components.containsKey("useSpec")) {
                        JCheckBox checkBox = (JCheckBox) components.get("useSpec");
                        configManager.setConfiguration(configGroup, "strengthUseSpec", String.valueOf(checkBox.isSelected()));
                    }
                    break;

                case DEFENCE:
                    if (components.containsKey("foodHP")) {
                        JSpinner spinner = (JSpinner) components.get("foodHP");
                        configManager.setConfiguration(configGroup, "defenceFoodHP", spinner.getValue().toString());
                    }
                    if (components.containsKey("useSpec")) {
                        JCheckBox checkBox = (JCheckBox) components.get("useSpec");
                        configManager.setConfiguration(configGroup, "defenceUseSpec", String.valueOf(checkBox.isSelected()));
                    }
                    break;

                case RANGED:
                    if (components.containsKey("foodHP")) {
                        JSpinner spinner = (JSpinner) components.get("foodHP");
                        configManager.setConfiguration(configGroup, "rangedFoodHP", spinner.getValue().toString());
                    }
                    if (components.containsKey("useSpec")) {
                        JCheckBox checkBox = (JCheckBox) components.get("useSpec");
                        configManager.setConfiguration(configGroup, "rangedUseSpec", String.valueOf(checkBox.isSelected()));
                    }
                    break;

                case MAGIC:
                    if (components.containsKey("splashProtection")) {
                        JCheckBox checkBox = (JCheckBox) components.get("splashProtection");
                        configManager.setConfiguration(configGroup, "magicSplashProtection", String.valueOf(checkBox.isSelected()));
                    }
                    break;

                case MINING:
                    if (components.containsKey("3tick")) {
                        JCheckBox checkBox = (JCheckBox) components.get("3tick");
                        configManager.setConfiguration(configGroup, "mining3Tick", String.valueOf(checkBox.isSelected()));
                    }
                    if (components.containsKey("ores")) {
                        JTextField textField = (JTextField) components.get("ores");
                        configManager.setConfiguration(configGroup, "miningOres", textField.getText());
                    }
                    break;

                case SMITHING:
                    if (components.containsKey("bars")) {
                        JTextField textField = (JTextField) components.get("bars");
                        configManager.setConfiguration(configGroup, "smithingBars", textField.getText());
                    }
                    break;

                case FISHING:
                    if (components.containsKey("fishingMode")) {
                        JComboBox<?> comboBox = (JComboBox<?>) components.get("fishingMode");
                        Object selected = comboBox.getSelectedItem();
                        if (selected instanceof Enum) {
                            configManager.setConfiguration(configGroup, "fishingMode", ((Enum<?>) selected).name());
                        }
                    }
                    if (components.containsKey("fishingMethod")) {
                        JComboBox<?> comboBox = (JComboBox<?>) components.get("fishingMethod");
                        Object selected = comboBox.getSelectedItem();
                        if (selected instanceof Enum) {
                            configManager.setConfiguration(configGroup, "fishingMethod", ((Enum<?>) selected).name());
                        }
                    }
                    if (components.containsKey("useSpec")) {
                        JCheckBox checkBox = (JCheckBox) components.get("useSpec");
                        configManager.setConfiguration(configGroup, "fishingSpecial", String.valueOf(checkBox.isSelected()));
                    }
                    break;

                case COOKING:
                    if (components.containsKey("gauntlets")) {
                        JCheckBox checkBox = (JCheckBox) components.get("gauntlets");
                        configManager.setConfiguration(configGroup, "cookingGauntlets", String.valueOf(checkBox.isSelected()));
                    }
                    break;

                case WOODCUTTING:
                    if (components.containsKey("nests")) {
                        JCheckBox checkBox = (JCheckBox) components.get("nests");
                        configManager.setConfiguration(configGroup, "woodcuttingNests", String.valueOf(checkBox.isSelected()));
                    }
                    if (components.containsKey("useSpec")) {
                        JCheckBox checkBox = (JCheckBox) components.get("useSpec");
                        configManager.setConfiguration(configGroup, "woodcuttingSpecial", String.valueOf(checkBox.isSelected()));
                    }
                    break;

                case AGILITY:
                    if (components.containsKey("stamina")) {
                        JCheckBox checkBox = (JCheckBox) components.get("stamina");
                        configManager.setConfiguration(configGroup, "agilityStamina", String.valueOf(checkBox.isSelected()));
                    }
                    if (components.containsKey("marks")) {
                        JCheckBox checkBox = (JCheckBox) components.get("marks");
                        configManager.setConfiguration(configGroup, "agilityMarks", String.valueOf(checkBox.isSelected()));
                    }
                    break;

                case SLAYER:
                    if (components.containsKey("cannon")) {
                        JCheckBox checkBox = (JCheckBox) components.get("cannon");
                        configManager.setConfiguration(configGroup, "slayerCannon", String.valueOf(checkBox.isSelected()));
                    }
                    break;

                case HUNTER:
                    if (components.containsKey("traps")) {
                        JSpinner spinner = (JSpinner) components.get("traps");
                        configManager.setConfiguration(configGroup, "hunterTraps", spinner.getValue().toString());
                    }
                    if (components.containsKey("relay")) {
                        JCheckBox checkBox = (JCheckBox) components.get("relay");
                        configManager.setConfiguration(configGroup, "hunterRelay", String.valueOf(checkBox.isSelected()));
                    }
                    break;

                case FARMING:
                    if (components.containsKey("compost")) {
                        JComboBox<?> comboBox = (JComboBox<?>) components.get("compost");
                        Object selected = comboBox.getSelectedItem();
                        if (selected instanceof Enum) {
                            configManager.setConfiguration(configGroup, "farmingCompost", ((Enum<?>) selected).name());
                        }
                    }
                    if (components.containsKey("birdhouses")) {
                        JCheckBox checkBox = (JCheckBox) components.get("birdhouses");
                        configManager.setConfiguration(configGroup, "farmingBirdhouses", String.valueOf(checkBox.isSelected()));
                    }
                    break;

                case CONSTRUCTION:
                    if (components.containsKey("servant")) {
                        JCheckBox checkBox = (JCheckBox) components.get("servant");
                        configManager.setConfiguration(configGroup, "constructionServant", String.valueOf(checkBox.isSelected()));
                    }
                    break;

                case HERBLORE:
                    if (components.containsKey("secondaries")) {
                        JCheckBox checkBox = (JCheckBox) components.get("secondaries");
                        configManager.setConfiguration(configGroup, "herbloreSecondaries", String.valueOf(checkBox.isSelected()));
                    }
                    break;

                case RUNECRAFTING:
                    if (components.containsKey("pouches")) {
                        JCheckBox checkBox = (JCheckBox) components.get("pouches");
                        configManager.setConfiguration(configGroup, "runecraftingPouches", String.valueOf(checkBox.isSelected()));
                    }
                    if (components.containsKey("repair")) {
                        JCheckBox checkBox = (JCheckBox) components.get("repair");
                        configManager.setConfiguration(configGroup, "runecraftingRepair", String.valueOf(checkBox.isSelected()));
                    }
                    break;
            }
        } catch (Exception e) {
            System.out.println("Failed to update skill-specific config: " + e.getMessage());
        }
    }

    private String formatMinutesToReadable(int minutes) {
        if (minutes <= 0) return "0m";
        if (minutes < 60) return minutes + "m";
        int hours = minutes / 60;
        int remMin = minutes % 60;
        if (hours < 24) {
            if (remMin == 0) return hours + "h";
            return hours + "h " + remMin + "m";
        }
        int days = hours / 24;
        int remHours = hours % 24;
        if (remHours == 0 && remMin == 0) return days + "d";
        if (remMin == 0) return days + "d " + remHours + "h";
        return days + "d " + remHours + "h " + remMin + "m";
    }

    // ================== BEGIN ADDED METHODS (compile helpers) ==================
    private void updateTrainingMethods() {
        if (trainingMethodCombo == null) return;
        SkillType selectedSkill = (SkillType) skillCombo.getSelectedItem();
        trainingMethodCombo.removeAllItems();
        if (selectedSkill == null) return;
        populateMethodCombo(trainingMethodCombo, selectedSkill);
        loadCurrentMethodSelection(selectedSkill);
    }

    private Object getCurrentConfigMethod(SkillType skill) {
        if (config == null || skill == null) return null;
        switch (skill) {
            case ATTACK: return config.attackStyle();
            case STRENGTH: return config.strengthStyle();
            case DEFENCE: return config.defenceStyle();
            case RANGED: return config.rangedAmmo();
            case MAGIC: return config.magicMethod();
            case PRAYER: return config.prayerMethod();
            case MINING: return config.miningMode();
            case SMITHING: return config.smithingMethod();
            case FISHING: return config.fishingMethod();
            case COOKING: return config.cookingLocation();
            case FIREMAKING: return config.firemakingLogs();
            case WOODCUTTING: return config.woodcuttingTrees();
            case CRAFTING: return config.craftingMethod();
            case FLETCHING: return config.fletchingMethod();
            case HERBLORE: return config.herbloreMethod();
            case RUNECRAFTING: return config.runecraftingMethod();
            case CONSTRUCTION: return config.constructionMethod();
            case AGILITY: return config.agilityCourse();
            case THIEVING: return config.thievingMethod();
            case SLAYER: return config.slayerStrategy();
            case HUNTER: return config.hunterMethod();
            case FARMING: return config.farmingRunType();
            case HITPOINTS: return config.hitpointsMethod();
            default: return null;
        }
    }

    private void loadCurrentMethodSelection(SkillType skill) {
        Object current = getCurrentConfigMethod(skill);
        if (current == null) return;
    }
    // ================== END ADDED METHODS ==================

    // ================== RE-ADDED MISSING METHODS ==================
    private void updateSkillMethodConfig() {
        SkillType st = (SkillType) skillCombo.getSelectedItem();
        Object sel = trainingMethodCombo.getSelectedItem();
        if (st == null || sel == null) return;
        try {
            String key = getConfigKeyForMethod(st);
            if (key == null) return;
            ConfigManager cm = script.getConfigManager();
            String group = script.getConfigGroup();
            String value = (sel instanceof Enum) ? ((Enum<?>) sel).name() : sel.toString();
            cm.setConfiguration(group, key, value);
        } catch (Exception ex) {
            System.out.println("updateSkillMethodConfig failed: " + ex.getMessage());
        }
    }

    private boolean showSkillConfigDialog(SkillType skillType) {
        if (skillType == null) return false;
        JDialog dialog = new JDialog(this, "Configure " + skillType.getDisplayName(), true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.gridx=0; gbc.gridy=0; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel("Training Method:"), gbc);
        JComboBox<Object> methodCombo = new JComboBox<>();
        populateMethodCombo(methodCombo, skillType);
        // preselect current method
        Object current = getCurrentConfigMethod(skillType);
        if (current != null) {
            for (int i=0;i<methodCombo.getItemCount();i++) {
                Object it = methodCombo.getItemAt(i);
                if (it != null && it.toString().equalsIgnoreCase(current.toString())) { methodCombo.setSelectedIndex(i); break; }
            }
        }
        gbc.gridx=1; panel.add(methodCombo, gbc);
        Map<String, JComponent> extra = new HashMap<>();
        gbc.gridx=0; gbc.gridy++; gbc.gridwidth=2; addSkillSpecificOptions(skillType, panel, extra, gbc);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = new JButton("OK"); JButton cancel = new JButton("Cancel");
        final boolean[] result = {false};
        ok.addActionListener(e -> { updateConfigFromDialog(skillType, methodCombo, extra); result[0] = true; dialog.dispose(); });
        cancel.addActionListener(e -> { dialog.dispose(); });
        buttons.add(ok); buttons.add(cancel);
        gbc.gridy++; panel.add(buttons, gbc);
        dialog.add(panel);
        dialog.pack(); dialog.setLocationRelativeTo(this); applyDialogTheme(dialog); dialog.setVisible(true);
        return result[0];
    }

    private void populateMethodCombo(JComboBox<Object> combo, SkillType skillType) {
        combo.removeAllItems();
        if (skillType == null) return;
        switch (skillType) {
            case ATTACK: case STRENGTH: case DEFENCE:
                for (AllInOneConfig.CombatStyle cs : AllInOneConfig.CombatStyle.values()) combo.addItem(cs); break;
            case RANGED:
                combo.addItem("Bronze arrows"); combo.addItem("Iron arrows"); combo.addItem("Steel arrows"); combo.addItem("Rune arrows"); combo.addItem("Amethyst arrows"); break;
            case MAGIC: for (AllInOneConfig.MagicMethod m : AllInOneConfig.MagicMethod.values()) combo.addItem(m); break;
            case PRAYER: for (AllInOneConfig.PrayerMethod m : AllInOneConfig.PrayerMethod.values()) combo.addItem(m); break;
            case MINING: for (AllInOneConfig.GatheringMode m : AllInOneConfig.GatheringMode.values()) combo.addItem(m); break;
            case SMITHING: for (AllInOneConfig.SmithingMethod m : AllInOneConfig.SmithingMethod.values()) combo.addItem(m); break;
            case FISHING: for (AllInOneConfig.FishingMethod m : AllInOneConfig.FishingMethod.values()) combo.addItem(m); break;
            case COOKING: for (AllInOneConfig.CookingLocation m : AllInOneConfig.CookingLocation.values()) combo.addItem(m); break;
            case FIREMAKING: for (AllInOneConfig.LogType m : AllInOneConfig.LogType.values()) combo.addItem(m); break;
            case WOODCUTTING: for (AllInOneConfig.TreeType m : AllInOneConfig.TreeType.values()) combo.addItem(m); break;
            case CRAFTING: for (AllInOneConfig.CraftingMethod m : AllInOneConfig.CraftingMethod.values()) combo.addItem(m); break;
            case FLETCHING: for (AllInOneConfig.FletchingMethod m : AllInOneConfig.FletchingMethod.values()) combo.addItem(m); break;
            case HERBLORE: for (AllInOneConfig.HerbloreMethod m : AllInOneConfig.HerbloreMethod.values()) combo.addItem(m); break;
            case RUNECRAFTING: for (AllInOneConfig.RunecraftingMethod m : AllInOneConfig.RunecraftingMethod.values()) combo.addItem(m); break;
            case CONSTRUCTION: for (AllInOneConfig.ConstructionMethod m : AllInOneConfig.ConstructionMethod.values()) combo.addItem(m); break;
            case AGILITY: for (AllInOneConfig.AgilityCourse m : AllInOneConfig.AgilityCourse.values()) combo.addItem(m); break;
            case THIEVING: for (AllInOneConfig.ThievingMethod m : AllInOneConfig.ThievingMethod.values()) combo.addItem(m); break;
            case SLAYER: for (AllInOneConfig.SlayerStrategy m : AllInOneConfig.SlayerStrategy.values()) combo.addItem(m); break;
            case HUNTER: for (AllInOneConfig.HunterMethod m : AllInOneConfig.HunterMethod.values()) combo.addItem(m); break;
            case FARMING: for (AllInOneConfig.FarmingRunType m : AllInOneConfig.FarmingRunType.values()) combo.addItem(m); break;
            case HITPOINTS: combo.addItem("Combat"); combo.addItem("NMZ"); combo.addItem("PC"); break;
            default: combo.addItem("Auto");
        }
    }

    private void addSkillSpecificOptions(SkillType skillType, JPanel panel, Map<String,JComponent> components, GridBagConstraints gbc) {
        // Append options starting at current gbc.gridy. Each option increments row.
        java.util.function.BiConsumer<String,JComponent> addRow = (label, comp) -> {
            gbc.gridx=0; gbc.gridwidth=1; gbc.fill=GridBagConstraints.NONE; panel.add(new JLabel(label), gbc);
            gbc.gridx=1; gbc.fill=GridBagConstraints.HORIZONTAL; panel.add(comp, gbc); gbc.gridy++; };
        switch (skillType) {
            case ATTACK: case STRENGTH: case DEFENCE: case RANGED: case THIEVING: {
                JSpinner food = new JSpinner(new SpinnerNumberModel(40,10,90,1)); addRow.accept("Food HP%:", food); components.put("foodHP", food);
                JCheckBox specOrDodgy = new JCheckBox(skillType==SkillType.THIEVING?"Use Dodgy?":"Use Spec?");
                gbc.gridx=0; gbc.gridwidth=2; gbc.fill=GridBagConstraints.NONE; panel.add(specOrDodgy, gbc); components.put(skillType==SkillType.THIEVING?"dodgy":"useSpec", specOrDodgy); gbc.gridy++;
                break; }
            case MINING: {
                JCheckBox three = new JCheckBox("3-Tick"); gbc.gridx=0; gbc.gridwidth=2; panel.add(three, gbc); components.put("3tick", three); gbc.gridy++;
                JTextField ores = new JTextField("iron,coal",12); addRow.accept("Ores:", ores); components.put("ores", ores); break; }
            case SMITHING: {
                JTextField bars = new JTextField("Steel",10); addRow.accept("Bars:", bars); components.put("bars", bars); break; }
            case FISHING: {
                JComboBox<Object> mode = new JComboBox<>(AllInOneConfig.GatheringMode.values()); addRow.accept("Mode:", mode); components.put("fishingMode", mode);
                JCheckBox spec = new JCheckBox("Use Spec?"); gbc.gridx=0; gbc.gridwidth=2; panel.add(spec, gbc); components.put("useSpec", spec); gbc.gridy++; break; }
            case COOKING: {
                JCheckBox gaunt = new JCheckBox("Gauntlets?"); gbc.gridx=0; gbc.gridwidth=2; panel.add(gaunt, gbc); components.put("gauntlets", gaunt); gbc.gridy++; break; }
            case WOODCUTTING: {
                JCheckBox nests = new JCheckBox("Bird Nests?"); gbc.gridx=0; gbc.gridwidth=2; panel.add(nests, gbc); components.put("nests", nests); gbc.gridy++;
                JCheckBox spec = new JCheckBox("Use Spec?"); panel.add(spec, gbc); components.put("useSpec", spec); gbc.gridy++; break; }
            case AGILITY: {
                JCheckBox stam = new JCheckBox("Stamina?"); gbc.gridx=0; gbc.gridwidth=2; panel.add(stam, gbc); components.put("stamina", stam); gbc.gridy++;
                JCheckBox marks = new JCheckBox("Collect Marks?"); panel.add(marks, gbc); components.put("marks", marks); gbc.gridy++; break; }
            case SLAYER: {
                JCheckBox cannon = new JCheckBox("Use Cannon?"); gbc.gridx=0; gbc.gridwidth=2; panel.add(cannon, gbc); components.put("cannon", cannon); gbc.gridy++; break; }
            case HUNTER: {
                JSpinner traps = new JSpinner(new SpinnerNumberModel(0,0,5,1)); addRow.accept("Traps:", traps); components.put("traps", traps);
                JCheckBox relay = new JCheckBox("Relay?"); gbc.gridx=0; gbc.gridwidth=2; panel.add(relay, gbc); components.put("relay", relay); gbc.gridy++; break; }
            case FARMING: {
                JComboBox<Object> compost = new JComboBox<>(AllInOneConfig.CompostType.values()); addRow.accept("Compost:", compost); components.put("compost", compost);
                JCheckBox bird = new JCheckBox("Birdhouses?"); gbc.gridx=0; gbc.gridwidth=2; panel.add(bird, gbc); components.put("birdhouses", bird); gbc.gridy++; break; }
            case CONSTRUCTION: {
                JCheckBox servant = new JCheckBox("Servant?"); gbc.gridx=0; gbc.gridwidth=2; panel.add(servant, gbc); components.put("servant", servant); gbc.gridy++; break; }
            case HERBLORE: {
                JCheckBox sec = new JCheckBox("Secondaries?"); gbc.gridx=0; gbc.gridwidth=2; panel.add(sec, gbc); components.put("secondaries", sec); gbc.gridy++; break; }
            case RUNECRAFTING: {
                JCheckBox pouch = new JCheckBox("Pouches?"); gbc.gridx=0; gbc.gridwidth=2; panel.add(pouch, gbc); components.put("pouches", pouch); gbc.gridy++;
                JCheckBox repair = new JCheckBox("Repair?"); panel.add(repair, gbc); components.put("repair", repair); gbc.gridy++; break; }
            case MAGIC: {
                JCheckBox splash = new JCheckBox("Splash Prot?"); gbc.gridx=0; gbc.gridwidth=2; panel.add(splash, gbc); components.put("splashProtection", splash); gbc.gridy++; break; }
            default: // no options
        }
    }
    // ================== END RE-ADDED MISSING METHODS ==================
}