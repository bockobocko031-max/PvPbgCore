package me.bobiyam.pvpbgcore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.HashMap;
import java.util.Map;

public class AntiCheatListener implements Listener {

    private final PvPBGCore plugin;

    // CPS tracking per player
    private final Map<Player, Integer> cpsCount = new HashMap<>();
    private final Map<Player, Long> cpsLastReset = new HashMap<>();

    // KillAura tracking per player per tick
    private final Map<Player, Integer> hitsThisTick = new HashMap<>();

    public AntiCheatListener(PvPBGCore plugin) {
        this.plugin = plugin;
        startResetTask();
    }

    // Reset CPS & hits every 20 ticks (1 second)
    private void startResetTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            cpsCount.clear();
            cpsLastReset.clear();
            hitsThisTick.clear();
        }, 20L, 20L);
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!plugin.getConfig().getBoolean("anticheat.enabled")) return;
        if (!(e.getDamager() instanceof Player)) return;

        Player p = (Player) e.getDamager();
        Location loc = p.getLocation();

        // --- Reach check ---
        if (plugin.getConfig().getBoolean("anticheat.reach.enabled")) {
            double maxDist = plugin.getConfig().getDouble("anticheat.reach.max-distance", 4.5);
            if (e.getEntity().getLocation().distance(loc) > maxDist) {
                flagPlayer(p, "reach", loc);
            }
        }

        // --- AutoClicker / CPS check ---
        if (plugin.getConfig().getBoolean("anticheat.autoclicker.enabled")) {
            cpsCount.putIfAbsent(p, 0);
            cpsLastReset.putIfAbsent(p, System.currentTimeMillis());

            long now = System.currentTimeMillis();
            if (now - cpsLastReset.get(p) > 1000) { // 1 second reset
                cpsCount.put(p, 0);
                cpsLastReset.put(p, now);
            }

            cpsCount.put(p, cpsCount.get(p) + 1);

            int maxCPS = plugin.getConfig().getInt("anticheat.autoclicker.max-cps", 15);
            if (cpsCount.get(p) > maxCPS) {
                flagPlayer(p, "autoclicker", loc);
                cpsCount.put(p, 0);
            }
        }

        // --- KillAura / hits-per-tick check ---
        if (plugin.getConfig().getBoolean("anticheat.killaura.enabled")) {
            hitsThisTick.put(p, hitsThisTick.getOrDefault(p, 0) + 1);

            int maxHits = plugin.getConfig().getInt("anticheat.killaura.max-hits-per-tick", 1);
            if (hitsThisTick.get(p) > maxHits) {
                flagPlayer(p, "killaura", loc);
                hitsThisTick.put(p, 0);
            }
        }
    }

    // --- Flag player method ---
    private void flagPlayer(Player p, String type, Location loc) {
        String rawMsg = "";

        switch (type.toLowerCase()) {
            case "reach":
                rawMsg = plugin.getConfig().getString("anticheat.messages.reach-flag", "&c[AntiCheat] {player} flagged for Reach!");
                break;
            case "autoclicker":
                rawMsg = plugin.getConfig().getString("anticheat.messages.autoclicker-flag", "&c[AntiCheat] {player} flagged for AutoClicker!");
                break;
            case "killaura":
                rawMsg = plugin.getConfig().getString("anticheat.messages.killaura-flag", "&c[AntiCheat] {player} flagged for KillAura!");
                break;
        }

        // Replace placeholders
        String msg = rawMsg.replace("{player}", p.getName())
                .replace("{x}", String.valueOf(loc.getBlockX()))
                .replace("{y}", String.valueOf(loc.getBlockY()))
                .replace("{z}", String.valueOf(loc.getBlockZ()));

        boolean alert = plugin.getConfig().getBoolean("anticheat." + type + ".alert-staff");
        boolean punish = plugin.getConfig().getBoolean("anticheat." + type + ".punish");

        // Alert staff
        if (alert) {
            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (staff.hasPermission("pvpbg.staff")) {
                    staff.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                }
            }
        }

        plugin.getLogger().info("[AntiCheat] " + p.getName() + " flagged for " + type);

        // Punish player
        if (punish) {
            p.kickPlayer(ChatColor.RED + "AntiCheat: " + type + " detected!");
        }
    }
}