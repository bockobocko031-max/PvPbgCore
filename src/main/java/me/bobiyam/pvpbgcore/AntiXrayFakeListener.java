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
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AntiXrayFakeListener implements Listener {

    private final FakeOreManager fakeOreManager;
    private final Map<UUID, Location> lastFakeHit = new HashMap<>();

    public AntiXrayFakeListener(FakeOreManager manager) {
        this.fakeOreManager = manager;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (e.getFrom().getBlockX() == e.getTo().getBlockX()
                && e.getFrom().getBlockY() == e.getTo().getBlockY()
                && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) return;

        fakeOreManager.sendFakeOres(e.getPlayer());
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Block b = e.getBlock();

        // ако е счупил stone, но client-side е било ore → FLAG
        if (b.getType() == Material.STONE) {

            Bukkit.getOnlinePlayers().forEach(staff -> {
                if (staff.hasPermission("pvpbg.staff")) {
                    staff.sendMessage(ChatColor.RED +
                            "[AntiXray] " + p.getName()
                            + " broke fake ore at "
                            + b.getX() + " " + b.getY() + " " + b.getZ());
                }
            });
        }
    }
}
