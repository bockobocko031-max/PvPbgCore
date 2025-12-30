package me.bobiyam.pvpbgcore;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class StaffListener implements Listener {

    private final Set<UUID> frozenPlayers = new HashSet<>();
    private final Map<UUID, Integer> leftClicks = new HashMap<>();
    private final Map<UUID, Integer> cpsTasks = new HashMap<>();
    private final Random random = new Random();
    private final String prefix = "§7[§bStaff§7] ";

    /* ===================== STAFF ITEMS ===================== */
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player staff = e.getPlayer();
        if (!StaffCommand.staffModes.containsKey(staff.getUniqueId())) return;
        if (e.getItem() == null) return;

        ItemMeta meta = e.getItem().getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;
        e.setCancelled(true);

        // INSPECT → Inventory + CPS
        if (meta.getDisplayName().equals("§aInspect Player")) {
            Player target = getTarget(staff);
            if (target == null) {
                staff.sendMessage(prefix + "§cLook at a player!");
                return;
            }
            openCpsInventory(staff, target);
        }

        // FREEZE
        if (meta.getDisplayName().equals("§bFreeze Player")) {
            Player target = getTarget(staff);
            if (target == null) {
                staff.sendMessage(prefix + "§cLook at a player!");
                return;
            }

            if (frozenPlayers.contains(target.getUniqueId())) {
                frozenPlayers.remove(target.getUniqueId());
                target.removePotionEffect(PotionEffectType.SLOW);
                target.removePotionEffect(PotionEffectType.JUMP);
                target.sendTitle("§aUNFROZEN", "§7You can move again");
                staff.sendMessage(prefix + "§aUnfroze §f" + target.getName());
                return;
            }

            frozenPlayers.add(target.getUniqueId());
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 10));
            target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 200));
            target.sendTitle("§cFROZEN", "§7Do not log out!");
            target.sendMessage(prefix + "§cYou have been frozen!");
            staff.sendMessage(prefix + "§bFroze §f" + target.getName());
        }

        // RANDOM TELEPORT
        if (meta.getDisplayName().equals("§dRandom Teleport")) {
            List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
            players.remove(staff);
            if (players.isEmpty()) {
                staff.sendMessage(prefix + "§cNo players online!");
                return;
            }
            Player target = players.get(random.nextInt(players.size()));
            staff.teleport(target.getLocation());
            staff.sendMessage(prefix + "§dTeleported to §f" + target.getName());
        }
    }

    /* ===================== FREEZE PROTECTION ===================== */
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!frozenPlayers.contains(p.getUniqueId())) return;

        if (e.getFrom().getX() == e.getTo().getX()
                && e.getFrom().getY() == e.getTo().getY()
                && e.getFrom().getZ() == e.getTo().getZ()) return; // allow head rotation

        Location back = e.getFrom();
        back.setYaw(e.getTo().getYaw());
        back.setPitch(e.getTo().getPitch());
        e.setTo(back);
    }

    @EventHandler
    public void onFrozenCommand(PlayerCommandPreprocessEvent e) {
        if (frozenPlayers.contains(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(prefix + "§cYou are frozen!");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (frozenPlayers.remove(e.getPlayer().getUniqueId())) {
            Bukkit.broadcastMessage(prefix + "§c" + e.getPlayer().getName() + " logged out while frozen!");
        }
    }

    /* ===================== STAFF MODE BLOCK ===================== */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player) {
            Player p = (Player) e.getWhoClicked();
            if (StaffCommand.staffModes.containsKey(p.getUniqueId())
                    || frozenPlayers.contains(p.getUniqueId())) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (StaffCommand.staffModes.containsKey(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent e) {
        if (StaffCommand.staffModes.containsKey(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    /* ===================== CPS COUNT ===================== */
    @EventHandler
    public void onClick(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (e.getAction().toString().contains("LEFT_CLICK")) {
            leftClicks.put(p.getUniqueId(), leftClicks.getOrDefault(p.getUniqueId(), 0) + 1);
        }
    }

    /* ===================== INSPECT INVENTORY + CPS SLOT ===================== */
    private void openCpsInventory(Player staff, Player target) {
        Inventory targetInv = Bukkit.createInventory(null, 54, "§bInspect: " + target.getName());

        // Copy target contents
        ItemStack[] contents = target.getInventory().getContents();
//        ItemStack[] armor = target.getInventory().getArmorContents();
        for (int i = 0; i < contents.length; i++) if (contents[i] != null) targetInv.setItem(i, contents[i].clone());
//        for (int i = 0; i < armor.length; i++) if (armor[i] != null) targetInv.setItem(36 + i, armor[i].clone());

        // CPS live slot (top-right)
        targetInv.setItem(8, createCpsItem(Material.DIAMOND_SWORD, "§aLeft CPS: §f0"));

        staff.openInventory(targetInv);

        // Cancel previous task
        if (cpsTasks.containsKey(staff.getUniqueId())) {
            Bukkit.getScheduler().cancelTask(cpsTasks.remove(staff.getUniqueId()));
        }

        // Live CPS update task
        int task = Bukkit.getScheduler().runTaskTimer(
                JavaPlugin.getProvidingPlugin(getClass()),
                () -> {
                    if (!staff.isOnline() || !staff.getOpenInventory().getTitle().contains("Inspect: ")) {
                        Bukkit.getScheduler().cancelTask(cpsTasks.remove(staff.getUniqueId()));
                        return;
                    }

                    int cps = leftClicks.getOrDefault(target.getUniqueId(), 0);

                    Inventory inv = staff.getOpenInventory().getTopInventory();
                    updateName(inv, 8, "§aLeft CPS: §f" + cps);

                    leftClicks.put(target.getUniqueId(), 0);
                }, 0L, 20L
        ).getTaskId();

        cpsTasks.put(staff.getUniqueId(), task);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        if (cpsTasks.containsKey(id)) {
            Bukkit.getScheduler().cancelTask(cpsTasks.remove(id));
        }
    }

    private ItemStack createCpsItem(Material m, String name) {
        ItemStack i = new ItemStack(m);
        ItemMeta im = i.getItemMeta();
        im.setDisplayName(name);
        i.setItemMeta(im);
        return i;
    }

    private void updateName(Inventory inv, int slot, String name) {
        ItemStack item = inv.getItem(slot);
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
    }

    /* ===================== TARGET ===================== */
    private Player getTarget(Player staff) {
        for (Entity e : staff.getNearbyEntities(5, 5, 5)) {
            if (e instanceof Player) return (Player) e;
        }
        return null;
    }
}