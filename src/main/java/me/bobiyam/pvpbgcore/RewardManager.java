package me.bobiyam.pvpbgcore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class RewardManager {

    private final FileConfiguration cfg;
    private final Map<String, Integer> killstreaks;

    private final Map<String, Integer> maxKillstreaks; // максимален killstreak на играча
    private final Map<String, Set<Integer>> milestonesGiven; // пази кои награди вече са дадени

    public RewardManager(FileConfiguration cfg) {
        this.cfg = cfg;
        this.killstreaks = new HashMap<>();
        this.maxKillstreaks = new HashMap<>();
        this.milestonesGiven = new HashMap<>();
    }

    // Увеличаване на killstreak и обновяване на максималния
    public int addKill(Player killer) {
        String name = killer.getName();

        // Увеличаваме текущия killstreak
        int streak = killstreaks.getOrDefault(name, 0) + 1;
        killstreaks.put(name, streak);

        // Вземаме максималния killstreak досега
        int maxStreak = maxKillstreaks.getOrDefault(name, 0);

        // Ако новият killstreak е по-голям от досегашния максимум
        if (streak > maxStreak) {
            // Обновяваме максималния
            maxKillstreaks.put(name, streak);

            // Проверяваме milestone награди, които се дават само веднъж
            checkMilestoneReward(killer, streak);
        }

        return streak;
    }

    // Ресет на текущия killstreak
    public void resetKillstreak(Player player) {
        killstreaks.put(player.getName(), 0);
    }

    // Текущ killstreak награди (по старата логика)
    public void checkKillstreakReward(Player killer) {
        if (!cfg.getBoolean("killstreaks.enabled", false)) return;

        int streak = killstreaks.getOrDefault(killer.getName(), 0);
        String key = String.valueOf(streak);
        if (!cfg.isConfigurationSection("killstreaks.rewards." + key)) return;

        // Съобщение
        String message = cfg.getString("killstreaks.rewards." + key + ".message", "");
        message = message.replace("%player%", killer.getName());
        killer.sendMessage(ChatColor.translateAlternateColorCodes('&', message));

        // Изпълнение на команди
        List<String> commands = cfg.getStringList("killstreaks.rewards." + key + ".commands");
        for (String cmd : commands) {
            cmd = cmd.replace("%player%", killer.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
    }

    // Нов метод: проверка за награди за „исторически“ killstreaks (дори след загуба)
    private void checkMilestoneReward(Player player, int streak) {
        if (!cfg.getBoolean("milestones.enabled", false)) return;

        String name = player.getName();
        Set<Integer> given = milestonesGiven.getOrDefault(name, new HashSet<>());

        for (String key : cfg.getConfigurationSection("milestones.rewards").getKeys(false)) {
            int milestone = Integer.parseInt(key);
            if (streak >= milestone && !given.contains(milestone)) {
                // Дава наградата
                String message = cfg.getString("milestones.rewards." + key + ".message", "")
                        .replace("%player%", name);
                Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));

                List<String> commands = cfg.getStringList("milestones.rewards." + key + ".commands");
                for (String cmd : commands) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", name));
                }

                given.add(milestone); // отбелязваме, че е дадено веднъж
            }
        }

        milestonesGiven.put(name, given);
    }

    public int getKillstreak(Player player) {
        return killstreaks.getOrDefault(player.getName(), 0);
    }
}