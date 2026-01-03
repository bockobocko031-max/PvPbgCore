package me.bobiyam.pvpbgcore;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.TimeUnit;

public class MaintenanceMotdListener implements Listener {

    private final JavaPlugin plugin;

    public MaintenanceMotdListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPing(ServerListPingEvent e) {

        boolean enabled = plugin.getConfig().getBoolean("maintenance.enabled");
        long expiresAt = plugin.getConfig().getLong("maintenance.expiresAt", 0L);
        plugin.getLogger().info("[MaintenanceMotdListener] onPing: maintenance.enabled=" + enabled + ", expiresAt=" + expiresAt + " (now=" + System.currentTimeMillis() + ")");

        if (!enabled) {
            e.setMotd(color(plugin.getConfig().getString("motd.normal")));
            return;
        }

        String motd = plugin.getConfig().getString("motd.maintenance");
        if (expiresAt > System.currentTimeMillis()) {
            long remaining = expiresAt - System.currentTimeMillis();
            motd = motd.replace("%remaining%", formatDuration(remaining));
        } else if (expiresAt == 0L) {
            // manual maintenance, no expiry set
            motd = motd.replace("%remaining%", "manual");
        } else {
            // expired or 0
            motd = motd.replace("%remaining%", "");
        }

        e.setMotd(color(motd));
        e.setMaxPlayers(0); // визуално "затворен"
    }

    private String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    private String formatDuration(long ms) {
        if (ms <= 0) return "0s";
        long days = TimeUnit.MILLISECONDS.toDays(ms);
        ms -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(ms);
        ms -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(ms);
        ms -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(ms);
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d");
        if (hours > 0) sb.append(hours).append("h");
        if (minutes > 0) sb.append(minutes).append("m");
        if (seconds > 0) sb.append(seconds).append("s");
        return sb.toString();
    }
}
