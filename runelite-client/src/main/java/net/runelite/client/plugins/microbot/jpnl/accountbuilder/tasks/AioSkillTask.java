package net.runelite.client.plugins.microbot.jpnl.accountbuilder.tasks;

import lombok.Getter;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.enums.SkillType;

@Getter
public class AioSkillTask extends AioTask {

    private final SkillType skillType;
    private final int startLevel;
    private int targetLevel; // made mutable
    private boolean complete;

    private int startXp = -1;
    private int lastXp = -1;
    private int xpGained = 0;

    // New: optional time based mode
    private boolean timeMode; // if true ignore targetLevel, use durationMinutes
    private int durationMinutes; // 0 => unused

    public AioSkillTask(SkillType skillType, int startLevel, int targetLevel) {
        super(TaskType.SKILL);
        this.skillType = skillType;
        this.startLevel = startLevel;
        this.targetLevel = targetLevel;
    }

    public static AioSkillTask timeTask(SkillType skillType, int startLevel, int minutes) {
        AioSkillTask t = new AioSkillTask(skillType, startLevel, 0);
        t.timeMode = true;
        t.durationMinutes = Math.max(1, minutes);
        return t;
    }

    public boolean isTimeMode() { return timeMode; }
    public int getDurationMinutes() { return durationMinutes; }

    public void setTargetLevel(int newTarget) { this.targetLevel = newTarget; this.timeMode = false; }
    public void setTimeMode(int minutes) { this.timeMode = true; this.durationMinutes = Math.max(1, minutes); }

    @Override
    public boolean isComplete() { return complete; }
    public void markComplete() { complete = true; }

    public long elapsedMs() { return getStartTimestamp() > 0 ? System.currentTimeMillis() - getStartTimestamp() : 0; }

    public boolean checkTimeComplete() {
        if (timeMode && durationMinutes > 0) {
            if (elapsedMs() >= durationMinutes * 60_000L) {
                markComplete();
                return true;
            }
        }
        return false;
    }

    @Override
    public String getDisplay() {
        if (timeMode) {
            return "Skill: " + skillType.name() + " (" + durationMinutes + "m)";
        }
        return "Skill: " + skillType.name() + " -> " + (targetLevel == 0 ? "(cfg)" : targetLevel);
    }

    @Override
    public SkillType getSkillTypeOrNull() { return skillType; }

    public Skill toRuneLiteSkill() {
        switch (skillType) {
            case ATTACK: return Skill.ATTACK;
            case STRENGTH: return Skill.STRENGTH;
            case DEFENCE: return Skill.DEFENCE;
            case RANGED: return Skill.RANGED;
            case PRAYER: return Skill.PRAYER;
            case MAGIC: return Skill.MAGIC;
            case RUNECRAFTING: return Skill.RUNECRAFT;
            case CONSTRUCTION: return Skill.CONSTRUCTION;
            case HITPOINTS: return Skill.HITPOINTS;
            case AGILITY: return Skill.AGILITY;
            case HERBLORE: return Skill.HERBLORE;
            case THIEVING: return Skill.THIEVING;
            case CRAFTING: return Skill.CRAFTING;
            case FLETCHING: return Skill.FLETCHING;
            case SLAYER: return Skill.SLAYER;
            case HUNTER: return Skill.HUNTER;
            case MINING: return Skill.MINING;
            case SMITHING: return Skill.SMITHING;
            case FISHING: return Skill.FISHING;
            case COOKING: return Skill.COOKING;
            case FIREMAKING: return Skill.FIREMAKING;
            case WOODCUTTING: return Skill.WOODCUTTING;
            case FARMING: return Skill.FARMING;
        }
        return Skill.ATTACK;
    }

    public int currentLevel() {
        if (Microbot.getClient() == null) return 0;
        return Microbot.getClient().getRealSkillLevel(toRuneLiteSkill());
    }

    public int currentXp() {
        if (Microbot.getClient() == null) return 0;
        return Microbot.getClient().getSkillExperience(toRuneLiteSkill());
    }

    public void updateXpTracking() {
        int cur = currentXp();
        if (startXp < 0) {
            startXp = cur;
            lastXp = cur;
            xpGained = 0;
            return;
        }
        if (cur != lastXp) {
            xpGained = cur - startXp;
            lastXp = cur;
        }
    }

    public int getXpGained() {
        return xpGained;
    }
}