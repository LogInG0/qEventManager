package wtf.pawlend.qeventmanager.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import wtf.pawlend.qeventmanager.qEventManager;
import wtf.pawlend.qeventmanager.util.ColorUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class ConfigManager {

    private final qEventManager plugin;

    private FileConfiguration mainConfig;
    private FileConfiguration schedulesConfig;
    private FileConfiguration eventsConfig;
    private FileConfiguration messagesConfig;
    private FileConfiguration guiConfig;

    public ConfigManager(qEventManager plugin) {
        this.plugin = plugin;
    }

    public void loadAllConfigs() {
        // Создаём папку плагина
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        // Загружаем конфиги
        mainConfig = loadConfig("config.yml");
        schedulesConfig = loadConfig("schedules.yml");
        eventsConfig = loadConfig("events.yml");
        messagesConfig = loadConfig("messages.yml");
        guiConfig = loadConfig("gui.yml");

        plugin.log(Level.INFO, "Конфигурационные файлы загружены.");
    }

    private FileConfiguration loadConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);

        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Загружаем дефолтные значения
        InputStream defaultStream = plugin.getResource(fileName);
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            config.setDefaults(defaultConfig);
        }

        return config;
    }

    public void saveConfig(FileConfiguration config, String fileName) {
        try {
            config.save(new File(plugin.getDataFolder(), fileName));
        } catch (IOException e) {
            plugin.log(Level.SEVERE, "Не удалось сохранить " + fileName + ": " + e.getMessage());
        }
    }

    public String getMessage(String key) {
        String prefix = messagesConfig.getString("prefix", "&8[&6qEvent&8] ");
        String message = messagesConfig.getString("messages." + key, "&cСообщение не найдено: " + key);
        return ColorUtil.colorize(prefix + message);
    }

    public String getRawMessage(String key) {
        return ColorUtil.colorize(messagesConfig.getString("messages." + key, ""));
    }

    public FileConfiguration getMainConfig() {
        return mainConfig;
    }

    public FileConfiguration getSchedulesConfig() {
        return schedulesConfig;
    }

    public FileConfiguration getEventsConfig() {
        return eventsConfig;
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    public FileConfiguration getGuiConfig() {
        return guiConfig;
    }
}