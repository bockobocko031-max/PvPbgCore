package me.bobiyam.pvpbgcore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.List;

public class AntiSwearManager implements Listener {

    private final PvPBGCore plugin;
    private FileConfiguration config;
    private List<String> blockedWords;
    private String playerMessage;
    private String staffAlert;

    public AntiSwearManager(PvPBGCore plugin) {
        this.plugin = plugin;
        reloadConfig();
    }

    /**
     * Load config values
     */
    public void reloadConfig() {
        this.config = plugin.getConfig();
        this.blockedWords = config.getStringList("anti-swear.blocked-words");
        this.playerMessage = ChatColor.translateAlternateColorCodes('&',
                config.getString("anti-swear.player-message",
                        "&cWriting inappropriate words is forbidden!"));
        this.staffAlert = ChatColor.translateAlternateColorCodes('&',
                config.getString("anti-swear.staff-alert",
                        "&4[AntiSwear] &c%player% wrote: &f%message%"));
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage().toLowerCase();

        boolean containsBlocked = blockedWords.stream()
                .anyMatch(word -> message.contains(word.toLowerCase()));

        if (containsBlocked) {
            event.setCancelled(true);

            // Warning to player
            player.sendMessage(playerMessage);

            // Notify staff
            String alert = staffAlert.replace("%player%", player.getName())
                    .replace("%message%", event.getMessage());

            Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission("pvpbgcore.staff"))
                    .forEach(p -> p.sendMessage(alert));
        }
    }
}
