package me.bobiyam.pvpbgcore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ReportCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public ReportCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        // Регистрираме командата
        plugin.getCommand("report").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player reporter = (Player) sender;

        // Проверка за аргументи
        if (args.length < 2) {
            reporter.sendMessage(color(plugin.getConfig().getString("report.messages.usage")));
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName); // може да е offline, все пак логваме името

        // Създаваме reason от всички аргументи след името
        StringBuilder reasonBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            reasonBuilder.append(args[i]).append(" ");
        }
        String reason = reasonBuilder.toString().trim();

        // Feedback към репортера
        reporter.sendMessage(color(plugin.getConfig().getString("report.messages.reported")
                .replace("%player%", targetName)
                .replace("%reason%", reason)));

        // Alert към staff
        String alert = color(plugin.getConfig().getString("report.messages.staff-alert")
                .replace("%reporter%", reporter.getName())
                .replace("%player%", targetName)
                .replace("%reason%", reason));

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("pvpbgcore.staff")) {
                p.sendMessage(alert);
            }
        }

        // Лог в конзолата
        plugin.getLogger().info("[REPORT] " + reporter.getName() + " reported " + targetName + " for: " + reason);

        return true;
    }

    // Цветове от & към §
    private String color(String msg) {
        if (msg == null) return "";
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
