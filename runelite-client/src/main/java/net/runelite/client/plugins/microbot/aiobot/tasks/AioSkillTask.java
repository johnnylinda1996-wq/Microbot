package net.runelite.client.plugins.microbot.aiobot.tasks;

import lombok.Getter;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.aiobot.enums.SkillType;

public class AioSkillTask implements AioTask {
    @Getter
    private final SkillType skillType;
    @Getter
    private final int targetLevel;
    @Getter
    private final int startLevel; // captured at creation (optional usage)
    private boolean complete;

    public AioSkillTask(SkillType skillType, int startLevel, int targetLevel) {
        this.skillType = skillType;
        this.startLevel = startLevel;
        this.targetLevel = targetLevel;
    }

    @Override
    public String getDisplay() {
        return "Skill: " + skillType.getDisplayName() + " -> " + targetLevel;
    }

    @Override
    public boolean isComplete() {
        return complete;
    }

    @Override
    public void markComplete() {
        complete = true;
    }

    @Override
    public TaskType getType() {
        return TaskType.SKILL;
    }

    public Skill toRuneLiteSkill() {
        // Mapping by name similarity
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
        return null;
    }
}