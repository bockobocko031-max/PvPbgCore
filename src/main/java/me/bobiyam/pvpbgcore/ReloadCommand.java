package me.bobiyam.pvpbgcore;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ReloadCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public ReloadCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        // Регистриране на командата в plugin.yml
        plugin.getCommand("pvpbgcore").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
            player.sendMessage(ChatColor.RED + "Usage: /pvpbgcore reload");
            return true;
        }

        if (!player.hasPermission("pvpbgcore.reload")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to reload the plugin!");
            return true;
        }

        // Викаме метода от основния клас
        ((PvPBGCore) plugin).reloadPluginConfig(player);

        return true;
    }
}