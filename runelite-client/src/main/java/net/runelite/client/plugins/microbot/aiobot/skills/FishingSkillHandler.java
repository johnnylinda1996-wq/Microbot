package net.runelite.client.plugins.microbot.aiobot.skills;

import net.runelite.api.NPCComposition;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.aiobot.settings.SkillRuntimeSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FishingSkillHandler implements SkillHandler {

    private boolean enabled = true;
    private Mode mode = Mode.DROP;
    private FishingTier tier = FishingTier.NET_BAIT;

    private long lastAttempt = 0L;
    private static final long ATTEMPT_COOLDOWN = 900L;

    private static final int DRAYNOR_MIN_COMBAT = 25;

    private static final WorldPoint LUMBRIDGE_NET = new WorldPoint(3242, 3154, 0);
    private static final WorldPoint DRAYNOR_NET = new WorldPoint(3086, 3232, 0);

    private static final BankLocation LUMBRIDGE_BANK = BankLocation.LUMBRIDGE_FRONT;
    private static final BankLocation DRAYNOR_BANK = BankLocation.DRAYNOR_VILLAGE;

    // TODO: vervang door juiste spot IDs uit je nate fishing script / config
    private static final int[] NET_BAIT_SPOTS = { 1530, 1528, 1526, 1527 };
    private static final int[] LURE_SPOTS     = { 1526, 1527, 1528 };
    private static final int[] CAGE_SPOTS     = { 1510, 1511, 1516 };

    private enum Mode { DROP, BANK }
    private enum FishingTier { NET_BAIT, LURE, CAGE_HARPOON }

    @Override
    public void applySettings(SkillRuntimeSettings settings) {
        if (settings != null) {
            enabled = settings.isEnabled();
            if (settings.getMode() != null) {
                String m = settings.getMode().toLowerCase(Locale.ROOT);
                if (m.contains("bank")) {
                    mode = Mode.BANK;
                } else {
                    mode = Mode.DROP;
                }
            }
        }
    }

    @Override
    public void execute() {
        if (!enabled) {
            Microbot.status = "Fishing: disabled";
            return;
        }
        if (!Microbot.isLoggedIn()) return;

        updateTier();

        if (!hasRequiredTools()) {
            Microbot.status = "Fishing: missing tools -> bank";
            obtainTools();
            return;
        }

        if (Rs2Inventory.isFull()) {
            if (mode == Mode.BANK) {
                bankFish();
            } else {
                dropFish();
            }
            return;
        }

        WorldPoint target = chooseLocation();
        if (target != null && Rs2Player.getWorldLocation().distanceTo(target) > 7) {
            if (Rs2Walker.walkTo(target, 4)) {
                Microbot.status = "Fishing: walking to spot";
            }
            return;
        }

        attemptFish();
    }

    private void updateTier() {
        int lvl = Microbot.getClient().getRealSkillLevel(Skill.FISHING);
        if (lvl >= 40) {
            tier = FishingTier.CAGE_HARPOON;
        } else if (lvl >= 20) {
            tier = FishingTier.LURE;
        } else {
            tier = FishingTier.NET_BAIT;
        }
    }

    private boolean hasRequiredTools() {
        switch (tier) {
            case NET_BAIT:
                return Rs2Inventory.contains("Small fishing net")
                        || (Rs2Inventory.contains("Fishing rod") && Rs2Inventory.contains("Fishing bait"));
            case LURE:
                return Rs2Inventory.contains("Fly fishing rod") && Rs2Inventory.contains("Feather");
            case CAGE_HARPOON:
                return Rs2Inventory.contains("Lobster pot") || Rs2Inventory.contains("Harpoon");
            default:
                return false;
        }
    }

    private void obtainTools() {
        if (!openNearestBank()) return;

        switch (tier) {
            case NET_BAIT:
                if (!Rs2Inventory.contains("Small fishing net") && Rs2Bank.hasItem("Small fishing net"))
                    Rs2Bank.withdrawOne("Small fishing net");
                if (!Rs2Inventory.contains("Fishing rod") && Rs2Bank.hasItem("Fishing rod"))
                    Rs2Bank.withdrawOne("Fishing rod");
                if (!Rs2Inventory.contains("Fishing bait") && Rs2Bank.hasItem("Fishing bait"))
                    Rs2Bank.withdrawX("Fishing bait", 300);
                break;
            case LURE:
                if (!Rs2Inventory.contains("Fly fishing rod") && Rs2Bank.hasItem("Fly fishing rod"))
                    Rs2Bank.withdrawOne("Fly fishing rod");
                if (!Rs2Inventory.contains("Feather") && Rs2Bank.hasItem("Feather"))
                    Rs2Bank.withdrawX("Feather", 400);
                break;
            case CAGE_HARPOON:
                if (!Rs2Inventory.contains("Lobster pot") && Rs2Bank.hasItem("Lobster pot"))
                    Rs2Bank.withdrawOne("Lobster pot");
                if (!Rs2Inventory.contains("Harpoon") && Rs2Bank.hasItem("Harpoon"))
                    Rs2Bank.withdrawOne("Harpoon");
                break;
            default:
                break;
        }
        Rs2Bank.closeBank();
    }

    private WorldPoint chooseLocation() {
        int combat = Microbot.getClient().getLocalPlayer().getCombatLevel();
        if (combat < DRAYNOR_MIN_COMBAT) {
            return LUMBRIDGE_NET;
        }
        return DRAYNOR_NET;
    }

    private int[] currentSpotIds() {
        switch (tier) {
            case NET_BAIT: return NET_BAIT_SPOTS;
            case LURE: return LURE_SPOTS;
            case CAGE_HARPOON: return CAGE_SPOTS;
            default: return NET_BAIT_SPOTS;
        }
    }

    private List<String> desiredActions() {
        switch (tier) {
            case NET_BAIT: return Arrays.asList("Net", "Bait");
            case LURE: return Collections.singletonList("Lure");
            case CAGE_HARPOON: return Arrays.asList("Cage", "Harpoon");
            default: return Collections.emptyList();
        }
    }

    private void attemptFish() {
        long now = System.currentTimeMillis();
        if (now - lastAttempt < ATTEMPT_COOLDOWN) return;
        if (Rs2Player.isAnimating() || Rs2Player.isMoving()) return;

        int[] ids = currentSpotIds();
        List<String> wanted = desiredActions();

        for (int id : ids) {
            Rs2NpcModel npc = Rs2Npc.getNpc(id); // Verwacht Rs2NpcModel in jouw codebase
            if (npc == null) continue;

            Set<String> npcActions = getAllActions(npc);
            if (npcActions.isEmpty()) continue;

            for (String w : wanted) {
                if (containsActionIgnoreCase(npcActions, w)) {
                    boolean ok = Rs2Npc.interact(npc, w); // niet-deprecated
                    if (ok) {
                        Microbot.status = "Fishing: " + tier.name() + " (" + w + ")";
                        lastAttempt = now;
                        return;
                    }
                }
            }
        }

        Microbot.status = "Fishing: no spot (retry)";
        lastAttempt = now;
    }

    private Set<String> getAllActions(Rs2NpcModel npc) {
        Set<String> set = new LinkedHashSet<String>();
        try {
            NPCComposition trans = npc.getTransformedComposition();
            NPCComposition base = npc.getComposition();
            if (trans != null && trans.getActions() != null) {
                String[] ta = trans.getActions();
                for (String a : ta) {
                    if (a != null && !a.isEmpty()) set.add(a);
                }
            }
            if (base != null && base.getActions() != null) {
                String[] ba = base.getActions();
                for (String a : ba) {
                    if (a != null && !a.isEmpty()) set.add(a);
                }
            }
        } catch (Exception ignored) {}
        return set;
    }

    private boolean containsActionIgnoreCase(Set<String> actions, String desired) {
        for (String a : actions) {
            if (a.equalsIgnoreCase(desired)) return true;
        }
        return false;
    }

    private void bankFish() {
        if (!openNearestBank()) return;
        Set<String> keep = new HashSet<String>();
        switch (tier) {
            case NET_BAIT:
                if (Rs2Inventory.contains("Small fishing net")) keep.add("Small fishing net");
                if (Rs2Inventory.contains("Fishing rod")) keep.add("Fishing rod");
                if (Rs2Inventory.contains("Fishing bait")) keep.add("Fishing bait");
                break;
            case LURE:
                keep.add("Fly fishing rod");
                keep.add("Feather");
                break;
            case CAGE_HARPOON:
                if (Rs2Inventory.contains("Lobster pot")) keep.add("Lobster pot");
                if (Rs2Inventory.contains("Harpoon")) keep.add("Harpoon");
                break;
            default:
                break;
        }
        Rs2Bank.depositAllExcept(keep.toArray(new String[0]));
        Rs2Bank.closeBank();
        Microbot.status = "Fishing: banked";
    }

    private void dropFish() {
        final AtomicInteger dropped = new AtomicInteger(0);
        Rs2Inventory.dropAll(model -> {
            String n = model.getName();
            if (n == null) return false;
            String ln = n.toLowerCase(Locale.ROOT);
            if (ln.contains("net") || ln.contains("rod") || ln.contains("bait")
                    || ln.contains("feather") || ln.contains("harpoon") || ln.contains("pot"))
                return false;
            if (isFish(ln)) {
                dropped.incrementAndGet();
                return true;
            }
            return false;
        });
        Microbot.status = "Fishing: dropped " + dropped.get();
    }

    private boolean isFish(String ln) {
        return ln.contains("shrimp") || ln.contains("anchovy") || ln.contains("sardine")
                || ln.contains("herring") || ln.contains("trout") || ln.contains("salmon")
                || ln.contains("tuna") || ln.contains("lobster") || ln.contains("bass")
                || ln.contains("swordfish") || ln.contains("shark") || ln.contains("monkfish")
                || ln.contains("angler") || ln.contains("karambwan") || ln.contains("manta")
                || ln.contains("sea turtle") || ln.contains("pike") || ln.contains("eel")
                || ln.contains("cavefish") || ln.contains("rocktail") || ln.contains("cod");
    }

    private boolean openNearestBank() {
        int combat = Microbot.getClient().getLocalPlayer().getCombatLevel();
        BankLocation bank = (combat < DRAYNOR_MIN_COMBAT) ? LUMBRIDGE_BANK : DRAYNOR_BANK;
        boolean ok = Rs2Bank.walkToBankAndUseBank(bank) && Rs2Bank.isOpen();
        if (!ok) Microbot.status = "Fishing: opening bank...";
        return ok;
    }

    // Debug getters
    public boolean isEnabled() { return enabled; }
    public String getMode() { return mode.name(); }
    public String getTierName() { return tier.name(); }
}