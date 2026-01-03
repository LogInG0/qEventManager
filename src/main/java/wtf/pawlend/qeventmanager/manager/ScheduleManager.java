package wtf.pawlend.qeventmanager.manager;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import wtf.pawlend.qeventmanager.config.Schedule;
import wtf.pawlend.qeventmanager.qEventManager;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;

public class ScheduleManager {

    private final qEventManager plugin;
    private final List<Schedule> schedules;
    private BukkitTask schedulerTask;
    private BukkitTask intervalTask;
    private final Set<String> executedOneTimeSchedules;
    private final Set<String> executedTodaySchedules;
    private int lastCheckedMinute = -1;

    public ScheduleManager(qEventManager plugin) {
        this.plugin = plugin;
        this.schedules = new ArrayList<>();
        this.executedOneTimeSchedules = new HashSet<>();
        this.executedTodaySchedules = new HashSet<>();
        loadSchedules();
    }

    public void loadSchedules() {
        schedules.clear();

        if (!plugin.getConfigManager().getSchedulesConfig().contains("schedules")) {
            return;
        }

        for (String key : plugin.getConfigManager().getSchedulesConfig().getConfigurationSection("schedules").getKeys(false)) {
            String path = "schedules." + key;

            try {
                Schedule schedule = new Schedule(
                        key,
                        plugin.getConfigManager().getSchedulesConfig().getString(path + ".type", "daily"),
                        plugin.getConfigManager().getSchedulesConfig().getString(path + ".time", "18:00"),
                        plugin.getConfigManager().getSchedulesConfig().getStringList(path + ".days"),
                        plugin.getConfigManager().getSchedulesConfig().getStringList(path + ".events"),
                        plugin.getConfigManager().getSchedulesConfig().getString(path + ".mode", "random"),
                        plugin.getConfigManager().getSchedulesConfig().getInt(path + ".min-players", 0),
                        plugin.getConfigManager().getSchedulesConfig().getInt(path + ".max-players", 0),
                        plugin.getConfigManager().getSchedulesConfig().getBoolean(path + ".enabled", true)
                );

                schedules.add(schedule);
                plugin.log(Level.INFO, "Загружено расписание: " + key);

            } catch (Exception e) {
                plugin.log(Level.WARNING, "Ошибка загрузки расписания '" + key + "': " + e.getMessage());
            }
        }
    }

    public void startScheduler() {
        // Запуск системы расписаний из schedules.yml
        startSchedulesSystem();

        // Запуск интервальной системы
        startIntervalSystem();
    }

