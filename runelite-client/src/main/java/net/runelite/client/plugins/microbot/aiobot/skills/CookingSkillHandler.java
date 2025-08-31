package net.runelite.client.plugins.microbot.aiobot.skills;

import net.runelite.api.GameObject;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.aiobot.settings.SkillRuntimeSettings;
import net.runelite.client.plugins.microbot.cooking.enums.CookingAreaType;
import net.runelite.client.plugins.microbot.cooking.enums.CookingItem;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.camera.Rs2Camera;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.awt.event.KeyEvent;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Verbeterde CookingSkillHandler:
 * - Laat cook-all volledig uitlopen (geen spam use).
 * - Bepaalt batch einde pas bij rawCount == 0 of stall-timeout.
 * - Bankt opnieuw zodra inventory leeg is.
 * - Herselecteert pas na bevestiging (bank open) dat huidig item op is.
 */
public class CookingSkillHandler implements SkillHandler {

    /* ============== CONFIG CONSTANTS ============== */
    private static final long LOOP_INTERVAL_MS = 150;
    private static final long GENERIC_ACTION_COOLDOWN = 500;
    private static final long COOK_STALL_TIMEOUT_MS = 4500; // tijd zonder raw verbruik voordat we herstarten
    private static final int DIST_TO_SPOT = 5;

    /* ============== RUNTIME VARS ============== */
    private boolean enabled = true;
    private String mode = "range"; // wordt overschreven door applySettings()
    private boolean dropBurnt = true;

    private CookingItem activeItem;
    private CookingState state = CookingState.FETCHING;

    private long stateSince;
    private long lastLoop;

    private int lastRawCount = -1;
    private long lastRawChangeTime = 0L;
    private boolean batchStarted = false;

    /* ============== STATES ============== */
    private enum CookingState {
        FETCHING,
        WALKING,
        STARTING,
        COOKING,
        DROPPING
    }

    /* ============== LOCATIONS ============== */
    private static class CookingLocation {
        private final String name;
        private final WorldPoint point;
        private final BankLocation bank;
        private final boolean membersOnly;
        CookingLocation(String name, WorldPoint p, BankLocation bank, boolean membersOnly) {
            this.name = name;
            this.point = p;
            this.bank = bank;
            this.membersOnly = membersOnly;
        }
        boolean isAccessible() { return !membersOnly || Rs2Player.isMember(); }
        WorldPoint getPoint() { return point; }
        String getName() { return name; }
    }

    private static final List<CookingLocation> COOKING_LOCATIONS = Arrays.asList(
            new CookingLocation("Al Kharid", new WorldPoint(3273, 3181, 0), BankLocation.AL_KHARID, false),
            new CookingLocation("Lumbridge", new WorldPoint(3209, 3214, 0), BankLocation.LUMBRIDGE_FRONT, false),
            new CookingLocation("Varrock East", new WorldPoint(3253, 3421, 0), BankLocation.VARROCK_EAST, false),
            new CookingLocation("Edgeville", new WorldPoint(3077, 3492, 0), BankLocation.EDGEVILLE, false)
    );

    /* ============== SETTINGS ============== */
    @Override
    public void applySettings(SkillRuntimeSettings settings) {
        if (settings == null) return;
        enabled = settings.isEnabled();

        // Koppel cooking mode uit de config
        if (settings.getMode() != null) {
            mode = settings.getMode().toLowerCase(Locale.ROOT);
        }

        // Koppel cooking gauntlets flag
        if (settings.getFlags().contains("COOKING_GAUNTLETS")) {
            // Dit zou gebruikt kunnen worden voor logic om gauntlets aan te trekken
            // Voor nu loggen we het alleen
        }
    }

    /* ============== MAIN LOOP ============== */
    @Override
    public void execute() {
        if (!enabled || !Microbot.isLoggedIn()) return;

        long now = System.currentTimeMillis();
        if (now - lastLoop < LOOP_INTERVAL_MS) return;
        lastLoop = now;

        if (stateSince == 0L) stateSince = now;

        // Houd activeItem geldig qua level & mode maar gooi hem NIET weg als alleen de voorraad op is (dat handelen we in FETCHING).
        ensureActiveItemValidity();

        switch (state) {
            case FETCHING: handleFetching(); break;
            case WALKING:  handleWalking();  break;
            case STARTING: handleStarting(); break;
            case COOKING:  handleCooking();  break;
            case DROPPING: handleDropping(); break;
        }
    }

    /* ============== STATE HANDLERS ============== */

