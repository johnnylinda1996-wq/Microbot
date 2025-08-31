package net.runelite.client.plugins.microbot.aiobot.skills;

import net.runelite.api.NPCComposition;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.aiobot.settings.SkillRuntimeSettings;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.cache.Rs2NpcCache;
import net.runelite.client.plugins.microbot.util.cache.util.Rs2NpcCacheUtils;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * FishingSkillHandler
 * - Gebruikt NPC cache i.p.v. deprecated client.getNpcs()
 * - Zoekt fishing spots via naam + actie via compositie acties
 * - OR-logica voor equipment sets
 * - Inventory normalisatie: eerst alles deponeren behalve gekozen set & consumables
 * - Minimal cooldown om spam te voorkomen
 */
public class FishingSkillHandler implements SkillHandler {

    private boolean enabled = true;
    private String mode = "drop"; // drop | bank
    private boolean useBank = false;

    private FishingMethod currentMethod = FishingMethod.NET_BAIT;
    private List<String> chosenEquipmentGroup = new ArrayList<>();

    private long lastActionTs = 0;
    private static final long ACTION_COOLDOWN_MS = 1400;
    private long lastSpotFailTs = 0;
    private static final long SPOT_FAIL_COOLDOWN_MS = 3000;

    private static final int MAX_FISH_SPOT_RADIUS = 25;

    private static final Map<FishingMethod, List<FishingLocation>> LOCATIONS = new HashMap<>();
    static {
        LOCATIONS.put(FishingMethod.NET_BAIT, Arrays.asList(
                new FishingLocation("Draynor Village", new WorldPoint(3086, 3232, 0), BankLocation.DRAYNOR_VILLAGE, false),
                new FishingLocation("Lumbridge South", new WorldPoint(3238, 3241, 0), BankLocation.LUMBRIDGE_FRONT, false)
        ));
        LOCATIONS.put(FishingMethod.LURE_BAIT, Arrays.asList(
                new FishingLocation("Barbarian Village", new WorldPoint(3105, 3431, 0), BankLocation.EDGEVILLE, false),
                new FishingLocation("Lumbridge River", new WorldPoint(3238, 3251, 0), BankLocation.LUMBRIDGE_FRONT, false)
        ));
        LOCATIONS.put(FishingMethod.CAGE_HARPOON, Collections.singletonList(
                new FishingLocation("Karamja Dock", new WorldPoint(2925, 3180, 0), BankLocation.CAMELOT, false)
        ));
    }

    private enum FishingMethod {
        NET_BAIT("Net/Bait", 1,
                Arrays.asList(
                        List.of("Small fishing net"),
                        List.of("Fishing rod", "Fishing bait")
                ),
                Arrays.asList("Net", "Bait")),
        LURE_BAIT("Lure/Bait", 20,
                Collections.singletonList(List.of("Fly fishing rod", "Feather")),
                Collections.singletonList("Lure")),
        CAGE_HARPOON("Cage/Harpoon", 40,
                Arrays.asList(
                        List.of("Lobster pot"),
                        List.of("Harpoon")
                ),
                Arrays.asList("Cage", "Harpoon"));

        private final String name;
        private final int level;
        private final List<List<String>> equipmentGroups;
        private final List<String> preferredActions;
        FishingMethod(String name, int level, List<List<String>> eq, List<String> actions) {
            this.name = name;
            this.level = level;
            this.equipmentGroups = eq;
            this.preferredActions = actions;
        }
        boolean hasRequiredLevel() {
            return Microbot.getClient().getRealSkillLevel(Skill.FISHING) >= level;
        }
        String getName() { return name; }
        List<List<String>> getEquipmentGroups() { return equipmentGroups; }
        List<String> getPreferredActions() { return preferredActions; }
    }

