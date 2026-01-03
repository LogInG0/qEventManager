package wtf.pawlend.qeventmanager.manager;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import wtf.pawlend.qeventmanager.api.EventInfo;
import wtf.pawlend.qeventmanager.api.QEvent;
import wtf.pawlend.qeventmanager.qEventManager;
import wtf.pawlend.qeventmanager.util.ColorUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class VoteManager {

    private final qEventManager plugin;
    private final Map<UUID, String> votes;
    private final Map<String, Integer> voteCounts;
    private List<String> votingEvents;
    private boolean votingActive;
    private BukkitTask votingTask;
    private int remainingSeconds;

    public VoteManager(qEventManager plugin) {
        this.plugin = plugin;
        this.votes = new ConcurrentHashMap<>();
        this.voteCounts = new ConcurrentHashMap<>();
        this.votingEvents = new ArrayList<>();
        this.votingActive = false;
    }

    public boolean startVoting(List<String> eventIds) {
        if (votingActive) {
            plugin.log(Level.WARNING, "Голосование уже активно!");
            return false;
        }

        if (plugin.getActiveEventManager().hasActiveEvent()) {
            plugin.log(Level.WARNING, "Невозможно начать голосование: есть активный ивент!");
            return false;
        }

        // Определяем список ивентов для голосования
        List<QEvent> eligibleEvents;
        if (eventIds != null && !eventIds.isEmpty()) {
            eligibleEvents = eventIds.stream()
                    .map(id -> plugin.getEventRegistry().getEvent(id))
                    .filter(Objects::nonNull)
                    .filter(e -> {
                        EventInfo info = plugin.getEventRegistry().getEventInfo(e.getId());
                        return info != null && info.isEnabled() && info.isVotingAllowed();
                    })
                    .collect(Collectors.toList());
        } else {
            eligibleEvents = plugin.getEventRegistry().getVotingEligibleEvents();
        }

        if (eligibleEvents.size() < 2) {
            plugin.log(Level.WARNING, "Недостаточно ивентов для голосования (минимум 2)!");
            return false;
        }

        // Очищаем предыдущие данные
        votes.clear();
        voteCounts.clear();
        votingEvents.clear();

        // Заполняем список ивентов
        for (QEvent event : eligibleEvents) {
            votingEvents.add(event.getId());
            voteCounts.put(event.getId(), 0);
        }

        // Рассчитываем время голосования
        int baseDuration = plugin.getConfigManager().getMainConfig().getInt("voting.base-duration", 15);
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        remainingSeconds = calculateVotingDuration(baseDuration, onlinePlayers);

        votingActive = true;

        // Отправляем сообщение о начале голосования
        sendVotingStartMessage();

        // Запускаем таймер
        startVotingTimer();

        plugin.log(Level.INFO, "Голосование запущено. Время: " + remainingSeconds + " сек. Ивентов: " + eligibleEvents.size());

        return true;
    }

    private int calculateVotingDuration(int baseDuration, int onlinePlayers) {
        String formula = plugin.getConfigManager().getMainConfig().getString("voting.duration-formula", "base");

        if (formula.equals("scaled")) {
            // Ступенчатое увеличение
            if (onlinePlayers > 50) return baseDuration + 15;
            if (onlinePlayers > 30) return baseDuration + 10;
            if (onlinePlayers > 15) return baseDuration + 5;
        } else if (formula.equals("linear")) {
            // Линейное увеличение
            return baseDuration + (onlinePlayers / 10) * 2;
        }

        return baseDuration;
    }

    private void sendVotingStartMessage() {
        String message = plugin.getConfigManager().getMessage("voting-started");
        String buttonText = plugin.getConfigManager().getMessage("voting-button");
        String buttonHover = plugin.getConfigManager().getMessage("voting-button-hover");

        TextComponent textComponent = new TextComponent(ColorUtil.colorize(message + " "));

        TextComponent button = new TextComponent(ColorUtil.colorize(buttonText));
        button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/qevent vote"));
        button.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(ColorUtil.colorize(buttonHover)).create()));

        textComponent.addExtra(button);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("qeventmanager.vote")) {
                player.spigot().sendMessage(textComponent);
            }
        }
    }

    private void startVotingTimer() {
        votingTask = new BukkitRunnable() {
            @Override
            public void run() {
                remainingSeconds--;

                if (remainingSeconds <= 0) {
                    endVoting();
                    cancel();
                    return;
                }

                // Уведомления о времени
                if (remainingSeconds == 10 || remainingSeconds == 5 || remainingSeconds <= 3) {
                    String timeMsg = plugin.getConfigManager().getMessage("voting-time-remaining")
                            .replace("{seconds}", String.valueOf(remainingSeconds));
                    plugin.getMessageUtil().broadcast(timeMsg);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void endVoting() {
        if (!votingActive) return;

        votingActive = false;

        if (votingTask != null) {
            votingTask.cancel();
            votingTask = null;
        }

        // Подсчитываем голоса
        String winnerId = determineWinner();

        if (winnerId != null) {
            QEvent winner = plugin.getEventRegistry().getEvent(winnerId);

            // Логируем результаты
            logVotingResults(winnerId);

            // Отправляем сообщение
            String winMsg = plugin.getConfigManager().getMessage("voting-winner")
                    .replace("{event}", winner.getDisplayName())
                    .replace("{votes}", String.valueOf(voteCounts.getOrDefault(winnerId, 0)));
            plugin.getMessageUtil().broadcast(winMsg);

            // Запускаем победивший ивент
            plugin.getActiveEventManager().startEvent(winnerId);
        } else {
            // Нет голосов - обрабатываем по конфигу
            handleNoVotes();
        }

        // Очищаем данные
        votes.clear();
        voteCounts.clear();
        votingEvents.clear();
    }

    private String determineWinner() {
        if (voteCounts.isEmpty()) return null;

        int maxVotes = voteCounts.values().stream().mapToInt(Integer::intValue).max().orElse(0);

        if (maxVotes == 0) return null;

        List<String> leaders = voteCounts.entrySet().stream()
                .filter(e -> e.getValue() == maxVotes)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (leaders.size() == 1) {
            return leaders.get(0);
        }

        // При равенстве - случайный выбор
        return leaders.get(new Random().nextInt(leaders.size()));
    }

    private void handleNoVotes() {
        String behavior = plugin.getConfigManager().getMainConfig().getString("voting.no-votes-behavior", "random");

        if (behavior.equals("random")) {
            String msg = plugin.getConfigManager().getMessage("voting-no-votes-random");
            plugin.getMessageUtil().broadcast(msg);
            plugin.getActiveEventManager().startRandomEvent();
        } else if (behavior.equals("cancel")) {
            String msg = plugin.getConfigManager().getMessage("voting-no-votes-cancelled");
            plugin.getMessageUtil().broadcast(msg);
        }
    }

    private void logVotingResults(String winnerId) {
        plugin.log(Level.INFO, "=== Результаты голосования ===");

        voteCounts.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .forEach(entry -> {
                    QEvent event = plugin.getEventRegistry().getEvent(entry.getKey());
                    String name = event != null ? event.getDisplayName() : entry.getKey();
                    plugin.log(Level.INFO, name + ": " + entry.getValue() + " голосов" +
                            (entry.getKey().equals(winnerId) ? " (ПОБЕДИТЕЛЬ)" : ""));
                });

        plugin.log(Level.INFO, "Всего проголосовало: " + votes.size() + " игроков");
        plugin.log(Level.INFO, "==============================");
    }

    public void vote(Player player, String eventId) {
        if (!votingActive) {
            String msg = plugin.getConfigManager().getMessage("voting-not-active");
            player.sendMessage(ColorUtil.colorize(msg));
            return;
        }

        if (!votingEvents.contains(eventId.toLowerCase())) {
            String msg = plugin.getConfigManager().getMessage("voting-invalid-event");
            player.sendMessage(ColorUtil.colorize(msg));
            return;
        }

        String previousVote = votes.get(player.getUniqueId());
        boolean canChange = plugin.getConfigManager().getMainConfig().getBoolean("voting.allow-change", true);

        if (previousVote != null && !canChange) {
            String msg = plugin.getConfigManager().getMessage("voting-already-voted");
            player.sendMessage(ColorUtil.colorize(msg));
            return;
        }

        // Убираем предыдущий голос
        if (previousVote != null) {
            voteCounts.put(previousVote, voteCounts.getOrDefault(previousVote, 1) - 1);
        }

        // Добавляем новый голос
        votes.put(player.getUniqueId(), eventId.toLowerCase());
        voteCounts.put(eventId.toLowerCase(), voteCounts.getOrDefault(eventId.toLowerCase(), 0) + 1);

        QEvent event = plugin.getEventRegistry().getEvent(eventId);
        String eventName = event != null ? event.getDisplayName() : eventId;

        String msg = plugin.getConfigManager().getMessage("voting-vote-cast")
                .replace("{event}", eventName);
        player.sendMessage(ColorUtil.colorize(msg));
    }

    public void cancelVoting() {
        if (!votingActive) return;

        votingActive = false;

        if (votingTask != null) {
            votingTask.cancel();
            votingTask = null;
        }

        votes.clear();
        voteCounts.clear();
        votingEvents.clear();

        String msg = plugin.getConfigManager().getMessage("voting-cancelled");
        plugin.getMessageUtil().broadcast(msg);

        plugin.log(Level.INFO, "Голосование отменено.");
    }

    public boolean isVotingActive() {
        return votingActive;
    }

    public List<String> getVotingEvents() {
        return new ArrayList<>(votingEvents);
    }

    public String getPlayerVote(UUID playerId) {
        return votes.get(playerId);
    }

    public int getVoteCount(String eventId) {
        return voteCounts.getOrDefault(eventId.toLowerCase(), 0);
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    public int getTotalVotes() {
        return votes.size();
    }
}