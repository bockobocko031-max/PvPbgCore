package me.bobiyam.pvpbgcore;

import me.bobiyam.pvpbgcore.Enchanting;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class CustomEnchantCommand implements CommandExecutor {

    private final Plugin plugin;

    public CustomEnchantCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { // Java 8 синтаксис
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        Player player = (Player) sender; // класически cast за Java 8
        Enchanting.attemptUpgrade(player, plugin);
        return true;
    }
}