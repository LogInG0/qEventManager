package wtf.pawlend.qeventmanager.manager;

import wtf.pawlend.qeventmanager.api.EventInfo;
import wtf.pawlend.qeventmanager.api.EventStatus;
import wtf.pawlend.qeventmanager.api.QEvent;
import wtf.pawlend.qeventmanager.qEventManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class EventRegistry {

    private final qEventManager plugin;
    private final Map<String, QEvent> events;
    private final Map<String, EventInfo> eventInfos;

    public EventRegistry(qEventManager plugin) {
        this.plugin = plugin;
        this.events = new ConcurrentHashMap<>();
        this.eventInfos = new ConcurrentHashMap<>();
    }

    public boolean registerEvent(QEvent event) {
        String id = event.getId().toLowerCase();

        if (events.containsKey(id)) {
            plugin.log(Level.WARNING, "Ивент с ID '" + id + "' уже зарегистрирован!");
            return false;
        }

        events.put(id, event);
        eventInfos.put(id, new EventInfo(event));

        // Применяем настройки из конфига events.yml
        applyConfigSettings(id);

        plugin.log(Level.INFO, "Зарегистрирован ивент: " + event.getDisplayName() +
                " (ID: " + id + ") от плагина " + event.getOwnerPlugin());

        return true;
    }

    public boolean unregisterEvent(String eventId) {
        String id = eventId.toLowerCase();

        if (!events.containsKey(id)) {
            return false;
        }

        QEvent event = events.remove(id);
        eventInfos.remove(id);

        plugin.log(Level.INFO, "Ивент '" + id + "' удалён из реестра.");

        return true;
    }

    public QEvent getEvent(String eventId) {
        return events.get(eventId.toLowerCase());
    }

    public EventInfo getEventInfo(String eventId) {
        return eventInfos.get(eventId.toLowerCase());
    }

    public List<QEvent> getAllEvents() {
        return new ArrayList<>(events.values());
    }

    public List<EventInfo> getAllEventInfo() {
        return new ArrayList<>(eventInfos.values());
    }

    public List<QEvent> getAvailableEvents() {
        int onlinePlayers = plugin.getServer().getOnlinePlayers().size();

        return events.values().stream()
                .filter(e -> {
                    EventInfo info = eventInfos.get(e.getId().toLowerCase());
                    if (info == null || !info.isEnabled()) return false;
                    if (info.getStatus() == EventStatus.DISABLED) return false;
                    if (info.getMinPlayers() > 0 && onlinePlayers < info.getMinPlayers()) return false;
                    if (info.getMaxPlayers() > 0 && onlinePlayers > info.getMaxPlayers()) return false;
                    return true;
                })
                .collect(Collectors.toList());
    }

    public List<QEvent> getVotingEligibleEvents() {
        return getAvailableEvents().stream()
                .filter(e -> {
                    EventInfo info = eventInfos.get(e.getId().toLowerCase());
                    return info != null && info.isVotingAllowed();
                })
                .collect(Collectors.toList());
    }

    public List<QEvent> getRandomEligibleEvents() {
        return getAvailableEvents().stream()
                .filter(e -> {
                    EventInfo info = eventInfos.get(e.getId().toLowerCase());
                    return info != null && info.isRandomAllowed();
                })
                .collect(Collectors.toList());
    }

    public boolean isRegistered(String eventId) {
        return events.containsKey(eventId.toLowerCase());
    }

    public int getRegisteredCount() {
        return events.size();
    }

    private void applyConfigSettings(String eventId) {
        // Применяем настройки из events.yml если они есть
        if (plugin.getConfigManager().getEventsConfig().contains("events." + eventId)) {
            String path = "events." + eventId;
            EventInfo info = eventInfos.get(eventId);

            if (plugin.getConfigManager().getEventsConfig().contains(path + ".enabled")) {
                info.setEnabled(plugin.getConfigManager().getEventsConfig().getBoolean(path + ".enabled", true));
            }
        }
    }
}