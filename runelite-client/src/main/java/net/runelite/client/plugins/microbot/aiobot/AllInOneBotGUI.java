package net.runelite.client.plugins.microbot.aiobot;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.aiobot.enums.QuestType;
import net.runelite.client.plugins.microbot.aiobot.enums.SkillType;
import net.runelite.client.plugins.microbot.aiobot.tasks.AioQuestTask;
import net.runelite.client.plugins.microbot.aiobot.tasks.AioSkillTask;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Arrays;
import java.util.Comparator;

public class AllInOneBotGUI extends JFrame {

    private final AllInOneScript script;

    private JComboBox<SkillType> skillCombo;
    private JSpinner targetLevelSpinner;
    private JButton addSkillButton;

    private JComboBox<QuestType> questCombo;
    private JButton addQuestButton;

    private DefaultListModel<String> queueModel;
    private JList<String> queueList;
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

    private final Timer uiTimer;

    public AllInOneBotGUI(AllInOneScript script) {
        super("AIO Bot Manager");
        this.script = script;
        initComponents();
        layoutComponents();
        attachListeners();
        refreshQueue();
        setMinimumSize(new Dimension(820, 580));
        setLocationRelativeTo(null);
        setVisible(true);

        uiTimer = new Timer(1000, e -> refreshStatus());
        uiTimer.start();
    }

    private void initComponents() {
        skillCombo = new JComboBox<>(Arrays.stream(SkillType.values())
                .sorted(Comparator.comparing(SkillType::name))
                .toArray(SkillType[]::new));
        targetLevelSpinner = new JSpinner(new SpinnerNumberModel(10, 2, 120, 1));
        addSkillButton = new JButton("Add Skill Task");

        questCombo = new JComboBox<>(Arrays.stream(QuestType.values())
                .sorted(Comparator.comparing(QuestType::name))
                .toArray(QuestType[]::new));
        addQuestButton = new JButton("Add Quest Task");

        queueModel = new DefaultListModel<>();
        queueList = new JList<>(queueModel);
        queueList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        removeSelectedButton = new JButton("Remove Selected");
        startButton = new JButton("Start / Resume");
        pauseButton = new JButton("Pause");
        clearButton = new JButton("Clear Queue");
        refreshButton = new JButton("Refresh");

        currentTaskLabel = new JLabel("Current: none");
        statusLabel = new JLabel("Status: " + (Microbot.status == null ? "" : Microbot.status));
        levelLabel = new JLabel("Level: -");
        xpGainedLabel = new JLabel("XP Gained: 0");
        xpPerHourLabel = new JLabel("XP/h: 0");
        xpRemainingLabel = new JLabel("Remaining: -");
        elapsedLabel = new JLabel("Elapsed: 0s");
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
    }

    private JPanel buildSkillPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new TitledBorder("Skill Task"));
        GridBagConstraints gbc = baseGbc();

        p.add(new JLabel("Skill:"), gbc);
        gbc.gridx = 1;
        p.add(skillCombo, gbc);

        gbc.gridx = 0; gbc.gridy++;
        p.add(new JLabel("Target level:"), gbc);
        gbc.gridx = 1;
        p.add(targetLevelSpinner, gbc);

        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2;
        p.add(addSkillButton, gbc);

        return p;
    }

    private JPanel buildQuestPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new TitledBorder("Quest Task"));
        GridBagConstraints gbc = baseGbc();

        p.add(new JLabel("Quest:"), gbc);
        gbc.gridx = 1;
        p.add(questCombo, gbc);

        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2;
        p.add(addQuestButton, gbc);

        return p;
    }

    private JPanel buildQueuePanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new TitledBorder("Queue"));
        p.add(new JScrollPane(queueList), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(removeSelectedButton);
        buttons.add(clearButton);
        buttons.add(refreshButton);
        p.add(buttons, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildControlPanel() {
        JPanel p = new JPanel(new GridLayout(0,1,3,3));
        p.setBorder(new TitledBorder("Control"));

        p.add(startButton);
        p.add(pauseButton);

        return p;
    }

    private JPanel buildStatusPanel() {
        JPanel p = new JPanel(new GridLayout(0,1,2,2));
        p.setBorder(new TitledBorder("Status"));
        p.add(currentTaskLabel);
        p.add(statusLabel);
        p.add(levelLabel);
        p.add(xpGainedLabel);
        p.add(xpPerHourLabel);
        p.add(xpRemainingLabel);
        p.add(elapsedLabel);
        p.add(progressBar);
        return p;
    }

    private void layoutComponents() {
        JPanel left = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = baseGbc();

        left.add(buildSkillPanel(), gbc);
        gbc.gridy++;
        left.add(buildQuestPanel(), gbc);
        gbc.gridy++;
        left.add(buildControlPanel(), gbc);
        gbc.gridy++;
        left.add(buildStatusPanel(), gbc);

        JPanel right = new JPanel(new BorderLayout(5,5));
        right.add(buildQueuePanel(), BorderLayout.CENTER);

        JPanel root = new JPanel(new BorderLayout(8,8));
        root.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        root.add(left, BorderLayout.WEST);
        root.add(right, BorderLayout.CENTER);

        setContentPane(root);
    }

    private void attachListeners() {
        addSkillButton.addActionListener(e -> {
            SkillType st = (SkillType) skillCombo.getSelectedItem();
            int target = (int) targetLevelSpinner.getValue();
            script.addSkillTask(st, target);
            refreshQueue();
        });

        addQuestButton.addActionListener(e -> {
            QuestType qt = (QuestType) questCombo.getSelectedItem();
            script.addQuestTask(qt);
            refreshQueue();
        });

        removeSelectedButton.addActionListener(e -> {
            int idx = queueList.getSelectedIndex();
            if (idx >= 0) {
                script.removeTask(idx);
                refreshQueue();
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
    }

    private void refreshQueue() {
        queueModel.clear();
        for (String s : script.getQueueDisplay()) {
            queueModel.addElement(s);
        }
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

        updatePauseLabel();
    }

    private void updatePauseLabel() {
        pauseButton.setText(script.isPaused() ? "Resume" : "Pause");
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
}