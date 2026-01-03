package wtf.pawlend.qeventmanager.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import wtf.pawlend.qeventmanager.qEventManager;

public class MessageUtil {

    private final qEventManager plugin;

    public MessageUtil(qEventManager plugin) {
        this.plugin = plugin;
    }

    public void broadcast(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(ColorUtil.colorize(message));
        }
    }

    public void broadcastWithPermission(String message, String permission) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(permission)) {
                player.sendMessage(ColorUtil.colorize(message));
            }
        }
    }

    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(
                ColorUtil.colorize(title),
                ColorUtil.colorize(subtitle),
                fadeIn, stay, fadeOut
        );
    }

    public void broadcastTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendTitle(player, title, subtitle, fadeIn, stay, fadeOut);
        }
    }
}