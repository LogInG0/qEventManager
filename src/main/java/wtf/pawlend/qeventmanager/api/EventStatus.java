package wtf.pawlend.qeventmanager.api;

public enum EventStatus {
    /**
     * Ивент зарегистрирован и доступен для запуска
     */
    AVAILABLE,

    /**
     * Ивент в данный момент активен
     */
    ACTIVE,

    /**
     * Ивент временно отключен
     */
    DISABLED,

    /**
     * Ивент не может быть запущен (не выполнены условия)
     */
    UNAVAILABLE
}