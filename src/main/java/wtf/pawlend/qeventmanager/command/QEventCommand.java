package wtf.pawlend.qeventmanager.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import wtf.pawlend.qeventmanager.api.EventInfo;
import wtf.pawlend.qeventmanager.api.QEvent;
import wtf.pawlend.qeventmanager.config.Schedule;
import wtf.pawlend.qeventmanager.qEventManager;
import wtf.pawlend.qeventmanager.util.ColorUtil;

import java.util.List;

public class QEventCommand implements CommandExecutor {

    private final qEventManager plugin;

    public QEventCommand(qEventManager plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player && sender.hasPermission("qeventmanager.menu")) {
                plugin.getGuiManager().openMainMenu((Player) sender);
            } else {
                sendHelp(sender);
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "help":
                sendHelp(sender);
                break;

            case "list":
                if (!hasPermission(sender, "qeventmanager.admin")) return true;
                listEvents(sender);
                break;

            case "vote":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ColorUtil.colorize("&cТолько для игроков!"));
                    return true;
                }
                if (!hasPermission(sender, "qeventmanager.vote")) return true;
                handleVote((Player) sender, args);
                break;

            case "start":
                if (!hasPermission(sender, "qeventmanager.start")) return true;
                handleStart(sender, args);
                break;

            case "stop":
                if (!hasPermission(sender, "qeventmanager.stop")) return true;
                handleStop(sender);
                break;

            case "rand":
            case "random":
                if (!hasPermission(sender, "qeventmanager.start")) return true;
                handleRandom(sender);
                break;

            case "startvote":
                if (!hasPermission(sender, "qeventmanager.admin")) return true;
                handleStartVote(sender);
                break;

            case "reload":
                if (!hasPermission(sender, "qeventmanager.reload")) return true;
                handleReload(sender);
                break;

            case "status":
                if (!hasPermission(sender, "qeventmanager.admin")) return true;
                showStatus(sender);
                break;

            case "schedules":
                if (!hasPermission(sender, "qeventmanager.admin")) return true;
                showSchedules(sender);
                break;

            case "forceinterval":
                if (!hasPermission(sender, "qeventmanager.admin")) return true;
                handleForceInterval(sender);
                break;

            default:
                sender.sendMessage(ColorUtil.colorize("&cНеизвестная команда. Используйте /qevent help"));
                break;
        }

        return true;
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission) && !sender.hasPermission("qeventmanager.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return false;
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ColorUtil.colorize("&6=== qEventManager Помощь ==="));
        sender.sendMessage(ColorUtil.colorize("&e/qevent &7- Открыть главное меню"));
        sender.sendMessage(ColorUtil.colorize("&e/qevent list &7- Список ивентов"));
        sender.sendMessage(ColorUtil.colorize("&e/qevent vote &7- Меню голосования"));
        sender.sendMessage(ColorUtil.colorize("&e/qevent start <id> &7- Запустить ивент"));
        sender.sendMessage(ColorUtil.colorize("&e/qevent stop &7- Остановить ивент"));
        sender.sendMessage(ColorUtil.colorize("&e/qevent rand &7- Случайный ивент"));
        sender.sendMessage(ColorUtil.colorize("&e/qevent startvote &7- Начать голосование"));
        sender.sendMessage(ColorUtil.colorize("&e/qevent status &7- Статус системы"));
        sender.sendMessage(ColorUtil.colorize("&e/qevent schedules &7- Список расписаний"));
        sender.sendMessage(ColorUtil.colorize("&e/qevent forceinterval &7- Принудительный интервальный запуск"));
        sender.sendMessage(ColorUtil.colorize("&e/qevent reload &7- Перезагрузить конфиги"));
    }

    private void listEvents(CommandSender sender) {
        List<EventInfo> events = plugin.getEventRegistry().getAllEventInfo();

        if (events.isEmpty()) {
            sender.sendMessage(ColorUtil.colorize("&7Нет зарегистрированных ивентов."));
            return;
        }

        sender.sendMessage(ColorUtil.colorize("&6=== Зарегистрированные ивенты ==="));

        for (EventInfo info : events) {
            String status = info.isEnabled() ? "&a●" : "&c●";
            sender.sendMessage(ColorUtil.colorize(status + " &e" + info.getId() + " &7- " + info.getDisplayName()));
        }
    }

    private void handleVote(Player player, String[] args) {
        if (!plugin.getVoteManager().isVotingActive()) {
            player.sendMessage(plugin.getConfigManager().getMessage("voting-not-active"));
            return;
        }

        if (args.length > 1) {
            plugin.getVoteManager().vote(player, args[1]);
        } else {
            plugin.getGuiManager().openVoteMenu(player);
        }
    }

    private void handleStart(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.colorize("&cИспользование: /qevent start <id>"));
            return;
        }

        String eventId = args[1];

        if (!plugin.getEventRegistry().isRegistered(eventId)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("event-not-found")
                    .replace("{id}", eventId));
            return;
        }

        if (plugin.getActiveEventManager().startEvent(eventId)) {
            sender.sendMessage(ColorUtil.colorize("&aИвент запускается..."));
        } else {
            sender.sendMessage(ColorUtil.colorize("&cНе удалось запустить ивент."));
        }
    }

    private void handleStop(CommandSender sender) {
        if (!plugin.getActiveEventManager().hasActiveEvent()) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-active-event"));
            return;
        }

        if (plugin.getActiveEventManager().stopCurrentEvent()) {
            sender.sendMessage(ColorUtil.colorize("&aИвент остановлен."));
        } else {
            sender.sendMessage(ColorUtil.colorize("&cОшибка при остановке ивента."));
        }
    }

    private void handleRandom(CommandSender sender) {
        if (plugin.getActiveEventManager().startRandomEvent()) {
            sender.sendMessage(ColorUtil.colorize("&aЗапуск случайного ивента..."));
        } else {
            sender.sendMessage(ColorUtil.colorize("&cНе удалось запустить случайный ивент."));
        }
    }

    private void handleStartVote(CommandSender sender) {
        if (plugin.getVoteManager().startVoting(null)) {
            sender.sendMessage(ColorUtil.colorize("&aГолосование запущено!"));
        } else {
            sender.sendMessage(ColorUtil.colorize("&cНе удалось запустить голосование."));
        }
    }

    private void handleReload(CommandSender sender) {
        plugin.reload();
        sender.sendMessage(ColorUtil.colorize("&aКонфигурация перезагружена!"));
    }

    private void showStatus(CommandSender sender) {
        sender.sendMessage(ColorUtil.colorize("&6=== Статус qEventManager ==="));
        sender.sendMessage(ColorUtil.colorize("&7Зарегистрировано ивентов: &e" + plugin.getEventRegistry().getRegisteredCount()));

        // Активный ивент
        if (plugin.getActiveEventManager().hasActiveEvent()) {
            QEvent active = plugin.getActiveEventManager().getActiveEvent();
            sender.sendMessage(ColorUtil.colorize("&7Активный ивент: &a" + active.getDisplayName()));
        } else {
            sender.sendMessage(ColorUtil.colorize("&7Активный ивент: &cнет"));
        }

        // Голосование
        if (plugin.getVoteManager().isVotingActive()) {
            sender.sendMessage(ColorUtil.colorize("&7Голосование: &aактивно &7(" +
                    plugin.getVoteManager().getRemainingSeconds() + " сек)"));
            sender.sendMessage(ColorUtil.colorize("&7Проголосовало: &e" + plugin.getVoteManager().getTotalVotes()));
        } else {
            sender.sendMessage(ColorUtil.colorize("&7Голосование: &cнеактивно"));
        }

        // Система расписаний
        sender.sendMessage(ColorUtil.colorize("&6--- Расписания ---"));

        boolean schedulesEnabled = plugin.getScheduleManager().isSchedulesEnabled();
        sender.sendMessage(ColorUtil.colorize("&7Расписания (schedules.yml): " +
                (schedulesEnabled ? "&aВКЛ" : "&cВЫКЛ")));

        if (schedulesEnabled) {
            sender.sendMessage(ColorUtil.colorize("&7Загружено расписаний: &e" +
                    plugin.getScheduleManager().getSchedules().size()));
        }

        // Интервальный режим
        boolean intervalEnabled = plugin.getScheduleManager().isIntervalEnabled();
        sender.sendMessage(ColorUtil.colorize("&7Интервальный режим: " +
                (intervalEnabled ? "&aВКЛ" : "&cВЫКЛ")));

        if (intervalEnabled) {
            int interval = plugin.getScheduleManager().getIntervalMinutes();
            String mode = plugin.getConfigManager().getMainConfig()
                    .getString("settings.interval-mode", "RANDOM");
            sender.sendMessage(ColorUtil.colorize("&7  Интервал: &e" + interval + " мин"));
            sender.sendMessage(ColorUtil.colorize("&7  Режим: &e" + mode));
        }
    }

    private void showSchedules(CommandSender sender) {
        sender.sendMessage(ColorUtil.colorize("&6=== Расписания ==="));

        // Статус систем
        boolean schedulesEnabled = plugin.getScheduleManager().isSchedulesEnabled();
        boolean intervalEnabled = plugin.getScheduleManager().isIntervalEnabled();

        sender.sendMessage(ColorUtil.colorize("&7Система расписаний: " +
                (schedulesEnabled ? "&aВКЛ" : "&cВЫКЛ")));
        sender.sendMessage(ColorUtil.colorize("&7Интервальный режим: " +
                (intervalEnabled ? "&aВКЛ" : "&cВЫКЛ")));

        if (intervalEnabled) {
            int interval = plugin.getScheduleManager().getIntervalMinutes();
            String mode = plugin.getConfigManager().getMainConfig()
                    .getString("settings.interval-mode", "RANDOM");
            int minPlayers = plugin.getConfigManager().getMainConfig()
                    .getInt("settings.interval-min-players", 0);

            sender.sendMessage(ColorUtil.colorize("&e  Интервал: &f" + interval + " мин"));
            sender.sendMessage(ColorUtil.colorize("&e  Режим: &f" + mode));
            sender.sendMessage(ColorUtil.colorize("&e  Мин. игроков: &f" + minPlayers));
        }

        sender.sendMessage("");

        // Список расписаний из schedules.yml
        List<Schedule> schedules = plugin.getScheduleManager().getSchedules();

        if (schedules.isEmpty()) {
            sender.sendMessage(ColorUtil.colorize("&7Нет расписаний в schedules.yml"));
        } else {
            sender.sendMessage(ColorUtil.colorize("&6Расписания из schedules.yml:"));
            for (Schedule schedule : schedules) {
                String status = schedule.isEnabled() ? "&a●" : "&c●";
                sender.sendMessage(ColorUtil.colorize(status + " &e" + schedule.getName()));
                sender.sendMessage(ColorUtil.colorize("  &7Тип: &f" + schedule.getType() +
                        " &7| Время: &f" + schedule.getTime() +
                        " &7| Режим: &f" + schedule.getMode()));

                if (schedule.getMinPlayers() > 0) {
                    sender.sendMessage(ColorUtil.colorize("  &7Мин. игроков: &f" + schedule.getMinPlayers()));
                }
            }
        }
    }

    private void handleForceInterval(CommandSender sender) {
        if (!plugin.getScheduleManager().isIntervalEnabled()) {
            sender.sendMessage(ColorUtil.colorize("&cИнтервальный режим отключен в конфиге!"));
            return;
        }

        sender.sendMessage(ColorUtil.colorize("&aПринудительный запуск интервального ивента..."));
        plugin.getScheduleManager().forceIntervalEvent();
    }
}