package me.bobiyam.pvpbgcore;

import org.bukkit.*;
import org.bukkit.command.Command;
import me.bobiyam.pvpbgcore.AntiSwearManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;

import java.io.File;
import java.util.*;

import java.util.*;

public final class PvPBGCore extends JavaPlugin implements Listener {

    private FileConfiguration cfg;
    private RewardManager rewardManager;
    private String prefix;
    private HourlyRewards hourlyRewards;
    private final Map<UUID, UUID> tpaRequests = new HashMap<>();
    private final Map<UUID, GameMode> staffModes = new HashMap<>();
    private final Map<UUID, UUID> lastMessaged = new HashMap<>();
    private final Map<UUID, Set<UUID>> recentKills = new HashMap<>();
    private final long KILLSTREAK_COOLDOWN = 5 * 60 * 1000; // 5 минути cooldown
    private final Map<UUID, Map<UUID, Long>> lastKillTime = new HashMap<>();
    private final Set<UUID> frozenPlayers = new HashSet<>();
    private final Map<UUID, Location> homes = new HashMap<>();

    private final Map<UUID, ItemStack[]> savedInventory = new HashMap<>();
    private final Map<UUID, ItemStack[]> savedArmor = new HashMap<>();

    private final Map<UUID, Long> homeCooldowns = new HashMap<>();



    @Override
    public void onEnable() {
        saveDefaultConfig();
        cfg = getConfig();
        getCommand("customenchant").setExecutor(new me.bobiyam.pvpbgcore.CustomEnchantCommand(this));
        getCommand("discord").setExecutor(new me.bobiyam.pvpbgcore.DiscordCommand(this));

        // Register listener for GUI
        getServer().getPluginManager().registerEvents(new EnchantingListener(this), this);
        getServer().getPluginManager().registerEvents(new EnchantingListener(this), this);

        ScreenshareManager ssManager = new ScreenshareManager(this);

        // Команди
        getCommand("setSSROOM").setExecutor(ssManager);
        getCommand("setssspawn").setExecutor(ssManager);
        getCommand("ss").setExecutor(ssManager);
        getCommand("unss").setExecutor(ssManager);

        Bukkit.getPluginManager().registerEvents(
                new AntiXrayListener(this), this
        );

        Bukkit.getPluginManager().registerEvents(new AntiCheatListener(this), this);


        // register the command only if present in plugin.yml
        if (getCommand("spawnenchantnpc") != null) {
            getCommand("spawnenchantnpc").setExecutor(new EnchantingCommand(this));
        } else {
            getLogger().warning("Command 'spawnenchantnpc' not found in plugin.yml; skipping command registration.");
        }

        getLogger().info("Enchanting module registered.");
        AntiSwearManager antiSwear = new AntiSwearManager(this);
        new ReportCommand(this);
        getServer().getPluginManager().registerEvents(antiSwear, this);
        new StaffChatCommand(this);
        new SpawnManager(this);
        getCommand("staffmode").setExecutor(new StaffCommand());
        Bukkit.getPluginManager().registerEvents(new StaffListener(), this);
        hourlyRewards = new HourlyRewards(this);
        new ReloadCommand(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        prefix = ChatColor.translateAlternateColorCodes('&', cfg.getString("prefix", "&6[Server] &f"));
        rewardManager = new RewardManager(cfg); // <- това трябва да е тук
        getServer().getPluginManager().registerEvents(this, this);
        new PvPSystem(this);
        new RenameChestBypasser(this);
    }

    @Override
    public void onDisable() {
    }

    private String color(String text) {
        if (text == null) return "§cMessage not found";
        return text.replace("&", "§");
    }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(color("&c❌ This command can only be used by players!"));
            return true;
        }
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("killstreak")) {
            int streak = rewardManager.getKillstreak(player);
            player.sendMessage(prefix +ChatColor.GREEN + "Vashiqt tekusht killstreak: " + streak);
            return true;
        }

        if (!(sender instanceof Player)) return true;

        if (cmd.getName().equalsIgnoreCase("menu")) {
            if (!player.hasPermission("pvpbgcore.menu")) {
                player.sendMessage(prefix + ChatColor.RED + "You don't have permission to use this command!");
                return true;
            }

            Inventory inv = Bukkit.createInventory(null, cfg.getInt("menu-size"),
                    ChatColor.translateAlternateColorCodes('&', cfg.getString("menu-title")));

            // Добавяне на item-ите от config
            for (String key : cfg.getConfigurationSection("items").getKeys(false)) {
                int slot = cfg.getInt("items." + key + ".slot");
                Material mat = Material.getMaterial(cfg.getString("items." + key + ".material").toUpperCase());
                short data = (short) cfg.getInt("items." + key + ".data", 0);
                String name = ChatColor.translateAlternateColorCodes('&', cfg.getString("items." + key + ".name"));

                if (mat == null) continue; // игнорира ако материалът не съществува

                ItemStack item = new ItemStack(mat, 1, data);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(name);
                item.setItemMeta(meta);
                inv.setItem(slot, item);
            }

            if (cmd.getName().equalsIgnoreCase("pvpbgcore")) {
                if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                    if (!player.hasPermission("pvpbgcore.reload")) {
                        player.sendMessage(prefix + ChatColor.RED + "You don't have permission to reload the plugin!");
                        return true;
                    }

                    reloadConfig();
                    cfg = getConfig();
                    prefix = ChatColor.translateAlternateColorCodes('&', cfg.getString("prefix", "&6[Server] &f"));
                    rewardManager = new RewardManager(cfg); // преинициализираме RewardManager с новата конфигурация

                    player.sendMessage(prefix + ChatColor.GREEN + "PvPBGCore configuration reloaded!");
                    return true;
                }
            }

