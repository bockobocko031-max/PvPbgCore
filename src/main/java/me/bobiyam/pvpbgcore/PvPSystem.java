package me.bobiyam.pvpbgcore;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PvPSystem implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();

    public PvPSystem(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getConfig().getBoolean("pvp-system.enabled")) return;

        World from = event.getFrom().getWorld();
        World to = event.getTo().getWorld();
        if (from == null || to == null) return;

        String pvpWorld = plugin.getConfig().getString("pvp-system.worlds.pvp-world");
        String spawnWorld = plugin.getConfig().getString("pvp-system.worlds.spawn-world");

        // ➜ ВЛИЗА В PvP WORLD
        if (!from.getName().equalsIgnoreCase(pvpWorld)
                && to.getName().equalsIgnoreCase(pvpWorld)) {

            if (!savedInventories.containsKey(player.getUniqueId())) {
                savedInventories.put(player.getUniqueId(), player.getInventory().getContents());
            }

            player.getInventory().clear();
            player.updateInventory();

            send(player, "pvp-system.messages.inventory-cleared");
            return;
        }

        // ➜ ИЗЛИЗА ОТ PvP WORLD
        if (from.getName().equalsIgnoreCase(pvpWorld)
                && to.getName().equalsIgnoreCase(spawnWorld)) {

            if (savedInventories.containsKey(player.getUniqueId())) {
                player.getInventory().clear();
                player.getInventory().setContents(savedInventories.get(player.getUniqueId()));
                savedInventories.remove(player.getUniqueId());
                player.updateInventory();
            }

            send(player, "pvp-system.messages.inventory-restored");
        }
    }

    private void send(Player p, String path) {
        String msg = plugin.getConfig().getString(path);
        if (msg != null && !msg.isEmpty()) {
            p.sendMessage(msg.replace("&", "§"));
        }
    }
}