package me.bobiyam.pvpbgcore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AntiCheatListener implements Listener {

    private final PvPBGCore plugin;

    // AutoClicker tracking
    private final Map<UUID, Integer> cpsCount = new HashMap<>();

    // Reach tracking (can be extended)
    public AntiCheatListener(PvPBGCore plugin) {
        this.plugin = plugin;
        startCPSResetTask();
    }

    private void startCPSResetTask() {
        int period = plugin.getConfig().getInt("anticheat.autoclicker.check-period-ticks", 20);
        Bukkit.getScheduler().runTaskTimer(plugin, () -> cpsCount.clear(), period, period);
    }

    @EventHandler
    public void onEntityDamageByPlayer(EntityDamageByEntityEvent e) {
        if (!plugin.getConfig().getBoolean("anticheat.enabled")) return;
        if (!(e.getDamager() instanceof Player)) return;

        Player p = (Player) e.getDamager();

        // --- Reach check ---
        if (plugin.getConfig().getBoolean("anticheat.reach.enabled")) {
            double maxDist = plugin.getConfig().getDouble("anticheat.reach.max-distance", 4.5);
            if (e.getEntity().getLocation().distance(p.getLocation()) > maxDist) {
                flagPlayer(p, "Reach", e.getEntity().getLocation());
            }
        }

        // --- AutoClicker / CPS check ---
        if (plugin.getConfig().getBoolean("anticheat.autoclicker.enabled")) {
            int cps = cpsCount.getOrDefault(p.getUniqueId(), 0) + 1;
            cpsCount.put(p.getUniqueId(), cps);

            int maxCPS = plugin.getConfig().getInt("anticheat.autoclicker.max-cps", 15);
            if (cps > maxCPS) {
                flagPlayer(p, "AutoClicker", p.getLocation());
                cpsCount.put(p.getUniqueId(), 0); // reset after flag
            }
        }
    }

    private void flagPlayer(Player p, String reason, Location loc) {
        boolean alert = reason.equals("Reach")
                ? plugin.getConfig().getBoolean("anticheat.reach.alert-staff")
                : plugin.getConfig().getBoolean("anticheat.autoclicker.alert-staff");

        boolean punish = reason.equals("Reach")
                ? plugin.getConfig().getBoolean("anticheat.reach.punish")
                : plugin.getConfig().getBoolean("anticheat.autoclicker.punish");

        String msg = ChatColor.RED + "[AntiCheat] " + ChatColor.YELLOW + p.getName()
                + " flagged for " + reason
                + " at " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ();

        if (alert) {
            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (staff.hasPermission("pvpbg.staff")) {
                    staff.sendMessage(msg);
                }
            }
        }

        plugin.getLogger().info(msg);

        if (punish) {
            p.kickPlayer(ChatColor.RED + "AntiCheat: " + reason + " detected!");
        }
    }
}