// Добавяне на border
            if (cfg.getBoolean("border.enabled")) {
                Material borderMat = Material.getMaterial(cfg.getString("border.material").toUpperCase());
                short borderData = (short) cfg.getInt("border.data", 0);

                if (borderMat != null) {
                    ItemStack border = new ItemStack(borderMat, 1, borderData);
                    ItemMeta bMeta = border.getItemMeta();

                    // Взимаме име от config, ако няма, използваме празен стринг
                    String borderName = cfg.getString("border.name", " ");
                    if (borderName != null) {
                        borderName = ChatColor.translateAlternateColorCodes('&', borderName);
                        bMeta.setDisplayName(borderName);
                    } else {
                        bMeta.setDisplayName(" ");
                    }

                    border.setItemMeta(bMeta);

                    // Слагаме border на всички празни слотове
                    for (int i = 0; i < inv.getSize(); i++) {
                        if (inv.getItem(i) == null) inv.setItem(i, border);
                    }
                }
            }

            player.openInventory(inv);
        }

        return true;
    }



    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer != null && killer != victim) {

            UUID killerId = killer.getUniqueId();
            UUID victimId = victim.getUniqueId();

            long now = System.currentTimeMillis();

            // Инициализираме Map за този играч ако няма
            lastKillTime.putIfAbsent(killerId, new HashMap<>());
            Map<UUID, Long> killerMap = lastKillTime.get(killerId);

            // Проверка дали този убиец вече е убил тази жертва наскоро
            if (killerMap.containsKey(victimId)) {
                long lastTime = killerMap.get(victimId);
                if (now - lastTime < KILLSTREAK_COOLDOWN) {
                    // Играчът е убил жертвата наскоро, не увеличаваме killstreak
                    return;
                }
            }

            // Увеличаваме killstreak
            rewardManager.addKill(killer);
            rewardManager.checkKillstreakReward(killer);

            // Обновяваме времето на последния kill
            killerMap.put(victimId, now);
        }

        // Вземаме текущия killstreak на жертвата преди ресет
        int lostStreak = rewardManager.getKillstreak(victim);

        // Ресет на killstreak
        rewardManager.resetKillstreak(victim);

        if (lostStreak > 0 && cfg.getBoolean("lost_streak_message.enabled", true)) {
            // Съобщение за жертвата
            String msgSelf = cfg.getString("lost_streak_message.message_self", "&cВие загубихте своя killstreak от %streak%!");
            msgSelf = msgSelf.replace("%streak%", String.valueOf(lostStreak));
            victim.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', msgSelf));

            // Съобщение за останалите играчи
            String msgOthers = cfg.getString("lost_streak_message.message_others", "&e%player% е загубил своя killstreak от %streak%!");
            msgOthers = msgOthers.replace("%player%", victim.getName())
                    .replace("%streak%", String.valueOf(lostStreak));

            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.equals(victim)) {
                    online.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', msgOthers));
                }
            }
        }
    }

    // Играчите запазват killstreak при излизане, няма ресет на PlayerQuitEvent
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Не правим нищо – killstreak остава запазен
    }

    public void reloadPluginConfig(Player player) {
        reloadConfig();             // презареждаме config.yml
        cfg = getConfig();          // обновяваме локалния обект
        prefix = ChatColor.translateAlternateColorCodes('&', cfg.getString("prefix", "&6[Server] &f"));
        rewardManager = new RewardManager(cfg); // презареждаме RewardManager с новата конфигурация
        hourlyRewards.reload();
        Bukkit.getPluginManager().registerEvents(new AntiCheatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PvPSystem(this), this);
        if (player != null)
            player.sendMessage(prefix + ChatColor.GREEN + "PvPBGCore configuration reloaded!");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;

        Player player = (Player) e.getWhoClicked();
        if (!e.getView().getTitle().equals(ChatColor.translateAlternateColorCodes('&', cfg.getString("menu-title"))))
            return;

        e.setCancelled(true);
        if (e.getCurrentItem() == null || !e.getCurrentItem().hasItemMeta()) return;

        String clickedName = ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());

        for (String key : cfg.getConfigurationSection("items").getKeys(false)) {
            String itemName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', cfg.getString("items." + key + ".name")));
            if (clickedName.equals(itemName)) {
                String permission = cfg.getString("items." + key + ".permission");
                if (permission != null && !permission.isEmpty() && !player.hasPermission(permission)) {
                    player.sendMessage(ChatColor.RED + "You don't have permission for this!");
                    return;
                }

                String command = cfg.getString("items." + key + ".command");
                if (command != null && !command.isEmpty()) {
                    player.closeInventory();
                    player.performCommand(command);
                    // Sound за 1.8
                    player.playSound(player.getLocation(), Sound.valueOf("CLICK"), 1f, 1f);
                }
            }
        }
    }
}
