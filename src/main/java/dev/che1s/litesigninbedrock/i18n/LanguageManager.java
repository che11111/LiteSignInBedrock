package dev.che1s.litesigninbedrock.i18n;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/** Immutable in-memory snapshots; text rendering never reads files. */
public final class LanguageManager {
    private static final String[] BUILT_INS = {"zh_CN", "en_US"};
    private static final Pattern ARGUMENT = Pattern.compile("\\{([a-zA-Z0-9_-]+)}");
    private final Path directory;
    private final Function<String, InputStream> resources;
    private final Logger logger;
    private final Map<String, YamlConfiguration> bundled = new HashMap<>();
    private volatile Bundle active;

    public LanguageManager(Path folder, Function<String, InputStream> resources, Logger logger) {
        this.directory = folder.resolve("lang");
        this.resources = resources;
        this.logger = logger;
        for (String locale : BUILT_INS) bundled.put(locale, resource("lang/" + locale + ".yml"));
        this.active = bundle("zh_CN", bundled.get("zh_CN"));
    }

    public synchronized void load(String requested) {
        String locale = normalize(requested);
        if (locale == null) {
            logger.warning(text("logs.language-unknown", "language", String.valueOf(requested)));
            locale = "zh_CN";
        }
        YamlConfiguration selected = new YamlConfiguration();
        for (String builtIn : BUILT_INS) {
            Path file = directory.resolve(builtIn + ".yml");
            YamlConfiguration values = bundled.get(builtIn);
            try {
                Files.createDirectories(directory);
                if (!Files.exists(file)) {
                    try (InputStream stream = resources.apply("lang/" + builtIn + ".yml")) { Files.copy(stream, file); }
                }
                try {
                    YamlConfiguration disk = read(Files.newBufferedReader(file, StandardCharsets.UTF_8));
                    values = merge(bundled.get(builtIn), disk);
                    // Missing leaf keys are filled without replacing server-owned translations.
                    if (hasMissing(bundled.get(builtIn), disk)) writeAtomic(file, values.saveToString());
                } catch (IOException | InvalidConfigurationException error) {
                    logger.warning(text("logs.language-read-failed", "file", file.toString(), "error", error.toString()));
                }
            } catch (IOException error) {
                logger.warning(text("logs.language-save-failed", "file", file.toString(), "error", error.toString()));
            }
            if (builtIn.equals(locale)) selected = values;
        }
        active = bundle(locale, merge(bundled.get("en_US"), selected));
    }

    public String locale() { return active.locale; }

    public String text(String key, String... arguments) {
        Bundle snapshot = active;
        String template = snapshot.text.get(key);
        return format(template == null ? key : template, arguments);
    }

    public List<String> weekdays() { return active.weekdays; }

    public String message(String key, String... arguments) {
        String[] values = Arrays.copyOf(arguments, arguments.length + 2);
        values[arguments.length] = "prefix";
        values[arguments.length + 1] = text("messages.prefix");
        return ChatColor.translateAlternateColorCodes('&', text(key, values));
    }

    /** Replace once: inserted values are never parsed as another placeholder. */
    public static String format(String template, String... arguments) {
        Map<String, String> values = new HashMap<>();
        for (int i = 0; i + 1 < arguments.length; i += 2) values.put(arguments[i], arguments[i + 1]);
        Matcher matcher = ARGUMENT.matcher(template);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) matcher.appendReplacement(result, Matcher.quoteReplacement(values.containsKey(matcher.group(1))
            ? Objects.toString(values.get(matcher.group(1)), "") : matcher.group()));
        matcher.appendTail(result);
        return result.toString().replace("\\n", "\n");
    }

    static String normalize(String text) {
        if (text == null) return null;
        String normalized = text.trim().replace('-', '_');
        for (String locale : BUILT_INS) if (locale.equalsIgnoreCase(normalized)) return locale;
        return null;
    }

    private Bundle bundle(String locale, YamlConfiguration yaml) {
        Map<String, String> strings = new HashMap<>();
        for (String key : yaml.getKeys(true)) if (yaml.isString(key)) strings.put(key, yaml.getString(key));
        List<String> weekdays = yaml.getStringList("date.weekdays");
        if (weekdays.size() != 7) weekdays = bundled.get(locale).getStringList("date.weekdays");
        return new Bundle(locale, Collections.unmodifiableMap(strings),
            Collections.unmodifiableList(new ArrayList<>(weekdays)));
    }

    private YamlConfiguration resource(String name) {
        InputStream stream = resources.apply(name);
        if (stream == null) throw new IllegalStateException("Missing bundled resource: " + name);
        try { return read(new InputStreamReader(stream, StandardCharsets.UTF_8)); }
        catch (IOException | InvalidConfigurationException error) { throw new IllegalStateException(name, error); }
    }

    public static YamlConfiguration read(Reader source) throws IOException, InvalidConfigurationException {
        try (Reader reader = source) {
            StringBuilder text = new StringBuilder();
            char[] buffer = new char[4096];
            int length;
            while ((length = reader.read(buffer)) != -1) text.append(buffer, 0, length);
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.loadFromString(text.toString());
            return yaml;
        }
    }

    private static YamlConfiguration merge(YamlConfiguration defaults, YamlConfiguration disk) {
        YamlConfiguration merged = new YamlConfiguration();
        for (String key : defaults.getKeys(true)) if (!defaults.isConfigurationSection(key)) merged.set(key, defaults.get(key));
        for (String key : disk.getKeys(true)) {
            Object expected = defaults.get(key);
            Object value = disk.get(key);
            if (!disk.isConfigurationSection(key) && (expected == null || expected instanceof String && value instanceof String
                || expected instanceof List && value instanceof List)) merged.set(key, value);
        }
        return merged;
    }

    private static boolean hasMissing(YamlConfiguration defaults, YamlConfiguration disk) {
        for (String key : defaults.getKeys(true)) if (!defaults.isConfigurationSection(key) && !disk.contains(key)) return true;
        return false;
    }

    public static void writeAtomic(Path file, String text) throws IOException {
        Path temporary = Files.createTempFile(file.getParent(), file.getFileName().toString(), ".tmp");
        try {
            Files.write(temporary, text.getBytes(StandardCharsets.UTF_8));
            try { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException unsupported) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }

    private static final class Bundle {
        final String locale;
        final Map<String, String> text;
        final List<String> weekdays;
        Bundle(String locale, Map<String, String> text, List<String> weekdays) {
            this.locale = locale; this.text = text; this.weekdays = weekdays;
        }
    }
}
