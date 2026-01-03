package wtf.pawlend.qeventmanager.listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import wtf.pawlend.qeventmanager.api.EventInfo;
import wtf.pawlend.qeventmanager.gui.GUIManager;
import wtf.pawlend.qeventmanager.qEventManager;

import java.util.List;

public class GUIListener implements Listener {

    private final qEventManager plugin;

    public GUIListener(qEventManager plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (title.startsWith(GUIManager.MAIN_MENU_TITLE)) {
            event.setCancelled(true);
            handleMainMenuClick(player, event);
        } else if (title.equals(GUIManager.VOTE_MENU_TITLE)) {
            event.setCancelled(true);
            handleVoteMenuClick(player, event);
        }
    }

    private void handleMainMenuClick(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        int slot = event.getRawSlot();

        // Навигация
        if (slot == 48 && clicked.getType() == Material.ARROW) {
            // Предыдущая страница
            int currentPage = getCurrentPage(event.getView().getTitle());
            plugin.getGuiManager().openMainMenu(player, currentPage - 1);
            return;
        }

        if (slot == 50 && clicked.getType() == Material.ARROW) {
            // Следующая страница
            int currentPage = getCurrentPage(event.getView().getTitle());
            plugin.getGuiManager().openMainMenu(player, currentPage + 1);
            return;
        }

        // Кнопки управления
        if (slot == 45 && clicked.getType() == Material.EMERALD) {
            player.closeInventory();
            player.performCommand("qevent rand");
            return;
        }

        if (slot == 49 && clicked.getType() == Material.NETHER_STAR) {
            player.closeInventory();
            player.performCommand("qevent startvote");
            return;
        }

        if (slot == 53 && clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            player.performCommand("qevent stop");
            return;
        }

        // Клик по ивенту
        if (clicked.hasItemMeta() && clicked.getItemMeta().hasLore()) {
            List<String> lore = clicked.getItemMeta().getLore();
            for (String line : lore) {
                if (line.contains("ID:")) {
                    String eventId = line.split(":")[1].trim().replace("§f", "").replace("§7", "");
                    player.closeInventory();
                    player.performCommand("qevent start " + eventId);
                    return;
                }
            }
        }
    }

    private void handleVoteMenuClick(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType() == Material.CLOCK) return;

        if (!clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) return;

        // Ищем ивент по названию
        String displayName = clicked.getItemMeta().getDisplayName();
        displayName = displayName.replace("§a✓ ", "").replace("§e", "");

        for (String eventId : plugin.getVoteManager().getVotingEvents()) {
            EventInfo info = plugin.getEventRegistry().getEventInfo(eventId);
            if (info != null && info.getDisplayName().equals(displayName)) {
                plugin.getVoteManager().vote(player, eventId);
                plugin.getGuiManager().openVoteMenu(player); // Обновляем меню
                return;
            }
        }
    }

    private int getCurrentPage(String title) {
        try {
            // Формат: "§6§lМенеджер Ивентов §7(1/2)"
            String[] parts = title.split("\\(");
            if (parts.length > 1) {
                String pageStr = parts[1].split("/")[0].trim();
                return Integer.parseInt(pageStr) - 1;
            }
        } catch (Exception ignored) {}
        return 0;
    }
}