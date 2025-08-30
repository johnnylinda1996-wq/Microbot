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
    private JButton clearButton;

    private JLabel currentTaskLabel;
    private JLabel statusLabel;

    public AllInOneBotGUI(AllInOneScript script) {
        super("AIO Bot Manager");
        this.script = script;
        initComponents();
        layoutComponents();
        attachListeners();
        refreshQueue();
        setMinimumSize(new Dimension(720, 520));
        setLocationRelativeTo(null);
        setVisible(true);
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
        clearButton = new JButton("Clear Queue");

        currentTaskLabel = new JLabel("Current: none");
        statusLabel = new JLabel("Status: " + (Microbot.status == null ? "" : Microbot.status));
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
        JPanel p = new JPanel(new BorderLayout(5,5));
        p.setBorder(new TitledBorder("Task Queue"));
        JScrollPane scroll = new JScrollPane(queueList);
        p.add(scroll, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(removeSelectedButton);
        buttons.add(clearButton);
        buttons.add(startButton);
        p.add(buttons, BorderLayout.SOUTH);

        return p;
    }

    private JPanel buildStatusPanel() {
        JPanel p = new JPanel(new GridLayout(0,1));
        p.setBorder(new TitledBorder("Runtime"));
        p.add(currentTaskLabel);
        p.add(statusLabel);
        return p;
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(10,10));
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(buildSkillPanel());
        left.add(Box.createVerticalStrut(10));
        left.add(buildQuestPanel());
        add(left, BorderLayout.WEST);

        add(buildQueuePanel(), BorderLayout.CENTER);
        add(buildStatusPanel(), BorderLayout.SOUTH);
        pack();
    }

    private void attachListeners() {
        addSkillButton.addActionListener(e -> {
            SkillType skill = (SkillType) skillCombo.getSelectedItem();
            int target = (int) targetLevelSpinner.getValue();
            if (skill == null) return;
            int current = 1;
            try {
                current = Microbot.getClient().getRealSkillLevel(skillToApi(skill));
            } catch (Exception ignored) {}
            script.addTask(new AioSkillTask(skill, current, target));
            refreshQueue();
        });

        addQuestButton.addActionListener(e -> {
            QuestType quest = (QuestType) questCombo.getSelectedItem();
            if (quest == null) return;
            script.addTask(new AioQuestTask(quest));
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
            while (!script.getSnapshotQueue().isEmpty()) {
                script.removeTask(0);
            }
            refreshQueue();
        });

        startButton.addActionListener(e -> {
            if (!script.isRunning()) {
                script.startLoop();
            }
        });

        new Timer(1000, e -> refreshRuntime()).start();
    }

    private void refreshQueue() {
        queueModel.clear();
        script.getSnapshotQueue().forEach(t -> queueModel.addElement(t.getDisplay()));
    }

    private void refreshRuntime() {
        if (script.getCurrentTask() != null) {
            currentTaskLabel.setText("Current: " + script.getCurrentTask().getDisplay());
        } else {
            currentTaskLabel.setText("Current: none");
        }
        statusLabel.setText("Status: " + (Microbot.status == null ? "" : Microbot.status));
    }

    private net.runelite.api.Skill skillToApi(SkillType st) {
        switch (st) {
            case ATTACK: return net.runelite.api.Skill.ATTACK;
            case STRENGTH: return net.runelite.api.Skill.STRENGTH;
            case DEFENCE: return net.runelite.api.Skill.DEFENCE;
            case RANGED: return net.runelite.api.Skill.RANGED;
            case PRAYER: return net.runelite.api.Skill.PRAYER;
            case MAGIC: return net.runelite.api.Skill.MAGIC;
            case RUNECRAFTING: return net.runelite.api.Skill.RUNECRAFT;
            case CONSTRUCTION: return net.runelite.api.Skill.CONSTRUCTION;
            case HITPOINTS: return net.runelite.api.Skill.HITPOINTS;
            case AGILITY: return net.runelite.api.Skill.AGILITY;
            case HERBLORE: return net.runelite.api.Skill.HERBLORE;
            case THIEVING: return net.runelite.api.Skill.THIEVING;
            case CRAFTING: return net.runelite.api.Skill.CRAFTING;
            case FLETCHING: return net.runelite.api.Skill.FLETCHING;
            case SLAYER: return net.runelite.api.Skill.SLAYER;
            case HUNTER: return net.runelite.api.Skill.HUNTER;
            case MINING: return net.runelite.api.Skill.MINING;
            case SMITHING: return net.runelite.api.Skill.SMITHING;
            case FISHING: return net.runelite.api.Skill.FISHING;
            case COOKING: return net.runelite.api.Skill.COOKING;
            case FIREMAKING: return net.runelite.api.Skill.FIREMAKING;
            case WOODCUTTING: return net.runelite.api.Skill.WOODCUTTING;
            case FARMING: return net.runelite.api.Skill.FARMING;
        }
        return net.runelite.api.Skill.ATTACK;
    }

    // FIX: toegevoegd helper methode baseGbc()
    private GridBagConstraints baseGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4,4,4,4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        return gbc;
    }
}