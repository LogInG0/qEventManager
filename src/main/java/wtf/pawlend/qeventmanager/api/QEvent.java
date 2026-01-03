package wtf.pawlend.qeventmanager.api;

import org.bukkit.inventory.ItemStack;

/**
 * Интерфейс для реализации игровых ивентов.
 * Сторонние плагины должны реализовать этот интерфейс для интеграции с qEventManager.
 */
public interface QEvent {

    /**
     * Получить уникальный идентификатор ивента
     * @return уникальный ID (например, "hermit", "parkour")
     */
    String getId();

    /**
     * Получить отображаемое название ивента
     * @return название для отображения в меню и сообщениях
     */
    String getDisplayName();

    /**
     * Получить краткое описание ивента
     * @return описание ивента
     */
    String getDescription();

    /**
     * Получить иконку для меню
     * @return ItemStack для отображения в GUI
     */
    ItemStack getIcon();

    /**
     * Получить минимальное количество игроков для запуска
     * @return минимум игроков (0 если нет ограничений)
     */
    default int getMinPlayers() {
        return 0;
    }

    /**
     * Получить максимальное количество игроков
     * @return максимум игроков (0 если нет ограничений)
     */
    default int getMaxPlayers() {
        return 0;
    }

    /**
     * Получить приоритет/вес при рандомном выборе
     * @return приоритет ивента
     */
    default EventPriority getPriority() {
        return EventPriority.NORMAL;
    }

    /**
     * Получить кастомный вес (переопределяет приоритет)
     * @return вес от 1 до 100, или -1 для использования приоритета
     */
    default int getCustomWeight() {
        return -1;
    }

    /**
     * Разрешено ли участие в голосовании
     * @return true если ивент может быть выбран голосованием
     */
    default boolean isVotingAllowed() {
        return true;
    }

    /**
     * Разрешен ли рандомный запуск
     * @return true если ивент может быть выбран случайно
     */
    default boolean isRandomAllowed() {
        return true;
    }

    /**
     * Проверка готовности ивента к запуску
     * @return true если все условия выполнены
     */
    boolean canStart();

    /**
     * Получить причину, почему ивент не может быть запущен
     * @return текст причины или null если canStart() == true
     */
    default String getCannotStartReason() {
        return null;
    }

    /**
     * Запустить ивент
     * Вызывается менеджером при старте ивента
     */
    void start();

    /**
     * Остановить ивент
     * Вызывается менеджером при принудительной остановке
     */
    void stop();

    /**
     * Вызывается когда ивент завершился естественным образом
     * Сторонний плагин должен вызвать этот метод самостоятельно
     */
    default void onEventEnd() {
        // По умолчанию ничего не делаем
    }

    /**
     * Получить плагин-владелец ивента
     * @return имя плагина
     */
    String getOwnerPlugin();
}