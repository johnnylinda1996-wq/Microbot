package net.runelite.client.plugins.microbot.aiobot;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.aiobot.enums.QuestType;
import net.runelite.client.plugins.microbot.aiobot.enums.SkillType;
import net.runelite.client.plugins.microbot.aiobot.quests.*;
import net.runelite.client.plugins.microbot.aiobot.settings.SkillRuntimeSettings;
import net.runelite.client.plugins.microbot.aiobot.settings.SkillSettingsRegistry;
import net.runelite.client.plugins.microbot.aiobot.skills.*;
import net.runelite.client.plugins.microbot.aiobot.tasks.AioQuestTask;
import net.runelite.client.plugins.microbot.aiobot.tasks.AioSkillTask;
import net.runelite.client.plugins.microbot.aiobot.tasks.AioTask;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.enums.Activity;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
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

    private final List<AioTask> taskQueue = new CopyOnWriteArrayList<>();

    @Getter
    private AioTask currentTask;

    private ScheduledFuture<?> loopFuture;
    private volatile boolean running;
    private final AtomicBoolean paused = new AtomicBoolean(false);

    private static final long LOOP_DELAY_MS = 1000L;
    private static final long SETTINGS_REFRESH_MS = 5000L;
    private long lastSettingsRefresh = 0L;

    private final Gson gson;

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
    }

    /* ================== Queue API ================== */

    public synchronized void addTask(AioTask task) {
        taskQueue.add(task);
    }

    public synchronized void addSkillTask(SkillType skillType, Integer targetLevelOverride) {
        int current = Microbot.isLoggedIn() && Microbot.getClient() != null
                ? Microbot.getClient().getRealSkillLevel(mapToApi(skillType))
                : 1;
        int target = (targetLevelOverride != null && targetLevelOverride > 0)
                ? targetLevelOverride
                : 99;
        taskQueue.add(new AioSkillTask(skillType, current, target));
    }

    public synchronized void addQuestTask(QuestType questType) {
        taskQueue.add(new AioQuestTask(questType));
    }

    public synchronized void removeTask(int index) {
        if (index >= 0 && index < taskQueue.size()) {
            taskQueue.remove(index);
        }
    }

    public synchronized void clearQueue() {
        taskQueue.clear();
        currentTask = null;
    }

    public List<AioTask> getSnapshotQueue() {
        return new ArrayList<>(taskQueue);
    }

    public boolean isRunning() { return running; }
    public boolean isPaused() { return paused.get(); }

    /* ================== Control ================== */

    public void startLoop() {
        if (loopFuture != null && !loopFuture.isCancelled()) return;
        running = true;
        paused.set(false);
        Rs2Antiban.setActivity(Activity.GENERAL_COLLECTING);
        loopFuture = scheduledExecutorService.scheduleWithFixedDelay(
                this::tickSafe, 0, LOOP_DELAY_MS, TimeUnit.MILLISECONDS);
        log.info("[AIO] Loop started");
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

    /* ================== Loop ================== */

    private void tickSafe() {
        try { tick(); } catch (Exception ex) {
            log.warn("[AIO] Exception in tick", ex);
        }
    }

    private void tick() {
        if (!running || paused.get()) return;
        if (!Microbot.isLoggedIn()) {
            Microbot.status = "Waiting login...";
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastSettingsRefresh >= SETTINGS_REFRESH_MS) {
            settingsRegistry.refresh();
            lastSettingsRefresh = now;
        }

        if (currentTask != null && currentTask.isComplete()) {
            Microbot.log("Task completed: " + currentTask.getDisplay());
            currentTask = null;
        }
        if (currentTask == null) {
            currentTask = pollNextTask();
            if (currentTask != null) {
                Microbot.status = "Start: " + currentTask.getDisplay();
                updateAntibanActivity(currentTask);
            } else {
                Microbot.status = "Idle - no tasks";
                return;
            }
        }

        if (currentTask instanceof AioSkillTask) {
            handleSkillTask((AioSkillTask) currentTask);
        } else if (currentTask instanceof AioQuestTask) {
            handleQuestTask((AioQuestTask) currentTask);
        }
    }

    private AioTask pollNextTask() {
        if (taskQueue.isEmpty()) return null;
        return taskQueue.remove(0);
    }

    /* ================== Handlers ================== */

    private void handleSkillTask(AioSkillTask task) {
        SkillType st = task.getSkillType();
        SkillHandler handler = skillHandlers.get(st);
        if (handler == null) {
            task.markComplete();
            return;
        }

        SkillRuntimeSettings settings = settingsRegistry.get(st);
        int effectiveTarget = task.getTargetLevel();
        if (effectiveTarget <= 0 && settings != null) {
            effectiveTarget = settings.getTargetLevel();
        }
        if (effectiveTarget <= 0) effectiveTarget = 99;

        if (handler instanceof FishingSkillHandler) {
            ((FishingSkillHandler) handler).applySettings(settings);
        }

        Skill apiSkill = task.toRuneLiteSkill();
        int level = apiSkill != null ? Microbot.getClient().getRealSkillLevel(apiSkill) : 0;

        if (level >= effectiveTarget) {
            Microbot.status = "Target reached: " + st.getDisplayName() + " (" + level + ")";
            task.markComplete();
            return;
        } else {
            Microbot.status = "Training " + st.getDisplayName() + " (" + level + "/" + effectiveTarget + ")";
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
        Microbot.status = "Quest placeholder: " + task.getQuestType().getDisplayName();
        task.markComplete();
    }

    private void updateAntibanActivity(AioTask task) {
        if (task.getType() == AioTask.TaskType.SKILL) {
            AioSkillTask sk = (AioSkillTask) task;
            switch (sk.getSkillType()) {
                case MINING:
                    Rs2Antiban.setActivity(Activity.GENERAL_MINING); break;
                case WOODCUTTING:
                    Rs2Antiban.setActivity(Activity.GENERAL_WOODCUTTING); break;
                case FISHING:
                    Rs2Antiban.setActivity(Activity.GENERAL_FISHING); break;
                case COOKING:
                    Rs2Antiban.setActivity(Activity.GENERAL_COOKING); break;
                case MAGIC:
                case RANGED:
                case ATTACK:
                case STRENGTH:
                case DEFENCE:
                case HITPOINTS:
                    Rs2Antiban.setActivity(Activity.GENERAL_COMBAT); break;
                default:
                    Rs2Antiban.setActivity(Activity.GENERAL_COLLECTING);
            }
        } else {
            Rs2Antiban.setActivity(Activity.GENERAL_COLLECTING);
        }
    }

    /* ================== Init ================== */

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

    /* ================== Persist (JSON) ================== */

    public void loadQueueFromConfig() {
        String raw = configManager.getConfiguration(configGroup, queueKey);
        if (raw == null || raw.isBlank()) return;
        try {
            List<AioTask> loaded = parseQueueJson(raw);
            synchronized (this) {
                taskQueue.clear();
                taskQueue.addAll(loaded);
            }
            log.info("[AIO] Loaded queue ({} tasks)", loaded.size());
        } catch (Exception ex) {
            log.warn("[AIO] Could not load queue", ex);
        }
    }

    public void saveQueueToConfig() {
        try {
            String json = buildQueueJson();
            configManager.setConfiguration(configGroup, queueKey, json);
            log.info("[AIO] Saved queue ({} chars)", json.length());
        } catch (Exception ex) {
            log.warn("[AIO] Could not save queue", ex);
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
                    int target = o.get("target").getAsInt();
                    list.add(new AioSkillTask(st, 1, target));
                } else if ("QUEST".equals(kind)) {
                    QuestType qt = QuestType.valueOf(o.get("id").getAsString());
                    list.add(new AioQuestTask(qt));
                }
            }
            return list;
        }
        // fallback simple
        String[] parts = raw.split(";");
        for (String p : parts) {
            p = p.trim();
            if (p.isEmpty()) continue;
            if (p.startsWith("SKILL:")) {
                String[] seg = p.split(":");
                if (seg.length >= 3) {
                    SkillType st = SkillType.valueOf(seg[1]);
                    int target = Integer.parseInt(seg[2]);
                    list.add(new AioSkillTask(st, 1, target));
                }
            } else if (p.startsWith("QUEST:")) {
                String[] seg = p.split(":");
                if (seg.length >= 2) {
                    QuestType qt = QuestType.valueOf(seg[1]);
                    list.add(new AioQuestTask(qt));
                }
            }
        }
        return list;
    }

    private String buildQueueJson() {
        if (gson != null) {
            JsonArray arr = new JsonArray();
            for (AioTask t : getSnapshotQueueWithCurrentFirst()) {
                JsonObject o = new JsonObject();
                o.addProperty("kind", t.getType().name());
                if (t instanceof AioSkillTask) {
                    AioSkillTask s = (AioSkillTask) t;
                    o.addProperty("id", s.getSkillType().name());
                    o.addProperty("target", s.getTargetLevel());
                } else if (t instanceof AioQuestTask) {
                    AioQuestTask q = (AioQuestTask) t;
                    o.addProperty("id", q.getQuestType().name());
                }
                arr.add(o);
            }
            return gson.toJson(arr);
        }
        StringBuilder sb = new StringBuilder();
        for (AioTask t : getSnapshotQueueWithCurrentFirst()) {
            if (t instanceof AioSkillTask) {
                AioSkillTask s = (AioSkillTask) t;
                sb.append("SKILL:").append(s.getSkillType().name()).append(":").append(s.getTargetLevel());
            } else if (t instanceof AioQuestTask) {
                AioQuestTask q = (AioQuestTask) t;
                sb.append("QUEST:").append(q.getQuestType().name());
            }
            sb.append(";");
        }
        return sb.toString();
    }

    private List<AioTask> getSnapshotQueueWithCurrentFirst() {
        List<AioTask> snapshot = new ArrayList<>();
        if (currentTask != null && !currentTask.isComplete()) {
            snapshot.add(currentTask);
        }
        snapshot.addAll(getSnapshotQueue());
        return snapshot;
    }

    /* ================== Helpers ================== */

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
}