package cn.superiormc.ultimateshop.utils;

import cn.superiormc.ultimateshop.gui.GUIStatus;
import cn.superiormc.ultimateshop.managers.MenuStatusManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CommandUtil {

    private static final Map<UUID, SchedulerUtil> guiUpdateTask = new ConcurrentHashMap<>();

    public static void updateGUI(Player player) {
        if (player == null) {
            Bukkit.getOnlinePlayers().forEach(CommandUtil::updateGUI);
            return;
        }

        GUIStatus guiStatus = MenuStatusManager.menuStatusManager.getGUIStatus(player);
        if (guiStatus == null || guiStatus.getGUI() == null) {
            return;
        }

        UUID playerUUID = player.getUniqueId();
        SchedulerUtil task = guiUpdateTask.remove(playerUUID);
        if (task != null) {
            task.cancel();
        }
        guiUpdateTask.put(playerUUID,
                SchedulerUtil.runTaskLater(() -> {
                    GUIStatus currentStatus = MenuStatusManager.menuStatusManager.getGUIStatus(player);
                    if (currentStatus != null && currentStatus.getGUI() != null) {
                        currentStatus.getGUI().updateGUI();
                    }
                    guiUpdateTask.remove(playerUUID);
                }, 20L));
    }

    public static void cancelGUIUpdate(Player player) {
        SchedulerUtil task = guiUpdateTask.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }
}