    /**
     * Запуск системы расписаний (schedules.yml)
     */
    private void startSchedulesSystem() {
        boolean schedulesEnabled = plugin.getConfigManager().getMainConfig()
                .getBoolean("settings.schedules-enabled", true);

        if (!schedulesEnabled) {
            plugin.log(Level.INFO, "Система расписаний (schedules.yml) ОТКЛЮЧЕНА в конфиге.");
            return;
        }

        if (schedules.isEmpty()) {
            plugin.log(Level.INFO, "Нет активных расписаний в schedules.yml");
            return;
        }

        schedulerTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkSchedules();
            }
        }.runTaskTimer(plugin, 20L, 20L * 30); // Проверка каждые 30 секунд

        plugin.log(Level.INFO, "Система расписаний запущена. Загружено расписаний: " + schedules.size());
    }

    /**
     * Запуск интервальной системы (каждые N минут)
     */
    private void startIntervalSystem() {
        boolean intervalEnabled = plugin.getConfigManager().getMainConfig()
                .getBoolean("settings.interval-enabled", false);

        if (!intervalEnabled) {
            plugin.log(Level.INFO, "Интервальный режим ОТКЛЮЧЕН в конфиге.");
            return;
        }

        int intervalMinutes = plugin.getConfigManager().getMainConfig()
                .getInt("settings.interval-minutes", 15);

        if (intervalMinutes <= 0) {
            plugin.log(Level.WARNING, "Некорректный интервал: " + intervalMinutes + " минут. Интервальный режим отключен.");
            return;
        }

        long intervalTicks = intervalMinutes * 60L * 20L; // Конвертация в тики

        intervalTask = new BukkitRunnable() {
            @Override
            public void run() {
                executeIntervalEvent();
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);

        plugin.log(Level.INFO, "Интервальный режим запущен. Интервал: " + intervalMinutes + " минут.");
    }

    /**
     * Выполнение интервального ивента
     */
    private void executeIntervalEvent() {
        // Проверяем, нет ли активного ивента или голосования
        if (plugin.getActiveEventManager().hasActiveEvent()) {
            logInterval("Пропуск интервального запуска: есть активный ивент.");
            return;
        }

        if (plugin.getVoteManager().isVotingActive()) {
            logInterval("Пропуск интервального запуска: идёт голосование.");
            return;
        }

        // Проверяем минимальное количество игроков
        int minPlayers = plugin.getConfigManager().getMainConfig()
                .getInt("settings.interval-min-players", 0);
        int onlinePlayers = Bukkit.getOnlinePlayers().size();

        if (onlinePlayers < minPlayers) {
            logInterval("Пропуск интервального запуска: онлайн " + onlinePlayers +
                    ", требуется минимум " + minPlayers);
            return;
        }

        // Получаем режим
        String mode = plugin.getConfigManager().getMainConfig()
                .getString("settings.interval-mode", "RANDOM").toUpperCase();

        // Получаем список ивентов
        List<String> events = plugin.getConfigManager().getMainConfig()
                .getStringList("settings.interval-events");

        logInterval("Запуск интервального ивента. Режим: " + mode + ", Онлайн: " + onlinePlayers);

        // Определяем режим для MIXED
        if (mode.equals("MIXED")) {
            int voteThreshold = plugin.getConfigManager().getMainConfig()
                    .getInt("mixed-mode.vote-threshold", 10);
            mode = onlinePlayers >= voteThreshold ? "VOTE" : "RANDOM";
            logInterval("MIXED режим: выбран " + mode + " (порог: " + voteThreshold + ")");
        }

        // Выполняем запуск
        switch (mode) {
            case "VOTE":
            case "VOTING":
                if (events.isEmpty()) {
                    plugin.getVoteManager().startVoting(null);
                } else {
                    plugin.getVoteManager().startVoting(events);
                }
                break;

            case "RANDOM":
            default:
                if (events.isEmpty()) {
                    plugin.getActiveEventManager().startRandomEvent();
                } else {
                    // Выбираем рандомный из списка
                    String eventId = events.get(new Random().nextInt(events.size()));
                    plugin.getActiveEventManager().startEvent(eventId);
                }
                break;
        }
    }

    private void logInterval(String message) {
        if (plugin.getConfigManager().getMainConfig().getBoolean("logging.log-interval", true)) {
            plugin.log(Level.INFO, "[Interval] " + message);
        }
    }

    public void stopScheduler() {
        if (schedulerTask != null) {
            schedulerTask.cancel();
            schedulerTask = null;
            plugin.log(Level.INFO, "Система расписаний остановлена.");
        }

        if (intervalTask != null) {
            intervalTask.cancel();
            intervalTask = null;
            plugin.log(Level.INFO, "Интервальный режим остановлен.");
        }
    }

    private void checkSchedules() {
        if (plugin.getActiveEventManager().hasActiveEvent()) {
            return;
        }

        if (plugin.getVoteManager().isVotingActive()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        int currentMinute = now.getHour() * 60 + now.getMinute();

        // Сброс выполненных расписаний в начале нового дня
        if (currentMinute < lastCheckedMinute) {
            executedTodaySchedules.clear();
        }
        lastCheckedMinute = currentMinute;

        int onlinePlayers = Bukkit.getOnlinePlayers().size();

        for (Schedule schedule : schedules) {
            if (!schedule.isEnabled()) continue;

            // Пропускаем уже выполненные сегодня
            String scheduleKey = schedule.getName() + "_" + now.toLocalDate();
            if (executedTodaySchedules.contains(scheduleKey)) continue;

            if (shouldExecute(schedule, now, onlinePlayers)) {
                executedTodaySchedules.add(scheduleKey);
                executeSchedule(schedule);
                break; // Выполняем только одно расписание за проверку
            }
        }
    }

    private boolean shouldExecute(Schedule schedule, LocalDateTime now, int onlinePlayers) {
        // Проверка игроков
        if (schedule.getMinPlayers() > 0 && onlinePlayers < schedule.getMinPlayers()) {
            return false;
        }
        if (schedule.getMaxPlayers() > 0 && onlinePlayers > schedule.getMaxPlayers()) {
            return false;
        }

        // Парсим время
        LocalTime scheduleTime;
        try {
            scheduleTime = LocalTime.parse(schedule.getTime(), DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            return false;
        }

        // Проверяем время (с погрешностью 30 секунд)
        LocalTime currentTime = now.toLocalTime();
        if (Math.abs(currentTime.toSecondOfDay() - scheduleTime.toSecondOfDay()) > 30) {
            return false;
        }

        // Проверка типа расписания
        switch (schedule.getType().toLowerCase()) {
            case "daily":
                return true;

            case "weekly":
                DayOfWeek today = now.getDayOfWeek();
                for (String day : schedule.getDays()) {
                    try {
                        if (DayOfWeek.valueOf(day.toUpperCase()) == today) {
                            return true;
                        }
                    } catch (Exception ignored) {}
                }
                return false;

            case "once":
                if (executedOneTimeSchedules.contains(schedule.getName())) {
                    return false;
                }
                return true;

            default:
                return false;
        }
    }

    private void executeSchedule(Schedule schedule) {
        plugin.log(Level.INFO, "Выполняется расписание: " + schedule.getName());

        if (schedule.getType().equalsIgnoreCase("once")) {
            executedOneTimeSchedules.add(schedule.getName());
        }

        List<String> events = schedule.getEvents();
        String mode = schedule.getMode();

        // Обработка MIXED режима
        if (mode.equalsIgnoreCase("mixed")) {
            int onlinePlayers = Bukkit.getOnlinePlayers().size();
            int voteThreshold = plugin.getConfigManager().getMainConfig()
                    .getInt("mixed-mode.vote-threshold", 10);
            mode = onlinePlayers >= voteThreshold ? "vote" : "random";
        }

        switch (mode.toLowerCase()) {
            case "vote":
            case "voting":
                if (events.isEmpty()) {
                    plugin.getVoteManager().startVoting(null);
                } else {
                    plugin.getVoteManager().startVoting(events);
                }
                break;

            case "random":
            default:
                if (events.isEmpty()) {
                    plugin.getActiveEventManager().startRandomEvent();
                } else {
                    // Выбираем рандомный из списка
                    String eventId = events.get(new Random().nextInt(events.size()));
                    plugin.getActiveEventManager().startEvent(eventId);
                }
                break;
        }
    }

    public List<Schedule> getSchedules() {
        return new ArrayList<>(schedules);
    }

    /**
     * Проверить, включена ли система расписаний
     */
    public boolean isSchedulesEnabled() {
        return plugin.getConfigManager().getMainConfig()
                .getBoolean("settings.schedules-enabled", true);
    }

    /**
     * Проверить, включен ли интервальный режим
     */
    public boolean isIntervalEnabled() {
        return plugin.getConfigManager().getMainConfig()
                .getBoolean("settings.interval-enabled", false);
    }

    /**
     * Получить интервал в минутах
     */
    public int getIntervalMinutes() {
        return plugin.getConfigManager().getMainConfig()
                .getInt("settings.interval-minutes", 15);
    }

    /**
     * Принудительно запустить интервальный ивент
     */
    public void forceIntervalEvent() {
        executeIntervalEvent();
    }
}