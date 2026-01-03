package wtf.pawlend.qeventmanager.api;

import wtf.pawlend.qeventmanager.qEventManager;

import java.util.*;
import java.util.logging.Level;

/**
 * Публичный API для регистрации и управления ивентами
 */
public class QEventAPI {

    private final qEventManager plugin;

    public QEventAPI(qEventManager plugin) {
        this.plugin = plugin;
    }

    /**
     * Зарегистрировать новый ивент
     * @param event реализация QEvent
     * @return true если регистрация успешна
     */
    public boolean registerEvent(QEvent event) {
        if (event == null) {
            plugin.log(Level.WARNING, "Попытка зарегистрировать null ивент!");
            return false;
        }

        if (event.getId() == null || event.getId().isEmpty()) {
            plugin.log(Level.WARNING, "Попытка зарегистрировать ивент без ID!");
            return false;
        }

        return plugin.getEventRegistry().registerEvent(event);
    }

    /**
     * Отменить регистрацию ивента
     * @param eventId ID ивента
     * @return true если успешно
     */
    public boolean unregisterEvent(String eventId) {
        return plugin.getEventRegistry().unregisterEvent(eventId);
    }

    /**
     * Получить информацию о зарегистрированном ивенте
     * @param eventId ID ивента
     * @return EventInfo или null
     */
    public EventInfo getEventInfo(String eventId) {
        return plugin.getEventRegistry().getEventInfo(eventId);
    }

    /**
     * Получить список всех зарегистрированных ивентов
     * @return неизменяемый список
     */
    public List<EventInfo> getRegisteredEvents() {
        return plugin.getEventRegistry().getAllEventInfo();
    }

    /**
     * Получить текущий активный ивент
     * @return QEvent или null
     */
    public QEvent getActiveEvent() {
        return plugin.getActiveEventManager().getActiveEvent();
    }

    /**
     * Проверить, активен ли какой-либо ивент
     * @return true если есть активный ивент
     */
    public boolean hasActiveEvent() {
        return plugin.getActiveEventManager().hasActiveEvent();
    }

    /**
     * Запустить конкретный ивент
     * @param eventId ID ивента
     * @return true если запуск успешен
     */
    public boolean startEvent(String eventId) {
        return plugin.getActiveEventManager().startEvent(eventId);
    }

    /**
     * Остановить текущий активный ивент
     * @return true если остановлен
     */
    public boolean stopCurrentEvent() {
        return plugin.getActiveEventManager().stopCurrentEvent();
    }

    /**
     * Запустить случайный ивент
     * @return true если запуск успешен
     */
    public boolean startRandomEvent() {
        return plugin.getActiveEventManager().startRandomEvent();
    }

    /**
     * Запустить голосование
     * @param eventIds список ID ивентов для голосования (null = все доступные)
     * @return true если голосование запущено
     */
    public boolean startVoting(List<String> eventIds) {
        return plugin.getVoteManager().startVoting(eventIds);
    }

    /**
     * Проверить, идёт ли голосование
     * @return true если активно
     */
    public boolean isVotingActive() {
        return plugin.getVoteManager().isVotingActive();
    }

    /**
     * Уведомить менеджер о завершении ивента
     * Должно вызываться сторонним плагином при естественном завершении ивента
     * @param eventId ID завершившегося ивента
     */
    public void notifyEventEnd(String eventId) {
        plugin.getActiveEventManager().onEventEnd(eventId);
    }

    /**
     * Включить/выключить ивент
     * @param eventId ID ивента
     * @param enabled состояние
     */
    public void setEventEnabled(String eventId, boolean enabled) {
        EventInfo info = plugin.getEventRegistry().getEventInfo(eventId);
        if (info != null) {
            info.setEnabled(enabled);
        }
    }
}