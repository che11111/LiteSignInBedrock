package dev.che1s.litesigninbedrock.metrics;

import dev.che1s.litesigninbedrock.LiteSignInBedrock;
import org.bstats.MetricsBase;
import org.bstats.json.JsonObjectBuilder;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.bukkit.configuration.InvalidConfigurationException;
import dev.che1s.litesigninbedrock.i18n.LanguageManager;
import java.util.Arrays;
import dev.che1s.litesigninbedrock.Scheduler;
import java.util.UUID;
import java.util.logging.Level;

/** Official bStats transport with a global-region data collector instead of BukkitScheduler. */
final class RegionMetrics {
    private final MetricsBase base;

    RegionMetrics(LiteSignInBedrock plugin, int serviceId) throws IOException {
        File file = new File(plugin.getDataFolder().getParentFile(), "bStats/config.yml");
        YamlConfiguration config;
        try {
            config = file.isFile() ? LanguageManager.read(Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) : new YamlConfiguration();
        } catch (InvalidConfigurationException invalid) {
            throw new IOException("Invalid bStats/config.yml; keeping metrics disabled", invalid);
        }
        if (!config.isSet("serverUuid")) {
            config.addDefault("enabled", true);
            config.addDefault("serverUuid", UUID.randomUUID().toString());
            config.addDefault("logFailedRequests", false);
            config.addDefault("logSentData", false);
            config.addDefault("logResponseStatusText", false);
            config.options().copyDefaults(true).header(String.join("\n", Arrays.asList(
                    "bStats anonymous metrics / bStats 匿名统计",
                    "Set enabled to false to disable globally. / enabled: false 全局关闭统计。",
                    "https://bstats.org/")));
            config.save(file);
        }
        this.base = new MetricsBase("bukkit", config.getString("serverUuid"), serviceId,
                config.getBoolean("enabled", true), RegionMetrics::platformData,
                data -> serviceData(data, plugin.getDescription().getVersion()),
                action -> {
                    if (plugin.isEnabled()) Scheduler.global(plugin, action);
                }, plugin::isEnabled,
                (message, error) -> plugin.getLogger().log(Level.WARNING, message, error),
                plugin.getLogger()::info, config.getBoolean("logFailedRequests", false),
                config.getBoolean("logSentData", false), config.getBoolean("logResponseStatusText", false));
    }

    private static void platformData(JsonObjectBuilder data) {
        // Called by MetricsBase through the global dispatcher; it never accesses player entities.
        data.appendField("playerAmount", Bukkit.getOnlinePlayers().size())
                .appendField("onlineMode", Bukkit.getOnlineMode() ? 1 : 0)
                .appendField("bukkitVersion", Bukkit.getVersion())
                .appendField("bukkitName", Bukkit.getName())
                .appendField("javaVersion", System.getProperty("java.version"))
                .appendField("osName", System.getProperty("os.name"))
                .appendField("osArch", System.getProperty("os.arch"))
                .appendField("osVersion", System.getProperty("os.version"))
                .appendField("coreCount", Runtime.getRuntime().availableProcessors());
    }

    static void serviceData(JsonObjectBuilder data, String version) {
        data.appendField("pluginVersion", version);
    }

    void shutdown() { this.base.shutdown(); }
}
