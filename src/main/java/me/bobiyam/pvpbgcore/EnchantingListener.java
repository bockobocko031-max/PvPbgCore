package me.bobiyam.pvpbgcore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;

public class EnchantingListener implements Listener {

    private final Plugin plugin;
    private static final String META_KEY = "enchant_villager";

    public EnchantingListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager)) return;

        Villager villager = (Villager) event.getRightClicked();
        if (!villager.hasMetadata(META_KEY)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        String title = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("enchanting.gui-title", "&6Enchanting"));

        Inventory inv = Bukkit.createInventory(null, 9, title);

        String buttonName = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("enchanting.gui-button-name", "&bAttempt Upgrade"));

        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(buttonName);
            int costPer = plugin.getConfig().getInt("enchanting.levels-cost", 30);
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Cost per item: " + ChatColor.YELLOW + costPer + " levels",
                    ChatColor.GRAY + "Upgrades: any armor piece with Protection III"
            ));
            button.setItemMeta(meta);
        }

        inv.setItem(4, button); // center slot
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() == null) return;

        String title = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("enchanting.gui-title", "&6Enchanting"));
        if (!event.getInventory().getTitle().equals(title)) return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;

        ItemMeta meta = event.getCurrentItem().getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        String buttonName = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("enchanting.gui-button-name", "&bAttempt Upgrade"));
        if (!meta.getDisplayName().equals(buttonName)) return;

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked(); // cast за Java 8

        player.closeInventory();
        Enchanting.attemptUpgrade(player, plugin);
    }
}