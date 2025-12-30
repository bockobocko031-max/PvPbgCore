package me.bobiyam.pvpbgcore;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class EventCommand implements CommandExecutor {

    private final EventManager eventManager;
    private final FileConfiguration config;

    public EventCommand(PvPBGCore plugin, EventManager eventManager) {
        this.eventManager = eventManager;
        this.config = plugin.getConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Тази команда може да се използва само от играчи!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendTitle("§6/ EVENT /", "§eПодкоманди: timer | message | start | stop");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "timer":
                int timer = config.getInt("event.timer", 60);
                player.sendTitle("§aEvent Timer", "§e" + timer + " секунди");
                break;

            case "message":
                String message = config.getString("event.message", "Събитието започва скоро!");
                player.sendTitle("§bEvent Message", message);
                break;

            case "start":
                if (eventManager.isRunning()) {
                    player.sendTitle("§cСъбитието вече е стартирало!", "");
                } else {
                    int duration = config.getInt("event.timer", 60);
                    eventManager.startEvent(duration);
                    player.sendTitle("§aСъбитието стартира!", "");
                }
                break;

            case "stop":
                if (!eventManager.isRunning()) {
                    player.sendTitle("§cСъбитието не е стартирало!", "");
                } else {
                    eventManager.stopEvent();
                    player.sendTitle("§cСъбитието спря!", "");
                }
                break;

            default:
                player.sendTitle("§cГрешка!", "§eНевалидна подкоманда!");
                break;
        }

        return true;
    }
}