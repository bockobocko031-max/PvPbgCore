package me.bobiyam.pvpbgcore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class RenameChestBypasser implements Listener {

    private final JavaPlugin plugin;

    public RenameChestBypasser(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // ==========================
    // COMMAND BLOCKER ONLY
    // ==========================
    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        String msg = e.getMessage().toLowerCase().trim();

        // remove "/" to get just the command
        String command = msg.startsWith("/") ? msg.substring(1) : msg;

        List<String> blocked = plugin.getConfig().getStringList("command-blocker.blocked-commands");

        for (String cmd : blocked) {
            if (command.equalsIgnoreCase(cmd)) { // exact match
                e.setCancelled(true); // cancel the command
                kickPlayer(p);
                alertStaff(p, "/" + command);
                return;
            }
        }
    }

    // ==========================
    // KICK PLAYER
    // ==========================
    private void kickPlayer(Player p) {
        String msg = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("command-blocker.messages.kick-command",
                        "&cYou are not allowed to run this command!"));
        p.kickPlayer(msg);
    }

    // ==========================
    // STAFF ALERT
    // ==========================
    private void alertStaff(Player p, String command) {
        String alertMsg = ChatColor.translateAlternateColorCodes('&',
                "&c[ALERT] &6" + p.getName() + " &ctried forbidden command: &6" + command);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("pvpbgcore.staff")) {
                online.sendMessage(alertMsg);
            }
        }
    }
}
