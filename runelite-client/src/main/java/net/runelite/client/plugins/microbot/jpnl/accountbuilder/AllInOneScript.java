package net.runelite.client.plugins.microbot.jpnl.accountbuilder;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.enums.QuestType;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.enums.SkillType;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.enums.MinigameType;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.minigames.MinigameHandler;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.minigames.impl.*;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.quests.*;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.settings.SkillRuntimeSettings;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.settings.SkillSettingsRegistry;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.skills.*;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.tasks.*;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.queue.QueueManager;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.util.XpUtil;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.enums.Activity;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class AllInOneScript extends Script {

    private final AllInOneConfig config;
    private final ConfigManager configManager;
    private final String configGroup;
    private final String queueKey;

    private final SkillSettingsRegistry settingsRegistry;

    private final Map<SkillType, SkillHandler> skillHandlers = new EnumMap<>(SkillType.class);
    private final Map<QuestType, QuestHandler> questHandlers = new EnumMap<>(QuestType.class);
    private final Map<MinigameType, MinigameHandler> minigameHandlers = new EnumMap<>(MinigameType.class);

    private final QueueManager queueManager = new QueueManager();

    @Getter
    private AioTask currentTask;

    private ScheduledFuture<?> loopFuture;
    private volatile boolean running;
    private final AtomicBoolean paused = new AtomicBoolean(false);

    private static final long LOOP_DELAY_MS = 1000L;
    private static final long SETTINGS_REFRESH_MS = 5000L;
    private long lastSettingsRefresh = 0L;

    private final Gson gson;
    private final StatusAccessor statusAccessor = new StatusAccessor();

    public AllInOneScript(AllInOneConfig config,
                          ConfigManager configManager,
                          String configGroup,
                          String queueKey) {
        this.config = config;
        this.configManager = configManager;
        this.configGroup = configGroup;
        this.queueKey = queueKey;

        Gson g;
        try { g = new Gson(); } catch (Throwable t) { g = null; }
        this.gson = g;

        this.settingsRegistry = new SkillSettingsRegistry(config);
        initSkillHandlers();
        initQuestHandlers();
        initMinigameHandlers();
    }

    /* Queue API */

    public synchronized void addTask(AioTask task) {
        queueManager.add(task);
    }

    public synchronized void addSkillTask(SkillType skillType, Integer targetLevelOverride) {
        int current = Microbot.isLoggedIn() && Microbot.getClient() != null
                ? Microbot.getClient().getRealSkillLevel(mapToApi(skillType))
                : 1;
        int target = (targetLevelOverride != null && targetLevelOverride > 0)
                ? targetLevelOverride
                : 0; // 0 = fallback config
        queueManager.add(new AioSkillTask(skillType, current, target));
    }

    public synchronized void addSkillTaskTime(SkillType skillType, int minutes) {
        int current = Microbot.isLoggedIn() && Microbot.getClient() != null
                ? Microbot.getClient().getRealSkillLevel(mapToApi(skillType))
                : 1;
        queueManager.add(AioSkillTask.timeTask(skillType, current, minutes));
    }

    public synchronized void addQuestTask(QuestType questType) {
        queueManager.add(new AioQuestTask(questType));
    }

    public synchronized void addMinigameTask(MinigameType minigameType) {
        queueManager.add(new AioMinigameTask(minigameType));
    }

    // New overloaded method that accepts duration
    public synchronized void addMinigameTask(MinigameType minigameType, int durationMinutes) {
        queueManager.add(new AioMinigameTask(minigameType, durationMinutes));
    }

    public synchronized void removeTask(int index) {
        List<AioTask> snap = queueManager.snapshot();
        if (index >= 0 && index < snap.size()) {
            snap.remove(index);
            queueManager.setNewOrder(snap);
        }
    }

    public synchronized void clearQueue() {
        queueManager.clear();
        currentTask = null;
    }

    public synchronized List<String> getQueueDisplay() {
        List<String> list = new ArrayList<>();
        for (AioTask t : queueManager.snapshot()) list.add(t.getDisplay());
        return list;
    }

    public List<AioTask> getQueueSnapshotRaw() {
        return queueManager.snapshot();
    }

    public synchronized List<AioTask> getSnapshotQueue() {
        return queueManager.snapshot();
    }
    public synchronized List<AioTask> getQueueSnapshot() { return getSnapshotQueue(); }

    public StatusAccessor getStatusAccessor() { return statusAccessor; }

    public boolean isRunning() { return running; }
    public boolean isPaused() { return paused.get(); }

    /* Control */

    public void startLoop() {
        if (loopFuture != null && !loopFuture.isCancelled()) return;
        running = true;
        paused.set(false);
        Rs2Antiban.setActivity(Activity.GENERAL_COLLECTING);
        loopFuture = scheduledExecutorService.scheduleWithFixedDelay(
                this::tickSafe, 0, LOOP_DELAY_MS, TimeUnit.MILLISECONDS);
        log.info("[AIO] Loop gestart");
    }

    public void pauseLoop() {
        paused.set(true);
        Microbot.status = "AIO: Paused";
    }

    public void resumeLoop() {
        if (!running) {
            startLoop();
            return;
        }
        paused.set(false);
        Microbot.status = "AIO: Resumed";
    }

    public void shutdown() {
        running = false;
        paused.set(false);
        if (loopFuture != null) {
            loopFuture.cancel(true);
            loopFuture = null;
        }
        super.shutdown();
        log.info("[AIO] Script shutdown");
    }

    /* Loop */

    private void tickSafe() {
        try { tick(); } catch (Exception ex) {
            log.warn("[AIO] Exception in tick", ex);
        }
    }

    private void tick() {
        if (!running || paused.get()) return;

        if (!Microbot.isLoggedIn()) {
            Microbot.status = "Waiting login...";
            updateStatusAccessor();
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastSettingsRefresh >= SETTINGS_REFRESH_MS) {
            settingsRegistry.refresh();
            lastSettingsRefresh = now;
        }

        if (currentTask != null && currentTask.isComplete()) {
            log.info("[AIO] Task complete: {}", currentTask.getDisplay());
            currentTask = null;
        }

        if (currentTask == null) {
            currentTask = queueManager.poll();
            if (currentTask != null) {
                currentTask.markStarted();
                Microbot.status = "Start: " + currentTask.getDisplay();
                updateAntibanActivity(currentTask);
            } else {
                Microbot.status = "Idle - no tasks";
                updateStatusAccessor();
                return;
            }
        }

        if (currentTask instanceof AioSkillTask) {
            handleSkillTask((AioSkillTask) currentTask);
        } else if (currentTask instanceof AioQuestTask) {
            handleQuestTask((AioQuestTask) currentTask);
        } else if (currentTask instanceof AioMinigameTask) {
            handleMinigameTask((AioMinigameTask) currentTask);
        }

        updateStatusAccessor();
    }

    /* Task handlers */

    private void handleSkillTask(AioSkillTask task) {
        // Time completion check first
        if (task.checkTimeComplete()) {
            Microbot.status = "Time done: " + task.getSkillType().name();
            return;
        }
        SkillType st = task.getSkillType();
        SkillHandler handler = skillHandlers.get(st);
        if (handler == null) {
            task.markComplete();
            return;
        }

        // IMPORTANT: Refresh settings from config before executing task
        settingsRegistry.refresh();
        SkillRuntimeSettings settings = settingsRegistry.get(st);

        int target = task.getTargetLevel();
        if (!task.isTimeMode()) {
            if (target <= 0 && settings != null) target = settings.getTargetLevel();
            if (target <= 0) target = 99;
        }

        // XP tracking update
        task.updateXpTracking();

        // Apply current settings to the handler
        handler.applySettings(settings);

        // Pass config to handlers that need specific config settings
        if (handler instanceof FishingSkillHandler) {
            ((FishingSkillHandler) handler).setConfig(config);
        }

        if (!task.isTimeMode()) {
            Skill apiSkill = task.toRuneLiteSkill();
            int level = (Microbot.getClient() != null) ? Microbot.getClient().getRealSkillLevel(apiSkill) : 0;

            if (level >= target) {
                Microbot.status = "Target reached: " + st.name() + " (" + level + ")";
                task.markComplete();
                return;
            } else {
                Microbot.status = "Training " + st.name() + " (" + level + "/" + target + ")";
            }
        } else {
            long remaining = (task.getDurationMinutes() * 60_000L) - task.elapsedMs();
            Microbot.status = "Training " + st.name() + " (" + Math.max(0, remaining/1000) + "s left)";
        }

        handler.execute();
    }

    private void handleQuestTask(AioQuestTask task) {
        QuestHandler handler = questHandlers.get(task.getQuestType());
        if (handler == null) {
            task.markComplete();
            return;
        }
        handler.execute();
        Microbot.status = "Quest placeholder: " + task.getQuestType().name();
        task.markComplete();
    }

    private void handleMinigameTask(AioMinigameTask task) {
        MinigameHandler handler = minigameHandlers.get(task.getMinigameType());
        if (handler == null) {
            Microbot.status = "Minigame (no handler): " + task.getMinigameType().name();
            task.markComplete();
            return;
        }
        boolean done = false;
        try { done = handler.execute(); } catch (Exception ex) { Microbot.status = "Minigame err: " + ex.getClass().getSimpleName(); done = true; }
        Microbot.status = "Minigame: " + task.getMinigameType().name() + (handler.statusDetail().isEmpty()?"":" - "+handler.statusDetail());
        if (done) task.markComplete();
    }

    private void updateAntibanActivity(AioTask task) {
        if (task.getType() == AioTask.TaskType.SKILL) {
            SkillType st = ((AioSkillTask) task).getSkillType();
            switch (st) {
                case MINING: Rs2Antiban.setActivity(Activity.GENERAL_MINING); break;
                case WOODCUTTING: Rs2Antiban.setActivity(Activity.GENERAL_WOODCUTTING); break;
                case FISHING: Rs2Antiban.setActivity(Activity.GENERAL_FISHING); break;
                case COOKING: Rs2Antiban.setActivity(Activity.GENERAL_COOKING); break;
                case MAGIC: case RANGED: case ATTACK: case STRENGTH:
                case DEFENCE: case HITPOINTS:
                    Rs2Antiban.setActivity(Activity.GENERAL_COMBAT); break;
                default:
                    Rs2Antiban.setActivity(Activity.GENERAL_COLLECTING);
            }
        } else {
            Rs2Antiban.setActivity(Activity.GENERAL_COLLECTING);
        }
    }

    private void updateStatusAccessor() {
        String name = currentTask == null ? "none" : currentTask.getDisplay();
        int curLvl = 0; int tgtLvl = 0; long elapsed = 0; int xpGained = 0; double xpPerHour = 0; int xpToTarget = -1; double pctToTarget = 0;

        if (currentTask instanceof AioSkillTask) {
            AioSkillTask s = (AioSkillTask) currentTask;
            curLvl = s.currentLevel();
            if (s.isTimeMode()) {
                tgtLvl = 0;
                long durMs = s.getDurationMinutes() * 60_000L;
                elapsed = s.elapsedMs();
                pctToTarget = durMs > 0 ? Math.min(100.0, (elapsed * 100.0) / durMs) : 0;
            } else {
                int t = s.getTargetLevel();
                SkillRuntimeSettings cfg = settingsRegistry.get(s.getSkillType());
                if (t <= 0 && cfg != null) t = cfg.getTargetLevel();
                if (t <= 0) t = 99; tgtLvl = t;

                int currentXp = s.currentXp();
                xpGained = s.getXpGained();
                int targetXp = XpUtil.xpForTarget(t);
                xpToTarget = Math.max(0, targetXp - currentXp);
                pctToTarget = XpUtil.percentToTarget(currentXp, t);

                if (s.getStartTimestamp() > 0) {
                    elapsed = System.currentTimeMillis() - s.getStartTimestamp();
                    if (elapsed > 0 && xpGained > 0) {
                        xpPerHour = xpGained * (3600000.0 / elapsed);
                    }
                }
            }
        }

        if (currentTask != null && currentTask.getStartTimestamp() > 0 && elapsed == 0) {
            elapsed = System.currentTimeMillis() - currentTask.getStartTimestamp();
        }

        statusAccessor.update(
                name,
                Microbot.status == null ? "" : Microbot.status,
                curLvl,
                tgtLvl,
                elapsed,
                xpGained,
                xpPerHour,
                xpToTarget,
                pctToTarget
        );
    }

    /* Init handlers */

    private void initSkillHandlers() {
        skillHandlers.put(SkillType.ATTACK, new AttackSkillHandler());
        skillHandlers.put(SkillType.STRENGTH, new StrengthSkillHandler());
        skillHandlers.put(SkillType.DEFENCE, new DefenceSkillHandler());
        skillHandlers.put(SkillType.RANGED, new RangedSkillHandler());
        skillHandlers.put(SkillType.PRAYER, new PrayerSkillHandler());
        skillHandlers.put(SkillType.MAGIC, new MagicSkillHandler());
        skillHandlers.put(SkillType.RUNECRAFTING, new RunecraftingSkillHandler());
        skillHandlers.put(SkillType.CONSTRUCTION, new ConstructionSkillHandler());
        skillHandlers.put(SkillType.HITPOINTS, new HitpointsSkillHandler());
        skillHandlers.put(SkillType.AGILITY, new AgilitySkillHandler());
        skillHandlers.put(SkillType.HERBLORE, new HerbloreSkillHandler());
        skillHandlers.put(SkillType.THIEVING, new ThievingSkillHandler());
        skillHandlers.put(SkillType.CRAFTING, new CraftingSkillHandler());
        skillHandlers.put(SkillType.FLETCHING, new FletchingSkillHandler());
        skillHandlers.put(SkillType.SLAYER, new SlayerSkillHandler());
        skillHandlers.put(SkillType.HUNTER, new HunterSkillHandler());
        skillHandlers.put(SkillType.MINING, new MiningSkillHandler());
        skillHandlers.put(SkillType.SMITHING, new SmithingSkillHandler());
        skillHandlers.put(SkillType.FISHING, new FishingSkillHandler());
        skillHandlers.put(SkillType.COOKING, new CookingSkillHandler());
        skillHandlers.put(SkillType.FIREMAKING, new FiremakingSkillHandler());
        skillHandlers.put(SkillType.WOODCUTTING, new WoodcuttingSkillHandler());
        skillHandlers.put(SkillType.FARMING, new FarmingSkillHandler());
    }

    private void initQuestHandlers() {
        questHandlers.put(QuestType.COOKS_ASSISTANT, new CooksAssistantHandler());
        questHandlers.put(QuestType.DEMON_SLAYER, new DemonSlayerHandler());
        questHandlers.put(QuestType.THE_RESTLESS_GHOST, new RestlessGhostHandler());
        questHandlers.put(QuestType.ROMEO_AND_JULIET, new RomeoAndJulietHandler());
        questHandlers.put(QuestType.SHEEP_SHEARER, new SheepShearerHandler());
    }

    private void initMinigameHandlers() {
        // Lightweight placeholder handlers – extend later with real logic
        register(new BarbarianAssaultHandler());
        register(new WintertodtHandler());
        register(new PestControlHandler());
        register(new NightmareZoneHandler());
        register(new SoulWarsHandler());
        register(new GuardiansOfTheRiftHandler());
        register(new TitheFarmHandler());
        register(new FishingTrawlerHandler());
        register(new MageTrainingArenaHandler());
        register(new TempleTrekkingHandler());
    }

    private void register(MinigameHandler h) { if (h != null) minigameHandlers.put(h.getType(), h); }

    /* Persist */

    public void loadQueueFromConfig() {
        String raw = configManager.getConfiguration(configGroup, queueKey);
        if (raw == null || raw.isBlank()) return;
        try {
            List<AioTask> loaded = parseQueueJson(raw);
            queueManager.clear();
            for (AioTask t : loaded) queueManager.add(t);
            log.info("[AIO] Loaded queue: {} tasks", loaded.size());
        } catch (Exception ex) {
            log.warn("[AIO] Failed load queue", ex);
        }
    }

    public void saveQueueToConfig() {
        try {
            String json = buildQueueJson();
            configManager.setConfiguration(configGroup, queueKey, json);
            log.info("[AIO] Saved queue ({} chars)", json.length());
        } catch (Exception ex) {
            log.warn("[AIO] Failed save queue", ex);
        }
    }

    private List<AioTask> parseQueueJson(String raw) {
        List<AioTask> list = new ArrayList<>();
        if (gson != null) {
            JsonArray arr = gson.fromJson(raw, JsonArray.class);
            for (int i = 0; i < arr.size(); i++) {
                JsonObject o = arr.get(i).getAsJsonObject();
                String kind = o.get("kind").getAsString();
                if ("SKILL".equals(kind)) {
                    SkillType st = SkillType.valueOf(o.get("id").getAsString());
                    if (o.has("mode") && "TIME".equals(o.get("mode").getAsString())) {
                        int minutes = o.get("minutes").getAsInt();
                        list.add(AioSkillTask.timeTask(st, 1, minutes));
                    } else {
                        int target = o.get("target").getAsInt();
                        list.add(new AioSkillTask(st, 1, target));
                    }
                } else if ("QUEST".equals(kind)) {
                    QuestType qt = QuestType.valueOf(o.get("id").getAsString());
                    list.add(new AioQuestTask(qt));
                } else if ("MINIGAME".equals(kind)) {
                    MinigameType mt = MinigameType.valueOf(o.get("id").getAsString());
                    list.add(new AioMinigameTask(mt));
                }
            }
            return list;
        }
        String[] parts = raw.split(";");
        for (String p : parts) {
            String s = p.trim();
            if (s.isEmpty()) continue;
            if (s.startsWith("SKILL:")) {
                String[] seg = s.split(":");
                if (seg.length >= 3) {
                    SkillType st = SkillType.valueOf(seg[1]);
                    int target = Integer.parseInt(seg[2]);
                    list.add(new AioSkillTask(st, 1, target));
                }
            } else if (s.startsWith("QUEST:")) {
                String[] seg = s.split(":");
                if (seg.length >= 2) {
                    QuestType qt = QuestType.valueOf(seg[1]);
                    list.add(new AioQuestTask(qt));
                }
            } else if (s.startsWith("MINIGAME:")) {
                String[] seg = s.split(":");
                if (seg.length >= 2) {
                    MinigameType mt = MinigameType.valueOf(seg[1]);
                    list.add(new AioMinigameTask(mt));
                }
            }
        }
        return list;
    }

    private String buildQueueJson() {
        if (gson != null) {
            JsonArray arr = new JsonArray();
            if (currentTask != null && !currentTask.isComplete()) {
                arr.add(toJsonObj(currentTask));
            }
            for (AioTask t : queueManager.snapshot()) {
                arr.add(toJsonObj(t));
            }
            return gson.toJson(arr);
        }
        StringBuilder sb = new StringBuilder();
        if (currentTask != null && !currentTask.isComplete()) {
            appendLegacy(sb, currentTask);
        }
        for (AioTask t : queueManager.snapshot()) {
            appendLegacy(sb, t);
        }
        return sb.toString();
    }

    private JsonObject toJsonObj(AioTask t) {
        JsonObject o = new JsonObject();
        o.addProperty("kind", t.getType().name());
        if (t instanceof AioSkillTask) {
            AioSkillTask s = (AioSkillTask) t;
            o.addProperty("id", s.getSkillType().name());
            if (s.isTimeMode()) {
                o.addProperty("mode", "TIME");
                o.addProperty("minutes", s.getDurationMinutes());
            } else {
                o.addProperty("mode", "LEVEL");
                o.addProperty("target", s.getTargetLevel());
            }
        } else if (t instanceof AioQuestTask) {
            AioQuestTask q = (AioQuestTask) t;
            o.addProperty("id", q.getQuestType().name());
        } else if (t instanceof AioMinigameTask) {
            AioMinigameTask m = (AioMinigameTask) t;
            o.addProperty("id", m.getMinigameType().name());
        }
        return o;
    }

    private void appendLegacy(StringBuilder sb, AioTask t) {
        if (t instanceof AioSkillTask) {
            AioSkillTask s = (AioSkillTask) t;
            sb.append("SKILL:").append(s.getSkillType().name()).append(":").append(s.getTargetLevel()).append(";");
        } else if (t instanceof AioQuestTask) {
            AioQuestTask q = (AioQuestTask) t;
            sb.append("QUEST:").append(q.getQuestType().name()).append(";");
        } else if (t instanceof AioMinigameTask) {
            AioMinigameTask m = (AioMinigameTask) t;
            sb.append("MINIGAME:").append(m.getMinigameType().name()).append(";");
        }
    }

    private Skill mapToApi(SkillType st) {
        switch (st) {
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

    public synchronized boolean moveTaskUp(int index) {
        return queueManager.moveUp(index);
    }

    public synchronized boolean moveTaskDown(int index) {
        return queueManager.moveDown(index);
    }

    // --- Added utility operations for enhanced GUI ---
    public synchronized void shuffleQueue() {
        List<AioTask> snap = queueManager.snapshot();
        if (snap.size() < 2) return;
        Collections.shuffle(snap);
        queueManager.setNewOrder(snap);
    }

    public synchronized void moveTaskTop(int index) {
        List<AioTask> snap = queueManager.snapshot();
        if (index <= 0 || index >= snap.size()) return;
        AioTask t = snap.remove(index);
        snap.add(0, t);
        queueManager.setNewOrder(snap);
    }

    public synchronized void moveTaskBottom(int index) {
        List<AioTask> snap = queueManager.snapshot();
        if (index < 0 || index >= snap.size()-1) return;
        AioTask t = snap.remove(index);
        snap.add(t);
        queueManager.setNewOrder(snap);
    }

    public synchronized void duplicateTask(int index) {
        List<AioTask> snap = queueManager.snapshot();
        if (index < 0 || index >= snap.size()) return;
        AioTask orig = snap.get(index);
        AioTask copy = null;
        if (orig instanceof AioSkillTask) {
            AioSkillTask s = (AioSkillTask) orig;
            if (s.isTimeMode()) {
                copy = AioSkillTask.timeTask(s.getSkillType(), s.getStartLevel(), s.getDurationMinutes());
            } else {
                copy = new AioSkillTask(s.getSkillType(), s.getStartLevel(), s.getTargetLevel());
            }
        } else if (orig instanceof AioQuestTask) {
            copy = new AioQuestTask(((AioQuestTask) orig).getQuestType());
        } else if (orig instanceof AioMinigameTask) {
            copy = new AioMinigameTask(((AioMinigameTask) orig).getMinigameType());
        }
        if (copy != null) {
            snap.add(index + 1, copy);
            queueManager.setNewOrder(snap);
        }
    }

    public synchronized void skipCurrentTask() {
        if (currentTask == null) return;
        if (currentTask instanceof AioSkillTask) {
            ((AioSkillTask) currentTask).markComplete();
        } else if (currentTask instanceof AioQuestTask) {
            ((AioQuestTask) currentTask).markComplete();
        } else if (currentTask instanceof AioMinigameTask) {
            ((AioMinigameTask) currentTask).markComplete();
        }
    }

    public synchronized void editSkillTask(int index, Integer newTargetLevel, Integer newMinutes, boolean timeMode) {
        List<AioTask> snap = queueManager.snapshot();
        if (index < 0 || index >= snap.size()) return;
        AioTask t = snap.get(index);
        if (t instanceof AioSkillTask) {
            AioSkillTask st = (AioSkillTask) t;
            if (timeMode) {
                if (newMinutes != null && newMinutes > 0) {
                    st.setTimeMode(newMinutes);
                }
            } else {
                if (newTargetLevel != null && newTargetLevel > 0) {
                    st.setTargetLevel(newTargetLevel);
                }
            }
            queueManager.setNewOrder(snap);
        }
    }

    public AllInOneConfig getConfig() {
        return config;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public String getConfigGroup() {
        return configGroup;
    }
}
