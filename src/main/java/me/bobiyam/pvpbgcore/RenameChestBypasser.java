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
    // ULTRA-SAFE COMMAND BLOCKER
    // ==========================
    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();

        String msg = e.getMessage().toLowerCase().trim();
        String command = msg.startsWith("/") ? msg.substring(1) : msg;

        List<String> blocked =
                plugin.getConfig().getStringList("command-blocker.blocked-commands");

        for (String cmd : blocked) {
            if (command.equalsIgnoreCase(cmd.toLowerCase())) {

                // BLOCK FOR EVERYONE
                e.setCancelled(true);

                // ALERT STAFF (even bypass)
                alertStaff(p, "/" + command);

                // KICK PLAYER
                kickPlayer(p);
                return;
            }
        }
    }

    // ==========================
    // KICK PLAYER
    // ==========================
    private void kickPlayer(Player p) {
        String msg = plugin.getConfig()
                .getString("command-blocker.messages.kick-command",
                        "&cYou are not allowed to run this command!");

        p.kickPlayer(ChatColor.translateAlternateColorCodes('&', msg));
    }

    // ==========================
    // STAFF / BYPASS ALERT
    // ==========================
    private void alertStaff(Player offender, String command) {
        String alert = ChatColor.translateAlternateColorCodes('&',
                "&8[&4SECURITY&8] &c" + offender.getName()
                        + " &7tried blocked command: &6" + command);

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("pvpbgcore.staff")
                    || p.hasPermission("pvpbgcore.bypass")) {
                p.sendMessage(alert);
            }
        }

        // Console alert
        Bukkit.getConsoleSender().sendMessage(alert);
    }
}
