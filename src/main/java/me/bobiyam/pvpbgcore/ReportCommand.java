package me.bobiyam.pvpbgcore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class ReportCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public ReportCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("report").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player reporter = (Player) sender;

        // /report <player> <reason>
        if (args.length < 2) {
            reporter.sendMessage(color(plugin.getConfig().getString("report.messages.usage")));
            return true;
        }

        String targetName = args[0];

        // reason от всички думи след името
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            sb.append(args[i]).append(" ");
        }
        String reason = sb.toString().trim();

        // ===== Message to reporter =====
        reporter.sendMessage(color(
                plugin.getConfig().getString("report.messages.reported")
                        .replace("%player%", targetName)
                        .replace("%reason%", reason)
        ));

        // ===== STAFF ALERT (ASCII LIST) =====
        if (!plugin.getConfig().isList("report.messages.staff-alert")) {
            plugin.getLogger().warning("report.messages.staff-alert is not a list!");
            return true;
        }

        List<String> staffAlert =
                plugin.getConfig().getStringList("report.messages.staff-alert");

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("pvpbgcore.staff")) {
                for (String line : staffAlert) {
                    staff.sendMessage(color(
                            line.replace("%reporter%", reporter.getName())
                                    .replace("%player%", targetName)
                                    .replace("%reason%", reason)
                    ));
                }
            }
        }

        // ===== Console log =====
        Bukkit.getConsoleSender().sendMessage(
                color("&c[REPORT] &7" + reporter.getName()
                        + " -> " + targetName
                        + " | Reason: " + reason)
        );

        return true;
    }

    private String color(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
