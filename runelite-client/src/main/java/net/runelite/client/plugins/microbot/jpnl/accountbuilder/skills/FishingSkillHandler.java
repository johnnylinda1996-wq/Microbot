package net.runelite.client.plugins.microbot.jpnl.accountbuilder.skills;

import net.runelite.api.NPCComposition;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.settings.SkillRuntimeSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FishingSkillHandler implements SkillHandler {
    private boolean enabled = true;
    private Mode mode = Mode.DROP;
    private FishingTier tier = FishingTier.NET_BAIT;
    private String configuredFishingMethod = "AUTO"; // New field to store the configured fishing method

    private long lastAttempt = 0L;
    private static final long ATTEMPT_COOLDOWN = 900L;

    private static final int DRAYNOR_MIN_COMBAT = 25;

    private static final WorldPoint LUMBRIDGE_NET = new WorldPoint(3242, 3154, 0);
    private static final WorldPoint DRAYNOR_NET = new WorldPoint(3086, 3232, 0);

    private static final BankLocation LUMBRIDGE_BANK = BankLocation.LUMBRIDGE_FRONT;
    private static final BankLocation DRAYNOR_BANK = BankLocation.DRAYNOR_VILLAGE;

    // Enums
    private enum Mode { DROP, BANK }
    private enum FishingTier { NET_BAIT, LURE, CAGE_HARPOON }

    // Fishing spot data structure
    private static class FishingSpot {
        String name;
        int minLevel;
        List<String> fishTypes;
        List<Integer> spotIds;
        WorldPoint location;
        BankLocation bankLocation;
        List<String> actions;

        public FishingSpot(String name, int minLevel, List<String> fishTypes, List<Integer> spotIds,
                           WorldPoint location, BankLocation bankLocation, List<String> actions) {
            this.name = name;
            this.minLevel = minLevel;
            this.fishTypes = fishTypes;
            this.spotIds = spotIds;
            this.location = location;
            this.bankLocation = bankLocation;
            this.actions = actions;
        }
    }

    private static final List<FishingSpot> FISHING_SPOTS = Arrays.asList(
            new FishingSpot("Lumbridge", 1, Arrays.asList("shrimp", "anchovy"), Arrays.asList(1526, 1527, 1530, 1528), new WorldPoint(3242, 3154, 0), BankLocation.LUMBRIDGE_TOP, Arrays.asList("Net", "Bait")),
            new FishingSpot("Draynor", 1, Arrays.asList("shrimp", "anchovy"), Arrays.asList(1526, 1527, 1530, 1528), new WorldPoint(3086, 3232, 0), BankLocation.DRAYNOR_VILLAGE, Arrays.asList("Net", "Bait")),
            new FishingSpot("Catherby", 1, Arrays.asList("shrimp", "anchovy", "lobster", "tuna", "swordfish", "shark"), Arrays.asList(1526, 1527, 1510, 1511, 1516, 1518, 1520), new WorldPoint(2853, 3431, 0), BankLocation.CATHERBY, Arrays.asList("Net", "Bait", "Cage", "Harpoon")),
            new FishingSpot("Barbarian Village", 20, Arrays.asList("trout", "salmon"), Arrays.asList(1526, 1527, 1528), new WorldPoint(3104, 3433, 0), BankLocation.EDGEVILLE, Arrays.asList("Lure")),
            new FishingSpot("Shilo Village", 30, Arrays.asList("trout", "salmon"), Arrays.asList(1526, 1527, 1528), new WorldPoint(2854, 2952, 0), BankLocation.SHILO_VILLAGE, Arrays.asList("Lure")),
            new FishingSpot("Fishing Guild", 40, Arrays.asList("lobster", "tuna", "swordfish", "shark"), Arrays.asList(1510, 1511, 1516, 1518, 1520), new WorldPoint(2599, 3421, 0), BankLocation.FISHING_GUILD, Arrays.asList("Cage", "Harpoon")),
            new FishingSpot("Piscatoris", 62, Arrays.asList("monkfish"), Arrays.asList(317), new WorldPoint(2334, 3647, 0), BankLocation.PISCATORIS_FISHING_COLONY, Arrays.asList("Net")),
            new FishingSpot("Tai Bwo Wannai", 65, Arrays.asList("karambwan"), Arrays.asList(4712), new WorldPoint(2780, 3178, 0), BankLocation.SHILO_VILLAGE, Arrays.asList("Karambwanji")),
            new FishingSpot("Port Piscarilius", 82, Arrays.asList("anglerfish"), Arrays.asList(6488), new WorldPoint(1799, 3756, 0), BankLocation.PISCARILIUS, Arrays.asList("Bait")),
            new FishingSpot("Living Rock Caverns", 85, Arrays.asList("cavefish", "rocktail"), Arrays.asList(6825), new WorldPoint(3652, 5123, 0), BankLocation.EDGEVILLE, Arrays.asList("Bait"))
    );
    private FishingSpot currentSpot = null;

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

            // Extract fishing method from flags
            configuredFishingMethod = "AUTO"; // Default
            if (settings.getFlags() != null) {
                for (String flag : settings.getFlags()) {
                    if (flag.startsWith("FISHING_METHOD:")) {
                        configuredFishingMethod = flag.substring("FISHING_METHOD:".length());
                        break;
                    }
                }
            }
        }
    }

    private void updateTier() {
        // Check if user has configured a specific fishing method
        if (!"AUTO".equals(configuredFishingMethod)) {
            // User has chosen a specific method, use that instead of auto-progression
            switch (configuredFishingMethod) {
                case "NET":
                case "BAIT":
                    tier = FishingTier.NET_BAIT;
                    break;
                case "LURE":
                    tier = FishingTier.LURE;
                    break;
                case "CAGE":
                case "HARPOON":
                    tier = FishingTier.CAGE_HARPOON;
                    break;
                default:
                    // Fallback to auto if unknown method
                    updateTierAuto();
                    break;
            }
        } else {
            // Auto mode: use level-based progression as before
            updateTierAuto();
        }
    }

    private void updateTierAuto() {
        // Use CURRENT level only, not target level
        // The bot will automatically upgrade tools when it reaches the required level
        int currentLvl = Microbot.getClient().getRealSkillLevel(Skill.FISHING);

        if (currentLvl >= 40) {
            tier = FishingTier.CAGE_HARPOON;
        } else if (currentLvl >= 20) {
            tier = FishingTier.LURE;
        } else {
            tier = FishingTier.NET_BAIT;
        }
    }

    private void updateCurrentSpot() {
        int lvl = Microbot.getClient().getRealSkillLevel(Skill.FISHING);
        FishingSpot best = null;

        for (FishingSpot spot : FISHING_SPOTS) {
            if (lvl >= spot.minLevel) {
                boolean supportsMethod = false;

                // If user has configured a specific fishing method, check if spot supports it
                if (!"AUTO".equals(configuredFishingMethod)) {
                    for (String action : spot.actions) {
                        switch (configuredFishingMethod) {
                            case "NET":
                                if (action.equalsIgnoreCase("Net")) supportsMethod = true;
                                break;
                            case "BAIT":
                                if (action.equalsIgnoreCase("Bait")) supportsMethod = true;
                                break;
                            case "LURE":
                                if (action.equalsIgnoreCase("Lure")) supportsMethod = true;
                                break;
                            case "CAGE":
                                if (action.equalsIgnoreCase("Cage")) supportsMethod = true;
                                break;
                            case "HARPOON":
                                if (action.equalsIgnoreCase("Harpoon")) supportsMethod = true;
                                break;
                        }
                    }
                    if (!supportsMethod) continue; // Skip spots that don't support the chosen method
                } else {
                    // AUTO mode: check if spot supports current tier's methods
                    for (String action : spot.actions) {
                        switch (tier) {
                            case NET_BAIT:
                                if (action.equalsIgnoreCase("Net") || action.equalsIgnoreCase("Bait")) {
                                    supportsMethod = true;
                                }
                                break;
                            case LURE:
                                if (action.equalsIgnoreCase("Lure")) {
                                    supportsMethod = true;
                                }
                                break;
                            case CAGE_HARPOON:
                                if (action.equalsIgnoreCase("Cage") || action.equalsIgnoreCase("Harpoon")) {
                                    supportsMethod = true;
                                }
                                break;
                        }
                    }
                    if (!supportsMethod) continue; // Skip spots that don't support current tier
                }

                if (best == null || (spot.bankLocation != null && best.bankLocation != null &&
                        Rs2Walker.getDistanceBetween(Microbot.getClient().getLocalPlayer().getWorldLocation(), spot.bankLocation.getWorldPoint()) <
                                Rs2Walker.getDistanceBetween(Microbot.getClient().getLocalPlayer().getWorldLocation(), best.bankLocation.getWorldPoint()))) {
                    best = spot;
                }
            }
        }
        currentSpot = best;
    }

    @Override
    public void execute() {
        if (!enabled) {
            Microbot.status = "Fishing: disabled";
            return;
        }
        if (!Microbot.isLoggedIn()) return;

        updateTier();
        updateCurrentSpot();
        if (currentSpot == null) {
            Microbot.status = "Fishing: no suitable spot found for your level";
            return;
        }

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

        WorldPoint target = currentSpot.location;
        if (target != null && Rs2Player.getWorldLocation().distanceTo(target) > 15) {
            if (Rs2Walker.walkTo(target, 4)) {
                Microbot.status = "Fishing: walking to spot";
            }
            return;
        }

        attemptFish();
    }

    private void attemptFish() {
        long now = System.currentTimeMillis();
        if (now - lastAttempt < ATTEMPT_COOLDOWN) return;
        if (Rs2Player.isAnimating() || Rs2Player.isMoving()) return;

        int[] ids = currentSpotIds();
        List<String> wanted = desiredActions();

        for (int id : ids) {
            Rs2NpcModel npc = Rs2Npc.getNpc(id);
            if (npc == null) continue;

            Set<String> npcActions = getAllActions(npc);
            if (npcActions.isEmpty()) continue;

            for (String w : wanted) {
                if (containsActionIgnoreCase(npcActions, w)) {
                    boolean ok = Rs2Npc.interact(npc, w);
                    if (ok) {
                        Microbot.status = "Fishing: " + configuredFishingMethod + " (" + w + ")";
                        lastAttempt = now;
                        return;
                    }
                }
            }
        }

        Microbot.status = "Fishing: no spot (retry)";
        lastAttempt = now;
    }

    private int[] currentSpotIds() {
        if (currentSpot != null) {
            return currentSpot.spotIds.stream().mapToInt(i -> i).toArray();
        }
        return new int[0];
    }

    private List<String> desiredActions() {
        if (currentSpot == null) {
            return Collections.emptyList();
        }

        // If user has configured a specific fishing method, filter actions accordingly
        if (!"AUTO".equals(configuredFishingMethod)) {
            List<String> filteredActions = new ArrayList<>();
            for (String action : currentSpot.actions) {
                switch (configuredFishingMethod) {
                    case "NET":
                        if (action.equalsIgnoreCase("Net")) {
                            filteredActions.add(action);
                        }
                        break;
                    case "BAIT":
                        if (action.equalsIgnoreCase("Bait")) {
                            filteredActions.add(action);
                        }
                        break;
                    case "LURE":
                        if (action.equalsIgnoreCase("Lure")) {
                            filteredActions.add(action);
                        }
                        break;
                    case "CAGE":
                        if (action.equalsIgnoreCase("Cage")) {
                            filteredActions.add(action);
                        }
                        break;
                    case "HARPOON":
                        if (action.equalsIgnoreCase("Harpoon")) {
                            filteredActions.add(action);
                        }
                        break;
                }
            }
            return filteredActions.isEmpty() ? currentSpot.actions : filteredActions;
        }

        // Auto mode: return all available actions for the current tier
        return currentSpot.actions;
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
        if (currentSpot != null && currentSpot.bankLocation != null) {
            int maxAttempts = 3;
            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                boolean ok = Rs2Bank.walkToBankAndUseBank(currentSpot.bankLocation) && Rs2Bank.isOpen();
                if (ok) {
                    Microbot.status = "Fishing: bank opened";
                    return true;
                }
                Microbot.status = "Fishing: opening bank (retry " + (attempt + 1) + ")...";
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
            Microbot.status = "Fishing: failed to open bank!";
            return false;
        }
        // Fallback to old logic if currentSpot is null
        int combat = Microbot.getClient().getLocalPlayer().getCombatLevel();
        BankLocation bank = (combat < DRAYNOR_MIN_COMBAT) ? LUMBRIDGE_BANK : DRAYNOR_BANK;
        int maxAttempts = 3;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            boolean ok = Rs2Bank.walkToBankAndUseBank(bank) && Rs2Bank.isOpen();
            if (ok) {
                Microbot.status = "Fishing: bank opened";
                return true;
            }
            Microbot.status = "Fishing: opening bank (retry " + (attempt + 1) + ")...";
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        }
        Microbot.status = "Fishing: failed to open bank!";
        return false;
    }

    // Debug getters
    public boolean isEnabled() { return enabled; }
    public String getMode() { return mode.name(); }
    public String getTierName() { return tier.name(); }
}