    private void handleFetching() {
        // Deposit cooked fish if present before withdrawing new raw fish
        if (activeItem != null && Rs2Inventory.hasItem(activeItem.getCookedItemName())) {
            if (!openNearestBank()) {
                Microbot.status = "Cooking: open bank to deposit cooked fish...";
                return;
            }
            Rs2Bank.depositAll(activeItem.getCookedItemName());
            Microbot.status = "Cooking: deposited cooked fish";
            Rs2Bank.closeBank();
        }

        if (activeItem == null) {
            // Geen huidig item: probeer direct bank + selectie
            if (!openNearestBank()) {
                Microbot.status = "Cooking: no item";
                return;
            }
            selectBestFromBankWhenOpen(); // kiest nieuw activeItem
            Rs2Bank.closeBank();
            if (activeItem == null) {
                Microbot.status = "Cooking: nothing available";
                return;
            }
            resetState(CookingState.WALKING);
            return;
        }

        // We hebben activeItem – check of het in inventory zit
        if (Rs2Inventory.hasItem(activeItem.getRawItemName())) {
            resetState(CookingState.WALKING);
            return;
        }

        // Niet in inventory: open bank en kijk of het daar nog is
        if (!openNearestBank()) {
            Microbot.status = "Cooking: open bank...";
            return;
        }

        if (Rs2Bank.hasItem(activeItem.getRawItemName())) {
            Rs2Bank.withdrawAll(activeItem.getRawItemName());
            Microbot.status = "Cooking: withdraw " + activeItem.getRawItemName();
            Rs2Bank.closeBank();
            resetState(CookingState.WALKING);
            return;
        }

        // Huidige item bestaat niet in bank -> kies ander
        selectBestFromBankWhenOpen();
        Rs2Bank.closeBank();
        if (activeItem == null) {
            Microbot.status = "Cooking: nothing to cook";
            return;
        }
        resetState(CookingState.WALKING);
    }

    private void handleWalking() {
        if (activeItem == null) {
            resetState(CookingState.FETCHING);
            return;
        }
        CookingLocation loc = nearestCookingLocation();
        if (loc == null) {
            Microbot.status = "Cooking: no location";
            return;
        }
        if (!atLocation(loc)) {
            if (System.currentTimeMillis() - stateSince < GENERIC_ACTION_COOLDOWN) return;
            Rs2Walker.walkTo(loc.getPoint(), 4);
            Microbot.status = "Cooking: walking " + loc.getName();
            return;
        }
        resetState(CookingState.STARTING);
    }

    private void handleStarting() {
        if (activeItem == null) {
            resetState(CookingState.FETCHING);
            return;
        }
        if (!Rs2Inventory.hasItem(activeItem.getRawItemName())) {
            resetState(CookingState.FETCHING);
            return;
        }

        GameObject source = findCookingSource();
        if (source == null) {
            Microbot.status = "Cooking: no source";
            resetState(CookingState.WALKING);
            return;
        }

        if (!Rs2Camera.isTileOnScreen(source.getLocalLocation())) {
            Rs2Camera.turnTo(source.getLocalLocation());
            Microbot.status = "Cooking: camera adjust";
            return;
        }

        if (System.currentTimeMillis() - stateSince < GENERIC_ACTION_COOLDOWN) return;

        boolean used = Rs2Inventory.useItemOnObject(activeItem.getRawItemID(), source.getId());
        if (!used) {
            Microbot.status = "Cooking: use failed";
            return;
        }

        Microbot.status = "Cooking: waiting cook-all";
        waitForCookWidgetOrFail();
    }

    private void handleCooking() {
        if (activeItem == null) {
            resetState(CookingState.FETCHING);
            return;
        }

        int currentRaw = Rs2Inventory.itemQuantity(activeItem.getRawItemID());
        long now = System.currentTimeMillis();

        if (!batchStarted) {
            batchStarted = true;
            lastRawCount = currentRaw;
            lastRawChangeTime = now;
        }

        if (currentRaw < lastRawCount) {
            // Er is vooruitgang (item verbruikt)
            lastRawCount = currentRaw;
            lastRawChangeTime = now;
        }

        // Batch klaar
        if (currentRaw == 0) {
            if (dropBurnt && hasBurntItem(activeItem)) {
                resetState(CookingState.DROPPING);
            } else {
                resetState(CookingState.FETCHING);
            }
            return;
        }

        // Geen raw verbruik & geen animatie te lang -> waarschijnlijk dialog gemist / onderbroken
        boolean anim = Rs2Player.isAnimating(800);
        if ((now - lastRawChangeTime) > COOK_STALL_TIMEOUT_MS && !anim) {
            // Herstart cook poging
            resetState(CookingState.STARTING);
        } else {
            Microbot.status = "Cooking: batch (" + currentRaw + " left)";
        }
    }

    private void handleDropping() {
        if (activeItem == null) {
            resetState(CookingState.FETCHING);
            return;
        }
        if (!hasBurntItem(activeItem)) {
            resetState(CookingState.FETCHING);
            return;
        }
        Microbot.status = "Cooking: drop burnt " + activeItem.getBurntItemName();
        Rs2Inventory.dropAll(i -> i.getName().equalsIgnoreCase(activeItem.getBurntItemName()));
        resetState(CookingState.FETCHING);
    }

    /* ============== SELECTION / VALIDATION ============== */

    private void ensureActiveItemValidity() {
        if (activeItem == null) return;
        int level = Rs2Player.getRealSkillLevel(Skill.COOKING);
        if (level < levelReq(activeItem) || !areaCompatible(activeItem)) {
            activeItem = null;
        }
    }

