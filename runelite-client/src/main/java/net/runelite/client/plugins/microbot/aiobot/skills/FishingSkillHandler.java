package net.runelite.client.plugins.microbot.aiobot.skills;

import lombok.Getter;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.aiobot.settings.SkillRuntimeSettings;
import net.runelite.client.plugins.microbot.aiobot.enums.SkillType;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.*;
import java.util.stream.Collectors;

import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

public class FishingSkillHandler implements SkillHandler {

    private FishingMethod currentMethod;
    private boolean initialized;
    private int plannedTarget = 99;
    private Mode resolvedMode = Mode.POWERFISH;
    private Set<String> customDrops = Set.of();
    private boolean useSpec;

    private enum Mode { POWERFISH, BANK, COOK_DROP }

    @Getter
    private enum FishingMethod {
        SHRIMP(1, "Net", List.of("Fishing spot"), List.of("Small fishing net"),
                new WorldPoint(3239, 3144, 0)),
        TROUT_SALMON(20, "Lure", List.of("Fishing spot"), List.of("Feather","Fly fishing rod"),
                new WorldPoint(3104, 3424, 0)),
        TUNA_SWORD(35, "Harpoon", List.of("Fishing spot"), List.of("Harpoon"),
                new WorldPoint(2926, 3181, 0)),
        LOBSTER(40, "Cage", List.of("Fishing spot"), List.of("Lobster pot"),
                new WorldPoint(2926, 3181, 0)),
        MONKFISH(62, "Net", List.of("Fishing spot"), List.of("Small fishing net"),
                new WorldPoint(2339, 3706, 0)),
        SHARK(76, "Harpoon", List.of("Fishing spot"), List.of("Harpoon"),
                new WorldPoint(2926, 3181, 0));

        private final int minLevel;
        private final String action;
        private final List<String> npcNames;
        private final List<String> requiredItems;
        private final WorldPoint defaultTile;

        FishingMethod(int minLevel, String action, List<String> npcNames,
                      List<String> requiredItems, WorldPoint defaultTile) {
            this.minLevel = minLevel;
            this.action = action;
            this.npcNames = npcNames;
            this.requiredItems = requiredItems;
            this.defaultTile = defaultTile;
        }
    }

    private static final Set<String> DEFAULT_LOW_TIER_DROP = Set.of(
            "Raw shrimps","Raw anchovies","Raw sardine","Raw herring","Raw trout","Raw salmon"
    );

    public void applySettings(SkillRuntimeSettings settings) {
        if (settings == null || !settings.isEnabled()) {
            // Mark as impossible / idle
            initialized = false;
            return;
        }
        this.plannedTarget = settings.getTargetLevel();
        this.resolvedMode = mapMode(settings.getMode());
        this.useSpec = settings.isUseSpecial();
        this.customDrops = settings.getCustomList().isEmpty() ? DEFAULT_LOW_TIER_DROP : settings.getCustomList();
    }

    private Mode mapMode(String raw) {
        if (raw == null) return Mode.POWERFISH;
        try {
            return Mode.valueOf(raw);
        } catch (Exception e) {
            return Mode.POWERFISH;
        }
    }

    private void initializeIfNeeded() {
        if (initialized) return;
        int current = Microbot.getClient().getRealSkillLevel(Skill.FISHING);
        currentMethod = Arrays.stream(FishingMethod.values())
                .filter(m -> m.minLevel <= current)
                .max(Comparator.comparingInt(m -> m.minLevel))
                .orElse(FishingMethod.SHRIMP);
        initialized = true;
    }

    @Override
    public void execute() {
        if (Rs2AntibanSettings.actionCooldownActive) return;

        initializeIfNeeded();
        if (!initialized) {
            Microbot.status = "Fishing disabled in config";
            return;
        }

        int level = Microbot.getClient().getRealSkillLevel(Skill.FISHING);

        // Switch method if new unlocked & below target
        FishingMethod best = Arrays.stream(FishingMethod.values())
                .filter(m -> m.minLevel <= level && m.minLevel <= plannedTarget)
                .max(Comparator.comparingInt(m -> m.minLevel))
                .orElse(currentMethod);
        if (best != currentMethod) {
            currentMethod = best;
            Microbot.log("Switched fishing method -> " + currentMethod.name());
        }

        Microbot.status = "Fishing " + currentMethod.name() + " (" + level + "/" + plannedTarget + ") Mode=" + resolvedMode;

        if (level >= plannedTarget) {
            Microbot.status = "Fishing target bereikt (" + level + ")";
            return; // Script bovenliggend markeert taak complete
        }

        // Full inventory:
        if (Rs2Inventory.isFull()) {
            handleFullInventory();
            return;
        }

        // Check gear
        if (!hasRequiredItems(currentMethod)) {
            Microbot.status = "Missing fishing gear for " + currentMethod.name();
            return;
        }

        // Interact
        if (!Rs2Player.isAnimating() && !Rs2Player.isInteracting()) {
            if (Rs2Player.getWorldLocation().distanceTo(currentMethod.defaultTile) > 12) {
                Rs2Walker.walkTo(currentMethod.defaultTile, 2);
                return;
            }
            var npc = Rs2Npc.getNpcs(currentMethod.npcNames.get(0), true)
                    .filter(n -> n != null && !n.isDead())
                    .findFirst().orElse(null);
            if (npc == null) {
                Microbot.status = "Geen fishing spot...";
                return;
            }

            if (useSpec && currentMethod.action.equals("Harpoon") && Rs2Combat.getSpecEnergy() >= 1000) {
                Rs2Combat.setSpecState(true, 100);
            }

            boolean interacted = Rs2Npc.interact(npc, currentMethod.action);
            if (interacted) {
                sleepUntil(Rs2Player::isAnimating, 4000);
            }
        }
    }

    private void handleFullInventory() {
        switch (resolvedMode) {
            case POWERFISH:
            case COOK_DROP: // COOK_DROP nog niet geactiveerd -> valt terug op droppen
                dropCustom();
                break;
            case BANK:
                // TODO: implement bank-loop (detect closest bank chest/npc)
                Microbot.status = "Inventory full -> BANK mode placeholder";
                break;
        }
    }

    private void dropCustom() {
        boolean didDrop = false;
        for (String item : customDrops) {
            while (Rs2Inventory.hasItem(item)) {
                Rs2Inventory.drop(item);
                didDrop = true;
            }
        }
        if (!didDrop) {
            Microbot.status = "Full inventory (no droppables) - consider BANK mode";
        }
    }

    private boolean hasRequiredItems(FishingMethod method) {
        for (String req : method.requiredItems) {
            if (!Rs2Inventory.hasItem(req)) return false;
        }
        return true;
    }
}