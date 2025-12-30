package me.bobiyam.pvpbgcore;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Enchanting {

    public static class Found {
        ItemStack item;
        int inventorySlot; // слот в инвентара
    }

    public static boolean attemptUpgrade(Player player, Plugin plugin) {
        FileConfiguration cfg = plugin.getConfig();

        boolean useArmor = cfg.getBoolean("enchanting.use-armor", true);
        int levelsCost = cfg.getInt("enchanting.levels-cost", 30);
        String msgNeedArmor = ChatColor.translateAlternateColorCodes('&',
                cfg.getString("enchanting.messages.need-armor", "&cYou need at least one Protection III armor piece!"));
        String msgNeedLevels = ChatColor.translateAlternateColorCodes('&',
                cfg.getString("enchanting.messages.need-levels", "&cYou need %levels% levels to upgrade!"));
        String msgSuccess = ChatColor.translateAlternateColorCodes('&',
                cfg.getString("enchanting.messages.success", "&aUpgrade successful!"));

        if (!useArmor) {
            player.sendMessage(ChatColor.DARK_GREEN + "Armor upgrading is disabled in config.");
            return false;
        }

        List<Found> prot3Armor = new ArrayList<>();
        collectArmorFromInventory(player, prot3Armor);

        if (prot3Armor.isEmpty()) {
            player.sendMessage(msgNeedArmor);
            return false;
        }

        if (player.getLevel() < levelsCost) {
            player.sendMessage(msgNeedLevels.replace("%levels%", String.valueOf(levelsCost)));
            return false;
        }

        // Избира случайна броня
        Random rand = new Random();
        Found chosen = prot3Armor.get(rand.nextInt(prot3Armor.size()));

        // Клониране и ъпгрейд
        ItemStack newItem = chosen.item.clone();
        newItem.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);

        // Прави бронята unbreakable (съвместимост)
        ItemMeta meta = newItem.getItemMeta();
        if (meta != null) {
            try {
                newItem.setItemMeta(meta);
            } catch (NoSuchMethodError ignored) { }
        }

        // Поставяне обратно в инвентара на същия слот
        player.getInventory().setItem(chosen.inventorySlot, newItem);

        // Отнема нива
        player.setLevel(Math.max(0, player.getLevel() - levelsCost));

        player.sendMessage(msgSuccess + " " + chosen.item.getType().name() + " -> Protection IV");

        // Пуска звук
        playConfiguredSound(player, plugin, "enchanting.sounds.success", "ENTITY_PLAYER_LEVELUP");

        return true;
    }

    // Обхожда целия инвентар за броня с Prot III
    private static void collectArmorFromInventory(Player player, List<Found> list) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL) == 3) {
                Found f = new Found();
                f.item = item;
                f.inventorySlot = i;
                list.add(f);
            }
        }
    }

    private static void playConfiguredSound(Player player, Plugin plugin, String configKey, String defaultName) {
        String name = plugin.getConfig().getString(configKey, defaultName);
        if (name == null || name.isEmpty()) return;
        try {
            Sound sound = Sound.valueOf(name);
            player.playSound(player.getLocation(), sound, 1f, 1f);
        } catch (IllegalArgumentException ignored) { }
    }
}