    /**
     * Alleen callen wanneer bank open is om nieuwe activeItem te kiezen.
     */
    private void selectBestFromBankWhenOpen() {
        int level = Rs2Player.getRealSkillLevel(Skill.COOKING);
        CookingItem best = Arrays.stream(CookingItem.values())
                .filter(ci -> level >= levelReq(ci))
                .filter(this::areaCompatible)
                .filter(ci ->
                        Rs2Inventory.hasItem(ci.getRawItemName()) ||
                                Rs2Bank.hasItem(ci.getRawItemName()) // bank is open hier
                )
                .max(Comparator.comparingInt(this::levelReq))
                .orElse(null);

        activeItem = best;
        if (best != null) {
            Microbot.status = "Cooking: select " + best.getRawItemName();
        }
    }

    private int levelReq(CookingItem item) {
        return item.getLevelRequired();
    }

    private boolean areaCompatible(CookingItem item) {
        CookingAreaType type = item.getCookingAreaType();
        switch (mode) {
            case "fire":  return type == CookingAreaType.FIRE || type == CookingAreaType.BOTH;
            case "range": return type == CookingAreaType.RANGE || type == CookingAreaType.BOTH;
            default:      return true;
        }
    }

    /* ============== COOK START HELPERS ============== */

    private void waitForCookWidgetOrFail() {
        new Thread(() -> {
            long start = System.currentTimeMillis();
            boolean found = false;
            while (System.currentTimeMillis() - start < 4000) {
                if (hasCookAllWidget()) {
                    found = true;
                    break;
                }
                try { Thread.sleep(120); } catch (InterruptedException ignored) {}
            }
            if (found) {
                Rs2Keyboard.keyPress(KeyEvent.VK_SPACE);
                Rs2Antiban.actionCooldown();
                Rs2Antiban.takeMicroBreakByChance();
                int currentRaw = Rs2Inventory.itemQuantity(activeItem.getRawItemID());
                lastRawCount = currentRaw;
                lastRawChangeTime = System.currentTimeMillis();
                batchStarted = false; // wordt true bij eerste COOKING tick
                resetState(CookingState.COOKING);
            } else {
                // mislukt, opnieuw proberen
                resetState(CookingState.STARTING);
            }
        }).start();
    }

    private boolean hasCookAllWidget() {
        return Rs2Widget.findWidget("like to cook?", null, false) != null
                || Rs2Widget.findWidget("How many would you like to cook?", null, false) != null
                || Rs2Widget.findWidget("How many would you like to burn?", null, false) != null
                || Rs2Widget.findWidget("How many would you like to make?", null, false) != null;
    }

    /* ============== WORLD / OBJECT HELPERS ============== */

    private CookingLocation nearestCookingLocation() {
        WorldPoint me = Rs2Player.getWorldLocation();
        if (me == null) return null;
        return COOKING_LOCATIONS.stream()
                .filter(CookingLocation::isAccessible)
                .min(Comparator.comparingInt(c -> c.getPoint().distanceTo(me)))
                .orElse(null);
    }

    private boolean atLocation(CookingLocation loc) {
        WorldPoint me = Rs2Player.getWorldLocation();
        return me != null && me.distanceTo(loc.getPoint()) <= DIST_TO_SPOT;
    }

    private GameObject findCookingSource() {
        if ("range".equals(mode)) {
            GameObject r = Rs2GameObject.getGameObject("Range", true);
            if (r != null) return r;
            return Rs2GameObject.getGameObject("Fire", true);
        }
        if ("fire".equals(mode)) {
            GameObject f = Rs2GameObject.getGameObject("Fire", true);
            if (f != null) return f;
            return Rs2GameObject.getGameObject("Range", true);
        }
        GameObject r = Rs2GameObject.getGameObject("Range", true);
        if (r != null) return r;
        return Rs2GameObject.getGameObject("Fire", true);
    }

    private boolean hasBurntItem(CookingItem item) {
        if (item.getBurntItemID() <= 0) return false;
        String name = item.getBurntItemName();
        return name != null && !name.equalsIgnoreCase("none") && Rs2Inventory.hasItem(name);
    }

    private boolean openNearestBank() {
        if (Rs2Bank.isOpen()) return true;
        BankLocation nearest = Rs2Bank.getNearestBank();
        if (nearest == null) return false;
        return Rs2Bank.walkToBankAndUseBank(nearest);
    }

    private void resetState(CookingState newState) {
        state = newState;
        stateSince = System.currentTimeMillis();
        if (newState != CookingState.COOKING) {
            // reset batch markers buiten COOKING
            batchStarted = false;
            lastRawCount = -1;
            lastRawChangeTime = 0L;
        }
    }

    /* ============== GETTERS (debug / ui) ============== */
    public CookingItem getActiveItem() { return activeItem; }
    public String getMode() { return mode; }
    public boolean isEnabled() { return enabled; }
    public String getState() { return state.name(); }
}
