package wtf.pawlend.qeventmanager.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import wtf.pawlend.qeventmanager.api.EventInfo;
import wtf.pawlend.qeventmanager.api.EventStatus;
import wtf.pawlend.qeventmanager.api.QEvent;
import wtf.pawlend.qeventmanager.qEventManager;
import wtf.pawlend.qeventmanager.util.ColorUtil;

import java.util.*;

public class GUIManager {

    private final qEventManager plugin;
    public static final String MAIN_MENU_TITLE = "§6§lМенеджер Ивентов";
    public static final String VOTE_MENU_TITLE = "§e§lГолосование за ивент";

    public GUIManager(qEventManager plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        openMainMenu(player, 0);
    }

    public void openMainMenu(Player player, int page) {
        List<EventInfo> events = plugin.getEventRegistry().getAllEventInfo();
        int totalPages = (int) Math.ceil(events.size() / 28.0);
        if (totalPages == 0) totalPages = 1;

        String title = MAIN_MENU_TITLE + " §7(" + (page + 1) + "/" + totalPages + ")";
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Заполняем рамку
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inv.setItem(i, filler);
        for (int i = 45; i < 54; i++) inv.setItem(i, filler);
        for (int i = 9; i < 45; i += 9) inv.setItem(i, filler);
        for (int i = 17; i < 45; i += 9) inv.setItem(i, filler);

        // Отображаем ивенты
        int startIndex = page * 28;
        int slot = 10;

        for (int i = startIndex; i < Math.min(startIndex + 28, events.size()); i++) {
            if (slot == 17 || slot == 18 || slot == 26 || slot == 27 || slot == 35 || slot == 36) {
                slot++;
            }
            if (slot >= 44) break;

            EventInfo info = events.get(i);
            inv.setItem(slot, createEventItem(info));
            slot++;
        }

        // Кнопки навигации
        if (page > 0) {
            inv.setItem(48, createItem(Material.ARROW, "§a◀ Предыдущая страница"));
        }
        if (page < totalPages - 1) {
            inv.setItem(50, createItem(Material.ARROW, "§aСледующая страница ▶"));
        }

        // Кнопки управления
        inv.setItem(45, createItem(Material.EMERALD, "§a§lЗапустить рандомный"));
        inv.setItem(49, createItem(Material.NETHER_STAR, "§e§lЗапустить голосование"));

        if (plugin.getActiveEventManager().hasActiveEvent()) {
            inv.setItem(53, createItem(Material.BARRIER, "§c§lОстановить текущий ивент"));
        }

        player.openInventory(inv);
    }

    public void openVoteMenu(Player player) {
        if (!plugin.getVoteManager().isVotingActive()) {
            player.sendMessage(plugin.getConfigManager().getMessage("voting-not-active"));
            return;
        }

        List<String> votingEvents = plugin.getVoteManager().getVotingEvents();
        int size = Math.min(54, ((votingEvents.size() / 9) + 1) * 9 + 9);
        if (size < 27) size = 27;

        Inventory inv = Bukkit.createInventory(null, size, VOTE_MENU_TITLE);

        String playerVote = plugin.getVoteManager().getPlayerVote(player.getUniqueId());

        int slot = 10;
        for (String eventId : votingEvents) {
            if (slot >= size - 9) break;
            if (slot % 9 == 0 || slot % 9 == 8) {
                slot++;
                continue;
            }

            QEvent event = plugin.getEventRegistry().getEvent(eventId);
            if (event == null) continue;

            EventInfo info = plugin.getEventRegistry().getEventInfo(eventId);
            boolean voted = eventId.equals(playerVote);

            inv.setItem(slot, createVoteEventItem(event, info, voted));
            slot++;
        }

        // Информация о времени
        int remaining = plugin.getVoteManager().getRemainingSeconds();
        inv.setItem(size - 5, createItem(Material.CLOCK,
                "§eОсталось времени: §f" + remaining + " сек"));

        player.openInventory(inv);
    }

    private ItemStack createEventItem(EventInfo info) {
        ItemStack icon = info.getIcon();
        ItemMeta meta = icon.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize("&e" + info.getDisplayName()));

            List<String> lore = new ArrayList<>();
            lore.add(ColorUtil.colorize("&7" + info.getDescription()));
            lore.add("");
            lore.add(ColorUtil.colorize("&7ID: &f" + info.getId()));
            lore.add(ColorUtil.colorize("&7Статус: " + getStatusColor(info.getStatus())));

            if (info.getMinPlayers() > 0) {
                lore.add(ColorUtil.colorize("&7Мин. игроков: &f" + info.getMinPlayers()));
            }
            if (info.getMaxPlayers() > 0) {
                lore.add(ColorUtil.colorize("&7Макс. игроков: &f" + info.getMaxPlayers()));
            }

            lore.add("");
            lore.add(ColorUtil.colorize("&7Голосование: " + (info.isVotingAllowed() ? "&aДа" : "&cНет")));
            lore.add(ColorUtil.colorize("&7Рандом: " + (info.isRandomAllowed() ? "&aДа" : "&cНет")));
            lore.add("");
            lore.add(ColorUtil.colorize("&eНажмите для запуска"));

            meta.setLore(lore);
            icon.setItemMeta(meta);
        }

        return icon;
    }

    private ItemStack createVoteEventItem(QEvent event, EventInfo info, boolean voted) {
        ItemStack icon = event.getIcon().clone();
        ItemMeta meta = icon.getItemMeta();

        if (meta != null) {
            String prefix = voted ? "&a✓ " : "&e";
            meta.setDisplayName(ColorUtil.colorize(prefix + event.getDisplayName()));

            List<String> lore = new ArrayList<>();
            lore.add(ColorUtil.colorize("&7" + event.getDescription()));
            lore.add("");

            int votes = plugin.getVoteManager().getVoteCount(event.getId());
            lore.add(ColorUtil.colorize("&7Голосов: &f" + votes));

            lore.add("");
            if (voted) {
                lore.add(ColorUtil.colorize("&aВы проголосовали за этот ивент"));
            } else {
                lore.add(ColorUtil.colorize("&eНажмите, чтобы проголосовать"));
            }

            meta.setLore(lore);
            icon.setItemMeta(meta);
        }

        return icon;
    }

    private String getStatusColor(EventStatus status) {
        switch (status) {
            case ACTIVE: return "&a" + status.name();
            case AVAILABLE: return "&e" + status.name();
            case DISABLED: return "&c" + status.name();
            case UNAVAILABLE: return "&7" + status.name();
            default: return "&f" + status.name();
        }
    }

    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize(name));
            item.setItemMeta(meta);
        }
        return item;
    }
}