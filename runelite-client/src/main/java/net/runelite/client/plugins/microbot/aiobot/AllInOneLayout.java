package net.runelite.client.plugins.microbot.aiobot;

import net.runelite.client.plugins.microbot.aiobot.enums.SkillType;
import net.runelite.client.plugins.microbot.aiobot.enums.QuestType;

import javax.swing.*;
import java.awt.*;

public class AllInOneLayout extends JPanel {
    public AllInOneLayout() {
        setLayout(new BorderLayout());
        JLabel title = new JLabel("All-in-One OSRS Bot", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        add(title, BorderLayout.NORTH);

        JPanel mainPanel = new JPanel(new GridLayout(2, 1));

        // Skills panel
        JPanel skillPanel = new JPanel(new GridLayout(0, 5, 5, 5));
        skillPanel.setBorder(BorderFactory.createTitledBorder("Skills"));
        for (SkillType skill : SkillType.values()) {
            JButton btn = new JButton(skill.getDisplayName());
            btn.setName(skill.name());
            skillPanel.add(btn);
        }

        // Quests panel
        JPanel questPanel = new JPanel(new GridLayout(0, 3, 5, 5));
        questPanel.setBorder(BorderFactory.createTitledBorder("Quests"));
        for (QuestType quest : QuestType.values()) {
            JButton btn = new JButton(quest.getDisplayName());
            btn.setName(quest.name());
            questPanel.add(btn);
        }

        mainPanel.add(skillPanel);
        mainPanel.add(questPanel);
        add(mainPanel, BorderLayout.CENTER);
    }
}