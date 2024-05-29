package com.ashkiano.slowchat;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public class SlowChat extends JavaPlugin implements Listener, CommandExecutor {
    private final HashMap<String, Long> lastMessageTime = new HashMap<>();
    private int delayInSeconds;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.getServer().getPluginManager().registerEvents(this, this);
        this.getCommand("slowchat").setExecutor(this);
        loadDelayFromConfig();
        Metrics metrics = new Metrics(this, 21924);
        this.getLogger().info("Thank you for using the SlowChat plugin! If you enjoy using this plugin, please consider making a donation to support the development. You can donate at: https://donate.ashkiano.com");
    }

    @Override
    public void onDisable() {
        saveDelayToConfig();
    }

    private void loadDelayFromConfig() {
        FileConfiguration config = this.getConfig();
        delayInSeconds = config.getInt("chat-delay", 5); // Defaultní hodnota je 5 sekund
    }

    private void saveDelayToConfig() {
        FileConfiguration config = this.getConfig();
        config.set("chat-delay", delayInSeconds);
        saveConfig();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String playerId = event.getPlayer().getUniqueId().toString();
        if (!event.getPlayer().hasPermission("slowchat.use")) {
            return; // Hráč nemá oprávnění, ignorujeme
        }

        long currentTime = System.currentTimeMillis();
        if (!canSendMessage(playerId, currentTime)) {
            event.getPlayer().sendMessage(ChatColor.RED + "You must wait " + delayInSeconds + " seconds before sending another message.");
            event.setCancelled(true); // Zrušíme event, takže zpráva nebude odeslána
        } else {
            lastMessageTime.put(playerId, currentTime); // Aktualizujeme čas poslední zprávy
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof CommandSender)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        if (!sender.hasPermission("slowchat.setdelay")) {
            sender.sendMessage(ChatColor.RED + "You have no right to use this command!");
            return true;
        }

        if (args.length > 0) {
            try {
                delayInSeconds = Integer.parseInt(args[0]);
                saveDelayToConfig();
                sender.sendMessage(ChatColor.GREEN + "Chat delay has been set to " + delayInSeconds + " seconds.");
                return true;
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Usage: /slowchat <seconds>");
                return false;
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Usage: /slowchat <seconds>");
            return false;
        }
    }

    private boolean canSendMessage(String playerId, long currentTime) {
        return !lastMessageTime.containsKey(playerId) || (currentTime - lastMessageTime.get(playerId) >= delayInSeconds * 1000);
    }
}