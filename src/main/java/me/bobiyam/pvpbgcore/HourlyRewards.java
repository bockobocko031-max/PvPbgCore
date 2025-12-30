package me.bobiyam.pvpbgcore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class HourlyRewards {

    private final PvPBGCore plugin;
    private BukkitRunnable task;

    public HourlyRewards(PvPBGCore plugin) {
        this.plugin = plugin;
        start();
    }

    public void reload() {
        start();
    }

    private void start() {
        if (task != null) task.cancel();

        FileConfiguration cfg = plugin.getConfig();

        if (!cfg.getBoolean("hourly-rewards.enabled")) return;

        long minutes = cfg.getLong("hourly-rewards.interval.minutes", 0);
        long hours = cfg.getLong("hourly-rewards.interval.hours", 0);

        long totalMinutes = minutes + (hours * 60);
        if (totalMinutes <= 0) return;

        long ticks = totalMinutes * 60 * 20;

        task = new BukkitRunnable() {
            @Override
            public void run() {

                String message = cfg.getString("hourly-rewards.message", "");

                // ✅ САМО ONLINE ИГРАЧИ
                for (Player player : Bukkit.getOnlinePlayers()) {

                    if (!message.isEmpty()) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
                    }

                    for (String cmd : cfg.getStringList("hourly-rewards.commands")) {
                        Bukkit.dispatchCommand(
                                Bukkit.getConsoleSender(),
                                cmd.replace("%player%", player.getName())
                        );
                    }
                }
            }
        };

        task.runTaskTimer(plugin, ticks, ticks);
    }
}