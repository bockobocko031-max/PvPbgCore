package me.bobiyam.pvpbgcore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

public class EnchantingCommand implements CommandExecutor {

    private final Plugin plugin;

    public EnchantingCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Само играч може да използва тази команда.");
            return true;
        }

        Player player = (Player) sender;
        Location loc = player.getLocation();

        Villager villager = (Villager) loc.getWorld().spawnEntity(loc, EntityType.VILLAGER);

        String name = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("enchanting.villager-name", "&aEnchanting NPC"));

        villager.setCustomName(name);
        villager.setCustomNameVisible(true);

        // Маркираме NPC-то
        villager.setMetadata("enchant_villager",
                new FixedMetadataValue(plugin, true));

        // 🔒 FREEZE – не се мърда
        Location freezeLoc = villager.getLocation().clone();
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!villager.isValid()) return;
            villager.teleport(freezeLoc);
        }, 1L, 1L);

        player.sendMessage(ChatColor.GREEN + "Enchanting NPC създаден.");
        return true;
    }
}