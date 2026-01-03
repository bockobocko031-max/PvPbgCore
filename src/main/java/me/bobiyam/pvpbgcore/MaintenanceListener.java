package me.bobiyam.pvpbgcore;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class MaintenanceListener implements Listener {

    private final JavaPlugin plugin;

    public MaintenanceListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {

        if (!plugin.getConfig().getBoolean("maintenance.enabled")) return;

        if (e.getPlayer().hasPermission("maintenance.bypass")) return;

        String kickMessage = plugin.getConfig().getString("maintenance.kick-message");
        e.getPlayer().kickPlayer(ChatColor.translateAlternateColorCodes('&', kickMessage));
    }
}
