package me.bobiyam.pvpbgcore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class StaffChatCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final String prefix;

    public StaffChatCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        this.prefix = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("staffchat.prefix", "&6[StaffChat] &f"));
        plugin.getCommand("staffchat").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("pvpbgcore.staffchat")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use StaffChat!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /staffchat <message>");
            return true;
        }

        String message = String.join(" ", args);

        String formatted = prefix + ChatColor.YELLOW + player.getName() + ": " + ChatColor.WHITE + message;

        // Изпращаме съобщението само на онлайн staff
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("pvpbgcore.staffchat")) {
                online.sendMessage(formatted);
            }
        }

        // Съобщението не се изпраща на нормални играчи
        return true;
    }
}