    private static class FishingLocation {
        private final String name;
        private final WorldPoint location;
        private final BankLocation bank;
        private final boolean membersOnly;
        public FishingLocation(String name, WorldPoint location, BankLocation bank, boolean membersOnly) {
            this.name = name; this.location = location; this.bank = bank; this.membersOnly = membersOnly;
        }
        boolean isAccessible() { return !membersOnly || Rs2Player.isMember(); }
        WorldPoint getLocation() { return location; }
        BankLocation getBank() { return bank; }
        String getName() { return name; }
    }

    @Override
    public void applySettings(SkillRuntimeSettings settings) {
        if (settings != null) {
            enabled = settings.isEnabled();
            if (settings.getMode() != null) mode = settings.getMode().toLowerCase();
            useBank = "bank".equals(mode);
        }
    }

    @Override
    public void execute() {
        if (!enabled || !Microbot.isLoggedIn()) return;

        updateMethod();

        // Selecteer of verkrijg equipment set
        if (chosenEquipmentGroup.isEmpty() || !groupIntact(chosenEquipmentGroup)) {
            selectOrAcquireGroup();
            return;
        }

        // Inventory opschonen vóór start
        if (needsCleanup()) {
            if (!openNearestBank()) return;
            normalizeInventory();
            return;
        }

        // Volle inventaris
        if (Rs2Inventory.isFull()) {
            if (useBank) bankFish(); else dropFish();
            return;
        }

        if (!currentMethod.hasRequiredLevel()) {
            Microbot.status = "Fishing: level too low for " + currentMethod.getName();
            return;
        }

        FishingLocation loc = nearestLocation();
        if (loc != null && !atLocation(loc)) {
            walkTo(loc);
            return;
        }

        attemptFish();
    }

    /* ================= LOGIC ================= */

    private void updateMethod() {
        int lvl = Microbot.getClient().getRealSkillLevel(Skill.FISHING);
        FishingMethod desired = (lvl >= 40) ? FishingMethod.CAGE_HARPOON :
                (lvl >= 20) ? FishingMethod.LURE_BAIT :
                        FishingMethod.NET_BAIT;
        if (desired != currentMethod) {
            currentMethod = desired;
            chosenEquipmentGroup.clear();
            Microbot.log("[Fishing] Methode -> " + currentMethod.getName());
        }
    }

    private void selectOrAcquireGroup() {
        // Inventaris check
        for (List<String> group : currentMethod.getEquipmentGroups()) {
            if (groupPresent(group)) {
                chosenEquipmentGroup = new ArrayList<>(group);
                Microbot.status = "Fishing: using " + group;
                return;
            }
        }
        // Bank poging
        if (!openNearestBank()) return;
        for (List<String> group : currentMethod.getEquipmentGroups()) {
            if (canWithdraw(group)) {
                withdrawGroup(group);
                chosenEquipmentGroup = new ArrayList<>(group);
                Rs2Bank.closeBank();
                Microbot.status = "Fishing: withdrew group " + group;
                return;
            }
        }
        Rs2Bank.closeBank();
        Microbot.status = "Fishing: no equipment group available";
    }

    private boolean groupPresent(List<String> group) {
        for (String item : group) {
            if (!Rs2Inventory.contains(item)) return false;
        }
        return true;
    }

    private boolean groupIntact(List<String> group) {
        return groupPresent(group);
    }

    private boolean needsCleanup() {
        Set<String> allowed = new HashSet<>(chosenEquipmentGroup.stream()
                .map(String::toLowerCase).collect(Collectors.toSet()));
        allowed.add("fishing bait");
        allowed.add("feather");
        // Als er iets in inventory zit dat niet allowed is en geen vis nodig om te droppen -> cleanup
        return Rs2Inventory.all().stream().anyMatch(it -> {
            String n = it.getName();
            if (n == null) return false;
            String ln = n.toLowerCase();
            if (allowed.contains(ln)) return false;
            return true; // alles anders opruimen
        });
    }

