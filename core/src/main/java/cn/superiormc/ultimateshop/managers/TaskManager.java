package cn.superiormc.ultimateshop.managers;

import cn.superiormc.ultimateshop.UltimateShop;
import cn.superiormc.ultimateshop.objects.caches.ObjectCache;
import cn.superiormc.ultimateshop.utils.SchedulerUtil;
import cn.superiormc.ultimateshop.utils.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TaskManager {

    private static final long SAVE_STAGGER_TICKS = 5L;

    public static TaskManager taskManager;

    private SchedulerUtil saveTask;

    private SchedulerUtil sellChestTask;

    public TaskManager() {
        taskManager = this;
        if (ConfigManager.configManager.getBoolean("auto-save.enabled")) {
            initSaveTasks();
        }
        initSellChestTasks();
    }

    public void initSaveTasks() {
        saveTask = SchedulerUtil.runTaskTimer(() -> {
            if (!ConfigManager.configManager.getBoolean("auto-save.hide-message")) {
                TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §fAuto saving data...");
                TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §fIf this lead to server TPS drop, " +
                        "you should consider disable auto save feature at config.yml!");
            }
            List<ObjectCache> targets = new ArrayList<>();
            ObjectCache serverCache = CacheManager.cacheManager.serverCache;
            if (serverCache != null && !serverCache.canNotModify() && serverCache.isDirty()) {
                targets.add(serverCache);
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                ObjectCache cache = CacheManager.cacheManager.getObjectCache(player);
                if (cache != null && cache.isDirty()) {
                    targets.add(cache);
                }
            }
            // 借鉴 craft-engine：错峰分发保存任务，避免同一 tick 全员齐射造成 CPU/IO 尖峰；
            // 未发生变化的缓存会在 shutCache 内部跳过落盘。
            int index = 0;
            for (ObjectCache cache : targets) {
                long delayTicks = Math.max(1L, SAVE_STAGGER_TICKS * index++);
                SchedulerUtil.runTaskLaterAsynchronously(() -> cache.shutCache(false), delayTicks);
            }
        }, 180L, ConfigManager.configManager.config.getLong("auto-save.period-tick", 6000L));
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
        if (saveTask != null) {
            saveTask.cancel();
        }
        if (sellChestTask != null) {
            sellChestTask.cancel();
        }
    }
}
