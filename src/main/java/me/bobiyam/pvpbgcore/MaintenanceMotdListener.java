package me.bobiyam.pvpbgcore;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class MaintenanceMotdListener implements Listener {

    private final JavaPlugin plugin;

    public MaintenanceMotdListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPing(ServerListPingEvent e) {

        if (!plugin.getConfig().getBoolean("maintenance.enabled")) {
            e.setMotd(color(plugin.getConfig().getString("motd.normal")));
            return;
        }

        e.setMotd(color(plugin.getConfig().getString("motd.maintenance")));
        e.setMaxPlayers(0); // визуално "затворен"
    }

    private String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
