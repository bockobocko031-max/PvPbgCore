package me.bobiyam.pvpbgcore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class ScreenshareManager implements Listener, TabExecutor {

    private final JavaPlugin plugin;
    private Location ssRoom;
    private Location spawnLocation;
    private final Map<UUID, UUID> privateChat = new HashMap<>();
    private final Set<UUID> beingSSed = new HashSet<>();
    private final String prefix;

    public ScreenshareManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.prefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("ss.prefix", "&6[SS] &f"));
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        plugin.getCommand("setSSROOM").setExecutor(this);
        plugin.getCommand("setssspawn").setExecutor(this);
        plugin.getCommand("ss").setExecutor(this);
        plugin.getCommand("unss").setExecutor(this);

        loadLocations();
    }

    private void loadLocations() {
        if (plugin.getConfig().contains("ssRoom.world")) {
            String world = plugin.getConfig().getString("ssRoom.world");
            double x = plugin.getConfig().getDouble("ssRoom.x");
            double y = plugin.getConfig().getDouble("ssRoom.y");
            double z = plugin.getConfig().getDouble("ssRoom.z");
            float yaw = (float) plugin.getConfig().getDouble("ssRoom.yaw", 0);
            float pitch = (float) plugin.getConfig().getDouble("ssRoom.pitch", 0);
            if (Bukkit.getWorld(world) != null) {
                ssRoom = new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
            }
        }
        if (plugin.getConfig().contains("spawn.world")) {
            String world = plugin.getConfig().getString("spawn.world");
            double x = plugin.getConfig().getDouble("spawn.x");
            double y = plugin.getConfig().getDouble("spawn.y");
            double z = plugin.getConfig().getDouble("spawn.z");
            float yaw = (float) plugin.getConfig().getDouble("spawn.yaw", 0);
            float pitch = (float) plugin.getConfig().getDouble("spawn.pitch", 0);
            if (Bukkit.getWorld(world) != null) {
                spawnLocation = new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
            }
        }
    }

    private void saveSSRoom(Location loc) {
        plugin.getConfig().set("ssRoom.world", loc.getWorld().getName());
        plugin.getConfig().set("ssRoom.x", loc.getX());
        plugin.getConfig().set("ssRoom.y", loc.getY());
        plugin.getConfig().set("ssRoom.z", loc.getZ());
        plugin.getConfig().set("ssRoom.yaw", loc.getYaw());
        plugin.getConfig().set("ssRoom.pitch", loc.getPitch());
        plugin.saveConfig();
        ssRoom = loc;
    }

    private void saveSpawn(Location loc) {
        plugin.getConfig().set("spawn.world", loc.getWorld().getName());
        plugin.getConfig().set("spawn.x", loc.getX());
        plugin.getConfig().set("spawn.y", loc.getY());
        plugin.getConfig().set("spawn.z", loc.getZ());
        plugin.getConfig().set("spawn.yaw", loc.getYaw());
        plugin.getConfig().set("spawn.pitch", loc.getPitch());
        plugin.saveConfig();
        spawnLocation = loc;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        switch (cmd.getName().toLowerCase()) {
            case "setssroom":
                if (!player.hasPermission("ss.setSS")) return true;
                saveSSRoom(player.getLocation());
                player.sendMessage(prefix + ChatColor.GREEN + " SS room set.");
                return true;
            case "setssspawn":
                if (!player.hasPermission("ss.setspawn")) return true;
                saveSpawn(player.getLocation());
                player.sendMessage(prefix + ChatColor.GREEN + " Spawn set.");
                return true;
            case "ss":
                if (!player.hasPermission("ss.screanshare")) return true;
                if (args.length < 1) {
                    player.sendMessage(prefix + ChatColor.RED + " Specify a player to SS.");
                    return true;
                }
                if (ssRoom == null) {
                    player.sendMessage(prefix + ChatColor.RED + " SS Room is not set!");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[0]);
                if (target == null) {
                    player.sendMessage(prefix + ChatColor.RED + " Player not found.");
                    return true;
                }

                // Телепортиране
                target.teleport(ssRoom);
                player.teleport(ssRoom);

                beingSSed.add(target.getUniqueId());
                privateChat.put(player.getUniqueId(), target.getUniqueId());
                privateChat.put(target.getUniqueId(), player.getUniqueId());

                player.sendMessage(prefix + ChatColor.GREEN + " You are screensharing " + target.getName());
                target.sendMessage(prefix + ChatColor.RED + " You are being screenshared! Only SS chat is visible.");

                // 1.8.9 Title/Subtitle
                target.sendTitle(ChatColor.RED + "You are being", ChatColor.RED + "SSed! Do not logout!");

                // ✅ Broadcast to global chat in English
                Bukkit.broadcastMessage(ChatColor.GRAY + "[SS] " + ChatColor.AQUA + target.getName() +
                        ChatColor.GRAY + " is being screenshared by " + ChatColor.YELLOW + player.getName());

                return true;
            case "unss":
                if (!player.hasPermission("ss.stopss")) return true;
                if (args.length < 1) return true;
                Player unssTarget = Bukkit.getPlayerExact(args[0]);
                if (unssTarget == null) return true;

                if (spawnLocation != null) {
                    unssTarget.teleport(spawnLocation);
                    player.teleport(spawnLocation);
                }

                beingSSed.remove(unssTarget.getUniqueId());
                privateChat.remove(player.getUniqueId());
                privateChat.remove(unssTarget.getUniqueId());

                player.sendMessage(prefix + ChatColor.GREEN + " Stopped SS for " + unssTarget.getName());
                unssTarget.sendMessage(prefix + ChatColor.GREEN + " You are no longer being SSed.");
                return true;
        }
        return false;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        if (beingSSed.contains(sender.getUniqueId())) {
            UUID targetUUID = privateChat.get(sender.getUniqueId());
            Player target = Bukkit.getPlayer(targetUUID);
            if (target != null) {
                event.setCancelled(true);
                String msg = ChatColor.GRAY + "[SS] " + ChatColor.AQUA + sender.getName() + ": " + ChatColor.WHITE + event.getMessage();
                sender.sendMessage(msg);
                target.sendMessage(msg);
            } else {
                beingSSed.remove(sender.getUniqueId());
                privateChat.remove(sender.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (beingSSed.contains(player.getUniqueId())) {
            String cmd = event.getMessage().split(" ")[0].toLowerCase();
            if (cmd.equals("/discord") || cmd.equals("/spawn")) return;
            event.setCancelled(true);
            player.sendMessage(prefix + ChatColor.RED + " You cannot use commands while SSed!");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        return Collections.emptyList();
    }
}