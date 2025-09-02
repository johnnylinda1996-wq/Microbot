package net.runelite.client.plugins.microbot.jpnl.accountbuilder;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.enums.QuestType;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.enums.SkillType;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.enums.MinigameType;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.tasks.AioTask;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.tasks.AioSkillTask;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.tasks.AioQuestTask;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.tasks.AioMinigameTask;

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

    // Theme Management
    private enum Theme {
        DARK("🌙 Dark Theme", new Color(32, 36, 40), new Color(45, 50, 56), Color.WHITE, new Color(220, 50, 50)),
        LIGHT("☀️ Light Theme", new Color(245, 245, 245), new Color(255, 255, 255), Color.BLACK, new Color(180, 30, 30)),
        BLUE("💙 Blue Theme", new Color(25, 35, 45), new Color(35, 45, 60), Color.WHITE, new Color(100, 150, 255)),
        GREEN("💚 Green Theme", new Color(20, 40, 30), new Color(30, 50, 40), Color.WHITE, new Color(80, 200, 120)),
        RED("❤️ Red Theme", new Color(40, 20, 20), new Color(60, 30, 30), Color.WHITE, new Color(255, 100, 100));

        final String displayName;
        final Color background;
        final Color panelBackground;
        final Color foreground;
        final Color accent;
        final Color hoverColor; // New hover color for interactive elements

        Theme(String displayName, Color bg, Color panelBg, Color fg, Color accent) {
            this.displayName = displayName;
            this.background = bg;
            this.panelBackground = panelBg;
            this.foreground = fg;
            this.accent = accent;
            // Set hover color based on theme name
            if ("🌙 Default Theme(dark)".equals(displayName)) {
                this.hoverColor = new Color(255, 100, 100); // Light red hover for dark mode
            } else if ("☀️ White Theme(light)".equals(displayName)) {
                this.hoverColor = new Color(200, 60, 60); // Darker red hover for light mode
            } else {
                this.hoverColor = accent.brighter();
            }
        }
    }

    // Language support
    private enum Language {
        ENGLISH("English(default)", "en"),
        DUTCH("Nederlands", "nl"),
        GERMAN("Deutsch", "de"),
        FRENCH("Français", "fr");

        final String displayName;
        final String code;

        Language(String displayName, String code) {
            this.displayName = displayName;
            this.code = code;
        }

        // Translation methods
        public String translate(String key) {
            switch (this) {
                case DUTCH:
                    return translateDutch(key);
                case GERMAN:
                    return translateGerman(key);
                case FRENCH:
                    return translateFrench(key);
                default:
                    return key; // English as fallback
            }
        }

        private String translateDutch(String key) {
            switch (key) {
                case "Skill Task": return "Vaardigheid Taak";
                case "Quest Task": return "Quest Taak";
                case "Minigame Task": return "Minispel Taak";
                case "Target Level": return "Doelniveau";
                case "Duration (min)": return "Duur (min)";
                case "Add Skill Task": return "Voeg Vaardigheid Toe";
                case "Add Quest Task": return "Voeg Quest Toe";
                case "Add Minigame Task": return "Voeg Minispel Toe";
                case "Start / Resume": return "Start / Hervat";
                case "Pause": return "Pauze";
                case "Resume": return "Hervat";
                default: return key;
            }
        }

        private String translateGerman(String key) {
            switch (key) {
                case "Skill Task": return "Fertigkeits-Aufgabe";
                case "Quest Task": return "Quest-Aufgabe";
                case "Minigame Task": return "Minispiel-Aufgabe";
                case "Target Level": return "Ziellevel";
                case "Duration (min)": return "Dauer (min)";
                case "Add Skill Task": return "Fertigkeit hinzufügen";
                case "Add Quest Task": return "Quest hinzufügen";
                case "Add Minigame Task": return "Minispiel hinzufügen";
                case "Start / Resume": return "Start / Fortsetzen";
                case "Pause": return "Pause";
                case "Resume": return "Fortsetzen";
                default: return key;
            }
        }

        private String translateFrench(String key) {
            switch (key) {
                case "Skill Task": return "Tâche de Compétence";
                case "Quest Task": return "Tâche de Quête";
                case "Minigame Task": return "Tâche de Mini-jeu";
                case "Target Level": return "Niveau Cible";
                case "Duration (min)": return "Durée (min)";
                case "Add Skill Task": return "Ajouter Compétence";
                case "Add Quest Task": return "Ajouter Quête";
                case "Add Minigame Task": return "Ajouter Mini-jeu";
                case "Start / Resume": return "Démarrer / Reprendre";
                case "Pause": return "Pause";
                case "Resume": return "Reprendre";
                default: return key;
            }
        }
    }

    private Language currentLanguage = Language.ENGLISH;

    private Theme currentTheme = Theme.DARK;
    private Preferences prefs = Preferences.userNodeForPackage(AllInOneBotGUI.class);

    // Layout visibility options
    private boolean showSkillPanel = true;
    private boolean showQuestPanel = true;
    private boolean showMinigamePanel = true;
    private boolean showStatusPanel = true;
    private boolean showControlPanel = true;
    private boolean compactMode = false;

    private JComboBox<SkillType> skillCombo;
    private JSpinner targetLevelSpinner;
    private JComboBox<Object> trainingMethodCombo; // NEW: Dynamic training method selector
    private JButton addSkillButton;

    private JComboBox<QuestType> questCombo;
    private JButton addQuestButton;
    private JComboBox<MinigameType> minigameCombo;
    private JSpinner minigameDurationSpinner; // NEW: Duration for minigames
    private JButton addMinigameButton;

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
    private JTextField filterField;

    private final Timer uiTimer;

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

    // Keep panel references for language/theme updates
    private JPanel skillPanelRef, questPanelRef, minigamePanelRef, queuePanelRef, controlPanelRef, statusPanelRef;

    // Menu item references for language switching
    private JMenu appearanceMenuRef; // to add language submenu here instead of Help
    private JMenu languageMenuRef;

    private JMenuItem resetLayoutItemRef; // For translation if desired later

    // Store original window position to prevent unwanted repositioning
    private Point originalLocation;
    private boolean preserveLocation = false;

    public AllInOneBotGUI(AllInOneScript script) {
        super("🚀 Account Builder v0.1");
        this.script = script;
        this.config = script.getConfig(); // Direct config access

        // Set custom icon for the window
        setCustomWindowIcon();

        loadPreferences();
        initComponents();
        createMenuBar();
        layoutComponents();
        attachListeners();
        applyThemeTweaks();
        refreshQueue();

        // Store initial position and enable location preservation
        updateWindowSize();
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        // Set initial position but don't force it
        setLocationRelativeTo(null);

        // Store the location after initial setup
        SwingUtilities.invokeLater(() -> {
            originalLocation = getLocation();
            preserveLocation = true;
        });

        setAlwaysOnTop(false);
        setVisible(true);

        uiTimer = new Timer(1000, e -> refreshStatus());
        uiTimer.start();
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
        compactMode = prefs.getBoolean("compactMode", false);

        String langCode = prefs.get("language", Language.ENGLISH.code);
        for (Language l : Language.values()) {
            if (l.code.equalsIgnoreCase(langCode)) { currentLanguage = l; break; }
        }
    }

    private void savePreferences() {
        prefs.put("theme", currentTheme.name());
        prefs.putBoolean("showSkillPanel", showSkillPanel);
        prefs.putBoolean("showQuestPanel", showQuestPanel);
        prefs.putBoolean("showMinigamePanel", showMinigamePanel);
        prefs.putBoolean("showStatusPanel", showStatusPanel);
        prefs.putBoolean("showControlPanel", showControlPanel);
        prefs.putBoolean("compactMode", compactMode);
        prefs.put("language", currentLanguage.code);
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

        // Layout submenu
        JMenu layoutMenu = new JMenu("Layout Options");

        JCheckBoxMenuItem compactItem = new JCheckBoxMenuItem("Compact Mode");
        compactItem.setSelected(compactMode);
        compactItem.addActionListener(e -> {
            compactMode = compactItem.isSelected();
            taskList.setFixedCellHeight(compactMode ? 18 : 28);
            updatePanelVisibility(); // Complete refresh instead of just layout
            savePreferences();
        });
        layoutMenu.add(compactItem);

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

        // Language submenu
        appearanceMenu.addSeparator();
        languageMenuRef = new JMenu("Language");
        ButtonGroup languageGroup = new ButtonGroup();
        for (Language lang : Language.values()) {
            JRadioButtonMenuItem langItem = new JRadioButtonMenuItem(lang.displayName);
            langItem.setSelected(lang == currentLanguage);
            langItem.addActionListener(e -> {
                currentLanguage = lang;
                applyLanguage();
                savePreferences();
            });
            languageGroup.add(langItem);
            languageMenuRef.add(langItem);
        }
        appearanceMenu.add(languageMenuRef);

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

        menuBar.add(appearanceMenu);
        menuBar.add(toolsMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    private void updatePanelVisibility() {
        layoutComponents();
        applyLanguage();
        applyThemeTweaks();
        updateWindowSize();
        revalidate();
        repaint();
    }

    private void resetLayout() {
        showSkillPanel = true;
        showQuestPanel = true;
        showMinigamePanel = true;
        showStatusPanel = true;
        showControlPanel = true;
        compactMode = false;
        currentTheme = Theme.DARK;
        currentLanguage = Language.ENGLISH;

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
            }
        } catch (Exception e) {
            // Skip invalid lines
        }
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
                loadButton, shuffleButton};

        for (JButton b : buttons) {
            if (b != null) {
                b.setFocusPainted(false);
                b.setBackground(bgAlt.darker());
                b.setForeground(fgColor);
                b.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(bgAlt.brighter(), 1),
                        BorderFactory.createEmptyBorder(4,8,4,8)));
            }
        }

        // Apply label styling
        JLabel[] labels = {currentTaskLabel, statusLabel, levelLabel, xpGainedLabel,
                xpPerHourLabel, xpRemainingLabel, elapsedLabel, tasksSummaryLabel};

        for (JLabel lab : labels) {
            if (lab != null) {
                lab.setForeground(currentTheme == Theme.LIGHT ? Color.DARK_GRAY : Color.LIGHT_GRAY);
            }
        }

        // Apply combo box styling
        JComboBox<?>[] combos = {skillCombo, questCombo, minigameCombo};
        for (JComboBox<?> combo : combos) {
            if (combo != null) {
                combo.setBackground(bgAlt);
                combo.setForeground(fgColor);
            }
        }

        // Apply spinner styling
        JSpinner[] spinners = {targetLevelSpinner, minutesSpinner, minigameDurationSpinner};
        for (JSpinner spinner : spinners) {
            if (spinner != null) {
                spinner.getEditor().getComponent(0).setBackground(bgAlt);
                spinner.getEditor().getComponent(0).setForeground(fgColor);
            }
        }

        // Apply filter field styling
        if (filterField != null) {
            filterField.setBackground(bgAlt);
            filterField.setForeground(fgColor);
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
        if (c instanceof JComponent) {
            TitledBorder tb = null;
            if ((tb = getTitledBorder((JComponent)c)) != null) {
                tb.setTitleColor(Color.WHITE);
                tb.setTitleFont(bold);
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
                .toArray(MinigameType[]::new));
        minigameCombo.setRenderer(new MinigameIconRenderer());
        addMinigameButton = new JButton("Add Minigame Task");

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
        filterField = new JTextField();
        filterField.setToolTipText("Filter tasks (substring)");
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
        skipButton = new JButton("Skip Current");
        hideButton = new JButton("Hide");
        saveButton = new JButton("Save");
        loadButton = new JButton("Load");
        shuffleButton = new JButton("Shuffle");

        generateControlIcons();
        applyControlIcons();
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

    private void applyControlIcons() {
        startButton.setIcon(playIcon);
        pauseButton.setIcon(pauseIcon);
        skipButton.setIcon(skipIcon);
        hideButton.setIcon(hideIcon);
        saveButton.setIcon(saveIconSmall);
        loadButton.setIcon(loadIconSmall);
        shuffleButton.setIcon(shuffleIcon);
        startButton.setHorizontalTextPosition(SwingConstants.RIGHT);
        pauseButton.setHorizontalTextPosition(SwingConstants.RIGHT);
        skipButton.setHorizontalTextPosition(SwingConstants.RIGHT);
        hideButton.setHorizontalTextPosition(SwingConstants.RIGHT);
        saveButton.setHorizontalTextPosition(SwingConstants.RIGHT);
        loadButton.setHorizontalTextPosition(SwingConstants.RIGHT);
        shuffleButton.setHorizontalTextPosition(SwingConstants.RIGHT);
    }

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
            // Theme adaptive colors
            Color even, odd, sel, currentColor, baseFg;
            if (currentTheme == Theme.LIGHT) {
                even = new Color(245,245,245);
                odd = new Color(235,235,235);
                sel = new Color(200,230,255);
                currentColor = new Color(220,250,220);
                baseFg = Color.DARK_GRAY;
            } else {
                even = new Color(50,55,60);
                odd = new Color(44,48,52);
                sel = new Color(80,120,160);
                currentColor = new Color(60,100,60);
                baseFg = Color.WHITE;
            }
            if (value instanceof TaskListEntry) {
                TaskListEntry e = (TaskListEntry) value;
                String base = e.displayText;
                if (e.isCurrent) base = "▶ " + base; else base = e.number + ". " + base;
                lbl.setText(base);
                if (e.type == AioTask.TaskType.SKILL && e.skillType != null) lbl.setIcon(SKILL_ICONS.get(e.skillType));
                else if (e.type == AioTask.TaskType.QUEST) lbl.setIcon(QUEST_ICON);
                else if (e.type == AioTask.TaskType.MINIGAME) {
                    Icon ic = MINIGAME_ICONS.get(e.minigameType); lbl.setIcon(ic != null ? ic : MINIGAME_FALLBACK_ICON);
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
                        BorderFactory.createMatteBorder(0,0,1,0,currentTheme == Theme.LIGHT ? new Color(210,210,210) : new Color(30,35,40)),
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

        // Add button
        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2; skillPanelRef.add(addSkillButton, gbc);

        return skillPanelRef;
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

    private JPanel buildQueuePanel() {
        queuePanelRef = new JPanel(new BorderLayout());
        queuePanelRef.setBorder(new TitledBorder(translate("Queue")));
        JPanel top = new JPanel(new BorderLayout(4,4));
        JLabel filterLbl = new JLabel(translate("Filter:"));
        filterLbl.setPreferredSize(new Dimension(50, filterLbl.getPreferredSize().height));
        top.add(filterLbl, BorderLayout.WEST);
        top.add(filterField, BorderLayout.CENTER);
        JPanel topRight = new JPanel(new BorderLayout());
        topRight.setOpaque(false);
        topRight.add(tasksSummaryLabel, BorderLayout.NORTH);
        topRight.add(typeCountsLabel, BorderLayout.SOUTH);
        top.add(topRight, BorderLayout.EAST);
        queuePanelRef.add(top, BorderLayout.NORTH);
        taskList.setFixedCellHeight(compactMode ? 18 : 28);
        JScrollPane sp = new JScrollPane(taskList);
        queuePanelRef.add(sp, BorderLayout.CENTER);

        // Reorganized button layout - place Load, Save, Refresh, Clear Queue above other buttons
        JPanel buttonArea = new JPanel(new BorderLayout());

        // Top row: Load, Save, Refresh, Clear Queue
        JPanel topButtons = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 2));
        topButtons.add(loadButton);
        topButtons.add(saveButton);
        topButtons.add(refreshButton);
        topButtons.add(clearButton);
        buttonArea.add(topButtons, BorderLayout.NORTH);

        // Bottom row: Remove selected, Edit, Arrow up, Arrow down, Shuffle
        JPanel bottomButtons = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 2));
        bottomButtons.add(removeSelectedButton);
        bottomButtons.add(editSelectedButton);
        bottomButtons.add(moveUpButton);
        bottomButtons.add(moveDownButton);
        bottomButtons.add(shuffleButton);
        buttonArea.add(bottomButtons, BorderLayout.SOUTH);

        queuePanelRef.add(buttonArea, BorderLayout.SOUTH);

        // Make the queue panel slightly wider to accommodate all buttons
        if (!compactMode) queuePanelRef.setPreferredSize(new Dimension(350, 380));
        else queuePanelRef.setPreferredSize(new Dimension(300, 320));
        return queuePanelRef;
    }

    private JPanel buildControlPanel() {
        controlPanelRef = new JPanel(compactMode ? new GridLayout(1,0,3,3) : new GridLayout(0,1,3,3));
        controlPanelRef.setBorder(new TitledBorder(translate("Control")));
        controlPanelRef.add(startButton);
        controlPanelRef.add(pauseButton);
        controlPanelRef.add(skipButton);
        if (!compactMode) controlPanelRef.add(hideButton);
        return controlPanelRef;
    }

    private JPanel buildStatusPanel() {
        statusPanelRef = new JPanel(new GridLayout(compactMode ? 0 : 0,1,2,2));
        statusPanelRef.setBorder(new TitledBorder(translate("Status")));
        if (compactMode) {
            JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT,4,2));
            row1.add(currentTaskLabel); row1.add(statusLabel);
            JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT,4,2));
            row2.add(levelLabel); row2.add(xpGainedLabel); row2.add(xpPerHourLabel);
            statusPanelRef.setLayout(new BoxLayout(statusPanelRef, BoxLayout.Y_AXIS));
            statusPanelRef.add(row1); statusPanelRef.add(row2); statusPanelRef.add(progressBar);
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
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.add(tp, BorderLayout.CENTER);
        return wrap;
    }

    private void updateWindowSize() {
        if (compactMode) {
            setMinimumSize(new Dimension(500, 400));
        } else {
            setMinimumSize(new Dimension(820, 600));
        }
        pack();
        setLocationRelativeTo(null);
    }

    private void attachListeners() {
        addSkillButton.addActionListener(e -> {
            // Store current location before adding task
            if (preserveLocation) {
                originalLocation = getLocation();
            }

            SkillType st = (SkillType) skillCombo.getSelectedItem();

            // NEW: Show skill-specific config popup before adding task
            if (showSkillConfigDialog(st)) {
                if (timeModeRadio.isSelected()) {
                    int mins = (int) minutesSpinner.getValue();
                    script.addSkillTaskTime(st, mins);
                } else {
                    int target = (int) targetLevelSpinner.getValue();
                    script.addSkillTask(st, target);
                }
                refreshQueue();
            }

            // Restore location after adding task
            if (preserveLocation && originalLocation != null) {
                SwingUtilities.invokeLater(() -> setLocation(originalLocation));
            }
        });

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

        // Filter listener
        filterField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { refreshQueue(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { refreshQueue(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refreshQueue(); }
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
        taskModel.clear();
        String filter = filterField.getText();
        if (filter != null) filter = filter.trim().toLowerCase();
        AioTask cur = script.getCurrentTask();
        if (cur != null && !cur.isComplete()) taskModel.addElement(buildEntryCurrent(cur));
        java.util.List<AioTask> raw = script.getQueueSnapshotRaw();
        int number = 1;
        int skillCount=0, questCount=0, miniCount=0;
        for (AioTask t : raw) {
            if (t.getType() == AioTask.TaskType.SKILL) skillCount++;
            else if (t.getType()==AioTask.TaskType.QUEST) questCount++;
            else if (t.getType()==AioTask.TaskType.MINIGAME) miniCount++;
            TaskListEntry e = buildEntry(t, number);
            if (filter == null || filter.isEmpty() || e.displayText.toLowerCase().contains(filter)) {
                taskModel.addElement(e);
            }
            number++;
        }
        tasksSummaryLabel.setText("Tasks: " + raw.size());
        typeCountsLabel.setText("<html><span style='color:#6fa8dc'>Skills: " + skillCount + "</span> | <span style='color:#f1c232'>Quests: " + questCount + "</span> | <span style='color:#b4a7d6'>Minigames: " + miniCount + "</span></html>");
        taskList.repaint();

        // Adjust preferred size when queue changes (auto sizing)
        SwingUtilities.invokeLater(this::updateWindowSize);
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
        gbc.insets = new Insets(4,4,4,4);
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

        dialog.setVisible(true);
        return result[0];
    }

    private void toggleTray() {
        if (!SystemTray.isSupported()) {
            setState(Frame.ICONIFIED);
            return;
        }
        try {
            if (tray == null) {
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

    private int toUnderlyingQueueIndex(int listIndex) {
        if (listIndex < 0) return -1;
        TaskListEntry e = taskModel.get(listIndex);
        if (e.isCurrent) return -1;
        return e.number - 1;
    }

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
        } else {
            e.displayText = t.getDisplay();
            e.tooltip = t.getDisplay();
        }
        return e;
    }

    private String formatMinutesToReadable(int totalMinutes) {
        if (totalMinutes < 60) {
            return totalMinutes + " Minutes";
        }

        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;

        if (minutes == 0) {
            return hours + (hours == 1 ? " Hour" : " Hours");
        } else {
            return hours + (hours == 1 ? " Hour " : " Hours ") + minutes + " Minutes";
        }
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

    private String translate(String key) {
        return currentLanguage.translate(key);
    }

    private void applyLanguage() {
        if (addSkillButton != null) addSkillButton.setText(translate("Add Skill Task"));
        if (addQuestButton != null) addQuestButton.setText(translate("Add Quest Task"));
        if (addMinigameButton != null) addMinigameButton.setText(translate("Add Minigame Task"));
        if (removeSelectedButton != null) removeSelectedButton.setText(translate("Remove Selected"));
        if (startButton != null) startButton.setText(translate("Start / Resume"));
        if (pauseButton != null) pauseButton.setText(translate(script.isPaused()?"Resume":"Pause"));
        if (clearButton != null) clearButton.setText(translate("Clear Queue"));
        if (refreshButton != null) refreshButton.setText(translate("Refresh"));
        if (skipButton != null) skipButton.setText(translate("Skip Current"));
        if (hideButton != null) hideButton.setText(translate("Hide"));
        if (saveButton != null) saveButton.setText(translate("Save"));
        if (loadButton != null) loadButton.setText(translate("Load"));
        if (shuffleButton != null) shuffleButton.setText(translate("Shuffle"));
        if (editSelectedButton != null) editSelectedButton.setText(translate("Edit"));
        if (moveUpButton != null) moveUpButton.setToolTipText(translate("Move Up"));
        if (moveDownButton != null) moveDownButton.setToolTipText(translate("Move Down"));
        if (levelModeRadio != null) levelModeRadio.setText(translate("Target Level"));
        if (timeModeRadio != null) timeModeRadio.setText(translate("Duration (min)"));

        updatePanelBorder(skillPanelRef, "Skill Task");
        updatePanelBorder(questPanelRef, "Quest Task");
        updatePanelBorder(minigamePanelRef, "Minigame Task");
        updatePanelBorder(queuePanelRef, "Queue");
        updatePanelBorder(controlPanelRef, "Control");
        updatePanelBorder(statusPanelRef, "Status");

        repaint();
    }

    private void updatePanelBorder(JComponent comp, String key) {
        if (comp == null) return;
        if (comp.getBorder() instanceof TitledBorder) {
            ((TitledBorder)comp.getBorder()).setTitle(translate(key));
        }
    }

    private void updateTrainingMethods() {
        SkillType selectedSkill = (SkillType) skillCombo.getSelectedItem();
        if (selectedSkill == null) return;

        trainingMethodCombo.removeAllItems();

        switch (selectedSkill) {
            case ATTACK:
            case STRENGTH:
            case DEFENCE:
                for (AllInOneConfig.CombatStyle style : AllInOneConfig.CombatStyle.values()) {
                    trainingMethodCombo.addItem(style);
                }
                break;

            case RANGED:
                trainingMethodCombo.addItem("Iron arrows");
                trainingMethodCombo.addItem("Steel arrows");
                trainingMethodCombo.addItem("Mithril arrows");
                trainingMethodCombo.addItem("Adamant arrows");
                trainingMethodCombo.addItem("Rune arrows");
                trainingMethodCombo.addItem("Dragon arrows");
                break;

            case MAGIC:
                for (AllInOneConfig.MagicMethod method : AllInOneConfig.MagicMethod.values()) {
                    trainingMethodCombo.addItem(method);
                }
                break;

            case PRAYER:
                for (AllInOneConfig.PrayerMethod method : AllInOneConfig.PrayerMethod.values()) {
                    trainingMethodCombo.addItem(method);
                }
                break;

            case MINING:
                for (AllInOneConfig.GatheringMode mode : AllInOneConfig.GatheringMode.values()) {
                    trainingMethodCombo.addItem(mode);
                }
                break;

            case SMITHING:
                for (AllInOneConfig.SmithingMethod method : AllInOneConfig.SmithingMethod.values()) {
                    trainingMethodCombo.addItem(method);
                }
                break;

            case FISHING:
                for (AllInOneConfig.FishingMethod method : AllInOneConfig.FishingMethod.values()) {
                    trainingMethodCombo.addItem(method);
                }
                break;

            case COOKING:
                for (AllInOneConfig.CookingLocation location : AllInOneConfig.CookingLocation.values()) {
                    trainingMethodCombo.addItem(location);
                }
                break;

            case FIREMAKING:
                for (AllInOneConfig.LogType logType : AllInOneConfig.LogType.values()) {
                    trainingMethodCombo.addItem(logType);
                }
                break;

            case WOODCUTTING:
                for (AllInOneConfig.TreeType treeType : AllInOneConfig.TreeType.values()) {
                    trainingMethodCombo.addItem(treeType);
                }
                break;

            case CRAFTING:
                for (AllInOneConfig.CraftingMethod method : AllInOneConfig.CraftingMethod.values()) {
                    trainingMethodCombo.addItem(method);
                }
                break;

            case FLETCHING:
                for (AllInOneConfig.FletchingMethod method : AllInOneConfig.FletchingMethod.values()) {
                    trainingMethodCombo.addItem(method);
                }
                break;

            case HERBLORE:
                for (AllInOneConfig.HerbloreMethod method : AllInOneConfig.HerbloreMethod.values()) {
                    trainingMethodCombo.addItem(method);
                }
                break;

            case RUNECRAFTING:
                for (AllInOneConfig.RunecraftingMethod method : AllInOneConfig.RunecraftingMethod.values()) {
                    trainingMethodCombo.addItem(method);
                }
                break;

            case CONSTRUCTION:
                for (AllInOneConfig.ConstructionMethod method : AllInOneConfig.ConstructionMethod.values()) {
                    trainingMethodCombo.addItem(method);
                }
                break;

            case AGILITY:
                for (AllInOneConfig.AgilityCourse course : AllInOneConfig.AgilityCourse.values()) {
                    trainingMethodCombo.addItem(course);
                }
                break;

            case THIEVING:
                for (AllInOneConfig.ThievingMethod method : AllInOneConfig.ThievingMethod.values()) {
                    trainingMethodCombo.addItem(method);
                }
                break;

            case SLAYER:
                for (AllInOneConfig.SlayerStrategy strategy : AllInOneConfig.SlayerStrategy.values()) {
                    trainingMethodCombo.addItem(strategy);
                }
                break;

            case HUNTER:
                for (AllInOneConfig.HunterMethod method : AllInOneConfig.HunterMethod.values()) {
                    trainingMethodCombo.addItem(method);
                }
                break;

            case FARMING:
                for (AllInOneConfig.FarmingRunType runType : AllInOneConfig.FarmingRunType.values()) {
                    trainingMethodCombo.addItem(runType);
                }
                break;

            case HITPOINTS:
                trainingMethodCombo.addItem("Combat");
                trainingMethodCombo.addItem("Safe Spotting");
                trainingMethodCombo.addItem("Pest Control");
                break;

            default:
                trainingMethodCombo.addItem("Auto");
                break;
        }

        loadCurrentMethodSelection(selectedSkill);
    }

    private void loadCurrentMethodSelection(SkillType skill) {
        try {
            Object currentMethod = getCurrentConfigMethod(skill);
            if (currentMethod != null) {
                for (int i = 0; i < trainingMethodCombo.getItemCount(); i++) {
                    Object item = trainingMethodCombo.getItemAt(i);
                    if (item.toString().equals(currentMethod.toString()) ||
                            (item instanceof Enum && currentMethod instanceof Enum &&
                                    ((Enum<?>) item).name().equals(((Enum<?>) currentMethod).name()))) {
                        trainingMethodCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to load current method for " + skill + ": " + e.getMessage());
        }
    }

    private Object getCurrentConfigMethod(SkillType skill) {
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
            default: return "Auto";
        }
    }

    private void updateSkillMethodConfig() {
        SkillType selectedSkill = (SkillType) skillCombo.getSelectedItem();
        Object selectedMethod = trainingMethodCombo.getSelectedItem();

        if (selectedSkill == null || selectedMethod == null) return;

        try {
            System.out.println("Updated " + selectedSkill.getDisplayName() + " method to: " + selectedMethod);
        } catch (Exception e) {
            System.out.println("Failed to update config for " + selectedSkill + ": " + e.getMessage());
        }
    }

    private boolean showSkillConfigDialog(SkillType skillType) {
        JDialog dialog = new JDialog(this, "Configure " + skillType.getDisplayName(), true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        Icon skillIcon = SKILL_ICONS.get(skillType);
        if (skillIcon != null) {
            JLabel iconLabel = new JLabel(skillIcon);
            titlePanel.add(iconLabel);
        }
        JLabel titleLabel = new JLabel("Configure " + skillType.getDisplayName() + " Training");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        titlePanel.add(titleLabel);
        mainPanel.add(titlePanel, BorderLayout.NORTH);

        JPanel configPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        configPanel.add(new JLabel("Training Method:"), gbc);

        JComboBox<Object> methodCombo = new JComboBox<>();
        populateMethodCombo(methodCombo, skillType);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        configPanel.add(methodCombo, gbc);

        Map<String, JComponent> additionalComponents = new HashMap<>();
        addSkillSpecificOptions(skillType, configPanel, additionalComponents, gbc);

        mainPanel.add(configPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        final boolean[] result = {false};

        okButton.addActionListener(e -> {
            updateConfigFromDialog(skillType, methodCombo, additionalComponents);
            result[0] = true;
            dialog.dispose();
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

        applyDialogTheme(dialog);

        dialog.setVisible(true);
        return result[0];
    }

    private void populateMethodCombo(JComboBox<Object> combo, SkillType skillType) {
        switch (skillType) {
            case ATTACK:
            case STRENGTH:
            case DEFENCE:
                for (AllInOneConfig.CombatStyle style : AllInOneConfig.CombatStyle.values()) {
                    combo.addItem(style);
                }
                break;
            case RANGED:
                combo.addItem("Iron arrows");
                combo.addItem("Steel arrows");
                combo.addItem("Mithril arrows");
                combo.addItem("Adamant arrows");
                combo.addItem("Rune arrows");
                combo.addItem("Dragon arrows");
                break;
            case MAGIC:
                for (AllInOneConfig.MagicMethod method : AllInOneConfig.MagicMethod.values()) {
                    combo.addItem(method);
                }
                break;
            case PRAYER:
                for (AllInOneConfig.PrayerMethod method : AllInOneConfig.PrayerMethod.values()) {
                    combo.addItem(method);
                }
                break;
            case MINING:
                for (AllInOneConfig.GatheringMode mode : AllInOneConfig.GatheringMode.values()) {
                    combo.addItem(mode);
                }
                break;
            case SMITHING:
                for (AllInOneConfig.SmithingMethod method : AllInOneConfig.SmithingMethod.values()) {
                    combo.addItem(method);
                }
                break;
            case FISHING:
                for (AllInOneConfig.FishingMethod method : AllInOneConfig.FishingMethod.values()) {
                    combo.addItem(method);
                }
                break;
            case COOKING:
                for (AllInOneConfig.CookingLocation location : AllInOneConfig.CookingLocation.values()) {
                    combo.addItem(location);
                }
                break;
            case FIREMAKING:
                for (AllInOneConfig.LogType logType : AllInOneConfig.LogType.values()) {
                    combo.addItem(logType);
                }
                break;
            case WOODCUTTING:
                for (AllInOneConfig.TreeType treeType : AllInOneConfig.TreeType.values()) {
                    combo.addItem(treeType);
                }
                break;
            case CRAFTING:
                for (AllInOneConfig.CraftingMethod method : AllInOneConfig.CraftingMethod.values()) {
                    combo.addItem(method);
                }
                break;
            case FLETCHING:
                for (AllInOneConfig.FletchingMethod method : AllInOneConfig.FletchingMethod.values()) {
                    combo.addItem(method);
                }
                break;
            case HERBLORE:
                for (AllInOneConfig.HerbloreMethod method : AllInOneConfig.HerbloreMethod.values()) {
                    combo.addItem(method);
                }
                break;
            case RUNECRAFTING:
                for (AllInOneConfig.RunecraftingMethod method : AllInOneConfig.RunecraftingMethod.values()) {
                    combo.addItem(method);
                }
                break;
            case CONSTRUCTION:
                for (AllInOneConfig.ConstructionMethod method : AllInOneConfig.ConstructionMethod.values()) {
                    combo.addItem(method);
                }
                break;
            case AGILITY:
                for (AllInOneConfig.AgilityCourse course : AllInOneConfig.AgilityCourse.values()) {
                    combo.addItem(course);
                }
                break;
            case THIEVING:
                for (AllInOneConfig.ThievingMethod method : AllInOneConfig.ThievingMethod.values()) {
                    combo.addItem(method);
                }
                break;
            case SLAYER:
                for (AllInOneConfig.SlayerStrategy strategy : AllInOneConfig.SlayerStrategy.values()) {
                    combo.addItem(strategy);
                }
                break;
            case HUNTER:
                for (AllInOneConfig.HunterMethod method : AllInOneConfig.HunterMethod.values()) {
                    combo.addItem(method);
                }
                break;
            case FARMING:
                for (AllInOneConfig.FarmingRunType runType : AllInOneConfig.FarmingRunType.values()) {
                    combo.addItem(runType);
                }
                break;
            case HITPOINTS:
                combo.addItem("Combat");
                combo.addItem("Safe Spotting");
                combo.addItem("Pest Control");
                break;
            default:
                combo.addItem("Auto");
                break;
        }

        try {
            Object currentMethod = getCurrentConfigMethod(skillType);
            if (currentMethod != null) {
                for (int i = 0; i < combo.getItemCount(); i++) {
                    Object item = combo.getItemAt(i);
                    if (item.toString().equals(currentMethod.toString()) ||
                            (item instanceof Enum && currentMethod instanceof Enum &&
                                    ((Enum<?>) item).name().equals(((Enum<?>) currentMethod).name()))) {
                        combo.setSelectedIndex(i);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            // Use default selection
        }
    }

    private void addSkillSpecificOptions(SkillType skillType, JPanel configPanel,
                                         Map<String, JComponent> components, GridBagConstraints gbc) {
        gbc.gridy++;

        switch (skillType) {
            case ATTACK:
            case STRENGTH:
            case DEFENCE:
                gbc.gridx = 0; gbc.fill = GridBagConstraints.NONE;
                configPanel.add(new JLabel("Food HP %:"), gbc);
                JSpinner foodHpSpinner = new JSpinner(new SpinnerNumberModel(40, 10, 90, 5));
                gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
                configPanel.add(foodHpSpinner, gbc);
                components.put("foodHP", foodHpSpinner);

                gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
                JCheckBox useSpecCheckBox = new JCheckBox("Use Special Attack");
                configPanel.add(useSpecCheckBox, gbc);
                components.put("useSpec", useSpecCheckBox);
                break;

            case RANGED:
                gbc.gridx = 0; gbc.fill = GridBagConstraints.NONE; gbc.gridwidth = 1;
                configPanel.add(new JLabel("Food HP %:"), gbc);
                JSpinner rangedFoodSpinner = new JSpinner(new SpinnerNumberModel(50, 10, 90, 5));
                gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
                configPanel.add(rangedFoodSpinner, gbc);
                components.put("foodHP", rangedFoodSpinner);

                gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
                JCheckBox rangedSpecCheckBox = new JCheckBox("Use Special Attack");
                configPanel.add(rangedSpecCheckBox, gbc);
                components.put("useSpec", rangedSpecCheckBox);
                break;

            case MAGIC:
                gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
                JCheckBox splashCheckBox = new JCheckBox("Splash Protection");
                splashCheckBox.setSelected(config.magicSplashProtection());
                configPanel.add(splashCheckBox, gbc);
                components.put("splashProtection", splashCheckBox);
                break;

            case MINING:
                gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
                JCheckBox tickMiningCheckBox = new JCheckBox("3-Tick Mining");
                tickMiningCheckBox.setSelected(config.mining3Tick());
                configPanel.add(tickMiningCheckBox, gbc);
                components.put("3tick", tickMiningCheckBox);

                gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
                configPanel.add(new JLabel("Ore Types:"), gbc);
                JTextField oresField = new JTextField(config.miningOres());
                gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
                configPanel.add(oresField, gbc);
                components.put("ores", oresField);
                break;

            case FISHING:
                // Training mode (bank vs drop)
                gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
                configPanel.add(new JLabel("Training Mode:"), gbc);
                JComboBox<AllInOneConfig.GatheringMode> fishingModeCombo = new JComboBox<>(AllInOneConfig.GatheringMode.values());
                fishingModeCombo.setSelectedItem(config.fishingMode());
                gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
                configPanel.add(fishingModeCombo, gbc);
                components.put("fishingMode", fishingModeCombo);

                // Fishing method
                gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
                configPanel.add(new JLabel("Fishing Method:"), gbc);
                JComboBox<AllInOneConfig.FishingMethod> fishingMethodCombo = new JComboBox<>(AllInOneConfig.FishingMethod.values());
                fishingMethodCombo.setSelectedItem(config.fishingMethod());
                gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
                configPanel.add(fishingMethodCombo, gbc);
                components.put("fishingMethod", fishingMethodCombo);

                // Use special attack
                gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
                JCheckBox fishingSpecCheckBox = new JCheckBox("Use Harpoon Special");
                fishingSpecCheckBox.setSelected(config.fishingSpecial());
                configPanel.add(fishingSpecCheckBox, gbc);
                components.put("useSpec", fishingSpecCheckBox);
                break;

            case COOKING:
                gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
                JCheckBox gauntletsCheckBox = new JCheckBox("Use Cooking Gauntlets");
                gauntletsCheckBox.setSelected(config.cookingGauntlets());
                configPanel.add(gauntletsCheckBox, gbc);
                components.put("gauntlets", gauntletsCheckBox);
                break;

            case WOODCUTTING:
                gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
                JCheckBox nestsCheckBox = new JCheckBox("Collect Bird Nests");
                nestsCheckBox.setSelected(config.woodcuttingNests());
                configPanel.add(nestsCheckBox, gbc);
                components.put("nests", nestsCheckBox);

                gbc.gridy++;
                JCheckBox axeSpecCheckBox = new JCheckBox("Use Dragon Axe Special");
                axeSpecCheckBox.setSelected(config.woodcuttingSpecial());
                configPanel.add(axeSpecCheckBox, gbc);
                components.put("useSpec", axeSpecCheckBox);
                break;

            case AGILITY:
                gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
                JCheckBox staminaCheckBox = new JCheckBox("Use Stamina Potions");
                staminaCheckBox.setSelected(config.agilityStamina());
                configPanel.add(staminaCheckBox, gbc);
                components.put("stamina", staminaCheckBox);

                gbc.gridy++;
                JCheckBox marksCheckBox = new JCheckBox("Collect Marks of Grace");
                marksCheckBox.setSelected(config.agilityMarks());
                configPanel.add(marksCheckBox, gbc);
                components.put("marks", marksCheckBox);
                break;

            case THIEVING:
                gbc.gridx = 0; gbc.fill = GridBagConstraints.NONE; gbc.gridwidth = 1;
                configPanel.add(new JLabel("Food HP %:"), gbc);
                JSpinner thievingFoodSpinner = new JSpinner(new SpinnerNumberModel(40, 10, 90, 5));
                gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
                configPanel.add(thievingFoodSpinner, gbc);
                components.put("foodHP", thievingFoodSpinner);

                gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
                JCheckBox dodgyCheckBox = new JCheckBox("Use Dodgy Necklace");
                dodgyCheckBox.setSelected(config.thievingDodgy());
                configPanel.add(dodgyCheckBox, gbc);
                components.put("dodgy", dodgyCheckBox);
                break;

            case SLAYER:
                gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
                JCheckBox cannonCheckBox = new JCheckBox("Use Dwarf Cannon");
                cannonCheckBox.setSelected(config.slayerCannon());
                configPanel.add(cannonCheckBox, gbc);
                components.put("cannon", cannonCheckBox);
                break;

            case HUNTER:
                gbc.gridx = 0; gbc.fill = GridBagConstraints.NONE; gbc.gridwidth = 1;
                configPanel.add(new JLabel("Trap Count:"), gbc);
                JSpinner trapSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 5, 1));
                trapSpinner.setValue(config.hunterTraps());
                gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
                configPanel.add(trapSpinner, gbc);
                components.put("traps", trapSpinner);

                gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
                JCheckBox relayCheckBox = new JCheckBox("Auto Trap Replacement");
                relayCheckBox.setSelected(config.hunterRelay());
                configPanel.add(relayCheckBox, gbc);
                components.put("relay", relayCheckBox);
                break;

            case FARMING:
                gbc.gridx = 0; gbc.fill = GridBagConstraints.NONE; gbc.gridwidth = 1;
                configPanel.add(new JLabel("Compost Type:"), gbc);
                JComboBox<AllInOneConfig.CompostType> compostCombo = new JComboBox<>(AllInOneConfig.CompostType.values());
                compostCombo.setSelectedItem(config.farmingCompost());
                gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
                configPanel.add(compostCombo, gbc);
                components.put("compost", compostCombo);

                gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
                JCheckBox birdhousesCheckBox = new JCheckBox("Include Birdhouses");
                birdhousesCheckBox.setSelected(config.farmingBirdhouses());
                configPanel.add(birdhousesCheckBox, gbc);
                components.put("birdhouses", birdhousesCheckBox);
                break;

            case CONSTRUCTION:
                gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
                JCheckBox servantCheckBox = new JCheckBox("Use Servant");
                servantCheckBox.setSelected(config.constructionServant());
                configPanel.add(servantCheckBox, gbc);
                components.put("servant", servantCheckBox);
                break;

            case HERBLORE:
                gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
                JCheckBox secondariesCheckBox = new JCheckBox("Use Secondary Ingredients");
                secondariesCheckBox.setSelected(config.herbloreSecondaries());
                configPanel.add(secondariesCheckBox, gbc);
                components.put("secondaries", secondariesCheckBox);
                break;

            case RUNECRAFTING:
                gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
                JCheckBox pouchesCheckBox = new JCheckBox("Use Essence Pouches");
                pouchesCheckBox.setSelected(config.runecraftingPouches());
                configPanel.add(pouchesCheckBox, gbc);
                components.put("pouches", pouchesCheckBox);

                gbc.gridy++;
                JCheckBox repairCheckBox = new JCheckBox("Repair Pouches");
                repairCheckBox.setSelected(config.runecraftingRepair());
                configPanel.add(repairCheckBox, gbc);
                components.put("repair", repairCheckBox);
                break;
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

                case THIEVING:
                    if (components.containsKey("foodHP")) {
                        JSpinner spinner = (JSpinner) components.get("foodHP");
                        configManager.setConfiguration(configGroup, "thievingFoodHP", spinner.getValue().toString());
                    }
                    if (components.containsKey("dodgy")) {
                        JCheckBox checkBox = (JCheckBox) components.get("dodgy");
                        configManager.setConfiguration(configGroup, "thievingDodgy", String.valueOf(checkBox.isSelected()));
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
}

