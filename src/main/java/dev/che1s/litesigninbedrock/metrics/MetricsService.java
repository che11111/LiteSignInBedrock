package dev.che1s.litesigninbedrock.metrics;

import java.io.IOException;
import dev.che1s.litesigninbedrock.LiteSignInBedrock;

public final class MetricsService {
    public static final int SERVICE_ID = 34823;
    private final LiteSignInBedrock plugin;
    private RegionMetrics reporter;
    public MetricsService(LiteSignInBedrock plugin) { this.plugin = plugin; }
    public synchronized void reload() {
        if (!plugin.getConfig().getBoolean("bstats.enabled", true)) { shutdown(); return; }
        if (reporter != null) return;
        try {
            reporter = new RegionMetrics(plugin, SERVICE_ID);
        } catch (IOException | RuntimeException error) {
            plugin.getLogger().warning(plugin.lang().text("logs.metrics-failed", "error", error.toString()));
        }
    }
    public synchronized void shutdown() {
        if (reporter != null) reporter.shutdown();
        reporter = null;
    }
}
