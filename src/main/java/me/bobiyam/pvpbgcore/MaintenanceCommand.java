package me.bobiyam.pvpbgcore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class MaintenanceCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public MaintenanceCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("maintenance").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!sender.hasPermission("maintenance.toggle")) {
            sender.sendMessage(color("&cYou don't have permission."));
            return true;
        }

        boolean enabled = plugin.getConfig().getBoolean("maintenance.enabled");

        if (enabled) {
            plugin.getConfig().set("maintenance.enabled", false);
            plugin.saveConfig();
            sender.sendMessage(color("&2• &aMaintenance has been disabled!"));
        } else {
            plugin.getConfig().set("maintenance.enabled", true);
            plugin.saveConfig();
            sender.sendMessage(color("&2• &aMaintenance has been activated!"));

            // 🔥 AUTO-KICK ALL ONLINE PLAYERS
            String kickMsg = color(plugin.getConfig().getString("maintenance.kick-message"));
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.hasPermission("maintenance.bypass")) {
                    p.kickPlayer(kickMsg);
                }
            }
        }

        return true;
    }

    private String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
