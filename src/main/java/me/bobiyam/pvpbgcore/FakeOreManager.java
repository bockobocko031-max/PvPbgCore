package me.bobiyam.pvpbgcore;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Random;

public class FakeOreManager {

    private final JavaPlugin plugin;
    private final Random random = new Random();

    public FakeOreManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void sendFakeOres(Player p) {
        if (!plugin.getConfig().getBoolean("anti-xray.fake-ores.enabled")) return;

        int radius = plugin.getConfig().getInt("anti-xray.fake-ores.radius");
        int chance = plugin.getConfig().getInt("anti-xray.fake-ores.chance");

        List<String> ores = plugin.getConfig()
                .getStringList("anti-xray.fake-ores.blocks");

        Location loc = p.getLocation();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {

                    Block b = loc.getBlock().getRelative(x, y, z);

                    if (b.getType() != Material.STONE) continue;
                    if (random.nextInt(100) > chance) continue;

                    Material fake = Material.valueOf(
                            ores.get(random.nextInt(ores.size()))
                    );

                    p.sendBlockChange(b.getLocation(), fake, (byte) 0);
                }
            }
        }
    }
}
