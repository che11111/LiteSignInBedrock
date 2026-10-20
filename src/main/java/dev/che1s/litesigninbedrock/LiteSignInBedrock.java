package dev.che1s.litesigninbedrock;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.Plugin;
import dev.che1s.litesigninbedrock.command.SignInFormCommand;
import dev.che1s.litesigninbedrock.i18n.LanguageManager;
import dev.che1s.litesigninbedrock.listener.PlayerJoinListener;
import dev.che1s.litesigninbedrock.metrics.MetricsService;
import dev.che1s.litesigninbedrock.update.UpdateChecker;

public class LiteSignInBedrock extends JavaPlugin {
    private static LiteSignInBedrock instance;
    private FormManager formManager;
    private LanguageManager language;
    private FileConfiguration settings;
    private MetricsService metrics;
    private UpdateChecker updates;

    @Override public void onEnable() {
        instance = this;
        saveDefaultConfig();
        language = new LanguageManager(getDataFolder().toPath(), this::getResource, getLogger());
        if (!reloadSettings()) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        formManager = new FormManager(this);
        SignInFormCommand command = new SignInFormCommand(this, formManager);
        if (getCommand("litesigninbedrock") == null) {
            getLogger().severe(language.text("logs.missing-command"));
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        getCommand("litesigninbedrock").setExecutor(command);
        getCommand("litesigninbedrock").setTabCompleter(command);
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this, formManager), this);
        metrics = new MetricsService(this);
        metrics.reload();
        updates = new UpdateChecker(this);
        Bukkit.getPluginManager().registerEvents(updates, this);
        updates.reload();
        logDetectedPlugins();
        getLogger().info(language.text("logs.enabled", "version", getDescription().getVersion(), "language", language.locale()));
    }

    private void logDetectedPlugins() {
        String[] names = {"LiteSignIn", "floodgate", "PlaceholderAPI", "Geyser-Spigot"};
        boolean geyserDetected = false;
        for (String name : names) {
            Plugin dependency = Bukkit.getPluginManager().getPlugin(name);
            if (dependency == null || !dependency.isEnabled()) continue;
            getLogger().info(language.text("logs.plugin-detected", "plugin", dependency.getName(),
                "version", dependency.getDescription().getVersion()));
            if (name.equals("Geyser-Spigot")) geyserDetected = true;
        }
        if (!geyserDetected) getLogger().info(language.text("logs.geyser-external"));
    }

    /** Strict read: malformed config never replaces the last working in-memory settings. */
    public synchronized boolean reloadSettings() {
        try {
            Path file = getDataFolder().toPath().resolve("config.yml");
            YamlConfiguration next = LanguageManager.read(Files.newBufferedReader(file, StandardCharsets.UTF_8));
            YamlConfiguration defaults = LanguageManager.read(new InputStreamReader(getResource("config.yml"), StandardCharsets.UTF_8));
            boolean changed = false;
            for (String key : defaults.getKeys(true)) {
                if (!defaults.isConfigurationSection(key) && !next.contains(key)) {
                    next.set(key, defaults.get(key));
                    changed = true;
                }
            }
            if (changed) {
                try { LanguageManager.writeAtomic(file, next.saveToString()); }
                catch (IOException error) { getLogger().warning(language.text("logs.config-save-failed", "error", error.toString())); }
            }
            language.load(next.getString("language", "zh_CN"));
            settings = next;
            if (metrics != null) metrics.reload();
            if (updates != null) updates.reload();
            return true;
        } catch (Exception error) {
            getLogger().log(Level.SEVERE, language.text("logs.reload-failed"), error);
            return false;
        }
    }

    @Override public FileConfiguration getConfig() {
        return settings == null ? super.getConfig() : settings;
    }

    @Override public void onDisable() {
        if (updates != null) updates.shutdown();
        if (metrics != null) metrics.shutdown();
        if (language != null) getLogger().info(language.text("logs.disabled"));
        instance = null;
    }

    public static LiteSignInBedrock getInstance() { return instance; }
    public FormManager getFormManager() { return formManager; }
    public LanguageManager lang() { return language; }
}
