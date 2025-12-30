package me.bobiyam.pvpbgcore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDeathListener implements Listener {

    private final RewardManager rewardManager;
    private final FileConfiguration cfg;
    private final String prefix = ChatColor.translateAlternateColorCodes('&', "&7[&cKillStreak&7] ");

    // Optional: за cooldown между убийствата на един и същ играч
    private final Map<UUID, Map<UUID, Long>> lastKillTime = new HashMap<>();
    private final long KILLSTREAK_COOLDOWN = 3000; // 3 секунди

    public PlayerDeathListener(RewardManager rewardManager, FileConfiguration cfg) {
        this.rewardManager = rewardManager;
        this.cfg = cfg;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer != null && !killer.equals(victim)) {
            UUID killerId = killer.getUniqueId();
            UUID victimId = victim.getUniqueId();
            long now = System.currentTimeMillis();

            // Инициализираме Map за този убиец
            lastKillTime.putIfAbsent(killerId, new HashMap<>());
            Map<UUID, Long> killerMap = lastKillTime.get(killerId);

            // Проверка за cooldown между убийства на същата жертва
            if (killerMap.containsKey(victimId)) {
                long lastTime = killerMap.get(victimId);
                if (now - lastTime < KILLSTREAK_COOLDOWN) {
                    return; // не увеличаваме killstreak
                }
            }

            // Увеличаваме killstreak на убиеца
            int newStreak = rewardManager.addKill(killer);

            // Проверка за текущи killstreak награди
            rewardManager.checkKillstreakReward(killer);

            // Обновяваме времето на последното убийство на жертвата
            killerMap.put(victimId, now);
        }

        // Вземаме текущия killstreak на жертвата преди ресет
        int lostStreak = rewardManager.getKillstreak(victim);

        // Ресет на killstreak
        rewardManager.resetKillstreak(victim);

        // Показване на lost_streak съобщения
        if (lostStreak > 0 && cfg.getBoolean("lost_streak_message.enabled", true)) {
            String msgSelf = cfg.getString("lost_streak_message.message_self", "&cВие загубихте своя killstreak от %streak%!");
            msgSelf = msgSelf.replace("%streak%", String.valueOf(lostStreak));
            victim.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', msgSelf));

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
}
