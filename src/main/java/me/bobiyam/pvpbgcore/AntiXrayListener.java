package me.bobiyam.pvpbgcore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AntiXrayListener implements Listener {

    private final PvPBGCore plugin;
    private final Map<UUID, Integer> oreCount = new HashMap<>();

    public AntiXrayListener(PvPBGCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (!plugin.getConfig().getBoolean("anti-xray.enabled")) return;

        Player p = e.getPlayer();
        Block b = e.getBlock();

        List<String> tracked = plugin.getConfig().getStringList("anti-xray.tracked-blocks");
        if (!tracked.contains(b.getType().name())) return;

        // count how many ores mined in a short period
        int count = oreCount.getOrDefault(p.getUniqueId(), 0) + 1;
        oreCount.put(p.getUniqueId(), count);

        int threshold = plugin.getConfig().getInt("anti-xray.flag-threshold");

        if (count >= threshold) {
            flagPlayer(p, b, count);
            oreCount.put(p.getUniqueId(), 0); // reset counter
        }

        // Optionally: reset counter after a delay
        Bukkit.getScheduler().runTaskLater(plugin, () -> oreCount.put(p.getUniqueId(), 0), 200L); // 10 seconds
    }

    private void flagPlayer(Player p, Block b, int count) {
        boolean alert = plugin.getConfig().getBoolean("anti-xray.alert-staff");
        boolean punish = plugin.getConfig().getBoolean("anti-xray.punish-on-flag");

        String msg = ChatColor.RED + "[AntiXray] " + ChatColor.YELLOW + p.getName()
                + " mined " + count + " ores around "
                + b.getX() + " " + b.getY() + " " + b.getZ();

        if (alert) {
            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (staff.hasPermission("pvpbg.staff")) {
                    staff.sendMessage(msg);
                }
            }
        }

        if (punish) {
            p.kickPlayer(ChatColor.RED + "Xray detected!"); // or /ban
        }

        plugin.getLogger().info(msg);
    }
}