    private void normalizeInventory() {
        if (!Rs2Bank.isOpen()) return;
        Set<String> keep = new HashSet<>(chosenEquipmentGroup);
        keep.add("Fishing bait");
        keep.add("Feather");
        Rs2Bank.depositAllExcept(keep.toArray(new String[0]));
        // Top up consumables
        if (chosenEquipmentGroup.stream().anyMatch(s -> s.toLowerCase().contains("rod"))) {
            if (!Rs2Inventory.contains("Fishing bait") && Rs2Bank.hasItem("Fishing bait"))
                Rs2Bank.withdrawX("Fishing bait", 300);
        }
        if (chosenEquipmentGroup.stream().anyMatch(s -> s.toLowerCase().contains("fly"))) {
            if (!Rs2Inventory.contains("Feather") && Rs2Bank.hasItem("Feather"))
                Rs2Bank.withdrawX("Feather", 300);
        }
        Rs2Bank.closeBank();
        Microbot.status = "Fishing: inventory normalized";
    }

    private boolean canWithdraw(List<String> group) {
        for (String item : group) {
            if (!Rs2Inventory.contains(item) && !Rs2Bank.hasItem(item)) return false;
        }
        return true;
    }

    private void withdrawGroup(List<String> group) {
        for (String item : group) {
            String li = item.toLowerCase();
            if (li.contains("net") || li.contains("pot") || li.contains("harpoon") || li.contains("rod")) {
                Rs2Bank.withdrawOne(item);
            } else if (li.contains("bait") || li.contains("feather")) {
                Rs2Bank.withdrawX(item, 300);
            } else {
                Rs2Bank.withdrawOne(item);
            }
        }
    }

    private void bankFish() {
        if (!openNearestBank()) return;
        Set<String> keep = new HashSet<>(chosenEquipmentGroup);
        keep.add("Fishing bait");
        keep.add("Feather");
        Rs2Bank.depositAllExcept(keep.toArray(new String[0]));
        Rs2Bank.closeBank();
        Microbot.status = "Fishing: banked fish";
        touchAction();
    }

    private void dropFish() {
        // We droppen alleen echte vissen (niet tools / bait)
        Rs2Inventory.dropAll(model -> {
            String n = model.getName();
            if (n == null) return false;
            String ln = n.toLowerCase();
            if (chosenEquipmentGroup.stream().anyMatch(e -> e.equalsIgnoreCase(n))) return false;
            if (ln.equals("fishing bait") || ln.equals("feather")) return false;
            return isFish(ln);
        });
        Microbot.status = "Fishing: dropped fish";
        touchAction();
    }

    private boolean isFish(String ln) {
        return ln.contains("shrimp") || ln.contains("anchovy") || ln.contains("sardine") ||
                ln.contains("herring") || ln.contains("trout") || ln.contains("salmon") ||
                ln.contains("tuna") || ln.contains("lobster") || ln.contains("bass") ||
                ln.contains("swordfish") || ln.contains("shark") || ln.contains("monkfish") ||
                ln.contains("angler") || ln.contains("karambwan") || ln.contains("manta") ||
                ln.contains("sea turtle") || ln.contains("cod") || ln.contains("pike") ||
                ln.contains("eel") || ln.contains("cavefish") || ln.contains("rocktail");
    }

    /* ============== Spot zoeken via cache ================= */

    private void attemptFish() {
        if (cooldown()) return;
        if (Rs2Player.isAnimating() || Rs2Player.isMoving()) return;

        Optional<Rs2NpcModel> spotOpt = findFishingSpot();
        if (spotOpt.isEmpty()) {
            if (System.currentTimeMillis() - lastSpotFailTs > SPOT_FAIL_COOLDOWN_MS) {
                Microbot.status = "Fishing: no spot found";
                lastSpotFailTs = System.currentTimeMillis();
            }
            return;
        }

        Rs2NpcModel spot = spotOpt.get();
        for (String action : currentMethod.getPreferredActions()) {
            if (!npcHasAction(spot, action)) continue;

            // NIEUW: gebruik non-deprecated variant -> Rs2Npc.interact(Rs2NpcModel, action)
            boolean interacted = Rs2Npc.interact(spot, action);
            if (interacted) {
                Rs2Player.waitForXpDrop(Skill.FISHING, true);
                Rs2Antiban.actionCooldown();
                Rs2Antiban.takeMicroBreakByChance();
                Microbot.status = "Fishing: " + currentMethod.getName() + " (" + action + ")";
                touchAction();
                return;
            }
        }
        touchAction(); // voorkom spam
    }

