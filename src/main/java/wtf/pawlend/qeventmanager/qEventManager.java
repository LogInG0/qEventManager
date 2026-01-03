package wtf.pawlend.qeventmanager;

import org.bukkit.plugin.java.JavaPlugin;
import wtf.pawlend.qeventmanager.api.QEventAPI;
import wtf.pawlend.qeventmanager.command.QEventCommand;
import wtf.pawlend.qeventmanager.command.QEventTabCompleter;
import wtf.pawlend.qeventmanager.config.ConfigManager;
import wtf.pawlend.qeventmanager.gui.GUIManager;
import wtf.pawlend.qeventmanager.listener.GUIListener;
import wtf.pawlend.qeventmanager.manager.ActiveEventManager;
import wtf.pawlend.qeventmanager.manager.EventRegistry;
import wtf.pawlend.qeventmanager.manager.ScheduleManager;
import wtf.pawlend.qeventmanager.manager.VoteManager;
import wtf.pawlend.qeventmanager.util.MessageUtil;

import java.util.logging.Level;

public class qEventManager extends JavaPlugin {

    private static qEventManager instance;

    private ConfigManager configManager;
    private EventRegistry eventRegistry;
    private ScheduleManager scheduleManager;
    private VoteManager voteManager;
    private ActiveEventManager activeEventManager;
    private GUIManager guiManager;
    private MessageUtil messageUtil;
    private QEventAPI api;

    @Override
    public void onEnable() {
        instance = this;

        // Инициализация конфигов
        this.configManager = new ConfigManager(this);
        configManager.loadAllConfigs();

        // Инициализация утилит
        this.messageUtil = new MessageUtil(this);

        // Инициализация менеджеров
        this.eventRegistry = new EventRegistry(this);
        this.activeEventManager = new ActiveEventManager(this);
        this.voteManager = new VoteManager(this);
        this.scheduleManager = new ScheduleManager(this);
        this.guiManager = new GUIManager(this);

        // Инициализация API
        this.api = new QEventAPI(this);

        // Регистрация команд
        QEventCommand commandExecutor = new QEventCommand(this);
        getCommand("qevent").setExecutor(commandExecutor);
        getCommand("qevent").setTabCompleter(new QEventTabCompleter(this));

        // Регистрация слушателей
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);

        // Запуск планировщика
        scheduleManager.startScheduler();

        log(Level.INFO, "qEventManager успешно запущен!");
        log(Level.INFO, "API доступен для регистрации ивентов.");
    }

    @Override
    public void onDisable() {
        // Остановка активного ивента
        if (activeEventManager != null && activeEventManager.hasActiveEvent()) {
            activeEventManager.stopCurrentEvent();
        }

        // Остановка голосования
        if (voteManager != null && voteManager.isVotingActive()) {
            voteManager.cancelVoting();
        }

        // Остановка планировщика
        if (scheduleManager != null) {
            scheduleManager.stopScheduler();
        }

        log(Level.INFO, "qEventManager выключен.");
        instance = null;
    }

    public void reload() {
        configManager.loadAllConfigs();
        scheduleManager.stopScheduler();
        scheduleManager.startScheduler();
        log(Level.INFO, "Конфигурация перезагружена.");
    }

    public void log(Level level, String message) {
        getLogger().log(level, message);
    }

    public static qEventManager getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public EventRegistry getEventRegistry() {
        return eventRegistry;
    }

    public ScheduleManager getScheduleManager() {
        return scheduleManager;
    }

    public VoteManager getVoteManager() {
        return voteManager;
    }

    public ActiveEventManager getActiveEventManager() {
        return activeEventManager;
    }

    public GUIManager getGuiManager() {
        return guiManager;
    }

    public MessageUtil getMessageUtil() {
        return messageUtil;
    }

    public QEventAPI getAPI() {
        return api;
    }
}