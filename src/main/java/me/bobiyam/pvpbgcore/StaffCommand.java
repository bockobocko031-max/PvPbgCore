package me.bobiyam.pvpbgcore;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class StaffCommand implements CommandExecutor {

    public static Map<UUID, GameMode> staffModes = new HashMap<>();
    public static Map<UUID, ItemStack[]> savedInventory = new HashMap<>();
    public static Map<UUID, ItemStack[]> savedArmor = new HashMap<>();

    private final String prefix = "§7[§bStaff§7] ";

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (!player.hasPermission("pvpbgcore.staffmode")) {
            player.sendMessage(prefix + "§cNo permission!");
            return true;
        }

        // ENABLE
        if (!staffModes.containsKey(player.getUniqueId())) {

            staffModes.put(player.getUniqueId(), player.getGameMode());
            savedInventory.put(player.getUniqueId(), player.getInventory().getContents());
            savedArmor.put(player.getUniqueId(), player.getInventory().getArmorContents());

            player.getInventory().clear();
            player.setGameMode(GameMode.CREATIVE);
            player.setAllowFlight(true);
            player.setFlying(true);

            // Vanish
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.equals(player)) {
                    online.hidePlayer(player);
                }
            }

            // Inspect
            player.getInventory().setItem(0, createItem(Material.COMPASS, "§aInspect Player"));

            // Freeze
            player.getInventory().setItem(1, createItem(Material.ICE, "§bFreeze Player"));

            // Random TP
            player.getInventory().setItem(4, createItem(Material.NETHER_STAR, "§dRandom Teleport"));

            player.sendMessage(prefix + "§aStaffMode ENABLED");
            return true;
        }

        // DISABLE
        player.setGameMode(staffModes.remove(player.getUniqueId()));
        player.getInventory().setContents(savedInventory.remove(player.getUniqueId()));
        player.getInventory().setArmorContents(savedArmor.remove(player.getUniqueId()));

        player.setAllowFlight(false);
        player.setFlying(false);

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(player);
        }

        player.sendMessage(prefix + "§cStaffMode DISABLED");
        return true;
    }

    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
}