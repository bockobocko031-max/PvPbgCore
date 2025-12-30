package me.bobiyam.pvpbgcore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpawnManager implements CommandExecutor, Listener {

    private final JavaPlugin plugin;
    private Location spawnLocation;
    private final Map<UUID, Long> spawnCooldowns = new HashMap<>();
    private final Map<UUID, Location> teleportStart = new HashMap<>();

    // Separate file to persist spawn across restarts
    private final File spawnFile;
    private FileConfiguration spawnCfg;

    public SpawnManager(JavaPlugin plugin) {
        this.plugin = plugin;

        spawnFile = new File(plugin.getDataFolder(), "spawns.yml");
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        if (!spawnFile.exists()) {
            try {
                spawnFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create spawns.yml: " + e.getMessage());
            }
        }
        spawnCfg = YamlConfiguration.loadConfiguration(spawnFile);

        plugin.getCommand("setspawn").setExecutor(this);
        plugin.getCommand("spawn").setExecutor(this);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Delay loading spawn to ensure worlds are registered/loaded on server start
        Bukkit.getScheduler().runTaskLater(plugin, this::loadSpawn, 20L);
    }

    // Load spawn from spawns.yml
    private void loadSpawn() {
        if (spawnCfg.contains("spawn.world")) {
            String world = spawnCfg.getString("spawn.world");
            double x = spawnCfg.getDouble("spawn.x");
            double y = spawnCfg.getDouble("spawn.y");
            double z = spawnCfg.getDouble("spawn.z");
            float yaw = (float) spawnCfg.getDouble("spawn.yaw", 0);
            float pitch = (float) spawnCfg.getDouble("spawn.pitch", 0);
            if (Bukkit.getWorld(world) != null) {
                spawnLocation = new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
                plugin.getLogger().info("Loaded spawn from spawns.yml (" + world + ").");
            } else {
                plugin.getLogger().warning("Spawn world '" + world + "' not loaded yet or does not exist.");
            }
        } else {
            plugin.getLogger().info("No spawn set in spawns.yml.");
        }
    }

    // Save spawn to spawns.yml
    private void saveSpawn(Location loc) {
        spawnCfg.set("spawn.world", loc.getWorld().getName());
        spawnCfg.set("spawn.x", loc.getX());
        spawnCfg.set("spawn.y", loc.getY());
        spawnCfg.set("spawn.z", loc.getZ());
        spawnCfg.set("spawn.yaw", loc.getYaw());
        spawnCfg.set("spawn.pitch", loc.getPitch());
        try {
            spawnCfg.save(spawnFile);
            spawnLocation = loc;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save spawn to spawns.yml: " + e.getMessage());
        }
    }

    private String prefix() {
        String raw = plugin.getConfig().getString("messages.prefix", "&7");
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(prefix() + ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        FileConfiguration cfg = plugin.getConfig();
        int cooldown = cfg.getInt("spawn.cooldown-seconds", 5);
        int delaySeconds = cfg.getInt("spawn.delay-seconds", 5);

        if (cmd.getName().equalsIgnoreCase("setspawn")) {
            if (!player.hasPermission("pvpbgcore.setspawn")) {
                player.sendMessage(prefix() + ChatColor.RED + "You don't have permission to use this command!");
                return true;
            }
            saveSpawn(player.getLocation());
            player.sendMessage(prefix() + ChatColor.GREEN + "Spawn location set!");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("spawn")) {
            if (spawnLocation == null) {
                player.sendMessage(prefix() + ChatColor.RED + "Spawn is not set yet.");
                return true;
            }

            UUID uuid = player.getUniqueId();
            long currentTime = System.currentTimeMillis();

            // Проверка за cooldown
            if (spawnCooldowns.containsKey(uuid)) {
                long lastUsed = spawnCooldowns.get(uuid);
                if ((currentTime - lastUsed) < cooldown * 1000L) {
                    long timeLeft = (cooldown * 1000L - (currentTime - lastUsed)) / 1000L;
                    player.sendMessage(prefix() + ChatColor.RED + "You must wait " + timeLeft + " more seconds before using /spawn again.");
                    return true;
                }
            }

            // Започваме teleport с delay
            player.sendMessage(prefix() + ChatColor.GREEN + "Teleporting in " + delaySeconds + " seconds. Don't move!");
            teleportStart.put(uuid, player.getLocation());

            new BukkitRunnable() {
                int secondsLeft = delaySeconds;

                @Override
                public void run() {
                    if (!player.isOnline()) {
                        teleportStart.remove(uuid);
                        cancel();
                        return;
                    }

                    // Проверка дали играчът се е преместил
                    Location start = teleportStart.get(uuid);
                    if (start == null) {
                        cancel();
                        return;
                    }
                    if (!player.getLocation().getBlock().equals(start.getBlock())) {
                        player.sendMessage(prefix() + ChatColor.RED + "Teleport cancelled because you moved!");
                        teleportStart.remove(uuid);
                        cancel();
                        return;
                    }

                    secondsLeft--;
                    if (secondsLeft <= 0) {
                        player.teleport(spawnLocation);
                        player.sendMessage(prefix() + ChatColor.GREEN + "Teleported to spawn!");
                        spawnCooldowns.put(uuid, System.currentTimeMillis());
                        teleportStart.remove(uuid);
                        cancel();
                    } else {
                        player.sendMessage(prefix() + ChatColor.YELLOW + "Teleporting in " + secondsLeft + "...");
                    }
                }
            }.runTaskTimer(plugin, 20, 20); // 20 ticks = 1 second
        }

        return true;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPlayedBefore() && spawnLocation != null) {
            player.teleport(spawnLocation);
            player.sendMessage(prefix() + ChatColor.GREEN + "Welcome! You have been teleported to spawn.");
        }
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }
}