    private Optional<Rs2NpcModel> findFishingSpot() {
        // Filter: naam "Fishing spot" + minstens één van de gewenste acties
        Predicate<Rs2NpcModel> nameFilter = npc -> {
            String name = safeName(npc);
            return name != null && name.equalsIgnoreCase("Fishing spot");
        };
        Predicate<Rs2NpcModel> actionFilter = npc -> currentMethod.getPreferredActions().stream()
                .anyMatch(a -> npcHasAction(npc, a));

        return Rs2NpcCache.getAllNpcs()
                .filter(nameFilter.and(actionFilter))
                .filter(n -> n.getDistanceFromPlayer() <= MAX_FISH_SPOT_RADIUS)
                .min(Comparator.comparingInt(Rs2NpcModel::getDistanceFromPlayer));
    }

    private String safeName(Rs2NpcModel npc) {
        try {
            NPCComposition comp = npc.getTransformedComposition();
            if (comp != null && comp.getName() != null) return comp.getName();
            comp = npc.getComposition();
            return comp != null ? comp.getName() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean npcHasAction(Rs2NpcModel npc, String action) {
        if (action == null) return false;
        try {
            NPCComposition base = npc.getComposition();
            NPCComposition trans = npc.getTransformedComposition();
            return containsAction(base, action) || containsAction(trans, action);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean containsAction(NPCComposition comp, String action) {
        if (comp == null || comp.getActions() == null) return false;
        for (String a : comp.getActions()) {
            if (a != null && a.equalsIgnoreCase(action)) return true;
        }
        return false;
    }

    /* ============== Locatie / Bank helpers ================= */

    private FishingLocation nearestLocation() {
        List<FishingLocation> list = LOCATIONS.get(currentMethod);
        if (list == null || list.isEmpty()) return null;
        WorldPoint player = Rs2Player.getWorldLocation();
        return list.stream()
                .filter(FishingLocation::isAccessible)
                .min(Comparator.comparingInt(l -> l.getLocation().distanceTo(player)))
                .orElse(list.get(0));
    }

    private boolean atLocation(FishingLocation loc) {
        return Rs2Player.getWorldLocation().distanceTo(loc.getLocation()) <= 7;
    }

    private void walkTo(FishingLocation loc) {
        if (Rs2Walker.walkTo(loc.getLocation(), 4)) {
            Microbot.status = "Fishing: walking -> " + loc.getName();
            touchAction();
        }
    }

    private boolean openNearestBank() {
        BankLocation bank = Rs2Bank.getNearestBank();
        if (bank == null) {
            Microbot.status = "Fishing: no bank found";
            return false;
        }
        boolean ok = Rs2Bank.walkToBankAndUseBank(bank) && Rs2Bank.isOpen();
        if (!ok) Microbot.status = "Fishing: opening bank...";
        return ok;
    }

    /* ============== Timing helpers ================= */

    private boolean cooldown() {
        return (System.currentTimeMillis() - lastActionTs) < ACTION_COOLDOWN_MS;
    }

    private void touchAction() {
        lastActionTs = System.currentTimeMillis();
    }

    /* ============== Debug getters ================= */

    public FishingMethod getCurrentMethod() { return currentMethod; }
    public boolean isEnabled() { return enabled; }
    public String getMode() { return mode; }
}