package wtf.pawlend.qeventmanager.manager;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import wtf.pawlend.qeventmanager.api.EventInfo;
import wtf.pawlend.qeventmanager.api.EventStatus;
import wtf.pawlend.qeventmanager.api.QEvent;
import wtf.pawlend.qeventmanager.qEventManager;

import java.util.List;
import java.util.Random;
import java.util.logging.Level;

public class ActiveEventManager {

    private final qEventManager plugin;
    private QEvent activeEvent;
    private final Random random;

    public ActiveEventManager(qEventManager plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }

    public boolean hasActiveEvent() {
        return activeEvent != null;
    }

    public QEvent getActiveEvent() {
        return activeEvent;
    }

    public boolean startEvent(String eventId) {
        if (hasActiveEvent()) {
            plugin.log(Level.WARNING, "Невозможно запустить ивент: уже есть активный ивент!");
            return false;
        }

        QEvent event = plugin.getEventRegistry().getEvent(eventId);
        if (event == null) {
            plugin.log(Level.WARNING, "Ивент '" + eventId + "' не найден!");
            return false;
        }

        EventInfo info = plugin.getEventRegistry().getEventInfo(eventId);
        if (info == null || !info.isEnabled()) {
            plugin.log(Level.WARNING, "Ивент '" + eventId + "' отключен!");
            return false;
        }

        // Проверяем возможность запуска
        try {
            if (!event.canStart()) {
                String reason = event.getCannotStartReason();
                plugin.log(Level.WARNING, "Ивент '" + eventId + "' не может быть запущен: " +
                        (reason != null ? reason : "условия не выполнены"));
                return false;
            }
        } catch (Exception e) {
            plugin.log(Level.SEVERE, "Ошибка при проверке готовности ивента '" + eventId + "': " + e.getMessage());
            return false;
        }

        // Задержка перед стартом
        int delay = plugin.getConfigManager().getMainConfig().getInt("settings.start-delay", 5);

        if (delay > 0) {
            // Отправляем сообщение о подготовке
            String prepareMsg = plugin.getConfigManager().getMessage("event-starting")
                    .replace("{event}", event.getDisplayName())
                    .replace("{seconds}", String.valueOf(delay));
            plugin.getMessageUtil().broadcast(prepareMsg);

            new BukkitRunnable() {
                @Override
                public void run() {
                    executeStart(event);
                }
            }.runTaskLater(plugin, delay * 20L);
        } else {
            executeStart(event);
        }

        return true;
    }

    private void executeStart(QEvent event) {
        try {
            activeEvent = event;
            event.start();

            EventInfo info = plugin.getEventRegistry().getEventInfo(event.getId());
            if (info != null) {
                info.setStatus(EventStatus.ACTIVE);
            }

            // Отправляем сообщение о старте
            String startMsg = plugin.getConfigManager().getMessage("event-started")
                    .replace("{event}", event.getDisplayName());
            plugin.getMessageUtil().broadcast(startMsg);

            plugin.log(Level.INFO, "Ивент '" + event.getId() + "' запущен.");

        } catch (Exception e) {
            plugin.log(Level.SEVERE, "Ошибка при запуске ивента '" + event.getId() + "': " + e.getMessage());
            e.printStackTrace();
            activeEvent = null;

            EventInfo info = plugin.getEventRegistry().getEventInfo(event.getId());
            if (info != null) {
                info.setStatus(EventStatus.AVAILABLE);
            }
        }
    }

    public boolean stopCurrentEvent() {
        if (!hasActiveEvent()) {
            return false;
        }

        try {
            activeEvent.stop();
            plugin.log(Level.INFO, "Ивент '" + activeEvent.getId() + "' остановлен.");

            // Отправляем сообщение
            String stopMsg = plugin.getConfigManager().getMessage("event-stopped")
                    .replace("{event}", activeEvent.getDisplayName());
            plugin.getMessageUtil().broadcast(stopMsg);

        } catch (Exception e) {
            plugin.log(Level.SEVERE, "Ошибка при остановке ивента: " + e.getMessage());
        }

        EventInfo info = plugin.getEventRegistry().getEventInfo(activeEvent.getId());
        if (info != null) {
            info.setStatus(EventStatus.AVAILABLE);
        }

        activeEvent = null;
        return true;
    }

    public void onEventEnd(String eventId) {
        if (activeEvent != null && activeEvent.getId().equalsIgnoreCase(eventId)) {
            EventInfo info = plugin.getEventRegistry().getEventInfo(eventId);
            if (info != null) {
                info.setStatus(EventStatus.AVAILABLE);
            }

            String endMsg = plugin.getConfigManager().getMessage("event-ended")
                    .replace("{event}", activeEvent.getDisplayName());
            plugin.getMessageUtil().broadcast(endMsg);

            plugin.log(Level.INFO, "Ивент '" + eventId + "' завершён.");
            activeEvent = null;
        }
    }

    public boolean startRandomEvent() {
        if (hasActiveEvent()) {
            plugin.log(Level.WARNING, "Невозможно запустить рандомный ивент: уже есть активный!");
            return false;
        }

        List<QEvent> eligible = plugin.getEventRegistry().getRandomEligibleEvents();

        if (eligible.isEmpty()) {
            plugin.log(Level.WARNING, "Нет доступных ивентов для рандомного запуска!");
            return false;
        }

        QEvent selected = selectWeightedRandom(eligible);

        if (selected != null) {
            plugin.log(Level.INFO, "Рандомно выбран ивент: " + selected.getId());
            return startEvent(selected.getId());
        }

        return false;
    }

    private QEvent selectWeightedRandom(List<QEvent> events) {
        if (events.isEmpty()) return null;
        if (events.size() == 1) return events.get(0);

        int totalWeight = 0;
        for (QEvent event : events) {
            EventInfo info = plugin.getEventRegistry().getEventInfo(event.getId());
            totalWeight += (info != null ? info.getWeight() : 5);
        }

        int randomValue = random.nextInt(totalWeight);
        int currentWeight = 0;

        for (QEvent event : events) {
            EventInfo info = plugin.getEventRegistry().getEventInfo(event.getId());
            currentWeight += (info != null ? info.getWeight() : 5);

            if (randomValue < currentWeight) {
                return event;
            }
        }

        return events.get(0);
    }
}