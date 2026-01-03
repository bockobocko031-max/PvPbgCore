package me.bobiyam.pvpbgcore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

public class HackWatcher implements Listener {

    private final JavaPlugin plugin;

    private final List<String> blockedBrands = Arrays.asList(
            "huzuni",
            "doomsday",
            "wurst",
            "sigma",
            "liquidbounce",
            "impact",
            "aristois"
    );

    public HackWatcher(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // ==========================
    // BRAND CHANNEL DETECTION
    // ==========================
    @EventHandler
    public void onBrandRegister(PlayerRegisterChannelEvent e) {
        Player p = e.getPlayer();
        String channel = e.getChannel().toLowerCase();

        for (String bad : blockedBrands) {
            if (channel.contains(bad)) {
                kick(p, "&cHacked client detected.");
                alertStaff(p, channel);
                return;
            }
        }
    }

    // ==========================
    // BASIC JOIN SAFETY
    // ==========================
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        p.sendMessage(ChatColor.GRAY + "Client check initialized.");
    }

    // ==========================
    // KICK
    // ==========================
    private void kick(Player p, String msg) {
        p.kickPlayer(ChatColor.translateAlternateColorCodes('&', msg));
    }

    // ==========================
    // STAFF ALERT
    // ==========================
    private void alertStaff(Player p, String brand) {
        String alert = ChatColor.translateAlternateColorCodes('&',
                "&4[SECURITY] &c" + p.getName() + " &7detected with client brand: &c" + brand);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("pvpbgcore.staff")) {
                online.sendMessage(alert);
            }
        }

        plugin.getLogger().warning("[HackWatcher] " + p.getName() + " brand=" + brand);
    }
}
