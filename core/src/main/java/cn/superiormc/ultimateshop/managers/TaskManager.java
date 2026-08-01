package cn.superiormc.ultimateshop.managers;

import cn.superiormc.ultimateshop.UltimateShop;
import cn.superiormc.ultimateshop.objects.caches.ObjectCache;
import cn.superiormc.ultimateshop.utils.SchedulerUtil;
import cn.superiormc.ultimateshop.utils.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Queue;
import java.util.Set;

public class TaskManager {

    private static final long SAVE_STAGGER_TICKS = 5L;

    public static TaskManager taskManager;

    private final Object saveQueueLock = new Object();

    private final Queue<ObjectCache> saveQueue = new ArrayDeque<>();

    private final Set<ObjectCache> queuedCaches = Collections.newSetFromMap(new IdentityHashMap<>());

    private boolean acceptingSaveRequests = true;

    private SchedulerUtil saveTask;

    private SchedulerUtil saveDispatchTask;

    private SchedulerUtil sellChestTask;

    public TaskManager() {
        taskManager = this;
        if (ConfigManager.configManager.getBoolean("auto-save.enabled")) {
            initSaveTasks();
        }
        initSellChestTasks();
    }

    public void initSaveTasks() {
        synchronized (saveQueueLock) {
            acceptingSaveRequests = true;
        }
        saveTask = SchedulerUtil.runTaskTimer(this::queueDirtyCaches,
                180L,
                ConfigManager.configManager.config.getLong("auto-save.period-tick", 6000L));
        saveDispatchTask = SchedulerUtil.runTaskTimerAsynchronously(
                this::dispatchNextSave,
                1L,
                SAVE_STAGGER_TICKS
        );
    }

    private void queueDirtyCaches() {
        int queuedCount = 0;
        ObjectCache serverCache = CacheManager.cacheManager.serverCache;
        if (enqueueIfDirty(serverCache)) {
            queuedCount++;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (enqueueIfDirty(CacheManager.cacheManager.getObjectCache(player))) {
                queuedCount++;
            }
        }
        if (queuedCount > 0 && !ConfigManager.configManager.getBoolean("auto-save.hide-message")) {
            TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §fAuto saving data...");
            TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §fIf this lead to server TPS drop, " +
                    "you should consider disable auto save feature at config.yml!");
        }
    }

    private boolean enqueueIfDirty(ObjectCache cache) {
        if (cache == null || cache.canNotModify() || !cache.isDirty()) {
            return false;
        }
        synchronized (saveQueueLock) {
            if (!acceptingSaveRequests || !queuedCaches.add(cache)) {
                return false;
            }
            saveQueue.offer(cache);
            return true;
        }
    }

    private void dispatchNextSave() {
        ObjectCache cache;
        synchronized (saveQueueLock) {
            if (!acceptingSaveRequests) {
                return;
            }
            cache = saveQueue.poll();
            if (cache != null) {
                queuedCaches.remove(cache);
            }
        }
        if (cache != null && !cache.canNotModify() && cache.isDirty()) {
            cache.shutCache(false);
        }
    }

    public void initSellChestTasks() {
        if (SellChestManager.sellChestManager != null &&
                !ConfigManager.configManager.getSellChests().isEmpty() &&
                !UltimateShop.isFolia) {
            sellChestTask = SchedulerUtil.runTaskTimer(
                    () -> SellChestManager.sellChestManager.tick(),
                    20L,
                    ConfigManager.configManager.getLong("sell.sell-chest.period-ticks", 60L)
            );
            for (World world : Bukkit.getWorlds()) {
                for (Chunk chunk : world.getLoadedChunks()) {
                    SellChestManager.sellChestManager.handleChunkLoad(chunk);
                }
            }
        }
    }

    public void cancelTask() {
        synchronized (saveQueueLock) {
            acceptingSaveRequests = false;
            saveQueue.clear();
            queuedCaches.clear();
        }
        if (saveTask != null) {
            saveTask.cancel();
            saveTask = null;
        }
        if (saveDispatchTask != null) {
            saveDispatchTask.cancel();
            saveDispatchTask = null;
        }
        if (sellChestTask != null) {
            sellChestTask.cancel();
            sellChestTask = null;
        }
    }
}
