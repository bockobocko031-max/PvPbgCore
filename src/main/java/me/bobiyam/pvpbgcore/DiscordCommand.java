package me.bobiyam.pvpbgcore;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class DiscordCommand implements CommandExecutor {

    private final Plugin plugin;

    public DiscordCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    private String prefix() {
        return "§7[§bPvPBG§7] "; // стандартен ChatColor формат
    }

    // Помощна функция за hex цветове
    private String hex(String hex) {
        return "§x" +
                "§" + hex.charAt(1) +
                "§" + hex.charAt(2) +
                "§" + hex.charAt(3) +
                "§" + hex.charAt(4) +
                "§" + hex.charAt(5) +
                "§" + hex.charAt(6);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String url = plugin.getConfig().getString("discord.url",
                plugin.getConfig().getString("discord.invite", "https://discord.gg/example"));

        if (!(sender instanceof Player)) {
            sender.sendMessage(prefix() + "DISCORD: " + url);
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("pvpbgcore.discord")) {
            player.sendMessage(prefix() + "§cYou don't have permission to use this command.");
            return true;
        }

        // Горна линия
        TextComponent topBorder = new TextComponent("§8══════════════════════════════\n");

        // Заглавие
        TextComponent title = new TextComponent(hex("#32D3F6") + "🌐 §lDISCORD\n");

        // Описание
        TextComponent desc = new TextComponent(
                "§7📢 Stay up to date with all " + hex("#32D3F6") + "announcements\n" +
                        "§7🎁 Participate in " + hex("#32D3F6") + "giveaways\n" +
                        "§7🎉 Join " + hex("#32D3F6") + "events and much more!\n" +
                        "§7💬 Be part of the " + hex("#32D3F6") + "PvPBulgaria §7Community Discord today!\n\n"
        );

        // Бутон / линк
        TextComponent button = new TextComponent(hex("#32D3F6") + "§l➥ §n" + url);
        button.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
        button.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("§eClick to open the Discord invite!\n§7Join chats, events & giveaways").create()));

        // Долна линия
        TextComponent bottomBorder = new TextComponent("\n§8══════════════════════════════");

        // Изпращане на съобщения
        player.spigot().sendMessage(topBorder);
        player.spigot().sendMessage(title);
        player.spigot().sendMessage(desc);
        player.spigot().sendMessage(button);
        player.spigot().sendMessage(bottomBorder);

        // Звук за player
        player.playSound(player.getLocation(), "entity.player.levelup", 5f, 1f);

        return true;
    }
}