package me.bobiyam.pvpbgcore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.TimeUnit;

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

        // no args -> toggle
        if (args.length == 0) {
            boolean enabled = plugin.getConfig().getBoolean("maintenance.enabled");

            if (enabled) {
                plugin.getConfig().set("maintenance.enabled", false);
                plugin.getConfig().set("maintenance.expiresAt", 0L);
                plugin.saveConfig();
                // cancel scheduled task if plugin provides it
                if (plugin instanceof PvPBGCore) ((PvPBGCore) plugin).cancelMaintenanceTask();
                sender.sendMessage(color("&2• &aMaintenance has been disabled!"));
                Bukkit.broadcastMessage(color("&6[Maintenance] &aMaintenance has been disabled by &f" + sender.getName()));
                // MOTD will be shown normally by MaintenanceMotdListener (reads maintenance.enabled)
            } else {
                plugin.getConfig().set("maintenance.enabled", true);
                plugin.getConfig().set("maintenance.expiresAt", 0L);
                plugin.saveConfig();
                sender.sendMessage(color("&2• &aMaintenance has been activated!"));

                // MOTD will be shown as maintenance by MaintenanceMotdListener (reads maintenance.enabled)

                // 🔥 AUTO-KICK ALL ONLINE PLAYERS
                String kickMsg = color(plugin.getConfig().getString("maintenance.kick-message"));
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.hasPermission("maintenance.bypass")) {
                        p.kickPlayer(kickMsg);
                    }
                }
                Bukkit.broadcastMessage(color("&6[Maintenance] &aMaintenance has been enabled by &f" + sender.getName()));
            }

            return true;
        }

        // args present -> expecting duration, 'off' or 'status'
        String param = args[0].toLowerCase();
        if (param.equals("off") || param.equals("cancel")) {
            plugin.getConfig().set("maintenance.enabled", false);
            plugin.getConfig().set("maintenance.expiresAt", 0L);
            plugin.saveConfig();
            if (plugin instanceof PvPBGCore) ((PvPBGCore) plugin).cancelMaintenanceTask();
            sender.sendMessage(color("&2• &aMaintenance cancelled."));
            Bukkit.broadcastMessage(color("&6[Maintenance] &aMaintenance has been cancelled by &f" + sender.getName()));
            // MOTD will be shown normally by MaintenanceMotdListener (reads maintenance.enabled)
            return true;
        }

        if (param.equals("status")) {
            boolean enabled = plugin.getConfig().getBoolean("maintenance.enabled", false);
            long expiresAt = plugin.getConfig().getLong("maintenance.expiresAt", 0L);
            if (!enabled) {
                sender.sendMessage(color("&6[Maintenance] &cMaintenance is not active."));
            } else if (expiresAt <= 0) {
                sender.sendMessage(color("&6[Maintenance] &aMaintenance is active (manual)."));
            } else {
                long remaining = expiresAt - System.currentTimeMillis();
                if (remaining <= 0) {
                    sender.sendMessage(color("&6[Maintenance] &cMaintenance expiry reached (pending cleanup)."));
                } else {
                    sender.sendMessage(color("&6[Maintenance] &aActive for: &f" + formatDuration(remaining) + " &7(ends: &f" + new java.util.Date(expiresAt) + "&7)"));
                }
            }
            return true;
        }

        // parse duration like 1h 30m 2d 15s
        long durationMillis = parseDuration(param);
        if (durationMillis <= 0) {
            sender.sendMessage(color("&cInvalid duration. Use examples: 30m, 1h, 2d, 15s"));
            return true;
        }

        long expiresAt = System.currentTimeMillis() + durationMillis;
        plugin.getConfig().set("maintenance.enabled", true);
        plugin.getConfig().set("maintenance.expiresAt", expiresAt);
        plugin.saveConfig();

        // schedule disable in main plugin
        if (plugin instanceof PvPBGCore) {
            ((PvPBGCore) plugin).scheduleMaintenanceDisable(expiresAt);
        }

        plugin.getLogger().info("[MaintenanceCommand] scheduled maintenance expiresAt=" + expiresAt + " (now=" + System.currentTimeMillis() + ")");

        // MOTD will be shown as maintenance by MaintenanceMotdListener (reads maintenance.expiresAt)

        sender.sendMessage(color("&2• &aMaintenance activated for &f" + param));
        Bukkit.broadcastMessage(color("&6[Maintenance] &aMaintenance activated for &f" + formatDuration(durationMillis) + " &7by &f" + sender.getName()));

        // AUTO-KICK
        String kickMsg = color(plugin.getConfig().getString("maintenance.kick-message"));
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.hasPermission("maintenance.bypass")) {
                p.kickPlayer(kickMsg);
            }
        }

        return true;
    }

    private long parseDuration(String s) {
        // supports combinations like 1h30m or single value like 1h
        long total = 0L;
        StringBuilder number = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isDigit(c)) {
                number.append(c);
                continue;
            }
            if (number.length() == 0) return -1;
            int val;
            try { val = Integer.parseInt(number.toString()); } catch (NumberFormatException ex) { return -1; }
            number.setLength(0);
            switch (c) {
                case 'd': total += val * 24L * 60L * 60L * 1000L; break;
                case 'h': total += val * 60L * 60L * 1000L; break;
                case 'm': total += val * 60L * 1000L; break;
                case 's': total += val * 1000L; break;
                default: return -1;
            }
        }
        if (number.length() > 0) return -1;
        return total;
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

    private String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
