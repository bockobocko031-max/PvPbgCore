package me.bobiyam.pvpbgcore;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class EventManager {

    private final JavaPlugin plugin;
    private BukkitRunnable runnable;
    private boolean isRunning = false;

    public EventManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void startEvent(int duration) {
        if (isRunning) return;

        isRunning = true;

        runnable = new BukkitRunnable() {
            int seconds = duration;

            @Override
            public void run() {
                if (seconds <= 0) {
                    broadcastTitle("§aСъбитието започна!", "");
                    stopEvent();
                    cancel();
                    return;
                }

                String color = "§e"; // жълт
                if (seconds <= 5) color = "§c"; // червен за последните 5 секунди
                broadcastTitle("§6Събитие", color + seconds + " секунди до старт");
                seconds--;
            }
        };

        runnable.runTaskTimer(plugin, 0L, 20L); // 1 секунда = 20 ticks
    }

    public void stopEvent() {
        if (!isRunning) return;

        if (runnable != null) runnable.cancel();
        isRunning = false;
        broadcastTitle("§cСъбитието спря!", "");
    }

    private void broadcastTitle(String title, String subtitle) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(title, subtitle);
        }
    }
}